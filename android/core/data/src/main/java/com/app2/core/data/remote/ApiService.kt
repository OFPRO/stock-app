package com.app2.core.data.remote

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ProductApiService {
    @GET("api/products")
    suspend fun getProducts(@Query("include_archived") includeArchived: Boolean = false): JsonElement

    @GET("api/products/{id}")
    suspend fun getProduct(@Path("id") id: Int): JsonElement

    @GET("api/products/for-sale")
    suspend fun getProductsForSale(
        @Query("search") search: String? = null,
        @Query("customer_id") customerId: Int? = null
    ): JsonElement

    @GET("api/categories")
    suspend fun getCategories(): JsonElement

    @POST("api/products")
    suspend fun createProduct(@Body body: JsonObject): JsonElement

    @PUT("api/products/{id}")
    suspend fun updateProduct(@Path("id") id: Int, @Body body: JsonObject): JsonElement

    @DELETE("api/products/{id}")
    suspend fun deleteProduct(@Path("id") id: Int): JsonElement
}

interface CustomerApiService {
    @GET("api/customers")
    suspend fun getCustomers(@Query("search") search: String? = null): JsonElement

    @GET("api/customers/{id}")
    suspend fun getCustomer(@Path("id") id: Int): JsonElement

    @POST("api/customers")
    suspend fun createCustomer(@Body body: JsonObject): JsonElement

    @PUT("api/customers/{id}")
    suspend fun updateCustomer(@Path("id") id: Int, @Body body: JsonObject): JsonElement

    @DELETE("api/customers/{id}")
    suspend fun deleteCustomer(@Path("id") id: Int): JsonElement
}

interface SupplierApiService {
    @GET("api/suppliers")
    suspend fun getSuppliers(): JsonElement

    @POST("api/suppliers")
    suspend fun createSupplier(@Body body: JsonObject): JsonElement

    @PUT("api/suppliers/{id}")
    suspend fun updateSupplier(@Path("id") id: Int, @Body body: JsonObject): JsonElement

    @DELETE("api/suppliers/{id}")
    suspend fun deleteSupplier(@Path("id") id: Int): JsonElement
}

interface WarehouseApiService {
    @GET("api/warehouses")
    suspend fun getWarehouses(): JsonElement

    @GET("api/warehouses/{id}")
    suspend fun getWarehouse(@Path("id") id: Int): JsonElement

    @POST("api/warehouses")
    suspend fun createWarehouse(@Body body: JsonObject): JsonElement

    @DELETE("api/warehouses/{id}")
    suspend fun deleteWarehouse(@Path("id") id: Int): JsonElement
}

interface LocationApiService {
    @GET("api/locations")
    suspend fun getLocations(@Query("warehouse_id") warehouseId: Int? = null): JsonElement

    @POST("api/locations")
    suspend fun createLocation(@Body body: JsonObject): JsonElement

    @PUT("api/locations/{id}")
    suspend fun updateLocation(@Path("id") id: Int, @Body body: JsonObject): JsonElement

    @DELETE("api/locations/{id}")
    suspend fun deleteLocation(@Path("id") id: Int): JsonElement
}

interface MovementApiService {
    @GET("api/movements")
    suspend fun getMovements(
        @Query("product_id") productId: Int? = null,
        @Query("warehouse_id") warehouseId: Int? = null
    ): JsonElement

    @POST("api/stock/{product_id}")
    suspend fun createMovement(
        @Path("product_id") productId: Int,
        @Body body: JsonObject
    ): JsonElement

    @POST("api/stock/transfer")
    suspend fun transferStock(@Body body: JsonObject): JsonElement

    @POST("api/stock/inter-warehouse")
    suspend fun interWarehouseTransfer(@Body body: JsonObject): JsonElement
}

interface POSApiService {
    @GET("api/pos/sessions")
    suspend fun getSessions(): JsonElement

    @POST("api/pos/sessions")
    suspend fun openSession(@Body body: JsonObject): JsonElement

    @POST("api/pos/sessions/{id}/close")
    suspend fun closeSession(@Path("id") id: Int, @Body body: JsonObject): JsonElement

    @POST("api/pos/transactions")
    suspend fun createTransaction(@Body body: JsonObject): JsonElement

    @GET("api/pos/transactions/recent")
    suspend fun getRecentTransactions(
        @Query("session_id") sessionId: Int? = null,
        @Query("limit") limit: Int? = null
    ): JsonElement

    @GET("api/pos/customers")
    suspend fun getPOSCustomers(): JsonElement

    @GET("api/pos/best-sellers")
    suspend fun getBestSellers(): JsonElement

    @GET("api/pos/cash-movements")
    suspend fun getCashMovements(): JsonElement

    @POST("api/pos/cash-movements")
    suspend fun createCashMovement(@Body body: JsonObject): JsonElement
}

interface OrderApiService {
    @GET("api/orders")
    suspend fun getOrders(
        @Query("warehouse_id") warehouseId: Int? = null,
        @Query("status") status: String? = null
    ): JsonElement

    @GET("api/orders/{id}")
    suspend fun getOrder(@Path("id") id: Int): JsonElement

