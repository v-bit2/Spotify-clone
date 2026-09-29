package com.example.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import com.example.MainActivity
import com.example.SpotificApplication
import com.example.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class AudioPlaybackService : Service(), MediaPlayer.OnPreparedListener,
    MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private val binder = LocalBinder()
    private var mediaPlayer: MediaPlayer? = null
    private var mediaSession: MediaSession? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())

    private var currentAlbumArtBitmap: Bitmap? = null
    private var isServiceForeground = false
    @Volatile
    private var isPlayerPrepared = false

    inner class LocalBinder : Binder() {
        fun getService(): AudioPlaybackService = this@AudioPlaybackService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AudioPlaybackService created")
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        setupMediaSession()
        setupMediaPlayer()
        startPositionUpdater()
    }

    private fun setupMediaSession() {
        mediaSession = MediaSession(this, "SpotificMediaSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    play()
                }

                override fun onPause() {
                    pause()
                }

                override fun onSkipToNext() {
                    skipNext()
                }

                override fun onSkipToPrevious() {
                    skipPrevious()
                }

                override fun onSeekTo(pos: Long) {
                    seekTo(pos)
                }

                override fun onStop() {
                    stop()
                }
            })
            isActive = true
        }
    }

    private fun setupMediaPlayer() {
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignored
        }
        isPlayerPrepared = false
        mediaPlayer = MediaPlayer().apply {
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnPreparedListener(this@AudioPlaybackService)
            setOnCompletionListener(this@AudioPlaybackService)
            setOnErrorListener(this@AudioPlaybackService)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> play()
            ACTION_PAUSE -> pause()
            ACTION_TOGGLE_PLAY_PAUSE -> togglePlayPause()
            ACTION_NEXT -> skipNext()
            ACTION_PREV -> skipPrevious()
            ACTION_STOP -> stop()
            ACTION_SEEK -> {
                val position = intent.getLongExtra(EXTRA_POSITION, 0L)
                seekTo(position)
            }
        }
        return START_STICKY
    }

    fun playTrack(track: Track, queue: List<Track> = emptyList()) {
        serviceScope.launch {
            val fullQueue = if (queue.isNotEmpty()) queue else listOf(track)
            val index = fullQueue.indexOfFirst { it.id == track.id }.let { if (it >= 0) it else 0 }

            PlaybackStateManager.updateState {
                it.copy(
                    currentTrack = track,
                    queue = fullQueue,
                    currentIndex = index,
                    isBuffering = true,
                    isPlaying = false,
                    currentPositionMs = 0L,
                    durationMs = 0L,
                    errorMessage = null
                )
            }

            // Immediately show notification so Android knows foreground service is starting
            startOrUpdateForegroundNotification()

            // Resolve playable URL using SpotificRepository (primary with retries + query fallback)
            val app = application as? SpotificApplication
            val resolvedUrl = if (!track.streamUrl.isNullOrEmpty()) {
                track.streamUrl
            } else if (!track.localFilePath.isNullOrEmpty()) {
                track.localFilePath
            } else {
                app?.repository?.resolveStreamUrl(track)
            }

            if (resolvedUrl.isNullOrEmpty()) {
                Log.e(TAG, "Failed to resolve playable URL for track: ${track.title}")
                PlaybackStateManager.updateState {
                    it.copy(
                        isBuffering = false,
                        isPlaying = false,
                        errorMessage = "Stream unavailable. Please try another track."
                    )
                }
                return@launch
            }

            // Prepare and play audio
            try {
                isPlayerPrepared = false
                try {
                    mediaPlayer?.reset()
                } catch (e: Exception) {
                    setupMediaPlayer()
                }
                mediaPlayer?.setDataSource(resolvedUrl)
                mediaPlayer?.prepareAsync()

                // Load artwork for notification and metadata in background
                loadArtwork(track.thumbnail)
            } catch (e: Exception) {
                Log.e(TAG, "Error setting data source: ${e.message}", e)
                isPlayerPrepared = false
                PlaybackStateManager.updateState {
                    it.copy(
                        isBuffering = false,
                        isPlaying = false,
                        errorMessage = "Playback failed: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private fun loadArtwork(urlStr: String?) {
        serviceScope.launch(Dispatchers.IO) {
            if (urlStr.isNullOrEmpty()) {
                currentAlbumArtBitmap = null
                updateMediaSessionMetadata()
                return@launch
            }
            try {
                val url = URL(urlStr)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.connect()
                val input = connection.inputStream
                currentAlbumArtBitmap = BitmapFactory.decodeStream(input)
                input.close()
                withContext(Dispatchers.Main) {
                    updateMediaSessionMetadata()
                    startOrUpdateForegroundNotification()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load album art: ${e.message}")
            }
        }
    }

    override fun onPrepared(mp: MediaPlayer?) {
        isPlayerPrepared = true
        if (!requestAudioFocus()) {
            Log.w(TAG, "Audio focus not granted")
            return
        }

        try {
            mp?.start()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start mediaPlayer: ${e.message}")
            return
        }

        val duration = try {
            mp?.duration?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        }

        PlaybackStateManager.updateState {
            val trackWithDuration = if (it.currentTrack?.duration == null || it.currentTrack.duration == "--:--") {
                it.currentTrack?.copy(duration = formatTime(duration))
            } else {
                it.currentTrack
            }
            it.copy(
                currentTrack = trackWithDuration,
                isPlaying = true,
                isBuffering = false,
                durationMs = duration
            )
        }

        updateMediaSessionPlaybackState(PlaybackState.STATE_PLAYING, 0L)
        updateMediaSessionMetadata()
        startOrUpdateForegroundNotification()
    }

    fun play() {
        if (!isPlayerPrepared) return
        if (requestAudioFocus()) {
            try {
                mediaPlayer?.start()
                val pos = try { mediaPlayer?.currentPosition?.toLong() ?: 0L } catch (e: Exception) { 0L }
                PlaybackStateManager.updateState { it.copy(isPlaying = true) }
                updateMediaSessionPlaybackState(PlaybackState.STATE_PLAYING, pos)
                startOrUpdateForegroundNotification()
            } catch (e: Exception) {
                Log.e(TAG, "Error in play: ${e.message}")
            }
        }
    }

    fun pause() {
        if (!isPlayerPrepared) return
        try {
            mediaPlayer?.pause()
            val pos = try { mediaPlayer?.currentPosition?.toLong() ?: 0L } catch (e: Exception) { 0L }
            PlaybackStateManager.updateState { it.copy(isPlaying = false) }
            updateMediaSessionPlaybackState(PlaybackState.STATE_PAUSED, pos)
            startOrUpdateForegroundNotification()
        } catch (e: Exception) {
            Log.e(TAG, "Error in pause: ${e.message}")
        }
    }

    fun togglePlayPause() {
        if (!isPlayerPrepared) return
        try {
            if (mediaPlayer?.isPlaying == true) {
                pause()
            } else {
                play()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in togglePlayPause: ${e.message}")
        }
    }

    fun seekTo(positionMs: Long) {
        if (!isPlayerPrepared) return
        try {
            mediaPlayer?.seekTo(positionMs.toInt())
            val isPlaying = try { mediaPlayer?.isPlaying == true } catch (e: Exception) { false }
            PlaybackStateManager.updateState { it.copy(currentPositionMs = positionMs) }
            val state = if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
            updateMediaSessionPlaybackState(state, positionMs)
        } catch (e: Exception) {
            Log.e(TAG, "Error in seekTo: ${e.message}")
        }
    }

    fun skipNext() {
        val state = PlaybackStateManager.getCurrentState()
        if (state.queue.isNotEmpty()) {
            val nextIndex = if (state.isShuffle) {
                (0 until state.queue.size).filter { it != state.currentIndex }.randomOrNull() ?: 0
            } else {
                (state.currentIndex + 1) % state.queue.size
            }
            playTrack(state.queue[nextIndex], state.queue)
        }
    }

    fun skipPrevious() {
        val state = PlaybackStateManager.getCurrentState()
        if (state.queue.isNotEmpty()) {
            // If current position > 3 seconds, replay track
            val currentPos = if (isPlayerPrepared) {
                try { mediaPlayer?.currentPosition ?: 0 } catch (e: Exception) { 0 }
            } else 0
            if (currentPos > 3000) {
                seekTo(0)
                return
            }
            val prevIndex = if (state.currentIndex > 0) state.currentIndex - 1 else state.queue.size - 1
            playTrack(state.queue[prevIndex], state.queue)
        }
    }

    fun stop() {
        isPlayerPrepared = false
        try {
            mediaPlayer?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping mediaPlayer: ${e.message}")
        }
        PlaybackStateManager.updateState {
            it.copy(isPlaying = false, currentPositionMs = 0L)
        }
        updateMediaSessionPlaybackState(PlaybackState.STATE_STOPPED, 0L)
        abandonAudioFocus()
        stopForeground(STOP_FOREGROUND_REMOVE)
        isServiceForeground = false
        stopSelf()
    }

    override fun onCompletion(mp: MediaPlayer?) {
        val state = PlaybackStateManager.getCurrentState()
        if (state.isRepeat) {
            state.currentTrack?.let { playTrack(it, state.queue) }
        } else {
            skipNext()
        }
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
        isPlayerPrepared = false
        if (what == -38) {
            Log.w(TAG, "Ignoring non-fatal MediaPlayer state error -38")
            return true
        }
        PlaybackStateManager.updateState {
            it.copy(
                isBuffering = false,
                isPlaying = false,
                errorMessage = "Playback stream error (Code: $what)"
            )
        }
        return true
    }

    private fun startPositionUpdater() {
        handler.post(object : Runnable {
            override fun run() {
                try {
                    if (isPlayerPrepared && mediaPlayer?.isPlaying == true) {
                        val pos = try { mediaPlayer?.currentPosition?.toLong() ?: 0L } catch (e: Exception) { 0L }
                        val dur = try { mediaPlayer?.duration?.toLong() ?: 0L } catch (e: Exception) { 0L }
                        PlaybackStateManager.updateState {
                            it.copy(
                                currentPositionMs = pos,
                                durationMs = if (dur > 0) dur else it.durationMs
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Ignored if mediaPlayer is not in a valid state
                }
                handler.postDelayed(this, 500)
            }
        })
    }

    private fun updateMediaSessionPlaybackState(state: Int, position: Long) {
        val playbackState = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or
                        PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackState.ACTION_SEEK_TO or
                        PlaybackState.ACTION_STOP
            )
            .setState(state, position, 1.0f)
            .build()
        mediaSession?.setPlaybackState(playbackState)
    }

    private fun updateMediaSessionMetadata() {
        val track = PlaybackStateManager.getCurrentState().currentTrack ?: return
        val currentDuration = if (isPlayerPrepared) {
            try {
                mediaPlayer?.duration?.toLong() ?: 0L
            } catch (e: Exception) {
                0L
            }
        } else {
            PlaybackStateManager.getCurrentState().durationMs
        }

        val metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, track.title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, currentDuration)
            .apply {
                currentAlbumArtBitmap?.let {
                    putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, it)
                }
            }
            .build()
        mediaSession?.setMetadata(metadata)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Spotific Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Media playback controls for Spotific"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startOrUpdateForegroundNotification() {
        val state = PlaybackStateManager.getCurrentState()
        val track = state.currentTrack ?: return

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = PendingIntent.getService(
            this, 1,
            Intent(this, AudioPlaybackService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = PendingIntent.getService(
            this, 2,
            Intent(this, AudioPlaybackService::class.java).apply {
                action = if (state.isPlaying) ACTION_PAUSE else ACTION_PLAY
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = PendingIntent.getService(
            this, 3,
            Intent(this, AudioPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 4,
            Intent(this, AudioPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (state.isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        builder
            .setContentTitle(track.title)
            .setContentText(track.artist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(stopIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(state.isPlaying)
            .addAction(Notification.Action.Builder(android.R.drawable.ic_media_previous, "Prev", prevIntent).build())
            .addAction(Notification.Action.Builder(playPauseIcon, if (state.isPlaying) "Pause" else "Play", playPauseIntent).build())
            .addAction(Notification.Action.Builder(android.R.drawable.ic_media_next, "Next", nextIntent).build())

        currentAlbumArtBitmap?.let {
            builder.setLargeIcon(it)
        }

        mediaSession?.let {
            val style = Notification.MediaStyle()
                .setMediaSession(it.sessionToken)
                .setShowActionsInCompactView(0, 1, 2)
            builder.style = style
        }

        val notification = builder.build()

        if (!isServiceForeground) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            isServiceForeground = true
        } else {
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun requestAudioFocus(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS -> pause()
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
                        AudioManager.AUDIOFOCUS_GAIN -> play()
                    }
                }
                .build()
            focusRequest = request
            return audioManager?.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager?.requestAudioFocus(
                { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS) pause()
                },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }
    }

    private fun formatTime(millis: Long): String {
        val totalSecs = (millis / 1000).coerceAtLeast(0)
        val minutes = totalSecs / 60
        val seconds = totalSecs % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AudioPlaybackService destroyed")
        isPlayerPrepared = false
        handler.removeCallbacksAndMessages(null)
        serviceScope.cancel()
        abandonAudioFocus()
        mediaSession?.release()
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignored
        }
        mediaPlayer = null
    }

    companion object {
        const val TAG = "SpotificPlaybackService"
        const val CHANNEL_ID = "spotific_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.example.spotific.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.spotific.ACTION_PAUSE"
        const val ACTION_TOGGLE_PLAY_PAUSE = "com.example.spotific.ACTION_TOGGLE"
        const val ACTION_NEXT = "com.example.spotific.ACTION_NEXT"
        const val ACTION_PREV = "com.example.spotific.ACTION_PREV"
        const val ACTION_STOP = "com.example.spotific.ACTION_STOP"
        const val ACTION_SEEK = "com.example.spotific.ACTION_SEEK"
        const val EXTRA_POSITION = "extra_position"
    }
}
