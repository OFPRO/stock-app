package com.app2.feature.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.repository.SupplierRepository
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupplierListItem(
    val id: Int,
    val name: String,
    val email: String?,
    val phone: String?,
    val address: String?,
    val contactPerson: String?,
    val createdAt: String?
)

@HiltViewModel
class SuppliersViewModel @Inject constructor(
    private val supplierRepository: SupplierRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<List<SupplierListItem>>>(ViewState.Loading)
    val state = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private var allSuppliers: List<SupplierListItem> = emptyList()

    init {
        loadSuppliers()
    }

    fun loadSuppliers() {
        viewModelScope.launch {
            _state.value = ViewState.Loading
            try {
                allSuppliers = supplierRepository.getSuppliers().map { dto ->
                    SupplierListItem(
                        id = dto.id,
                        name = dto.name,
                        email = dto.email,
                        phone = dto.phone,
                        address = dto.address,
                        contactPerson = dto.contactPerson,
                        createdAt = dto.createdAt
                    )
                }
                applyFilter()
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de chargement des fournisseurs")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    fun deleteSupplier(id: Int) {
        viewModelScope.launch {
            try {
                supplierRepository.deleteSupplier(id)
                loadSuppliers()
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de suppression")
            }
        }
    }

    fun refresh() {
        allSuppliers = emptyList()
        loadSuppliers()
    }

    private fun applyFilter() {
        val query = _searchQuery.value.trim().lowercase()
        val filtered = if (query.isEmpty()) {
            allSuppliers
        } else {
            allSuppliers.filter { supplier ->
                supplier.name.lowercase().contains(query) ||
                    supplier.email?.lowercase()?.contains(query) == true ||
                    supplier.phone?.lowercase()?.contains(query) == true
            }
        }
        _state.value = if (filtered.isEmpty()) {
            ViewState.Empty
        } else {
            ViewState.Loaded(filtered)
        }
    }
}