    @POST("api/orders")
    suspend fun createOrder(@Body body: JsonObject): JsonElement

    @PUT("api/orders/{id}")
    suspend fun updateOrder(@Path("id") id: Int, @Body body: JsonObject): JsonElement

    @GET("api/orders/{id}/items")
    suspend fun getOrderItems(@Path("id") id: Int): JsonElement

    @DELETE("api/orders/{id}")
    suspend fun deleteOrder(@Path("id") id: Int): JsonElement
}

interface InvoiceApiService {
    @GET("api/invoices")
    suspend fun getInvoices(
        @Query("status") status: String? = null,
        @Query("date_start") dateStart: String? = null,
        @Query("date_end") dateEnd: String? = null
    ): JsonElement

    @GET("api/invoices/{id}")
    suspend fun getInvoice(@Path("id") id: Int): JsonElement

    @POST("api/invoices")
    suspend fun createInvoice(@Body body: JsonObject): JsonElement

    @PUT("api/invoices/{id}")
    suspend fun updateInvoice(@Path("id") id: Int, @Body body: JsonObject): JsonElement

    @DELETE("api/invoices/{id}")
    suspend fun deleteInvoice(@Path("id") id: Int): JsonElement

    @GET("api/invoices/{id}/items")
    suspend fun getInvoiceItems(@Path("id") id: Int): JsonElement

    @POST("api/invoices/{id}/items")
    suspend fun addInvoiceItem(@Path("id") id: Int, @Body body: JsonObject): JsonElement

    @DELETE("api/invoices/{id}/items/{itemId}")
    suspend fun deleteInvoiceItem(@Path("id") id: Int, @Path("itemId") itemId: Int): JsonElement
}

interface ReorderRuleApiService {
    @GET("api/reorder-rules")
    suspend fun getReorderRules(@Query("warehouse_id") warehouseId: Int? = null): JsonElement

    @POST("api/reorder-rules")
    suspend fun createReorderRule(@Body body: JsonObject): JsonElement

    @PUT("api/reorder-rules/{id}")
    suspend fun updateReorderRule(@Path("id") id: Int, @Body body: JsonObject): JsonElement

    @DELETE("api/reorder-rules/{id}")
    suspend fun deleteReorderRule(@Path("id") id: Int): JsonElement
}

interface NotificationApiService {
    @GET("api/notifications")
    suspend fun getNotifications(@Query("warehouse_id") warehouseId: Int? = null): JsonElement

    @POST("api/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Int): JsonElement

    @POST("api/notifications/mark-all-read")
    suspend fun markAllNotificationsRead(): JsonElement
}

interface KPIApiService {
    @GET("api/kpis/dashboard")
    suspend fun getDashboardKPIs(): JsonElement

    @GET("api/kpis/sales-daily")
    suspend fun getSalesDaily(@Query("period") period: Int = 7): JsonElement

    @GET("api/kpis/categories-distribution")
    suspend fun getCategoriesDistribution(): JsonElement

    @GET("api/kpis/top-selling-products")
    suspend fun getTopSellingProducts(@Query("limit") limit: Int = 10): JsonElement

    @GET("api/kpis/alertes")
    suspend fun getAlertes(): JsonElement

    @GET("api/kpis/sales")
    suspend fun getSalesKPIs(): JsonElement

    @GET("api/kpis/margins")
    suspend fun getMargins(): JsonElement

    @GET("api/kpis/receivables")
    suspend fun getReceivablesKPIs(): JsonElement

    @GET("api/kpis/invoices-status")
    suspend fun getInvoicesStatus(): JsonElement

    @GET("api/kpis/sessions-history")
    suspend fun getSessionsHistory(): JsonElement

    @GET("api/kpis/sessions-summary")
    suspend fun getSessionsSummary(): JsonElement

    @GET("api/kpis/sessions/{id}/details")
    suspend fun getSessionDetails(@Path("id") id: Int): JsonElement

    @GET("api/kpis/warehouse-overview")
    suspend fun getWarehouseOverview(): JsonElement

    @GET("api/kpis/top-products")
    suspend fun getTopProducts(): JsonElement

    @GET("api/kpis/payment-methods")
    suspend fun getPaymentMethods(): JsonElement

    @GET("api/kpis/trends")
    suspend fun getTrends(): JsonElement
}

interface MainAccountApiService {
    @GET("api/main-account")
    suspend fun getMainAccount(): JsonElement

    @POST("api/main-account/deposit")
    suspend fun deposit(@Body body: JsonObject): JsonElement

    @POST("api/main-account/withdraw")
    suspend fun withdraw(@Body body: JsonObject): JsonElement

    @POST("api/main-account/transfer-to-pos")
    suspend fun transferToPOS(@Body body: JsonObject): JsonElement
}

interface ReportApiService {
    @GET("api/reports")
    suspend fun getReport(@Query("type") type: String): JsonElement

    @GET("api/reports/export")
    suspend fun exportReport(@Query("type") type: String): JsonElement
}

interface AdminApiService {
    @POST("api/reset-data")
    suspend fun resetData(): JsonElement

    @POST("api/seed-data")
    suspend fun seedData(): JsonElement
}
