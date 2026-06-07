package com.app2.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "locations",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = WarehouseEntity::class,
            parentColumns = ["id"],
            childColumns = ["warehouse_id"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["warehouse_id"])
    ]
)
data class LocationEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "warehouse_id") val warehouseId: Int,
    val name: String,
    val type: String = "rack",
    val capacity: Int? = null,
    @ColumnInfo(name = "created_at") val createdAt: String? = null
)
