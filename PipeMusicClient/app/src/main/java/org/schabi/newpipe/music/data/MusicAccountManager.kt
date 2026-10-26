package org.schabi.newpipe.music.data

import android.content.Context
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.schabi.newpipe.App
import org.schabi.newpipe.R
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import org.schabi.newpipe.util.InfoCache
import org.schabi.newpipe.util.ServiceHelper
import org.schabi.newpipe.views.BiliBiliLoginWebViewActivity
import org.schabi.newpipe.views.NicoNicoLoginWebViewActivity
import org.schabi.newpipe.views.YouTubeLoginWebViewActivity
import org.schabi.newpipe.youtube.LocalDomPoTokenProvider

enum class MusicAccountService(
    val serviceId: Int,
    val label: String,
    val cookiesKeyRes: Int,
    val loginActivity: Class<*>,
    val potKeyRes: Int? = null
) {
    YOUTUBE(
        0,
        "YouTube",
        R.string.youtube_cookies_key,
        YouTubeLoginWebViewActivity::class.java,
        R.string.youtube_po_token_key
    ),
    BILIBILI(
        5,
        "BiliBili",
        R.string.bilibili_cookies_key,
        BiliBiliLoginWebViewActivity::class.java
    ),
    NICONICO(
        6,
        "NicoNico",
        R.string.niconico_cookies_key,
        NicoNicoLoginWebViewActivity::class.java
    );

    companion object {
        fun fromServiceId(serviceId: Int): MusicAccountService? =
            entries.firstOrNull { it.serviceId == serviceId }
    }
}

data class MusicAccountState(
    val youtube: Boolean = false,
    val bilibili: Boolean = false,
    val niconico: Boolean = false
) {
    fun isLoggedIn(service: MusicAccountService): Boolean = when (service) {
        MusicAccountService.YOUTUBE -> youtube
        MusicAccountService.BILIBILI -> bilibili
        MusicAccountService.NICONICO -> niconico
    }
}

/**
 * Stores the cookies returned by the existing PipePipe login WebViews in the same preference
 * keys that the extractor already reads. After a login/logout the services and the info cache are
 * reinitialised immediately so the next search or download uses the new credentials.
 */
class MusicAccountManager(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)
    private val _state = MutableStateFlow(readState())
    val state: StateFlow<MusicAccountState> = _state.asStateFlow()

    fun isLoggedIn(service: MusicAccountService): Boolean = _state.value.isLoggedIn(service)

    fun hasStoredCookie(service: MusicAccountService): Boolean =
        !prefs.getString(appContext.getString(service.cookiesKeyRes), null).isNullOrBlank()

    /**
     * Validates and persists the login result. Returns false when the WebView did not return a
     * usable cookie (for example when the user closed it before finishing).
     */
    fun saveLogin(
        service: MusicAccountService,
        cookies: String?,
        poToken: String? = null
    ): Boolean {
        if (cookies.isNullOrBlank()) {
            return false
        }
        val cleanedCookies = cookies.trim().let {
            if (it.startsWith("cookie:", ignoreCase = true)) {
                it.substringAfter(':').trim()
            } else {
                it
            }
        }
        if (cleanedCookies.isEmpty()) {
            return false
        }
        val valid = when (service) {
            MusicAccountService.YOUTUBE -> runCatching {
                YoutubeParsingHelper.getAuthorizationHeader(cleanedCookies)
            }.isSuccess
            MusicAccountService.BILIBILI -> hasNamedCookie(cleanedCookies, "SESSDATA")
            MusicAccountService.NICONICO -> hasNamedCookie(cleanedCookies, "user_session")
        }
        if (!valid) {
            return false
        }
        val editor = prefs.edit().putString(
            appContext.getString(service.cookiesKeyRes),
            cleanedCookies
        )
        if (service.potKeyRes != null) {
            editor.putString(appContext.getString(service.potKeyRes), poToken.orEmpty())
        }
        if (service == MusicAccountService.BILIBILI) {
            // The extractor gates logged-in stream requests behind these flags. Without them a
            // stored cookie would be ignored, which would defeat the point of logging in.
            editor.putStringSet(
                appContext.getString(R.string.cookie_functions_bilibili_key),
                setOf("high_res", "ai_subtitle")
            )
        }
        editor.apply()
        refreshServices(service)
        _state.value = readState()
        return true
    }

    fun logout(service: MusicAccountService) {
        val editor = prefs.edit().putString(
            appContext.getString(service.cookiesKeyRes),
            ""
        )
        if (service.potKeyRes != null) {
            editor.putString(appContext.getString(service.potKeyRes), "")
        }
        editor.apply()
        refreshServices(service)
        _state.value = readState()
    }

    private fun refreshServices(service: MusicAccountService) {
        if (service == MusicAccountService.YOUTUBE) {
            LocalDomPoTokenProvider.invalidate()
            App.reconcileYoutubePlayerClient(appContext)
        }
        ServiceHelper.initServices(appContext)
        InfoCache.getInstance().clearCache()
    }

    private fun hasNamedCookie(cookies: String, name: String): Boolean =
        Regex("(?:^|;\\s*)${Regex.escape(name)}=").containsMatchIn(cookies)

    private fun readState(): MusicAccountState = MusicAccountState(
        youtube = hasStoredCookie(MusicAccountService.YOUTUBE),
        bilibili = hasStoredCookie(MusicAccountService.BILIBILI),
        niconico = hasStoredCookie(MusicAccountService.NICONICO)
    )
}
