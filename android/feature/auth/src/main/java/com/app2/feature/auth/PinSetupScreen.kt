package com.app2.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinSetupScreen(
    mode: PinScreenMode,
    onSuccess: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: PinViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var pin by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is PinViewState.Success) onSuccess()
    }

    val title = when (mode) {
        PinScreenMode.Setup -> "Créer un code"
        PinScreenMode.Change -> "Modifier le code"
    }

    val subtitle = when (state) {
        is PinViewState.ConfirmPin -> "Confirmez le code"
        is PinViewState.Error -> (state as PinViewState.Error).message
        else -> "Entrez un code à 6 chiffres"
    }

    Scaffold(
        topBar = {
            if (onBack != null) {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state is PinViewState.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                PinDotIndicator(length = pin.length, max = 6)
                Spacer(modifier = Modifier.height(32.dp))
                PinKeypad(
                    enabled = state !is PinViewState.Locked,
                    onDigit = { d ->
                        if (pin.length < 6) {
                            pin += d
                            if (pin.length == 6) {
                                if (state is PinViewState.ConfirmPin) {
                                    viewModel.onPinEntered(pin)
                                } else {
                                    viewModel.setupNewPin(pin)
                                }
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
}
