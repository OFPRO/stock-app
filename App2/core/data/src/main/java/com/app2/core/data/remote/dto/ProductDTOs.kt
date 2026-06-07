package com.app2.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductDetailDTO(
    val id: Int,
    val name: String,
    val sku: String,
    val description: String? = null,
    val price: Double = 0.0,
    val quantity: Int = 0,
    val category: String? = null,
    val barcode: String? = null,
    @SerialName("min_quantity") val minStock: Int? = null,
    @SerialName("max_quantity") val maxStock: Int? = null,
    @SerialName("is_deleted") val isDeleted: Int? = null,
    val weight: Double? = null,
    val unit: String? = null,
    @SerialName("purchase_price_avg") val purchasePrice: Double? = null,
    @SerialName("wholesale_price") val wholesalePrice: Double? = null,
    @SerialName("price_base") val priceBase: Double? = null,
    @SerialName("price_loyal") val priceLoyal: Double? = null,
    @SerialName("price_school") val priceSchool: Double? = null,
    @SerialName("price_student") val priceStudent: Double? = null,
    @SerialName("tax_category") val taxCategory: String? = "20",
    @SerialName("discount_category") val discountCategory: String? = "aucun",
    @SerialName("margin_percent") val marginPercent: Double? = 15.0,
    @SerialName("is_liquidation") val isLiquidation: Int? = null,
    @SerialName("extra_prices") val extraPrices: String? = "[]",
    @SerialName("warehouse_id") val warehouseId: Int? = null,
    @SerialName("location_id") val locationId: Int? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ProductCreateRequest(
    val name: String,
    val sku: String,
    val description: String? = null,
    val price: Double,
    val category: String? = null,
    val barcode: String? = null,
    @SerialName("min_quantity") val minStock: Int? = null,
    @SerialName("max_quantity") val maxStock: Int? = null,
    @SerialName("purchase_price_avg") val purchasePrice: Double? = null
)

@Serializable
data class ProductUpdateRequest(
    val name: String? = null,
    val sku: String? = null,
    val description: String? = null,
    val price: Double? = null,
    val category: String? = null,
    val barcode: String? = null,
    @SerialName("min_quantity") val minStock: Int? = null,
    @SerialName("max_quantity") val maxStock: Int? = null
)

enum class StockStatus {
    InStock, Low, OutOfStock;

    val label: String
        get() = when (this) {
            InStock -> "En stock"
            Low -> "Stock faible"
            OutOfStock -> "Rupture"
        }
}

@Serializable
data class ForSaleProductDTO(
    val id: Int,
    val name: String,
    val sku: String,
    val barcode: String? = null,
    val price: Double = 0.0,
    @SerialName("sale_price") val salePrice: Double? = null,
    @SerialName("price_loyal") val priceLoyal: Double? = null,
    @SerialName("price_school") val priceSchool: Double? = null,
    @SerialName("price_student") val priceStudent: Double? = null,
    val quantity: Int = 0,
    @SerialName("min_quantity") val minQuantity: Int? = null,
    val category: String? = null,
    @SerialName("warehouse_id") val warehouseId: Int? = null
)

data class ScannedProduct(
    val id: Int,
    val name: String,
    val sku: String,
    val price: String,
    val stock: Int,
    val barcode: String
)

@Serializable
data class CategoryDTO(
    val name: String,
    val count: Int? = null
)

@Serializable
data class PriceTierDTO(
    val type: String,
    val label: String,
    val price: Double,
    @SerialName("discount_percent") val discountPercent: Int = 0
)

@Serializable
data class StockMovementDTO(
    val id: Int,
    @SerialName("product_id") val productId: Int,
    val type: String,
    val quantity: Int,
    @SerialName("source_location_id") val sourceLocationId: Int? = null,
    @SerialName("dest_location_id") val destLocationId: Int? = null,
    @SerialName("lot_number") val lotNumber: String? = null,
    @SerialName("serial_number") val serialNumber: String? = null,
    val note: String? = null,
    @SerialName("product_name") val productName: String = "",
    @SerialName("source_location") val sourceLocation: String? = null,
    @SerialName("dest_location") val destLocation: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class StockMovementCreateRequest(
    @SerialName("product_id") val productId: Int,
    val type: String,
    val quantity: Int,
    @SerialName("location_id") val locationId: Int? = null,
    val note: String? = null
)

@Serializable
data class StockTransferRequest(
    @SerialName("product_id") val productId: Int,
    val quantity: Int,
    @SerialName("from_location_id") val fromLocationId: Int? = null,
    @SerialName("to_location_id") val toLocationId: Int? = null,
    val note: String? = null
)

@Serializable
data class InterWarehouseTransferRequest(
    @SerialName("product_id") val productId: Int,
    val quantity: Int,
    @SerialName("from_warehouse_id") val fromWarehouseId: Int,
    @SerialName("to_warehouse_id") val toWarehouseId: Int,
    val note: String? = null
)
