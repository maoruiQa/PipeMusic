package org.schabi.newpipe.music.stream

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.schabi.newpipe.R
import org.schabi.newpipe.music.MusicGraph
import org.schabi.newpipe.music.data.MusicLocale
import org.schabi.newpipe.music.data.db.DOWNLOADING
import org.schabi.newpipe.music.data.db.DOWNLOAD_QUEUED
import org.schabi.newpipe.music.data.db.TRACK_TYPE_LOCAL
import org.schabi.newpipe.music.data.db.TrackEntity
import org.schabi.newpipe.music.ui.MusicMainActivity
import java.io.File
import java.io.IOException

/**
 * Joins the parts of a multi-part video, in order, into one song in the library. Parts that are
 * not downloaded yet are downloaded first; the worker waits for those downloads to finish.
 */
class PartsMergeWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private var notificationId = NOTIFICATION_BASE

    override suspend fun doWork(): Result {
        MusicGraph.init(applicationContext)
        val ids = inputData.getLongArray(KEY_TRACK_IDS)?.toList().orEmpty()
        val title = inputData.getString(KEY_TITLE).orEmpty()
        val deleteParts = inputData.getBoolean(KEY_DELETE_PARTS, false)
        if (ids.size < 2 || title.isBlank()) {
            return Result.failure()
        }
        val repository = MusicGraph.repository
        // One notification per merge, so that merging two videos at once never mixes them up.
        notificationId = NOTIFICATION_BASE + (AudioConcat.baseName(ids).hashCode() and 0x3f) * 2
        AudioDownloadWorker.createChannel(applicationContext)
        runCatching {
            setForeground(foregroundInfo(title, string(R.string.pm_merge_waiting)))
        }

        return try {
            repository.getTracks(ids).forEach { track ->
                val inProgress = track.downloadState == DOWNLOADING ||
                    track.downloadState == DOWNLOAD_QUEUED
                if (track.remoteUrl != null && repository.localFile(track) == null && !inProgress) {
                    repository.setDownloadState(track.id, DOWNLOAD_QUEUED, 0, null)
                    AudioDownloadWorker.enqueue(applicationContext, track.id)
                }
            }
            val idSet = ids.toHashSet()
            repository.allTracks.first { all ->
                all.none {
                    it.id in idSet &&
                        (it.downloadState == DOWNLOADING || it.downloadState == DOWNLOAD_QUEUED)
                }
            }

            val parts = repository.getTracks(ids)
            val files = parts.mapNotNull { repository.localFile(it) }
            if (parts.size != ids.size || files.size != ids.size) {
                throw IOException(string(R.string.pm_merge_error_missing, ids.size - files.size))
            }
            runCatching {
                setForeground(foregroundInfo(title, string(R.string.pm_merge_running)))
            }
            val baseName = AudioConcat.baseName(ids)
            val (merged, copied) = withContext(Dispatchers.IO) { concat(files, baseName) }
            val first = parts.first()
            val artwork = withContext(Dispatchers.IO) { copyArtwork(first, baseName) }
            repository.saveMergedTrack(
                TrackEntity(
                    identity = AudioConcat.identity(ids),
                    type = TRACK_TYPE_LOCAL,
                    title = title,
                    artist = first.artist,
                    album = first.album,
                    durationMs = parts.sumOf { it.durationMs },
                    artworkUri = artwork,
                    mediaUri = merged.absolutePath,
                    serviceId = first.serviceId,
                    thumbnailUrl = first.thumbnailUrl,
                    mimeType = if (copied) first.mimeType else MIME_FALLBACK,
                    fileSize = merged.length(),
                    addedAt = System.currentTimeMillis()
                )
            )
            if (deleteParts) {
                repository.clearDownloads(ids)
            }
            notifyFinished(title, string(R.string.pm_merge_complete))
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w(TAG, "Merging ${ids.size} parts failed: ${error.javaClass.simpleName}")
            val message = error.message?.takeIf { error is IOException }
                ?: string(R.string.pm_download_error_generic)
            notifyFinished(title, string(R.string.pm_merge_failed, message))
            Result.failure()
        }
    }

    /**
     * Joins [files] without re-encoding when they share a container, falling back to AAC. Returns
     * the merged file and whether the parts were copied as they were.
     */
    private fun concat(files: List<File>, baseName: String): Pair<File, Boolean> {
        val musicDir = MusicGraph.repository.musicDir
        val list = File(applicationContext.cacheDir, "$baseName.txt")
        list.writeText(AudioConcat.listFile(files.map { it.absolutePath }))
        try {
            AudioConcat.commonSuffix(files.map { it.name })?.let { suffix ->
                val target = File(musicDir, "$baseName.$suffix")
                val copied = runFfmpeg(list, target, suffix, listOf("-c", "copy"))
                if (copied != null) {
                    return copied to true
                }
            }
            val encoded = runFfmpeg(
                list,
                File(musicDir, "$baseName.${AudioConcat.FALLBACK_SUFFIX}"),
                AudioConcat.FALLBACK_SUFFIX,
                listOf("-c:a", "aac", "-b:a", "192k")
            ) ?: throw IOException(string(R.string.pm_merge_error_ffmpeg))
            return encoded to false
        } finally {
            list.delete()
        }
    }

    /** Writes to a temporary file first, so a failed run never replaces an earlier merge. */
    private fun runFfmpeg(list: File, target: File, suffix: String, codec: List<String>): File? {
        val temp = File(target.parentFile, "${target.nameWithoutExtension}.tmp.$suffix")
        temp.delete()
        val arguments = listOf("-y", "-f", "concat", "-safe", "0", "-i", list.absolutePath) +
            listOf("-vn") + codec + temp.absolutePath
        val session = FFmpegKit.executeWithArguments(arguments.toTypedArray())
        if (!ReturnCode.isSuccess(session.returnCode) || !temp.exists() || temp.length() == 0L) {
            temp.delete()
            return null
        }
        target.delete()
        if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
        return target
    }

    /**
     * Gives the merged song its own copy of the first part's artwork, so deleting either song
     * never removes the other one's cover.
     */
    private fun copyArtwork(first: TrackEntity, baseName: String): String? {
        val source = first.artworkUri
        if (source != null && source.startsWith("/") && File(source).exists()) {
            val copy = File(MusicGraph.repository.artworkDir, "art_$baseName.jpg")
            runCatching { File(source).copyTo(copy, overwrite = true) }
                .onSuccess { return copy.absolutePath }
        }
        return first.thumbnailUrl
    }

    private fun string(resId: Int, vararg args: Any): String =
        MusicLocale.localized(applicationContext).getString(resId, *args)

    private fun contentIntent(requestCode: Int): PendingIntent = PendingIntent.getActivity(
        applicationContext,
        requestCode,
        Intent(applicationContext, MusicMainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun foregroundInfo(title: String, text: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent(402))
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .addAction(
                0,
                string(R.string.pm_action_cancel),
                WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
            )
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun notifyFinished(title: String, text: String) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent(403))
            .setAutoCancel(true)
            .build()
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(notificationId + 1, notification)
    }

    companion object {
        private const val TAG = "MusicDownload"
        private const val KEY_TRACK_IDS = "track_ids"
        private const val KEY_TITLE = "title"
        private const val KEY_DELETE_PARTS = "delete_parts"
        private const val MIME_FALLBACK = "audio/mp4"
        private const val NOTIFICATION_BASE = 4600
        private const val CHANNEL_ID = AudioDownloadWorker.CHANNEL_ID

        fun enqueue(context: Context, trackIds: List<Long>, title: String, deleteParts: Boolean) {
            val request = OneTimeWorkRequestBuilder<PartsMergeWorker>()
                .setInputData(
                    workDataOf(
                        KEY_TRACK_IDS to trackIds.toLongArray(),
                        KEY_TITLE to title,
                        KEY_DELETE_PARTS to deleteParts
                    )
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "pipemusic-merge-" + AudioConcat.baseName(trackIds),
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
