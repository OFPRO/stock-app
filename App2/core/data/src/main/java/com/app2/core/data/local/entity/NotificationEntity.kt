package com.app2.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: Int,
    val type: String,
    val title: String,
    val message: String? = null,
    @ColumnInfo(name = "product_id") val productId: Int? = null,
    @ColumnInfo(name = "warehouse_id") val warehouseId: Int? = null,
    @ColumnInfo(name = "is_read") val isRead: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String? = null
)
