import Foundation

final class LocationService: LocationServiceProtocol {
    private let api = APIClient.shared

    func fetchLocations(warehouseId: Int? = nil) async throws -> [LocationListItem] {
        typealias Response = [LocationDTO]
        let dtos: Response = try await api.request(.locations(warehouseId: warehouseId))
        return dtos.map { $0.toItem() }
    }

    func fetchLocation(id: Int) async throws -> LocationListItem? {
        let all = try await fetchLocations()
        return all.first { $0.id == id }
    }

    func createLocation(_ request: LocationCreateRequest) async throws {
        let _: EmptyResponse = try await api.request(.createLocation, body: request)
    }

    func updateLocation(id: Int, _ request: LocationUpdateRequest) async throws {
        let _: EmptyResponse = try await api.request(.updateLocation(id), body: request)
    }

    func deleteLocation(id: Int) async throws {
        try await api.requestVoid(.deleteLocation(id))
    }
}

final class MockLocationService: LocationServiceProtocol {
    func fetchLocations(warehouseId: Int? = nil) async throws -> [LocationListItem] {
        let all = [
            LocationListItem(id: 1, warehouseId: 1, name: "Zone A - Rayons", type: "rack", capacity: 500, warehouseName: "Entrepôt Principal"),
            LocationListItem(id: 2, warehouseId: 1, name: "Zone B - Rayons", type: "rack", capacity: 500, warehouseName: "Entrepôt Principal"),
            LocationListItem(id: 3, warehouseId: 1, name: "Zone C - Rayons", type: "rack", capacity: 400, warehouseName: "Entrepôt Principal"),
            LocationListItem(id: 4, warehouseId: 1, name: "Zone D - Étagères", type: "shelf", capacity: 200, warehouseName: "Entrepôt Principal"),
            LocationListItem(id: 5, warehouseId: 1, name: "Stock Principal", type: "zone", capacity: 2000, warehouseName: "Entrepôt Principal"),
            LocationListItem(id: 6, warehouseId: 1, name: "Réception", type: "zone", capacity: 300, warehouseName: "Entrepôt Principal"),
            LocationListItem(id: 7, warehouseId: 1, name: "Expédition", type: "zone", capacity: 300, warehouseName: "Entrepôt Principal"),
            LocationListItem(id: 8, warehouseId: 1, name: "Contrôle Qualité", type: "zone", capacity: 100, warehouseName: "Entrepôt Principal"),
            LocationListItem(id: 9, warehouseId: 2, name: "Stock Principal", type: "zone", capacity: 1500, warehouseName: "Entrepôt Secondaire"),
            LocationListItem(id: 10, warehouseId: 2, name: "Rayons A", type: "rack", capacity: 300, warehouseName: "Entrepôt Secondaire"),
            LocationListItem(id: 11, warehouseId: 3, name: "Stock Dépôt", type: "zone", capacity: 800, warehouseName: "Dépôt Guéliz"),
        ]
        if let wid = warehouseId {
            return all.filter { $0.warehouseId == wid }
        }
        return all
    }

    func fetchLocation(id: Int) async throws -> LocationListItem? {
        try await fetchLocations().first { $0.id == id }
    }

    func createLocation(_ request: LocationCreateRequest) async throws {}
    func updateLocation(id: Int, _ request: LocationUpdateRequest) async throws {}
    func deleteLocation(id: Int) async throws {}
}
