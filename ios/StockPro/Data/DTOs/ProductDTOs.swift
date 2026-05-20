import SwiftUI

struct ProductDetailDTO: Decodable {
    let id: Int
    let name: String
    let sku: String
    let description: String?
    let price: Double
    let quantity: Int
    let category: String?
    let barcode: String?
    let min_stock: Int?
    let max_stock: Int?
    let is_active: Bool
    let weight: Double?
    let unit: String?
    let purchase_price: Double?
    let wholesale_price: Double?

    func toDetail() -> ProductDetail {
        ProductDetail(
            id: id,
            name: name,
            sku: sku,
            description: description ?? "",
            price: price,
            quantity: quantity,
            category: category ?? "Général",
            barcode: barcode ?? "",
            minStock: min_stock ?? 0,
            maxStock: max_stock ?? 0,
            isActive: is_active,
            weight: weight ?? 0,
            unit: unit ?? "pièce",
            purchasePrice: purchase_price ?? 0,
            wholesalePrice: wholesale_price ?? 0
        )
    }
}

struct ProductCreateRequest: Encodable {
    let name: String
    let sku: String
    let description: String?
    let price: Double
    let category: String?
    let barcode: String?
    let min_stock: Int?
    let max_stock: Int?
    let weight: Double?
    let unit: String?
    let purchase_price: Double?
    let wholesale_price: Double?
}

struct ProductUpdateRequest: Encodable {
    let name: String?
    let sku: String?
    let description: String?
    let price: Double?
    let category: String?
    let barcode: String?
    let min_stock: Int?
    let max_stock: Int?
    let is_active: Bool?
    let weight: Double?
    let unit: String?
    let purchase_price: Double?
    let wholesale_price: Double?
}

struct ProductDetail {
    let id: Int
    let name: String
    let sku: String
    let description: String
    let price: Double
    let quantity: Int
    let category: String
    let barcode: String
    let minStock: Int
    let maxStock: Int
    let isActive: Bool
    let weight: Double
    let unit: String
    let purchasePrice: Double
    let wholesalePrice: Double

    var formattedPrice: String { String(format: "%.2f MAD", price) }
    var formattedPurchasePrice: String { String(format: "%.2f MAD", purchasePrice) }
    var formattedWholesalePrice: String { String(format: "%.2f MAD", wholesalePrice) }
    var stockStatus: StockStatus {
        if quantity <= 0 { return .outOfStock }
        if quantity <= minStock { return .low }
        return .inStock
    }
}

enum StockStatus {
    case inStock, low, outOfStock
    var label: String {
        switch self {
        case .inStock: "En stock"
        case .low: "Stock faible"
        case .outOfStock: "Rupture"
        }
    }
    var color: Color {
        switch self {
        case .inStock: AppColor.success
        case .low: AppColor.warning
        case .outOfStock: AppColor.error
        }
    }
}

struct StockMovementDTO: Decodable, Identifiable {
    let id: Int
    let product_id: Int
    let quantity: Int
    let movement_type: String
    let reference: String?
    let created_at: String
    let warehouse_name: String?
}

struct PriceTierDTO: Decodable {
    let type: String
    let label: String
    let price: Double
    let discount_percent: Int

    var formattedPrice: String { String(format: "%.2f MAD", price) }
}

struct ForSaleProductDTO: Decodable, Identifiable {
    let id: Int
    let name: String
    let sku: String
    let barcode: String?
    let price: Double
    let sale_price: Double?
    let quantity: Int
    let category: String?
    let warehouse_id: Int?

    func toScannedProduct() -> ScannedProduct {
        ScannedProduct(
            id: id,
            name: name,
            sku: sku,
            price: String(format: "%.2f MAD", sale_price ?? price),
            stock: quantity,
            barcode: barcode ?? ""
        )
    }
}

struct ScannedProduct: Identifiable, Equatable {
    let id: Int
    let name: String
    let sku: String
    let price: String
    let stock: Int
    let barcode: String
}

struct CategoryDTO: Decodable, Identifiable {
    var id: String { name }
    let name: String
    let count: Int?
}
