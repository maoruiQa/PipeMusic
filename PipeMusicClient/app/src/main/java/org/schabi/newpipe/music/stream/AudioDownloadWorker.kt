package org.schabi.newpipe.music.stream

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.schabi.newpipe.DownloaderImpl
import org.schabi.newpipe.R
import org.schabi.newpipe.music.MusicGraph
import org.schabi.newpipe.music.data.db.DOWNLOADING
import org.schabi.newpipe.music.data.db.DOWNLOAD_FAILED
import org.schabi.newpipe.music.data.db.DOWNLOAD_NONE
import org.schabi.newpipe.music.data.db.DOWNLOAD_QUEUED
import org.schabi.newpipe.music.data.db.TrackEntity
import org.schabi.newpipe.music.ui.MusicMainActivity
import org.schabi.newpipe.util.ServiceHelper
import java.io.File
import java.io.IOException
import java.util.Locale

/**
 * Downloads the audio of a remote song. The streaming URL is resolved inside the worker so that
 * an expired URL from an earlier search result is never reused.
 */
class AudioDownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        MusicGraph.init(applicationContext)
        // Re-apply account cookies before resolving the stream. This keeps downloads working
        // when WorkManager restarts the process or the user has just entered a cookie manually.
        runCatching { ServiceHelper.initServices(applicationContext) }
        val trackId = inputData.getLong(KEY_TRACK_ID, -1)
        if (trackId <= 0) {
            return Result.failure()
        }
        val repository = MusicGraph.repository
        val track = repository.getTrack(trackId) ?: return Result.failure()
        createChannel()
        runCatching {
            setForeground(foregroundInfo(track, 0, string(R.string.pm_download_preparing)))
        }

        if (track.remoteUrl == null) {
            repository.setDownloadState(
                trackId,
                DOWNLOAD_FAILED,
                0,
                string(R.string.pm_download_error_no_source)
            )
            return Result.failure()
        }

        return try {
            repository.setDownloadState(trackId, DOWNLOAD_QUEUED, 0, null)
            val resolver = AudioStreamResolver(applicationContext)
            val info = withContext(Dispatchers.IO) {
                resolver.loadInfo(track.serviceId, track.remoteUrl)
            }
            val stream = resolver.pickDownloadStream(info, MusicGraph.preferences.downloadQuality.value)
                ?: throw IOException(string(R.string.pm_download_error_no_audio))

            repository.setDownloadState(trackId, DOWNLOADING, 0, null)
            val baseName = safeName(track.title)
            if (stream.sabrStream != null) {
                val sabrFile = File(repository.musicDir, "${baseName}_${track.id}.${stream.suffix}")
                withContext(Dispatchers.IO) {
                    var lastUpdate = 0L
                    SabrAudioDownloader(
                        File(applicationContext.cacheDir, "music-sabr/${track.id}"),
                        { isStopped }
                    ) { progress ->
                        val now = System.currentTimeMillis()
                        if (now - lastUpdate > 500) {
                            lastUpdate = now
                            runBlocking {
                                repository.setDownloadState(trackId, DOWNLOADING, progress, null)
                            }
                        }
                    }.download(stream.sabrStream, sabrFile)
                }
                repository.setDownloadState(trackId, DOWNLOADING, 99, null)
                val sabrArtwork = withContext(Dispatchers.IO) { downloadArtwork(track) }
                repository.completeDownload(
                    trackId,
                    sabrFile.absolutePath,
                    sabrFile.length(),
                    stream.mimeType,
                    sabrArtwork
                )
                notifyFinished(track, string(R.string.pm_download_complete))
                return Result.success(workDataOf(KEY_PATH to sabrFile.absolutePath))
            }
            if (stream.isHls) {
                val hlsFile = downloadHlsFile(stream, track, baseName)
                repository.setDownloadState(trackId, DOWNLOADING, 99, null)
                val hlsArtwork = withContext(Dispatchers.IO) { downloadArtwork(track) }
                repository.completeDownload(
                    trackId,
                    hlsFile.absolutePath,
                    hlsFile.length(),
                    stream.mimeType,
                    hlsArtwork
                )
                notifyFinished(track, string(R.string.pm_download_complete))
                return Result.success(workDataOf(KEY_PATH to hlsFile.absolutePath))
            }
            val temp = File(repository.musicDir, "${baseName}_${track.id}.${stream.suffix}.part")
            val finalFile = File(repository.musicDir, "${baseName}_${track.id}.${stream.suffix}")

            withContext(Dispatchers.IO) {
                download(stream, temp, trackId)
            }
            if (isStopped) {
                throw CancellationException("download cancelled")
            }

            var resultFile = finalFile
            if (stream.needsAudioExtraction) {
                resultFile = extractAudio(temp, finalFile)
            } else if (!temp.renameTo(finalFile)) {
                temp.copyTo(finalFile, overwrite = true)
                temp.delete()
            }
            if (!resultFile.exists() || resultFile.length() == 0L) {
                throw IOException(string(R.string.pm_download_error_empty))
            }

            val artwork = withContext(Dispatchers.IO) { downloadArtwork(track) }
            repository.completeDownload(
                trackId,
                resultFile.absolutePath,
                resultFile.length(),
                stream.mimeType,
                artwork
            )
            notifyFinished(track, string(R.string.pm_download_complete))
            Result.success(workDataOf(KEY_PATH to resultFile.absolutePath))
        } catch (cancelled: CancellationException) {
            repository.setDownloadState(trackId, DOWNLOAD_NONE, 0, null)
            throw cancelled
        } catch (error: Exception) {
            val message = friendlyError(error)
            repository.setDownloadState(trackId, DOWNLOAD_FAILED, 0, message)
            notifyFinished(
                track,
                string(R.string.pm_download_failed_notification, message)
            )
            Result.failure(
                workDataOf(
                    KEY_ERROR to (error.message ?: string(R.string.pm_download_error_generic))
                )
            )
        }
    }

    private fun string(resId: Int, vararg args: Any): String =
        org.schabi.newpipe.music.data.MusicLocale.localized(applicationContext)
            .getString(resId, *args)

    private suspend fun download(stream: DownloadableStream, temp: File, trackId: Long) =
        withContext(Dispatchers.IO) {
        val client = DownloaderImpl.getInstance().client
        val requestBuilder = Request.Builder()
            .url(stream.url)
            .header(
                "User-Agent",
                stream.headers["User-Agent"] ?: DownloaderImpl.USER_AGENT
            )
            .header("Accept-Encoding", "identity")
            .header("Range", "bytes=0-")
        stream.headers.forEach { (key, value) ->
            if (key != "User-Agent" && key != "Accept-Encoding" && key != "Range") {
                requestBuilder.header(key, value)
            }
        }
        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }
            val body = response.body
                ?: throw IOException(string(R.string.pm_download_error_generic))
            val total = body.contentLength()
            var copied = 0L
            var lastUpdate = 0L
            body.byteStream().use { input ->
                temp.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        if (isStopped) {
                            throw CancellationException("download cancelled")
                        }
                        val read = input.read(buffer)
                        if (read < 0) {
                            break
                        }
                        output.write(buffer, 0, read)
                        copied += read
                        val now = System.currentTimeMillis()
                        if (now - lastUpdate > 500) {
                            lastUpdate = now
                            val progress = if (total > 0) {
                                (copied * 100 / total).toInt().coerceIn(0, 99)
                            } else {
                                0
                            }
                            MusicGraph.repository.setDownloadState(
                                trackId,
                                DOWNLOADING,
                                progress,
                                null
                            )
                        }
                    }
                }
            }
            val progress = if (total > 0) {
                (copied * 100 / total).toInt().coerceIn(0, 99)
            } else {
                0
            }
            MusicGraph.repository.setDownloadState(trackId, DOWNLOADING, progress, null)
        }
    }

    private suspend fun downloadHlsFile(
        stream: DownloadableStream,
        track: TrackEntity,
        baseName: String
    ): File = withContext(Dispatchers.IO) {
        val output = File(MusicGraph.repository.musicDir, "${baseName}_${track.id}.${stream.suffix}")
        if (output.exists()) {
            output.delete()
        }
        val headerText = stream.headers
            .filterKeys { it != "Range" && it != "Accept-Encoding" }
            .entries
            .joinToString("\r\n") { (key, value) -> "$key: $value" }
        val arguments = ArrayList<String>()
        arguments.add("-y")
        if (headerText.isNotBlank()) {
            arguments.add("-headers")
            arguments.add(headerText)
        }
        arguments.add("-i")
        arguments.add(stream.url)
        arguments.add("-vn")
        arguments.add("-c:a")
        arguments.add("copy")
        arguments.add(output.absolutePath)
        val session = FFmpegKit.executeWithArguments(arguments.toTypedArray())
        if (!ReturnCode.isSuccess(session.returnCode) || !output.exists() || output.length() == 0L) {
            output.delete()
            throw IOException(string(R.string.pm_download_error_hls))
        }
        output
    }

    private fun extractAudio(temp: File, target: File): File {
        if (target.exists()) {
            target.delete()
        }
        val session = FFmpegKit.executeWithArguments(
            arrayOf(
                "-y",
                "-i", temp.absolutePath,
                "-vn",
                "-c:a", "copy",
                target.absolutePath
            )
        )
        return if (ReturnCode.isSuccess(session.returnCode) && target.exists()) {
            temp.delete()
            target
        } else {
            val fallback = File(target.absolutePath)
            if (fallback.exists()) {
                fallback.delete()
            }
            if (temp.renameTo(fallback)) {
                fallback
            } else {
                temp.copyTo(fallback, overwrite = true)
                temp.delete()
                fallback
            }
        }
    }

    private fun downloadArtwork(track: TrackEntity): String? {
        val url = track.thumbnailUrl ?: track.artworkUri ?: return null
        if (!url.startsWith("http")) {
            return null
        }
        val bitmap = runCatching {
            Picasso.get().load(url).resize(512, 512).centerCrop().get()
        }.getOrNull() ?: return null
        val file = File(MusicGraph.repository.artworkDir, "art_${track.id}.jpg")
        return runCatching {
            file.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)
            }
            file.absolutePath
        }.getOrNull()
    }

    private fun friendlyError(error: Throwable): String {
        val text = error.message.orEmpty()
        return when {
            error is org.schabi.newpipe.extractor.exceptions.AntiBotException ||
                text.contains("not a bot", true) ->
                string(R.string.pm_player_error_antibot)
            error is org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException ->
                string(R.string.pm_player_error_unavailable)
            text.contains("Bilibili", true) ->
                string(R.string.pm_player_error_bilibili)
            text.contains("Unable to resolve host", true) ||
                text.contains("timeout", true) ->
                string(R.string.pm_download_error_network)
            text.isNotBlank() -> text
            else -> string(R.string.pm_download_error_generic)
        }
    }

    private fun safeName(title: String): String =
        title.replace(Regex("[\\\\/:*?\"<>|\\n\\r]"), "_").take(60).trim()
            .ifBlank { "track" }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    string(R.string.pm_download_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = string(R.string.pm_download_channel_desc)
                }
            )
        }
    }

    private fun foregroundInfo(track: TrackEntity, progress: Int, text: String): ForegroundInfo {
        val intent = Intent(applicationContext, MusicMainActivity::class.java)
        val pending = PendingIntent.getActivity(
            applicationContext,
            400,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(track.title)
            .setContentText(text)
            .setContentIntent(pending)
            .setOnlyAlertOnce(true)
            .setOngoing(progress < 100)
            .setProgress(100, progress, progress <= 0)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId(track.id),
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(notificationId(track.id), notification)
        }
    }

    private fun notifyFinished(track: TrackEntity, text: String) {
        val intent = Intent(applicationContext, MusicMainActivity::class.java)
        val pending = PendingIntent.getActivity(
            applicationContext,
            401,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(track.title)
            .setContentText(text)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(notificationId(track.id), notification)
    }

    private fun notificationId(trackId: Long): Int = 4400 + (trackId % 100).toInt()

    companion object {
        const val KEY_TRACK_ID = "track_id"
        const val KEY_PATH = "path"
        const val KEY_ERROR = "error"

        private const val CHANNEL_ID = "music_download"

        fun workName(trackId: Long): String = "pipemusic-download-$trackId"

        fun enqueue(context: Context, trackId: Long) {
            val request = OneTimeWorkRequestBuilder<AudioDownloadWorker>()
                .setInputData(workDataOf(KEY_TRACK_ID to trackId))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                workName(trackId),
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancel(context: Context, trackId: Long) {
            WorkManager.getInstance(context).cancelUniqueWork(workName(trackId))
        }
    }
}
