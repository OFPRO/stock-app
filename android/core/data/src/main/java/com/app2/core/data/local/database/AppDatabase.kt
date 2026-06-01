package com.app2.core.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.app2.core.data.local.dao.CustomerDao
import com.app2.core.data.local.dao.InvoiceDao
import com.app2.core.data.local.dao.NotificationDao
import com.app2.core.data.local.dao.POSSessionDao
import com.app2.core.data.local.dao.ProductDao
import com.app2.core.data.local.dao.SupplierDao
import com.app2.core.data.local.dao.WarehouseDao
import com.app2.core.data.local.entity.CustomerEntity
import com.app2.core.data.local.entity.InvoiceEntity
import com.app2.core.data.local.entity.InvoiceItemEntity
import com.app2.core.data.local.entity.LocationEntity
import com.app2.core.data.local.entity.NotificationEntity
import com.app2.core.data.local.entity.POSSessionEntity
import com.app2.core.data.local.entity.POSTransactionEntity
import com.app2.core.data.local.entity.ProductEntity
import com.app2.core.data.local.entity.PurchaseOrderEntity
import com.app2.core.data.local.entity.ReorderRuleEntity
import com.app2.core.data.local.entity.StockMovementEntity
import com.app2.core.data.local.entity.SupplierEntity
import com.app2.core.data.local.entity.WarehouseEntity

@Database(
    entities = [
        ProductEntity::class,
        CustomerEntity::class,
        SupplierEntity::class,
        WarehouseEntity::class,
        LocationEntity::class,
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        StockMovementEntity::class,
        PurchaseOrderEntity::class,
        ReorderRuleEntity::class,
        NotificationEntity::class,
        POSSessionEntity::class,
        POSTransactionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun supplierDao(): SupplierDao
    abstract fun warehouseDao(): WarehouseDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun notificationDao(): NotificationDao
    abstract fun posSessionDao(): POSSessionDao
}
