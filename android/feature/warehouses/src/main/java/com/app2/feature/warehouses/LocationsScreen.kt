package com.app2.feature.warehouses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app2.core.ui.ViewState
import com.app2.core.ui.components.BadgeVariant
import com.app2.core.ui.components.StockBadge
import com.app2.core.ui.components.StockCard
import com.app2.core.ui.components.StockErrorView
import com.app2.core.ui.components.StockSkeletonList
import com.app2.core.ui.theme.Error

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationsScreen(
    warehouseId: Int,
    warehouseName: String,
    onBack: () -> Unit,
    viewModel: LocationsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var deleteTarget by remember { mutableStateOf<LocationListItem?>(null) }
    var showCreateForm by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<LocationListItem?>(null) }

    LaunchedEffect(warehouseId) {
        viewModel.loadLocations(warehouseId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("Zones", style = MaterialTheme.typography.titleMedium)
                    Text(
                        warehouseName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        when (val s = state) {
            is ViewState.Loading -> {
                StockSkeletonList(modifier = Modifier.padding(12.dp))
            }
            is ViewState.Loaded -> {
                PullToRefreshBox(
                    isRefreshing = false,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = s.data,
                            key = { it.id }
                        ) { location ->
                            LocationCard(
                                location = location,
                                onClick = { editTarget = location },
                                onDelete = { deleteTarget = location },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
            is ViewState.Empty -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucune zone dans cet entrepôt",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            is ViewState.Error -> {
                StockErrorView(
                    message = s.message,
                    onRetry = { viewModel.refresh() }
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        FloatingActionButton(
            onClick = { showCreateForm = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Ajouter une zone")
        }
    }

    deleteTarget?.let { location ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Supprimer la zone") },
            text = { Text("Voulez-vous vraiment supprimer « ${location.name} » ?\nCette action est irréversible.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteLocation(location.id)
                    deleteTarget = null
                }) {
                    Text("Supprimer", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    if (showCreateForm) {
        LocationFormScreen(
            warehouseId = warehouseId,
            editData = null,
            onDismiss = { showCreateForm = false },
            onSaved = {
                showCreateForm = false
                viewModel.refresh()
            }
        )
    }

    editTarget?.let { location ->
        LocationFormScreen(
            warehouseId = warehouseId,
            editData = location,
            onDismiss = { editTarget = null },
            onSaved = {
                editTarget = null
                viewModel.refresh()
            }
        )
    }
}

@Composable
private fun LocationCard(
    location: LocationListItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    StockCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = locationTypeLabel(location.type),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (location.capacity != null) {
                        Text(
                            text = " • Capacité: ${location.capacity}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

internal fun locationTypeLabel(type: String?): String = when (type) {
    "rack" -> "Étagère"
    "bin" -> "Bac"
    "shelf" -> "Rayon"
    else -> type ?: "Étagère"
}
