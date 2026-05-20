import Foundation

@MainActor
final class WarehouseListViewModel: ObservableObject {
    @Published var state: ViewState<[WarehouseListItem]> = .loading
    @Published var searchQuery = ""

    private let warehouseService: WarehouseServiceProtocol

    init(warehouseService: WarehouseServiceProtocol = WarehouseService()) {
        self.warehouseService = warehouseService
    }

    var filteredWarehouses: [WarehouseListItem] {
        guard case .loaded(let warehouses) = state else { return [] }
        if searchQuery.isEmpty { return warehouses }
        return warehouses.filter {
            $0.name.localizedCaseInsensitiveContains(searchQuery) ||
            $0.address.localizedCaseInsensitiveContains(searchQuery) ||
            $0.manager.localizedCaseInsensitiveContains(searchQuery)
        }
    }

    func load() async {
        state = .loading
        do {
            let warehouses = try await warehouseService.fetchWarehouses()
            state = warehouses.isEmpty ? .empty("") : .loaded(warehouses)
        } catch {
            state = .error(.from(error))
        }
    }


}
