package com.fmusic.app.ui.screens.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fmusic.app.data.model.TrackItem
import com.fmusic.app.ui.components.*
import com.fmusic.app.ui.theme.*

@Composable
fun BrowseDetailScreen(
    browseId: String,
    titleHint: String? = null,
    typeHint: String? = null,
    onBack: () -> Unit,
    onTrackClick: (TrackItem, List<TrackItem>) -> Unit,
    viewModel: BrowseViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(browseId) {
        viewModel.loadBrowse(browseId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Back Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
            }
            Text(
                text = uiState.header?.title ?: titleHint ?: typeHint?.replaceFirstChar { it.uppercase() } ?: "Details",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (uiState.isLoading) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                ShimmerBox(modifier = Modifier.size(180.dp).align(Alignment.CenterHorizontally), cornerRadius = 16.dp)
                Spacer(modifier = Modifier.height(16.dp))
                repeat(5) {
                    ShimmerTrackItem()
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                // Header Banner
                item {
                    val header = uiState.header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(DarkSurfaceVariant, DarkBackground)
                                )
                            )
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(190.dp)
                                .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = NeonCyan)
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurface)
                        ) {
                            AsyncImage(
                                model = header?.thumbnail,
                                contentDescription = header?.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = header?.title ?: titleHint ?: "Album",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp
                            ),
                            color = TextWhite,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = header?.subtitle ?: "Koleksi Lagu",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextGray
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Play & Shuffle Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (uiState.tracks.isNotEmpty()) {
                                        onTrackClick(uiState.tracks.first(), uiState.tracks)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(24.dp),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                            ) {
                                Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, tint = DarkBackground)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Putar Semua", color = DarkBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Tracks List
                itemsIndexed(uiState.tracks) { index, track ->
                    TrackListItem(
                        track = track,
                        index = index + 1,
                        onClick = { onTrackClick(track, uiState.tracks) }
                    )
                }
            }
        }
    }
}
