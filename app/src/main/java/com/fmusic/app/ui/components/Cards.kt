package com.fmusic.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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
import com.fmusic.app.data.model.MoodCategory
import com.fmusic.app.data.model.TrackItem
import com.fmusic.app.ui.theme.*

@Composable
fun AlbumCard(
    track: TrackItem,
    onClick: () -> Unit,
    onPlayClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isArtist = track.type == "artist"

    Column(
        modifier = modifier
            .width(148.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(136.dp)
                .clip(if (isArtist) CircleShape else RoundedCornerShape(10.dp))
                .background(DarkSurfaceVariant)
        ) {
            AsyncImage(
                model = track.thumbnail,
                contentDescription = track.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Optional Quick Play Button
            if (onPlayClick != null && !isArtist) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NeonCyan)
                        .clickable { onPlayClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = DarkBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = track.getCleanTitle(),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = TextWhite
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = track.getDisplayArtist(),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = TextGray
        )
    }
}

@Composable
fun CategoryCard(
    category: MoodCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardColor = try {
        if (!category.color.isNullOrBlank()) {
            val hex = if (category.color.startsWith("#")) category.color else "#${category.color}"
            val parsed = android.graphics.Color.parseColor(hex)
            Color(parsed.toLong() and 0xFFFFFFFFL)
        } else {
            DarkSurfaceVariant
        }
    } catch (e: Exception) {
        DarkSurfaceVariant
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(cardColor)
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Text(
            text = category.title ?: "Genre",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = 16.sp
            ),
            color = TextWhite,
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    onSeeAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            color = TextWhite
        )

        if (onSeeAllClick != null) {
            Text(
                text = "Lihat Semua",
                style = MaterialTheme.typography.labelMedium,
                color = NeonCyan,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onSeeAllClick() }
                    .padding(4.dp)
            )
        }
    }
}
