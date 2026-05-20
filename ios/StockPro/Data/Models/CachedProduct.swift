import SwiftData
import Foundation

@Model
final class CachedProduct {
    @Attribute(.unique) var id: Int
    var name: String
    var sku: String
    var price: Decimal
    var category: String
    var stockQuantity: Int
    var lastUpdated: Date

    init(id: Int, name: String, sku: String, price: Decimal, category: String, stockQuantity: Int, lastUpdated: Date = .now) {
        self.id = id
        self.name = name
        self.sku = sku
        self.price = price
        self.category = category
        self.stockQuantity = stockQuantity
        self.lastUpdated = lastUpdated
    }
}

@Model
final class CachedCustomer {
    @Attribute(.unique) var id: Int
    var name: String
    var clientCode: String
    var type: String
    var lastUpdated: Date

    init(id: Int, name: String, clientCode: String, type: String, lastUpdated: Date = .now) {
        self.id = id
        self.name = name
        self.clientCode = clientCode
        self.type = type
        self.lastUpdated = lastUpdated
    }
}

@Model
final class CachedSupplier {
    @Attribute(.unique) var id: Int
    var name: String
    var lastUpdated: Date

    init(id: Int, name: String, lastUpdated: Date = .now) {
        self.id = id
        self.name = name
        self.lastUpdated = lastUpdated
    }
}

@Model
final class CachedWarehouse {
    @Attribute(.unique) var id: Int
    var name: String
    var lastUpdated: Date

    init(id: Int, name: String, lastUpdated: Date = .now) {
        self.id = id
        self.name = name
        self.lastUpdated = lastUpdated
    }
}

@Model
final class CachedKPI {
    @Attribute(.unique) var key: String
    var value: Decimal
    var label: String
    var lastUpdated: Date

    init(key: String, value: Decimal, label: String, lastUpdated: Date = .now) {
        self.key = key
        self.value = value
        self.label = label
        self.lastUpdated = lastUpdated
    }
}

@Model
final class CachedNotification {
    @Attribute(.unique) var id: Int
    var message: String
    var isRead: Bool
    var createdAt: Date

    init(id: Int, message: String, isRead: Bool, createdAt: Date = .now) {
        self.id = id
        self.message = message
        self.isRead = isRead
        self.createdAt = createdAt
    }
}
