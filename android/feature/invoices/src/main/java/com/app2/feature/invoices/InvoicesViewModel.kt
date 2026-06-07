package com.app2.feature.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.repository.InvoiceRepository
import com.app2.core.data.remote.dto.InvoiceDTO
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

val INVOICE_FILTERS = listOf("Toutes", "Brouillon", "Finalisée", "Payée", "Annulée")

@HiltViewModel
class InvoicesViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<List<InvoiceDTO>>>(ViewState.Loading)
    val state = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(0)
    val selectedFilter = _selectedFilter.asStateFlow()

    private var allInvoices: List<InvoiceDTO> = emptyList()

    val filters = INVOICE_FILTERS

    init {
        loadInvoices()
    }

    fun loadInvoices() {
        viewModelScope.launch {
            _state.value = ViewState.Loading
            try {
                allInvoices = invoiceRepository.getInvoices()
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
                invoiceRepository.deleteInvoice(id)
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
                statusFilter == mapStatusToLabel((inv.status ?: "brouillon"))

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
}
