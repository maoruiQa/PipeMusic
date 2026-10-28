package org.schabi.newpipe.music.update

import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Looks up the latest published GitHub release of PipeMusic. Pre-releases and drafts are excluded
 * by the `releases/latest` endpoint itself.
 */
object UpdateChecker {

    private const val TAG = "MusicUpdate"
    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/maoruiQa/PipeMusic/releases/latest"
    const val RELEASES_PAGE_URL = "https://github.com/maoruiQa/PipeMusic/releases"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /** Returns the latest release, or throws [IOException] when GitHub cannot be reached. */
    suspend fun latestRelease(): ReleaseInfo = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(LATEST_RELEASE_URL)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "Release lookup failed with HTTP ${response.code}")
                throw IOException("HTTP ${response.code}")
            }
            parse(response.body.string())
        }
    }

    private fun parse(body: String): ReleaseInfo {
        val json = JSONObject(body)
        val assetsJson = json.optJSONArray("assets")
        val assets = ArrayList<ReleaseAsset>()
        if (assetsJson != null) {
            for (i in 0 until assetsJson.length()) {
                val asset = assetsJson.optJSONObject(i) ?: continue
                val name = asset.optString("name")
                val url = asset.optString("browser_download_url")
                if (name.isNotBlank() && url.startsWith("https://")) {
                    assets.add(ReleaseAsset(name, url))
                }
            }
        }
        val tag = json.optString("tag_name")
        if (tag.isBlank()) {
            throw IOException("Release without tag")
        }
        return ReleaseInfo(
            version = AppVersion.normalize(tag),
            notes = json.optString("body").trim(),
            pageUrl = json.optString("html_url").takeIf { it.startsWith("https://") }
                ?: RELEASES_PAGE_URL,
            downloadUrl = AppVersion.pickApk(assets, Build.SUPPORTED_ABIS.toList())?.url
        )
    }
}
