package com.app2.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StockCard(
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        accentColor != null -> accentColor.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outline
    }

    val shape = RoundedCornerShape(12.dp)
    val cardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    )
    val border = BorderStroke(if (accentColor != null || isSelected) 1.dp else 0.5.dp, borderColor)

    if (onClick != null) {
        Card(
            modifier = modifier
                .shadow(if (isSelected) 8.dp else 4.dp, shape, clip = false),
            shape = shape,
            colors = cardColors,
            border = border,
            onClick = onClick
        ) {
            CardContent(accentColor, content)
        }
    } else {
        Card(
            modifier = modifier
                .shadow(if (isSelected) 8.dp else 4.dp, shape, clip = false),
            shape = shape,
            colors = cardColors,
            border = border
        ) {
            CardContent(accentColor, content)
        }
    }
}

@Composable
private fun CardContent(
    accentColor: Color?,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.padding(12.dp)) {
        if (accentColor != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .width(4.dp)
                    .align(Alignment.CenterStart)
            )
        }
        content()
    }
}

@Composable
fun StockKPICard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    StockCard(
        modifier = modifier,
        accentColor = color
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
