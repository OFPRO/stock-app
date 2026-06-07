package com.app2.feature.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.repository.SupplierRepository
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupplierDetailData(
    val id: Int,
    val name: String,
    val email: String?,
    val phone: String?,
    val address: String?,
    val contactPerson: String?,
    val createdAt: String?
)

@HiltViewModel
class SupplierDetailViewModel @Inject constructor(
    private val supplierRepository: SupplierRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<SupplierDetailData>>(ViewState.Loading)
    val state = _state.asStateFlow()

    private var lastLoadedId: Int? = null

    fun loadSupplier(id: Int) {
        if (lastLoadedId == id) return
        lastLoadedId = id
        viewModelScope.launch {
            _state.value = ViewState.Loading
            try {
                val items = supplierRepository.getSuppliers()
                val dto = items.firstOrNull { it.id == id }
                if (dto != null) {
                    _state.value = ViewState.Loaded(
                        SupplierDetailData(
                            id = dto.id,
                            name = dto.name,
                            email = dto.email,
                            phone = dto.phone,
                            address = dto.address,
                            contactPerson = dto.contactPerson,
                            createdAt = dto.createdAt
                        )
                    )
                } else {
                    _state.value = ViewState.Error("Fournisseur introuvable")
                }
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de chargement du fournisseur")
            }
        }
    }

    fun deleteSupplier() {
        val id = lastLoadedId ?: return
        viewModelScope.launch {
            try {
                supplierRepository.deleteSupplier(id)
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de suppression")
            }
        }
    }
}
