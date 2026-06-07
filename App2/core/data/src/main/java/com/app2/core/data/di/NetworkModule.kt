package com.app2.core.data.di

import com.app2.core.data.network.ApiConstants
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
import com.app2.core.data.remote.ReportApiService
import com.app2.core.data.remote.SupplierApiService
import com.app2.core.data.remote.WarehouseApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }
        return OkHttpClient.Builder()
            .connectTimeout(ApiConstants.TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(ApiConstants.TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(ApiConstants.TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        client: OkHttpClient,
        json: Json
    ): Retrofit = Retrofit.Builder()
        .baseUrl(ApiConstants.BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides @Singleton
    fun provideProductApi(retrofit: Retrofit): ProductApiService = retrofit.create(ProductApiService::class.java)

    @Provides @Singleton
    fun provideCustomerApi(retrofit: Retrofit): CustomerApiService = retrofit.create(CustomerApiService::class.java)

    @Provides @Singleton
    fun provideSupplierApi(retrofit: Retrofit): SupplierApiService = retrofit.create(SupplierApiService::class.java)

    @Provides @Singleton
    fun provideWarehouseApi(retrofit: Retrofit): WarehouseApiService = retrofit.create(WarehouseApiService::class.java)

    @Provides @Singleton
    fun provideLocationApi(retrofit: Retrofit): LocationApiService = retrofit.create(LocationApiService::class.java)

    @Provides @Singleton
    fun provideMovementApi(retrofit: Retrofit): MovementApiService = retrofit.create(MovementApiService::class.java)

    @Provides @Singleton
    fun providePOSApi(retrofit: Retrofit): POSApiService = retrofit.create(POSApiService::class.java)

    @Provides @Singleton
    fun provideOrderApi(retrofit: Retrofit): OrderApiService = retrofit.create(OrderApiService::class.java)

    @Provides @Singleton
    fun provideInvoiceApi(retrofit: Retrofit): InvoiceApiService = retrofit.create(InvoiceApiService::class.java)

    @Provides @Singleton
    fun provideReorderRuleApi(retrofit: Retrofit): ReorderRuleApiService = retrofit.create(ReorderRuleApiService::class.java)

    @Provides @Singleton
    fun provideNotificationApi(retrofit: Retrofit): NotificationApiService = retrofit.create(NotificationApiService::class.java)

    @Provides @Singleton
    fun provideKPIApi(retrofit: Retrofit): KPIApiService = retrofit.create(KPIApiService::class.java)

    @Provides @Singleton
    fun provideMainAccountApi(retrofit: Retrofit): MainAccountApiService = retrofit.create(MainAccountApiService::class.java)

    @Provides @Singleton
    fun provideReportApi(retrofit: Retrofit): ReportApiService = retrofit.create(ReportApiService::class.java)

    @Provides @Singleton
    fun provideAdminApi(retrofit: Retrofit): AdminApiService = retrofit.create(AdminApiService::class.java)
}
