package org.schabi.newpipe.music.local

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.R
import org.schabi.newpipe.music.MusicGraph
import org.schabi.newpipe.music.data.db.TRACK_TYPE_LOCAL
import org.schabi.newpipe.music.data.db.TrackEntity
import org.schabi.newpipe.music.ui.MusicMainActivity
import java.io.File
import java.util.Locale

/**
 * Copies a user selected folder into app private storage and registers every audio file in the
 * music library. Progress is exposed both through WorkManager and [ImportStatus] for the UI.
 */
class LocalImportWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        MusicGraph.init(applicationContext)
        ImportStatus.begin()
        createChannel()
        runCatching {
            setForeground(foregroundInfo(0, 0, string(R.string.pm_import_scanning), true))
        }

        val documentUri = inputData.getString(KEY_DOCUMENT_URI)?.let(Uri::parse)
        if (documentUri != null) {
            return importSingle(documentUri)
        }

        val treeUri = inputData.getString(KEY_TREE_URI)?.let(Uri::parse)
            ?: return fail(string(R.string.pm_import_error_no_folder))

        return try {
            val files = withContext(Dispatchers.IO) { collectFiles(treeUri) }
            if (files.isEmpty()) {
                ImportStatus.finish()
                return Result.success(workDataOf(KEY_ADDED to 0, KEY_SKIPPED to 0, KEY_FAILED to 0))
            }
            ImportStatus.update { it.copy(total = files.size) }

            var added = 0
            var skipped = 0
            var failed = 0
            files.forEachIndexed { index, item ->
                if (isStopped) {
                    throw CancellationException("import cancelled")
                }
                ImportStatus.update {
                    it.copy(current = index + 1, currentName = item.displayName)
                }
                if (MusicGraph.repository.findBySourceUri(item.uri) != null) {
                    skipped += 1
                } else {
                    val ok = withContext(Dispatchers.IO) { importOne(item) }
                    if (ok) {
                        added += 1
                    } else {
                        failed += 1
                    }
                }
                ImportStatus.update {
                    it.copy(added = added, skipped = skipped, failed = failed)
                }
                setProgress(
                    workDataOf(
                        KEY_DONE to index + 1,
                        KEY_TOTAL to files.size,
                        KEY_ADDED to added
                    )
                )
                if ((index + 1) % 5 == 0 || index == files.lastIndex) {
                    runCatching {
                        setForeground(
                            foregroundInfo(
                                index + 1,
                                files.size,
                                item.displayName,
                                indeterminate = false
                            )
                        )
                    }
                }
            }

            ImportStatus.finish()
            Result.success(
                workDataOf(
                    KEY_ADDED to added,
                    KEY_SKIPPED to skipped,
                    KEY_FAILED to failed
                )
            )
        } catch (cancelled: CancellationException) {
            ImportStatus.finish(string(R.string.pm_import_cancelled))
            throw cancelled
        } catch (error: Exception) {
            ImportStatus.finish(error.message ?: string(R.string.pm_import_failed))
            Result.failure(
                workDataOf(KEY_ERROR to (error.message ?: string(R.string.pm_import_failed)))
            )
        }
    }

    private suspend fun importSingle(uri: Uri): Result {
        val item = withContext(Dispatchers.IO) { readDocumentInfo(uri) }
            ?: return fail(string(R.string.pm_import_error_no_file))
        ImportStatus.update {
            it.copy(total = 1, current = 1, currentName = item.displayName)
        }
        return try {
            val alreadyImported = MusicGraph.repository.findBySourceUri(item.uri) != null
            val added = when {
                alreadyImported -> 0
                withContext(Dispatchers.IO) { importOne(item) } -> 1
                else -> 0
            }
            val skipped = if (alreadyImported) 1 else 0
            val failed = if (!alreadyImported && added == 0) 1 else 0
            ImportStatus.update {
                it.copy(added = added, skipped = skipped, failed = failed)
            }
            ImportStatus.finish(
                if (failed > 0) string(R.string.pm_import_failed) else null
            )
            if (failed > 0) {
                Result.failure(workDataOf(KEY_ERROR to string(R.string.pm_import_failed)))
            } else {
                Result.success(
                    workDataOf(KEY_ADDED to added, KEY_SKIPPED to skipped, KEY_FAILED to 0)
                )
            }
        } catch (cancelled: CancellationException) {
            ImportStatus.finish(string(R.string.pm_import_cancelled))
            throw cancelled
        } catch (error: Exception) {
            ImportStatus.finish(error.message ?: string(R.string.pm_import_failed))
            Result.failure(
                workDataOf(KEY_ERROR to (error.message ?: string(R.string.pm_import_failed)))
            )
        }
    }

    private fun readDocumentInfo(uri: Uri): ScannedAudio? {
        val resolver = applicationContext.contentResolver
        var displayName: String? = null
        var size: Long? = null
        runCatching {
            resolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    displayName = cursor.getString(0)
                    if (!cursor.isNull(1)) {
                        size = cursor.getLong(1)
                    }
                }
            }
        }
        val name = displayName?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: return null
        val mime = runCatching { resolver.getType(uri) }.getOrNull()
        if (!FolderScanner.isAudioDocument(name, mime)) {
            return null
        }
        return ScannedAudio(
            uri = uri.toString(),
            displayName = name,
            size = size,
            mimeType = mime,
            folderName = string(R.string.pm_local_music)
        )
    }

    private fun collectFiles(treeUri: Uri): List<ScannedAudio> {
        val result = ArrayList<ScannedAudio>()
        try {
            FolderScanner(applicationContext).scan(treeUri) { item ->
                if (isStopped) {
                    false
                } else {
                    result.add(item)
                    true
                }
            }
        } catch (error: SecurityException) {
            throw IllegalStateException(string(R.string.pm_import_error_no_permission), error)
        }
        return result
    }

    private suspend fun importOne(item: ScannedAudio): Boolean {
        val name = safeName(item.displayName)
        val hash = Math.abs(item.uri.hashCode()).toString(16)
        val extension = extensionOf(item)
        val destination = File(MusicGraph.repository.musicDir, "${name}_$hash.$extension")
        val temp = File(destination.absolutePath + ".part")
        return try {
            val opened = applicationContext.contentResolver.openInputStream(Uri.parse(item.uri))
                ?: return false
            opened.use { input ->
                temp.outputStream().use { output ->
                    input.copyTo(output, DEFAULT_BUFFER_SIZE * 4)
                }
            }
            if (isStopped) {
                temp.delete()
                return false
            }
            if (!temp.renameTo(destination)) {
                temp.copyTo(destination, overwrite = true)
                temp.delete()
            }
            val metadata = AudioMetadataReader.read(
                destination,
                fallbackTitle = item.displayName.substringBeforeLast('.')
                    .ifBlank { string(R.string.pm_untitled_song) },
                fallbackAlbum = item.folderName.ifBlank { string(R.string.pm_local_music) },
                fallbackArtist = string(R.string.pm_unknown_artist)
            )
            val artworkPath = saveArtwork(item.uri, metadata.artwork)
            val id = MusicGraph.repository.insertLocalTrack(
                TrackEntity(
                    identity = "local|${item.uri}",
                    type = TRACK_TYPE_LOCAL,
                    title = metadata.title ?: destination.nameWithoutExtension,
                    artist = metadata.artist ?: string(R.string.pm_unknown_artist),
                    album = metadata.album ?: string(R.string.pm_local_music),
                    durationMs = metadata.durationMs,
                    artworkUri = artworkPath,
                    mediaUri = destination.absolutePath,
                    sourceUri = item.uri,
                    mimeType = item.mimeType,
                    fileSize = destination.length(),
                    addedAt = System.currentTimeMillis()
                )
            )
            if (id <= 0) {
                destination.delete()
                artworkPath?.let { File(it).delete() }
                return false
            }
            true
        } catch (error: Exception) {
            temp.delete()
            destination.delete()
            false
        }
    }

    private fun saveArtwork(sourceUri: String, bytes: ByteArray?): String? {
        if (bytes == null || !AudioMetadataReader.isValidImage(bytes)) {
            return null
        }
        return runCatching {
            val hash = Math.abs(sourceUri.hashCode()).toString(16)
            val file = File(MusicGraph.repository.artworkDir, "art_$hash.jpg")
            file.writeBytes(bytes)
            file.absolutePath
        }.getOrNull()
    }

    private fun safeName(displayName: String): String {
        val base = displayName.substringBeforeLast('.').ifBlank { "track" }
        return base.replace(Regex("[\\\\/:*?\"<>|\\n\\r]"), "_").take(60).trim()
    }

    private fun string(resId: Int): String =
        org.schabi.newpipe.music.data.MusicLocale.localized(applicationContext)
            .getString(resId)

    private fun extensionOf(item: ScannedAudio): String {
        val fromName = item.displayName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (fromName.isNotBlank() && fromName.length <= 5) {
            return fromName
        }
        return when (item.mimeType?.lowercase(Locale.ROOT)) {
            "audio/flac", "audio/x-flac" -> "flac"
            "audio/mp4", "audio/m4a", "audio/x-m4a" -> "m4a"
            "audio/mpeg" -> "mp3"
            "audio/ogg" -> "ogg"
            "audio/opus" -> "opus"
            "audio/wav", "audio/x-wav" -> "wav"
            "audio/aac" -> "aac"
            else -> "mp3"
        }
    }

    private fun fail(message: String): Result {
        ImportStatus.finish(message)
        return Result.failure(workDataOf(KEY_ERROR to message))
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    string(R.string.pm_import_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = string(R.string.pm_import_channel_desc)
                }
            )
        }
    }

    private fun foregroundInfo(
        current: Int,
        total: Int,
        text: String,
        indeterminate: Boolean
    ): ForegroundInfo {
        val intent = Intent(applicationContext, MusicMainActivity::class.java)
        val pending = PendingIntent.getActivity(
            applicationContext,
            300,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(string(R.string.pm_import_notification_title))
            .setContentText(text)
            .setContentIntent(pending)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(if (total > 0) total else 100, current, indeterminate || total == 0)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val UNIQUE_NAME = "pipemusic-local-import"
        const val KEY_TREE_URI = "tree_uri"
        const val KEY_DOCUMENT_URI = "document_uri"
        const val KEY_DONE = "done"
        const val KEY_TOTAL = "total"
        const val KEY_ADDED = "added"
        const val KEY_SKIPPED = "skipped"
        const val KEY_FAILED = "failed"
        const val KEY_ERROR = "error"

        private const val CHANNEL_ID = "music_import"
        private const val NOTIFICATION_ID = 4301
    }
}
