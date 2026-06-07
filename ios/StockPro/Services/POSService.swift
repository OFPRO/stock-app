import Foundation

final class POSService: POSServiceProtocol {
    private let api = APIClient.shared

    func fetchSession() async throws -> POSSessionDTO? {
        let sessions: [POSSessionDTO] = try await api.request(.posSessions)
        return sessions.first
    }

    func openSession(openingCash: Double) async throws -> POSSessionDTO {
        struct Body: Encodable { let opening_cash: Double }
        let response: OpenSessionResponse = try await api.request(.openSession, body: Body(opening_cash: openingCash))
        guard let session = response.session else {
            throw AppError.businessRuleViolation("Impossible d'ouvrir la session")
        }
        return session
    }

    func closeSession(id: Int, closingCash: Double) async throws -> CloseSessionResponse {
        struct Body: Encodable { let closing_cash: Double; let deposit_to_main: Bool }
        return try await api.request(.closeSession(id), body: Body(closing_cash: closingCash, deposit_to_main: true))
    }

    func createTransaction(_ request: POSTransactionRequest) async throws -> POSTransactionResponse {
        try await api.request(.createTransaction, body: request)
    }

    func fetchBestSellers() async throws -> [BestSellerDTO] {
        try await api.request(.bestSellers)
    }

    func fetchPOSCustomers() async throws -> [POSCustomerDTO] {
        try await api.request(.posCustomers)
    }

    func fetchCashMovements() async throws -> [CashMovementDTO] {
        try await api.request(.cashMovements)
    }

    func createCashMovement(type: String, amount: Double, reason: String, note: String?) async throws {
        struct Body: Encodable {
            let type: String
            let amount: Double
            let reason: String
            let note: String?
        }
        let _: CreateCashMovementResponse = try await api.request(.createCashMovement, body: Body(type: type, amount: amount, reason: reason, note: note))
    }

    func fetchRecentTransactions(sessionId: Int?, limit: Int?) async throws -> [POSTransactionDTO] {
        try await api.request(.recentTransactions(sessionId: sessionId, limit: limit))
    }
}

final class MockPOSService: POSServiceProtocol {
    func fetchSession() async throws -> POSSessionDTO? {
        POSSessionDTO(
            id: 1,
            session_number: "SES-20260520-0001",
            warehouse_id: 1,
            opening_cash: 500,
            closing_cash: nil,
            expected_cash: nil,
            status: "open",
            opened_at: "2026-05-20T08:00:00",
            closed_at: nil
        )
    }

    func openSession(openingCash: Double) async throws -> POSSessionDTO {
        POSSessionDTO(
            id: 1,
            session_number: "SES-20260520-0001",
            warehouse_id: 1,
            opening_cash: openingCash,
            closing_cash: nil,
            expected_cash: nil,
            status: "open",
            opened_at: ISO8601DateFormatter().string(from: Date()),
            closed_at: nil
        )
    }

    func closeSession(id: Int, closingCash: Double) async throws -> CloseSessionResponse {
        CloseSessionResponse(success: true, expected_cash: closingCash, deposited: true)
    }

    func createTransaction(_ request: POSTransactionRequest) async throws -> POSTransactionResponse {
        let total = request.items.reduce(0.0) { sum, item in
            let line = Double(item.quantity) * item.unit_price
            return sum + line + line * 0.20
        }
        return POSTransactionResponse(
            success: true,
            document_number: "Ticket-20260520-0001",
            document_id: 1,
            document_type: "ticket",
            document_status: "completed",
            total: total,
            change_amount: max(0, request.tendered_amount - total),
            customer_name: request.customer_id != nil ? "Client Test" : "Client Comptoir"
        )
    }

    func fetchBestSellers() async throws -> [BestSellerDTO] {
        [
            BestSellerDTO(id: 1, name: "Ramette A4", price: 45, price_base: 30, sku: "PAP-A4-001", quantity: 120, total_sold: 42),
            BestSellerDTO(id: 2, name: "Stylo Bleu", price: 3.5, price_base: 2.5, sku: "STY-BLE-002", quantity: 500, total_sold: 38),
            BestSellerDTO(id: 3, name: "Cahier 96p", price: 12, price_base: 8, sku: "CAH-96-003", quantity: 0, total_sold: 31),
            BestSellerDTO(id: 4, name: "Clavier USB", price: 120, price_base: 80, sku: "CLV-USB-004", quantity: 8, total_sold: 15),
            BestSellerDTO(id: 5, name: "Souris Optique", price: 65, price_base: 40, sku: "SRL-OPT-005", quantity: 15, total_sold: 12),
        ]
    }

    func fetchPOSCustomers() async throws -> [POSCustomerDTO] {
        [
            POSCustomerDTO(id: 1, name: "Client Comptoir", client_code: nil, discount_rate: 0),
            POSCustomerDTO(id: 2, name: "Pharmacie Centrale", client_code: "CLI-001", discount_rate: 10),
            POSCustomerDTO(id: 3, name: "École Al Massira", client_code: "CLI-002", discount_rate: 20),
        ]
    }

    func fetchCashMovements() async throws -> [CashMovementDTO] { [] }

    func createCashMovement(type: String, amount: Double, reason: String, note: String?) async throws {}

    func fetchRecentTransactions(sessionId: Int?, limit: Int?) async throws -> [POSTransactionDTO] { [] }
}
