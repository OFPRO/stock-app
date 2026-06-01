package com.app2.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reordering_rules")
data class ReorderRuleEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "product_id") val productId: Int,
    @ColumnInfo(name = "warehouse_id") val warehouseId: Int? = null,
    @ColumnInfo(name = "min_quantity") val minQuantity: Int = 5,
    @ColumnInfo(name = "max_quantity") val maxQuantity: Int = 100,
    @ColumnInfo(name = "trigger_type") val triggerType: String? = "manual",
    @ColumnInfo(name = "supplier_id") val supplierId: Int? = null,
    @ColumnInfo(name = "created_at") val createdAt: String? = null
)
