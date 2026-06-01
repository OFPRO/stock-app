package com.app2.feature.customers

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.app2.core.ui.components.StockTextField
import com.app2.core.ui.components.TextFieldVariant
import com.app2.core.ui.theme.Accent
import com.app2.core.ui.theme.Brand
import com.app2.core.ui.theme.Error
import com.app2.core.ui.theme.Info

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: CustomersViewModel = hiltViewModel(),
    onCustomerClick: (Int) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var deleteTarget by remember { mutableStateOf<CustomerListItem?>(null) }
    var showCreateForm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Clients") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        StockTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            placeholder = "Rechercher un client...",
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
                        ) { customer ->
                            CustomerCard(
                                customer = customer,
                                onClick = { onCustomerClick(customer.id) },
                                onDelete = { deleteTarget = customer },
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
                        text = "Aucun client trouvé",
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
            Icon(Icons.Default.Add, contentDescription = "Ajouter un client")
        }
    }

    deleteTarget?.let { customer ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Supprimer le client") },
            text = { Text("Voulez-vous vraiment supprimer « ${customer.name} » ?\nCette action est irréversible.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCustomer(customer.id)
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
        CustomerFormScreen(
            editData = null,
            onDismiss = { showCreateForm = false },
            onSaved = {
                showCreateForm = false
                viewModel.refresh()
            }
        )
    }
}

@Composable
private fun CustomerCard(
    customer: CustomerListItem,
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
                imageVector = customerTypeIcon(customer.type),
                contentDescription = null,
                tint = when (customer.type) {
                    "school" -> Brand
                    "student" -> Info
                    "company" -> Accent
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = customer.clientCode ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (customer.email != null) {
                        Text(
                            text = " • ${customer.email}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (customer.isLoyal) {
                StockBadge(
                    text = "Loyal",
                    variant = BadgeVariant.Success
                )
            }
        }
    }
}

internal fun customerTypeIcon(type: String?): ImageVector = when (type) {
    "school" -> Icons.Default.School
    "company" -> Icons.Default.Business
    else -> Icons.Default.Person
}

internal fun customerTypeLabel(type: String?): String = when (type) {
    "school" -> "École"
    "student" -> "Étudiant"
    "company" -> "Société"
    else -> "Particulier"
}
