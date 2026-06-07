package com.app2.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReorderRuleDTO(
    val id: Int,
    @SerialName("product_id") val productId: Int,
    @SerialName("product_name") val productName: String? = null,
    @SerialName("warehouse_id") val warehouseId: Int? = null,
    @SerialName("min_quantity") val minQuantity: Int? = null,
    @SerialName("max_quantity") val maxQuantity: Int? = null,
    @SerialName("trigger_type") val triggerType: String? = "manual",
    @SerialName("supplier_id") val supplierId: Int? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ReorderRuleCreateRequest(
    @SerialName("product_id") val productId: Int,
    @SerialName("warehouse_id") val warehouseId: Int? = null,
    @SerialName("min_quantity") val minQuantity: Int? = null,
    @SerialName("max_quantity") val maxQuantity: Int? = null,
    @SerialName("trigger_type") val triggerType: String? = null,
    @SerialName("supplier_id") val supplierId: Int? = null
)

@Serializable
data class ReplenishmentSuggestion(
    val id: Int? = null,
    @SerialName("product_id") val productId: Int,
    @SerialName("product_name") val productName: String? = null,
    @SerialName("current_quantity") val currentQuantity: Int? = null,
    @SerialName("suggested_quantity") val suggestedQuantity: Int? = null,
    @SerialName("warehouse_id") val warehouseId: Int? = null,
    @SerialName("warehouse_name") val warehouseName: String? = null
)
