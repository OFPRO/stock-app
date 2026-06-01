package com.app2.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String? = null,
    val sku: String? = null,
    val barcode: String? = null,
    val quantity: Int = 0,
    @ColumnInfo(name = "min_quantity") val minQuantity: Int = 5,
    @ColumnInfo(name = "max_quantity") val maxQuantity: Int = 100,
    val price: Double = 0.0,
    @ColumnInfo(name = "price_base") val priceBase: Double? = null,
    @ColumnInfo(name = "price_loyal") val priceLoyal: Double? = null,
    @ColumnInfo(name = "price_school") val priceSchool: Double? = null,
    @ColumnInfo(name = "price_student") val priceStudent: Double? = null,
    val category: String? = "Général",
    @ColumnInfo(name = "warehouse_id") val warehouseId: Int? = null,
    @ColumnInfo(name = "location_id") val locationId: Int? = null,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "purchase_price_avg") val purchasePriceAvg: Double? = null,
    @ColumnInfo(name = "discount_category") val discountCategory: String? = "aucun",
    @ColumnInfo(name = "margin_percent") val marginPercent: Double? = 15.0,
    @ColumnInfo(name = "is_liquidation") val isLiquidation: Boolean = false,
    @ColumnInfo(name = "extra_prices") val extraPrices: String? = "[]",
    @ColumnInfo(name = "created_at") val createdAt: String? = null
)
