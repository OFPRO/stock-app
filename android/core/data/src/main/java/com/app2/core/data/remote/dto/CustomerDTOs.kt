package com.app2.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CustomerDTO(
    val id: Int,
    val name: String,
    val type: String? = "particulier",
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    @SerialName("client_code") val clientCode: String? = null,
    @SerialName("discount_rate") val discountRate: Double? = null,
    @SerialName("is_loyal") val isLoyal: Int? = null,
    @SerialName("is_active") val isActive: Int? = null,
    val ice: String? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class CustomerCreateRequest(
    val name: String,
    val type: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    @SerialName("client_code") val clientCode: String? = null,
    @SerialName("discount_rate") val discountRate: Double? = null
)

@Serializable
data class CustomerUpdateRequest(
    val name: String? = null,
    val type: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    @SerialName("discount_rate") val discountRate: Double? = null,
    @SerialName("is_active") val isActive: Int? = null
)
