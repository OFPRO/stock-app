import Foundation

enum HTTPMethod: String {
    case get = "GET", post = "POST", put = "PUT", delete = "DELETE"
}

enum Endpoint {
    case products(includeArchived: Bool)
    case product(Int)
    case createProduct
    case updateProduct(Int)
    case deleteProduct(Int)
    case productsForSale
    case productByBarcode(String)
    case categories

    case customers(search: String?)
    case createCustomer
    case customer(Int)
    case updateCustomer(Int)
    case deleteCustomer(Int)

    case suppliers
    case createSupplier
    case updateSupplier(Int)
    case deleteSupplier(Int)

    case warehouses
    case warehouse(Int)
    case createWarehouse
    case deleteWarehouse(Int)

    case locations(warehouseId: Int?)
    case createLocation
    case updateLocation(Int)
    case deleteLocation(Int)

    case movements(productId: Int?, warehouseId: Int?)
    case productMovements(Int)
    case createMovement
    case transferStock
    case interWarehouseTransfer
    case stockForProduct(Int)

    case posSessions
    case openSession
    case closeSession(Int)
    case createTransaction
    case recentTransactions(sessionId: Int?, limit: Int?)
    case transactionByInvoice(String)
    case cashMovements
    case createCashMovement
    case posCustomers
    case bestSellers
    case ticketPDF(String)

    case orders(warehouseId: Int?, status: String?)
    case createOrder
    case updateOrder(Int)
    case orderItems(Int)
    case deleteOrder(Int)

    case reorderRules(warehouseId: Int?)
    case createReorderRule
    case updateReorderRule(Int)
    case deleteReorderRule(Int)

    case replenishment

    case invoices(status: String?, dateStart: String?, dateEnd: String?)
    case createInvoice
    case invoice(Int)
    case updateInvoice(Int)
    case deleteInvoice(Int)
    case invoiceItems(Int)
    case addInvoiceItem(Int)
    case deleteInvoiceItem(Int, itemId: Int)
    case invoicePDF(Int)

    case invoiceStats
    case receivables

    case notifications(warehouseId: Int?)
    case markNotificationRead(Int)
    case markAllNotificationsRead

    case mainKPIs
    case dashboardKPIs
    case alertes
    case stats
    case salesKPIs
    case margins
    case receivablesKPIs
    case invoicesStatus
    case salesDaily(days: Int)
    case categoriesDistribution
    case topSellingProducts(limit: Int)
    case sessionsHistory(limit: Int, status: String)
    case sessionsSummary(period: Int)
    case sessionDetails(Int)
    case trends
    case topProducts
    case byLocation
    case warehouseOverview
    case ordersSummary
    case invoicesSummary
    case customersSummary
    case evolution
    case paymentMethods

    case report(type: String, warehouseId: Int?)
    case reportExport(type: String, warehouseId: Int?)

    case mainAccount
    case mainAccountDeposit
    case mainAccountWithdraw
    case transferToPOS

    case resetData
    case seedData

