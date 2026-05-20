import Foundation

@MainActor
final class DashboardViewModel: ObservableObject {
    @Published var state: ViewState<DashboardData> = .loading
    @Published var dailySales: [DailySalesData] = []
    @Published var categoryDistrib: [CategoryDistribution] = []
    @Published var topProducts: [TopProductData] = []

    private let kpiProvider: KPIProviderProtocol

    init(kpiProvider: KPIProviderProtocol = KPIProvider()) {
        self.kpiProvider = kpiProvider
    }

    func refresh() async {
        state = .loading
        do {
            async let header = kpiProvider.fetchHeaderKPIs()
            async let financial = kpiProvider.fetchFinancialKPIs()
            async let pos = kpiProvider.fetchPOSKPIs()
            async let sales = kpiProvider.fetchDailySales(days: 30)
            async let categories = kpiProvider.fetchCategoryDistribution()
            async let top = kpiProvider.fetchTopSellingProducts(limit: 10)

            let (h, f, p, s, c, t) = try await (header, financial, pos, sales, categories, top)
            dailySales = s
            categoryDistrib = c
            topProducts = t
            state = .loaded(DashboardData(headerKPIs: h, financialKPIs: f, posKPIs: p))
        } catch {
            state = .error(.from(error))
        }
    }
}

struct DashboardData {
    let headerKPIs: DashboardHeaderKPIs
    let financialKPIs: DashboardFinancialKPIs
    let posKPIs: DashboardPOSKPIs
}

struct DashboardHeaderKPIs {
    let caJour: String
    let nbVentes: String
    let ticketMoyen: String
    let margeBrute: String
}

struct DashboardFinancialKPIs {
    let creances: String
    let tauxEncaissement: String
    let valeurStock: String
    let ruptures: String
}

struct DashboardPOSKPIs {
    let total: String
    let especes: String
    let carte: String
}

// MARK: - Chart Data Models

struct DailySalesData: Identifiable {
    var id: String { date }
    let date: String
    let amount: Double
}

struct CategoryDistribution: Identifiable {
    var id: String { category }
    let category: String
    let percentage: Double
    let amount: Double
}

struct TopProductData: Identifiable {
    var id: String { name }
    let name: String
    let quantity: Int
    let revenue: Double
}
