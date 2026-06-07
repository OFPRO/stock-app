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

    enum CodingKeys: String, CodingKey {
        case id, name, sku, description, price, quantity, category, barcode
        case min_stock = "min_quantity"
        case max_stock = "max_quantity"
        case is_active = "is_deleted"
        case weight, unit
        case purchase_price = "purchase_price_avg"
        case wholesale_price
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(Int.self, forKey: .id)
        name = try container.decode(String.self, forKey: .name)
        sku = try container.decode(String.self, forKey: .sku)
        description = try container.decodeIfPresent(String.self, forKey: .description)
        price = try container.decode(Double.self, forKey: .price)
        quantity = try container.decode(Int.self, forKey: .quantity)
        category = try container.decodeIfPresent(String.self, forKey: .category)
        barcode = try container.decodeIfPresent(String.self, forKey: .barcode)
        min_stock = try container.decodeIfPresent(Int.self, forKey: .min_stock)
        max_stock = try container.decodeIfPresent(Int.self, forKey: .max_stock)
        let deleted = try container.decodeIfPresent(Int.self, forKey: .is_active) ?? 0
        is_active = deleted == 0
        weight = try container.decodeIfPresent(Double.self, forKey: .weight)
        unit = try container.decodeIfPresent(String.self, forKey: .unit)
        purchase_price = try container.decodeIfPresent(Double.self, forKey: .purchase_price)
        wholesale_price = try container.decodeIfPresent(Double.self, forKey: .wholesale_price)
    }

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
    let type: String
    let quantity: Int
    let source_location_id: Int?
    let dest_location_id: Int?
    let lot_number: String?
    let serial_number: String?
    let note: String?
    let product_name: String
    let source_location: String?
    let dest_location: String?
    let created_at: String
}

struct StockMovementItem: Identifiable {
    let id: Int
    let productName: String
    let type: String
    let quantity: Int
    let sourceLocation: String?
    let destLocation: String?
    let note: String?
    let createdAt: String

    var typeLabel: String {
        switch type {
        case "in": "Entrée"
        case "out": "Sortie"
        case "transfer": "Transfert"
        case "inter_warehouse": "Trans. Entrepôt"
        case "sale": "Vente"
        default: type
        }
    }

    var typeIcon: String {
        switch type {
        case "in": "arrow.down.to.line"
        case "out": "arrow.up.from.line"
        case "transfer": "arrow.left.arrow.right"
        case "inter_warehouse": "building.2.arrow.circlepath"
        case "sale": "cart"
        default: "arrow.triangle.swap"
        }
    }

    var typeColor: Color {
        switch type {
        case "in": AppColor.success
        case "out": AppColor.error
        case "transfer": AppColor.info
        case "inter_warehouse": AppColor.warning
        case "sale": AppColor.brand
        default: .secondary
        }
    }

    var locationSummary: String {
        switch type {
        case "in": destLocation ?? "—"
        case "out": sourceLocation ?? "—"
        case "transfer", "inter_warehouse":
            [sourceLocation, destLocation].compactMap { $0 }.joined(separator: " → ")
        default: "—"
        }
    }

    var displayQuantity: String {
        let qty = (type == "out" || type == "sale") ? -quantity : quantity
        return qty >= 0 ? "+\(qty)" : "\(qty)"
    }

    var quantityColor: Color {
        switch type {
        case "in": AppColor.success
        case "out", "sale": AppColor.error
        case "transfer", "inter_warehouse": AppColor.info
        default: .primary
        }
    }
}

struct StockMovementCreateRequest: Encodable {
    let product_id: Int
    let type: String
    let quantity: Int
    let location_id: Int?
    let note: String?
}

struct StockTransferRequest: Encodable {
    let product_id: Int
    let quantity: Int
    let from_location_id: Int?
    let to_location_id: Int?
    let note: String?
}

struct InterWarehouseTransferRequest: Encodable {
    let product_id: Int
    let quantity: Int
    let from_warehouse_id: Int
    let to_warehouse_id: Int
    let note: String?
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
    let price_loyal: Double?
    let price_school: Double?
    let price_student: Double?
    let quantity: Int
    let min_quantity: Int?
    let category: String?
    let warehouse_id: Int?

    var stockStatus: StockStatus {
        if quantity <= 0 { return .outOfStock }
        if let min = min_quantity, quantity <= min { return .low }
        return .inStock
    }

    var formattedPrice: String {
        String(format: "%.2f MAD", sale_price ?? price)
    }

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

    init(name: String, count: Int? = nil) {
        self.name = name
        self.count = count
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        name = try container.decode(String.self)
        count = nil
    }
}
