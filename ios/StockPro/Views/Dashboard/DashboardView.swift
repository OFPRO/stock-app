import SwiftUI
import Charts

struct DashboardView: View {
    @StateObject private var viewModel = DashboardViewModel()

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: Spacing.md.rawValue) {
                    switch viewModel.state {
                    case .loading:
                        ForEach(0..<6) { _ in
                            StockSkeleton(variant: .card)
                        }
                    case .loaded(let dashboard):
                        headerKPIsSection(dashboard.headerKPIs)
                        financialSection(dashboard.financialKPIs)
                        posSection(dashboard.posKPIs)
                        Divider()
                        salesEvolutionChart
                        categoriesChart
                        topProductsChart
                        Divider()
                        quickActionsSection
                    case .error(let error):
                        StockErrorView(message: error.errorDescription ?? "Erreur", onRetry: {
                            Task { await viewModel.refresh() }
                        })
                    case .empty:
                        EmptyPlaceholderView(title: "Aucune donnée")
                    }
                }
                .padding()
            }
            .background(AppColor.background)
            .navigationTitle("Tableau de Bord")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        Task { await viewModel.refresh() }
                    } label: {
                        Image(systemName: "arrow.clockwise")
                    }
                }
            }
            .task { await viewModel.refresh() }
        }
    }

    private func headerKPIsSection(_ kpis: DashboardHeaderKPIs) -> some View {
        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: Spacing.sm.rawValue) {
            StockKPICard(title: "CA Aujourd'hui", value: kpis.caJour, icon: "eurosign", color: .green)
            StockKPICard(title: "Ventes", value: kpis.nbVentes, icon: "cart", color: .blue)
            StockKPICard(title: "Ticket Moyen", value: kpis.ticketMoyen, icon: "receipt", color: .orange)
            StockKPICard(title: "Marge Brute", value: kpis.margeBrute, icon: "percent", color: .purple)
        }
    }

    private func financialSection(_ kpis: DashboardFinancialKPIs) -> some View {
        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: Spacing.sm.rawValue) {
            StockKPICard(title: "Créances", value: kpis.creances, icon: "creditcard", color: .red)
            StockKPICard(title: "Taux Encaissement", value: kpis.tauxEncaissement, icon: "checkmark.circle", color: .green)
            StockKPICard(title: "Valeur Stock", value: kpis.valeurStock, icon: "shippingbox", color: AppColor.brand)
            StockKPICard(title: "Ruptures", value: kpis.ruptures, icon: "exclamationmark.triangle", color: AppColor.error)
        }
    }

    private func posSection(_ kpis: DashboardPOSKPIs) -> some View {
        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: Spacing.sm.rawValue) {
            StockKPICard(title: "Total Encaissé", value: kpis.total, icon: "eurosign.circle", color: .green)
            StockKPICard(title: "Espèces", value: kpis.especes, icon: "banknote", color: .brown)
            StockKPICard(title: "Carte", value: kpis.carte, icon: "creditcard.fill", color: .blue)
        }
    }

    private var salesEvolutionChart: some View {
        StockCard {
            VStack(alignment: .leading, spacing: Spacing.sm.rawValue) {
                Text("Évolution des ventes (30 jours)")
                    .font(.subheadline.weight(.semibold))
                if viewModel.dailySales.isEmpty {
                    Text("Aucune donnée")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                } else {
                    Chart(viewModel.dailySales) { day in
                        LineMark(
                            x: .value("Date", day.date),
                            y: .value("CA", day.amount)
                        )
                        .foregroundStyle(AppColor.brand)
                        AreaMark(
                            x: .value("Date", day.date),
                            y: .value("CA", day.amount)
                        )
                        .foregroundStyle(
                            LinearGradient(colors: [AppColor.brand.opacity(0.3), .clear],
                                         startPoint: .top, endPoint: .bottom)
                        )
                    }
                    .chartXAxis {
                        AxisMarks(values: .stride(by: 7)) { _ in
                            AxisValueLabel(format: .dateTime.day().month())
                        }
                    }
                    .frame(height: 200)
                }
            }
        }
    }

    private var categoriesChart: some View {
        StockCard {
            VStack(alignment: .leading, spacing: Spacing.sm.rawValue) {
                Text("Répartition par catégorie")
                    .font(.subheadline.weight(.semibold))
                if viewModel.categoryDistrib.isEmpty {
                    Text("Aucune donnée")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                } else {
                    Chart(viewModel.categoryDistrib) { cat in
                        SectorMark(
                            angle: .value("Montant", cat.amount),
                            innerRadius: .ratio(0.6),
                            angularInset: 2
                        )
                        .foregroundStyle(by: .value("Catégorie", cat.category))
                    }
                    .frame(height: 200)
                    .chartLegend(position: .bottom, spacing: 8)
                }
            }
        }
    }

    private var topProductsChart: some View {
        StockCard {
            VStack(alignment: .leading, spacing: Spacing.sm.rawValue) {
                Text("Top ventes")
                    .font(.subheadline.weight(.semibold))
                if viewModel.topProducts.isEmpty {
                    Text("Aucune donnée")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                } else {
                    Chart(viewModel.topProducts.prefix(5)) { product in
                        BarMark(
                            x: .value("Quantité", product.quantity),
                            y: .value("Produit", product.name)
                        )
                        .foregroundStyle(AppColor.accent)
                    }
                    .chartXAxisLabel("Quantité")
                    .frame(height: 200)
                }
            }
        }
    }

    private var quickActionsSection: some View {
        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: Spacing.sm.rawValue) {
            NavigationLink(destination: ProductListView()) {
                StockCard(isPressable: true) {
                    Label("Nouveau produit", systemImage: "plus.square")
                        .font(.subheadline)
                }
            }
            .buttonStyle(.plain)
            NavigationLink(destination: EmptyPlaceholderView(title: "Nouvelle commande")) {
                StockCard(isPressable: true) {
                    Label("Nouvelle commande", systemImage: "doc.badge.plus")
                        .font(.subheadline)
                }
            }
            .buttonStyle(.plain)
        }
    }
}
