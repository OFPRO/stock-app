package com.app2.feature.warehouses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.repository.LocationRepository
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<List<LocationListItem>>>(ViewState.Loading)
    val state = _state.asStateFlow()

    private var warehouseId: Int? = null

    fun loadLocations(whId: Int) {
        warehouseId = whId
        viewModelScope.launch {
            _state.value = ViewState.Loading
            try {
                val list = locationRepository.getLocations(warehouseId = whId).map { dto ->
                    LocationListItem(
                        id = dto.id,
                        warehouseId = dto.warehouseId,
                        name = dto.name,
                        type = dto.type,
                        capacity = dto.capacity,
                        createdAt = dto.createdAt
                    )
                }
                _state.value = if (list.isEmpty()) ViewState.Empty else ViewState.Loaded(list)
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de chargement des zones")
            }
        }
    }

    fun deleteLocation(id: Int) {
        viewModelScope.launch {
            try {
                locationRepository.deleteLocation(id)
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
}
