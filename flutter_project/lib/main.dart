import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'models/track.dart';
import 'services/playback_platform_bridge.dart';
import 'services/spotific_api_service.dart';
import 'widgets/mini_player.dart';
import 'screens/full_player_screen.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
    statusBarColor: Colors.transparent,
    statusBarIconBrightness: Brightness.light,
    systemNavigationBarColor: Color(0xFF0A0A0F),
    systemNavigationBarIconBrightness: Brightness.light,
  ));

  // CRITICAL REQUIREMENT: Re-sync state with currently running Kotlin service BEFORE building UI!
  PlaybackPlatformBridge.instance.initialize();
  await PlaybackPlatformBridge.instance.reSyncState();

  runApp(const SpotificApp());
}

class SpotificApp extends StatelessWidget {
  const SpotificApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Spotific',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: const Color(0xFF0A0A0F),
        primaryColor: const Color(0xFF1E90FF),
        colorScheme: const ColorScheme.dark(
          primary: Color(0xFF1E90FF),
          secondary: Color(0xFF4FACFE),
          surface: Color(0xFF14141E),
        ),
        fontFamily: 'Inter',
      ),
      home: const MainNavigationScreen(),
    );
  }
}

class MainNavigationScreen extends StatefulWidget {
  const MainNavigationScreen({Key? key}) : super(key: key);

  @override
  State<MainNavigationScreen> createState() => _MainNavigationScreenState();
}

class _MainNavigationScreenState extends State<MainNavigationScreen> {
  int _currentIndex = 0;
  PlaybackStateModel _playbackState = PlaybackPlatformBridge.instance.currentState;
  List<Track> _trending = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    PlaybackPlatformBridge.instance.playbackStream.listen((state) {
      if (mounted) setState(() => _playbackState = state);
    });
    _loadFeed();
  }

  Future<void> _loadFeed() async {
    final results = await SpotificApiService.search('Top Hits');
    if (mounted) {
      setState(() {
        _trending = results;
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Stack(
        children: [
          // Screen content
          IndexedStack(
            index: _currentIndex,
            children: [
              _buildHome(),
              _buildPlaceholder('Search'),
              _buildPlaceholder('Library'),
              _buildPlaceholder('Downloads'),
              _buildPlaceholder('About Spotific'),
            ],
          ),

          // Docked Mini Player
          if (_playbackState.currentTrack != null)
            Positioned(
              left: 0,
              right: 0,
              bottom: 0,
              child: MiniPlayerWidget(
                playbackState = _playbackState,
                onTap: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) => FullPlayerScreen(playbackState: _playbackState),
                    ),
                  );
                },
              ),
            ),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _currentIndex,
        backgroundColor: const Color(0xFF0E0E18),
        indicatorColor: const Color(0x2B1E90FF),
        onDestinationSelected: (idx) => setState(() => _currentIndex = idx),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.home_outlined),
            selectedIcon: Icon(Icons.home, color: Color(0xFF1E90FF)),
            label: 'Home',
          ),
          NavigationDestination(
            icon: Icon(Icons.search_outlined),
            selectedIcon: Icon(Icons.search, color: Color(0xFF1E90FF)),
            label: 'Search',
          ),
          NavigationDestination(
            icon: Icon(Icons.favorite_outline),
            selectedIcon: Icon(Icons.favorite, color: Color(0xFF1E90FF)),
            label: 'Library',
          ),
          NavigationDestination(
            icon: Icon(Icons.download_outlined),
            selectedIcon: Icon(Icons.download, color: Color(0xFF1E90FF)),
            label: 'Downloads',
          ),
          NavigationDestination(
            icon: Icon(Icons.info_outline),
            selectedIcon: Icon(Icons.info, color: Color(0xFF1E90FF)),
            label: 'About',
          ),
        ],
      ),
    );
  }

  Widget _buildHome() {
    return SafeArea(
      child: ListView(
        padding: const EdgeInsets.only(bottom: 120),
        children: [
          Padding(
            padding: const EdgeInsets.all(20),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  'Good evening',
                  style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
                ),
                IconButton(
                  icon: const Icon(Icons.search),
                  onPressed: () => setState(() => _currentIndex = 1),
                ),
              ],
            ),
          ),
          if (_isLoading)
            const Center(child: CircularProgressIndicator(color: Color(0xFF1E90FF)))
          else ...[
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 20, vertical: 8),
              child: Text(
                'Trending Now',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
            ),
            SizedBox(
              height: 200,
              child: ListView.separated(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: 20),
                itemCount: _trending.length,
                separatorBuilder: (_, __) => const SizedBox(width: 14),
                itemBuilder: (ctx, i) {
                  final track = _trending[i];
                  return GestureDetector(
                    onTap: () {
                      PlaybackPlatformBridge.instance.playTrack(track, _trending);
                    },
                    child: SizedBox(
                      width: 140,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          ClipRRect(
                            borderRadius: BorderRadius.circular(14),
                            child: track.thumbnail != null
                                ? Image.network(track.thumbnail!,
                                    width: 140, height: 140, fit: BoxFit.cover)
                                : Container(
                                    width: 140,
                                    height: 140,
                                    color: const Color(0xFF1E1E28),
                                  ),
                          ),
                          const SizedBox(height: 8),
                          Text(track.title,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(fontWeight: FontWeight.bold)),
                          Text(track.artist,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(color: Color(0xFFA0A5B5), fontSize: 12)),
                        ],
                      ),
                    ),
                  );
                },
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildPlaceholder(String title) {
    return Center(
      child: Text(title,
          style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
    );
  }
}
