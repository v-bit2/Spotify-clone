class Track {
  final String title;
  final String artist;
  final String? thumbnail;
  final String url;
  final String? duration;
  final String? streamUrl;
  final String? localFilePath;

  Track({
    required this.title,
    required this.artist,
    this.thumbnail,
    required this.url,
    this.duration,
    this.streamUrl,
    this.localFilePath,
  });

  String get id => url.isNotEmpty ? url : '${title}_${artist}'.hashCode.toString();

  factory Track.fromJson(Map<String, dynamic> json) {
    return Track(
      title: json['title'] ?? '',
      artist: json['artist'] ?? 'Unknown Artist',
      thumbnail: json['thumbnail'],
      url: json['url'] ?? '',
      duration: json['duration'],
      streamUrl: json['streamUrl'],
      localFilePath: json['localFilePath'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'title': title,
      'artist': artist,
      'thumbnail': thumbnail,
      'url': url,
      'duration': duration,
      'streamUrl': streamUrl,
      'localFilePath': localFilePath,
    };
  }
}

class PlaybackStateModel {
  final Track? currentTrack;
  final bool isPlaying;
  final bool isBuffering;
  final int positionMs;
  final int durationMs;
  final bool isShuffle;
  final bool isRepeat;

  PlaybackStateModel({
    this.currentTrack,
    this.isPlaying = false,
    this.isBuffering = false,
    this.positionMs = 0,
    this.durationMs = 0,
    this.isShuffle = false,
    this.isRepeat = false,
  });

  factory PlaybackStateModel.fromMap(Map<dynamic, dynamic> map) {
    Track? track;
    final title = map['title'] as String?;
    if (title != null && title.isNotEmpty) {
      track = Track(
        title: title,
        artist: (map['artist'] as String?) ?? 'Unknown Artist',
        thumbnail: map['thumbnail'] as String?,
        url: (map['url'] as String?) ?? '',
        duration: map['durationFormatted'] as String?,
      );
    }

    return PlaybackStateModel(
      currentTrack: track,
      isPlaying: map['isPlaying'] == true,
      isBuffering: map['isBuffering'] == true,
      positionMs: (map['position'] as num?)?.toInt() ?? 0,
      durationMs: (map['duration'] as num?)?.toInt() ?? 0,
      isShuffle: map['isShuffle'] == true,
      isRepeat: map['isRepeat'] == true,
    );
  }
}
