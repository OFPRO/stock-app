package com.app2.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app2.core.data.local.entity.LocationEntity

@Dao
interface LocationDao {
    @Query("SELECT * FROM locations WHERE warehouse_id = :warehouseId ORDER BY name ASC")
    suspend fun getByWarehouseId(warehouseId: Int): List<LocationEntity>

    @Query("SELECT * FROM locations ORDER BY name ASC")
    suspend fun getAll(): List<LocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(locations: List<LocationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: LocationEntity)

    @Query("DELETE FROM locations WHERE warehouse_id = :warehouseId")
    suspend fun deleteByWarehouseId(warehouseId: Int)

    @Query("DELETE FROM locations")
    suspend fun deleteAll()
}
