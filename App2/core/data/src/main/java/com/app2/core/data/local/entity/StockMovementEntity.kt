package com.app2.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_movements")
data class StockMovementEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "product_id") val productId: Int? = null,
    val type: String,
    val quantity: Int,
    @ColumnInfo(name = "source_location_id") val sourceLocationId: Int? = null,
    @ColumnInfo(name = "dest_location_id") val destLocationId: Int? = null,
    val note: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: String? = null
)
