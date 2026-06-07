import Foundation

struct SessionHistoryItem: Decodable, Identifiable {
    let id: Int
    let session_number: String?
    let warehouse_id: Int?
    let opening_cash: Double?
    let closing_cash: Double?
    let expected_cash: Double?
    let status: String?
    let opened_at: String?
    let closed_at: String?
    let total_sales: Double?
    let nb_transactions: Int?
    let total_cash_in: Double?
    let total_cash_out: Double?
    let warehouse_name: String?
}

struct SessionDetailResponse: Decodable {
    let session: SessionDetailInfo
    let transactions: [SessionTransactionDTO]
    let cash_movements: [SessionCashMovementDTO]
}

struct SessionDetailInfo: Decodable {
    let id: Int
    let session_number: String?
    let warehouse_id: Int?
    let warehouse_name: String?
    let opening_cash: Double?
    let closing_cash: Double?
    let expected_cash: Double?
    let status: String?
    let opened_at: String?
    let closed_at: String?
    let total_sales: Double?
}

struct SessionTransactionDTO: Decodable, Identifiable {
    let id: Int
    let ticket_number: String?
    let customer_name: String?
    let payment_method: String?
    let total: Double?
    let items_count: Int?
    let created_at: String?
}

struct SessionCashMovementDTO: Decodable, Identifiable {
    let id: Int
    let type: String?
    let amount: Double?
    let reason: String?
    let note: String?
    let created_at: String?
}
