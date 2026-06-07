import Foundation

protocol ProductServiceProtocol {
    func fetchProducts() async throws -> [ProductListItem]
    func fetchProduct(id: Int) async throws -> ProductDetail
    func createProduct(_ request: ProductCreateRequest) async throws -> ProductDetail
    func updateProduct(id: Int, _ request: ProductUpdateRequest) async throws -> ProductDetail
    func deleteProduct(id: Int) async throws
    func fetchCategories() async throws -> [CategoryDTO]
    func fetchProductByBarcode(_ barcode: String) async throws -> ScannedProduct?
    func fetchProductsForSale() async throws -> [ForSaleProductDTO]
}

protocol CustomerServiceProtocol {
    func fetchCustomers(search: String?) async throws -> [CustomerListItem]
    func fetchCustomer(id: Int) async throws -> CustomerDTO
    func createCustomer(_ request: CustomerCreateRequest) async throws
    func updateCustomer(id: Int, _ request: CustomerUpdateRequest) async throws
    func deleteCustomer(id: Int) async throws
}

protocol SupplierServiceProtocol {
    func fetchSuppliers() async throws -> [SupplierListItem]
    func fetchSupplier(id: Int) async throws -> SupplierListItem?
    func createSupplier(_ request: SupplierCreateRequest) async throws
    func updateSupplier(id: Int, _ request: SupplierUpdateRequest) async throws
    func deleteSupplier(id: Int) async throws
}

protocol WarehouseServiceProtocol {
    func fetchWarehouses() async throws -> [WarehouseListItem]
    func fetchWarehouse(id: Int) async throws -> WarehouseListItem?
    func createWarehouse(_ request: WarehouseCreateRequest) async throws
}

protocol LocationServiceProtocol {
    func fetchLocations(warehouseId: Int?) async throws -> [LocationListItem]
    func fetchLocation(id: Int) async throws -> LocationListItem?
    func createLocation(_ request: LocationCreateRequest) async throws
    func updateLocation(id: Int, _ request: LocationUpdateRequest) async throws
    func deleteLocation(id: Int) async throws
}
protocol StockServiceProtocol {
    func fetchMovements(productId: Int?, warehouseId: Int?) async throws -> [StockMovementItem]
    func createMovement(productId: Int, type: String, quantity: Int, locationId: Int?, note: String?) async throws
    func transferStock(productId: Int, quantity: Int, fromLocationId: Int?, toLocationId: Int?, note: String?) async throws
    func interWarehouseTransfer(productId: Int, quantity: Int, fromWarehouseId: Int, toWarehouseId: Int, note: String?) async throws
}
protocol POSServiceProtocol {
    func fetchSession() async throws -> POSSessionDTO?
    func openSession(openingCash: Double) async throws -> POSSessionDTO
    func closeSession(id: Int, closingCash: Double) async throws -> CloseSessionResponse
    func createTransaction(_ request: POSTransactionRequest) async throws -> POSTransactionResponse
    func fetchBestSellers() async throws -> [BestSellerDTO]
    func fetchPOSCustomers() async throws -> [POSCustomerDTO]
    func fetchCashMovements() async throws -> [CashMovementDTO]
    func createCashMovement(type: String, amount: Double, reason: String, note: String?) async throws
    func fetchRecentTransactions(sessionId: Int?, limit: Int?) async throws -> [POSTransactionDTO]
}
protocol OrderServiceProtocol {
    func fetchOrders(warehouseId: Int?, status: String?) async throws -> [OrderDTO]
    func createOrder(_ request: OrderCreateRequest) async throws
    func updateOrder(id: Int, _ request: OrderUpdateRequest) async throws
    func fetchOrderItems(id: Int) async throws -> [OrderItemDTO]
    func deleteOrder(id: Int) async throws
}
protocol InvoiceServiceProtocol {
    func fetchInvoices(status: String?, dateStart: String?, dateEnd: String?) async throws -> [InvoiceDTO]
    func createInvoice(_ request: InvoiceCreateRequest) async throws
    func fetchInvoice(id: Int) async throws -> InvoiceDTO
    func updateInvoice(id: Int, _ request: InvoiceUpdateRequest) async throws
    func deleteInvoice(id: Int) async throws
    func fetchInvoiceItems(id: Int) async throws -> [InvoiceItemDTO]
    func addInvoiceItem(id: Int, _ request: InvoiceItemRequest) async throws
    func deleteInvoiceItem(id: Int, itemId: Int) async throws
    func invoicePDF(id: Int) async throws -> Data
}
protocol NotificationServiceProtocol {
    func fetchNotifications(warehouseId: Int?) async throws -> [NotificationDTO]
    func markRead(id: Int) async throws
    func markAllRead() async throws
}
protocol ReorderRuleServiceProtocol {
    func fetchRules(warehouseId: Int?) async throws -> [ReorderRuleDTO]
    func createRule(_ request: ReorderRuleCreateRequest) async throws
    func updateRule(id: Int, _ request: ReorderRuleUpdateRequest) async throws
    func deleteRule(id: Int) async throws
    func fetchReplenishment() async throws -> [ReplenishmentSuggestion]
}
protocol ReportServiceProtocol {
    func fetchOverview(warehouseId: Int?) async throws -> ReportOverviewDTO
    func fetchRotation(warehouseId: Int?) async throws -> [RotationReportItem]
    func fetchExpiry(warehouseId: Int?) async throws -> [ExpiryReportItem]
    func fetchCategories(warehouseId: Int?) async throws -> [CategoryReportItem]
    func fetchLowStock(warehouseId: Int?) async throws -> [LowStockReportItem]
    func fetchWarehouseReport() async throws -> [WarehouseReportItem]
    func exportCSV(type: String, warehouseId: Int?) async throws -> Data
}

protocol SessionServiceProtocol {
    func fetchHistory(limit: Int, status: String) async throws -> [SessionHistoryItem]
    func fetchSummary(period: Int) async throws -> SessionsSummaryDTO
    func fetchDetails(sessionId: Int) async throws -> SessionDetailResponse
}

protocol MainAccountServiceProtocol {
    func fetchMainAccount() async throws -> MainAccountResponse
    func deposit(amount: Double, reason: String, note: String?) async throws -> MainAccountActionResponse
    func withdraw(amount: Double, reason: String, note: String?) async throws -> MainAccountActionResponse
    func transferToPOS(amount: Double, note: String?) async throws -> MainAccountActionResponse
}

protocol KPIProviderProtocol {
    func fetchHeaderKPIs() async throws -> DashboardHeaderKPIs
    func fetchFinancialKPIs() async throws -> DashboardFinancialKPIs
    func fetchPOSKPIs() async throws -> DashboardPOSKPIs
    func fetchDailySales(days: Int) async throws -> [DailySalesData]
    func fetchCategoryDistribution() async throws -> [CategoryDistribution]
    func fetchTopSellingProducts(limit: Int) async throws -> [TopProductData]
}

protocol AuthServiceProtocol {}
