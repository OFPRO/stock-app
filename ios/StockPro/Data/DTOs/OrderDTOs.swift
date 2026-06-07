import Foundation

struct OrderDTO: Decodable, Identifiable {
    let id: Int
    let order_number: String?
    let supplier_id: Int?
    let supplier_name: String?
    let warehouse_id: Int?
    let warehouse_name: String?
    let status: String
    let total: Double?
    let notes: String?
    let created_at: String
    let received_at: String?

    var statusLabel: String {
        switch status {
        case "brouillon": "Brouillon"
        case "recue": "Reçue"
        case "payee": "Payée"
        case "annulee": "Annulée"
        default: status
        }
    }
}

struct OrderItemDTO: Decodable, Identifiable {
    let id: Int
    let product_id: Int
    let product_name: String?
    let quantity: Int
    let price: Double
    let total: Double?
}

struct OrderCreateRequest: Encodable {
    let supplier_id: Int
    let warehouse_id: Int
    let notes: String?
    let items: [OrderItemRequest]
}

struct OrderItemRequest: Encodable {
    let product_id: Int
    let quantity: Int
    let price: Double
}

struct OrderUpdateRequest: Encodable {
    let status: String
    let notes: String?
}
