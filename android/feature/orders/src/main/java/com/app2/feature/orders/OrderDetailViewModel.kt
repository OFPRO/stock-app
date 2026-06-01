package com.app2.feature.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.remote.OrderApiService
import com.app2.core.data.remote.SupplierApiService
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

data class OrderDetailData(
    val id: Int,
    val orderNumber: String?,
    val supplierId: Int?,
    val supplierName: String?,
    val warehouseId: Int?,
    val status: String,
    val total: Double?,
    val notes: String?,
    val sentAt: String?,
    val receivedAt: String?,
    val createdAt: String?
)

data class OrderItemData(
    val id: Int,
    val productId: Int?,
    val productName: String?,
    val quantity: Int?,
    val unitPrice: Double?,
    val receivedQty: Int?
)

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val orderApi: OrderApiService,
    private val supplierApi: SupplierApiService
) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<OrderDetailData>>(ViewState.Loading)
    val state = _state.asStateFlow()

    private val _items = MutableStateFlow<ViewState<List<OrderItemData>>>(ViewState.Loading)
    val items = _items.asStateFlow()

    private var lastLoadedId: Int? = null

    fun loadOrder(id: Int) {
        if (lastLoadedId == id) return
        lastLoadedId = id
        viewModelScope.launch {
            _state.value = ViewState.Loading
            _items.value = ViewState.Loading
            try {
                val response = supplierApi.getSuppliers()
                val allOrders = orderApi.getOrders()
                val obj = allOrders.jsonArray.firstOrNull {
                    it.jsonObject["id"]?.optInt() == id
                }?.jsonObject
                if (obj != null) {
                    _state.value = ViewState.Loaded(parseOrderDetail(obj))
                } else {
                    _state.value = ViewState.Error("Commande introuvable")
                }
                val itemsResponse = orderApi.getOrderItems(id)
                _items.value = ViewState.Loaded(parseOrderItems(itemsResponse))
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de chargement")
            }
        }
    }

    fun updateStatus(newStatus: String, onSuccess: () -> Unit) {
        val id = lastLoadedId ?: return
        viewModelScope.launch {
            try {
                val body = buildJsonObject { put("status", newStatus) }
                orderApi.updateOrder(id, body)
                onSuccess()
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de mise à jour")
            }
        }
    }

    fun deleteOrder() {
        val id = lastLoadedId ?: return
        viewModelScope.launch {
            try {
                orderApi.deleteOrder(id)
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de suppression")
            }
        }
    }

    private fun parseOrderDetail(obj: JsonElement): OrderDetailData {
        val o = obj.jsonObject
        return OrderDetailData(
            id = o["id"].optInt() ?: 0,
            orderNumber = o["order_number"].optString(),
            supplierId = o["supplier_id"].optInt(),
            supplierName = o["supplier_name"].optString(),
            warehouseId = o["warehouse_id"].optInt(),
            status = o["status"].optString() ?: "brouillon",
            total = o["total"].optDouble(),
            notes = o["notes"].optString(),
            sentAt = o["sent_at"].optString(),
            receivedAt = o["received_at"].optString(),
            createdAt = o["created_at"].optString()
        )
    }

    private fun parseOrderItems(json: JsonElement): List<OrderItemData> {
        return json.jsonArray.mapNotNull { item ->
            val o = item.jsonObject
            val id = o["id"].optInt() ?: return@mapNotNull null
            OrderItemData(
                id = id,
                productId = o["product_id"].optInt(),
                productName = o["product_name"].optString(),
                quantity = o["quantity"].optInt(),
                unitPrice = o["unit_price"].optDouble(),
                receivedQty = o["received_qty"].optInt()
            )
        }
    }
}
