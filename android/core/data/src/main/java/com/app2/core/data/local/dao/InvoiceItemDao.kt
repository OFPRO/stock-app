package com.app2.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app2.core.data.local.entity.InvoiceItemEntity

@Dao
interface InvoiceItemDao {
    @Query("SELECT * FROM invoice_items WHERE invoice_id = :invoiceId")
    suspend fun getByInvoiceId(invoiceId: Int): List<InvoiceItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<InvoiceItemEntity>)

    @Query("DELETE FROM invoice_items WHERE invoice_id = :invoiceId")
    suspend fun deleteByInvoiceId(invoiceId: Int)

    @Query("DELETE FROM invoice_items")
    suspend fun deleteAll()
}
