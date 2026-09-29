import 'dart:async';
import 'package:flutter/services.dart';
import '../models/track.dart';

class PlaybackPlatformBridge {
  static const MethodChannel _methodChannel =
      MethodChannel('com.example.spotific/playback_methods');
  static const EventChannel _eventChannel =
      EventChannel('com.example.spotific/playback_events');

  static final PlaybackPlatformBridge instance = PlaybackPlatformBridge._();
  PlaybackPlatformBridge._();

  final StreamController<PlaybackStateModel> _stateController =
      StreamController<PlaybackStateModel>.broadcast();
  Stream<PlaybackStateModel> get playbackStream => _stateController.stream;

  PlaybackStateModel _lastState = PlaybackStateModel();
  PlaybackStateModel get currentState => _lastState;

  void initialize() {
    _eventChannel.receiveBroadcastStream().listen((dynamic event) {
      if (event is Map) {
        _lastState = PlaybackStateModel.fromMap(event);
        _stateController.add(_lastState);
      }
    }, onError: (dynamic error) {
      // Ignore or log error
    });
  }

  /// CRITICAL REQUIREMENT — SURVIVING APP CLOSE:
  /// Flutter queries currently running Kotlin service on startup BEFORE building UI.
  Future<PlaybackStateModel> reSyncState() async {
    try {
      final dynamic result = await _methodChannel.invokeMethod('getPlaybackState');
      if (result is Map) {
        _lastState = PlaybackStateModel.fromMap(result);
        _stateController.add(_lastState);
        return _lastState;
      }
    } catch (e) {
      // Fallback
    }
    return _lastState;
  }

  Future<void> playTrack(Track track, List<Track> queue) async {
    try {
      await _methodChannel.invokeMethod('play', {
        'track': track.toJson(),
        'queue': queue.map((t) => t.toJson()).toList(),
      });
    } catch (e) {
      // Handle exception
    }
  }

  Future<void> togglePlayPause() async {
    try {
      await _methodChannel.invokeMethod('togglePlayPause');
    } catch (e) {
      // Handle exception
    }
  }

  Future<void> pause() async {
    try {
      await _methodChannel.invokeMethod('pause');
    } catch (e) {
      // Handle exception
    }
  }

  Future<void> seek(int positionMs) async {
    try {
      await _methodChannel.invokeMethod('seek', {'position': positionMs});
    } catch (e) {
      // Handle exception
    }
  }

  Future<void> skipNext() async {
    try {
      await _methodChannel.invokeMethod('skipNext');
    } catch (e) {
      // Handle exception
    }
  }

  Future<void> skipPrevious() async {
    try {
      await _methodChannel.invokeMethod('skipPrevious');
    } catch (e) {
      // Handle exception
    }
  }
}
