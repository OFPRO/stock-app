package com.app2.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupplierDTO(
    val id: Int,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    @SerialName("contact_person") val contactPerson: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class SupplierCreateRequest(
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    @SerialName("contact_person") val contactPerson: String? = null
)

@Serializable
data class SupplierUpdateRequest(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    @SerialName("contact_person") val contactPerson: String? = null
)
