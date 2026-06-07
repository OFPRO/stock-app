package com.app2.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocationDTO(
    val id: Int,
    @SerialName("warehouse_id") val warehouseId: Int,
    val name: String,
    val type: String? = "rack",
    val capacity: Int? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class LocationCreateRequest(
    @SerialName("warehouse_id") val warehouseId: Int,
    val name: String,
    val type: String? = null,
    val capacity: Int? = null
)

@Serializable
data class LocationUpdateRequest(
    val name: String? = null,
    val type: String? = null,
    val capacity: Int? = null
)
