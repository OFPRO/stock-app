package com.app2.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val type: String = "particulier",
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    @ColumnInfo(name = "client_code") val clientCode: String? = null,
    @ColumnInfo(name = "discount_rate") val discountRate: Double = 0.0,
    @ColumnInfo(name = "is_loyal") val isLoyal: Boolean = false,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    val ice: String? = null,
    val notes: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null
)
