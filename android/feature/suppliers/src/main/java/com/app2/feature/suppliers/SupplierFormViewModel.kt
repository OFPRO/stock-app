package com.app2.feature.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.remote.SupplierApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

@HiltViewModel
class SupplierFormViewModel @Inject constructor(
    private val supplierApi: SupplierApiService
) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()

    private val _phone = MutableStateFlow("")
    val phone = _phone.asStateFlow()

    private val _address = MutableStateFlow("")
    val address = _address.asStateFlow()

    private val _contactPerson = MutableStateFlow("")
    val contactPerson = _contactPerson.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private var editId: Int? = null
    private var initialized = false

    fun initForm(editData: SupplierDetailData?) {
        if (initialized) return
        initialized = true
        if (editData != null) {
            _name.value = editData.name
            _email.value = editData.email ?: ""
            _phone.value = editData.phone ?: ""
            _address.value = editData.address ?: ""
            _contactPerson.value = editData.contactPerson ?: ""
            editId = editData.id
        }
    }

    fun onNameChanged(v: String) { _name.value = v }
    fun onEmailChanged(v: String) { _email.value = v }
    fun onPhoneChanged(v: String) { _phone.value = v }
    fun onAddressChanged(v: String) { _address.value = v }
    fun onContactPersonChanged(v: String) { _contactPerson.value = v }

    fun save(isEdit: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val body = buildJsonObject {
                    put("name", _name.value)
                    put("email", _email.value.ifBlank { "" })
                    put("phone", _phone.value.ifBlank { "" })
                    put("address", _address.value.ifBlank { "" })
                    put("contact_person", _contactPerson.value.ifBlank { "" })
                }
                if (isEdit) {
                    val id = editId ?: return@launch
                    supplierApi.updateSupplier(id, body)
                } else {
                    supplierApi.createSupplier(body)
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
