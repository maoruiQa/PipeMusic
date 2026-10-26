package org.schabi.newpipe.music.stream

import android.content.Context
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import org.schabi.newpipe.DownloaderImpl
import org.schabi.newpipe.R
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.services.bilibili.BilibiliService
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.VideoStream
import org.schabi.newpipe.music.data.db.TrackEntity
import org.schabi.newpipe.player.helper.PlayerDataSource
import org.schabi.newpipe.player.resolver.AudioPlaybackResolver
import org.schabi.newpipe.util.ExtractorHelper
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class ResolvedAudio(
    val mediaSource: MediaSource,
    val durationMs: Long,
    val streamInfo: StreamInfo
)

data class DownloadableStream(
    val url: String,
    val suffix: String,
    val mimeType: String,
    val needsAudioExtraction: Boolean,
    val headers: Map<String, String>,
    val isHls: Boolean = false
)

/**
 * Resolves remote songs into ExoPlayer media sources and into downloadable progressive streams.
 * Playback reuses PipePipe's [AudioPlaybackResolver] so that YouTube DASH/SABR, BiliBili and
 * NicoNico keep all of their special request handling.
 */
class AudioStreamResolver(context: Context) {

    private val appContext = context.applicationContext
    private val strings = org.schabi.newpipe.music.data.MusicLocale.localized(appContext)
    private val dataSource: PlayerDataSource by lazy {
        PlayerDataSource(
            appContext,
            DownloaderImpl.USER_AGENT,
            DefaultBandwidthMeter.Builder(appContext).build()
        )
    }

    fun loadInfo(serviceId: Int, url: String): StreamInfo =
        ExtractorHelper.getNewStreamInfo(serviceId, url)

    fun resolve(track: TrackEntity): ResolvedAudio {
        val url = track.remoteUrl
            ?: throw IllegalStateException(strings.getString(R.string.pm_player_error_no_url))
        val info = loadInfo(track.serviceId, url)
        val source = AudioPlaybackResolver(appContext, dataSource).resolve(info)
            ?: throw IllegalStateException(
                strings.getString(R.string.pm_player_error_no_audio)
            )
        val duration = if (info.duration > 0) info.duration * 1000 else track.durationMs
        return ResolvedAudio(source, duration, info)
    }

    /**
     * Picks a directly downloadable audio stream. Progressive audio is preferred; when a service
     * only exposes a muxed progressive video the worker extracts the audio track with FFmpeg.
     */
    fun pickDownloadStream(info: StreamInfo, quality: String): DownloadableStream? {
        val audioCandidates = info.audioStreams.filter { stream ->
            runCatching {
                stream.isUrl &&
                    stream.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP &&
                    stream.content.startsWith("http")
            }.getOrDefault(false)
        }
        val audio = chooseByQuality(audioCandidates, quality)
        if (audio != null) {
            val (url, headers) = prepareUrl(audio.content, info.serviceId)
            return DownloadableStream(
                url = url,
                suffix = audio.format?.suffix ?: "m4a",
                mimeType = audio.format?.mimeType ?: "audio/mp4",
                needsAudioExtraction = false,
                headers = headers
            )
        }

        val hls = info.audioStreams
            .filter { stream ->
                runCatching {
                    stream.isUrl &&
                        stream.deliveryMethod == DeliveryMethod.HLS &&
                        stream.content.startsWith("http")
                }.getOrDefault(false)
            }
            .maxByOrNull { maxOf(it.bitrate, it.averageBitrate) }
        if (hls != null) {
            val (url, headers) = prepareUrl(hls.content, info.serviceId)
            return DownloadableStream(
                url = url,
                suffix = hls.format?.suffix ?: "m4a",
                mimeType = hls.format?.mimeType ?: "audio/mp4",
                needsAudioExtraction = false,
                headers = headers,
                isHls = true
            )
        }

        // BiliBili exposes a complete fragmented MP4 audio file at the audio stream URL, so a
        // plain download works even though playback uses a generated DASH manifest.
        if (info.serviceId == ServiceList.BiliBili.serviceId) {
            val dashAudio = info.audioStreams
                .filter { runCatching { it.isUrl && it.content.startsWith("http") }
                    .getOrDefault(false) }
                .maxByOrNull { maxOf(it.bitrate, it.averageBitrate) }
            if (dashAudio != null) {
                val (url, headers) = prepareUrl(dashAudio.content, info.serviceId)
                return DownloadableStream(
                    url = url,
                    suffix = "m4a",
                    mimeType = "audio/mp4",
                    needsAudioExtraction = false,
                    headers = headers
                )
            }
        }

        val muxed = info.videoStreams
            .filter { stream ->
                runCatching {
                    !stream.isVideoOnly &&
                        stream.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP &&
                        stream.content.startsWith("http")
                }.getOrDefault(false)
            }
            .maxByOrNull { it.bitrate }
            ?: return null

        val (url, headers) = prepareUrl(muxed.content, info.serviceId)
        val sourceSuffix = muxed.format?.suffix ?: "mp4"
        return DownloadableStream(
            url = url,
            suffix = if (sourceSuffix == "webm") "webm" else "m4a",
            mimeType = if (sourceSuffix == "webm") "audio/webm" else "audio/mp4",
            needsAudioExtraction = true,
            headers = headers
        )
    }

    private fun chooseByQuality(streams: List<AudioStream>, quality: String): AudioStream? {
        if (streams.isEmpty()) {
            return null
        }
        val sorted = streams.sortedByDescending { stream ->
            maxOf(stream.bitrate, stream.averageBitrate)
        }
        return when (quality) {
            "low" -> sorted.last()
            "medium" -> sorted.minByOrNull {
                kotlin.math.abs(maxOf(it.bitrate, it.averageBitrate) - 128_000)
            }
            else -> sorted.first()
        }
    }

    private fun prepareUrl(raw: String, serviceId: Int): Pair<String, Map<String, String>> {
        var url = raw
        var cookie: String? = null
        val marker = "#cookie="
        if (url.contains(marker)) {
            val parts = url.split(marker)
            url = parts[0]
            cookie = runCatching {
                URLDecoder.decode(parts[1].split("&length=")[0], StandardCharsets.UTF_8.name())
            }.getOrNull()
        }
        val headers = LinkedHashMap<String, String>()
        headers["User-Agent"] = DownloaderImpl.USER_AGENT
        headers["Accept-Encoding"] = "identity"
        headers["Range"] = "bytes=0-"
        if (serviceId == ServiceList.BiliBili.serviceId) {
            // BiliBili's CDN validates the same browser-like context that was used to obtain the
            // stream URL. Under IP risk control the URL is also bound to the logged-in session,
            // so the cookie must be sent with the media request itself, not only while parsing.
            headers["Referer"] = BilibiliService.WWW_REFERER
            headers["Origin"] = "https://www.bilibili.com"
            headers["Accept-Language"] = "zh-CN,zh;q=0.9"
            if (ServiceList.BiliBili.hasTokens()) {
                headers["Cookie"] = ServiceList.BiliBili.getTokens()
            }
        }
        if (serviceId == ServiceList.NicoNico.serviceId) {
            val nicoCookie = cookie?.takeIf { it.isNotBlank() }
                ?: ServiceList.NicoNico.getTokens()?.takeIf { it.isNotBlank() }
            if (nicoCookie != null) {
                headers["Cookie"] = nicoCookie
            }
        }
        return url to headers
    }
}
