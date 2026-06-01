package com.app2.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "invoice_items",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoice_id"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["invoice_id"])
    ]
)
data class InvoiceItemEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "invoice_id") val invoiceId: Int,
    @ColumnInfo(name = "product_id") val productId: Int? = null,
    @ColumnInfo(name = "product_name") val productName: String? = null,
    val quantity: Int,
    @ColumnInfo(name = "unit_price") val unitPrice: Double,
    @ColumnInfo(name = "discount_percent") val discountPercent: Double = 0.0,
    @ColumnInfo(name = "tax_rate") val taxRate: Double = 20.0,
    @ColumnInfo(name = "line_total") val lineTotal: Double,
    @ColumnInfo(name = "created_at") val createdAt: String? = null
)
