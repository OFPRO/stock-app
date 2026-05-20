import Foundation

struct WarehouseDTO: Decodable, Identifiable {
    let id: Int
    let name: String
    let address: String?
    let manager: String?
    let phone: String?
    let is_default: Int?
    let created_at: String?

    func toItem() -> WarehouseListItem {
        WarehouseListItem(
            id: id,
            name: name,
            address: address ?? "",
            manager: manager ?? "",
            phone: phone ?? "",
            isDefault: (is_default ?? 0) == 1
        )
    }
}

struct WarehouseCreateRequest: Encodable {
    let name: String
    let address: String?
    let manager: String?
}

struct WarehouseListItem: Identifiable {
    let id: Int
    let name: String
    let address: String
    let manager: String
    let phone: String
    let isDefault: Bool
}
