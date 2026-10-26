package org.schabi.newpipe.music.stream

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.Page
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

    suspend fun serviceIdForUrl(url: String): Int? = withContext(Dispatchers.IO) {
        runCatching { NewPipe.getServiceByUrl(url.trim()).serviceId }.getOrNull()
    }

    private companion object {
        const val TAG = "MusicSearch"
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