    var path: String {
        switch self {
        case .products(let archived): return "/api/products\(archived ? "?include_archived=true" : "")"
        case .product(let id): return "/api/products/\(id)"
        case .createProduct: return "/api/products"
        case .updateProduct(let id): return "/api/products/\(id)"
        case .deleteProduct(let id): return "/api/products/\(id)"
        case .productsForSale: return "/api/products/for-sale"
        case .productByBarcode(let code): return "/api/products/for-sale?search=\(code.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? code)"
        case .categories: return "/api/categories"

        case .customers(let search):
            if let s = search, !s.isEmpty { return "/api/customers?search=\(s.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? s)" }
            return "/api/customers"
        case .createCustomer: return "/api/customers"
        case .customer(let id): return "/api/customers/\(id)"
        case .updateCustomer(let id): return "/api/customers/\(id)"
        case .deleteCustomer(let id): return "/api/customers/\(id)"

        case .suppliers: return "/api/suppliers"
        case .createSupplier: return "/api/suppliers"
        case .updateSupplier(let id): return "/api/suppliers/\(id)"
        case .deleteSupplier(let id): return "/api/suppliers/\(id)"

        case .warehouses: return "/api/warehouses"
        case .warehouse(let id): return "/api/warehouses/\(id)"
        case .createWarehouse: return "/api/warehouses"
        case .deleteWarehouse(let id): return "/api/warehouses/\(id)"

        case .locations(let wid):
            if let id = wid { return "/api/locations?warehouse_id=\(id)" }
            return "/api/locations"
        case .createLocation: return "/api/locations"
        case .updateLocation(let id): return "/api/locations/\(id)"
        case .deleteLocation(let id): return "/api/locations/\(id)"

        case .movements(let pid, let wid):
            var params: [String] = []
            if let p = pid { params.append("product_id=\(p)") }
            if let w = wid { params.append("warehouse_id=\(w)") }
            let q = params.isEmpty ? "" : "?" + params.joined(separator: "&")
            return "/api/movements\(q)"
        case .productMovements(let id): return "/api/movements/\(id)"
        case .createMovement: return "/api/movements"
        case .transferStock: return "/api/stock/transfer"
        case .interWarehouseTransfer: return "/api/stock/inter-warehouse"
        case .stockForProduct(let id): return "/api/stock/\(id)"

        case .posSessions: return "/api/pos/sessions"
        case .openSession: return "/api/pos/sessions"
        case .closeSession(let id): return "/api/pos/sessions/\(id)/close"
        case .createTransaction: return "/api/pos/transactions"
        case .recentTransactions(let sid, let limit):
            var params: [String] = []
            if let s = sid { params.append("session_id=\(s)") }
            if let l = limit { params.append("limit=\(l)") }
            let q = params.isEmpty ? "" : "?" + params.joined(separator: "&")
            return "/api/pos/transactions/recent\(q)"
        case .transactionByInvoice(let num): return "/api/pos/transaction-by-invoice/\(num)"
        case .cashMovements: return "/api/pos/cash-movements"
        case .createCashMovement: return "/api/pos/cash-movements"
        case .posCustomers: return "/api/pos/customers"
        case .bestSellers: return "/api/pos/best-sellers"
        case .ticketPDF(let num): return "/api/pos/tickets/\(num)"

        case .orders(let wid, let status):
            var params: [String] = []
            if let w = wid { params.append("warehouse_id=\(w)") }
            if let s = status { params.append("status=\(s)") }
            let q = params.isEmpty ? "" : "?" + params.joined(separator: "&")
            return "/api/orders\(q)"
        case .createOrder: return "/api/orders"
        case .updateOrder(let id): return "/api/orders/\(id)"
        case .orderItems(let id): return "/api/orders/\(id)/items"
        case .deleteOrder(let id): return "/api/orders/\(id)"

        case .reorderRules(let wid):
            if let id = wid { return "/api/reorder-rules?warehouse_id=\(id)" }
            return "/api/reorder-rules"
        case .createReorderRule: return "/api/reorder-rules"
        case .updateReorderRule(let id): return "/api/reorder-rules/\(id)"
        case .deleteReorderRule(let id): return "/api/reorder-rules/\(id)"

        case .replenishment: return "/api/replenishment"

        case .invoices(let status, let ds, let de):
            var params: [String] = []
            if let s = status { params.append("status=\(s)") }
            if let d = ds { params.append("date_start=\(d)") }
            if let d = de { params.append("date_end=\(d)") }
            let q = params.isEmpty ? "" : "?" + params.joined(separator: "&")
            return "/api/invoices\(q)"
        case .createInvoice: return "/api/invoices"
        case .invoice(let id): return "/api/invoices/\(id)"
        case .updateInvoice(let id): return "/api/invoices/\(id)"
        case .deleteInvoice(let id): return "/api/invoices/\(id)"
        case .invoiceItems(let id): return "/api/invoices/\(id)/items"
        case .addInvoiceItem(let id): return "/api/invoices/\(id)/items"
        case .deleteInvoiceItem(let id, let itemId): return "/api/invoices/\(id)/items/\(itemId)"
        case .invoicePDF(let id): return "/api/invoices/\(id)/pdf"

        case .invoiceStats: return "/api/invoice-stats"
        case .receivables: return "/api/receivables"

        case .notifications(let wid):
            if let id = wid { return "/api/notifications?warehouse_id=\(id)" }
            return "/api/notifications"
        case .markNotificationRead(let id): return "/api/notifications/\(id)/read"
        case .markAllNotificationsRead: return "/api/notifications/mark-all-read"

        case .mainKPIs: return "/api/kpis"
        case .dashboardKPIs: return "/api/kpis/dashboard"
        case .alertes: return "/api/kpis/alertes"
        case .stats: return "/api/stats"
        case .salesKPIs: return "/api/kpis/sales"
        case .margins: return "/api/kpis/margins"
        case .receivablesKPIs: return "/api/kpis/receivables"
        case .invoicesStatus: return "/api/kpis/invoices-status"
        case .salesDaily(let days): return "/api/kpis/sales-daily?days=\(days)"
        case .categoriesDistribution: return "/api/kpis/categories-distribution"
        case .topSellingProducts(let limit): return "/api/kpis/top-selling-products?limit=\(limit)"
        case .sessionsHistory(let limit, let status): return "/api/kpis/sessions-history?limit=\(limit)&status=\(status)"
        case .sessionsSummary(let period): return "/api/kpis/sessions-summary?period=\(period)"
        case .sessionDetails(let id): return "/api/kpis/sessions/\(id)/details"
        case .trends: return "/api/kpis/trends"
        case .topProducts: return "/api/kpis/top-products"
        case .byLocation: return "/api/kpis/by-location"
        case .warehouseOverview: return "/api/kpis/warehouse-overview"
        case .ordersSummary: return "/api/kpis/orders-summary"
        case .invoicesSummary: return "/api/kpis/invoices-summary"
        case .customersSummary: return "/api/kpis/customers-summary"
        case .evolution: return "/api/kpis/evolution"
        case .paymentMethods: return "/api/kpis/payment-methods"

        case .report(let type, let wid):
            var params = "type=\(type)"
            if let w = wid { params += "&warehouse_id=\(w)" }
            return "/api/reports?\(params)"
        case .reportExport(let type, let wid):
            var params = "type=\(type)&format=csv"
            if let w = wid { params += "&warehouse_id=\(w)" }
            return "/api/reports/export?\(params)"

        case .mainAccount: return "/api/main-account"
        case .mainAccountDeposit: return "/api/main-account/deposit"
        case .mainAccountWithdraw: return "/api/main-account/withdraw"
        case .transferToPOS: return "/api/main-account/transfer-to-pos"

        case .resetData: return "/api/reset-data"
        case .seedData: return "/api/seed-data"
        }
    }

