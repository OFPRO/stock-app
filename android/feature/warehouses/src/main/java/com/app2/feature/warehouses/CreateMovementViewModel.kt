package com.app2.feature.warehouses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.repository.LocationRepository
import com.app2.core.data.repository.MovementRepository
import com.app2.core.data.repository.ProductRepository
import com.app2.core.data.repository.WarehouseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

data class ProductSearchItem(
    val id: Int,
    val name: String,
    val sku: String,
    val quantity: Int
)

@HiltViewModel
class CreateMovementViewModel @Inject constructor(
    private val movementRepository: MovementRepository,
    private val productRepository: ProductRepository,
    private val locationRepository: LocationRepository,
    private val warehouseRepository: WarehouseRepository
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
                _locations.value = locationRepository.getLocations(warehouseId = null).map { dto ->
                    LocationListItem(
                        id = dto.id,
                        warehouseId = dto.warehouseId,
                        name = dto.name,
                        type = dto.type,
                        capacity = dto.capacity,
                        createdAt = dto.createdAt
                    )
                }
            } catch (_: Exception) {}
            try {
                _warehouses.value = warehouseRepository.getWarehouses().map { dto ->
                    WarehouseListItem(
                        id = dto.id,
                        name = dto.name,
                        address = dto.address,
                        manager = dto.manager,
                        phone = dto.phone,
                        isDefault = dto.isDefault == 1,
                        createdAt = dto.createdAt
                    )
                }
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
                _productSearchResults.value = productRepository.getProductsForSale(search = query).map {
                    ProductSearchItem(
                        id = it.id,
                        name = it.name,
                        sku = it.sku,
                        quantity = it.quantity
                    )
                }
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
                        movementRepository.createMovement(product.id, body)
                    }
                    "transfer" -> {
                        val body = buildJsonObject {
                            put("product_id", product.id)
                            put("quantity", qty)
                            _fromLocationId.value?.let { put("from_location_id", it) }
                            _toLocationId.value?.let { put("to_location_id", it) }
                            put("note", _note.value.ifBlank { "Transfert" })
                        }
                        movementRepository.transferStock(body)
                    }
                    "inter_warehouse" -> {
                        val body = buildJsonObject {
                            put("product_id", product.id)
                            put("quantity", qty)
                            _fromWarehouseId.value?.let { put("from_warehouse_id", it) }
                            _toWarehouseId.value?.let { put("to_warehouse_id", it) }
                            put("note", _note.value.ifBlank { "Transfert inter-entrepôt" })
                        }
                        movementRepository.interWarehouseTransfer(body)
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
}
