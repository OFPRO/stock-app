package com.app2.feature.warehouses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.remote.LocationApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

@HiltViewModel
class LocationFormViewModel @Inject constructor(
    private val locationApi: LocationApiService
) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

    private val _type = MutableStateFlow("rack")
    val type = _type.asStateFlow()

    private val _capacity = MutableStateFlow("")
    val capacity = _capacity.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private var editId: Int? = null
    private var initialized = false

    fun initForm(editData: LocationListItem?) {
        if (initialized) return
        initialized = true
        if (editData != null) {
            _name.value = editData.name
            _type.value = editData.type ?: "rack"
            _capacity.value = editData.capacity?.toString() ?: ""
            editId = editData.id
        }
    }

    fun onNameChanged(v: String) { _name.value = v }
    fun onTypeChanged(v: String) { _type.value = v }
    fun onCapacityChanged(v: String) { _capacity.value = v }

    fun save(warehouseId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val capacityVal = _capacity.value.toIntOrNull()
                val body = buildJsonObject {
                    put("name", _name.value)
                    put("warehouse_id", warehouseId)
                    put("type", _type.value)
                    if (capacityVal != null) put("capacity", capacityVal)
                }
                val id = editId
                if (id != null) {
                    locationApi.updateLocation(id, body)
                } else {
                    locationApi.createLocation(body)
                }
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Erreur d'enregistrement"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
