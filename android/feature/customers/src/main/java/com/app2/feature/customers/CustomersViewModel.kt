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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject

private fun JsonElement?.optString(): String? {
    val prim = this?.jsonPrimitive
    return if (prim != null && prim !is JsonNull) prim.content else null
}

private fun JsonElement?.optDouble(): Double? =
    this?.jsonPrimitive?.doubleOrNull

private fun JsonElement?.optInt(): Int? =
    this?.jsonPrimitive?.intOrNull

data class CustomerListItem(
    val id: Int,
    val name: String,
    val type: String?,
    val email: String?,
    val phone: String?,
    val address: String?,
    val clientCode: String?,
    val discountRate: Double?,
    val isLoyal: Boolean,
    val isActive: Boolean,
    val notes: String?,
    val createdAt: String?
)

@HiltViewModel
class CustomersViewModel @Inject constructor(
    private val customerApi: CustomerApiService
) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<List<CustomerListItem>>>(ViewState.Loading)
    val state = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private var allCustomers: List<CustomerListItem> = emptyList()

    init {
        loadCustomers()
    }

    fun loadCustomers() {
        viewModelScope.launch {
            _state.value = ViewState.Loading
            try {
                val response = customerApi.getCustomers(search = null)
                allCustomers = parseCustomerList(response)
                applyFilter()
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de chargement des clients")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    fun deleteCustomer(id: Int) {
        viewModelScope.launch {
            try {
                customerApi.deleteCustomer(id)
                loadCustomers()
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de suppression")
            }
        }
    }

    fun refresh() {
        allCustomers = emptyList()
        loadCustomers()
    }

    private fun applyFilter() {
        val query = _searchQuery.value.trim().lowercase()
        val filtered = if (query.isEmpty()) {
            allCustomers
        } else {
            allCustomers.filter { customer ->
                customer.name.lowercase().contains(query) ||
                    customer.clientCode?.lowercase()?.contains(query) == true ||
                    customer.email?.lowercase()?.contains(query) == true ||
                    customer.phone?.lowercase()?.contains(query) == true
            }
        }
        _state.value = if (filtered.isEmpty()) {
            ViewState.Empty
        } else {
            ViewState.Loaded(filtered)
        }
    }

    private fun parseCustomerList(json: JsonElement): List<CustomerListItem> {
        return json.jsonArray.mapNotNull { item ->
            val obj = item.jsonObject
            val id = obj["id"].optInt() ?: return@mapNotNull null
            CustomerListItem(
                id = id,
                name = obj["name"].optString() ?: "",
                type = obj["type"].optString(),
                email = obj["email"].optString(),
                phone = obj["phone"].optString(),
                address = obj["address"].optString(),
                clientCode = obj["client_code"].optString(),
                discountRate = obj["discount_rate"].optDouble(),
                isLoyal = obj["is_loyal"].optInt() == 1,
                isActive = obj["is_active"]?.optInt() != 0,
                notes = obj["notes"].optString(),
                createdAt = obj["created_at"].optString()
            )
        }
    }
}
