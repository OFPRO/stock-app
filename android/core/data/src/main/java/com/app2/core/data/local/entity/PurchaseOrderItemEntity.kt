package com.app2.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "purchase_order_items",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseOrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["order_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["order_id"])]
)
data class PurchaseOrderItemEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "order_id") val orderId: Int,
    @ColumnInfo(name = "product_id") val productId: Int? = null,
    @ColumnInfo(name = "product_name") val productName: String? = null,
    val quantity: Int = 0,
    @ColumnInfo(name = "unit_price") val unitPrice: Double = 0.0,
    @ColumnInfo(name = "received_qty") val receivedQty: Int? = null
)
