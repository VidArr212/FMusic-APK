package com.fmusic.app.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fmusic.app.data.model.TrackItem
import com.fmusic.app.ui.components.*
import com.fmusic.app.ui.theme.*

@Composable
fun SearchScreen(
    onTrackClick: (TrackItem, List<TrackItem>) -> Unit,
    onBrowseClick: (browseId: String, title: String?, type: String?) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    val filterOptions = listOf("All", "Songs", "Videos", "Albums", "Artists", "Playlists")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
    ) {
        // Title "Search"
        Text(
            text = "Search",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 28.sp
            ),
            color = TextWhite,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Search Input Bar (Spotify Style)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = { viewModel.onQueryChange(it) },
                placeholder = {
                    Text(
                        text = "What do you want to play?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = if (uiState.query.isNotEmpty()) NeonCyan else TextGray
                    )
                },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearQuery() }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear",
                                tint = TextWhite
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurface,
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    cursorColor = NeonCyan
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    viewModel.performSearch(uiState.query, uiState.selectedFilter)
                }),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Filter Chips (Songs, Videos, Albums, Artists, Playlists)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterOptions) { filter ->
                val isSelected = (filter == "All" && uiState.selectedFilter == null) ||
                        (uiState.selectedFilter == filter)

                FilterChip(
                    selected = isSelected,
                    onClick = {
                        viewModel.onFilterSelect(if (filter == "All") null else filter)
                    },
                    label = {
                        Text(
                            text = filter,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan,
                        selectedLabelColor = DarkBackground,
                        containerColor = DarkSurfaceVariant,
                        labelColor = TextWhite
                    ),
                    border = null,
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        // Content Area
        if (uiState.isSearching) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(6) {
                    ShimmerTrackItem()
                }
            }
        } else if (uiState.query.isNotBlank() && uiState.searchResults.isNotEmpty()) {
            // Search Results
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                uiState.searchResults.forEach { section ->
                    item {
                        if (!section.title.isNullOrBlank()) {
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextWhite,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                    val items = section.items ?: emptyList()
                    items(items) { track ->
                        TrackListItem(
                            track = track,
                            onClick = {
                                if (track.videoId != null) {
                                    onTrackClick(track, items)
                                } else if (track.browseId != null) {
                                    onBrowseClick(track.browseId, track.title, track.browseType ?: track.type)
                                }
                            }
                        )
                    }
                }
            }
        } else if (uiState.query.isNotBlank() && uiState.suggestions.isNotEmpty()) {
            // Search Suggestions Dropdown
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(uiState.suggestions) { suggestion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                focusManager.clearFocus()
                                viewModel.onQueryChange(suggestion)
                                viewModel.performSearch(suggestion, uiState.selectedFilter)
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextWhite
                        )
                    }
                }
            }
        } else {
            // Idle / Discovery View (Matching Screenshot 1)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                // 1. Recent Searches Tags
                if (uiState.recentSearches.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent searches",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextWhite
                            )
                            Text(
                                text = "Clear",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { viewModel.clearAllRecentSearches() }
                                    .padding(4.dp)
                            )
                        }

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.recentSearches) { search ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(DarkSurfaceVariant)
                                        .clickable {
                                            viewModel.onQueryChange(search.query)
                                            viewModel.performSearch(search.query, uiState.selectedFilter)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.History,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = search.query,
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextWhite)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Delete",
                                        tint = TextMuted,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { viewModel.deleteRecentSearch(search.query) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Recently Played Tracks
                if (uiState.recentlyPlayed.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Recently played",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextWhite,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    val recentTracks = uiState.recentlyPlayed.map { it.toTrackItem() }
                    items(recentTracks.take(8)) { track ->
                        TrackListItem(
                            track = track,
                            onClick = {
                                onTrackClick(track, recentTracks)
                            }
                        )
                    }
                }

                // 3. Browse All Categories (Vibrant Genre / Mood Cards)
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Browse all",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = TextWhite,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                val categories = uiState.categories.ifEmpty {
                    listOf(
                        com.fmusic.app.data.model.MoodCategory("Bepergian", "#E6A100", "FEmusic_moods_and_genres"),
                        com.fmusic.app.data.model.MoodCategory("Fokus", "#4A5568", "FEmusic_moods_and_genres"),
                        com.fmusic.app.data.model.MoodCategory("Gaming", "#00B4D8", "FEmusic_moods_and_genres"),
                        com.fmusic.app.data.model.MoodCategory("Pop Indo", "#7209B7", "FEmusic_moods_and_genres"),
                        com.fmusic.app.data.model.MoodCategory("Santai", "#2A9D8F", "FEmusic_moods_and_genres"),
                        com.fmusic.app.data.model.MoodCategory("Workout", "#E76F51", "FEmusic_moods_and_genres")
                    )
                }

                // Render 2-column Category Cards in rows
                // Use itemsIndexed over pairs instead of items(List<List<>>) to avoid type crash
                for (i in categories.indices step 2) {
                    item {
                        val first = categories[i]
                        val second = if (i + 1 < categories.size) categories[i + 1] else null
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val firstId = first.browseId
                            CategoryCard(
                                category = first,
                                onClick = {
                                    if (!firstId.isNullOrBlank()) {
                                        onBrowseClick(firstId, first.title, "mood")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (second != null) {
                                val secondId = second.browseId
                                CategoryCard(
                                    category = second,
                                    onClick = {
                                        if (!secondId.isNullOrBlank()) {
                                            onBrowseClick(secondId, second.title, "mood")
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

