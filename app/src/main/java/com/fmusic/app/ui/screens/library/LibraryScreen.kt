package com.fmusic.app.ui.screens.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fmusic.app.data.model.TrackItem
import com.fmusic.app.ui.components.*
import com.fmusic.app.ui.theme.*

@Composable
fun LibraryScreen(
    onTrackClick: (TrackItem, List<TrackItem>) -> Unit,
    onPlaylistClick: (playlistId: Long, name: String) -> Unit,
    viewModel: LibraryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    val tabs = listOf("Playlists", "Favorites", "Saved", "History", "Stats")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
    ) {
        // Header "Library" (Matching Screenshot 2)
        Text(
            text = "Library",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 28.sp
            ),
            color = TextWhite,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Filter Tabs [Playlists, Favorites, Saved, History, Stats]
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabs) { tab ->
                val isSelected = uiState.selectedTab == tab
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectTab(tab) },
                    label = {
                        Text(
                            text = tab,
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

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons Row: [+ New playlist] [Import from YT Music] (Matching Screenshot 2)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { showCreatePlaylistDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = DarkBackground
                ),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "New playlist", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { showImportDialog = true },
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, DarkSurfaceElevated),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.weight(1.3f)
            ) {
                Icon(imageVector = Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Import from YT Music", style = MaterialTheme.typography.labelMedium)
            }
        }

        // Sub Buttons: [Backup] [Restore]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = { },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = DarkSurfaceVariant,
                    contentColor = TextWhite
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(imageVector = Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Backup", style = MaterialTheme.typography.labelSmall)
            }

            FilledTonalButton(
                onClick = { },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = DarkSurfaceVariant,
                    contentColor = TextWhite
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(imageVector = Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Restore", style = MaterialTheme.typography.labelSmall)
            }
        }

        HorizontalDivider(color = DarkSurfaceElevated, modifier = Modifier.padding(vertical = 4.dp))

        // Content Area according to selected tab
        when (uiState.selectedTab) {
            "Playlists" -> {
                if (uiState.playlists.isEmpty()) {
                    // Empty state (Matching Screenshot 2)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No playlists yet",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextWhite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Use New playlist above, or import one from YouTube Music.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 90.dp)
                    ) {
                        items(uiState.playlists) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPlaylistClick(playlist.id, playlist.name) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LibraryMusic,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextWhite
                                    )
                                    Text(
                                        text = playlist.description ?: "Custom Playlist",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextGray
                                    )
                                }
                                IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Delete",
                                        tint = TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            "Favorites" -> {
                val favoriteTracks = uiState.favorites.map { it.toTrackItem() }
                if (favoriteTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Belum Ada Lagu Favorit",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextWhite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ketuk ikon hati pada lagu untuk menyimpannya di sini.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 90.dp)
                    ) {
                        items(favoriteTracks) { track ->
                            TrackListItem(
                                track = track,
                                onClick = { onTrackClick(track, favoriteTracks) }
                            )
                        }
                    }
                }
            }

            "Saved" -> {
                val offlineTracks = uiState.savedOffline.map { it.toTrackItem() }
                if (offlineTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Belum Ada Lagu Offline",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextWhite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Simpan lagu favorit untuk didengarkan tanpa koneksi internet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 90.dp)
                    ) {
                        items(offlineTracks) { track ->
                            TrackListItem(
                                track = track,
                                onClick = { onTrackClick(track, offlineTracks) },
                                onOptionClick = {
                                    if (track.videoId != null) {
                                        viewModel.deleteOfflineTrack(track.videoId)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            "History" -> {
                val historyTracks = uiState.history.map { it.toTrackItem() }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(historyTracks) { track ->
                        TrackListItem(
                            track = track,
                            onClick = { onTrackClick(track, historyTracks) }
                        )
                    }
                }
            }

            "Stats" -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Statistik Musik Anda",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Lagu Favorit: ${uiState.favorites.size}", style = MaterialTheme.typography.bodyLarge, color = TextWhite)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Lagu Offline Tersimpan: ${uiState.savedOffline.size}", style = MaterialTheme.typography.bodyLarge, color = NeonCyan)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Playlist Dibuat: ${uiState.playlists.size}", style = MaterialTheme.typography.bodyLarge, color = TextWhite)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Riwayat Diputar: ${uiState.history.size} lagu", style = MaterialTheme.typography.bodyLarge, color = TextGray)
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = { name, desc ->
                viewModel.createPlaylist(name, desc)
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = DarkSurfaceVariant,
            title = {
                Text(text = "Import dari YT Music", style = MaterialTheme.typography.titleMedium, color = TextWhite)
            },
            text = {
                Text(
                    text = "Fitur import link YouTube Music akan segera hadir dalam pembaruan berikutnya.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
            },
            confirmButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("OK", color = NeonCyan)
                }
            }
        )
    }
}
