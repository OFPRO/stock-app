package com.app2.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pos_sessions")
data class POSSessionEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "session_number") val sessionNumber: String? = null,
    @ColumnInfo(name = "warehouse_id") val warehouseId: Int? = null,
    @ColumnInfo(name = "user_name") val userName: String? = "Caissier",
    @ColumnInfo(name = "opening_cash") val openingCash: Double = 0.0,
    @ColumnInfo(name = "closing_cash") val closingCash: Double? = null,
    @ColumnInfo(name = "expected_cash") val expectedCash: Double? = null,
    val status: String = "open",
    @ColumnInfo(name = "opened_at") val openedAt: String? = null,
    @ColumnInfo(name = "closed_at") val closedAt: String? = null
)
