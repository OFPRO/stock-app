package com.app2.feature.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.remote.POSApiService
import com.app2.core.data.remote.ProductApiService
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
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

private fun JsonElement?.optDouble(): Double? = this?.jsonPrimitive?.doubleOrNull
private fun JsonElement?.optInt(): Int? = this?.jsonPrimitive?.intOrNull

data class POSProduct(
    val id: Int,
    val name: String,
    val sku: String,
    val barcode: String?,
    val salePrice: Double,
    val priceBase: Double,
    val priceLoyal: Double?,
    val priceSchool: Double?,
    val priceStudent: Double?,
    val quantity: Int,
    val category: String?
)

data class CartItem(
    val product: POSProduct,
    var quantity: Int = 1
) {
    val lineTotal: Double get() = product.salePrice * quantity
}

data class POSCustomer(
    val id: Int,
    val name: String,
    val discountRate: Int? = null
)

data class PaymentResult(
    val success: Boolean,
    val documentNumber: String?,
    val total: Double,
    val changeAmount: Double,
    val customerName: String?
)

@HiltViewModel
class POSRegisterViewModel @Inject constructor(
    private val posApi: POSApiService,
    private val productApi: ProductApiService
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _products = MutableStateFlow<ViewState<List<POSProduct>>>(ViewState.Loading)
    val products = _products.asStateFlow()

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart = _cart.asStateFlow()

    private val _selectedCustomer = MutableStateFlow<POSCustomer?>(null)
    val selectedCustomer = _selectedCustomer.asStateFlow()

    private val _customers = MutableStateFlow<List<POSCustomer>>(emptyList())
    val customers = _customers.asStateFlow()

    private val _paymentState = MutableStateFlow<ViewState<PaymentResult>?>(null)
    val paymentState = _paymentState.asStateFlow()

    private val _bestSellers = MutableStateFlow<List<POSProduct>>(emptyList())
    val bestSellers = _bestSellers.asStateFlow()

    var currentSessionId: Int = 0

    fun loadProducts(search: String = "") {
        viewModelScope.launch {
            _searchQuery.value = search
            _products.value = ViewState.Loading
            try {
                val custId = _selectedCustomer.value?.id
                val response = productApi.getProductsForSale(
                    search = search.ifBlank { null },
                    customerId = custId
                )
                _products.value = ViewState.Loaded(parseProducts(response))
            } catch (e: Exception) {
                _products.value = ViewState.Error(e.message ?: "Erreur de chargement")
            }
        }
    }

    fun loadCustomers() {
        viewModelScope.launch {
            try {
                val response = posApi.getPOSCustomers()
                _customers.value = parseCustomers(response)
            } catch (_: Exception) {}
        }
    }

    fun loadBestSellers() {
        viewModelScope.launch {
            try {
                val response = posApi.getBestSellers()
                _bestSellers.value = parseProducts(response)
            } catch (_: Exception) {}
        }
    }

    fun selectCustomer(customer: POSCustomer?) {
        _selectedCustomer.value = customer
        loadProducts()
    }

    fun addToCart(product: POSProduct) {
        val current = _cart.value.toMutableList()
        val existing = current.indexOfFirst { it.product.id == product.id }
        if (existing >= 0) {
            current[existing] = current[existing].copy(quantity = current[existing].quantity + 1)
        } else {
            current.add(CartItem(product))
        }
        _cart.value = current
    }

    fun updateQuantity(productId: Int, quantity: Int) {
        if (quantity <= 0) { removeFromCart(productId); return }
        val current = _cart.value.toMutableList()
        val idx = current.indexOfFirst { it.product.id == productId }
        if (idx >= 0) {
            current[idx] = current[idx].copy(quantity = quantity)
            _cart.value = current
        }
    }

    fun removeFromCart(productId: Int) {
        _cart.value = _cart.value.filter { it.product.id != productId }
    }

    val subtotal: Double get() = _cart.value.sumOf { it.lineTotal }
    val itemCount: Int get() = _cart.value.sumOf { it.quantity }
    val isCartEmpty: Boolean get() = _cart.value.isEmpty()

    fun checkout(paymentMethod: String, tenderedAmount: Double) {
        val cartItems = _cart.value
        if (cartItems.isEmpty()) return

        viewModelScope.launch {
            _paymentState.value = ViewState.Loading
            try {
                val body = buildJsonObject {
                    put("session_id", currentSessionId)
                    put("payment_method", paymentMethod)
                    put("tendered_amount", tenderedAmount)
                    _selectedCustomer.value?.let { put("customer_id", it.id) }
                    put("items", buildJsonArray {
                        cartItems.forEach { item ->
                            add(buildJsonObject {
                                put("product_id", item.product.id)
                                put("quantity", item.quantity)
                                put("unit_price", item.product.salePrice)
                            })
                        }
                    })
                }
                val response = posApi.createTransaction(body)
                val obj = response.jsonObject
                val success = obj["success"]?.jsonPrimitive?.content == "true"
                _paymentState.value = ViewState.Loaded(
                    PaymentResult(
                        success = success,
                        documentNumber = obj["document_number"].optString(),
                        total = obj["total"].optDouble() ?: 0.0,
                        changeAmount = obj["change_amount"].optDouble() ?: 0.0,
                        customerName = obj["customer_name"].optString()
                    )
                )
                if (success) {
                    _cart.value = emptyList()
                    loadBestSellers()
                }
            } catch (e: Exception) {
                _paymentState.value = ViewState.Error(e.message ?: "Erreur de paiement")
            }
        }
    }

    fun clearPaymentState() { _paymentState.value = null }

    private fun parseProducts(json: JsonElement): List<POSProduct> {
        return json.jsonArray.mapNotNull { item ->
            val obj = item.jsonObject
            val id = obj["id"].optInt() ?: return@mapNotNull null
            POSProduct(
                id = id,
                name = obj["name"].optString() ?: "",
                sku = obj["sku"].optString() ?: "",
                barcode = obj["barcode"].optString(),
                salePrice = (obj["sale_price"].optDouble() ?: obj["price"].optDouble() ?: 0.0),
                priceBase = obj["price_base"].optDouble() ?: obj["price"].optDouble() ?: 0.0,
                priceLoyal = obj["price_loyal"].optDouble(),
                priceSchool = obj["price_school"].optDouble(),
                priceStudent = obj["price_student"].optDouble(),
                quantity = obj["quantity"].optInt() ?: 0,
                category = obj["category"].optString()
            )
        }
    }

    private fun parseCustomers(json: JsonElement): List<POSCustomer> {
        return json.jsonArray.mapNotNull { item ->
            val obj = item.jsonObject
            val id = obj["id"].optInt() ?: return@mapNotNull null
            POSCustomer(
                id = id,
                name = obj["name"].optString() ?: "",
                discountRate = obj["discount_rate"].optInt()
            )
        }
    }
}
