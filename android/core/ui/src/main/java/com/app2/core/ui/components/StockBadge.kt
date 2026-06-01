package com.app2.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app2.core.ui.theme.Error
import com.app2.core.ui.theme.Info
import com.app2.core.ui.theme.Success
import com.app2.core.ui.theme.Warning

enum class BadgeVariant {
    Success, Warning, Error, Info, Neutral
}

@Immutable
data class BadgeColors(
    val text: Color,
    val background: Color
)

@Composable
fun BadgeVariant.colors(): BadgeColors = when (this) {
    BadgeVariant.Success -> BadgeColors(Success, Success.copy(alpha = 0.15f))
    BadgeVariant.Warning -> BadgeColors(Warning, Warning.copy(alpha = 0.15f))
    BadgeVariant.Error -> BadgeColors(Error, Error.copy(alpha = 0.15f))
    BadgeVariant.Info -> BadgeColors(Info, Info.copy(alpha = 0.15f))
    BadgeVariant.Neutral -> BadgeColors(
        MaterialTheme.colorScheme.onSurfaceVariant,
        MaterialTheme.colorScheme.surfaceVariant
    )
}

@Composable
fun StockBadge(
    text: String,
    variant: BadgeVariant = BadgeVariant.Info,
    modifier: Modifier = Modifier
) {
    val colors = variant.colors()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.background)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.text
        )
    }
}
