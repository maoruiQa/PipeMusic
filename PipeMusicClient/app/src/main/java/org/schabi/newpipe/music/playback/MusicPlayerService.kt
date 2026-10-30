package org.schabi.newpipe.music.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.R
import org.schabi.newpipe.music.MusicGraph
import org.schabi.newpipe.music.data.QueueSnapshot
import org.schabi.newpipe.music.data.ThemePresets
import org.schabi.newpipe.music.data.db.TrackEntity
import org.schabi.newpipe.music.stream.AudioStreamResolver
import org.schabi.newpipe.music.ui.MusicMainActivity
import java.io.File
import kotlin.math.max

/**
 * Foreground playback service dedicated to music: it owns one ExoPlayer instance, a media session,
 * the playback queue, repeat/shuffle state and the media notification.
 */
class MusicPlayerService : Service(), Player.Listener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var player: ExoPlayer
    private lateinit var session: MediaSessionCompat
    private lateinit var resolver: AudioStreamResolver
    private lateinit var queue: PlaybackQueue

    private var noisyReceiver: BroadcastReceiver? = null
    private var loadJob: Job? = null
    private var tickerJob: Job? = null
    private var artworkJob: Job? = null
    private var sleepJob: Job? = null

    private var resolving = false
    private var buffering = false
    private var errorMessage: String? = null
    private var sleepEndsAt = 0L
    private var retryCount = 0
    private var artwork: Bitmap? = null

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(
            org.schabi.newpipe.music.data.MusicLocale.localized(base)
        )
    }

    override fun onCreate() {
        super.onCreate()
        MusicGraph.init(this)
        resolver = AudioStreamResolver(this)
        queue = PlaybackQueue()

        createNotificationChannel()

        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            addListener(this@MusicPlayerService)
        }

        session = MediaSessionCompat(this, "PipeMusic").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = resume()
                override fun onPause() = pause()
                override fun onSkipToNext() = skipNext()
                override fun onSkipToPrevious() = skipPrevious()
                override fun onSeekTo(pos: Long) = seek(pos)
                override fun onStop() = stopPlayback()
            })
            setSessionActivity(contentPendingIntent())
            isActive = true
        }

        registerNoisyReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null && queue.isEmpty) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForegroundCompat()
        when (intent?.action) {
            ACTION_PLAY_QUEUE -> {
                val ids = intent.getLongArrayExtra(EXTRA_IDS)?.toList().orEmpty()
                val index = intent.getIntExtra(EXTRA_INDEX, 0)
                val position = intent.getLongExtra(EXTRA_POSITION, 0)
                loadAndPlay(ids, index, position)
            }

            ACTION_TOGGLE -> toggle()
            ACTION_NEXT -> skipNext()
            ACTION_PREVIOUS -> skipPrevious()
            ACTION_SEEK -> seek(intent.getLongExtra(EXTRA_POSITION, 0))
            ACTION_SET_REPEAT -> {
                queue.setRepeatMode(intent.getIntExtra(EXTRA_REPEAT, PlaybackQueue.REPEAT_ALL))
                saveQueue()
                publish()
            }

            ACTION_TOGGLE_SHUFFLE -> {
                queue.setShuffle(!queue.shuffle)
                saveQueue()
                publish()
            }

            ACTION_EXPAND_CURRENT -> {
                val ids = intent.getLongArrayExtra(EXTRA_IDS)?.toList().orEmpty()
                expandCurrent(intent.getLongExtra(EXTRA_ANCHOR_ID, -1), ids)
            }

            ACTION_JUMP -> jumpTo(intent.getIntExtra(EXTRA_INDEX, 0))
            ACTION_REMOVE -> removeAt(intent.getIntExtra(EXTRA_INDEX, 0))
            ACTION_SLEEP_TIMER -> setSleepTimer(intent.getIntExtra(EXTRA_MINUTES, 0))
            ACTION_STOP -> stopPlayback()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        saveQueue()
        tickerJob?.cancel()
        loadJob?.cancel()
        artworkJob?.cancel()
        sleepJob?.cancel()
        noisyReceiver?.let { runCatching { unregisterReceiver(it) } }
        player.removeListener(this)
        player.release()
        session.isActive = false
        session.release()
        scope.cancel()
        super.onDestroy()
    }

    // region queue / playback

    private fun loadAndPlay(ids: List<Long>, index: Int, positionMs: Long) {
        loadJob?.cancel()
        loadJob = scope.launch {
            val tracks = withContext(Dispatchers.IO) { MusicGraph.repository.getTracks(ids) }
            if (tracks.isEmpty()) {
                stopPlayback()
                return@launch
            }
            queue.replaceAll(tracks, index)
            playCurrent(positionMs)
        }
    }

    private fun playCurrent(positionMs: Long, resetRetry: Boolean = true) {
        val track = queue.current ?: return
        loadJob?.cancel()
        if (resetRetry) {
            retryCount = 0
        }
        resolving = false
        buffering = false
        errorMessage = null
        artwork = null
        updateMetadata(track)
        loadArtwork(track)
        publish()
        updateNotification()
        saveQueue()
        startTicker()

        val localFile = MusicGraph.repository.localFile(track)
        if (localFile != null) {
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(localFile)))
            player.prepare()
            if (positionMs > 0) {
                player.seekTo(positionMs)
            }
            player.playWhenReady = true
            recordPlayed(track)
            return
        }

        // Drop the previous track right away: otherwise it keeps playing under the new title and
        // cover until the stream resolves. Clearing the items also resets the reported duration.
        resolving = true
        player.stop()
        player.clearMediaItems()
        publish()
        loadJob = scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { resolver.resolve(track) }
            }
            if (!isActive) {
                return@launch
            }
            resolving = false
            result.onSuccess { resolved ->
                player.setMediaSource(resolved.mediaSource)
                player.prepare()
                if (positionMs > 0) {
                    player.seekTo(positionMs)
                }
                player.playWhenReady = true
                updateMetadata(track)
                publish()
                recordPlayed(track)
            }.onFailure { throwable ->
                errorMessage = friendlyError(throwable)
                buffering = false
                publish()
                updateNotification()
                Log.w(TAG, "Could not resolve audio for track ${track.id}", throwable)
            }
        }
    }

    private fun recordPlayed(track: TrackEntity) {
        scope.launch(Dispatchers.IO) {
            runCatching { MusicGraph.repository.recordPlayed(track) }
        }
    }

    private fun toggle() {
        when {
            player.playbackState == Player.STATE_ENDED -> {
                player.seekTo(0)
                player.play()
            }

            player.isPlaying -> pause()
            else -> resume()
        }
    }

    private fun resume() {
        // While resolving the player is idle on purpose; restarting would throw the load away.
        if (queue.current == null || resolving) {
            return
        }
        when (player.playbackState) {
            Player.STATE_IDLE -> playCurrent(0)
            Player.STATE_ENDED -> {
                player.seekTo(0)
                player.play()
                updateNotification()
            }
            else -> {
                player.play()
                updateNotification()
            }
        }
    }

    private fun pause() {
        player.pause()
        saveQueue()
        publish()
        updateNotification()
    }

    private fun skipNext() {
        val next = queue.next(false)
        if (next == null) {
            player.pause()
            player.seekTo(0)
            publish()
        } else {
            playCurrent(0)
        }
    }

    private fun skipPrevious() {
        if (player.currentPosition > 3_000) {
            player.seekTo(0)
            publish()
            return
        }
        if (queue.previous() == null) {
            player.seekTo(0)
        } else {
            playCurrent(0)
        }
    }

    private fun seek(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0))
        publish()
        updatePlaybackState()
    }

    private fun jumpTo(index: Int) {
        if (queue.jumpTo(index) != null) {
            playCurrent(0)
        }
    }

    /** Queues the other parts of a multi-part video around the song that is already playing. */
    private fun expandCurrent(anchorId: Long, ids: List<Long>) {
        scope.launch {
            val parts = withContext(Dispatchers.IO) { MusicGraph.repository.getTracks(ids) }
            if (queue.expandCurrent(anchorId, parts)) {
                saveQueue()
                publish()
            }
        }
    }

    private fun removeAt(index: Int) {
        val wasCurrent = index == queue.currentIndex
        queue.removeAt(index)
        if (queue.isEmpty) {
            stopPlayback()
        } else if (wasCurrent) {
            playCurrent(0)
        } else {
            publish()
            saveQueue()
        }
    }

    private fun stopPlayback() {
        saveQueue()
        player.stop()
        player.clearMediaItems()
        queue = PlaybackQueue()
        MusicPlayerController.publish(PlayerUiState())
        stopForegroundCompat()
        stopSelf()
    }

    private fun advance(automatic: Boolean) {
        if (automatic && queue.repeatMode == PlaybackQueue.REPEAT_ONE && queue.current != null) {
            player.seekTo(0)
            player.play()
            publish()
            return
        }
        val next = queue.next(automatic)
        if (next == null) {
            player.pause()
            player.seekTo(0)
            publish()
            updateNotification()
        } else {
            playCurrent(0)
        }
    }

    // endregion

    // region player callbacks

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        publish()
        updatePlaybackState()
        updateNotification()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        buffering = playbackState == Player.STATE_BUFFERING
        if (playbackState == Player.STATE_ENDED) {
            advance(true)
            return
        }
        publish()
        updatePlaybackState()
        if (playbackState == Player.STATE_READY) {
            updateNotification()
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        val track = queue.current
        if (track != null && track.remoteUrl != null && retryCount < 1) {
            retryCount += 1
            val position = player.currentPosition.coerceAtLeast(0)
            Log.w(TAG, "Playback failed, retrying with a fresh URL (${error.errorCodeName})")
            playCurrent(position, resetRetry = false)
            return
        }
        errorMessage = friendlyError(error)
        buffering = false
        resolving = false
        publish()
        updateNotification()
        Log.e(TAG, "Playback error ${error.errorCodeName}", error)
    }

    // endregion

    // region state / metadata

    private fun publish() {
        val track = queue.current
        val duration = when {
            player.duration > 0 -> player.duration
            track != null -> track.durationMs
            else -> 0
        }
        val position = if (player.playbackState == Player.STATE_IDLE) {
            0
        } else {
            max(0, player.currentPosition)
        }
        MusicPlayerController.publish(
            PlayerUiState(
                queue = queue.tracks,
                index = queue.currentIndex,
                current = track,
                isPlaying = player.isPlaying,
                isBuffering = buffering || resolving,
                isResolving = resolving,
                positionMs = position,
                durationMs = duration,
                repeatMode = queue.repeatMode,
                shuffle = queue.shuffle,
                sleepTimerEndsAt = sleepEndsAt,
                error = errorMessage
            )
        )
        updatePlaybackState()
    }

    /**
     * Always includes the loaded artwork: rebuilding the metadata without it (e.g. once the stream
     * resolved) left system media controls without a cover or with the previous track's one.
     */
    private fun updateMetadata(track: TrackEntity) {
        session.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.album)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.durationMs)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artwork)
                .build()
        )
    }

    private fun updatePlaybackState() {
        val state = when {
            resolving || buffering -> PlaybackStateCompat.STATE_BUFFERING
            player.isPlaying -> PlaybackStateCompat.STATE_PLAYING
            queue.current != null -> PlaybackStateCompat.STATE_PAUSED
            else -> PlaybackStateCompat.STATE_STOPPED
        }
        val actions = PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
            PlaybackStateCompat.ACTION_SEEK_TO or
            PlaybackStateCompat.ACTION_STOP
        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(
                    state,
                    max(0, player.currentPosition),
                    if (player.isPlaying) 1f else 0f
                )
                .build()
        )
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) {
            return
        }
        tickerJob = scope.launch {
            var ticks = 0
            while (isActive) {
                publishPosition()
                ticks += 1
                if (ticks % 20 == 0) {
                    saveQueue()
                }
                delay(if (player.isPlaying) 500 else 1_000)
            }
        }
    }

    private fun publishPosition() {
        val position = if (player.playbackState == Player.STATE_IDLE) {
            0
        } else {
            max(0, player.currentPosition)
        }
        MusicPlayerController.publishPosition(position)
        updatePlaybackState()
    }

    private fun saveQueue() {
        if (queue.isEmpty) {
            return
        }
        MusicGraph.preferences.saveQueue(
            QueueSnapshot(
                ids = queue.tracks.map { it.id },
                index = queue.currentIndex,
                positionMs = max(0, player.currentPosition),
                repeatMode = queue.repeatMode,
                shuffle = queue.shuffle
            )
        )
    }

    // endregion

    // region sleep timer

    private fun setSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        if (minutes <= 0) {
            sleepEndsAt = 0
            publish()
            return
        }
        sleepEndsAt = System.currentTimeMillis() + minutes * 60_000L
        publish()
        sleepJob = scope.launch {
            delay(minutes * 60_000L)
            pause()
            sleepEndsAt = 0
            publish()
        }
    }

    // endregion

    // region notification

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.pm_playback_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.pm_playback_channel_desc)
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundCompat() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun updateNotification() {
        runCatching {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification())
        }
    }

    private fun buildNotification(): Notification {
        val track = queue.current
        val seed = MusicGraph.preferences.theme.value.let {
            it.customSeed ?: ThemePresets.find(it.presetId).seed
        }
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(track?.title ?: "PipeMusic")
            .setContentText(track?.artist ?: getString(R.string.pm_ready_to_play))
            .setContentIntent(contentPendingIntent())
            .setDeleteIntent(actionPendingIntent(ACTION_STOP, 90))
            .setColor(seed)
            .setColorized(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(player.isPlaying)
            .addAction(
                R.drawable.ic_music_previous,
                getString(R.string.pm_action_previous),
                actionPendingIntent(ACTION_PREVIOUS, 91)
            )
            .addAction(
                if (player.isPlaying) R.drawable.ic_music_pause else R.drawable.ic_music_play,
                getString(if (player.isPlaying) R.string.pm_action_pause else R.string.pm_action_play),
                actionPendingIntent(ACTION_TOGGLE, 92)
            )
            .addAction(
                R.drawable.ic_music_next,
                getString(R.string.pm_action_next),
                actionPendingIntent(ACTION_NEXT, 93)
            )
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(session.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
        artwork?.let { builder.setLargeIcon(it) }
        return builder.build()
    }

    private fun contentPendingIntent(): PendingIntent {
        val intent = Intent(this, MusicMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun actionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, MusicPlayerService::class.java).setAction(action)
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun loadArtwork(track: TrackEntity) {
        artworkJob?.cancel()
        artworkJob = scope.launch {
            val bitmap = withContext(Dispatchers.IO) { loadBitmap(track) } ?: return@launch
            if (queue.current?.id != track.id) {
                return@launch
            }
            artwork = bitmap
            updateMetadata(track)
            updateNotification()
        }
    }

    private fun loadBitmap(track: TrackEntity): Bitmap? {
        val artworkPath = track.artworkUri
        if (artworkPath != null && artworkPath.startsWith("/")) {
            val file = File(artworkPath)
            if (file.exists()) {
                return runCatching {
                    Picasso.get().load(file).resize(320, 320).centerCrop().get()
                }.getOrNull()
            }
        }
        val remote = track.thumbnailUrl ?: artworkPath
        if (remote.isNullOrBlank() || !remote.startsWith("http")) {
            return null
        }
        return runCatching {
            Picasso.get().load(remote).resize(320, 320).centerCrop().get()
        }.getOrNull()
    }

    // endregion

    // region noisy receiver

    private fun registerNoisyReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY && player.isPlaying) {
                    pause()
                }
            }
        }
        noisyReceiver = receiver
        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    // endregion

    private fun friendlyError(throwable: Throwable): String {
        val text = throwable.message.orEmpty()
        return when {
            throwable is org.schabi.newpipe.extractor.exceptions.AntiBotException ||
                text.contains("not a bot", true) ->
                getString(R.string.pm_player_error_antibot)
            throwable is org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException ->
                getString(R.string.pm_player_error_unavailable)
            text.contains("Bilibili", true) ->
                getString(R.string.pm_player_error_bilibili)
            text.contains("403") -> getString(R.string.pm_playback_error_expired)
            text.contains("Unable to resolve host", true) ||
                text.contains("timeout", true) ||
                text.contains("network", true) ->
                getString(R.string.pm_playback_error_network)
            throwable is IllegalStateException && text.isNotBlank() -> text
            else -> getString(R.string.pm_playback_error_generic)
        }
    }

    companion object {
        private const val TAG = "MusicPlayer"
        private const val CHANNEL_ID = "music_playback"
        private const val NOTIFICATION_ID = 4242

        const val ACTION_PLAY_QUEUE = "org.schabi.newpipe.music.PLAY_QUEUE"
        const val ACTION_TOGGLE = "org.schabi.newpipe.music.TOGGLE"
        const val ACTION_NEXT = "org.schabi.newpipe.music.NEXT"
        const val ACTION_PREVIOUS = "org.schabi.newpipe.music.PREVIOUS"
        const val ACTION_SEEK = "org.schabi.newpipe.music.SEEK"
        const val ACTION_SET_REPEAT = "org.schabi.newpipe.music.SET_REPEAT"
        const val ACTION_TOGGLE_SHUFFLE = "org.schabi.newpipe.music.TOGGLE_SHUFFLE"
        const val ACTION_EXPAND_CURRENT = "org.schabi.newpipe.music.EXPAND_CURRENT"
        const val ACTION_JUMP = "org.schabi.newpipe.music.JUMP"
        const val ACTION_REMOVE = "org.schabi.newpipe.music.REMOVE"
        const val ACTION_SLEEP_TIMER = "org.schabi.newpipe.music.SLEEP_TIMER"
        const val ACTION_STOP = "org.schabi.newpipe.music.STOP"

        const val EXTRA_IDS = "ids"
        const val EXTRA_INDEX = "index"
        const val EXTRA_ANCHOR_ID = "anchor_id"
        const val EXTRA_POSITION = "position"
        const val EXTRA_REPEAT = "repeat"
        const val EXTRA_MINUTES = "minutes"
    }
}
