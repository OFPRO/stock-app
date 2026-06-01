package com.app2.feature.warehouses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.remote.LocationApiService
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
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

private fun JsonElement?.optInt(): Int? =
    this?.jsonPrimitive?.intOrNull

data class LocationListItem(
    val id: Int,
    val warehouseId: Int,
    val name: String,
    val type: String?,
    val capacity: Int?,
    val createdAt: String?
)

@HiltViewModel
class LocationsViewModel @Inject constructor(
    private val locationApi: LocationApiService
) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<List<LocationListItem>>>(ViewState.Loading)
    val state = _state.asStateFlow()

    private var warehouseId: Int? = null

    fun loadLocations(whId: Int) {
        warehouseId = whId
        viewModelScope.launch {
            _state.value = ViewState.Loading
            try {
                val response = locationApi.getLocations(warehouseId = whId)
                val list = parseLocationList(response)
                _state.value = if (list.isEmpty()) ViewState.Empty else ViewState.Loaded(list)
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de chargement des zones")
            }
        }
    }

    fun deleteLocation(id: Int) {
        viewModelScope.launch {
            try {
                locationApi.deleteLocation(id)
                val whId = warehouseId ?: return@launch
                loadLocations(whId)
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de suppression")
            }
        }
    }

    fun refresh() {
        val whId = warehouseId ?: return
        loadLocations(whId)
    }

    private fun parseLocationList(json: JsonElement): List<LocationListItem> {
        return json.jsonArray.mapNotNull { item ->
            val obj = item.jsonObject
            val id = obj["id"].optInt() ?: return@mapNotNull null
            LocationListItem(
                id = id,
                warehouseId = obj["warehouse_id"].optInt() ?: 0,
                name = obj["name"].optString() ?: "",
                type = obj["type"].optString(),
                capacity = obj["capacity"].optInt(),
                createdAt = obj["created_at"].optString()
            )
        }
    }
}
