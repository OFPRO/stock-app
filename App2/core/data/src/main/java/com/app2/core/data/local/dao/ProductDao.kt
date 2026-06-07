package com.app2.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app2.core.data.local.entity.ProductEntity

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE is_deleted = 0 ORDER BY name ASC")
    suspend fun getAllActive(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Int): ProductEntity?

    @Query("SELECT * FROM products WHERE is_deleted = 0 AND (name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%')")
    suspend fun search(query: String): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity)

    @Query("DELETE FROM products")
    suspend fun deleteAll()
}
