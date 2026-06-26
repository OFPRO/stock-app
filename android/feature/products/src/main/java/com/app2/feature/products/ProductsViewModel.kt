package com.app2.feature.products

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.network.PdfExporter
import com.app2.core.data.remote.ProductApiService
import com.app2.core.data.repository.ProductRepository
import com.app2.core.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductListItem(
    val id: Int,
    val name: String,
    val price: Double,
    val quantity: Int,
    val purchasePrice: Double?,
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
    private val productRepository: ProductRepository,
    private val productApiService: ProductApiService,
    private val pdfExporter: PdfExporter
) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<List<ProductListItem>>>(ViewState.Loading)
    val state = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories = _categories.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _sortBy = MutableStateFlow("name")
    val sortBy = _sortBy.asStateFlow()

    private val _sortOrder = MutableStateFlow("asc")
    val sortOrder = _sortOrder.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting = _isExporting.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore = _isLoadingMore.asStateFlow()

    private val _currentPage = MutableStateFlow(1)
    val currentPage = _currentPage.asStateFlow()

    private val _totalPages = MutableStateFlow(1)
    val totalPages = _totalPages.asStateFlow()

    private val _pdfEvent = MutableSharedFlow<Uri>()
    val pdfEvent = _pdfEvent.asSharedFlow()

    private var allProducts: List<ProductListItem> = emptyList()

    init {
        loadProducts()
        loadCategories()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _state.value = ViewState.Loading
            _currentPage.value = 1
            try {
                val paged = productRepository.getProductsPaged(
                    page = 1, perPage = 50, includeArchived = true,
                    sortBy = _sortBy.value, sortOrder = _sortOrder.value
                )
                _totalPages.value = paged.totalPages
                allProducts = paged.data.map { it.toListItem() }
                applyFilter()
            } catch (e: Exception) {
                try {
                    allProducts = productRepository.getProducts(includeArchived = true).map { it.toListItem() }
                    _totalPages.value = 1
                    applyFilter()
                } catch (e2: Exception) {
                    _state.value = ViewState.Error(e.message ?: "Erreur de chargement des produits")
                }
            }
        }
    }

    fun loadMore() {
        viewModelScope.launch {
            val nextPage = _currentPage.value + 1
            if (nextPage > _totalPages.value || _isLoadingMore.value) return@launch
            _isLoadingMore.value = true
            try {
                val paged = productRepository.getProductsPaged(
                    page = nextPage, perPage = 50, includeArchived = true,
                    sortBy = _sortBy.value, sortOrder = _sortOrder.value
                )
                _currentPage.value = nextPage
                allProducts = allProducts + paged.data.map { it.toListItem() }
                applyFilter()
            } catch (_: Exception) { }
            finally { _isLoadingMore.value = false }
        }
    }

    fun onSortChanged(sortBy: String, sortOrder: String) {
        _sortBy.value = sortBy
        _sortOrder.value = sortOrder
        loadProducts()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    fun onCategorySelected(category: String?) {
        _selectedCategory.value = category
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

    fun exportPdf() {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val response = productApiService.exportProductsPdf(
                    category = _selectedCategory.value,
                    search = _searchQuery.value.takeIf { it.isNotBlank() }
                )
                val uri = pdfExporter.saveResponseAndGetUri(response, "produits.pdf")
                _pdfEvent.emit(uri)
            } catch (e: Exception) {
                _state.value = ViewState.Error("Erreur d'export PDF: ${e.message}")
            } finally {
                _isExporting.value = false
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val cats = productRepository.getCategories()
                _categories.value = cats.map { it.name }.sorted()
            } catch (_: Exception) { }
        }
    }

    private fun applyFilter() {
        val query = _searchQuery.value.trim().lowercase()
        val categoryFilter = _selectedCategory.value

        val filtered = allProducts.filter { product ->
            val matchesSearch = query.isEmpty() ||
                product.name.lowercase().contains(query) ||
                product.barcode?.lowercase()?.contains(query) == true ||
                product.category?.lowercase()?.contains(query) == true
            val matchesCategory = categoryFilter == null || product.category == categoryFilter
            matchesSearch && matchesCategory
        }
        _state.value = if (filtered.isEmpty()) {
            ViewState.Empty
        } else {
            ViewState.Loaded(filtered)
        }
    }
}

private fun com.app2.core.data.remote.dto.ProductDetailDTO.toListItem() = ProductListItem(
    id = id,
    name = name,
    price = price,
    quantity = quantity,
    purchasePrice = purchasePrice,
    category = category,
    barcode = barcode,
    isDeleted = isDeleted == 1
)
