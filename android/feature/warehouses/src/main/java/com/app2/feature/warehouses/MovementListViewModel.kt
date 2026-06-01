package com.app2.feature.warehouses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.remote.MovementApiService
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

data class MovementListItem(
    val id: Int,
    val productId: Int,
    val productName: String,
    val type: String,
    val quantity: Int,
    val sourceLocation: String?,
    val destLocation: String?,
    val note: String?,
    val createdAt: String?
)

@HiltViewModel
class MovementListViewModel @Inject constructor(
    private val movementApi: MovementApiService
) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<List<MovementListItem>>>(ViewState.Loading)
    val state = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(0)
    val selectedFilter = _selectedFilter.asStateFlow()

    private var allMovements: List<MovementListItem> = emptyList()

    val filters = listOf("Tous", "Entrées", "Sorties", "Transferts")

    init {
        loadMovements()
    }

    fun loadMovements() {
        viewModelScope.launch {
            _state.value = ViewState.Loading
            try {
                val response = movementApi.getMovements()
                allMovements = parseMovementList(response)
                applyFilters()
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de chargement des mouvements")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    fun onFilterSelected(index: Int) {
        _selectedFilter.value = index
        applyFilters()
    }

    fun refresh() {
        allMovements = emptyList()
        loadMovements()
    }

    private fun applyFilters() {
        val query = _searchQuery.value.trim().lowercase()
        val filterIndex = _selectedFilter.value

        val filtered = allMovements.filter { m ->
            val matchesSearch = query.isEmpty() ||
                m.productName.lowercase().contains(query) ||
                m.note?.lowercase()?.contains(query) == true

            val matchesFilter = when (filterIndex) {
                0 -> true
                1 -> m.type == "in" || m.type == "reception"
                2 -> m.type == "out" || m.type == "sale"
                3 -> m.type == "transfer" || m.type == "inter_warehouse"
                else -> true
            }

            matchesSearch && matchesFilter
        }

        _state.value = if (filtered.isEmpty()) {
            ViewState.Empty
        } else {
            ViewState.Loaded(filtered)
        }
    }

    private fun parseMovementList(json: JsonElement): List<MovementListItem> {
        return json.jsonArray.mapNotNull { item ->
            val obj = item.jsonObject
            val id = obj["id"].optInt() ?: return@mapNotNull null
            MovementListItem(
                id = id,
                productId = obj["product_id"].optInt() ?: 0,
                productName = obj["product_name"].optString() ?: "",
                type = obj["type"].optString() ?: "",
                quantity = obj["quantity"].optInt() ?: 0,
                sourceLocation = obj["source_location"].optString(),
                destLocation = obj["dest_location"].optString(),
                note = obj["note"].optString(),
                createdAt = obj["created_at"].optString()
            )
        }
    }
}
