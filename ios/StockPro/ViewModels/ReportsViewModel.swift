import Foundation

enum ReportTab: String, CaseIterable {
    case overview = "overview"
    case rotation = "rotation"
    case lowStock = "low_stock"
    case categories = "categories"
    case expiry = "expiry"
    case warehouses = "warehouses"

    var label: String {
        switch self {
        case .overview: "Aperçu"
        case .rotation: "Rotation"
        case .lowStock: "Stock Faible"
        case .categories: "Catégories"
        case .expiry: "Expirations"
        case .warehouses: "Entrepôts"
        }
    }

    var icon: String {
        switch self {
        case .overview: "chart.pie"
        case .rotation: "arrow.triangle.2.circlepath"
        case .lowStock: "exclamationmark.triangle"
        case .categories: "square.grid.3x3"
        case .expiry: "calendar"
        case .warehouses: "building.2"
        }
    }
}

@MainActor
final class ReportsViewModel: ObservableObject {
    @Published var selectedTab: ReportTab = .overview
    @Published var overview: ViewState<ReportOverviewDTO> = .loading
    @Published var rotation: ViewState<[RotationReportItem]>?
    @Published var lowStock: ViewState<[LowStockReportItem]>?
    @Published var categories: ViewState<[CategoryReportItem]>?
    @Published var expiry: ViewState<[ExpiryReportItem]>?
    @Published var warehouses: ViewState<[WarehouseReportItem]>?

    private let reportService: ReportServiceProtocol

    init(reportService: ReportServiceProtocol = ReportService()) {
        self.reportService = reportService
    }

    func loadOverview() async {
        overview = .loading
        do {
            let data = try await reportService.fetchOverview(warehouseId: nil)
            overview = .loaded(data)
        } catch {
            overview = .error(.from(error))
        }
    }

    func loadRotation() async {
        rotation = .loading
        do {
            let data = try await reportService.fetchRotation(warehouseId: nil)
            rotation = data.isEmpty ? .empty("Aucune donnée") : .loaded(data)
        } catch {
            rotation = .error(.from(error))
        }
    }

    func loadLowStock() async {
        lowStock = .loading
        do {
            let data = try await reportService.fetchLowStock(warehouseId: nil)
            lowStock = data.isEmpty ? .empty("Aucun produit en rupture") : .loaded(data)
        } catch {
            lowStock = .error(.from(error))
        }
    }

    func loadCategories() async {
        categories = .loading
        do {
            let data = try await reportService.fetchCategories(warehouseId: nil)
            categories = data.isEmpty ? .empty("Aucune catégorie") : .loaded(data)
        } catch {
            categories = .error(.from(error))
        }
    }

    func loadExpiry() async {
        expiry = .loading
        do {
            let data = try await reportService.fetchExpiry(warehouseId: nil)
            expiry = data.isEmpty ? .empty("Aucun produit expiré") : .loaded(data)
        } catch {
            expiry = .error(.from(error))
        }
    }

    func loadWarehouses() async {
        warehouses = .loading
        do {
            let data = try await reportService.fetchWarehouseReport()
            warehouses = data.isEmpty ? .empty("Aucun entrepôt") : .loaded(data)
        } catch {
            warehouses = .error(.from(error))
        }
    }

    func loadSelectedTab() async {
        switch selectedTab {
        case .overview: await loadOverview()
        case .rotation: await loadRotation()
        case .lowStock: await loadLowStock()
        case .categories: await loadCategories()
        case .expiry: await loadExpiry()
        case .warehouses: await loadWarehouses()
        }
    }
}
