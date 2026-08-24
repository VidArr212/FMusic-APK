package com.fmusic.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fmusic.app.data.model.TrackItem
import com.fmusic.app.player.PlayerState
import com.fmusic.app.ui.theme.*

@Composable
fun MiniPlayer(
    state: PlayerState,
    isFavorite: Boolean,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = state.currentTrack ?: return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .clickable { onClick() }
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Artwork
                AsyncImage(
                    model = track.thumbnail,
                    contentDescription = track.title,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurface),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Artist
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = track.getCleanTitle(),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = TextWhite
                    )
                    Text(
                        text = track.getDisplayArtist(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = TextGray
                    )
                }

                // Heart (Favorite)
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) NeonCyan else TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Add to Playlist (+)
                IconButton(
                    onClick = onAddToPlaylistClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = "Add to playlist",
                        tint = TextGray,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Play / Pause
                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    if (state.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = NeonCyan,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = TextWhite,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Skip Next
                IconButton(
                    onClick = onNextClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = TextWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Neon Blue Progress Bar (Spotify bottom line)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(DarkSurface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(state.progress)
                        .fillMaxHeight()
                        .background(NeonCyan)
                )
            }
        }
    }
}
