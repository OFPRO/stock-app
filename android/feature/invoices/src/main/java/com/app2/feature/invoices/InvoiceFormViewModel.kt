package com.app2.feature.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.remote.CustomerApiService
import com.app2.core.data.remote.InvoiceApiService
import com.app2.core.data.remote.ProductApiService
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
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

private fun JsonElement?.optDouble(): Double? =
    this?.jsonPrimitive?.doubleOrNull

data class CustomerOption(
    val id: Int,
    val name: String
)

data class ProductSearchResult(
    val id: Int,
    val name: String,
    val price: Double
)

data class InvoiceLineItem(
    val productId: Int,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double
) {
    val lineTotal: Double get() = quantity * unitPrice
}

@HiltViewModel
class InvoiceFormViewModel @Inject constructor(
    private val invoiceApi: InvoiceApiService,
    private val customerApi: CustomerApiService,
    private val productApi: ProductApiService
) : ViewModel() {

    private val _customers = MutableStateFlow<ViewState<List<CustomerOption>>>(ViewState.Loading)
    val customers = _customers.asStateFlow()

    private val _selectedCustomerId = MutableStateFlow<Int?>(null)
    val selectedCustomerId = _selectedCustomerId.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes = _notes.asStateFlow()

    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery = _productSearchQuery.asStateFlow()

    private val _productResults = MutableStateFlow<List<ProductSearchResult>>(emptyList())
    val productResults = _productResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _lines = MutableStateFlow<List<InvoiceLineItem>>(emptyList())
    val lines = _lines.asStateFlow()

    val total: Double get() = _lines.value.sumOf { it.lineTotal }

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private var searchJob: Job? = null
    private var initialized = false

    fun initForm() {
        if (initialized) return
        initialized = true
        loadCustomers()
    }

    fun onCustomerSelected(id: Int) { _selectedCustomerId.value = id }
    fun onNotesChanged(v: String) { _notes.value = v }

    fun onProductSearchChanged(query: String) {
        _productSearchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            if (query.isBlank()) {
                _productResults.value = emptyList()
                _isSearching.value = false
                return@launch
            }
            _isSearching.value = true
            try {
                val response = productApi.getProductsForSale(search = query)
                _productResults.value = response.jsonArray.mapNotNull { item ->
                    val o = item.jsonObject
                    val id = o["id"].optInt() ?: return@mapNotNull null
                    ProductSearchResult(
                        id = id,
                        name = o["name"].optString() ?: "",
                        price = o["price"].optDouble() ?: 0.0
                    )
                }
            } catch (_: Exception) {
                _productResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun addProductLine(result: ProductSearchResult) {
        val existing = _lines.value.indexOfFirst { it.productId == result.id }
        if (existing >= 0) {
            val lines = _lines.value.toMutableList()
            lines[existing] = lines[existing].copy(quantity = lines[existing].quantity + 1)
            _lines.value = lines
        } else {
            _lines.value = _lines.value + InvoiceLineItem(
                productId = result.id,
                productName = result.name,
                quantity = 1,
                unitPrice = result.price
            )
        }
        _productSearchQuery.value = ""
        _productResults.value = emptyList()
    }

    fun updateLineQuantity(index: Int, quantity: Int) {
        if (quantity <= 0) {
            removeLine(index)
            return
        }
        val lines = _lines.value.toMutableList()
        if (index in lines.indices) {
            lines[index] = lines[index].copy(quantity = quantity)
            _lines.value = lines
        }
    }

    fun updateLineUnitPrice(index: Int, price: Double) {
        val lines = _lines.value.toMutableList()
        if (index in lines.indices) {
            lines[index] = lines[index].copy(unitPrice = price)
            _lines.value = lines
        }
    }

    fun removeLine(index: Int) {
        val lines = _lines.value.toMutableList()
        if (index in lines.indices) {
            lines.removeAt(index)
            _lines.value = lines
        }
    }

    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val customerId = _selectedCustomerId.value ?: run {
                    _errorMessage.value = "Veuillez sélectionner un client"
                    return@launch
                }
                if (_lines.value.isEmpty()) {
                    _errorMessage.value = "Ajoutez au moins un article"
                    return@launch
                }

                val createBody = buildJsonObject {
                    put("customer_id", customerId)
                    put("status", "brouillon")
                    put("notes", _notes.value.ifBlank { "" })
                }
                val createResponse = invoiceApi.createInvoice(createBody)
                val invoiceId = createResponse.jsonObject["id"]?.optInt()
                    ?: throw Exception("Impossible de récupérer l'ID de la facture")

                for (line in _lines.value) {
                    val itemBody = buildJsonObject {
                        put("product_id", line.productId)
                        put("quantity", line.quantity)
                        put("unit_price", line.unitPrice)
                    }
                    invoiceApi.addInvoiceItem(invoiceId, itemBody)
                }

                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Erreur de création"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadCustomers() {
        viewModelScope.launch {
            try {
                val response = customerApi.getCustomers()
                _customers.value = ViewState.Loaded(
                    response.jsonArray.mapNotNull { item ->
                        val o = item.jsonObject
                        val id = o["id"].optInt() ?: return@mapNotNull null
                        CustomerOption(id = id, name = o["name"].optString() ?: "")
                    }
                )
            } catch (e: Exception) {
                _customers.value = ViewState.Error(e.message ?: "Erreur de chargement des clients")
            }
        }
    }
}
