import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/track.dart';

class SpotificApiService {
  static const String baseUrl = 'https://api.nexray.eu.cc';

  static Future<List<Track>> search(String query) async {
    try {
      final uri = Uri.parse('$baseUrl/search/spotify?q=${Uri.encodeComponent(query)}');
      final response = await http.get(uri);
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['status'] == true && data['result'] is List) {
          return (data['result'] as List)
              .map((item) => Track.fromJson(item))
              .where((t) => t.title.isNotEmpty)
              .toList();
        }
      }
    } catch (e) {
      // Error handling
    }
    return [];
  }

  /// Primary stream with retry, falling back to query-based endpoint
  static Future<String?> resolveStreamUrl(Track track) async {
    // 1. Try Primary
    if (track.url.isNotEmpty) {
      for (int attempt = 0; attempt < 3; attempt++) {
        try {
          final uri = Uri.parse('$baseUrl/downloader/spotify?url=${Uri.encodeComponent(track.url)}');
          final response = await http.get(uri);
          if (response.statusCode == 200) {
            final data = jsonDecode(response.body);
            if (data['status'] == true && data['result']?['url'] != null) {
              return data['result']['url'];
            }
          }
        } catch (e) {
          await Future.delayed(const Duration(milliseconds: 300));
        }
      }
    }

    // 2. Fallback query-based
    try {
      final q = Uri.encodeComponent('${track.title} ${track.artist}');
      final uri = Uri.parse('$baseUrl/downloader/spotifyplay?q=$q');
      final response = await http.get(uri);
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['status'] == true && data['result']?['download_url'] != null) {
          return data['result']['download_url'];
        }
      }
    } catch (e) {
      // Failed fallback
    }

    return null;
  }
}
