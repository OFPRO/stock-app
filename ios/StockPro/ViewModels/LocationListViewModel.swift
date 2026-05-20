import Foundation

@MainActor
final class LocationListViewModel: ObservableObject {
    @Published var state: ViewState<[LocationListItem]> = .loading
    @Published var searchQuery = ""
    @Published var selectedWarehouseId: Int? = nil

    private let locationService: LocationServiceProtocol
    private let warehouseService: WarehouseServiceProtocol

    init(locationService: LocationServiceProtocol = LocationService(),
         warehouseService: WarehouseServiceProtocol = WarehouseService()) {
        self.locationService = locationService
        self.warehouseService = warehouseService
    }

    var filteredLocations: [LocationListItem] {
        guard case .loaded(let locations) = state else { return [] }
        let filtered: [LocationListItem]
        if let wid = selectedWarehouseId {
            filtered = locations.filter { $0.warehouseId == wid }
        } else {
            filtered = locations
        }
        if searchQuery.isEmpty { return filtered }
        return filtered.filter {
            $0.name.localizedCaseInsensitiveContains(searchQuery) ||
            $0.warehouseName.localizedCaseInsensitiveContains(searchQuery)
        }
    }

    func load() async {
        state = .loading
        do {
            let locations = try await locationService.fetchLocations(warehouseId: nil)
            state = locations.isEmpty ? .empty("") : .loaded(locations)
        } catch {
            state = .error(.from(error))
        }
    }

    func deleteLocation(at indexSet: IndexSet) async {
        guard case .loaded(let locations) = state else { return }
        for idx in indexSet {
            try? await locationService.deleteLocation(id: locations[idx].id)
        }
        await load()
    }
}
