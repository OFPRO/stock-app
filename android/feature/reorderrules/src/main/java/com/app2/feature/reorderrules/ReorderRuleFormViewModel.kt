package com.app2.feature.reorderrules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.remote.dto.ProductDetailDTO
import com.app2.core.data.remote.dto.ReorderRuleDTO
import com.app2.core.data.remote.dto.SupplierDTO
import com.app2.core.data.remote.dto.WarehouseDTO
import com.app2.core.data.repository.ProductRepository
import com.app2.core.data.repository.ReorderRuleRepository
import com.app2.core.data.repository.SupplierRepository
import com.app2.core.data.repository.WarehouseRepository
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

@HiltViewModel
class ReorderRuleFormViewModel @Inject constructor(
    private val reorderRuleRepository: ReorderRuleRepository,
    private val productRepository: ProductRepository,
    private val warehouseRepository: WarehouseRepository,
    private val supplierRepository: SupplierRepository
) : ViewModel() {

    private val _products = MutableStateFlow<ViewState<List<ProductDetailDTO>>>(ViewState.Loading)
    val products = _products.asStateFlow()

    private val _warehouses = MutableStateFlow<ViewState<List<WarehouseDTO>>>(ViewState.Loading)
    val warehouses = _warehouses.asStateFlow()

    private val _suppliers = MutableStateFlow<ViewState<List<SupplierDTO>>>(ViewState.Loading)
    val suppliers = _suppliers.asStateFlow()

    private val _selectedProductId = MutableStateFlow<Int?>(null)
    val selectedProductId = _selectedProductId.asStateFlow()

    private val _selectedProductName = MutableStateFlow("")
    val selectedProductName = _selectedProductName.asStateFlow()

    private val _selectedWarehouseId = MutableStateFlow<Int?>(null)
    val selectedWarehouseId = _selectedWarehouseId.asStateFlow()

    private val _selectedSupplierId = MutableStateFlow<Int?>(null)
    val selectedSupplierId = _selectedSupplierId.asStateFlow()

    private val _minQuantity = MutableStateFlow("5")
    val minQuantity = _minQuantity.asStateFlow()

    private val _maxQuantity = MutableStateFlow("100")
    val maxQuantity = _maxQuantity.asStateFlow()

    private val _triggerType = MutableStateFlow("manual")
    val triggerType = _triggerType.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private var allProducts: List<ProductDetailDTO> = emptyList()
    private var productSearchResults = mutableListOf<ProductDetailDTO>()

    val filteredProducts: List<ProductDetailDTO>
        get() = productSearchResults

    val triggerTypes = listOf("manual", "automatic")

    private var editId: Int? = null
    private var isInitialized = false
    private var isFormInit = false

    fun initForm(editRule: ReorderRuleDTO? = null) {
        if (isInitialized) return
        isInitialized = true
        editId = editRule?.id

        viewModelScope.launch {
            try {
                allProducts = productRepository.getProducts()
                _products.value = ViewState.Loaded(allProducts)

                val wList = warehouseRepository.getWarehouses()
                _warehouses.value = ViewState.Loaded(wList)

                val sList = supplierRepository.getSuppliers()
                _suppliers.value = ViewState.Loaded(sList)

                if (editRule != null) {
                    populateForm(editRule)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Erreur de chargement"
            }
        }
    }

    private fun populateForm(rule: ReorderRuleDTO) {
        _selectedProductId.value = rule.productId
        _selectedProductName.value = rule.productName ?: ""
        _selectedWarehouseId.value = rule.warehouseId
        _selectedSupplierId.value = rule.supplierId
        _minQuantity.value = rule.minQuantity?.toString() ?: "5"
        _maxQuantity.value = rule.maxQuantity?.toString() ?: "100"
        _triggerType.value = rule.triggerType ?: "manual"
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            productSearchResults = mutableListOf()
            return
        }
        productSearchResults = allProducts
            .filter { it.name.lowercase().contains(query.lowercase()) }
            .take(5)
            .toMutableList()
    }

    fun onProductSelected(id: Int, name: String) {
        _selectedProductId.value = id
        _selectedProductName.value = name
        _searchQuery.value = name
        productSearchResults = mutableListOf()
    }

    fun onWarehouseSelected(id: Int) {
        _selectedWarehouseId.value = id
    }

    fun onSupplierSelected(id: Int) {
        _selectedSupplierId.value = id
    }

    fun onMinQuantityChanged(value: String) {
        _minQuantity.value = value.filter { it.isDigit() }
    }

    fun onMaxQuantityChanged(value: String) {
        _maxQuantity.value = value.filter { it.isDigit() }
    }

    fun onTriggerTypeChanged(type: String) {
        _triggerType.value = type
    }

    fun save(onSuccess: () -> Unit) {
        val productId = _selectedProductId.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val body = buildJsonObject {
                    put("product_id", JsonPrimitive(productId))
                    put("warehouse_id", _selectedWarehouseId.value?.let { JsonPrimitive(it) } ?: JsonNull)
                    put("min_quantity", _minQuantity.value.toIntOrNull() ?: 5)
                    put("max_quantity", _maxQuantity.value.toIntOrNull() ?: 100)
                    put("trigger_type", _triggerType.value)
                    put("supplier_id", _selectedSupplierId.value?.let { JsonPrimitive(it) } ?: JsonNull)
                }
                if (editId != null) {
                    reorderRuleRepository.updateReorderRule(editId!!, body)
                } else {
                    reorderRuleRepository.createReorderRule(body)
                }
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Erreur de sauvegarde"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
