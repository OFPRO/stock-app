package com.app2.feature.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app2.core.ui.ViewState
import com.app2.core.ui.components.StockCard
import com.app2.core.ui.theme.Error
import com.app2.core.ui.theme.Warning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderFormScreen(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: OrderFormViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val suppliersState by viewModel.suppliers.collectAsStateWithLifecycle()
    val warehousesState by viewModel.warehouses.collectAsStateWithLifecycle()
    val selectedSupplierId by viewModel.selectedSupplierId.collectAsStateWithLifecycle()
    val selectedWarehouseId by viewModel.selectedWarehouseId.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    viewModel.initForm()

    val suppliers = (suppliersState as? ViewState.Loaded)?.data ?: emptyList()
    val warehouses = (warehousesState as? ViewState.Loaded)?.data ?: emptyList()

    var supplierExpanded by remember { mutableStateOf(false) }
    var warehouseExpanded by remember { mutableStateOf(false) }

    val selectedSupplierName = suppliers.find { it.id == selectedSupplierId }?.name ?: ""
    val selectedWarehouseName = warehouses.find { it.id == selectedWarehouseId }?.name ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nouvelle commande") },
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
                        text = "Informations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    ExposedDropdownMenuBox(
                        expanded = supplierExpanded,
                        onExpandedChange = { supplierExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedSupplierName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Fournisseur *") },
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

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { viewModel.onNotesChanged(it) },
                        label = { Text("Notes") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                enabled = !isLoading && selectedSupplierId != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isLoading) "Création..." else "Créer la commande",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
