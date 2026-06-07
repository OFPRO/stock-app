import Foundation

struct MainAccountResponse: Decodable {
    let account: MainAccountDTO
    let transactions: [MainAccountTransactionDTO]
}

struct MainAccountDTO: Decodable {
    let id: Int
    let name: String?
    let initial_balance: Double?
    let current_balance: Double
    let created_at: String?
}

struct MainAccountTransactionDTO: Decodable, Identifiable {
    let id: Int
    let type: String
    let amount: Double
    let reason: String?
    let reference_id: Int?
    let note: String?
    let created_at: String?
}

struct MainAccountActionResponse: Decodable {
    let success: Bool
    let account: MainAccountDTO
}

struct MainAccountDepositRequest: Encodable {
    let amount: Double
    let reason: String
    let note: String?
}

struct MainAccountWithdrawRequest: Encodable {
    let amount: Double
    let reason: String
    let note: String?
}

struct MainAccountTransferRequest: Encodable {
    let amount: Double
    let note: String?
}
