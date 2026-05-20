import Foundation

protocol ProductServiceProtocol {
    func fetchProducts() async throws -> [ProductListItem]
    func fetchProduct(id: Int) async throws -> ProductDetail
    func createProduct(_ request: ProductCreateRequest) async throws -> ProductDetail
    func updateProduct(id: Int, _ request: ProductUpdateRequest) async throws -> ProductDetail
    func deleteProduct(id: Int) async throws
    func fetchCategories() async throws -> [CategoryDTO]
    func fetchProductByBarcode(_ barcode: String) async throws -> ScannedProduct?
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
protocol StockServiceProtocol {}
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
protocol OrderServiceProtocol {}
protocol InvoiceServiceProtocol {}
protocol NotificationServiceProtocol {}
protocol ReportServiceProtocol {}
protocol SessionServiceProtocol {}
protocol MainAccountServiceProtocol {}

protocol KPIProviderProtocol {
    func fetchHeaderKPIs() async throws -> DashboardHeaderKPIs
    func fetchFinancialKPIs() async throws -> DashboardFinancialKPIs
    func fetchPOSKPIs() async throws -> DashboardPOSKPIs
    func fetchDailySales(days: Int) async throws -> [DailySalesData]
    func fetchCategoryDistribution() async throws -> [CategoryDistribution]
    func fetchTopSellingProducts(limit: Int) async throws -> [TopProductData]
}

protocol AuthServiceProtocol {}
