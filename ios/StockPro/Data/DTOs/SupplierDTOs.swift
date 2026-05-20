import Foundation

struct SupplierDTO: Decodable, Identifiable {
    let id: Int
    let name: String
    let email: String?
    let phone: String?
    let address: String?
    let contact_person: String?

    func toItem() -> SupplierListItem {
        SupplierListItem(
            id: id,
            name: name,
            email: email ?? "",
            phone: phone ?? "",
            address: address ?? "",
            contactPerson: contact_person ?? ""
        )
    }
}

struct SupplierCreateRequest: Encodable {
    let name: String
    let email: String?
    let phone: String?
    let address: String?
    let contact_person: String?
}

struct SupplierUpdateRequest: Encodable {
    let name: String
    let email: String?
    let phone: String?
    let address: String?
    let contact_person: String?
}

struct SupplierListItem: Identifiable {
    let id: Int
    let name: String
    let email: String
    let phone: String
    let address: String
    let contactPerson: String
}
