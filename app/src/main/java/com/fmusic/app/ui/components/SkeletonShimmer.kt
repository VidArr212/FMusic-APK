package com.fmusic.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fmusic.app.ui.theme.DarkSurface
import com.fmusic.app.ui.theme.DarkSurfaceVariant

@Composable
fun shimmerBrush(showShimmer: Boolean = true, targetValue: Float = 1000f): Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            DarkSurface,
            DarkSurfaceVariant,
            DarkSurface
        )

        val transition = rememberInfiniteTransition(label = "shimmerTransition")
        val translateAnimation = transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1100, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmerTranslate"
        )

        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnimation.value, y = translateAnimation.value)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = Offset.Zero,
            end = Offset.Zero
        )
    }
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(shimmerBrush())
    )
}

@Composable
fun ShimmerTrackItem(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerBox(modifier = Modifier.size(52.dp), cornerRadius = 8.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f).height(16.dp), cornerRadius = 4.dp)
            Spacer(modifier = Modifier.height(6.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.4f).height(12.dp), cornerRadius = 4.dp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        ShimmerBox(modifier = Modifier.size(24.dp), cornerRadius = 12.dp)
    }
}

@Composable
fun ShimmerAlbumCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(140.dp)
            .padding(8.dp)
    ) {
        ShimmerBox(modifier = Modifier.size(140.dp), cornerRadius = 12.dp)
        Spacer(modifier = Modifier.height(8.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(14.dp), cornerRadius = 4.dp)
        Spacer(modifier = Modifier.height(4.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.6f).height(12.dp), cornerRadius = 4.dp)
    }
}

@Composable
fun ShimmerSection() {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        ShimmerBox(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .width(180.dp)
                .height(24.dp),
            cornerRadius = 6.dp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            repeat(3) {
                ShimmerAlbumCard()
            }
        }
    }
}
