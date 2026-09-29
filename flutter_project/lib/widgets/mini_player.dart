import 'package:flutter/material.dart';
import '../models/track.dart';
import '../services/playback_platform_bridge.dart';

class MiniPlayerWidget extends StatelessWidget {
  final PlaybackStateModel playbackState;
  final VoidCallback onTap;

  const MiniPlayerWidget({
    Key? key,
    required this.playbackState,
    required this.onTap,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final track = playbackState.currentTrack;
    if (track == null) return const SizedBox.shrink();

    final progress = playbackState.durationMs > 0
        ? (playbackState.positionMs / playbackState.durationMs).clamp(0.0, 1.0)
        : 0.0;

    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: const Color(0xF2161622),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: Colors.white.withOpacity(0.08)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.4),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(14),
          onTap: onTap,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                child: Row(
                  children: [
                    // Album art
                    ClipRRect(
                      borderRadius: BorderRadius.circular(8),
                      child: Container(
                        width: 44,
                        height: 44,
                        color: const Color(0xFF222230),
                        child: track.thumbnail != null
                            ? Image.network(
                                track.thumbnail!,
                                fit: BoxFit.cover,
                                errorBuilder: (_, __, ___) =>
                                    const Icon(Icons.music_note, color: Colors.white),
                              )
                            : const Icon(Icons.music_note, color: Colors.white),
                      ),
                    ),
                    const SizedBox(width: 12),
                    // Title and artist
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            track.title,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 14,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                          Text(
                            track.artist,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              color: Color(0xFFA0A5B5),
                              fontSize: 12,
                            ),
                          ),
                        ],
                      ),
                    ),
                    // Play/pause button
                    IconButton(
                      icon: Icon(
                        playbackState.isPlaying
                            ? Icons.pause_rounded
                            : Icons.play_arrow_rounded,
                        color: Colors.white,
                        size: 28,
                      ),
                      onPressed: () {
                        PlaybackPlatformBridge.instance.togglePlayPause();
                      },
                    ),
                  ],
                ),
              ),
              // Linear progress indicator
              ClipRRect(
                borderRadius: const BorderRadius.vertical(bottom: Radius.circular(14)),
                child: LinearProgressIndicator(
                  value: progress,
                  minHeight: 2.5,
                  backgroundColor: Colors.white.withOpacity(0.1),
                  valueColor: const AlwaysStoppedAnimation<Color>(Color(0xFF1E90FF)),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
