import Foundation

final class StockService: StockServiceProtocol {
    private let api = APIClient.shared

    func fetchMovements(productId: Int?, warehouseId: Int?) async throws -> [StockMovementItem] {
        let dtos: [StockMovementDTO] = try await api.request(.movements(productId: productId, warehouseId: warehouseId))
        return dtos.map { dto in
            StockMovementItem(
                id: dto.id, productName: dto.product_name,
                type: dto.type, quantity: dto.quantity,
                sourceLocation: dto.source_location, destLocation: dto.dest_location,
                note: dto.note, createdAt: dto.created_at
            )
        }
    }

    func createMovement(productId: Int, type: String, quantity: Int, locationId: Int?, note: String?) async throws {
        let request = StockMovementCreateRequest(product_id: productId, type: type, quantity: quantity, location_id: locationId, note: note)
        let _: EmptyResponse = try await api.request(.createMovement, body: request)
    }

    func transferStock(productId: Int, quantity: Int, fromLocationId: Int?, toLocationId: Int?, note: String?) async throws {
        let request = StockTransferRequest(product_id: productId, quantity: quantity, from_location_id: fromLocationId, to_location_id: toLocationId, note: note)
        let _: EmptyResponse = try await api.request(.transferStock, body: request)
    }

    func interWarehouseTransfer(productId: Int, quantity: Int, fromWarehouseId: Int, toWarehouseId: Int, note: String?) async throws {
        let request = InterWarehouseTransferRequest(product_id: productId, quantity: quantity, from_warehouse_id: fromWarehouseId, to_warehouse_id: toWarehouseId, note: note)
        let _: EmptyResponse = try await api.request(.interWarehouseTransfer, body: request)
    }
}

final class MockStockService: StockServiceProtocol {
    func fetchMovements(productId: Int?, warehouseId: Int?) async throws -> [StockMovementItem] {
        let all = [
            StockMovementItem(id: 1, productName: "Ramette A4", type: "in", quantity: 50, sourceLocation: nil, destLocation: "Zone A - Rayons", note: "Réapprovisionnement", createdAt: "2026-05-20 10:30:00"),
            StockMovementItem(id: 2, productName: "Stylo Bleu", type: "out", quantity: 100, sourceLocation: "Zone B - Tiroirs", destLocation: nil, note: "Vente", createdAt: "2026-05-20 09:15:00"),
            StockMovementItem(id: 3, productName: "Clavier USB", type: "transfer", quantity: 5, sourceLocation: "Zone A - Rayons", destLocation: "Zone C - Réserve", note: "Réorganisation", createdAt: "2026-05-19 14:00:00"),
            StockMovementItem(id: 4, productName: "Souris Optique", type: "out", quantity: 3, sourceLocation: "Zone A - Rayons", destLocation: nil, note: nil, createdAt: "2026-05-18 16:45:00"),
            StockMovementItem(id: 5, productName: "Écran 24\"", type: "in", quantity: 2, sourceLocation: nil, destLocation: "Zone C - Réserve", note: "Nouveau stock", createdAt: "2026-05-18 08:20:00"),
        ]
        if let pid = productId {
            return all.filter { $0.id == pid }
        }
        return all
    }

    func createMovement(productId: Int, type: String, quantity: Int, locationId: Int?, note: String?) async throws {}
    func transferStock(productId: Int, quantity: Int, fromLocationId: Int?, toLocationId: Int?, note: String?) async throws {}
    func interWarehouseTransfer(productId: Int, quantity: Int, fromWarehouseId: Int, toWarehouseId: Int, note: String?) async throws {}
}

final class ReorderRuleService: ReorderRuleServiceProtocol {
    private let api = APIClient.shared

    func fetchRules(warehouseId: Int?) async throws -> [ReorderRuleDTO] {
        try await api.request(.reorderRules(warehouseId: warehouseId))
    }

    func createRule(_ request: ReorderRuleCreateRequest) async throws {
        let _: EmptyResponse = try await api.request(.createReorderRule, body: request)
    }

    func updateRule(id: Int, _ request: ReorderRuleUpdateRequest) async throws {
        let _: EmptyResponse = try await api.request(.updateReorderRule(id), body: request)
    }

