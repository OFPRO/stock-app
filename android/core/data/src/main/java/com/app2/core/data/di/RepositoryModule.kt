package com.app2.core.data.di

import com.app2.core.data.local.dao.CustomerDao
import com.app2.core.data.local.dao.InvoiceDao
import com.app2.core.data.local.dao.InvoiceItemDao
import com.app2.core.data.local.dao.LocationDao
import com.app2.core.data.local.dao.MovementDao
import com.app2.core.data.local.dao.NotificationDao
import com.app2.core.data.local.dao.OrderDao
import com.app2.core.data.local.dao.OrderItemDao
import com.app2.core.data.local.dao.POSSessionDao
import com.app2.core.data.local.dao.ProductDao
import com.app2.core.data.local.dao.ReorderRuleDao
import com.app2.core.data.local.dao.SupplierDao
import com.app2.core.data.local.dao.WarehouseDao
import com.app2.core.data.remote.AdminApiService
import com.app2.core.data.remote.CustomerApiService
import com.app2.core.data.remote.InvoiceApiService
import com.app2.core.data.remote.KPIApiService
import com.app2.core.data.remote.LocationApiService
import com.app2.core.data.remote.MainAccountApiService
import com.app2.core.data.remote.MovementApiService
import com.app2.core.data.remote.NotificationApiService
import com.app2.core.data.remote.OrderApiService
import com.app2.core.data.remote.POSApiService
import com.app2.core.data.remote.ProductApiService
import com.app2.core.data.remote.ReorderRuleApiService
import com.app2.core.data.remote.SupplierApiService
import com.app2.core.data.remote.WarehouseApiService
import com.app2.core.data.repository.AdminRepository
import com.app2.core.data.repository.CustomerRepository
import com.app2.core.data.repository.InvoiceRepository
import com.app2.core.data.repository.KPIRepository
import com.app2.core.data.repository.LocationRepository
import com.app2.core.data.repository.MainAccountRepository
import com.app2.core.data.repository.MovementRepository
import com.app2.core.data.repository.NotificationRepository
import com.app2.core.data.repository.OrderRepository
import com.app2.core.data.repository.POSRepository
import com.app2.core.data.repository.ProductRepository
import com.app2.core.data.repository.ReorderRuleRepository
import com.app2.core.data.repository.SupplierRepository
import com.app2.core.data.repository.WarehouseRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideOrderRepository(
        api: OrderApiService,
        orderDao: OrderDao,
        orderItemDao: OrderItemDao
    ): OrderRepository = OrderRepository(api, orderDao, orderItemDao)

    @Provides
    @Singleton
    fun provideInvoiceRepository(
        api: InvoiceApiService,
        invoiceDao: InvoiceDao,
        invoiceItemDao: InvoiceItemDao
    ): InvoiceRepository = InvoiceRepository(api, invoiceDao, invoiceItemDao)

    @Provides
    @Singleton
    fun provideProductRepository(
        api: ProductApiService,
        productDao: ProductDao
    ): ProductRepository = ProductRepository(api, productDao)

    @Provides
    @Singleton
    fun provideCustomerRepository(
        api: CustomerApiService,
        customerDao: CustomerDao
    ): CustomerRepository = CustomerRepository(api, customerDao)

    @Provides
    @Singleton
    fun provideSupplierRepository(
        api: SupplierApiService,
        supplierDao: SupplierDao
    ): SupplierRepository = SupplierRepository(api, supplierDao)

    @Provides
    @Singleton
    fun provideWarehouseRepository(
        api: WarehouseApiService,
        warehouseDao: WarehouseDao
    ): WarehouseRepository = WarehouseRepository(api, warehouseDao)

    @Provides
    @Singleton
    fun provideNotificationRepository(
        api: NotificationApiService,
        notificationDao: NotificationDao
    ): NotificationRepository = NotificationRepository(api, notificationDao)

    @Provides
    @Singleton
    fun provideLocationRepository(
        api: LocationApiService,
        locationDao: LocationDao
    ): LocationRepository = LocationRepository(api, locationDao)

    @Provides
    @Singleton
    fun provideMovementRepository(
        api: MovementApiService,
        movementDao: MovementDao
    ): MovementRepository = MovementRepository(api, movementDao)

    @Provides
    @Singleton
    fun provideReorderRuleRepository(
        api: ReorderRuleApiService,
        reorderRuleDao: ReorderRuleDao
    ): ReorderRuleRepository = ReorderRuleRepository(api, reorderRuleDao)

    @Provides
    @Singleton
    fun providePOSRepository(
        api: POSApiService,
        sessionDao: POSSessionDao
    ): POSRepository = POSRepository(api, sessionDao)

    @Provides
    @Singleton
    fun provideKPIRepository(
        api: KPIApiService
    ): KPIRepository = KPIRepository(api)

    @Provides
    @Singleton
    fun provideMainAccountRepository(
        api: MainAccountApiService
    ): MainAccountRepository = MainAccountRepository(api)

    @Provides
    @Singleton
    fun provideAdminRepository(
        api: AdminApiService
    ): AdminRepository = AdminRepository(api)
}
