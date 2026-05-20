import Foundation

struct CustomerDTO: Decodable, Identifiable {
    let id: Int
    let name: String
    let type: String?
    let email: String?
    let phone: String?
    let address: String?
    let client_code: String?
    let discount_rate: Double?
    let is_loyal: Bool?
    let is_active: Bool?
    let ice: String?
    let notes: String?

    func toItem() -> CustomerListItem {
        CustomerListItem(
            id: id,
            name: name,
            clientCode: client_code ?? "",
            phone: phone ?? "",
            email: email ?? "",
            type: type ?? "particulier",
            isLoyal: is_loyal ?? false
        )
    }
}

struct CustomerCreateRequest: Encodable {
    let name: String
    let type: String?
    let email: String?
    let phone: String?
    let address: String?
    let discount_rate: Double?
    let is_loyal: Bool?
    let notes: String?
}

struct CustomerUpdateRequest: Encodable {
    let name: String
    let type: String?
    let email: String?
    let phone: String?
    let address: String?
    let discount_rate: Double?
    let is_loyal: Bool?
    let notes: String?
}

struct CustomerListItem: Identifiable {
    let id: Int
    let name: String
    let clientCode: String
    let phone: String
    let email: String
    let type: String
    let isLoyal: Bool

    var typeLabel: String {
        switch type {
        case "school": "École"
        case "student": "Étudiant"
        case "company": "Société"
        default: "Particulier"
        }
    }
}
