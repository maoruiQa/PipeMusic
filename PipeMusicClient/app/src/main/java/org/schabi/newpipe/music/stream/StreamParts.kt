package org.schabi.newpipe.music.stream

/**
 * Helpers for multi-part videos. A BiliBili upload can hold several parts ("P1", "P2", ...),
 * each one a separate stream whose URL only differs in its `p` query parameter.
 */
object StreamParts {

    private val PART_PARAM = Regex("[?&]p=(\\d+)")

    /** Part number encoded in [url], or null when the URL does not name one. */
    fun partNumber(url: String): Int? =
        PART_PARAM.find(url)?.groupValues?.get(1)?.toIntOrNull()

    /**
     * Position of [url] in [partUrls]. Pasted links carry tracking parameters or another URL
     * form, so the part number is compared when there is no exact match. A URL without a part
     * number refers to the first part.
     */
    fun indexOf(partUrls: List<String>, url: String): Int {
        val exact = partUrls.indexOf(url)
        if (exact >= 0) {
            return exact
        }
        val number = partNumber(url) ?: 1
        return partUrls.indexOfFirst { partNumber(it) == number }
    }

    /** Song title for a part: its own name, or "P<n>" when the uploader left it blank. */
    fun title(partName: String?, number: Int): String =
        partName?.trim().orEmpty().ifBlank { "P$number" }
}
