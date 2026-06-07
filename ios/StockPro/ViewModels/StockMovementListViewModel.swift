import Foundation
import SwiftUI

@MainActor
final class StockMovementListViewModel: ObservableObject {
    @Published var movements: [StockMovementItem] = []
    @Published var isLoading = false
    @Published var errorMessage: String?

    @Published var filterProduct = ""
    @Published var filterWarehouse: Int?
    @Published var filterType: String?

    @Published var warehouses: [WarehouseListItem] = []
    @Published var showCreateSheet = false

    private let stockService: StockServiceProtocol
    private let warehouseService: WarehouseServiceProtocol

    init(stockService: StockServiceProtocol = StockService(),
         warehouseService: WarehouseServiceProtocol = WarehouseService()) {
        self.stockService = stockService
        self.warehouseService = warehouseService
    }

    var filteredMovements: [StockMovementItem] {
        var result = movements
        if !filterProduct.isEmpty {
            result = result.filter { $0.productName.localizedCaseInsensitiveContains(filterProduct) }
        }
        if let type = filterType {
            result = result.filter { $0.type == type }
        }
        return result
    }

    func load() async {
        isLoading = true
        errorMessage = nil
        do {
            async let movementsTask = stockService.fetchMovements(productId: nil, warehouseId: filterWarehouse)
            async let warehousesTask = warehouseService.fetchWarehouses()
            (movements, warehouses) = try await (movementsTask, warehousesTask)
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }
}
