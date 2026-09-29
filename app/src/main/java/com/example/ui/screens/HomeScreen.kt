package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Track
import com.example.ui.components.GlassCard
import com.example.ui.theme.SpotificBluePrimary
import com.example.ui.theme.SpotificBlueSecondary
import com.example.ui.theme.SpotificCardSurface
import com.example.ui.theme.SpotificDarkBg
import com.example.ui.theme.SpotificTextMuted
import com.example.ui.theme.SpotificTextPrimary
import com.example.ui.theme.SpotificTextSecondary
import com.example.ui.viewmodel.ArtistItem
import com.example.ui.viewmodel.SpotificViewModel
import java.util.Calendar

@Composable
fun HomeScreen(
    viewModel: SpotificViewModel,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val featuredTracks by viewModel.featuredTracks.collectAsState()
    val trendingTracks by viewModel.trendingTracks.collectAsState()
    val biggestHits by viewModel.biggestHits.collectAsState()
    val chillTracks by viewModel.chillTracks.collectAsState()
    val isLoading by viewModel.isHomeLoading.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 4..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SpotificDarkBg),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        // Header: Greeting + Search icon (NO bell icon as required)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = greeting,
                        color = SpotificTextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Music tailored for right now",
                        color = SpotificTextMuted,
                        fontSize = 13.sp
                    )
                }

                IconButton(
                    onClick = onNavigateToSearch,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = SpotificTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Loading state
        if (isLoading && featuredTracks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = SpotificBluePrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading Spotific stream...",
                            color = SpotificTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Featured Hero Tracks (horizontal banner)
        if (featuredTracks.isNotEmpty()) {
            item {
                Text(
                    text = "Featured Now",
                    color = SpotificTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(featuredTracks) { track ->
                        HeroTrackCard(
                            track = track,
                            isPlaying = playbackState.currentTrack?.id == track.id && playbackState.isPlaying,
                            onClick = { viewModel.playTrack(track, featuredTracks) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // Top Artists Horizontal Row
        item {
            Text(
                text = "Top Artists",
                color = SpotificTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(viewModel.topArtists) { artist ->
                    ArtistCircleCard(
                        artist = artist,
                        onClick = { viewModel.searchCategory(artist.name) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(22.dp))
        }

        // Trending Now section
        if (trendingTracks.isNotEmpty()) {
            item {
                SectionHeader(title = "Trending Now", onMoreClick = { viewModel.searchCategory("Top Hits") })

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(trendingTracks) { track ->
                        SquareTrackCard(
                            track = track,
                            isPlaying = playbackState.currentTrack?.id == track.id && playbackState.isPlaying,
                            onClick = { viewModel.playTrack(track, trendingTracks) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))
            }
        }

        // Today's Biggest Hits section
        if (biggestHits.isNotEmpty()) {
            item {
                SectionHeader(title = "Today's Biggest Hits", onMoreClick = { viewModel.searchCategory("Pop") })

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(biggestHits) { track ->
                        SquareTrackCard(
                            track = track,
                            isPlaying = playbackState.currentTrack?.id == track.id && playbackState.isPlaying,
                            onClick = { viewModel.playTrack(track, biggestHits) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))
            }
        }

        // Chill & Focus section
        if (chillTracks.isNotEmpty()) {
            item {
                SectionHeader(title = "Chill & Focus", onMoreClick = { viewModel.searchCategory("Lo-Fi") })

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(chillTracks) { track ->
                        SquareTrackCard(
                            track = track,
                            isPlaying = playbackState.currentTrack?.id == track.id && playbackState.isPlaying,
                            onClick = { viewModel.playTrack(track, chillTracks) }
                        )
                    }
                }
            }
        }

        // Reload helper if empty
        if (!isLoading && trendingTracks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Connect to internet to load feed", color = SpotificTextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        IconButton(
                            onClick = { viewModel.loadHomeFeed() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SpotificBluePrimary)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = SpotificTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "See all",
            color = SpotificBluePrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clickable(onClick = onMoreClick)
                .padding(4.dp)
        )
    }
}

@Composable
fun HeroTrackCard(
    track: Track,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .width(280.dp)
            .height(150.dp)
            .clickable(onClick = onClick),
        cornerRadius = 18.dp,
        backgroundColor = Color(0x18FFFFFF)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(126.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF222230)),
                contentAlignment = Alignment.Center
            ) {
                if (!track.thumbnail.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(track.thumbnail)
                            .crossfade(true)
                            .build(),
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(126.dp)
                    )
                } else {
                    Text("🎧", fontSize = 28.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "FEATURED",
                        color = SpotificBluePrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.title,
                        color = SpotificTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.artist,
                        color = SpotificTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(SpotificBlueSecondary, SpotificBluePrimary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SquareTrackCard(
    track: Track,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1E1E28)),
            contentAlignment = Alignment.Center
        ) {
            if (!track.thumbnail.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(track.thumbnail)
                        .crossfade(true)
                        .build(),
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(140.dp)
                )
            } else {
                Text("🎵", fontSize = 24.sp)
            }

            // Play overlay button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) SpotificBluePrimary else Color(0xCC0A0A0F)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = track.title,
            color = if (isPlaying) SpotificBluePrimary else SpotificTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            color = SpotificTextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ArtistCircleCard(
    artist: ArtistItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(88.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFF222230)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artist.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = artist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(80.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = artist.name,
            color = SpotificTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
