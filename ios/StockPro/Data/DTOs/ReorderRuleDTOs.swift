import Foundation

struct ReorderRuleDTO: Decodable, Identifiable {
    let id: Int
    let product_id: Int
    let product_name: String?
    let supplier_id: Int?
    let supplier_name: String?
    let warehouse_id: Int
    let warehouse_name: String?
    let min_quantity: Int
    let max_quantity: Int
    let trigger_type: String?
    let is_active: Bool
    let created_at: String?
}

struct ReorderRuleCreateRequest: Encodable {
    let product_id: Int
    let supplier_id: Int?
    let warehouse_id: Int
    let min_quantity: Int
    let max_quantity: Int
    let trigger_type: String?
}

struct ReorderRuleUpdateRequest: Encodable {
    let min_quantity: Int?
    let max_quantity: Int?
    let trigger_type: String?
    let is_active: Bool?
    let supplier_id: Int?
}

struct ReplenishmentSuggestion: Decodable, Identifiable {
    let id: Int
    let product_id: Int
    let product_name: String
    let current_stock: Int
    let suggested_quantity: Int
    let warehouse_id: Int
    let warehouse_name: String?
    let supplier_name: String?
}
