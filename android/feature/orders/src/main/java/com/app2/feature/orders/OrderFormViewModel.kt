package com.app2.feature.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.repository.OrderRepository
import com.app2.core.data.repository.SupplierRepository
import com.app2.core.data.repository.WarehouseRepository
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

data class SupplierOption(
    val id: Int,
    val name: String
)

data class WarehouseOption(
    val id: Int,
    val name: String
)

@HiltViewModel
class OrderFormViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val supplierRepository: SupplierRepository,
    private val warehouseRepository: WarehouseRepository
) : ViewModel() {

    private val _suppliers = MutableStateFlow<ViewState<List<SupplierOption>>>(ViewState.Loading)
    val suppliers = _suppliers.asStateFlow()

    private val _warehouses = MutableStateFlow<ViewState<List<WarehouseOption>>>(ViewState.Loading)
    val warehouses = _warehouses.asStateFlow()

    private val _selectedSupplierId = MutableStateFlow<Int?>(null)
    val selectedSupplierId = _selectedSupplierId.asStateFlow()

    private val _selectedWarehouseId = MutableStateFlow<Int?>(null)
    val selectedWarehouseId = _selectedWarehouseId.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes = _notes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private var initialized = false

    fun initForm() {
        if (initialized) return
        initialized = true
        loadSuppliers()
        loadWarehouses()
    }

    fun onSupplierSelected(id: Int) { _selectedSupplierId.value = id }
    fun onWarehouseSelected(id: Int) { _selectedWarehouseId.value = id }
    fun onNotesChanged(v: String) { _notes.value = v }

    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val body = buildJsonObject {
                    put("supplier_id", _selectedSupplierId.value ?: return@launch)
                    _selectedWarehouseId.value?.let { put("warehouse_id", it) }
                    put("notes", _notes.value.ifBlank { "" })
                }
                orderRepository.createOrder(body)
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Erreur de création"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadSuppliers() {
        viewModelScope.launch {
            try {
                val result = supplierRepository.getSuppliers()
                _suppliers.value = ViewState.Loaded(
                    result.map { SupplierOption(id = it.id, name = it.name) }
                )
            } catch (e: Exception) {
                _suppliers.value = ViewState.Error(e.message ?: "Erreur de chargement des fournisseurs")
            }
        }
    }

    private fun loadWarehouses() {
        viewModelScope.launch {
            try {
                val result = warehouseRepository.getWarehouses()
                _warehouses.value = ViewState.Loaded(
                    result.map { WarehouseOption(id = it.id, name = it.name) }
                )
            } catch (e: Exception) {
                _warehouses.value = ViewState.Error(e.message ?: "Erreur de chargement des entrepôts")
            }
        }
    }
}
