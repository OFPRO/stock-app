package com.app2.feature.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.remote.SupplierApiService
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

private fun JsonElement?.optString(): String? {
    val prim = this?.jsonPrimitive
    return if (prim != null && prim !is JsonNull) prim.content else null
}

data class SupplierListItem(
    val id: Int,
    val name: String,
    val email: String?,
    val phone: String?,
    val address: String?,
    val contactPerson: String?,
    val createdAt: String?
)

@HiltViewModel
class SuppliersViewModel @Inject constructor(
    private val supplierApi: SupplierApiService
) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<List<SupplierListItem>>>(ViewState.Loading)
    val state = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private var allSuppliers: List<SupplierListItem> = emptyList()

    init {
        loadSuppliers()
    }

    fun loadSuppliers() {
        viewModelScope.launch {
            _state.value = ViewState.Loading
            try {
                val response = supplierApi.getSuppliers()
                allSuppliers = parseSupplierList(response)
                applyFilter()
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de chargement des fournisseurs")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    fun deleteSupplier(id: Int) {
        viewModelScope.launch {
            try {
                supplierApi.deleteSupplier(id)
                loadSuppliers()
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de suppression")
            }
        }
    }

    fun refresh() {
        allSuppliers = emptyList()
        loadSuppliers()
    }

    private fun applyFilter() {
        val query = _searchQuery.value.trim().lowercase()
        val filtered = if (query.isEmpty()) {
            allSuppliers
        } else {
            allSuppliers.filter { supplier ->
                supplier.name.lowercase().contains(query) ||
                    supplier.email?.lowercase()?.contains(query) == true ||
                    supplier.phone?.lowercase()?.contains(query) == true
            }
        }
        _state.value = if (filtered.isEmpty()) {
            ViewState.Empty
        } else {
            ViewState.Loaded(filtered)
        }
    }

    private fun parseSupplierList(json: JsonElement): List<SupplierListItem> {
        return json.jsonArray.mapNotNull { item ->
            val obj = item.jsonObject
            val id = obj["id"].optInt() ?: return@mapNotNull null
            SupplierListItem(
                id = id,
                name = obj["name"].optString() ?: "",
                email = obj["email"].optString(),
                phone = obj["phone"].optString(),
                address = obj["address"].optString(),
                contactPerson = obj["contact_person"].optString(),
                createdAt = obj["created_at"].optString()
            )
        }
    }
}

private fun JsonElement?.optInt(): Int? =
    this?.jsonPrimitive?.intOrNull
