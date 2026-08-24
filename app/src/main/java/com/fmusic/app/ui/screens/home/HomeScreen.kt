package com.fmusic.app.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fmusic.app.R
import com.fmusic.app.data.model.TrackItem
import com.fmusic.app.ui.components.*
import com.fmusic.app.ui.theme.*

@Composable
fun HomeScreen(
    onTrackClick: (TrackItem, List<TrackItem>) -> Unit,
    onBrowseClick: (browseId: String, title: String?, type: String?) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
    ) {
        // Top Brand Header (FMusic Logo + Title + Settings)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ic_fmusic_logo),
                    contentDescription = "FMusic Logo",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "FMusic",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        ),
                        color = TextWhite
                    )
                    Text(
                        text = "Mix Indo & Trending",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonCyan
                    )
                }
            }

            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = TextGray
                )
            }
        }

        // Content
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
                        text = "Gagal memuat musik",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.errorMessage ?: "Periksa koneksi server proxy API",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadHomeData() },
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
                items(uiState.sections) { section ->
                    val items = section.items ?: emptyList()
                    if (items.isNotEmpty()) {
                        SectionHeader(
                            title = section.title ?: "Rekomendasi",
                            onSeeAllClick = null
                        )

                        if (section.isList == true) {
                            // Render list items
                            Column(modifier = Modifier.fillMaxWidth()) {
                                items.take(6).forEach { track ->
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
                        } else {
                            // Horizontal Carousel
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
                                        },
                                        onPlayClick = if (track.videoId != null) {
                                            { onTrackClick(track, items) }
                                        } else null
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}
