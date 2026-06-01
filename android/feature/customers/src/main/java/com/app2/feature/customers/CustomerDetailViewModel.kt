package com.app2.feature.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.remote.CustomerApiService
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

private fun JsonElement?.optString(): String? {
    val prim = this?.jsonPrimitive
    return if (prim != null && prim !is JsonNull) prim.content else null
}

private fun JsonElement?.optDouble(): Double? =
    this?.jsonPrimitive?.doubleOrNull

private fun JsonElement?.optInt(): Int? =
    this?.jsonPrimitive?.intOrNull

data class CustomerDetailData(
    val id: Int,
    val name: String,
    val type: String?,
    val email: String?,
    val phone: String?,
    val address: String?,
    val clientCode: String?,
    val discountRate: Double?,
    val isLoyal: Boolean,
    val active: Boolean,
    val ice: String?,
    val notes: String?,
    val createdAt: String?
)

@HiltViewModel
class CustomerDetailViewModel @Inject constructor(
    private val customerApi: CustomerApiService
) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<CustomerDetailData>>(ViewState.Loading)
    val state = _state.asStateFlow()

    private var lastLoadedId: Int? = null

    fun loadCustomer(id: Int) {
        if (lastLoadedId == id) return
        lastLoadedId = id
        viewModelScope.launch {
            _state.value = ViewState.Loading
            try {
                val response = customerApi.getCustomer(id)
                _state.value = ViewState.Loaded(parseCustomerDetail(response))
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de chargement du client")
            }
        }
    }

    fun deleteCustomer(onDeleted: () -> Unit) {
        val id = lastLoadedId ?: return
        viewModelScope.launch {
            _state.value = ViewState.Loading
            try {
                customerApi.deleteCustomer(id)
                onDeleted()
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de suppression")
            }
        }
    }

    private fun parseCustomerDetail(json: JsonElement): CustomerDetailData {
        val obj = json.jsonObject
        return CustomerDetailData(
            id = obj["id"].optInt() ?: 0,
            name = obj["name"].optString() ?: "",
            type = obj["type"].optString(),
            email = obj["email"].optString(),
            phone = obj["phone"].optString(),
            address = obj["address"].optString(),
            clientCode = obj["client_code"].optString(),
            discountRate = obj["discount_rate"].optDouble(),
            isLoyal = obj["is_loyal"].optInt() == 1,
            active = obj["is_active"]?.optInt() != 0,
            ice = obj["ice"].optString(),
            notes = obj["notes"].optString(),
            createdAt = obj["created_at"].optString()
        )
    }
}
