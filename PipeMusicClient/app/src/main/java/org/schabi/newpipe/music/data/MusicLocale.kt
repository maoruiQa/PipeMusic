package org.schabi.newpipe.music.data

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Tiny per-app language override. "system" (the default) lets Android pick the best matching
 * resource folder, so a Chinese system automatically shows the Simplified Chinese UI and every
 * other locale falls back to the English default.
 */
object MusicLocale {

    const val SYSTEM = "system"
    const val ENGLISH = "en"
    const val CHINESE = "zh-CN"
    const val JAPANESE = "ja"
    const val GERMAN = "de"
    const val SPANISH = "es"
    const val FRENCH = "fr"
    const val RUSSIAN = "ru"

    private const val PREFS = "pipemusic_prefs"
    private const val KEY = "ui_language"

    fun currentTag(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, SYSTEM)
            ?: SYSTEM

    /** Returns a context whose resources follow the in-app language selection. */
    fun localized(context: Context): Context = wrap(context, currentTag(context))

    fun wrap(base: Context, tag: String): Context {
        if (tag.isBlank() || tag == SYSTEM) {
            val systemLocale = base.resources.configuration.locales[0]
            if (systemLocale != null) {
                Locale.setDefault(systemLocale)
            }
            return base
        }
        val locale = runCatching { Locale.forLanguageTag(tag) }.getOrNull() ?: return base
        Locale.setDefault(locale)
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return base.createConfigurationContext(configuration)
    }
}