    func deleteRule(id: Int) async throws {
        try await api.requestVoid(.deleteReorderRule(id))
    }

    func fetchReplenishment() async throws -> [ReplenishmentSuggestion] {
        try await api.request(.replenishment)
    }
}

final class MockReorderRuleService: ReorderRuleServiceProtocol {
    func fetchRules(warehouseId: Int?) async throws -> [ReorderRuleDTO] {
        [
            ReorderRuleDTO(id: 1, product_id: 1, product_name: "Ramette A4", supplier_id: 1, supplier_name: "Fournisseur A", warehouse_id: 1, warehouse_name: "Principal", min_quantity: 10, max_quantity: 100, trigger_type: "auto", is_active: true, created_at: "2026-05-01"),
            ReorderRuleDTO(id: 2, product_id: 5, product_name: "Souris Optique", supplier_id: 2, supplier_name: "Fournisseur B", warehouse_id: 1, warehouse_name: "Principal", min_quantity: 5, max_quantity: 30, trigger_type: "manual", is_active: true, created_at: "2026-05-01"),
        ]
    }

    func createRule(_ request: ReorderRuleCreateRequest) async throws {}
    func updateRule(id: Int, _ request: ReorderRuleUpdateRequest) async throws {}
    func deleteRule(id: Int) async throws {}

    func fetchReplenishment() async throws -> [ReplenishmentSuggestion] {
        [
            ReplenishmentSuggestion(id: 1, product_id: 1, product_name: "Ramette A4", current_stock: 3, suggested_quantity: 50, warehouse_id: 1, warehouse_name: "Principal", supplier_name: "Fournisseur A"),
            ReplenishmentSuggestion(id: 2, product_id: 5, product_name: "Souris Optique", current_stock: 2, suggested_quantity: 20, warehouse_id: 1, warehouse_name: "Principal", supplier_name: "Fournisseur B"),
        ]
    }
}

final class OrderService: OrderServiceProtocol {
    private let api = APIClient.shared

    func fetchOrders(warehouseId: Int?, status: String?) async throws -> [OrderDTO] {
        try await api.request(.orders(warehouseId: warehouseId, status: status))
    }

    func createOrder(_ request: OrderCreateRequest) async throws {
        let _: EmptyResponse = try await api.request(.createOrder, body: request)
    }

    func updateOrder(id: Int, _ request: OrderUpdateRequest) async throws {
        let _: EmptyResponse = try await api.request(.updateOrder(id), body: request)
    }

    func fetchOrderItems(id: Int) async throws -> [OrderItemDTO] {
        try await api.request(.orderItems(id))
    }

    func deleteOrder(id: Int) async throws {
        try await api.requestVoid(.deleteOrder(id))
    }
}

final class MockOrderService: OrderServiceProtocol {
    func fetchOrders(warehouseId: Int?, status: String?) async throws -> [OrderDTO] {
        [
            OrderDTO(id: 1, order_number: "CMD-001", supplier_id: 1, supplier_name: "Fournisseur A", warehouse_id: 1, warehouse_name: "Principal", status: "brouillon", total: 1250.00, notes: nil, created_at: "2026-05-20 10:00:00", received_at: nil),
            OrderDTO(id: 2, order_number: "CMD-002", supplier_id: 2, supplier_name: "Fournisseur B", warehouse_id: 1, warehouse_name: "Principal", status: "recue", total: 3400.00, notes: nil, created_at: "2026-05-19 14:30:00", received_at: "2026-05-20 09:00:00"),
        ]
    }

    func createOrder(_ request: OrderCreateRequest) async throws {}
    func updateOrder(id: Int, _ request: OrderUpdateRequest) async throws {}
    func fetchOrderItems(id: Int) async throws -> [OrderItemDTO] { [] }
    func deleteOrder(id: Int) async throws {}
}

final class InvoiceService: InvoiceServiceProtocol {
    private let api = APIClient.shared

    func fetchInvoices(status: String?, dateStart: String?, dateEnd: String?) async throws -> [InvoiceDTO] {
        try await api.request(.invoices(status: status, dateStart: dateStart, dateEnd: dateEnd))
    }

