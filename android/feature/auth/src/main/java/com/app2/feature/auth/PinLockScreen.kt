package com.app2.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PinLockScreen(
    onSuccess: () -> Unit,
    viewModel: PinViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var pin by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is PinViewState.Success) onSuccess()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Inventory2,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Code de verrouillage",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))

            when (val s = state) {
                is PinViewState.Locked -> {
                    val sec = (s.remainingMs + 999) / 1000
                    Text(
                        text = "Trop de tentatives. Réessayez dans ${sec}s",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                is PinViewState.Error -> {
                    Text(
                        text = s.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    PinDotIndicator(length = pin.length, max = 6)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            PinKeypad(
                enabled = state !is PinViewState.Locked,
                onDigit = { d ->
                    if (pin.length < 6) {
                        pin += d
                        if (pin.length == 6) {
                            viewModel.onPinEntered(pin)
                            pin = ""
                        }
                    }
                },
                onDelete = {
                    if (pin.isNotEmpty()) pin = pin.dropLast(1)
                },
                onClear = { pin = "" }
            )
        }
    }
}

@Composable
internal fun PinDotIndicator(length: Int, max: Int = 6) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(max) { i ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (i < length) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

@Composable
internal fun PinKeypad(
    enabled: Boolean,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (row in listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                row.forEach { d ->
                    Button(
                        onClick = { onDigit(d) },
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        enabled = enabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(d, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            Spacer(modifier = Modifier.size(80.dp))
            Button(
                onClick = { onDigit("0") },
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("0", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onDelete,
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                enabled = enabled && true,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("\u232B", fontSize = 24.sp)
            }
        }
    }
}
