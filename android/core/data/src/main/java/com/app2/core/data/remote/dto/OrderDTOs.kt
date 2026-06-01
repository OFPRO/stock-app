package com.app2.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderDTO(
    val id: Int,
    @SerialName("order_number") val orderNumber: String? = null,
    @SerialName("supplier_id") val supplierId: Int? = null,
    @SerialName("supplier_name") val supplierName: String? = null,
    @SerialName("warehouse_id") val warehouseId: Int? = null,
    val status: String? = "brouillon",
    val total: Double? = null,
    val notes: String? = null,
    @SerialName("sent_at") val sentAt: String? = null,
    @SerialName("received_at") val receivedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class OrderItemDTO(
    val id: Int,
    @SerialName("order_id") val orderId: Int,
    @SerialName("product_id") val productId: Int? = null,
    @SerialName("product_name") val productName: String? = null,
    val quantity: Int? = null,
    @SerialName("unit_price") val unitPrice: Double? = null,
    @SerialName("received_qty") val receivedQty: Int? = null
)

@Serializable
data class OrderCreateRequest(
    @SerialName("supplier_id") val supplierId: Int,
    @SerialName("warehouse_id") val warehouseId: Int? = null,
    val notes: String? = null
)

@Serializable
data class OrderUpdateRequest(
    val status: String? = null,
    val notes: String? = null
)
