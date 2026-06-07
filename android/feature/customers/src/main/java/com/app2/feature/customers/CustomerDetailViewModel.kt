package com.app2.feature.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.repository.CustomerRepository
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerDetailData(
    val id: Int,
    val name: String,
    val type: String?,
    val email: String?,
    val phone: String?,
    val address: String?,
    val clientCode: String?,
    val discountRate: Double?,
    val isLoyal: Boolean,
    val active: Boolean,
    val ice: String?,
    val notes: String?,
    val createdAt: String?
)

@HiltViewModel
class CustomerDetailViewModel @Inject constructor(
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<CustomerDetailData>>(ViewState.Loading)
    val state = _state.asStateFlow()

    private var lastLoadedId: Int? = null

    fun loadCustomer(id: Int) {
        if (lastLoadedId == id) return
        lastLoadedId = id
        viewModelScope.launch {
            _state.value = ViewState.Loading
            try {
                val dto = customerRepository.getCustomer(id)
                _state.value = ViewState.Loaded(
                    CustomerDetailData(
                        id = dto.id,
                        name = dto.name,
                        type = dto.type,
                        email = dto.email,
                        phone = dto.phone,
                        address = dto.address,
                        clientCode = dto.clientCode,
                        discountRate = dto.discountRate,
                        isLoyal = dto.isLoyal == 1,
                        active = dto.isActive?.let { it != 0 } ?: true,
                        ice = dto.ice,
                        notes = dto.notes,
                        createdAt = dto.createdAt
                    )
                )
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de chargement du client")
            }
        }
    }

    fun deleteCustomer(onDeleted: () -> Unit) {
        val id = lastLoadedId ?: return
        viewModelScope.launch {
            _state.value = ViewState.Loading
            try {
                customerRepository.deleteCustomer(id)
                onDeleted()
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de suppression")
            }
        }
    }
}
