package com.example.channel

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.data.model.Track
import com.example.playback.PlaybackController
import com.example.playback.PlaybackStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Platform Channel handler providing the exact contract requested by the architecture:
 * MethodChannel: com.example.spotific/playback_methods
 * EventChannel:  com.example.spotific/playback_events
 */
class SpotificPlatformChannel(
    private val context: Context,
    private val controller: PlaybackController
) {
    companion object {
        const val METHOD_CHANNEL = "com.example.spotific/playback_methods"
        const val EVENT_CHANNEL = "com.example.spotific/playback_events"
    }

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var eventSink: ((Map<String, Any?>) -> Unit)? = null

    init {
        // Collect PlaybackStateManager and emit to eventSink if attached
        scope.launch {
            PlaybackStateManager.playbackState.collect { state ->
                val payload = buildStateMap(state)
                eventSink?.invoke(payload)
            }
        }
    }

    fun handleMethodCall(method: String, arguments: Any?, resultCallback: (Any?, String?) -> Unit) {
        when (method) {
            "getPlaybackState" -> {
                val state = controller.reSyncState()
                resultCallback(buildStateMap(state), null)
            }
            "play" -> {
                val args = arguments as? Map<*, *>
                val trackMap = args?.get("track") as? Map<*, *>
                val queueList = args?.get("queue") as? List<*>

                if (trackMap != null) {
                    val track = parseTrackFromMap(trackMap)
                    val queue = queueList?.mapNotNull { (it as? Map<*, *>)?.let { m -> parseTrackFromMap(m) } } ?: emptyList()
                    controller.playTrack(track, queue)
                    resultCallback(true, null)
                } else {
                    resultCallback(null, "Missing track argument")
                }
            }
            "pause" -> {
                val state = PlaybackStateManager.getCurrentState()
                if (state.isPlaying) controller.togglePlayPause()
                resultCallback(true, null)
            }
            "togglePlayPause" -> {
                controller.togglePlayPause()
                resultCallback(true, null)
            }
            "seek" -> {
                val pos = (arguments as? Map<*, *>)?.get("position") as? Number
                if (pos != null) {
                    controller.seekTo(pos.toLong())
                    resultCallback(true, null)
                } else {
                    resultCallback(null, "Missing position argument")
                }
            }
            "skipNext" -> {
                controller.skipNext()
                resultCallback(true, null)
            }
            "skipPrevious" -> {
                controller.skipPrevious()
                resultCallback(true, null)
            }
            else -> {
                resultCallback(null, "Method not implemented: $method")
            }
        }
    }

    fun setEventSink(sink: ((Map<String, Any?>) -> Unit)?) {
        this.eventSink = sink
        // Immediately dispatch current state upon subscription
        sink?.invoke(buildStateMap(PlaybackStateManager.getCurrentState()))
    }

    private fun buildStateMap(state: com.example.playback.PlaybackStateData): Map<String, Any?> {
        val track = state.currentTrack
        return mapOf(
            "isPlaying" to state.isPlaying,
            "isBuffering" to state.isBuffering,
            "position" to state.currentPositionMs,
            "duration" to state.durationMs,
            "title" to (track?.title ?: ""),
            "artist" to (track?.artist ?: ""),
            "thumbnail" to (track?.thumbnail ?: ""),
            "url" to (track?.url ?: ""),
            "durationFormatted" to (track?.duration ?: "--:--"),
            "queueSize" to state.queue.size,
            "currentIndex" to state.currentIndex,
            "isShuffle" to state.isShuffle,
            "isRepeat" to state.isRepeat
        )
    }

    private fun parseTrackFromMap(map: Map<*, *>): Track {
        return Track(
            title = map["title"] as? String ?: "",
            artist = map["artist"] as? String ?: "Unknown Artist",
            thumbnail = map["thumbnail"] as? String,
            url = map["url"] as? String ?: "",
            duration = map["duration"] as? String,
            streamUrl = map["streamUrl"] as? String,
            localFilePath = map["localFilePath"] as? String
        )
    }
}
