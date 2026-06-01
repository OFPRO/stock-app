package com.app2.feature.invoices

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app2.core.ui.ViewState
import com.app2.core.ui.components.BadgeVariant
import com.app2.core.ui.components.StockBadge
import com.app2.core.ui.components.StockCard
import com.app2.core.ui.components.StockErrorView
import com.app2.core.ui.components.StockSkeletonCard
import com.app2.core.ui.theme.Accent
import com.app2.core.ui.theme.Error

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    invoiceId: Int,
    onBack: () -> Unit,
    onDeleted: () -> Unit = {},
    viewModel: InvoiceDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val itemsState by viewModel.items.collectAsStateWithLifecycle()
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    viewModel.loadInvoice(invoiceId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when (val s = state) {
                        is ViewState.Loaded -> s.data.invoiceNumber ?: "Facture #${s.data.id}"
                        else -> "Détail de la facture"
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
                                    text = { Text("Finaliser") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.updateStatus("finalized") {
                                            viewModel.loadInvoice(invoiceId)
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) }
                                )
                            }
                            if (currentStatus == "finalized" || currentStatus == "finalisée") {
                                DropdownMenuItem(
                                    text = { Text("Marquer comme payée") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.updateStatus("paid") {
                                            viewModel.loadInvoice(invoiceId)
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
                InvoiceDetailContent(
                    invoice = s.data,
                    itemsState = itemsState,
                    modifier = Modifier.padding(padding)
                )
            }
            is ViewState.Error -> {
                StockErrorView(
                    message = s.message,
                    onRetry = { viewModel.loadInvoice(invoiceId) },
                    modifier = Modifier.padding(padding)
                )
            }
            is ViewState.Empty -> {}
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Supprimer la facture") },
            text = { Text("Voulez-vous vraiment supprimer cette facture ?\nCette action est irréversible.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteInvoice()
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
private fun InvoiceDetailContent(
    invoice: InvoiceDetailData,
    itemsState: ViewState<List<InvoiceItemData>>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { InvoiceInfoCard(invoice) }
        item { InvoiceStatusCard(invoice) }
        item { InvoiceTotalsCard(invoice) }
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
                    InvoiceItemCard(item)
                }
                if (its.data.isEmpty()) {
                    item {
                        Text(
                            text = "Aucun article dans cette facture",
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
private fun InvoiceInfoCard(invoice: InvoiceDetailData) {
    StockCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = invoice.customerName ?: "Client inconnu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (invoice.invoiceNumber != null) {
                DetailRow("N° facture", invoice.invoiceNumber)
            }
            if (invoice.createdAt != null) {
                DetailRow("Créée le", invoice.createdAt.take(10))
            }
            if (invoice.dueDate != null) {
                DetailRow("Échéance", invoice.dueDate.take(10))
            }
            if (invoice.paymentMethod != null) {
                val methodLabel = when (invoice.paymentMethod) {
                    "cash" -> "Espèces"
                    "card" -> "Carte"
                    "mixed" -> "Mixte"
                    "credit" -> "Crédit"
                    else -> invoice.paymentMethod
                }
                DetailRow("Paiement", methodLabel)
            }
            if (invoice.paidAt != null) {
                DetailRow("Payée le", invoice.paidAt.take(10))
            }
            if (invoice.notes != null) {
                DetailRow("Notes", invoice.notes)
            }
        }
    }
}

@Composable
private fun InvoiceStatusCard(invoice: InvoiceDetailData) {
    val badgeVariant = when (invoice.status) {
        "brouillon" -> BadgeVariant.Neutral
        "finalized", "finalisée" -> BadgeVariant.Info
        "paid", "payée" -> BadgeVariant.Success
        "cancelled", "annulée" -> BadgeVariant.Error
        "overdue" -> BadgeVariant.Warning
        else -> BadgeVariant.Neutral
    }
    val statusLabel = when (invoice.status) {
        "brouillon" -> "Brouillon"
        "finalized", "finalisée" -> "Finalisée"
        "paid", "payée" -> "Payée"
        "cancelled", "annulée" -> "Annulée"
        "overdue" -> "En retard"
        else -> invoice.status
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
private fun InvoiceTotalsCard(invoice: InvoiceDetailData) {
    StockCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (invoice.subtotal != null) {
                DetailRow("Sous-total", "${invoice.subtotal} DH")
            }
            if (invoice.discountTotal != null && invoice.discountTotal != 0.0) {
                DetailRow("Remise", "-${invoice.discountTotal} DH")
            }
            if (invoice.taxAmount != null) {
                DetailRow("TVA", "${invoice.taxAmount} DH")
            }
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${invoice.total ?: 0.0} DH",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Accent
                )
            }
        }
    }
}

@Composable
private fun InvoiceItemCard(item: InvoiceItemData) {
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
                    text = "Qté: ${item.quantity} x ${item.unitPrice} DH",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${item.lineTotal} DH",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
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