    func createInvoice(_ request: InvoiceCreateRequest) async throws {
        let _: EmptyResponse = try await api.request(.createInvoice, body: request)
    }

    func fetchInvoice(id: Int) async throws -> InvoiceDTO {
        try await api.request(.invoice(id))
    }

    func updateInvoice(id: Int, _ request: InvoiceUpdateRequest) async throws {
        let _: EmptyResponse = try await api.request(.updateInvoice(id), body: request)
    }

    func deleteInvoice(id: Int) async throws {
        try await api.requestVoid(.deleteInvoice(id))
    }

    func fetchInvoiceItems(id: Int) async throws -> [InvoiceItemDTO] {
        try await api.request(.invoiceItems(id))
    }

    func addInvoiceItem(id: Int, _ request: InvoiceItemRequest) async throws {
        let _: EmptyResponse = try await api.request(.addInvoiceItem(id), body: request)
    }

    func deleteInvoiceItem(id: Int, itemId: Int) async throws {
        try await api.requestVoid(.deleteInvoiceItem(id, itemId: itemId))
    }

    func invoicePDF(id: Int) async throws -> Data {
        try await api.requestData(.invoicePDF(id))
    }
}

final class MockInvoiceService: InvoiceServiceProtocol {
    func fetchInvoices(status: String?, dateStart: String?, dateEnd: String?) async throws -> [InvoiceDTO] {
        [
            InvoiceDTO(id: 1, invoice_number: "FACT-001", customer_id: 1, customer_name: "Client A", status: "payee", total: 450.00, payment_method: "especes", notes: nil, due_date: nil, created_at: "2026-05-20", paid_at: "2026-05-20"),
            InvoiceDTO(id: 2, invoice_number: "FACT-002", customer_id: 2, customer_name: "Client B", status: "envoyee", total: 1200.00, payment_method: "carte", notes: nil, due_date: "2026-06-01", created_at: "2026-05-19", paid_at: nil),
        ]
    }

    func createInvoice(_ request: InvoiceCreateRequest) async throws {}
    func fetchInvoice(id: Int) async throws -> InvoiceDTO {
        InvoiceDTO(id: id, invoice_number: "FACT-00\(id)", customer_id: 1, customer_name: "Client A", status: "payee", total: 450.00, payment_method: "especes", notes: nil, due_date: nil, created_at: "2026-05-20", paid_at: "2026-05-20")
    }

    func updateInvoice(id: Int, _ request: InvoiceUpdateRequest) async throws {}
    func deleteInvoice(id: Int) async throws {}
    func fetchInvoiceItems(id: Int) async throws -> [InvoiceItemDTO] { [] }
    func addInvoiceItem(id: Int, _ request: InvoiceItemRequest) async throws {}
    func deleteInvoiceItem(id: Int, itemId: Int) async throws {}
    func invoicePDF(id: Int) async throws -> Data { Data() }
}

final class NotificationService: NotificationServiceProtocol {
    private let api = APIClient.shared

    func fetchNotifications(warehouseId: Int?) async throws -> [NotificationDTO] {
        try await api.request(.notifications(warehouseId: warehouseId))
    }

    func markRead(id: Int) async throws {
        let _: EmptyResponse = try await api.request(.markNotificationRead(id))
    }

    func markAllRead() async throws {
        let _: EmptyResponse = try await api.request(.markAllNotificationsRead)
    }
}

final class MockNotificationService: NotificationServiceProtocol {
    func fetchNotifications(warehouseId: Int?) async throws -> [NotificationDTO] {
        [
            NotificationDTO(id: 1, message: "Stock faible: Ramette A4 (3 restants)", type: "warning", is_read: false, warehouse_id: 1, warehouse_name: "Principal", link_type: "product", link_id: 1, created_at: "2026-05-22 08:00:00"),
            NotificationDTO(id: 2, message: "Commande CMD-002 reçue", type: "info", is_read: true, warehouse_id: 1, warehouse_name: "Principal", link_type: "order", link_id: 2, created_at: "2026-05-20 09:00:00"),
        ]
    }

    func markRead(id: Int) async throws {}
    func markAllRead() async throws {}
}

final class ReportService: ReportServiceProtocol {
    private let api = APIClient.shared

