package com.app2.feature.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.repository.InvoiceRepository
import com.app2.core.data.remote.dto.InvoiceDTO
import com.app2.core.data.remote.dto.InvoiceItemDTO
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

@HiltViewModel
class InvoiceDetailViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<InvoiceDTO>>(ViewState.Loading)
    val state = _state.asStateFlow()

    private val _items = MutableStateFlow<ViewState<List<InvoiceItemDTO>>>(ViewState.Loading)
    val items = _items.asStateFlow()

    private var lastLoadedId: Int? = null

    fun loadInvoice(id: Int) {
        if (lastLoadedId == id) return
        lastLoadedId = id
        viewModelScope.launch {
            _state.value = ViewState.Loading
            _items.value = ViewState.Loading
            try {
                _state.value = ViewState.Loaded(invoiceRepository.getInvoice(id))
                _items.value = ViewState.Loaded(invoiceRepository.getInvoiceItems(id))
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de chargement de la facture")
            }
        }
    }

    fun updateStatus(newStatus: String, onSuccess: () -> Unit) {
        val id = lastLoadedId ?: return
        viewModelScope.launch {
            try {
                val body = buildJsonObject { put("status", newStatus) }
                invoiceRepository.updateInvoice(id, body)
                onSuccess()
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de mise à jour")
            }
        }
    }

    fun deleteInvoice() {
        val id = lastLoadedId ?: return
        viewModelScope.launch {
            try {
                invoiceRepository.deleteInvoice(id)
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de suppression")
            }
        }
    }
}
