package com.app2.feature.warehouses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.repository.WarehouseRepository
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WarehouseListItem(
    val id: Int,
    val name: String,
    val address: String?,
    val manager: String?,
    val phone: String?,
    val isDefault: Boolean,
    val createdAt: String?
)

@HiltViewModel
class WarehousesViewModel @Inject constructor(
    private val warehouseRepository: WarehouseRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<List<WarehouseListItem>>>(ViewState.Loading)
    val state = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private var allWarehouses: List<WarehouseListItem> = emptyList()

    init {
        loadWarehouses()
    }

    fun loadWarehouses() {
        viewModelScope.launch {
            _state.value = ViewState.Loading
            try {
                allWarehouses = warehouseRepository.getWarehouses().map { dto ->
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
                applyFilter()
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de chargement des entrepôts")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    fun refresh() {
        allWarehouses = emptyList()
        loadWarehouses()
    }

    private fun applyFilter() {
        val query = _searchQuery.value.trim().lowercase()
        val filtered = if (query.isEmpty()) {
            allWarehouses
        } else {
            allWarehouses.filter { w ->
                w.name.lowercase().contains(query) ||
                    w.address?.lowercase()?.contains(query) == true ||
                    w.manager?.lowercase()?.contains(query) == true
            }
        }
        _state.value = if (filtered.isEmpty()) {
            ViewState.Empty
        } else {
            ViewState.Loaded(filtered)
        }
    }
}
