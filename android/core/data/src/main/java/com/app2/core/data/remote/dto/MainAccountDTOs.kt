package com.app2.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MainAccountDTO(
    val id: Int? = null,
    @SerialName("account_number") val accountNumber: String? = null,
    @SerialName("current_balance") val currentBalance: Double? = null,
    @SerialName("account") val account: MainAccountDetailDTO? = null
)

@Serializable
data class MainAccountDetailDTO(
    @SerialName("current_balance") val currentBalance: Double? = null,
    @SerialName("account_number") val accountNumber: String? = null
)
