package com.fmusic.app.ui.screens.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fmusic.app.data.model.TrackItem
import com.fmusic.app.ui.components.ShimmerBox
import com.fmusic.app.ui.theme.*

@Composable
fun LyricsScreen(
    track: TrackItem,
    currentPositionMs: Long,
    onSeek: (Long) -> Unit,
    onClose: () -> Unit,
    viewModel: LyricsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(track.videoId) {
        viewModel.loadLyrics(track)
    }

    // Determine current active lyric line index
    val activeIndex = remember(currentPositionMs, uiState.syncedLines) {
        val lines = uiState.syncedLines
        if (lines.isEmpty()) -1
        else {
            val idx = lines.indexOfLast { it.timeMs <= currentPositionMs }
            if (idx >= 0) idx else 0
        }
    }

    // Auto-scroll to active lyric line
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && uiState.syncedLines.isNotEmpty()) {
            listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.getCleanTitle(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextWhite,
                    maxLines = 1
                )
                Text(
                    text = track.getDisplayArtist(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    maxLines = 1
                )
            }

            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = TextWhite)
            }
        }

        // Lyrics Content
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                repeat(8) {
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(28.dp)
                            .padding(vertical = 4.dp),
                        cornerRadius = 6.dp
                    )
                }
            }
        } else if (uiState.syncedLines.isNotEmpty()) {
            // Realtime Synchronized Lyrics (Karaoke Style)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentPadding = PaddingValues(vertical = 40.dp)
            ) {
                itemsIndexed(uiState.syncedLines) { index, line ->
                    val isActive = index == activeIndex

                    Text(
                        text = line.text.ifBlank { "♪" },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                            fontSize = if (isActive) 24.sp else 18.sp,
                            lineHeight = if (isActive) 34.sp else 26.sp
                        ),
                        color = if (isActive) NeonCyan else TextMuted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSeek(line.timeMs) }
                            .padding(vertical = 10.dp)
                    )
                }

                item {
                    if (!uiState.source.isNullOrBlank()) {
                        Text(
                            text = "Sumber lirik: ${uiState.source}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 24.dp, bottom = 40.dp)
                        )
                    }
                }
            }
        } else if (!uiState.plainLyrics.isNullOrBlank()) {
            // Plain Lyrics
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = uiState.plainLyrics!!,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        lineHeight = 28.sp
                    ),
                    color = TextWhite
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (!uiState.source.isNullOrBlank()) {
                    Text(
                        text = "Sumber lirik: ${uiState.source}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        } else {
            // Not Found
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.errorMessage ?: "Lirik tidak tersedia.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