    func fetchOverview(warehouseId: Int?) async throws -> ReportOverviewDTO {
        try await api.request(.report(type: "overview", warehouseId: warehouseId))
    }

    func fetchRotation(warehouseId: Int?) async throws -> [RotationReportItem] {
        try await api.request(.report(type: "rotation", warehouseId: warehouseId))
    }

    func fetchExpiry(warehouseId: Int?) async throws -> [ExpiryReportItem] {
        try await api.request(.report(type: "expiry", warehouseId: warehouseId))
    }

    func fetchCategories(warehouseId: Int?) async throws -> [CategoryReportItem] {
        try await api.request(.report(type: "categories", warehouseId: warehouseId))
    }

    func fetchLowStock(warehouseId: Int?) async throws -> [LowStockReportItem] {
        try await api.request(.report(type: "low_stock", warehouseId: warehouseId))
    }

    func fetchWarehouseReport() async throws -> [WarehouseReportItem] {
        try await api.request(.report(type: "warehouses", warehouseId: nil))
    }

    func exportCSV(type: String, warehouseId: Int?) async throws -> Data {
        try await api.requestData(.reportExport(type: type, warehouseId: warehouseId))
    }
}

final class MockReportService: ReportServiceProtocol {
    func fetchOverview(warehouseId: Int?) async throws -> ReportOverviewDTO {
        ReportOverviewDTO(total_products: 450, total_value: 245_800, low_stock: 12, out_of_stock: 3, expiring_soon: 5)
    }

    func fetchRotation(warehouseId: Int?) async throws -> [RotationReportItem] {
        [
            RotationReportItem(name: "Ramette A4", quantity: 230, min_quantity: 20, movements: 45),
            RotationReportItem(name: "Stylo Bleu", quantity: 180, min_quantity: 50, movements: 38),
            RotationReportItem(name: "Clavier USB", quantity: 15, min_quantity: 10, movements: 22),
        ]
    }

    func fetchExpiry(warehouseId: Int?) async throws -> [ExpiryReportItem] { [] }
    func fetchCategories(warehouseId: Int?) async throws -> [CategoryReportItem] { [] }
    func fetchLowStock(warehouseId: Int?) async throws -> [LowStockReportItem] {
        [
            LowStockReportItem(name: "Souris Optique", quantity: 3, min_quantity: 10, max_quantity: 50, price: 120.0, needed: 7),
            LowStockReportItem(name: "Écran 24\"", quantity: 1, min_quantity: 5, max_quantity: 20, price: 2500.0, needed: 4),
        ]
    }
    func fetchWarehouseReport() async throws -> [WarehouseReportItem] { [] }
    func exportCSV(type: String, warehouseId: Int?) async throws -> Data { Data() }
}

final class SessionService: SessionServiceProtocol {
    private let api = APIClient.shared

    func fetchHistory(limit: Int, status: String) async throws -> [SessionHistoryItem] {
        try await api.request(.sessionsHistory(limit: limit, status: status))
    }

    func fetchSummary(period: Int) async throws -> SessionsSummaryDTO {
        try await api.request(.sessionsSummary(period: period))
    }

    func fetchDetails(sessionId: Int) async throws -> SessionDetailResponse {
        try await api.request(.sessionDetails(sessionId))
    }
}

final class MockSessionService: SessionServiceProtocol {
    func fetchHistory(limit: Int, status: String) async throws -> [SessionHistoryItem] {
        [
            SessionHistoryItem(id: 1, session_number: "SES-20260603-0001", warehouse_id: 1, opening_cash: 1000, closing_cash: 4500, expected_cash: 4520, status: "closed", opened_at: "2026-06-03 08:00:00", closed_at: "2026-06-03 18:30:00", total_sales: 3520, nb_transactions: 28, total_cash_in: 200, total_cash_out: 100, warehouse_name: "Principal"),
            SessionHistoryItem(id: 2, session_number: "SES-20260602-0001", warehouse_id: 1, opening_cash: 1000, closing_cash: 3900, expected_cash: 3880, status: "closed", opened_at: "2026-06-02 08:00:00", closed_at: "2026-06-02 18:00:00", total_sales: 2880, nb_transactions: 22, total_cash_in: 100, total_cash_out: 50, warehouse_name: "Principal"),
        ]
    }

