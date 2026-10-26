package org.schabi.newpipe.music.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import org.schabi.newpipe.music.MusicGraph
import org.schabi.newpipe.music.data.MusicLocale
import org.schabi.newpipe.music.ui.theme.PipeMusicTheme

class MusicMainActivity : ComponentActivity() {

    private val openUrl = MutableStateFlow<String?>(null)
    private val openQuery = MutableStateFlow<String?>(null)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(
            MusicLocale.wrap(newBase, MusicLocale.currentTag(newBase))
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MusicGraph.init(this)
        openUrl.value = intent?.getStringExtra(EXTRA_OPEN_URL)
        openQuery.value = intent?.getStringExtra(EXTRA_SEARCH_QUERY)
        requestNotificationPermission()

        setContent {
            val config by MusicGraph.preferences.theme.collectAsStateWithLifecycle()
            PipeMusicTheme(config) {
                MusicApp(
                    openUrl = openUrl,
                    onConsumedUrl = { openUrl.value = null },
                    openQuery = openQuery,
                    onConsumedQuery = { openQuery.value = null },
                    onLanguageChange = { tag ->
                        MusicGraph.preferences.setLanguage(tag)
                        recreate()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openUrl.value = intent.getStringExtra(EXTRA_OPEN_URL)
        openQuery.value = intent.getStringExtra(EXTRA_SEARCH_QUERY)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATIONS
            )
        }
    }

    companion object {
        const val EXTRA_OPEN_URL = "music_open_url"
        const val EXTRA_SEARCH_QUERY = "music_search_query"
        private const val REQUEST_NOTIFICATIONS = 701
    }
}
