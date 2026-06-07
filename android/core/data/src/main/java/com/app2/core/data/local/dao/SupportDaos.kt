package com.app2.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app2.core.data.local.entity.ReorderRuleEntity
import com.app2.core.data.local.entity.SupplierEntity
import com.app2.core.data.local.entity.WarehouseEntity
import com.app2.core.data.local.entity.NotificationEntity
import com.app2.core.data.local.entity.POSSessionEntity

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    suspend fun getAll(): List<SupplierEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(suppliers: List<SupplierEntity>)

    @Query("DELETE FROM suppliers")
    suspend fun deleteAll()
}

@Dao
interface WarehouseDao {
    @Query("SELECT * FROM warehouses ORDER BY name ASC")
    suspend fun getAll(): List<WarehouseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(warehouses: List<WarehouseEntity>)

    @Query("DELETE FROM warehouses")
    suspend fun deleteAll()
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY created_at DESC")
    suspend fun getAll(): List<NotificationEntity>

    @Query("SELECT * FROM notifications WHERE is_read = 0")
    suspend fun getUnread(): List<NotificationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET is_read = 1 WHERE id = :id")
    suspend fun markRead(id: Int)

    @Query("UPDATE notifications SET is_read = 1 WHERE is_read = 0")
    suspend fun markAllRead()

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()
}

@Dao
interface POSSessionDao {
    @Query("SELECT * FROM pos_sessions ORDER BY opened_at DESC")
    suspend fun getAll(): List<POSSessionEntity>

    @Query("SELECT * FROM pos_sessions WHERE status = 'open' LIMIT 1")
    suspend fun getOpenSession(): POSSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: POSSessionEntity)

    @Query("DELETE FROM pos_sessions")
    suspend fun deleteAll()
}

@Dao
interface ReorderRuleDao {
    @Query("SELECT * FROM reordering_rules ORDER BY product_id ASC")
    suspend fun getAll(): List<ReorderRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<ReorderRuleEntity>)

    @Query("DELETE FROM reordering_rules")
    suspend fun deleteAll()
}
