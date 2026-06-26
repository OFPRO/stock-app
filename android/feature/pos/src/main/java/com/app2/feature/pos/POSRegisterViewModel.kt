package com.app2.feature.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.remote.dto.DiscountTier
import com.app2.core.data.remote.dto.ForSaleProductDTO
import com.app2.core.data.remote.dto.POSCustomerDTO
import com.app2.core.data.repository.POSRepository
import com.app2.core.data.repository.ProductRepository
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

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
    val discountRate: Int? = null,
    val pricingTier: DiscountTier = DiscountTier.Normal
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
    private val posRepository: POSRepository,
    private val productRepository: ProductRepository
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
                val result = productRepository.getProductsForSale(
                    search = search.ifBlank { null },
                    customerId = custId
                )
                _products.value = ViewState.Loaded(result.map { it.toPOSProduct() })
            } catch (e: Exception) {
                _products.value = ViewState.Error(e.message ?: "Erreur de chargement")
            }
        }
    }

    fun loadCustomers() {
        viewModelScope.launch {
            try {
                val result = posRepository.getPOSCustomers()
                _customers.value = result.map {
                    POSCustomer(
                        id = it.id,
                        name = it.name,
                        discountRate = it.discountRate?.toInt(),
                        pricingTier = DiscountTier.fromType(it.type)
                    )
                }
            } catch (_: Exception) {}
        }
    }

    fun loadBestSellers() {
        viewModelScope.launch {
            try {
                val result = posRepository.getBestSellers()
                _bestSellers.value = result.map {
                    POSProduct(
                        id = it.id,
                        name = it.name,
                        sku = it.sku,
                        barcode = null,
                        salePrice = it.price,
                        priceBase = it.priceBase ?: it.price,
                        priceLoyal = null,
                        priceSchool = null,
                        priceStudent = null,
                        quantity = it.quantity ?: 0,
                        category = null
                    )
                }
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
                    _selectedCustomer.value?.let { cust ->
                        put("customer_id", cust.id)
                        put("pricing_tier", cust.pricingTier.name.lowercase())
                    }
                    put("items", buildJsonArray {
                        cartItems.forEach { item ->
                            add(buildJsonObject {
                                put("product_id", item.product.id)
                                put("product_name", item.product.name)
                                put("product_sku", item.product.sku)
                                put("quantity", item.quantity)
                                put("unit_price", item.product.salePrice)
                            })
                        }
                    })
                }
                val result = posRepository.createTransaction(body)
                _paymentState.value = ViewState.Loaded(
                    PaymentResult(
                        success = result.success,
                        documentNumber = result.documentNumber,
                        total = result.total ?: 0.0,
                        changeAmount = result.changeAmount ?: 0.0,
                        customerName = result.customerName
                    )
                )
                if (result.success) {
                    _cart.value = emptyList()
                    loadBestSellers()
                }
            } catch (e: Exception) {
                _paymentState.value = ViewState.Error(e.message ?: "Erreur de paiement")
            }
        }
    }

    fun clearPaymentState() { _paymentState.value = null }
}

private fun ForSaleProductDTO.toPOSProduct() = POSProduct(
    id = id,
    name = name,
    sku = sku,
    barcode = barcode,
    salePrice = salePrice ?: price,
    priceBase = price,
    priceLoyal = priceLoyal,
    priceSchool = priceSchool,
    priceStudent = priceStudent,
    quantity = quantity,
    category = category
)
