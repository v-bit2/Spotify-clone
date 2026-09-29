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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.TrackListItem
import com.example.ui.theme.SpotificBluePrimary
import com.example.ui.theme.SpotificBlueSecondary
import com.example.ui.theme.SpotificCardSurface
import com.example.ui.theme.SpotificDarkBg
import com.example.ui.theme.SpotificTextMuted
import com.example.ui.theme.SpotificTextPrimary
import com.example.ui.theme.SpotificTextSecondary
import com.example.ui.viewmodel.SpotificViewModel

data class CategoryItem(
    val title: String,
    val gradientColors: List<Color>
)

val GENRE_CATEGORIES = listOf(
    CategoryItem("Pop", listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))),
    CategoryItem("Hip-Hop", listOf(Color(0xFFE52D27), Color(0xFFB31217))),
    CategoryItem("Rock", listOf(Color(0xFF1E90FF), Color(0xFF0052D4))),
    CategoryItem("Electronic", listOf(Color(0xFF00C6FF), Color(0xFF0072FF))),
    CategoryItem("R&B", listOf(Color(0xFFFF416C), Color(0xFFFF4B2B))),
    CategoryItem("Dance", listOf(Color(0xFFF7971E), Color(0xFFFFD200))),
    CategoryItem("Latin", listOf(Color(0xFF11998E), Color(0xFF38EF7D))),
    CategoryItem("Lo-Fi & Chill", listOf(Color(0xFF4FACFE), Color(0xFF00F2FE))),
    CategoryItem("Indie", listOf(Color(0xFF43C6AC), Color(0xFF191654))),
    CategoryItem("Workout", listOf(Color(0xFFFF512F), Color(0xFFDD2476)))
)

@Composable
fun SearchScreen(
    viewModel: SpotificViewModel,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val downloads by viewModel.downloads.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpotificDarkBg)
    ) {
        // Pill-shaped Search Bar ("What do you want to listen to?")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = {
                    Text(
                        text = "What do you want to listen to?",
                        color = SpotificTextMuted,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = SpotificBluePrimary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = SpotificTextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpotificBluePrimary,
                    unfocusedBorderColor = Color(0x33FFFFFF),
                    focusedContainerColor = Color(0xFF181824),
                    unfocusedContainerColor = Color(0xFF14141E),
                    focusedTextColor = SpotificTextPrimary,
                    unfocusedTextColor = SpotificTextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Body: Categories or Search Results
        if (searchQuery.trim().isEmpty()) {
            // Genre Category Grid
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Browse all",
                    color = SpotificTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 110.dp)
                ) {
                    items(GENRE_CATEGORIES) { category ->
                        CategoryCard(
                            category = category,
                            onClick = {
                                keyboardController?.hide()
                                viewModel.searchCategory(category.title)
                            }
                        )
                    }
                }
            }
        } else {
            // Search Results State
            if (isSearching) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = SpotificBluePrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Searching...",
                            color = SpotificTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MusicOff,
                            contentDescription = "No Results",
                            tint = SpotificTextMuted,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No results found for \"$searchQuery\"",
                            color = SpotificTextSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Please try another search or category",
                            color = SpotificTextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp + 100.dp)
                ) {
                    item {
                        Text(
                            text = "Top results",
                            color = SpotificTextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    items(searchResults) { track ->
                        val isPlaying = playbackState.currentTrack?.id == track.id && playbackState.isPlaying
                        val isFav = favorites.any { it.id == track.id }
                        val isDown = downloads.any { it.id == track.id }
                        val progress = downloadProgress[track.id]

                        TrackListItem(
                            track = track,
                            isPlaying = isPlaying,
                            isFavorite = isFav,
                            isDownloaded = isDown,
                            downloadProgress = progress,
                            onTrackClick = {
                                viewModel.playTrack(track, searchResults)
                            },
                            onFavoriteClick = {
                                viewModel.toggleFavorite(track)
                            },
                            onDownloadClick = {
                                viewModel.downloadTrack(track)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryCard(
    category: CategoryItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(colors = category.gradientColors))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Text(
            text = category.title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
}
