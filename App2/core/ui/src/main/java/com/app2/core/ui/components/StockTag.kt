package com.app2.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app2.core.ui.theme.Success

enum class TagVariant {
    Brand, Surface, Success
}

@Composable
fun StockTag(
    text: String,
    variant: TagVariant = TagVariant.Brand,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val containerColor = when (variant) {
        TagVariant.Brand -> MaterialTheme.colorScheme.primary
        TagVariant.Surface -> MaterialTheme.colorScheme.surfaceVariant
        TagVariant.Success -> Success
    }
    val contentColor = when (variant) {
        TagVariant.Brand -> MaterialTheme.colorScheme.onPrimary
        TagVariant.Surface -> MaterialTheme.colorScheme.onSurfaceVariant
        TagVariant.Success -> Color.White
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = contentColor
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}
