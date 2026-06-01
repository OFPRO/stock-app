package com.app2.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationDTO(
    val id: Int,
    val type: String,
    val title: String? = null,
    val message: String? = null,
    @SerialName("product_id") val productId: Int? = null,
    @SerialName("warehouse_id") val warehouseId: Int? = null,
    @SerialName("warehouse_name") val warehouseName: String? = null,
    @SerialName("is_read") val isRead: Int? = null,
    @SerialName("link_type") val linkType: String? = null,
    @SerialName("link_id") val linkId: Int? = null,
    @SerialName("created_at") val createdAt: String? = null
)
