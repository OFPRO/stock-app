package com.app2.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.remote.ProductApiService
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

private fun JsonElement?.optString(): String? {
    val prim = this?.jsonPrimitive
    return if (prim != null && prim !is JsonNull) prim.content else null
}

private fun JsonElement?.optDouble(): Double? =
    this?.jsonPrimitive?.doubleOrNull

private fun JsonElement?.optInt(): Int? =
    this?.jsonPrimitive?.intOrNull

data class ProductListItem(
    val id: Int,
    val name: String,
    val sku: String,
    val price: Double,
    val quantity: Int,
    val category: String?,
    val barcode: String?,
    val isDeleted: Boolean
) {
    val stockStatus: StockStatus
        get() = when {
            quantity <= 0 -> StockStatus.OutOfStock
            quantity <= 5 -> StockStatus.Low
            else -> StockStatus.InStock
        }
}

enum class StockStatus(val label: String) {
    InStock("En stock"),
    Low("Stock faible"),
    OutOfStock("Rupture")
}

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val productApi: ProductApiService
) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<List<ProductListItem>>>(ViewState.Loading)
    val state = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private var allProducts: List<ProductListItem> = emptyList()

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _state.value = ViewState.Loading
            try {
                val response = productApi.getProducts(includeArchived = true)
                allProducts = parseProductList(response)
                applyFilter()
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de chargement des produits")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    fun deleteProduct(id: Int) {
        viewModelScope.launch {
            try {
                productApi.deleteProduct(id)
                loadProducts()
            } catch (e: Exception) {
                _state.value = ViewState.Error("Erreur de suppression: ${e.message}")
            }
        }
    }

    private fun applyFilter() {
        val query = _searchQuery.value.trim().lowercase()
        val filtered = if (query.isEmpty()) {
            allProducts
        } else {
            allProducts.filter { product ->
                product.name.lowercase().contains(query) ||
                    product.sku.lowercase().contains(query) ||
                    product.barcode?.lowercase()?.contains(query) == true ||
                    product.category?.lowercase()?.contains(query) == true
            }
        }
        _state.value = if (filtered.isEmpty()) {
            ViewState.Empty
        } else {
            ViewState.Loaded(filtered)
        }
    }

    private fun parseProductList(json: JsonElement): List<ProductListItem> {
        return json.jsonArray.mapNotNull { item ->
            val obj = item.jsonObject
            val id = obj["id"].optInt() ?: return@mapNotNull null
            ProductListItem(
                id = id,
                name = obj["name"].optString() ?: "",
                sku = obj["sku"].optString() ?: "",
                price = obj["price"].optDouble() ?: 0.0,
                quantity = obj["quantity"].optInt() ?: 0,
                category = obj["category"].optString(),
                barcode = obj["barcode"].optString(),
                isDeleted = obj["is_deleted"].optInt() == 1
            )
        }
    }
}
