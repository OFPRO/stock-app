package com.app2.feature.warehouses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.remote.WarehouseApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

@HiltViewModel
class WarehouseFormViewModel @Inject constructor(
    private val warehouseApi: WarehouseApiService
) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

    private val _address = MutableStateFlow("")
    val address = _address.asStateFlow()

    private val _manager = MutableStateFlow("")
    val manager = _manager.asStateFlow()

    private val _phone = MutableStateFlow("")
    val phone = _phone.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun onNameChanged(v: String) { _name.value = v }
    fun onAddressChanged(v: String) { _address.value = v }
    fun onManagerChanged(v: String) { _manager.value = v }
    fun onPhoneChanged(v: String) { _phone.value = v }

    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val body = buildJsonObject {
                    put("name", _name.value)
                    put("address", _address.value.ifBlank { "" })
                    put("manager", _manager.value.ifBlank { "" })
                    put("phone", _phone.value.ifBlank { "" })
                }
                warehouseApi.createWarehouse(body)
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Erreur d'enregistrement"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
