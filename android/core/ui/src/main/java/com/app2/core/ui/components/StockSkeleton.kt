package com.app2.core.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun StockSkeletonCard(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        StockSkeletonLine(width = 0.6f)
        Spacer(Modifier.height(8.dp))
        StockSkeletonLine(width = 0.4f, height = 28.dp)
    }
}

@Composable
fun StockSkeletonRow(
    withCircle: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (withCircle) {
            StockSkeletonCircle()
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            StockSkeletonLine(width = 0.7f)
            Spacer(Modifier.height(6.dp))
            StockSkeletonLine(width = 0.4f)
        }
    }
}

@Composable
fun StockSkeletonCircle(
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .shimmerEffect()
    )
}

@Composable
fun StockSkeletonLine(
    width: Float = 1f,
    height: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth(width)
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .shimmerEffect()
    )
}

@Composable
private fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val shimmerColors = listOf(
        surfaceColor.copy(alpha = 0.3f),
        surfaceColor.copy(alpha = 0.5f),
        surfaceColor.copy(alpha = 0.3f)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    return this.then(Modifier.background(brush))
}

@Composable
fun StockSkeletonGrid(
    columns: Int = 2,
    count: Int = 6,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        for (i in 0 until count step columns) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (j in 0 until columns) {
                    if (i + j < count) {
                        StockSkeletonCard(
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (j == 0) Modifier.width(0.dp) else Modifier
                                )
                        )
                        if (j == 0) Spacer(Modifier.width(12.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun StockSkeletonList(
    count: Int = 8,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count) {
            StockSkeletonRow(withCircle = true)
        }
    }
}
