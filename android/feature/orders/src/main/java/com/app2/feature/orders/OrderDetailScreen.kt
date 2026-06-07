package com.app2.feature.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app2.core.ui.ViewState
import com.app2.core.ui.components.BadgeVariant
import com.app2.core.ui.components.StockBadge
import com.app2.core.ui.components.StockCard
import com.app2.core.ui.components.StockErrorView
import com.app2.core.ui.components.StockSkeletonCard
import com.app2.core.data.remote.dto.OrderDTO
import com.app2.core.data.remote.dto.OrderItemDTO
import com.app2.core.ui.theme.Accent
import com.app2.core.ui.theme.Error

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: Int,
    onBack: () -> Unit,
    onDeleted: () -> Unit = {},
    viewModel: OrderDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val itemsState by viewModel.items.collectAsStateWithLifecycle()
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }

    viewModel.loadOrder(orderId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when (val s = state) {
                        is ViewState.Loaded -> s.data.orderNumber ?: "Commande #${s.data.id}"
                        else -> "Détail de la commande"
                    }
                    Text(title)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (state is ViewState.Loaded) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Actions")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            val currentStatus = (state as ViewState.Loaded).data.status
                            if (currentStatus == "brouillon") {
                                DropdownMenuItem(
                                    text = { Text("Marquer comme envoyée") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.updateStatus("sent") {
                                            viewModel.loadOrder(orderId)
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) }
                                )
                            }
                            if (currentStatus == "sent" || currentStatus == "envoyée") {
                                DropdownMenuItem(
                                    text = { Text("Marquer comme reçue") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.updateStatus("received") {
                                            viewModel.loadOrder(orderId)
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Supprimer", color = Error) },
                                onClick = {
                                    showMenu = false
                                    showDeleteConfirm = true
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Error) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        when (val s = state) {
            is ViewState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(3) { StockSkeletonCard() }
                }
            }
            is ViewState.Loaded -> {
                OrderDetailContent(
                    order = s.data,
                    itemsState = itemsState,
                    modifier = Modifier.padding(padding)
                )
            }
            is ViewState.Error -> {
                StockErrorView(
                    message = s.message,
                    onRetry = { viewModel.loadOrder(orderId) },
                    modifier = Modifier.padding(padding)
                )
            }
            is ViewState.Empty -> {}
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Supprimer la commande") },
            text = { Text("Voulez-vous vraiment supprimer cette commande ?\nCette action est irréversible.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteOrder()
                    showDeleteConfirm = false
                    onDeleted()
                }) {
                    Text("Supprimer", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun OrderDetailContent(
    order: OrderDTO,
    itemsState: ViewState<List<OrderItemDTO>>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { InfoCard(order) }
        item { StatusCard(order) }
        item {
            Text(
                text = "Articles",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        when (val its = itemsState) {
            is ViewState.Loading -> {
                item { repeat(2) { StockSkeletonCard() } }
            }
            is ViewState.Loaded -> {
                items(its.data, key = { it.id }) { item ->
                    ItemCard(item)
                }
                if (its.data.isEmpty()) {
                    item {
                        Text(
                            text = "Aucun article dans cette commande",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            is ViewState.Error -> {
                item {
                    Text(
                        text = its.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Error
                    )
                }
            }
            is ViewState.Empty -> {}
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun InfoCard(order: OrderDTO) {
    StockCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = order.supplierName ?: "Fournisseur inconnu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (order.orderNumber != null) {
                DetailRow("N° commande", order.orderNumber ?: "")
            }
            if (order.total != null) {
                DetailRow("Total", "${order.total} DH")
            }
            if (order.createdAt != null) {
                DetailRow("Créée le", (order.createdAt ?: "").take(10))
            }
            if (order.sentAt != null) {
                DetailRow("Envoyée le", (order.sentAt ?: "").take(10))
            }
            if (order.receivedAt != null) {
                DetailRow("Reçue le", (order.receivedAt ?: "").take(10))
            }
            if (order.notes != null) {
                DetailRow("Notes", order.notes ?: "")
            }
        }
    }
}

@Composable
private fun StatusCard(order: OrderDTO) {
    val badgeVariant = when (order.status) {
        "brouillon" -> BadgeVariant.Neutral
        "sent", "envoyée" -> BadgeVariant.Info
        "received", "reçue" -> BadgeVariant.Success
        "cancelled", "annulée" -> BadgeVariant.Error
        else -> BadgeVariant.Neutral
    }
    val statusLabel = when (order.status) {
        "brouillon" -> "Brouillon"
        "sent", "envoyée" -> "Envoyée"
        "received", "reçue" -> "Reçue"
        "cancelled", "annulée" -> "Annulée"
        else -> order.status ?: "Inconnu"
    }
    StockCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Statut",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            StockBadge(text = statusLabel, variant = badgeVariant)
        }
    }
}

@Composable
private fun ItemCard(item: OrderItemDTO) {
    StockCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = item.productName ?: "Produit #${item.productId}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Qté: ${item.quantity ?: 0}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.unitPrice != null) {
                    Text(
                        text = "${item.unitPrice} DH",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (item.receivedQty != null) {
                Text(
                    text = "Reçue: ${item.receivedQty}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
