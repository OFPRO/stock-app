package com.app2.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WarehouseDTO(
    val id: Int,
    val name: String,
    val address: String? = null,
    val manager: String? = null,
    val phone: String? = null,
    @SerialName("is_default") val isDefault: Int? = null,
    val ice: String? = null,
    val patente: String? = null,
    val rc: String? = null,
    @SerialName("taxe_number") val taxeNumber: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class WarehouseCreateRequest(
    val name: String,
    val address: String? = null,
    val manager: String? = null,
    val phone: String? = null,
    val ice: String? = null,
    val patente: String? = null,
    val rc: String? = null,
    @SerialName("taxe_number") val taxeNumber: String? = null
)
