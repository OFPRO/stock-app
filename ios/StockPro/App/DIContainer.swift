import SwiftUI

struct DIContainer {
    let productService: ProductServiceProtocol
    let customerService: CustomerServiceProtocol
    let supplierService: SupplierServiceProtocol
    let warehouseService: WarehouseServiceProtocol
    let locationService: LocationServiceProtocol
    let stockService: StockServiceProtocol
    let posService: POSServiceProtocol
    let orderService: OrderServiceProtocol
    let invoiceService: InvoiceServiceProtocol
    let notificationService: NotificationServiceProtocol
    let reportService: ReportServiceProtocol
    let sessionService: SessionServiceProtocol
    let mainAccountService: MainAccountServiceProtocol
    let kpiService: KPIProviderProtocol
    let authService: AuthServiceProtocol

    static let live = DIContainer(
        productService: ProductService(),
        customerService: CustomerService(),
        supplierService: SupplierService(),
        warehouseService: WarehouseService(),
        locationService: LocationService(),
        stockService: StockService(),
        posService: POSService(),
        orderService: OrderService(),
        invoiceService: InvoiceService(),
        notificationService: NotificationService(),
        reportService: ReportService(),
        sessionService: SessionService(),
        mainAccountService: MainAccountService(),
        kpiService: KPIProvider(),
        authService: AuthService()
    )

    static let preview = DIContainer(
        productService: MockProductService(),
        customerService: MockCustomerService(),
        supplierService: MockSupplierService(),
        warehouseService: MockWarehouseService(),
        locationService: MockLocationService(),
        stockService: MockStockService(),
        posService: MockPOSService(),
        orderService: MockOrderService(),
        invoiceService: MockInvoiceService(),
        notificationService: MockNotificationService(),
        reportService: MockReportService(),
        sessionService: MockSessionService(),
        mainAccountService: MockMainAccountService(),
        kpiService: MockKPIProvider(),
        authService: MockAuthService()
    )
}

struct DIContainerKey: EnvironmentKey {
    static let defaultValue: DIContainer = .preview
}

extension EnvironmentValues {
    var diContainer: DIContainer {
        get { self[DIContainerKey.self] }
        set { self[DIContainerKey.self] = newValue }
    }
}
