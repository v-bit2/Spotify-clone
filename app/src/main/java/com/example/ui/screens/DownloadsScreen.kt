package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.TrackListItem
import com.example.ui.theme.SpotificDarkBg
import com.example.ui.theme.SpotificTextMuted
import com.example.ui.theme.SpotificTextPrimary
import com.example.ui.theme.SpotificTextSecondary
import com.example.ui.viewmodel.SpotificViewModel

@Composable
fun DownloadsScreen(
    viewModel: SpotificViewModel,
    modifier: Modifier = Modifier
) {
    val downloads by viewModel.downloads.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpotificDarkBg)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Downloaded Music",
                color = SpotificTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${downloads.size} tracks saved offline",
                color = SpotificTextMuted,
                fontSize = 13.sp
            )
        }

        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 110.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.FileDownload,
                        contentDescription = "No Downloads",
                        tint = SpotificTextMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No downloads yet",
                        color = SpotificTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap the download icon on any song to listen offline",
                        color = SpotificTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp + 100.dp)
            ) {
                items(downloads) { track ->
                    val isPlaying = playbackState.currentTrack?.id == track.id && playbackState.isPlaying
                    val isFav = favorites.any { it.id == track.id }

                    TrackListItem(
                        track = track,
                        isPlaying = isPlaying,
                        isFavorite = isFav,
                        isDownloaded = true,
                        onTrackClick = {
                            viewModel.playTrack(track, downloads)
                        },
                        onFavoriteClick = {
                            viewModel.toggleFavorite(track)
                        },
                        onDeleteClick = {
                            viewModel.removeDownload(track.id)
                        }
                    )
                }
            }
        }
    }
}
