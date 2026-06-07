package com.app2.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app2.core.data.local.entity.PurchaseOrderItemEntity

@Dao
interface OrderItemDao {
    @Query("SELECT * FROM purchase_order_items WHERE order_id = :orderId")
    suspend fun getByOrderId(orderId: Int): List<PurchaseOrderItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PurchaseOrderItemEntity>)

    @Query("DELETE FROM purchase_order_items WHERE order_id = :orderId")
    suspend fun deleteByOrderId(orderId: Int)

    @Query("DELETE FROM purchase_order_items")
    suspend fun deleteAll()
}
