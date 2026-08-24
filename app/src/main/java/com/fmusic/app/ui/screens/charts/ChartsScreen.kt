package com.fmusic.app.ui.screens.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fmusic.app.data.model.TrackItem
import com.fmusic.app.ui.components.*
import com.fmusic.app.ui.theme.*

@Composable
fun ChartsScreen(
    onTrackClick: (TrackItem, List<TrackItem>) -> Unit,
    onBrowseClick: (browseId: String, title: String?, type: String?) -> Unit,
    viewModel: ChartsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
    ) {
        // Title Header
        Text(
            text = "Charts & Tangga Lagu",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 28.sp
            ),
            color = TextWhite,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (uiState.isLoading) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(4) {
                    ShimmerSection()
                }
            }
        } else if (uiState.errorMessage != null && uiState.sections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Gagal memuat charts",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.errorMessage ?: "Periksa koneksi server",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadCharts() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, tint = DarkBackground)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Coba Lagi", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                uiState.sections.forEach { section ->
                    val items = section.items ?: emptyList()
                    if (items.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = section.title ?: "Tangga Lagu",
                                onSeeAllClick = null
                            )
                        }

                        if (section.isList == true || (section.title?.contains("Tangga Lagu", ignoreCase = true) == true) || (section.title?.contains("Top", ignoreCase = true) == true)) {
                            // Render numbered top track list (1, 2, 3...)
                            itemsIndexed(items.take(10)) { index, track ->
                                TrackListItem(
                                    track = track,
                                    index = index + 1,
                                    onClick = {
                                        if (track.videoId != null) {
                                            onTrackClick(track, items)
                                        } else if (track.browseId != null) {
                                            onBrowseClick(track.browseId, track.title, track.browseType ?: track.type)
                                        }
                                    }
                                )
                            }
                        } else {
                            // Render horizontal artist/video carousel
                            item {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    items(items) { track ->
                                        AlbumCard(
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
                        }

                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}
