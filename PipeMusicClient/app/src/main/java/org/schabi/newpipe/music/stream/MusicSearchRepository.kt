package org.schabi.newpipe.music.stream

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.services.bilibili.BilibiliService
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.util.ExtractorHelper

data class MusicService(
    val id: Int,
    val name: String,
    val shortName: String
)

data class SearchPage(
    val items: List<StreamInfoItem>,
    val nextPage: Page?
)

/** The parts of one multi-part video and the video's own title. */
data class VideoParts(
    val title: String,
    val items: List<StreamInfoItem>
)

data class PlaylistPage(
    val name: String,
    val uploaderName: String,
    val items: List<StreamInfoItem>,
    val nextPage: Page?
)

/**
 * Thin coroutine wrapper around PipePipe's extractor search APIs.
 */
class MusicSearchRepository(context: Context) {

    private val appContext = context.applicationContext
    private val strings = org.schabi.newpipe.music.data.MusicLocale.localized(appContext)

    val services: List<MusicService> = runCatching {
        NewPipe.getServices()
            .map { service ->
                val name = service.serviceInfo.name
                MusicService(
                    id = service.serviceId,
                    name = name,
                    shortName = shortName(name)
                )
            }
            .sortedBy { service ->
                when (service.shortName) {
                    "YouTube" -> 0
                    "SoundCloud" -> 1
                    "BiliBili" -> 2
                    else -> 10
                }
            }
    }.getOrDefault(emptyList())

    fun serviceName(serviceId: Int): String =
        services.firstOrNull { it.id == serviceId }?.shortName
            ?: strings.getString(org.schabi.newpipe.R.string.pm_service_online)

    suspend fun search(serviceId: Int, query: String, page: Page? = null): SearchPage =
        withContext(Dispatchers.IO) {
            val trimmed = query.trim()
            require(trimmed.isNotEmpty()) {
                strings.getString(org.schabi.newpipe.R.string.pm_error_empty_query)
            }
            val contentFilter = defaultContentFilter(serviceId)
            val sortFilter = emptyList<org.schabi.newpipe.extractor.search.filter.FilterItem>()
            if (page == null) {
                val info = ExtractorHelper.searchFor(
                    serviceId,
                    trimmed,
                    contentFilter,
                    sortFilter
                ).blockingGet()
                SearchPage(
                    items = info.relatedItems.filterIsInstance<StreamInfoItem>(),
                    nextPage = info.nextPage
                )
            } else {
                val more = ExtractorHelper.getMoreSearchItems(
                    serviceId,
                    trimmed,
                    contentFilter,
                    sortFilter,
                    page
                ).blockingGet()
                SearchPage(
                    items = more.items.filterIsInstance<StreamInfoItem>(),
                    nextPage = more.nextPage
                )
            }
        }

    /**
     * Search extractors require the platform's default content filter ("all"). Passing an empty
     * content filter list makes YouTube's filter evaluator throw "we have a problem here".
     */
    private fun defaultContentFilter(
        serviceId: Int
    ): List<org.schabi.newpipe.extractor.search.filter.FilterItem> {
        return runCatching {
            val service = NewPipe.getService(serviceId)
            listOfNotNull(service.searchQHFactory.getFilterItem(0))
        }.getOrElse {
            Log.w(TAG, "No default search filter for service $serviceId", it)
            emptyList()
        }
    }

    /** Only BiliBili splits one upload into several parts. */
    fun supportsParts(serviceId: Int): Boolean = serviceId == ServiceList.BiliBili.serviceId

    /**
     * Number of parts of the video at [url], from BiliBili's light part list; null when it
     * cannot be told. Search results do not say whether a video has several parts.
     */
    suspend fun partCount(serviceId: Int, url: String): Int? =
        withContext(Dispatchers.IO) {
            val bvid = BVID.find(url)?.value
            if (!supportsParts(serviceId) || bvid == null) {
                return@withContext null
            }
            try {
                val body = NewPipe.getDownloader()
                    .get(BilibiliService.GET_PARTITION_URL + bvid, BilibiliService.getHeaders(url))
                    .responseBody()
                JSONObject(body).optJSONArray("data")?.length()
            } catch (error: Exception) {
                Log.w(TAG, "Part count lookup failed for $serviceId: ${error.javaClass.simpleName}")
                null
            }
        }

    /**
     * Every part of the multi-part video at [url], in upload order. No parts when the video has
     * a single part or the service has no parts at all.
     */
    suspend fun loadParts(serviceId: Int, url: String): VideoParts =
        withContext(Dispatchers.IO) {
            if (!supportsParts(serviceId)) {
                return@withContext VideoParts("", emptyList())
            }
            val info = ExtractorHelper.getStreamInfo(serviceId, url, false).blockingGet()
            val items = info.partitions.orEmpty().takeIf { it.size > 1 } ?: emptyList()
            VideoParts(
                title = StreamParts.videoTitle(info.name.orEmpty(), items.map { it.name }),
                items = items
            )
        }

    suspend fun linkType(serviceId: Int, url: String): StreamingService.LinkType =
        withContext(Dispatchers.IO) {
            runCatching { NewPipe.getService(serviceId).getLinkTypeByUrl(url.trim()) }
                .getOrDefault(StreamingService.LinkType.NONE)
        }

    /** First page of a playlist or album, with the list's own name and uploader. */
    suspend fun loadPlaylist(serviceId: Int, url: String): PlaylistPage =
        withContext(Dispatchers.IO) {
            val info = ExtractorHelper.getPlaylistInfo(serviceId, url, false).blockingGet()
            PlaylistPage(
                name = info.name.orEmpty(),
                uploaderName = info.uploaderName.orEmpty(),
                items = info.relatedItems.orEmpty(),
                nextPage = info.nextPage?.takeIf { Page.isValid(it) }
            )
        }

    suspend fun loadMorePlaylist(serviceId: Int, url: String, page: Page): SearchPage =
        withContext(Dispatchers.IO) {
            val more = ExtractorHelper.getMorePlaylistItems(serviceId, url, page).blockingGet()
            SearchPage(
                items = more.items.orEmpty(),
                nextPage = more.nextPage?.takeIf { Page.isValid(it) }
            )
        }

    suspend fun serviceIdForUrl(url: String): Int? = withContext(Dispatchers.IO) {
        runCatching { NewPipe.getServiceByUrl(url.trim()).serviceId }.getOrNull()
    }

    private companion object {
        const val TAG = "MusicSearch"
        val BVID = Regex("BV[0-9A-Za-z]{10}")
    }

    private fun shortName(name: String): String = when {
        name.contains("YouTube", ignoreCase = true) -> "YouTube"
        name.contains("SoundCloud", ignoreCase = true) -> "SoundCloud"
        name.contains("Bandcamp", ignoreCase = true) -> "Bandcamp"
        name.contains("Bili", ignoreCase = true) -> "BiliBili"
        name.contains("Nico", ignoreCase = true) -> "Niconico"
        name.contains("PeerTube", ignoreCase = true) -> "PeerTube"
        name.contains("media.ccc", ignoreCase = true) -> "media.ccc.de"
        else -> name
    }
}
