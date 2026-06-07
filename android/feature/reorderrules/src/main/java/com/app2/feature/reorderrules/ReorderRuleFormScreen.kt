package com.app2.feature.reorderrules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app2.core.data.remote.dto.ReorderRuleDTO
import com.app2.core.ui.ViewState
import com.app2.core.ui.components.StockCard
import com.app2.core.ui.theme.Accent
import com.app2.core.ui.theme.Error

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderRuleFormScreen(
    editRule: ReorderRuleDTO? = null,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ReorderRuleFormViewModel = hiltViewModel()
) {
    val productsState by viewModel.products.collectAsStateWithLifecycle()
    val warehousesState by viewModel.warehouses.collectAsStateWithLifecycle()
    val suppliersState by viewModel.suppliers.collectAsStateWithLifecycle()
    val selectedProductId by viewModel.selectedProductId.collectAsStateWithLifecycle()
    val selectedProductName by viewModel.selectedProductName.collectAsStateWithLifecycle()
    val selectedWarehouseId by viewModel.selectedWarehouseId.collectAsStateWithLifecycle()
    val selectedSupplierId by viewModel.selectedSupplierId.collectAsStateWithLifecycle()
    val minQuantity by viewModel.minQuantity.collectAsStateWithLifecycle()
    val maxQuantity by viewModel.maxQuantity.collectAsStateWithLifecycle()
    val triggerType by viewModel.triggerType.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    viewModel.initForm(editRule)

    val warehouses = (warehousesState as? ViewState.Loaded)?.data ?: emptyList()
    val suppliers = (suppliersState as? ViewState.Loaded)?.data ?: emptyList()

    var warehouseExpanded by remember { mutableStateOf(false) }
    var supplierExpanded by remember { mutableStateOf(false) }
    var triggerExpanded by remember { mutableStateOf(false) }

    val selectedWarehouseName = warehouses.find { it.id == selectedWarehouseId }?.name ?: ""
    val selectedSupplierName = suppliers.find { it.id == selectedSupplierId }?.name ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editRule != null) "Modifier la règle" else "Nouvelle règle") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Fermer")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StockCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Produit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = { Text("Rechercher un produit...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (viewModel.filteredProducts.isNotEmpty()) {
                        viewModel.filteredProducts.forEach { product ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${product.price} DH",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.onProductSelected(product.id, product.name) }
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Sélectionner",
                                        tint = Accent
                                    )
                                }
                            }
                        }
                    }

                    if (selectedProductId != null) {
                        Text(
                            text = "Sélectionné: $selectedProductName",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Accent
                        )
                    }
                }
            }

            StockCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = minQuantity,
                            onValueChange = { viewModel.onMinQuantityChanged(it) },
                            label = { Text("Min") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxQuantity,
                            onValueChange = { viewModel.onMaxQuantityChanged(it) },
                            label = { Text("Max") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    ExposedDropdownMenuBox(
                        expanded = triggerExpanded,
                        onExpandedChange = { triggerExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = when (triggerType) {
                                "automatic" -> "Automatique"
                                else -> "Manuel"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Déclencheur") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = triggerExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = triggerExpanded,
                            onDismissRequest = { triggerExpanded = false }
                        ) {
                            viewModel.triggerTypes.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(if (t == "automatic") "Automatique" else "Manuel") },
                                    onClick = {
                                        viewModel.onTriggerTypeChanged(t)
                                        triggerExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = warehouseExpanded,
                        onExpandedChange = { warehouseExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedWarehouseName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Entrepôt") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = warehouseExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = warehouseExpanded,
                            onDismissRequest = { warehouseExpanded = false }
                        ) {
                            warehouses.forEach { w ->
                                DropdownMenuItem(
                                    text = { Text(w.name) },
                                    onClick = {
                                        viewModel.onWarehouseSelected(w.id)
                                        warehouseExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = supplierExpanded,
                        onExpandedChange = { supplierExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedSupplierName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Fournisseur (optionnel)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = supplierExpanded,
                            onDismissRequest = { supplierExpanded = false }
                        ) {
                            suppliers.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.name) },
                                    onClick = {
                                        viewModel.onSupplierSelected(s.id)
                                        supplierExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            errorMessage?.let { msg ->
                Text(
                    text = msg,
                    color = Error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Button(
                onClick = { viewModel.save(onSuccess = onSaved) },
                enabled = !isLoading && selectedProductId != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isLoading) "Sauvegarde..." else "Enregistrer",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
