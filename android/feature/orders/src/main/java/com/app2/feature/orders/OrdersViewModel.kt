package com.app2.feature.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.repository.OrderRepository
import com.app2.core.data.remote.dto.OrderDTO
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

val ORDER_FILTERS = listOf("Tous", "Brouillon", "Envoyée", "Reçue", "Annulée")

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<List<OrderDTO>>>(ViewState.Loading)
    val state = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(0)
    val selectedFilter = _selectedFilter.asStateFlow()

    private var allOrders: List<OrderDTO> = emptyList()

    val filters = ORDER_FILTERS

    init {
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            _state.value = ViewState.Loading
            try {
                allOrders = orderRepository.getOrders()
                applyFilters()
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de chargement des commandes")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    fun onFilterSelected(index: Int) {
        _selectedFilter.value = index
        applyFilters()
    }

    fun deleteOrder(id: Int) {
        viewModelScope.launch {
            try {
                orderRepository.deleteOrder(id)
                loadOrders()
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de suppression")
            }
        }
    }

    fun refresh() {
        allOrders = emptyList()
        loadOrders()
    }

    private fun applyFilters() {
        val query = _searchQuery.value.trim().lowercase()
        val filterIndex = _selectedFilter.value

        val filtered = allOrders.filter { o ->
            val matchesSearch = query.isEmpty() ||
                o.supplierName?.lowercase()?.contains(query) == true ||
                o.orderNumber?.lowercase()?.contains(query) == true

            val statusFilter = ORDER_FILTERS.getOrElse(filterIndex) { "Tous" }
            val matchesFilter = statusFilter == "Tous" ||
                statusFilter == mapStatusToLabel((o.status ?: "brouillon"))

            matchesSearch && matchesFilter
        }

        _state.value = if (filtered.isEmpty()) {
            ViewState.Empty
        } else {
            ViewState.Loaded(filtered)
        }
    }

    private fun mapStatusToLabel(status: String): String = when (status) {
        "brouillon" -> "Brouillon"
        "sent", "envoyée" -> "Envoyée"
        "received", "reçue" -> "Reçue"
        "cancelled", "annulée" -> "Annulée"
        else -> status
    }
}
