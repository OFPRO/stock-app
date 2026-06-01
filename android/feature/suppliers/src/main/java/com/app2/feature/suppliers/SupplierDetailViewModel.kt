package com.app2.feature.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.remote.SupplierApiService
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
    private val supplierApi: SupplierApiService
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
                val response = supplierApi.getSuppliers()
                val items = response.jsonArray
                val obj = items.firstOrNull { it.jsonObject["id"]?.optInt() == id }?.jsonObject
                if (obj != null) {
                    _state.value = ViewState.Loaded(parseSupplierDetail(obj))
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
                supplierApi.deleteSupplier(id)
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de suppression")
            }
        }
    }

    private fun parseSupplierDetail(obj: JsonElement): SupplierDetailData {
        val o = obj.jsonObject
        return SupplierDetailData(
            id = o["id"].optInt() ?: 0,
            name = o["name"].optString() ?: "",
            email = o["email"].optString(),
            phone = o["phone"].optString(),
            address = o["address"].optString(),
            contactPerson = o["contact_person"].optString(),
            createdAt = o["created_at"].optString()
        )
    }
}
