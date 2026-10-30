package org.schabi.newpipe.music.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import org.schabi.newpipe.R

data class ThemeConfig(
    val presetId: String = "ocean",
    val customSeed: Int? = null,
    /** 0 = follow system, 1 = light, 2 = dark. */
    val darkMode: Int = 0,
    val amoled: Boolean = false,
    val dynamicColor: Boolean = false,
    val artworkTint: Boolean = true
)

data class ThemePreset(
    val id: String,
    val labelRes: Int,
    val seed: Int,
    val gradientStart: Int,
    val gradientEnd: Int
)

object ThemePresets {
    val all = listOf(
        ThemePreset("aurora", R.string.pm_preset_aurora, 0xFF7C5CFF.toInt(), 0xFF7C5CFF.toInt(), 0xFF4DA3FF.toInt()),
        ThemePreset("sunset", R.string.pm_preset_sunset, 0xFFFF7A6B.toInt(), 0xFFFF7A6B.toInt(), 0xFFFFC857.toInt()),
        ThemePreset("forest", R.string.pm_preset_forest, 0xFF3DDC97.toInt(), 0xFF3DDC97.toInt(), 0xFF4DA3FF.toInt()),
        ThemePreset("ocean", R.string.pm_preset_ocean, 0xFF4DA3FF.toInt(), 0xFF4DA3FF.toInt(), 0xFF3DDC97.toInt()),
        ThemePreset("lavender", R.string.pm_preset_lavender, 0xFFB388FF.toInt(), 0xFFB388FF.toInt(), 0xFFFF8ACB.toInt()),
        ThemePreset("rose", R.string.pm_preset_rose, 0xFFFF6FA5.toInt(), 0xFFFF6FA5.toInt(), 0xFFFF9A8B.toInt()),
        ThemePreset("amber", R.string.pm_preset_amber, 0xFFFFC857.toInt(), 0xFFFFC857.toInt(), 0xFFFF7A6B.toInt()),
        ThemePreset("graphite", R.string.pm_preset_graphite, 0xFF8A94A6.toInt(), 0xFF8A94A6.toInt(), 0xFFB0BACB.toInt())
    )

    fun find(id: String): ThemePreset = all.firstOrNull { it.id == id } ?: all.first()
}

data class QueueSnapshot(
    val ids: List<Long>,
    val index: Int,
    val positionMs: Long,
    val repeatMode: Int,
    val shuffle: Boolean
)

class MusicPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("pipemusic_prefs", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(readTheme())
    val theme: StateFlow<ThemeConfig> = _theme.asStateFlow()

    private val _downloadQuality = MutableStateFlow(prefs.getString(KEY_QUALITY, "high") ?: "high")
    val downloadQuality: StateFlow<String> = _downloadQuality.asStateFlow()

    private val _preferredService = MutableStateFlow(prefs.getInt(KEY_SERVICE, 0))
    val preferredService: StateFlow<Int> = _preferredService.asStateFlow()

    private val _language = MutableStateFlow(
        prefs.getString(KEY_LANGUAGE, MusicLocale.SYSTEM) ?: MusicLocale.SYSTEM
    )
    val language: StateFlow<String> = _language.asStateFlow()

    private fun readTheme(): ThemeConfig = ThemeConfig(
        presetId = prefs.getString(KEY_PRESET, "ocean") ?: "ocean",
        customSeed = if (prefs.getBoolean(KEY_HAS_CUSTOM_SEED, false)) {
            prefs.getInt(KEY_CUSTOM_SEED, 0)
        } else {
            null
        },
        darkMode = prefs.getInt(KEY_DARK_MODE, 0),
        amoled = prefs.getBoolean(KEY_AMOLED, false),
        dynamicColor = prefs.getBoolean(KEY_DYNAMIC, false),
        artworkTint = prefs.getBoolean(KEY_ARTWORK_TINT, true)
    )

    fun updateTheme(transform: (ThemeConfig) -> ThemeConfig) {
        val updated = transform(_theme.value)
        prefs.edit()
            .putString(KEY_PRESET, updated.presetId)
            .putInt(KEY_CUSTOM_SEED, updated.customSeed ?: 0)
            .putBoolean(KEY_HAS_CUSTOM_SEED, updated.customSeed != null)
            .putInt(KEY_DARK_MODE, updated.darkMode)
            .putBoolean(KEY_AMOLED, updated.amoled)
            .putBoolean(KEY_DYNAMIC, updated.dynamicColor)
            .putBoolean(KEY_ARTWORK_TINT, updated.artworkTint)
            .apply()
        _theme.value = updated
    }

    fun setCustomSeed(seed: Int?) {
        updateTheme { it.copy(customSeed = seed) }
    }

    fun setDownloadQuality(quality: String) {
        prefs.edit().putString(KEY_QUALITY, quality).apply()
        _downloadQuality.value = quality
    }

    fun setPreferredService(serviceId: Int) {
        prefs.edit().putInt(KEY_SERVICE, serviceId).apply()
        _preferredService.value = serviceId
    }

    fun setLanguage(tag: String) {
        prefs.edit().putString(KEY_LANGUAGE, tag).apply()
        _language.value = tag
    }

    private val _autoUpdateCheck = MutableStateFlow(prefs.getBoolean(KEY_AUTO_UPDATE, true))
    val autoUpdateCheck: StateFlow<Boolean> = _autoUpdateCheck.asStateFlow()

    fun setAutoUpdateCheck(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_UPDATE, enabled).apply()
        _autoUpdateCheck.value = enabled
    }

    private val _groupParts = MutableStateFlow(prefs.getBoolean(KEY_GROUP_PARTS, true))

    /** Whether the parts of a multi-part video show as one entry in the library and playlists. */
    val groupParts: StateFlow<Boolean> = _groupParts.asStateFlow()

    fun setGroupParts(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GROUP_PARTS, enabled).apply()
        _groupParts.value = enabled
    }

    var lastUpdateCheck: Long
        get() = prefs.getLong(KEY_LAST_UPDATE_CHECK, 0)
        set(value) = prefs.edit().putLong(KEY_LAST_UPDATE_CHECK, value).apply()

    /** Release version the user chose to skip; automatic checks stay quiet about it. */
    var skippedUpdateVersion: String?
        get() = prefs.getString(KEY_SKIPPED_UPDATE, null)
        set(value) = prefs.edit().putString(KEY_SKIPPED_UPDATE, value).apply()

    fun saveQueue(snapshot: QueueSnapshot) {
        val json = JSONObject().apply {
            put("ids", JSONArray(snapshot.ids))
            put("index", snapshot.index)
            put("position", snapshot.positionMs)
            put("repeat", snapshot.repeatMode)
            put("shuffle", snapshot.shuffle)
        }
        prefs.edit().putString(KEY_QUEUE, json.toString()).apply()
    }

    fun loadQueue(): QueueSnapshot? {
        val raw = prefs.getString(KEY_QUEUE, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            val array = json.optJSONArray("ids") ?: JSONArray()
            val ids = ArrayList<Long>(array.length())
            for (i in 0 until array.length()) {
                ids.add(array.optLong(i))
            }
            QueueSnapshot(
                ids = ids,
                index = json.optInt("index", 0),
                positionMs = json.optLong("position", 0),
                repeatMode = json.optInt("repeat", 2),
                shuffle = json.optBoolean("shuffle", false)
            )
        }.getOrNull()
    }

    fun clearQueue() {
        prefs.edit().remove(KEY_QUEUE).apply()
    }

    private companion object {
        const val KEY_PRESET = "theme_preset"
        const val KEY_CUSTOM_SEED = "theme_custom_seed"
        const val KEY_HAS_CUSTOM_SEED = "theme_has_custom_seed"
        const val KEY_DARK_MODE = "theme_dark_mode"
        const val KEY_AMOLED = "theme_amoled"
        const val KEY_DYNAMIC = "theme_dynamic"
        const val KEY_ARTWORK_TINT = "theme_artwork_tint"
        const val KEY_QUALITY = "download_quality"
        const val KEY_SERVICE = "preferred_service"
        const val KEY_LANGUAGE = "ui_language"
        const val KEY_QUEUE = "last_queue"
        const val KEY_AUTO_UPDATE = "auto_update_check"
        const val KEY_LAST_UPDATE_CHECK = "last_update_check"
        const val KEY_SKIPPED_UPDATE = "skipped_update_version"
        const val KEY_GROUP_PARTS = "group_parts"
    }
}
