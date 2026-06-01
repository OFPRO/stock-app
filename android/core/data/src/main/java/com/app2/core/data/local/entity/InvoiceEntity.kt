package com.app2.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "invoice_number") val invoiceNumber: String,
    @ColumnInfo(name = "customer_id") val customerId: Int? = null,
    @ColumnInfo(name = "warehouse_id") val warehouseId: Int? = null,
    val status: String = "brouillon",
    val subtotal: Double = 0.0,
    @ColumnInfo(name = "discount_total") val discountTotal: Double = 0.0,
    @ColumnInfo(name = "tax_amount") val taxAmount: Double = 0.0,
    val total: Double = 0.0,
    val notes: String? = null,
    @ColumnInfo(name = "due_date") val dueDate: String? = null,
    @ColumnInfo(name = "paid_at") val paidAt: String? = null,
    val type: String? = "facture",
    @ColumnInfo(name = "payment_method") val paymentMethod: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: String? = null
)
