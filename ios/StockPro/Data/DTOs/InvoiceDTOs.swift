import Foundation

struct InvoiceDTO: Decodable, Identifiable {
    let id: Int
    let invoice_number: String?
    let customer_id: Int?
    let customer_name: String?
    let status: String
    let total: Double
    let payment_method: String?
    let notes: String?
    let due_date: String?
    let created_at: String
    let paid_at: String?

    var statusLabel: String {
        switch status {
        case "ticket": "Ticket de caisse"
        case "brouillon": "Brouillon"
        case "envoyee": "Envoyée"
        case "payee": "Payée"
        case "annulee": "Annulée"
        default: status
        }
    }
}

struct InvoiceItemDTO: Decodable, Identifiable {
    let id: Int
    let product_id: Int
    let product_name: String?
    let quantity: Int
    let price: Double
    let total: Double?

    enum CodingKeys: String, CodingKey {
        case id, product_id, product_name, quantity
        case price = "unit_price"
        case total = "line_total"
    }
}

struct InvoiceCreateRequest: Encodable {
    let customer_id: Int?
    let items: [InvoiceItemRequest]
    let payment_method: String?
    let notes: String?
}

struct InvoiceItemRequest: Encodable {
    let product_id: Int
    let quantity: Int
    let price: Double
}

struct InvoiceUpdateRequest: Encodable {
    let status: String?
    let notes: String?
}
