package com.app2.feature.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.remote.InvoiceApiService
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

data class InvoiceListItem(
    val id: Int,
    val invoiceNumber: String?,
    val customerName: String?,
    val status: String,
    val total: Double?,
    val paymentMethod: String?,
    val createdAt: String?
)

val INVOICE_FILTERS = listOf("Toutes", "Brouillon", "Finalisée", "Payée", "Annulée")

@HiltViewModel
class InvoicesViewModel @Inject constructor(
    private val invoiceApi: InvoiceApiService
) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<List<InvoiceListItem>>>(ViewState.Loading)
    val state = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(0)
    val selectedFilter = _selectedFilter.asStateFlow()

    private var allInvoices: List<InvoiceListItem> = emptyList()

    val filters = INVOICE_FILTERS

    init {
        loadInvoices()
    }

    fun loadInvoices() {
        viewModelScope.launch {
            _state.value = ViewState.Loading
            try {
                val response = invoiceApi.getInvoices()
                allInvoices = parseInvoiceList(response)
                applyFilters()
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de chargement des factures")
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

    fun deleteInvoice(id: Int) {
        viewModelScope.launch {
            try {
                invoiceApi.deleteInvoice(id)
                loadInvoices()
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de suppression")
            }
        }
    }

    fun refresh() {
        allInvoices = emptyList()
        loadInvoices()
    }

    private fun applyFilters() {
        val query = _searchQuery.value.trim().lowercase()
        val filterIndex = _selectedFilter.value

        val filtered = allInvoices.filter { inv ->
            val matchesSearch = query.isEmpty() ||
                inv.customerName?.lowercase()?.contains(query) == true ||
                inv.invoiceNumber?.lowercase()?.contains(query) == true

            val statusFilter = INVOICE_FILTERS.getOrElse(filterIndex) { "Toutes" }
            val matchesFilter = statusFilter == "Toutes" ||
                statusFilter == mapStatusToLabel(inv.status)

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
        "finalized", "finalisée" -> "Finalisée"
        "paid", "payée" -> "Payée"
        "cancelled", "annulée" -> "Annulée"
        else -> status
    }

    private fun parseInvoiceList(json: JsonElement): List<InvoiceListItem> {
        return json.jsonArray.mapNotNull { item ->
            val obj = item.jsonObject
            val id = obj["id"].optInt() ?: return@mapNotNull null
            InvoiceListItem(
                id = id,
                invoiceNumber = obj["invoice_number"].optString(),
                customerName = obj["customer_name"].optString(),
                status = obj["status"].optString() ?: "brouillon",
                total = obj["total"].optDouble(),
                paymentMethod = obj["payment_method"].optString(),
                createdAt = obj["created_at"].optString()
            )
        }
    }
}
