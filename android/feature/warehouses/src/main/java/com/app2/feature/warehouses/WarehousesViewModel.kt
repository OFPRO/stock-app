package com.app2.feature.warehouses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.remote.WarehouseApiService
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

private fun JsonElement?.optInt(): Int? =
    this?.jsonPrimitive?.intOrNull

data class WarehouseListItem(
    val id: Int,
    val name: String,
    val address: String?,
    val manager: String?,
    val phone: String?,
    val isDefault: Boolean,
    val createdAt: String?
)

@HiltViewModel
class WarehousesViewModel @Inject constructor(
    private val warehouseApi: WarehouseApiService
) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<List<WarehouseListItem>>>(ViewState.Loading)
    val state = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private var allWarehouses: List<WarehouseListItem> = emptyList()

    init {
        loadWarehouses()
    }

    fun loadWarehouses() {
        viewModelScope.launch {
            _state.value = ViewState.Loading
            try {
                val response = warehouseApi.getWarehouses()
                allWarehouses = parseWarehouseList(response)
                applyFilter()
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de chargement des entrepôts")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    fun refresh() {
        allWarehouses = emptyList()
        loadWarehouses()
    }

    private fun applyFilter() {
        val query = _searchQuery.value.trim().lowercase()
        val filtered = if (query.isEmpty()) {
            allWarehouses
        } else {
            allWarehouses.filter { w ->
                w.name.lowercase().contains(query) ||
                    w.address?.lowercase()?.contains(query) == true ||
                    w.manager?.lowercase()?.contains(query) == true
            }
        }
        _state.value = if (filtered.isEmpty()) {
            ViewState.Empty
        } else {
            ViewState.Loaded(filtered)
        }
    }

    private fun parseWarehouseList(json: JsonElement): List<WarehouseListItem> {
        return json.jsonArray.mapNotNull { item ->
            val obj = item.jsonObject
            val id = obj["id"].optInt() ?: return@mapNotNull null
            WarehouseListItem(
                id = id,
                name = obj["name"].optString() ?: "",
                address = obj["address"].optString(),
                manager = obj["manager"].optString(),
                phone = obj["phone"].optString(),
                isDefault = obj["is_default"].optInt() == 1,
                createdAt = obj["created_at"].optString()
            )
        }
    }
}
