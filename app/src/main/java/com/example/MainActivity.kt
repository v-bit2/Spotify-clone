package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.playback.PlaybackStateManager
import com.example.ui.components.BufferingOverlay
import com.example.ui.components.MiniPlayer
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.FullPlayerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SpotificBluePrimary
import com.example.ui.theme.SpotificCardSurface
import com.example.ui.theme.SpotificDarkBg
import com.example.ui.theme.SpotificTextMuted
import com.example.ui.theme.SpotificTextPrimary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.SpotificViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SpotificViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Re-sync with Kotlin background service immediately on startup
        (application as SpotificApplication).playbackController.reSyncState()

        setContent {
            MyApplicationTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainContent(viewModel: SpotificViewModel) {
    var showSplash by remember { mutableStateOf(true) }
    val currentScreen by viewModel.currentScreen.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val isFullPlayerVisible by viewModel.isFullPlayerVisible.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    // Request Notification permission on Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (showSplash) {
        SplashScreen(onSplashFinished = { showSplash = false })
        return
    }

    // Handle back button when full player is open
    BackHandler(enabled = isFullPlayerVisible) {
        viewModel.setFullPlayerVisible(false)
    }

    // Handle back button when on sub-screen
    BackHandler(enabled = !isFullPlayerVisible && currentScreen != AppScreen.HOME) {
        viewModel.setScreen(AppScreen.HOME)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpotificDarkBg)
    ) {
        Scaffold(
            containerColor = SpotificDarkBg,
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    // Mini Player docked directly above bottom nav
                    if (playbackState.currentTrack != null) {
                        val isFav = favorites.any { it.id == playbackState.currentTrack?.id }
                        MiniPlayer(
                            playbackState = playbackState,
                            isFavorite = isFav,
                            onExpandClick = { viewModel.setFullPlayerVisible(true) },
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onToggleFavorite = {
                                playbackState.currentTrack?.let { viewModel.toggleFavorite(it) }
                            }
                        )
                    }

                    // Flush Bottom Navigation Bar
                    NavigationBar(
                        containerColor = Color(0xF20E0E18),
                        tonalElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.HOME,
                            onClick = { viewModel.setScreen(AppScreen.HOME) },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == AppScreen.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                    contentDescription = "Home"
                                )
                            },
                            label = { Text("Home", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SpotificBluePrimary,
                                selectedTextColor = SpotificBluePrimary,
                                unselectedIconColor = SpotificTextMuted,
                                unselectedTextColor = SpotificTextMuted,
                                indicatorColor = Color(0x1F1E90FF)
                            )
                        )

                        NavigationBarItem(
                            selected = currentScreen == AppScreen.SEARCH,
                            onClick = { viewModel.setScreen(AppScreen.SEARCH) },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == AppScreen.SEARCH) Icons.Filled.Search else Icons.Outlined.Search,
                                    contentDescription = "Search"
                                )
                            },
                            label = { Text("Search", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SpotificBluePrimary,
                                selectedTextColor = SpotificBluePrimary,
                                unselectedIconColor = SpotificTextMuted,
                                unselectedTextColor = SpotificTextMuted,
                                indicatorColor = Color(0x1F1E90FF)
                            )
                        )

                        NavigationBarItem(
                            selected = currentScreen == AppScreen.LIBRARY,
                            onClick = { viewModel.setScreen(AppScreen.LIBRARY) },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == AppScreen.LIBRARY) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Library"
                                )
                            },
                            label = { Text("Library", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SpotificBluePrimary,
                                selectedTextColor = SpotificBluePrimary,
                                unselectedIconColor = SpotificTextMuted,
                                unselectedTextColor = SpotificTextMuted,
                                indicatorColor = Color(0x1F1E90FF)
                            )
                        )

                        NavigationBarItem(
                            selected = currentScreen == AppScreen.DOWNLOADS,
                            onClick = { viewModel.setScreen(AppScreen.DOWNLOADS) },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == AppScreen.DOWNLOADS) Icons.Filled.Download else Icons.Outlined.Download,
                                    contentDescription = "Downloads"
                                )
                            },
                            label = { Text("Downloads", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SpotificBluePrimary,
                                selectedTextColor = SpotificBluePrimary,
                                unselectedIconColor = SpotificTextMuted,
                                unselectedTextColor = SpotificTextMuted,
                                indicatorColor = Color(0x1F1E90FF)
                            )
                        )

                        NavigationBarItem(
                            selected = currentScreen == AppScreen.ABOUT,
                            onClick = { viewModel.setScreen(AppScreen.ABOUT) },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == AppScreen.ABOUT) Icons.Filled.Info else Icons.Outlined.Info,
                                    contentDescription = "About"
                                )
                            },
                            label = { Text("About", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SpotificBluePrimary,
                                selectedTextColor = SpotificBluePrimary,
                                unselectedIconColor = SpotificTextMuted,
                                unselectedTextColor = SpotificTextMuted,
                                indicatorColor = Color(0x1F1E90FF)
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .statusBarsPadding()
            ) {
                when (currentScreen) {
                    AppScreen.HOME -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToSearch = { viewModel.setScreen(AppScreen.SEARCH) }
                    )
                    AppScreen.SEARCH -> SearchScreen(viewModel = viewModel)
                    AppScreen.LIBRARY -> LibraryScreen(viewModel = viewModel)
                    AppScreen.DOWNLOADS -> DownloadsScreen(viewModel = viewModel)
                    AppScreen.ABOUT -> AboutScreen()
                }
            }
        }

        // Full Player Modal Screen
        AnimatedVisibility(
            visible = isFullPlayerVisible,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            FullPlayerScreen(
                viewModel = viewModel,
                onCollapse = { viewModel.setFullPlayerVisible(false) }
            )
        }

        // Buffering Overlay
        BufferingOverlay(
            isBuffering = playbackState.isBuffering,
            statusText = "Connecting Stream..."
        )
    }
}