    var method: HTTPMethod {
        switch self {
        case .products, .product, .productsForSale, .categories,
                .productByBarcode,
                .customers, .customer,
                .suppliers,
                .warehouses, .warehouse,
                .locations, .movements, .productMovements, .stockForProduct,
                .posSessions, .recentTransactions, .transactionByInvoice, .cashMovements, .posCustomers, .bestSellers, .ticketPDF,
                .orders, .orderItems,
                .reorderRules,
                .replenishment,
                .invoices, .invoice, .invoiceItems, .invoicePDF,
                .invoiceStats, .receivables,
                .notifications,
                .mainKPIs, .dashboardKPIs, .alertes, .stats, .salesKPIs, .margins,
                .receivablesKPIs, .invoicesStatus, .salesDaily, .categoriesDistribution,
                .topSellingProducts, .sessionsHistory, .sessionsSummary, .sessionDetails,
                .trends, .topProducts, .byLocation, .warehouseOverview, .ordersSummary,
                .invoicesSummary, .customersSummary, .evolution, .paymentMethods,
                .report, .reportExport,
                .mainAccount:
            return .get
        case .createProduct, .createCustomer, .createSupplier, .createWarehouse, .createLocation,
                .createMovement, .transferStock, .interWarehouseTransfer,
                .openSession, .createTransaction, .createCashMovement,
                .createOrder, .createReorderRule,
                .createInvoice, .addInvoiceItem,
                .markNotificationRead, .markAllNotificationsRead,
                .mainAccountDeposit, .mainAccountWithdraw, .transferToPOS,
                .resetData, .seedData:
            return .post
        case .updateProduct, .updateCustomer, .updateSupplier,
                .updateLocation, .updateOrder, .updateReorderRule, .updateInvoice:
            return .put
        case .deleteProduct, .deleteCustomer, .deleteSupplier, .deleteWarehouse, .deleteLocation,
                .deleteOrder, .deleteReorderRule, .deleteInvoice, .deleteInvoiceItem,
                .closeSession:
            return .delete
        }
    }
}
