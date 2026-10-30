package org.schabi.newpipe.music.stream

/** Rules for joining the downloaded parts of a multi-part video into one audio file. */
object AudioConcat {

    /** Used when the parts do not share one container, so they are re-encoded to AAC. */
    const val FALLBACK_SUFFIX = "m4a"

    /**
     * The input list for FFmpeg's concat demuxer. Single quotes in a path are closed, escaped and
     * reopened, as the demuxer's quoting rules require.
     */
    fun listFile(paths: List<String>): String =
        paths.joinToString(separator = "\n", postfix = "\n") { path ->
            "file '${path.replace("'", "'\\''")}'"
        }

    /** The parts' common file extension, or `null` when they differ and must be re-encoded. */
    fun commonSuffix(fileNames: List<String>): String? {
        val suffixes = fileNames.map { it.substringAfterLast('.', "").lowercase() }.distinct()
        return suffixes.singleOrNull()?.takeIf { it.isNotEmpty() }
    }

    /**
     * Library identity of the song merged from [trackIds], in order. Merging the same parts again
     * replaces that song instead of adding a duplicate.
     */
    fun identity(trackIds: List<Long>): String = "merged|" + trackIds.joinToString(",")

    /** File name (without extension) for the song merged from [trackIds]. */
    fun baseName(trackIds: List<Long>): String =
        "merged_${trackIds.first()}_${trackIds.size}_" +
            Integer.toHexString(identity(trackIds).hashCode())
}
