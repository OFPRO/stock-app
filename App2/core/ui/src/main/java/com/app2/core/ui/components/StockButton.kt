package com.app2.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app2.core.ui.theme.Accent
import com.app2.core.ui.theme.Brand
import com.app2.core.ui.theme.Error

enum class ButtonVariant {
    Primary, Secondary, Danger, Ghost
}

@Composable
fun StockButton(
    text: String,
    onClick: () -> Unit,
    variant: ButtonVariant = ButtonVariant.Primary,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding
) {
    val colors = when (variant) {
        ButtonVariant.Primary -> ButtonDefaults.buttonColors(
            containerColor = Brand,
            contentColor = Color.White,
            disabledContainerColor = Brand.copy(alpha = 0.38f),
            disabledContentColor = Color.White.copy(alpha = 0.38f)
        )
        ButtonVariant.Secondary -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f)
        )
        ButtonVariant.Danger -> ButtonDefaults.buttonColors(
            containerColor = Error,
            contentColor = Color.White,
            disabledContainerColor = Error.copy(alpha = 0.38f)
        )
        ButtonVariant.Ghost -> ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Brand,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Brand.copy(alpha = 0.38f)
        )
    }

    val border = if (variant == ButtonVariant.Secondary) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    } else null

    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        elevation = if (variant != ButtonVariant.Ghost) ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ) else ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            if (text.isNotEmpty()) {
                androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun StockIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(44.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun StockCompactButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    variant: ButtonVariant = ButtonVariant.Primary
) {
    StockButton(
        text = text,
        onClick = onClick,
        variant = variant,
        icon = icon,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    )
}
