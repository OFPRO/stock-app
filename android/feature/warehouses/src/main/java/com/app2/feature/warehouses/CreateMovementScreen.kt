package com.app2.feature.warehouses

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
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app2.core.ui.components.StockCard
import com.app2.core.ui.theme.Error

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMovementScreen(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: CreateMovementViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val movementType by viewModel.movementType.collectAsStateWithLifecycle()
    val productSearchQuery by viewModel.productSearchQuery.collectAsStateWithLifecycle()
    val productSearchResults by viewModel.productSearchResults.collectAsStateWithLifecycle()
    val selectedProduct by viewModel.selectedProduct.collectAsStateWithLifecycle()
    val quantity by viewModel.quantity.collectAsStateWithLifecycle()
    val note by viewModel.note.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val locations by viewModel.locations.collectAsStateWithLifecycle()
    val warehouses by viewModel.warehouses.collectAsStateWithLifecycle()
    val locationSearchQuery by viewModel.locationSearchQuery.collectAsStateWithLifecycle()
    val selectedLocationId by viewModel.selectedLocationId.collectAsStateWithLifecycle()
    val fromLocationId by viewModel.fromLocationId.collectAsStateWithLifecycle()
    val toLocationId by viewModel.toLocationId.collectAsStateWithLifecycle()
    val fromWarehouseId by viewModel.fromWarehouseId.collectAsStateWithLifecycle()
    val toWarehouseId by viewModel.toWarehouseId.collectAsStateWithLifecycle()

    val movementTypes = listOf(
        "in" to "Entrée",
        "out" to "Sortie",
        "transfer" to "Transfert zone",
        "inter_warehouse" to "Transfert entrepôt"
    )
    var typeExpanded by remember { mutableStateOf(false) }
    var productExpanded by remember { mutableStateOf(false) }
    var locationExpanded by remember { mutableStateOf(false) }
    var fromLocationExpanded by remember { mutableStateOf(false) }
    var toLocationExpanded by remember { mutableStateOf(false) }
    var fromWarehouseExpanded by remember { mutableStateOf(false) }
    var toWarehouseExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadData() }

    val filteredLocations = if (locationSearchQuery.isBlank()) {
        locations
    } else {
        val q = locationSearchQuery.trim().lowercase()
        locations.filter { loc ->
            val whName = getWarehouseName(loc.warehouseId, warehouses) ?: ""
            loc.name.lowercase().contains(q) || whName.lowercase().contains(q)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nouveau mouvement") },
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
                        text = "Type de mouvement",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    ExposedDropdownMenuBox(
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = movementTypes.first { it.first == movementType }.second,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Type *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                        )
                        ExposedDropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false }
                        ) {
                            movementTypes.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        viewModel.onMovementTypeChanged(value)
                                        typeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            StockCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Produit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    ExposedDropdownMenuBox(
                        expanded = productExpanded,
                        onExpandedChange = { productExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = productSearchQuery,
                            onValueChange = {
                                viewModel.onProductSearchQueryChanged(it)
                                productExpanded = true
                            },
                            label = { Text("Produit *") },
                            isError = selectedProduct == null && productSearchQuery.isNotBlank(),
                            singleLine = true,
                            trailingIcon = {
                                if (selectedProduct != null) {
                                    IconButton(onClick = { viewModel.clearProductSearch() }) {
                                        Icon(Icons.Default.Close, contentDescription = "Effacer")
                                    }
                                } else {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                        )
                        ExposedDropdownMenu(
                            expanded = productExpanded && productSearchResults.isNotEmpty(),
                            onDismissRequest = { productExpanded = false }
                        ) {
                            productSearchResults.forEach { product ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(product.name, fontWeight = FontWeight.Medium)
                                            Text(
                                                "${product.sku} • Stock: ${product.quantity}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.onProductSelected(product)
                                        productExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            StockCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Quantité",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { viewModel.onQuantityChanged(it) },
                        label = { Text("Quantité *") },
                        isError = quantity.isNotBlank() && (quantity.toIntOrNull() == null || (quantity.toIntOrNull() ?: 0) <= 0),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            when (movementType) {
                "in", "out" -> {
                    StockCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Zone",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            OutlinedTextField(
                                value = locationSearchQuery,
                                onValueChange = { viewModel.onLocationSearchChanged(it) },
                                label = { Text("Filtrer par entrepôt...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            ExposedDropdownMenuBox(
                                expanded = locationExpanded,
                                onExpandedChange = { locationExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = filteredLocations.firstOrNull { it.id == selectedLocationId }?.let {
                                        "${getWarehouseName(it.warehouseId, warehouses)?.let { "$it / " } ?: ""}${it.name}"
                                    } ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Zone") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                                )
                                ExposedDropdownMenu(
                                    expanded = locationExpanded && filteredLocations.isNotEmpty(),
                                    onDismissRequest = { locationExpanded = false }
                                ) {
                                    filteredLocations.forEach { loc ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(loc.name)
                                                    getWarehouseName(loc.warehouseId, warehouses)?.let {
                                                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            },
                                            onClick = {
                                                viewModel.onLocationSelected(loc.id)
                                                locationExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                "transfer" -> {
                    StockCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Zones",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text("De", fontWeight = FontWeight.Medium)
                            LocationDropdown(
                                locations = locations,
                                warehouses = warehouses,
                                selectedId = fromLocationId,
                                expanded = fromLocationExpanded,
                                onExpandedChange = { fromLocationExpanded = it },
                                onSelected = { viewModel.onFromLocationSelected(it) },
                                label = "Zone source"
                            )

                            Text("Vers", fontWeight = FontWeight.Medium)
                            LocationDropdown(
                                locations = locations,
                                warehouses = warehouses,
                                selectedId = toLocationId,
                                expanded = toLocationExpanded,
                                onExpandedChange = { toLocationExpanded = it },
                                onSelected = { viewModel.onToLocationSelected(it) },
                                label = "Zone destination"
                            )
                        }
                    }
                }
                "inter_warehouse" -> {
                    StockCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Entrepôts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text("De", fontWeight = FontWeight.Medium)
                            WarehouseDropdown(
                                warehouses = warehouses,
                                selectedId = fromWarehouseId,
                                expanded = fromWarehouseExpanded,
                                onExpandedChange = { fromWarehouseExpanded = it },
                                onSelected = { viewModel.onFromWarehouseSelected(it) },
                                label = "Entrepôt source"
                            )

                            Text("Vers", fontWeight = FontWeight.Medium)
                            WarehouseDropdown(
                                warehouses = warehouses,
                                selectedId = toWarehouseId,
                                expanded = toWarehouseExpanded,
                                onExpandedChange = { toWarehouseExpanded = it },
                                onSelected = { viewModel.onToWarehouseSelected(it) },
                                label = "Entrepôt destination"
                            )
                        }
                    }
                }
            }

            StockCard(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { viewModel.onNoteChanged(it) },
                    label = { Text("Note") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            errorMessage?.let { msg ->
                Text(
                    text = msg,
                    color = Error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            val canSubmit = !isLoading && selectedProduct != null &&
                quantity.toIntOrNull() != null && (quantity.toIntOrNull() ?: 0) > 0 &&
                when (movementType) {
                    "in", "out" -> selectedLocationId != null
                    "transfer" -> fromLocationId != null && toLocationId != null && fromLocationId != toLocationId
                    "inter_warehouse" -> fromWarehouseId != null && toWarehouseId != null && fromWarehouseId != toWarehouseId
                    else -> true
                }

            Button(
                onClick = { viewModel.save(onSuccess = onSaved) },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isLoading) "Enregistrement..." else "Créer",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationDropdown(
    locations: List<LocationListItem>,
    warehouses: List<WarehouseListItem>,
    selectedId: Int?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelected: (Int) -> Unit,
    label: String
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            value = locations.firstOrNull { it.id == selectedId }?.let {
                "${getWarehouseName(it.warehouseId, warehouses)?.let { "$it / " } ?: ""}${it.name}"
            } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
        )
        ExposedDropdownMenu(
            expanded = expanded && locations.isNotEmpty(),
            onDismissRequest = { onExpandedChange(false) }
        ) {
            locations.forEach { loc ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(loc.name)
                            getWarehouseName(loc.warehouseId, warehouses)?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    onClick = {
                        onSelected(loc.id)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WarehouseDropdown(
    warehouses: List<WarehouseListItem>,
    selectedId: Int?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelected: (Int) -> Unit,
    label: String
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            value = warehouses.firstOrNull { it.id == selectedId }?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
        )
        ExposedDropdownMenu(
            expanded = expanded && warehouses.isNotEmpty(),
            onDismissRequest = { onExpandedChange(false) }
        ) {
            warehouses.forEach { wh ->
                DropdownMenuItem(
                    text = {
                        Text(wh.name)
                    },
                    onClick = {
                        onSelected(wh.id)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

internal fun getWarehouseName(warehouseId: Int, warehouses: List<WarehouseListItem>): String? {
    return warehouses.firstOrNull { it.id == warehouseId }?.name
}
