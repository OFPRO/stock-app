package com.app2.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InvoiceDTO(
    val id: Int,
    @SerialName("invoice_number") val invoiceNumber: String? = null,
    @SerialName("customer_id") val customerId: Int? = null,
    @SerialName("warehouse_id") val warehouseId: Int? = null,
    val status: String? = "brouillon",
    val subtotal: Double? = null,
    @SerialName("discount_total") val discountTotal: Double? = null,
    @SerialName("tax_amount") val taxAmount: Double? = null,
    val total: Double? = null,
    val notes: String? = null,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("paid_at") val paidAt: String? = null,
    @SerialName("customer_name") val customerName: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class InvoiceItemDTO(
    val id: Int,
    @SerialName("invoice_id") val invoiceId: Int? = null,
    @SerialName("product_id") val productId: Int? = null,
    @SerialName("product_name") val productName: String? = null,
    @SerialName("product_sku") val productSku: String? = null,
    val quantity: Int,
    @SerialName("unit_price") val unitPrice: Double,
    @SerialName("discount_percent") val discountPercent: Double? = null,
    @SerialName("tax_rate") val taxRate: Double? = 20.0,
    @SerialName("line_total") val lineTotal: Double,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class InvoiceCreateRequest(
    @SerialName("customer_id") val customerId: Int? = null,
    val status: String? = null,
    val notes: String? = null,
    @SerialName("due_date") val dueDate: String? = null
)

@Serializable
data class InvoiceItemRequest(
    @SerialName("product_id") val productId: Int,
    val quantity: Int,
    @SerialName("unit_price") val unitPrice: Double,
    @SerialName("discount_percent") val discountPercent: Double? = null
)
