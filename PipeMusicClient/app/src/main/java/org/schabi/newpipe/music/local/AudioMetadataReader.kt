package org.schabi.newpipe.music.local

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import java.io.File

data class AudioMetadata(
    val title: String?,
    val artist: String?,
    val album: String?,
    val durationMs: Long,
    val artwork: ByteArray?
)

object AudioMetadataReader {

    private const val TAG = "MusicImport"

    fun read(
        file: File,
        fallbackTitle: String,
        fallbackAlbum: String,
        fallbackArtist: String
    ): AudioMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.trim()?.takeIf { it.isNotEmpty() }
                ?: fallbackTitle
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.trim()?.takeIf { it.isNotEmpty() }
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                    ?.trim()?.takeIf { it.isNotEmpty() }
                ?: fallbackArtist
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.trim()?.takeIf { it.isNotEmpty() }
                ?: fallbackAlbum
            val duration = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val picture = runCatching { retriever.embeddedPicture }.getOrNull()
            AudioMetadata(
                title = title,
                artist = artist,
                album = album,
                durationMs = duration,
                artwork = picture?.takeIf { it.isNotEmpty() }
            )
        } catch (error: Exception) {
            Log.w(TAG, "Could not read metadata from ${file.name}", error)
            AudioMetadata(
                title = fallbackTitle,
                artist = fallbackArtist,
                album = fallbackAlbum,
                durationMs = 0,
                artwork = null
            )
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun isValidImage(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) {
            return false
        }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        return options.outWidth > 0 && options.outHeight > 0
    }
}
