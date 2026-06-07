import Foundation

struct OpenSessionResponse: Decodable {
    let success: Bool
    let session: POSSessionDTO?
    let session_number: String?
}

struct CreateCashMovementResponse: Decodable {
    let success: Bool
    let movement: CashMovementDTO?
}

struct CartItem: Identifiable, Equatable {
    let id = UUID()
    let productId: Int
    let productName: String
    let productSku: String
    var quantity: Int
    var unitPrice: Double
    var baseUnitPrice: Double
    var priceLoyal: Double?
    var priceSchool: Double?
    var priceStudent: Double?
    var discountPercent: Double

    var lineTotal: Double {
        let base = Double(quantity) * unitPrice
        return base - (base * discountPercent / 100)
    }

    var taxAmount: Double { lineTotal * 0.20 }
    var formattedUnitPrice: String { String(format: "%.2f MAD", unitPrice) }
    var formattedLineTotal: String { String(format: "%.2f MAD", lineTotal) }
}

struct BestSellerDTO: Decodable, Identifiable {
    let id: Int
    let name: String
    let price: Double
    let price_base: Double
    let sku: String
    let quantity: Int?
    let total_sold: Int

    var stockStatus: StockStatus {
        guard let q = quantity, q > 0 else { return .outOfStock }
        return .inStock
    }
}

struct POSSessionDTO: Decodable, Identifiable {
    let id: Int
    let session_number: String?
    let warehouse_id: Int?
    let opening_cash: Double?
    let closing_cash: Double?
    let expected_cash: Double?
    let status: String?
    let opened_at: String?
    let closed_at: String?

    var isOpen: Bool { status == "open" }
}

struct CloseSessionResponse: Decodable {
    let success: Bool
    let expected_cash: Double?
    let deposited: Bool?
}

struct CashMovementDTO: Decodable, Identifiable {
    let id: Int
    let session_id: Int?
    let type: String?
    let amount: Double?
    let reason: String?
    let note: String?
    let created_at: String?
}

struct POSTransactionDTO: Decodable, Identifiable {
    let id: Int
    let ticket_number: String?
    let transaction_number: String?
    let session_id: Int?
    let customer_id: Int?
    let customer_name: String?
    let payment_method: String?
    let subtotal: Double?
    let discount_total: Double?
    let tax_amount: Double?
    let total: Double?
    let tendered_amount: Double?
    let change_given: Double?
    let status: String?
    let created_at: String?
}

struct POSCustomerDTO: Decodable, Identifiable {
    let id: Int
    let name: String
    let client_code: String?
    let discount_rate: Double?
}

struct POSTransactionRequest: Encodable {
    let session_id: Int
    let customer_id: Int?
    let items: [TransactionItem]
    let payment_method: String
    let tendered_amount: Double
    let notes: String
    let is_credit: Bool

    struct TransactionItem: Encodable {
        let product_id: Int
        let quantity: Int
        let unit_price: Double
        let discount_percent: Double
        let product_name: String
        let product_sku: String
    }
}

struct POSTransactionResponse: Decodable {
    let success: Bool
    let document_number: String?
    let document_id: Int?
    let document_type: String?
    let document_status: String?
    let total: Double?
    let change_amount: Double?
    let customer_name: String?
}

enum DiscountTier: String, CaseIterable {
    case normal = "normal"
    case loyal = "loyal"
    case student = "student"
    case school = "school"

    var label: String {
        switch self {
        case .normal: "Normal"
        case .loyal: "Fidèle"
        case .student: "Étudiant"
        case .school: "École"
        }
    }

    private var fallbackRate: Double {
        switch self {
        case .normal: return 0
        case .loyal: return 0.15
        case .student: return 0.15
        case .school: return 0.20
        }
    }

    func priceFor(item: CartItem) -> Double {
        switch self {
        case .normal: return item.baseUnitPrice
        case .loyal: return item.priceLoyal ?? item.baseUnitPrice * (1 - 0.15)
        case .student: return item.priceStudent ?? item.baseUnitPrice * (1 - 0.15)
        case .school: return item.priceSchool ?? item.baseUnitPrice * (1 - 0.20)
        }
    }
}

enum PaymentMethod: String, CaseIterable {
    case cash = "cash"
    case card = "card"
    case mixed = "mixed"

    var label: String {
        switch self {
        case .cash: "Espèces"
        case .card: "Carte bancaire"
        case .mixed: "Mixte"
        }
    }

    var icon: String {
        switch self {
        case .cash: "banknote"
        case .card: "creditcard"
        case .mixed: "rectangle.split.2x2"
        }
    }
}
