import Foundation

final class KPIProvider: KPIProviderProtocol {
    private let api = APIClient.shared

    func fetchHeaderKPIs() async throws -> DashboardHeaderKPIs {
        let sales: SalesKPIDTO = try await api.request(.salesKPIs)
        return DashboardHeaderKPIs(
            caJour: String(format: "%.2f MAD", sales.ca_jour ?? 0),
            nbVentes: "\(sales.nb_ventes_jour ?? 0)",
            ticketMoyen: String(format: "%.2f MAD", sales.ticket_moyen ?? 0),
            margeBrute: String(format: "%.1f%%", sales.marge_brute ?? 0)
        )
    }

    func fetchFinancialKPIs() async throws -> DashboardFinancialKPIs {
        let receivables: ReceivablesKPIDTO = try await api.request(.receivablesKPIs)
        let dashboard: DashboardKPIDTO = try await api.request(.dashboardKPIs)
        return DashboardFinancialKPIs(
            creances: String(format: "%.2f MAD", receivables.total_creances ?? 0),
            tauxEncaissement: String(format: "%.1f%%", receivables.taux_encaissement ?? 0),
            valeurStock: String(format: "%.2f MAD", dashboard.total_value ?? 0),
            ruptures: "\(dashboard.out_of_stock ?? 0)"
        )
    }

    func fetchPOSKPIs() async throws -> DashboardPOSKPIs {
        let sessions: SessionsSummaryDTO = try await api.request(.sessionsSummary(period: 30))
        let payments: PaymentMethodsDTO = try await api.request(.paymentMethods)
        return DashboardPOSKPIs(
            total: String(format: "%.2f MAD", sessions.total_sales_period ?? 0),
            especes: String(format: "%.2f MAD", payments.methods?.cash?.total ?? 0),
            carte: String(format: "%.2f MAD", payments.methods?.card?.total ?? 0)
        )
    }

    func fetchDailySales(days: Int) async throws -> [DailySalesData] {
        typealias Response = [DailySalesDTO]
        let dtos: Response = try await api.request(.salesDaily(days: days))
        return dtos.map { DailySalesData(date: $0.date, amount: $0.ca ?? $0.total ?? $0.amount ?? 0) }
    }

    func fetchCategoryDistribution() async throws -> [CategoryDistribution] {
        typealias Response = [CategoryDistribDTO]
        let dtos: Response = try await api.request(.categoriesDistribution)
        return dtos.map { CategoryDistribution(category: $0.category ?? $0.name ?? "Autre", percentage: $0.percentage ?? $0.percent ?? 0, amount: $0.ca ?? $0.total ?? $0.amount ?? 0) }
    }

    func fetchTopSellingProducts(limit: Int) async throws -> [TopProductData] {
        typealias Response = [TopProductDTO]
        let dtos: Response = try await api.request(.topSellingProducts(limit: limit))
        return dtos.map { TopProductData(name: $0.name ?? $0.product_name ?? "Produit", quantity: $0.qty_vendue ?? $0.quantity ?? $0.total_qty ?? 0, revenue: $0.ca ?? $0.revenue ?? $0.total_revenue ?? 0) }
    }
}

final class MockKPIProvider: KPIProviderProtocol {
    func fetchHeaderKPIs() async throws -> DashboardHeaderKPIs {
        DashboardHeaderKPIs(caJour: "12 450,00 MAD", nbVentes: "24", ticketMoyen: "518,75 MAD", margeBrute: "32.5%")
    }

    func fetchFinancialKPIs() async throws -> DashboardFinancialKPIs {
        DashboardFinancialKPIs(creances: "8 320,00 MAD", tauxEncaissement: "76.2%", valeurStock: "245 000,00 MAD", ruptures: "3")
    }

    func fetchPOSKPIs() async throws -> DashboardPOSKPIs {
        DashboardPOSKPIs(total: "12 450,00 MAD", especes: "8 700,00 MAD", carte: "3 750,00 MAD")
    }

    func fetchDailySales(days: Int) async throws -> [DailySalesData] {
        let calendar = Calendar.current
        return (0..<min(days, 30)).map { i in
            let date = calendar.date(byAdding: .day, value: -i, to: Date())!
            return DailySalesData(date: date.ISO8601Format().prefix(10).description, amount: Double.random(in: 200...3000))
        }.reversed()
    }

    func fetchCategoryDistribution() async throws -> [CategoryDistribution] {
        [
            CategoryDistribution(category: "Fournitures", percentage: 45, amount: 180_000),
            CategoryDistribution(category: "Informatique", percentage: 30, amount: 120_000),
            CategoryDistribution(category: "Papeterie", percentage: 15, amount: 60_000),
            CategoryDistribution(category: "Mobilier", percentage: 10, amount: 40_000),
        ]
    }

    func fetchTopSellingProducts(limit: Int) async throws -> [TopProductData] {
        [
            TopProductData(name: "Ramette A4", quantity: 230, revenue: 10_350),
            TopProductData(name: "Stylo Bleu", quantity: 180, revenue: 630),
            TopProductData(name: "Dossier Suspendu", quantity: 95, revenue: 1_140),
            TopProductData(name: "Clavier USB", quantity: 45, revenue: 5_400),
            TopProductData(name: "Souris Optique", quantity: 38, revenue: 2_470),
        ]
    }
}

// MARK: - KPI DTOs

struct SalesKPIDTO: Decodable {
    let ca_jour: Double?
    let nb_ventes_jour: Int?
    let ticket_moyen: Double?
    let marge_brute: Double?
}

struct ReceivablesKPIDTO: Decodable {
    let total_creances: Double?
    let taux_encaissement: Double?
}

struct DashboardKPIDTO: Decodable {
    let total_value: Double?
    let out_of_stock: Int?
}

struct SessionsSummaryDTO: Decodable {
    let total_sessions: Int?
    let closed_sessions: Int?
    let open_sessions: Int?
    let total_closing_cash: Double?
    let total_expected_cash: Double?
    let total_sales_period: Double?
    let nb_transactions_period: Int?
}

struct PaymentMethodsDTO: Decodable {
    struct MethodTotals: Decodable {
        struct MethodAmount: Decodable {
            let total: Double?
        }
        let cash: MethodAmount?
        let card: MethodAmount?
    }
    let methods: MethodTotals?
}

// MARK: - Chart DTOs

struct DailySalesDTO: Decodable {
    let date: String
    let ca: Double?
    let total: Double?
    let amount: Double?
}

struct CategoryDistribDTO: Decodable {
    let category: String?
    let name: String?
    let ca: Double?
    let percentage: Double?
    let percent: Double?
    let total: Double?
    let amount: Double?
}

struct TopProductDTO: Decodable {
    let name: String?
    let product_name: String?
    let ca: Double?
    let qty_vendue: Int?
    let quantity: Int?
    let total_qty: Int?
    let revenue: Double?
    let total_revenue: Double?
}
