package org.schabi.newpipe.music.stream

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import org.schabi.newpipe.extractor.services.youtube.sabr.YoutubeSabrInfo
import org.schabi.newpipe.extractor.services.youtube.sabr.YoutubeSabrRequest
import org.schabi.newpipe.extractor.services.youtube.sabr.YoutubeSabrSession
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.youtube.SabrAttestationRetryHandler
import org.schabi.newpipe.youtube.SabrPlayerReloader
import org.schabi.newpipe.youtube.SabrRequestCoordinator
import us.shandian.giga.get.MissionRecoveryInfo
import us.shandian.giga.get.SabrDownloadFormatResolver
import us.shandian.giga.get.SabrDownloadTarget
import us.shandian.giga.get.SabrSegmentWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.function.BooleanSupplier
import kotlin.coroutines.cancellation.CancellationException

/**
 * Downloads a single YouTube SABR audio format. YouTube no longer exposes progressive audio URLs,
 * so the media has to be pulled segment by segment through a SABR session, the same way
 * PipePipe's own downloader does, and then remuxed into a standalone audio file.
 */
internal class SabrAudioDownloader(
    private val workDir: File,
    private val isStopped: () -> Boolean,
    private val onProgress: (Int) -> Unit
) {

    @Throws(IOException::class)
    fun download(stream: AudioStream, output: File) {
        val info = stream.deliveryMethodInfo as? YoutubeSabrInfo
            ?: throw IOException("Missing SABR info")
        val poToken = info.poToken ?: throw IOException("SABR info has no player PO token")
        val recoveries = arrayOf(MissionRecoveryInfo(stream))

        workDir.deleteRecursively()
        if (!workDir.mkdirs()) {
            throw IOException("Could not create temporary directory")
        }
        try {
            val targets = SabrDownloadFormatResolver.buildTargets(info, recoveries, workDir)
            val expectedLength = targets.sumOf { it.format.contentLength.coerceAtLeast(0) }
            var written = 0L
            val session = YoutubeSabrSession(info)
            session.setPoToken(poToken)
            session.setInfoReloader(SabrPlayerReloader)
            val coordinator = SabrRequestCoordinator(
                session,
                SabrAttestationRetryHandler(info.videoId),
                null
            )
            val outputs = targets.associate { it.resourceIndex to FileOutputStream(it.file) }
            try {
                val writer = SabrSegmentWriter(targets, outputs) { _, delta ->
                    written += delta
                    if (expectedLength > 0) {
                        onProgress((written * 100 / expectedLength).toInt().coerceIn(0, 99))
                    }
                }
                downloadSegments(coordinator, targets, writer)
            } finally {
                outputs.values.forEach { runCatching { it.close() } }
            }
            ensureRunning()
            remux(targets.first().file, output)
        } finally {
            workDir.deleteRecursively()
        }
    }

    private fun downloadSegments(
        coordinator: SabrRequestCoordinator,
        targets: List<SabrDownloadTarget>,
        writer: SabrSegmentWriter
    ) {
        ensureRunning()
        requestWithRetry(
            coordinator,
            YoutubeSabrRequest.preparation(0L, targets.map { it.format }),
            writer,
            { targets.all { it.initializationWritten } }
        )
        writer.observeWrittenInitializations()

        var noProgressResponses = 0
        while (!isComplete(targets)) {
            ensureRunning()
            val playerTimeMs = targets.minOf { target ->
                if (target.nextWriteSequence <= 1) {
                    0L
                } else {
                    target.timeline?.getEndMs(target.nextWriteSequence - 1) ?: 0L
                }
            }
            val tracks = targets.map {
                YoutubeSabrRequest.Track.of(it.format, it.timeline, it.nextWriteSequence - 1)
            }
            val before = targets.map { it.nextWriteSequence }
            requestWithRetry(
                coordinator,
                YoutubeSabrRequest.playback(playerTimeMs, 1.0f, tracks),
                writer,
                null
            )
            writer.observeWrittenInitializations()
            if (isComplete(targets)) {
                break
            }
            if (targets.indices.any { targets[it].nextWriteSequence > before[it] }) {
                noProgressResponses = 0
            } else if (++noProgressResponses >= MAX_NO_PROGRESS_RESPONSES) {
                throw IOException("SABR download stalled")
            } else {
                Thread.sleep(IDLE_POLL_MS)
            }
        }
    }

    private fun requestWithRetry(
        coordinator: SabrRequestCoordinator,
        request: YoutubeSabrRequest,
        writer: SabrSegmentWriter,
        progressChecker: BooleanSupplier?
    ) {
        var attempts = 0
        while (true) {
            try {
                coordinator.request(request, { writer.acceptSegment(it) }, progressChecker)
                return
            } catch (error: IOException) {
                val retryable = error is SocketTimeoutException ||
                    error is ConnectException ||
                    error is UnknownHostException ||
                    error is SocketException
                if (!retryable || attempts >= MAX_NETWORK_RETRIES) {
                    throw error
                }
                attempts++
                Thread.sleep(NETWORK_RETRY_DELAY_MS)
                ensureRunning()
            }
        }
    }

    private fun isComplete(targets: List<SabrDownloadTarget>): Boolean = targets.all { target ->
        val endSequence = target.timeline?.endSequence ?: Int.MAX_VALUE
        target.pending.isEmpty() && target.nextWriteSequence > endSequence
    }

    private fun remux(input: File, output: File) {
        if (output.exists()) {
            output.delete()
        }
        val arguments = mutableListOf("-hide_banner", "-loglevel", "fatal", "-y",
            "-i", input.absolutePath, "-map", "0:a?", "-vn", "-c", "copy")
        if (output.extension.equals("m4a", ignoreCase = true)) {
            arguments += listOf("-movflags", "+faststart")
        }
        arguments += output.absolutePath
        val session = FFmpegKit.executeWithArguments(arguments.toTypedArray())
        if (!ReturnCode.isSuccess(session.returnCode) || !output.exists() || output.length() == 0L) {
            output.delete()
            throw IOException("SABR download failed: ffmpeg remux failed (${session.returnCode})")
        }
    }

    private fun ensureRunning() {
        if (isStopped() || Thread.currentThread().isInterrupted) {
            throw CancellationException("download cancelled")
        }
    }

    private companion object {
        private const val IDLE_POLL_MS = 250L
        private const val MAX_NO_PROGRESS_RESPONSES = 60
        private const val MAX_NETWORK_RETRIES = 2
        private const val NETWORK_RETRY_DELAY_MS = 1_000L
    }
}
