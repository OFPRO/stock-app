package com.app2.feature.warehouses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.remote.LocationApiService
import com.app2.core.data.remote.MovementApiService
import com.app2.core.data.remote.ProductApiService
import com.app2.core.data.remote.WarehouseApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject

private fun JsonElement?.optString(): String? {
    val prim = this?.jsonPrimitive
    return if (prim != null && prim !is JsonNull) prim.content else null
}

private fun JsonElement?.optInt(): Int? =
    this?.jsonPrimitive?.intOrNull

private fun JsonElement?.optDouble(): Double? =
    this?.jsonPrimitive?.doubleOrNull

data class ProductSearchItem(
    val id: Int,
    val name: String,
    val sku: String,
    val quantity: Int
)

@HiltViewModel
class CreateMovementViewModel @Inject constructor(
    private val movementApi: MovementApiService,
    private val productApi: ProductApiService,
    private val locationApi: LocationApiService,
    private val warehouseApi: WarehouseApiService
) : ViewModel() {

    private val _movementType = MutableStateFlow("in")
    val movementType = _movementType.asStateFlow()

    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery = _productSearchQuery.asStateFlow()

    private val _productSearchResults = MutableStateFlow<List<ProductSearchItem>>(emptyList())
    val productSearchResults = _productSearchResults.asStateFlow()

    private val _selectedProduct = MutableStateFlow<ProductSearchItem?>(null)
    val selectedProduct = _selectedProduct.asStateFlow()

    private val _quantity = MutableStateFlow("")
    val quantity = _quantity.asStateFlow()

    private val _note = MutableStateFlow("")
    val note = _note.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _locations = MutableStateFlow<List<LocationListItem>>(emptyList())
    val locations = _locations.asStateFlow()

    private val _warehouses = MutableStateFlow<List<WarehouseListItem>>(emptyList())
    val warehouses = _warehouses.asStateFlow()

    private val _locationSearchQuery = MutableStateFlow("")
    val locationSearchQuery = _locationSearchQuery.asStateFlow()

    private val _selectedLocationId = MutableStateFlow<Int?>(null)
    val selectedLocationId = _selectedLocationId.asStateFlow()

    private val _fromLocationId = MutableStateFlow<Int?>(null)
    val fromLocationId = _fromLocationId.asStateFlow()

    private val _toLocationId = MutableStateFlow<Int?>(null)
    val toLocationId = _toLocationId.asStateFlow()

    private val _fromWarehouseId = MutableStateFlow<Int?>(null)
    val fromWarehouseId = _fromWarehouseId.asStateFlow()

    private val _toWarehouseId = MutableStateFlow<Int?>(null)
    val toWarehouseId = _toWarehouseId.asStateFlow()

    private var searchJob: Job? = null
    private var dataLoaded = false

    fun loadData() {
        if (dataLoaded) return
        dataLoaded = true
        viewModelScope.launch {
            try {
                _locations.value = parseLocationList(locationApi.getLocations(warehouseId = null))
            } catch (_: Exception) {}
            try {
                _warehouses.value = parseWarehouseList(warehouseApi.getWarehouses())
            } catch (_: Exception) {}
        }
    }

    fun onMovementTypeChanged(v: String) {
        _movementType.value = v
        _selectedLocationId.value = null
        _fromLocationId.value = null
        _toLocationId.value = null
        _fromWarehouseId.value = null
        _toWarehouseId.value = null
    }

    fun onProductSearchQueryChanged(query: String) {
        _productSearchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _productSearchResults.value = emptyList()
            _selectedProduct.value = null
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            try {
                val response = productApi.getProductsForSale(search = query)
                _productSearchResults.value = parseProductSearchResults(response)
            } catch (_: Exception) {}
        }
    }

    fun onProductSelected(product: ProductSearchItem) {
        _selectedProduct.value = product
        _productSearchQuery.value = product.name
        _productSearchResults.value = emptyList()
    }

    fun clearProductSearch() {
        _selectedProduct.value = null
        _productSearchQuery.value = ""
        _productSearchResults.value = emptyList()
    }

    fun onQuantityChanged(v: String) { _quantity.value = v }
    fun onNoteChanged(v: String) { _note.value = v }
    fun onLocationSearchChanged(v: String) { _locationSearchQuery.value = v }

    fun onLocationSelected(id: Int) { _selectedLocationId.value = id }
    fun onFromLocationSelected(id: Int) { _fromLocationId.value = id }
    fun onToLocationSelected(id: Int) { _toLocationId.value = id }
    fun onFromWarehouseSelected(id: Int) { _fromWarehouseId.value = id }
    fun onToWarehouseSelected(id: Int) { _toWarehouseId.value = id }

    fun save(onSuccess: () -> Unit) {
        val product = _selectedProduct.value ?: return
        val qty = _quantity.value.toIntOrNull() ?: return

        when (_movementType.value) {
            "in", "out" -> {
                if (_selectedLocationId.value == null) {
                    _errorMessage.value = "Veuillez sélectionner une zone"
                    return
                }
            }
            "transfer" -> {
                if (_fromLocationId.value == null || _toLocationId.value == null) {
                    _errorMessage.value = "Veuillez sélectionner les zones source et destination"
                    return
                }
                if (_fromLocationId.value == _toLocationId.value) {
                    _errorMessage.value = "La zone source et destination doivent être différentes"
                    return
                }
            }
            "inter_warehouse" -> {
                if (_fromWarehouseId.value == null || _toWarehouseId.value == null) {
                    _errorMessage.value = "Veuillez sélectionner les entrepôts source et destination"
                    return
                }
                if (_fromWarehouseId.value == _toWarehouseId.value) {
                    _errorMessage.value = "L'entrepôt source et destination doivent être différents"
                    return
                }
            }
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                when (_movementType.value) {
                    "in", "out" -> {
                        val body = buildJsonObject {
                            put("type", _movementType.value)
                            put("quantity", qty)
                            put("note", _note.value.ifBlank { "" })
                            _selectedLocationId.value?.let { put("location_id", it) }
                        }
                        movementApi.createMovement(product.id, body)
                    }
                    "transfer" -> {
                        val body = buildJsonObject {
                            put("product_id", product.id)
                            put("quantity", qty)
                            _fromLocationId.value?.let { put("from_location_id", it) }
                            _toLocationId.value?.let { put("to_location_id", it) }
                            put("note", _note.value.ifBlank { "Transfert" })
                        }
                        movementApi.transferStock(body)
                    }
                    "inter_warehouse" -> {
                        val body = buildJsonObject {
                            put("product_id", product.id)
                            put("quantity", qty)
                            _fromWarehouseId.value?.let { put("from_warehouse_id", it) }
                            _toWarehouseId.value?.let { put("to_warehouse_id", it) }
                            put("note", _note.value.ifBlank { "Transfert inter-entrepôt" })
                        }
                        movementApi.interWarehouseTransfer(body)
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Erreur d'enregistrement"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun parseProductSearchResults(json: JsonElement): List<ProductSearchItem> {
        return json.jsonArray.mapNotNull { item ->
            val obj = item.jsonObject
            val id = obj["id"].optInt() ?: return@mapNotNull null
            ProductSearchItem(
                id = id,
                name = obj["name"].optString() ?: "",
                sku = obj["sku"].optString() ?: "",
                quantity = obj["quantity"].optInt() ?: 0
            )
        }
    }

    private fun parseLocationList(json: JsonElement): List<LocationListItem> {
        return json.jsonArray.mapNotNull { item ->
            val obj = item.jsonObject
            val id = obj["id"].optInt() ?: return@mapNotNull null
            LocationListItem(
                id = id,
                warehouseId = obj["warehouse_id"].optInt() ?: 0,
                name = obj["name"].optString() ?: "",
                type = obj["type"].optString(),
                capacity = obj["capacity"].optInt(),
                createdAt = obj["created_at"].optString()
            )
        }
    }

    private fun parseWarehouseList(json: JsonElement): List<WarehouseListItem> {
        return json.jsonArray.mapNotNull { item ->
            val obj = item.jsonObject
            val id = obj["id"].optInt() ?: return@mapNotNull null
            WarehouseListItem(
                id = id,
                name = obj["name"].optString() ?: "",
                address = obj["address"].optString(),
                manager = obj["manager"].optString(),
                phone = obj["phone"].optString(),
                isDefault = obj["is_default"].optInt() == 1,
                createdAt = obj["created_at"].optString()
            )
        }
    }
}
