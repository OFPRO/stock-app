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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject

private fun JsonElement?.optString(): String? {
    val prim = this?.jsonPrimitive
    return if (prim != null && prim !is JsonNull) prim.content else null
}

private fun JsonElement?.optInt(): Int? =
    this?.jsonPrimitive?.intOrNull

private fun JsonElement?.optDouble(): Double? =
    this?.jsonPrimitive?.doubleOrNull

data class InvoiceDetailData(
    val id: Int,
    val invoiceNumber: String?,
    val customerId: Int?,
    val customerName: String?,
    val status: String,
    val subtotal: Double?,
    val discountTotal: Double?,
    val taxAmount: Double?,
    val total: Double?,
    val notes: String?,
    val dueDate: String?,
    val paymentMethod: String?,
    val paidAt: String?,
    val createdAt: String?
)

data class InvoiceItemData(
    val id: Int,
    val productId: Int?,
    val productName: String?,
    val quantity: Int,
    val unitPrice: Double,
    val discountPercent: Double?,
    val taxRate: Double?,
    val lineTotal: Double
)

@HiltViewModel
class InvoiceDetailViewModel @Inject constructor(
    private val invoiceApi: InvoiceApiService
) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<InvoiceDetailData>>(ViewState.Loading)
    val state = _state.asStateFlow()

    private val _items = MutableStateFlow<ViewState<List<InvoiceItemData>>>(ViewState.Loading)
    val items = _items.asStateFlow()

    private var lastLoadedId: Int? = null

    fun loadInvoice(id: Int) {
        if (lastLoadedId == id) return
        lastLoadedId = id
        viewModelScope.launch {
            _state.value = ViewState.Loading
            _items.value = ViewState.Loading
            try {
                val detailResponse = invoiceApi.getInvoice(id)
                _state.value = ViewState.Loaded(parseInvoiceDetail(detailResponse))

                val itemsResponse = invoiceApi.getInvoiceItems(id)
                _items.value = ViewState.Loaded(parseInvoiceItems(itemsResponse))
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
                invoiceApi.updateInvoice(id, body)
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
                invoiceApi.deleteInvoice(id)
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de suppression")
            }
        }
    }

    private fun parseInvoiceDetail(obj: JsonElement): InvoiceDetailData {
        val o = obj.jsonObject
        return InvoiceDetailData(
            id = o["id"].optInt() ?: 0,
            invoiceNumber = o["invoice_number"].optString(),
            customerId = o["customer_id"].optInt(),
            customerName = o["customer_name"].optString(),
            status = o["status"].optString() ?: "brouillon",
            subtotal = o["subtotal"].optDouble(),
            discountTotal = o["discount_total"].optDouble(),
            taxAmount = o["tax_amount"].optDouble(),
            total = o["total"].optDouble(),
            notes = o["notes"].optString(),
            dueDate = o["due_date"].optString(),
            paymentMethod = o["payment_method"].optString(),
            paidAt = o["paid_at"].optString(),
            createdAt = o["created_at"].optString()
        )
    }

    private fun parseInvoiceItems(json: JsonElement): List<InvoiceItemData> {
        return json.jsonArray.mapNotNull { item ->
            val o = item.jsonObject
            val id = o["id"].optInt() ?: return@mapNotNull null
            InvoiceItemData(
                id = id,
                productId = o["product_id"].optInt(),
                productName = o["product_name"].optString(),
                quantity = o["quantity"].optInt() ?: 0,
                unitPrice = o["unit_price"].optDouble() ?: 0.0,
                discountPercent = o["discount_percent"].optDouble(),
                taxRate = o["tax_rate"].optDouble(),
                lineTotal = o["line_total"].optDouble() ?: 0.0
            )
        }
    }
}
