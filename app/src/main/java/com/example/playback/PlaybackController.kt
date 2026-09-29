package com.example.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.data.model.Track

class PlaybackController(private val context: Context) {

    private var service: AudioPlaybackService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Log.d(TAG, "AudioPlaybackService bound successfully")
            val localBinder = binder as? AudioPlaybackService.LocalBinder
            service = localBinder?.getService()
            isBound = true
            // Re-sync immediately upon binding!
            reSyncState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "AudioPlaybackService disconnected")
            service = null
            isBound = false
        }
    }

    init {
        bindService()
    }

    fun bindService() {
        val intent = Intent(context, AudioPlaybackService::class.java)
        // Start foreground service first so it's persistent
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not start service: ${e.message}")
        }
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService() {
        if (isBound) {
            try {
                context.unbindService(connection)
            } catch (e: Exception) {
                Log.w(TAG, "Unbind error: ${e.message}")
            }
            isBound = false
        }
    }

    /**
     * CRITICAL REQUIREMENT: Re-sync state with running service on cold boot / app reopen.
     * Guarantees the Mini Player and Full Player immediately match the active playback.
     */
    fun reSyncState(): PlaybackStateData {
        return PlaybackStateManager.getCurrentState()
    }

    fun playTrack(track: Track, queue: List<Track> = emptyList()) {
        if (service != null) {
            service?.playTrack(track, queue)
        } else {
            bindService()
            // Dispatch intent fallback
            val intent = Intent(context, AudioPlaybackService::class.java).apply {
                action = AudioPlaybackService.ACTION_PLAY
            }
            context.startService(intent)
            // Retry calling service once bound
            service?.playTrack(track, queue)
        }
    }

    fun togglePlayPause() {
        if (service != null) {
            service?.togglePlayPause()
        } else {
            val intent = Intent(context, AudioPlaybackService::class.java).apply {
                action = AudioPlaybackService.ACTION_TOGGLE_PLAY_PAUSE
            }
            context.startService(intent)
        }
    }

    fun seekTo(positionMs: Long) {
        if (service != null) {
            service?.seekTo(positionMs)
        } else {
            val intent = Intent(context, AudioPlaybackService::class.java).apply {
                action = AudioPlaybackService.ACTION_SEEK
                putExtra(AudioPlaybackService.EXTRA_POSITION, positionMs)
            }
            context.startService(intent)
        }
    }

    fun skipNext() {
        if (service != null) {
            service?.skipNext()
        } else {
            val intent = Intent(context, AudioPlaybackService::class.java).apply {
                action = AudioPlaybackService.ACTION_NEXT
            }
            context.startService(intent)
        }
    }

    fun skipPrevious() {
        if (service != null) {
            service?.skipPrevious()
        } else {
            val intent = Intent(context, AudioPlaybackService::class.java).apply {
                action = AudioPlaybackService.ACTION_PREV
            }
            context.startService(intent)
        }
    }

    fun toggleShuffle() {
        PlaybackStateManager.updateState { it.copy(isShuffle = !it.isShuffle) }
    }

    fun toggleRepeat() {
        PlaybackStateManager.updateState { it.copy(isRepeat = !it.isRepeat) }
    }

    companion object {
        private const val TAG = "PlaybackController"
    }
}
