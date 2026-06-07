package com.app2.feature.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.repository.CustomerRepository
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerListItem(
    val id: Int,
    val name: String,
    val type: String?,
    val email: String?,
    val phone: String?,
    val address: String?,
    val clientCode: String?,
    val discountRate: Double?,
    val isLoyal: Boolean,
    val isActive: Boolean,
    val notes: String?,
    val createdAt: String?
)

@HiltViewModel
class CustomersViewModel @Inject constructor(
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<List<CustomerListItem>>>(ViewState.Loading)
    val state = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private var allCustomers: List<CustomerListItem> = emptyList()

    init {
        loadCustomers()
    }

    fun loadCustomers() {
        viewModelScope.launch {
            _state.value = ViewState.Loading
            try {
                allCustomers = customerRepository.getCustomers().map { dto ->
                    CustomerListItem(
                        id = dto.id,
                        name = dto.name,
                        type = dto.type,
                        email = dto.email,
                        phone = dto.phone,
                        address = dto.address,
                        clientCode = dto.clientCode,
                        discountRate = dto.discountRate,
                        isLoyal = dto.isLoyal == 1,
                        isActive = dto.isActive?.let { it != 0 } ?: true,
                        notes = dto.notes,
                        createdAt = dto.createdAt
                    )
                }
                applyFilter()
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de chargement des clients")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    fun deleteCustomer(id: Int) {
        viewModelScope.launch {
            try {
                customerRepository.deleteCustomer(id)
                loadCustomers()
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de suppression")
            }
        }
    }

    fun refresh() {
        allCustomers = emptyList()
        loadCustomers()
    }

    private fun applyFilter() {
        val query = _searchQuery.value.trim().lowercase()
        val filtered = if (query.isEmpty()) {
            allCustomers
        } else {
            allCustomers.filter { customer ->
                customer.name.lowercase().contains(query) ||
                    customer.clientCode?.lowercase()?.contains(query) == true ||
                    customer.email?.lowercase()?.contains(query) == true ||
                    customer.phone?.lowercase()?.contains(query) == true
            }
        }
        _state.value = if (filtered.isEmpty()) {
            ViewState.Empty
        } else {
            ViewState.Loaded(filtered)
        }
    }
}
