package com.app2.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pos_transactions")
data class POSTransactionEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "ticket_number") val ticketNumber: String? = null,
    @ColumnInfo(name = "session_id") val sessionId: Int,
    @ColumnInfo(name = "customer_id") val customerId: Int? = null,
    @ColumnInfo(name = "payment_method") val paymentMethod: String,
    val subtotal: Double,
    @ColumnInfo(name = "tax_amount") val taxAmount: Double,
    @ColumnInfo(name = "discount_amount") val discountAmount: Double = 0.0,
    val total: Double,
    @ColumnInfo(name = "tendered_amount") val tenderedAmount: Double = 0.0,
    @ColumnInfo(name = "change_amount") val changeAmount: Double = 0.0,
    val status: String? = "completed",
    @ColumnInfo(name = "invoice_id") val invoiceId: Int? = null,
    @ColumnInfo(name = "created_at") val createdAt: String? = null
)
