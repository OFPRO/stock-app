package com.app2.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app2.core.data.remote.ProductApiService
import com.app2.core.data.remote.MovementApiService
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

data class ProductDetailDisplayData(
    val id: Int,
    val name: String,
    val sku: String,
    val description: String?,
    val price: Double,
    val category: String?,
    val barcode: String?,
    val quantity: Int,
    val minStock: Int?,
    val maxStock: Int?,
    val priceLoyal: Double?,
    val priceSchool: Double?,
    val priceStudent: Double?,
    val purchasePrice: Double?,
    val wholesalePrice: Double?,
    val marginPercent: Double?,
    val warehouseName: String?,
    val locationName: String?,
    val createdAt: String?,
    val purchaseStats: Pair<Int, Double>,
    val salesStats: Pair<Int, Double>
)

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val productApi: ProductApiService,
    private val movementApi: MovementApiService
) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<ProductDetailDisplayData>>(ViewState.Loading)
    val state = _state.asStateFlow()

    private var lastLoadedId: Int? = null

    fun loadProduct(id: Int) {
        if (lastLoadedId == id) return
        lastLoadedId = id
        viewModelScope.launch {
            _state.value = ViewState.Loading
            try {
                val response = productApi.getProduct(id)
                _state.value = ViewState.Loaded(parseProductDetail(response))
            } catch (e: Exception) {
                _state.value = ViewState.Error(e.message ?: "Erreur de chargement du produit")
            }
        }
    }

    private fun parseProductDetail(json: JsonElement): ProductDetailDisplayData {
        val obj = json.jsonObject
        val product = obj["product"]?.jsonObject ?: obj

        val purchaseStatsObj = obj["purchase_stats"]?.jsonObject
        val salesStatsObj = obj["sales_stats"]?.jsonObject

        return ProductDetailDisplayData(
            id = product["id"].optInt() ?: 0,
            name = product["name"].optString() ?: "",
            sku = product["sku"].optString() ?: "",
            description = product["description"].optString(),
            price = product["price"].optDouble() ?: 0.0,
            category = product["category"].optString(),
            barcode = product["barcode"].optString(),
            quantity = product["quantity"].optInt() ?: 0,
            minStock = product["min_quantity"].optInt(),
            maxStock = product["max_quantity"].optInt(),
            priceLoyal = product["price_loyal"].optDouble(),
            priceSchool = product["price_school"].optDouble(),
            priceStudent = product["price_student"].optDouble(),
            purchasePrice = product["purchase_price_avg"].optDouble(),
            wholesalePrice = product["wholesale_price"].optDouble(),
            marginPercent = product["margin_percent"].optDouble(),
            warehouseName = product["warehouse_name"].optString(),
            locationName = product["location_name"].optString(),
            createdAt = product["created_at"].optString(),
            purchaseStats = Pair(
                purchaseStatsObj?.get("total_qty")?.optInt() ?: 0,
                purchaseStatsObj?.get("total_purchases")?.optDouble() ?: 0.0
            ),
            salesStats = Pair(
                salesStatsObj?.get("total_qty")?.optInt() ?: 0,
                salesStatsObj?.get("total_sales")?.optDouble() ?: 0.0
            )
        )
    }
}
