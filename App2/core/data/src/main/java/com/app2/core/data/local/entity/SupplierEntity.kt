package com.app2.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    @ColumnInfo(name = "contact_person") val contactPerson: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: String? = null
)
