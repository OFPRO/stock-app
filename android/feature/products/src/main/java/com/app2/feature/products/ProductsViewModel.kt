package com.app2.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.repository.ProductRepository
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    private val productRepository: ProductRepository
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
                allProducts = productRepository.getProducts(includeArchived = true).map { dto ->
                    ProductListItem(
                        id = dto.id,
                        name = dto.name,
                        sku = dto.sku,
                        price = dto.price,
                        quantity = dto.quantity,
                        category = dto.category,
                        barcode = dto.barcode,
                        isDeleted = dto.isDeleted == 1
                    )
                }
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
                productRepository.deleteProduct(id)
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
}
