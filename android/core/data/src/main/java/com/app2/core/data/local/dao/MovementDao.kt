package com.app2.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app2.core.data.local.entity.StockMovementEntity

@Dao
interface MovementDao {
    @Query("SELECT * FROM stock_movements ORDER BY created_at DESC")
    suspend fun getAll(): List<StockMovementEntity>

    @Query("SELECT * FROM stock_movements WHERE product_id = :productId ORDER BY created_at DESC")
    suspend fun getByProductId(productId: Int): List<StockMovementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movements: List<StockMovementEntity>)

    @Query("DELETE FROM stock_movements")
    suspend fun deleteAll()
}
