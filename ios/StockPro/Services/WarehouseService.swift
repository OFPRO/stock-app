import Foundation

final class WarehouseService: WarehouseServiceProtocol {
    private let api = APIClient.shared

    func fetchWarehouses() async throws -> [WarehouseListItem] {
        typealias Response = [WarehouseDTO]
        let dtos: Response = try await api.request(.warehouses)
        return dtos.map { $0.toItem() }
    }

    func fetchWarehouse(id: Int) async throws -> WarehouseListItem? {
        let all = try await fetchWarehouses()
        return all.first { $0.id == id }
    }

    func createWarehouse(_ request: WarehouseCreateRequest) async throws {
        let _: EmptyResponse = try await api.request(.createWarehouse, body: request)
    }
}

final class MockWarehouseService: WarehouseServiceProtocol {
    func fetchWarehouses() async throws -> [WarehouseListItem] {
        [
            WarehouseListItem(id: 1, name: "Entrepôt Principal", address: "Av. Mohammed VI, Marrakech", manager: "Ahmed Benali", phone: "0522123456", isDefault: true),
            WarehouseListItem(id: 2, name: "Entrepôt Secondaire", address: "Zone Industrielle Sidi Ghanem", manager: "Fatima Zahra", phone: "0522345678", isDefault: false),
            WarehouseListItem(id: 3, name: "Dépôt Guéliz", address: "Rue Yougoslavie, Guéliz", manager: "Karim Tazi", phone: "0522567890", isDefault: false),
        ]
    }

    func fetchWarehouse(id: Int) async throws -> WarehouseListItem? {
        try await fetchWarehouses().first { $0.id == id }
    }

    func createWarehouse(_ request: WarehouseCreateRequest) async throws {}
}
