import Foundation

struct ReportOverviewDTO: Decodable {
    let total_products: Int
    let total_value: Double
    let low_stock: Int
    let out_of_stock: Int
    let expiring_soon: Int
}

struct RotationReportItem: Decodable, Identifiable {
    let id = UUID()
    let name: String
    let quantity: Int
    let min_quantity: Int
    let movements: Int
}

struct ExpiryReportItem: Decodable, Identifiable {
    let id = UUID()
    let name: String
    let lot_number: String?
    let expiry_date: String?
    let quantity: Int
}

struct CategoryReportItem: Decodable, Identifiable {
    let id = UUID()
    let category: String
    let count: Int
    let total_qty: Int?
    let value: Double?
}

struct LowStockReportItem: Decodable, Identifiable {
    let id = UUID()
    let name: String
    let quantity: Int
    let min_quantity: Int
    let max_quantity: Int?
    let price: Double?
    let needed: Int
}

struct WarehouseReportItem: Decodable, Identifiable {
    let id: Int
    let name: String
    let address: String?
    let product_count: Int
    let total_quantity: Int
    let total_value: Double
}

extension ReportOverviewDTO {
    var formattedValue: String {
        String(format: "%.2f MAD", total_value)
    }
}
