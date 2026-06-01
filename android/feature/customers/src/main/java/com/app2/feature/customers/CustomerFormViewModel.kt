package com.app2.feature.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.remote.CustomerApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

@HiltViewModel
class CustomerFormViewModel @Inject constructor(
    private val customerApi: CustomerApiService
) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

    private val _type = MutableStateFlow("particulier")
    val type = _type.asStateFlow()

    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()

    private val _phone = MutableStateFlow("")
    val phone = _phone.asStateFlow()

    private val _address = MutableStateFlow("")
    val address = _address.asStateFlow()

    private val _isLoyal = MutableStateFlow(false)
    val isLoyal = _isLoyal.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes = _notes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private var editId: Int? = null
    private var initialized = false

    fun initForm(editData: CustomerDetailData?) {
        if (initialized) return
        initialized = true
        if (editData != null) {
            _name.value = editData.name
            _type.value = editData.type ?: "particulier"
            _email.value = editData.email ?: ""
            _phone.value = editData.phone ?: ""
            _address.value = editData.address ?: ""
            _isLoyal.value = editData.isLoyal
            _notes.value = editData.notes ?: ""
            editId = editData.id
        }
    }

    fun onNameChanged(v: String) { _name.value = v }
    fun onTypeChanged(v: String) { _type.value = v }
    fun onEmailChanged(v: String) { _email.value = v }
    fun onPhoneChanged(v: String) { _phone.value = v }
    fun onAddressChanged(v: String) { _address.value = v }
    fun onLoyalChanged(v: Boolean) { _isLoyal.value = v }
    fun onNotesChanged(v: String) { _notes.value = v }

    fun save(isEdit: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val body = buildJsonObject {
                    put("name", _name.value)
                    put("type", _type.value)
                    put("email", _email.value.ifBlank { "" })
                    put("phone", _phone.value.ifBlank { "" })
                    put("address", _address.value.ifBlank { "" })
                    put("discount_rate", 0)
                    put("is_loyal", if (_isLoyal.value) 1 else 0)
                    put("notes", _notes.value.ifBlank { "" })
                }
                if (isEdit) {
                    val id = editId ?: return@launch
                    customerApi.updateCustomer(id, body)
                } else {
                    customerApi.createCustomer(body)
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
