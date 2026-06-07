package com.app2.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "purchase_orders")
data class PurchaseOrderEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "order_number") val orderNumber: String? = null,
    @ColumnInfo(name = "supplier_id") val supplierId: Int? = null,
    @ColumnInfo(name = "warehouse_id") val warehouseId: Int? = null,
    val status: String = "brouillon",
    val total: Double = 0.0,
    val notes: String? = null,
    @ColumnInfo(name = "sent_at") val sentAt: String? = null,
    @ColumnInfo(name = "received_at") val receivedAt: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: String? = null
)
