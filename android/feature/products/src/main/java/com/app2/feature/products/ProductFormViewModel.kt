package com.app2.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

@HiltViewModel
class ProductFormViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

    private val _sku = MutableStateFlow("")
    val sku = _sku.asStateFlow()

    private val _barcode = MutableStateFlow("")
    val barcode = _barcode.asStateFlow()

    private val _price = MutableStateFlow("")
    val price = _price.asStateFlow()

    private val _purchasePrice = MutableStateFlow("")
    val purchasePrice = _purchasePrice.asStateFlow()

    private val _category = MutableStateFlow("")
    val category = _category.asStateFlow()

    private val _description = MutableStateFlow("")
    val description = _description.asStateFlow()

    private val _minQuantity = MutableStateFlow("")
    val minQuantity = _minQuantity.asStateFlow()

    private val _maxQuantity = MutableStateFlow("")
    val maxQuantity = _maxQuantity.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    var editId: Int? = null
    private var initialized = false

    fun initForm(editData: ProductDetailDisplayData?) {
        if (initialized) return
        initialized = true
        if (editData != null) {
            _name.value = editData.name
            _sku.value = editData.sku
            _barcode.value = editData.barcode ?: ""
            _price.value = if (editData.price > 0) editData.price.toString() else ""
            _purchasePrice.value = editData.purchasePrice?.toString() ?: ""
            _category.value = editData.category ?: ""
            _description.value = editData.description ?: ""
            _minQuantity.value = editData.minStock?.toString() ?: ""
            _maxQuantity.value = editData.maxStock?.toString() ?: ""
            editId = editData.id
        }
    }

    fun onNameChanged(v: String) { _name.value = v }
    fun onSkuChanged(v: String) { _sku.value = v }
    fun onBarcodeChanged(v: String) { _barcode.value = v }
    fun onPriceChanged(v: String) { _price.value = v }
    fun onPurchasePriceChanged(v: String) { _purchasePrice.value = v }
    fun onCategoryChanged(v: String) { _category.value = v }
    fun onDescriptionChanged(v: String) { _description.value = v }
    fun onMinQuantityChanged(v: String) { _minQuantity.value = v }
    fun onMaxQuantityChanged(v: String) { _maxQuantity.value = v }

    fun save(isEdit: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val priceVal = _price.value.toDoubleOrNull()
                    ?: throw IllegalArgumentException("Prix invalide")
                val body = buildJsonObject {
                    put("name", _name.value)
                    put("sku", _sku.value)
                    put("price", priceVal)
                    put("barcode", _barcode.value.ifBlank { "" })
                    put("category", _category.value.ifBlank { "" })
                    put("description", _description.value.ifBlank { "" })
                    _purchasePrice.value.toDoubleOrNull()?.let { put("purchase_price_avg", it) }
                    _minQuantity.value.toIntOrNull()?.let { put("min_quantity", it) }
                    _maxQuantity.value.toIntOrNull()?.let { put("max_quantity", it) }
                    put("price_base", priceVal)
                    put("quantity", 0)
                }
                if (isEdit) {
                    val id = editId ?: return@launch
                    productRepository.updateProduct(id, body)
                } else {
                    productRepository.createProduct(body)
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
