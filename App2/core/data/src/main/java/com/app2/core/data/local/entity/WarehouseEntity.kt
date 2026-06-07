package com.app2.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "warehouses")
data class WarehouseEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val address: String? = null,
    val manager: String? = null,
    val phone: String? = null,
    val ice: String? = null,
    val patente: String? = null,
    val rc: String? = null,
    @ColumnInfo(name = "taxe_number") val taxeNumber: String? = null,
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String? = null
)
