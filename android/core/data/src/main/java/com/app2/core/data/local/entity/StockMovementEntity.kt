package com.app2.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_movements")
data class StockMovementEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "product_id") val productId: Int? = null,
    @ColumnInfo(name = "product_name") val productName: String = "",
    val type: String,
    val quantity: Int,
    @ColumnInfo(name = "source_location_id") val sourceLocationId: Int? = null,
    @ColumnInfo(name = "dest_location_id") val destLocationId: Int? = null,
    @ColumnInfo(name = "source_location") val sourceLocation: String? = null,
    @ColumnInfo(name = "dest_location") val destLocation: String? = null,
    val note: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: String? = null
)
