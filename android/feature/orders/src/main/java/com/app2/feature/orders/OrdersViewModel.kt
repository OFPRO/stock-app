package com.app2.feature.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.remote.OrderApiService
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

private fun JsonElement?.optString(): String? {
    val prim = this?.jsonPrimitive
    return if (prim != null && prim !is JsonNull) prim.content else null
}

private fun JsonElement?.optInt(): Int? =
    this?.jsonPrimitive?.intOrNull

private fun JsonElement?.optDouble(): Double? =
    this?.jsonPrimitive?.doubleOrNull

data class OrderListItem(
    val id: Int,
    val orderNumber: String?,
    val supplierName: String?,
    val status: String,
    val total: Double?,
    val notes: String?,
    val createdAt: String?
)

val ORDER_FILTERS = listOf("Tous", "Brouillon", "Envoyée", "Reçue", "Annulée")

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderApi: OrderApiService
) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<List<OrderListItem>>>(ViewState.Loading)
    val state = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(0)
    val selectedFilter = _selectedFilter.asStateFlow()

    private var allOrders: List<OrderListItem> = emptyList()

    val filters = ORDER_FILTERS

    init {
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            _state.value = ViewState.Loading
            try {
                val response = orderApi.getOrders()
                allOrders = parseOrderList(response)
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
                orderApi.deleteOrder(id)
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
                statusFilter == mapStatusToLabel(o.status)

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

    private fun parseOrderList(json: JsonElement): List<OrderListItem> {
        return json.jsonArray.mapNotNull { item ->
            val obj = item.jsonObject
            val id = obj["id"].optInt() ?: return@mapNotNull null
            OrderListItem(
                id = id,
                orderNumber = obj["order_number"].optString(),
                supplierName = obj["supplier_name"].optString(),
                status = obj["status"].optString() ?: "brouillon",
                total = obj["total"].optDouble(),
                notes = obj["notes"].optString(),
                createdAt = obj["created_at"].optString()
            )
        }
    }
}
