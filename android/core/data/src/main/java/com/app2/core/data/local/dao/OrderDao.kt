package com.app2.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app2.core.data.local.entity.PurchaseOrderEntity

@Dao
interface OrderDao {
    @Query("SELECT * FROM purchase_orders ORDER BY created_at DESC")
    suspend fun getAll(): List<PurchaseOrderEntity>

    @Query("SELECT * FROM purchase_orders WHERE id = :id")
    suspend fun getById(id: Int): PurchaseOrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(orders: List<PurchaseOrderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: PurchaseOrderEntity)

    @Query("DELETE FROM purchase_orders")
    suspend fun deleteAll()
}
