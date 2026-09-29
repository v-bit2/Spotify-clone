package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SpotificApplication
import com.example.data.model.Track
import com.example.playback.PlaybackStateData
import com.example.playback.PlaybackStateManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    HOME,
    SEARCH,
    LIBRARY,
    DOWNLOADS,
    ABOUT
}

data class ArtistItem(
    val name: String,
    val imageUrl: String
)

class SpotificViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SpotificApplication
    private val repository = app.repository
    private val playbackController = app.playbackController

    // Playback state - Single source of truth from Kotlin Service!
    val playbackState: StateFlow<PlaybackStateData> = PlaybackStateManager.playbackState

    // Navigation state
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _isFullPlayerVisible = MutableStateFlow(false)
    val isFullPlayerVisible: StateFlow<Boolean> = _isFullPlayerVisible.asStateFlow()

    // Home feed state
    private val _featuredTracks = MutableStateFlow<List<Track>>(emptyList())
    val featuredTracks: StateFlow<List<Track>> = _featuredTracks.asStateFlow()

    private val _trendingTracks = MutableStateFlow<List<Track>>(emptyList())
    val trendingTracks: StateFlow<List<Track>> = _trendingTracks.asStateFlow()

    private val _biggestHits = MutableStateFlow<List<Track>>(emptyList())
    val biggestHits: StateFlow<List<Track>> = _biggestHits.asStateFlow()

    private val _chillTracks = MutableStateFlow<List<Track>>(emptyList())
    val chillTracks: StateFlow<List<Track>> = _chillTracks.asStateFlow()

    private val _isHomeLoading = MutableStateFlow(true)
    val isHomeLoading: StateFlow<Boolean> = _isHomeLoading.asStateFlow()

    // Top Artists
    val topArtists = listOf(
        ArtistItem("The Weeknd", "https://i.scdn.co/image/ab6761610000e5ebbba33b3b248a31e8c0500259"),
        ArtistItem("Taylor Swift", "https://i.scdn.co/image/ab6761610000e5eb5a00969a4698c3132a15fbb0"),
        ArtistItem("Drake", "https://i.scdn.co/image/ab6761610000e5eb4293385d324db8558179afd9"),
        ArtistItem("Billie Eilish", "https://i.scdn.co/image/ab6761610000e5ebd8b9980db6720d4424c5e3d7"),
        ArtistItem("Bruno Mars", "https://i.scdn.co/image/ab6761610000e5ebc36dd9eb55fb0db4911f25dd"),
        ArtistItem("Dua Lipa", "https://i.scdn.co/image/ab6761610000e5ebd42a27db3286b58553da8858"),
        ArtistItem("Travis Scott", "https://i.scdn.co/image/ab6761610000e5ebb99cacf8acd5378206767261"),
        ArtistItem("Coldplay", "https://i.scdn.co/image/ab6761610000e5eb989ed050d1036b72b0797f1f")
    )

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Track>>(emptyList())
    val searchResults: StateFlow<List<Track>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var searchJob: Job? = null

    // Library & Downloads from Room
    val favorites: StateFlow<List<Track>> = repository.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloads: StateFlow<List<Track>> = repository.downloadsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Download progress map
    private val _downloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Int>> = _downloadProgress.asStateFlow()

    init {
        // Cold start re-sync: restore state immediately before UI render
        playbackController.reSyncState()
        loadHomeFeed()
    }

    fun setScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setFullPlayerVisible(visible: Boolean) {
        _isFullPlayerVisible.value = visible
    }

    fun loadHomeFeed() {
        viewModelScope.launch {
            _isHomeLoading.value = true
            try {
                // Fetch real data from verified working API endpoints
                val trending = repository.search("Top Hits 2024")
                _trendingTracks.value = trending
                _featuredTracks.value = trending.take(5)

                val hits = repository.search("Today's Top Hits")
                _biggestHits.value = hits

                val chill = repository.search("Chill Lo-Fi Vibes")
                _chillTracks.value = chill
            } catch (e: Exception) {
                // Keep existing or empty
            } finally {
                _isHomeLoading.value = false
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()

        if (query.trim().isEmpty()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(400) // Debounce
            _isSearching.value = true
            try {
                val results = repository.search(query)
                _searchResults.value = results
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun searchCategory(category: String) {
        _searchQuery.value = category
        _isSearching.value = true
        _currentScreen.value = AppScreen.SEARCH
        viewModelScope.launch {
            try {
                val results = repository.search(category)
                _searchResults.value = results
            } finally {
                _isSearching.value = false
            }
        }
    }

    // Playback actions
    fun playTrack(track: Track, queue: List<Track> = emptyList()) {
        playbackController.playTrack(track, queue)
    }

    fun togglePlayPause() {
        playbackController.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        playbackController.seekTo(positionMs)
    }

    fun skipNext() {
        playbackController.skipNext()
    }

    fun skipPrevious() {
        playbackController.skipPrevious()
    }

    fun toggleShuffle() {
        playbackController.toggleShuffle()
    }

    fun toggleRepeat() {
        playbackController.toggleRepeat()
    }

    // Favorite actions
    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            repository.toggleFavorite(track)
        }
    }

    fun isTrackFavorited(trackId: String): Boolean {
        return favorites.value.any { it.id == trackId }
    }

    // Download actions
    fun downloadTrack(track: Track) {
        if (_downloadProgress.value.containsKey(track.id)) return
        viewModelScope.launch {
            _downloadProgress.value = _downloadProgress.value + (track.id to 0)
            repository.downloadTrack(track) { progress ->
                _downloadProgress.value = _downloadProgress.value + (track.id to progress)
            }
            _downloadProgress.value = _downloadProgress.value - track.id
        }
    }

    fun removeDownload(trackId: String) {
        viewModelScope.launch {
            repository.removeDownload(trackId)
        }
    }

    fun isTrackDownloaded(trackId: String): Boolean {
        return downloads.value.any { it.id == trackId }
    }
}
