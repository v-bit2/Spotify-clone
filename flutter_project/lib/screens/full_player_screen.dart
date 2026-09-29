import 'package:flutter/material.dart';
import '../models/track.dart';
import '../services/playback_platform_bridge.dart';

class FullPlayerScreen extends StatefulWidget {
  final PlaybackStateModel playbackState;

  const FullPlayerScreen({Key? key, required this.playbackState}) : super(key: key);

  @override
  State<FullPlayerScreen> createState() => _FullPlayerScreenState();
}

class _FullPlayerScreenState extends State<FullPlayerScreen> {
  double? _dragValue;

  String _formatTime(int ms) {
    final s = (ms / 1000).floor();
    final m = (s / 60).floor();
    final rem = s % 60;
    return '$m:${rem.toString().padLeft(2, '0')}';
  }

  @override
  Widget build(BuildContext context) {
    final track = widget.playbackState.currentTrack;
    if (track == null) return const SizedBox.shrink();

    final dur = widget.playbackState.durationMs > 0 ? widget.playbackState.durationMs : 1;
    final pos = widget.playbackState.positionMs.clamp(0, dur);
    final sliderVal = _dragValue ?? (pos / dur).clamp(0.0, 1.0);

    return Scaffold(
      backgroundColor: const Color(0xFF0A0A0F),
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [Color(0xFF0F1A30), Color(0xFF0A0A0F), Color(0xFF050508)],
          ),
        ),
        child: SafeArea(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 24),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                // Top bar
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    IconButton(
                      icon: const Icon(Icons.keyboard_arrow_down_rounded,
                          color: Colors.white, size: 32),
                      onPressed: () => Navigator.pop(context),
                    ),
                    const Column(
                      children: [
                        Text('PLAYING FROM SPOTIFIC',
                            style: TextStyle(
                                color: Color(0xFF6B7280),
                                fontSize: 10,
                                fontWeight: FontWeight.bold,
                                letterSpacing: 1.2)),
                        Text('NOW PLAYING',
                            style: TextStyle(
                                color: Colors.white,
                                fontSize: 12,
                                fontWeight: FontWeight.bold,
                                letterSpacing: 1)),
                      ],
                    ),
                    const Icon(Icons.more_vert_rounded, color: Colors.white),
                  ],
                ),

                // Artwork
                Container(
                  width: MediaQuery.of(context).size.width * 0.8,
                  height: MediaQuery.of(context).size.width * 0.8,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(20),
                    boxShadow: [
                      BoxShadow(
                        color: const Color(0xFF1E90FF).withOpacity(0.3),
                        blurRadius: 30,
                        spreadRadius: 4,
                      ),
                    ],
                  ),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(20),
                    child: track.thumbnail != null
                        ? Image.network(track.thumbnail!, fit: BoxFit.cover)
                        : Container(
                            color: const Color(0xFF161622),
                            child: const Icon(Icons.music_note,
                                color: Colors.white, size: 64),
                          ),
                  ),
                ),

                // Title and Artist
                Column(
                  children: [
                    Text(
                      track.title,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 22,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      track.artist,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        color: Color(0xFFA0A5B5),
                        fontSize: 16,
                      ),
                    ),
                  ],
                ),

                // Slider with attached circular knob
                Column(
                  children: [
                    SliderTheme(
                      data: SliderTheme.of(context).copyWith(
                        activeTrackColor: const Color(0xFF1E90FF),
                        inactiveTrackColor: Colors.white.withOpacity(0.2),
                        thumbColor: const Color(0xFF1E90FF),
                        thumbShape: const RoundSliderThumbShape(
                            enabledThumbRadius: 7.0),
                        overlayColor: const Color(0xFF1E90FF).withOpacity(0.2),
                        trackHeight: 4.0,
                      ),
                      child: Slider(
                        value: sliderVal,
                        onChanged: (val) {
                          setState(() => _dragValue = val);
                        },
                        onChangeEnd: (val) {
                          final targetMs = (val * dur).toInt();
                          PlaybackPlatformBridge.instance.seek(targetMs);
                          setState(() => _dragValue = null);
                        },
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 16),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Text(_formatTime((sliderVal * dur).toInt()),
                              style: const TextStyle(
                                  color: Color(0xFF6B7280), fontSize: 12)),
                          Text(_formatTime(dur),
                              style: const TextStyle(
                                  color: Color(0xFF6B7280), fontSize: 12)),
                        ],
                      ),
                    ),
                  ],
                ),

                // Controls row
                Padding(
                  padding: const EdgeInsets.only(bottom: 24),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    children: [
                      const Icon(Icons.favorite_border,
                          color: Color(0xFFA0A5B5), size: 28),
                      IconButton(
                        icon: const Icon(Icons.skip_previous_rounded,
                            color: Colors.white, size: 38),
                        onPressed: () => PlaybackPlatformBridge.instance.skipPrevious(),
                      ),
                      // Large circular electric blue play/pause
                      GestureDetector(
                        onTap: () => PlaybackPlatformBridge.instance.togglePlayPause(),
                        child: Container(
                          width: 68,
                          height: 68,
                          decoration: const BoxDecoration(
                            shape: BoxShape.circle,
                            gradient: LinearGradient(
                              colors: [Color(0xFF4FACFE), Color(0xFF1E90FF)],
                            ),
                          ),
                          child: Icon(
                            widget.playbackState.isPlaying
                                ? Icons.pause_rounded
                                : Icons.play_arrow_rounded,
                            color: Colors.white,
                            size: 38,
                          ),
                        ),
                      ),
                      IconButton(
                        icon: const Icon(Icons.skip_next_rounded,
                            color: Colors.white, size: 38),
                        onPressed: () => PlaybackPlatformBridge.instance.skipNext(),
                      ),
                      const Icon(Icons.download_rounded,
                          color: Color(0xFFA0A5B5), size: 28),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
