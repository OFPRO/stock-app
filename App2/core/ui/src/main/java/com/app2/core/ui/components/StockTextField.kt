package com.app2.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

enum class TextFieldVariant {
    Default, Search, Barcode, Currency
}

@Composable
fun StockTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    variant: TextFieldVariant = TextFieldVariant.Default,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    imeAction: ImeAction = ImeAction.Search,
    onSearch: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val leadingIcon: (@Composable () -> Unit)? = when (variant) {
        TextFieldVariant.Search -> ({
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        })
        TextFieldVariant.Barcode -> ({
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        })
        TextFieldVariant.Currency -> ({
            Text(
                text = "MAD",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        })
        else -> null
    }

    val trailingIcon: (@Composable () -> Unit)? = if (value.isNotEmpty()) {{
        IconButton(
            onClick = { onValueChange("") }
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Effacer",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }} else null

    val prefix: (@Composable () -> Unit)? = if (variant == TextFieldVariant.Currency) {{
        Text(
            text = "MAD ",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }} else null

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        readOnly = readOnly,
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onSearch = { onSearch?.invoke() }
        ),
        textStyle = MaterialTheme.typography.bodyLarge
    )
}