    func fetchSummary(period: Int) async throws -> SessionsSummaryDTO {
        SessionsSummaryDTO(total_sessions: 22, closed_sessions: 20, open_sessions: 2, total_closing_cash: 84000, total_expected_cash: 83800, total_sales_period: 77500, nb_transactions_period: 480)
    }

    func fetchDetails(sessionId: Int) async throws -> SessionDetailResponse {
        SessionDetailResponse(
            session: SessionDetailInfo(id: sessionId, session_number: "SES-20260603-0001", warehouse_id: 1, warehouse_name: "Principal", opening_cash: 1000, closing_cash: 4500, expected_cash: 4520, status: "closed", opened_at: "2026-06-03 08:00:00", closed_at: "2026-06-03 18:30:00", total_sales: 3520),
            transactions: [
                SessionTransactionDTO(id: 1, ticket_number: "TKT-001", customer_name: "Client A", payment_method: "especes", total: 450.0, items_count: 3, created_at: "2026-06-03 09:15:00"),
                SessionTransactionDTO(id: 2, ticket_number: "TKT-002", customer_name: nil, payment_method: "carte", total: 1200.0, items_count: 5, created_at: "2026-06-03 10:30:00"),
            ],
            cash_movements: [
                SessionCashMovementDTO(id: 1, type: "in", amount: 200, reason: "Dépôt", note: nil, created_at: "2026-06-03 12:00:00"),
            ]
        )
    }
}

final class MainAccountService: MainAccountServiceProtocol {
    private let api = APIClient.shared

    func fetchMainAccount() async throws -> MainAccountResponse {
        try await api.request(.mainAccount)
    }

    func deposit(amount: Double, reason: String, note: String?) async throws -> MainAccountActionResponse {
        let request = MainAccountDepositRequest(amount: amount, reason: reason, note: note)
        return try await api.request(.mainAccountDeposit, body: request)
    }

    func withdraw(amount: Double, reason: String, note: String?) async throws -> MainAccountActionResponse {
        let request = MainAccountWithdrawRequest(amount: amount, reason: reason, note: note)
        return try await api.request(.mainAccountWithdraw, body: request)
    }

    func transferToPOS(amount: Double, note: String?) async throws -> MainAccountActionResponse {
        let request = MainAccountTransferRequest(amount: amount, note: note)
        return try await api.request(.transferToPOS, body: request)
    }
}

final class MockMainAccountService: MainAccountServiceProtocol {
    func fetchMainAccount() async throws -> MainAccountResponse {
        MainAccountResponse(
            account: MainAccountDTO(id: 1, name: "Compte Principal", initial_balance: 10000, current_balance: 45230.50, created_at: "2026-01-01"),
            transactions: [
                MainAccountTransactionDTO(id: 1, type: "in", amount: 5000, reason: "Dépôt", reference_id: nil, note: "Vente du jour", created_at: "2026-06-03 18:00:00"),
                MainAccountTransactionDTO(id: 2, type: "out", amount: 1200, reason: "Retrait", reference_id: nil, note: "Fournitures bureau", created_at: "2026-06-02 10:00:00"),
            ]
        )
    }

    func deposit(amount: Double, reason: String, note: String?) async throws -> MainAccountActionResponse {
        MainAccountActionResponse(success: true, account: MainAccountDTO(id: 1, name: "Compte Principal", initial_balance: 10000, current_balance: 50230.50, created_at: "2026-01-01"))
    }

    func withdraw(amount: Double, reason: String, note: String?) async throws -> MainAccountActionResponse {
        MainAccountActionResponse(success: true, account: MainAccountDTO(id: 1, name: "Compte Principal", initial_balance: 10000, current_balance: 44030.50, created_at: "2026-01-01"))
    }

    func transferToPOS(amount: Double, note: String?) async throws -> MainAccountActionResponse {
        MainAccountActionResponse(success: true, account: MainAccountDTO(id: 1, name: "Compte Principal", initial_balance: 10000, current_balance: 44030.50, created_at: "2026-01-01"))
    }
}
