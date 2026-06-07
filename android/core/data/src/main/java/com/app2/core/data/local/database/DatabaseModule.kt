package com.app2.core.data.local.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "stockpro.db"
    ).fallbackToDestructiveMigration().build()

    @Provides
    fun provideProductDao(db: AppDatabase) = db.productDao()

    @Provides
    fun provideOrderDao(db: AppDatabase) = db.orderDao()

    @Provides
    fun provideOrderItemDao(db: AppDatabase) = db.orderItemDao()

    @Provides
    fun provideCustomerDao(db: AppDatabase) = db.customerDao()

    @Provides
    fun provideSupplierDao(db: AppDatabase) = db.supplierDao()

    @Provides
    fun provideWarehouseDao(db: AppDatabase) = db.warehouseDao()

    @Provides
    fun provideInvoiceDao(db: AppDatabase) = db.invoiceDao()

    @Provides
    fun provideInvoiceItemDao(db: AppDatabase) = db.invoiceItemDao()

    @Provides
    fun provideNotificationDao(db: AppDatabase) = db.notificationDao()

    @Provides
    fun providePOSSessionDao(db: AppDatabase) = db.posSessionDao()

    @Provides
    fun provideLocationDao(db: AppDatabase) = db.locationDao()

    @Provides
    fun provideMovementDao(db: AppDatabase) = db.movementDao()

    @Provides
    fun provideReorderRuleDao(db: AppDatabase) = db.reorderRuleDao()
}
