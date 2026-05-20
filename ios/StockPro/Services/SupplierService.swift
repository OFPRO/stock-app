import Foundation

final class SupplierService: SupplierServiceProtocol {
    private let api = APIClient.shared

    func fetchSuppliers() async throws -> [SupplierListItem] {
        typealias Response = [SupplierDTO]
        let dtos: Response = try await api.request(.suppliers)
        return dtos.map { $0.toItem() }
    }

    func fetchSupplier(id: Int) async throws -> SupplierListItem? {
        let all = try await fetchSuppliers()
        return all.first { $0.id == id }
    }

    func createSupplier(_ request: SupplierCreateRequest) async throws {
        let _: EmptyResponse = try await api.request(.createSupplier, body: request)
    }

    func updateSupplier(id: Int, _ request: SupplierUpdateRequest) async throws {
        let _: EmptyResponse = try await api.request(.updateSupplier(id), body: request)
    }

    func deleteSupplier(id: Int) async throws {
        try await api.requestVoid(.deleteSupplier(id))
    }
}

final class MockSupplierService: SupplierServiceProtocol {
    func fetchSuppliers() async throws -> [SupplierListItem] {
        [
            SupplierListItem(id: 1, name: "Papetech Maroc", email: "contact@papetech.ma", phone: "0522123456", address: "Zone Industrielle Sidi Ghanem, Marrakech", contactPerson: "Hassan El Fassi"),
            SupplierListItem(id: 2, name: "Informatique Distribution", email: "commandes@infodist.ma", phone: "0522345678", address: "10 Rue Mohammed V, Casablanca", contactPerson: "Karim Tazi"),
            SupplierListItem(id: 3, name: "Fourniture Pro", email: "", phone: "0522567890", address: "Av. Hassan II, Rabat", contactPerson: ""),
            SupplierListItem(id: 4, name: "Mobilier School", email: "info@mobilierschool.ma", phone: "0522789012", address: "Route de Safi, Marrakech", contactPerson: "Youssef El Omari"),
        ]
    }

    func fetchSupplier(id: Int) async throws -> SupplierListItem? {
        try await fetchSuppliers().first { $0.id == id }
    }

    func createSupplier(_ request: SupplierCreateRequest) async throws {}
    func updateSupplier(id: Int, _ request: SupplierUpdateRequest) async throws {}
    func deleteSupplier(id: Int) async throws {}
}
