package org.schabi.newpipe.music.local

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.util.ArrayDeque

data class ScannedAudio(
    val uri: String,
    val displayName: String,
    val size: Long?,
    val mimeType: String?,
    val folderName: String
)

/**
 * Recursively walks a SAF tree using [DocumentsContract] queries, which is considerably faster
 * than creating a [androidx.documentfile.provider.DocumentFile] for every entry.
 */
class FolderScanner(private val context: Context) {

    /**
     * Scans [treeUri] and calls [onAudio] for every audio document. Returning false from the
     * callback stops the scan, which is how cancellation is implemented.
     */
    fun scan(treeUri: Uri, onAudio: (ScannedAudio) -> Boolean) {
        val resolver = context.contentResolver
        val rootId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
            ?: return
        val queue = ArrayDeque<Pair<String, String>>()
        queue.add(rootId to "")
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )

        while (queue.isNotEmpty()) {
            val (documentId, parentName) = queue.removeFirst()
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                documentId
            )
            resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val childId = cursor.getString(0) ?: continue
                    val name = cursor.getString(1).orEmpty()
                    val mime = cursor.getString(2)
                    val size = if (cursor.isNull(3)) null else cursor.getLong(3)
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        queue.add(childId to (name.ifBlank { parentName }))
                        continue
                    }
                    if (!isAudio(name, mime)) {
                        continue
                    }
                    val documentUri = DocumentsContract.buildDocumentUriUsingTree(
                        treeUri,
                        childId
                    )
                    val item = ScannedAudio(
                        uri = documentUri.toString(),
                        displayName = name,
                        size = size,
                        mimeType = mime,
                        folderName = parentName
                    )
                    if (!onAudio(item)) {
                        return
                    }
                }
            }
        }
    }

    private fun isAudio(name: String, mime: String?): Boolean = isAudioDocument(name, mime)

    companion object {
        private val AUDIO_EXTENSIONS = setOf(
            "mp3", "m4a", "aac", "flac", "wav", "ogg", "oga", "opus", "wma", "ape", "alac",
            "m4b", "mp4", "webm"
        )

        fun isAudioDocument(name: String, mime: String?): Boolean {
            if (mime != null && mime.startsWith("audio/")) {
                return true
            }
            val extension = name.substringAfterLast('.', "").lowercase()
            return extension in AUDIO_EXTENSIONS
        }
    }
}
