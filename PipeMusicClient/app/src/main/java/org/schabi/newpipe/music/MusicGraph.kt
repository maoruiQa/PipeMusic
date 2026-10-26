package org.schabi.newpipe.music

import android.content.Context
import org.schabi.newpipe.music.data.MusicAccountManager
import org.schabi.newpipe.music.data.MusicPreferences
import org.schabi.newpipe.music.data.MusicRepository
import org.schabi.newpipe.music.data.db.MusicDatabase
import org.schabi.newpipe.music.stream.MusicSearchRepository

/**
 * Small manual dependency container. The app deliberately avoids a DI framework so that the
 * music layer stays easy to follow and boots quickly.
 */
object MusicGraph {

    @Volatile
    private var initialized = false

    lateinit var database: MusicDatabase
        private set
    lateinit var repository: MusicRepository
        private set
    lateinit var preferences: MusicPreferences
        private set
    lateinit var search: MusicSearchRepository
        private set
    lateinit var accounts: MusicAccountManager
        private set

    fun init(context: Context) {
        if (initialized) {
            return
        }
        synchronized(this) {
            if (initialized) {
                return
            }
            val appContext = context.applicationContext
            database = MusicDatabase.getInstance(appContext)
            repository = MusicRepository(appContext, database)
            preferences = MusicPreferences(appContext)
            search = MusicSearchRepository(appContext)
            accounts = MusicAccountManager(appContext)
            initialized = true
        }
    }

    val isReady: Boolean get() = initialized
}
