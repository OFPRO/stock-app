package com.app2.feature.warehouses

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app2.core.ui.ViewState
import com.app2.core.ui.components.StockCard
import com.app2.core.ui.components.StockErrorView
import com.app2.core.ui.components.StockSkeletonList
import com.app2.core.ui.components.StockTextField
import com.app2.core.ui.components.TextFieldVariant
import com.app2.core.ui.theme.Error
import com.app2.core.ui.theme.Info
import com.app2.core.ui.theme.Success

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementListScreen(
    viewModel: MovementListViewModel = hiltViewModel(),
    onCreateClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Mouvements") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        LazyRow(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            items(viewModel.filters.size) { index ->
                FilterChip(
                    selected = selectedFilter == index,
                    onClick = { viewModel.onFilterSelected(index) },
                    label = { Text(viewModel.filters[index]) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        StockTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            placeholder = "Rechercher par produit...",
            variant = TextFieldVariant.Search,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
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
                        ) { movement ->
                            MovementCard(
                                movement = movement,
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
                        text = "Aucun mouvement trouvé",
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
            onClick = onCreateClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nouveau mouvement")
        }
    }
}

@Composable
private fun MovementCard(
    movement: MovementListItem,
    modifier: Modifier = Modifier
) {
    val typeColor = when (movement.type) {
        "in", "reception" -> Success
        "out", "sale" -> Error
        "transfer", "inter_warehouse" -> Info
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val typeLabel = when (movement.type) {
        "in" -> "Entrée"
        "reception" -> "Réception"
        "out" -> "Sortie"
        "sale" -> "Vente"
        "transfer" -> "Transfert"
        "inter_warehouse" -> "Trans. WH"
        else -> movement.type
    }
    val qtyPrefix = when (movement.type) {
        "in", "reception" -> "+"
        "out", "sale" -> "-"
        else -> ""
    }
    val showLocations = movement.sourceLocation != null || movement.destLocation != null

    StockCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = typeColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = movement.productName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = " • ${qtyPrefix}${movement.quantity}",
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (movement.createdAt != null) {
                    Text(
                        text = movement.createdAt.take(10),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (showLocations) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = buildString {
                        movement.sourceLocation?.let { append(it) }
                        if (movement.sourceLocation != null && movement.destLocation != null) append(" → ")
                        movement.destLocation?.let { append(it) }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!movement.note.isNullOrBlank()) {
                Text(
                    text = movement.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
