import Foundation

struct LocationDTO: Decodable, Identifiable {
    let id: Int
    let warehouse_id: Int
    let name: String
    let type: String?
    let capacity: Int?
    let warehouse_name: String?
    let created_at: String?

    func toItem() -> LocationListItem {
        LocationListItem(
            id: id,
            warehouseId: warehouse_id,
            name: name,
            type: type ?? "rack",
            capacity: capacity,
            warehouseName: warehouse_name ?? ""
        )
    }
}

struct LocationCreateRequest: Encodable {
    let name: String
    let warehouse_id: Int
    let type: String?
    let capacity: Int?
}

struct LocationUpdateRequest: Encodable {
    let name: String
    let warehouse_id: Int
    let type: String?
    let capacity: Int?
}

struct LocationListItem: Identifiable {
    let id: Int
    let warehouseId: Int
    let name: String
    let type: String
    let capacity: Int?
    let warehouseName: String

    var typeLabel: String {
        switch type {
        case "rack": "Rayon"
        case "shelf": "Étagère"
        case "zone": "Zone"
        default: type
        }
    }
}
