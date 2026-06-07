import SwiftUI

struct ReportsView: View {
    @StateObject private var vm = ReportsViewModel()

    var body: some View {
        VStack(spacing: 0) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 0) {
                    ForEach(ReportTab.allCases, id: \.self) { tab in
                        Button {
                            vm.selectedTab = tab
                            Task { await vm.loadSelectedTab() }
                        } label: {
                            VStack(spacing: 4) {
                                Image(systemName: tab.icon)
                                    .font(.body)
                                Text(tab.label)
                                    .font(.caption2)
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .padding(.horizontal, 8)
                            .foregroundStyle(vm.selectedTab == tab ? Color.accentColor : .secondary)
                            .background(vm.selectedTab == tab ? Color.accentColor.opacity(0.1) : .clear)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .background(.regularMaterial)

            List {
                switch vm.selectedTab {
                case .overview:
                    overviewContent
                case .rotation:
                    rotationContent
                case .lowStock:
                    lowStockContent
                case .categories:
                    categoriesContent
                case .expiry:
                    expiryContent
                case .warehouses:
                    warehousesContent
                }
            }
            .listStyle(.insetGrouped)
        }
        .navigationTitle("Rapports")
        .task { await vm.loadOverview() }
        .refreshable { await vm.loadSelectedTab() }
    }

    @ViewBuilder
    private var overviewContent: some View {
        switch vm.overview {
        case .loading:
            ProgressView().frame(maxWidth: .infinity).listRowBackground(Color.clear)
        case .loaded(let data):
            Section {
                KPIReportRow(label: "Produits", value: "\(data.total_products)", icon: "shippingbox")
                KPIReportRow(label: "Valeur du stock", value: data.formattedValue, icon: "dollarsign")
                KPIReportRow(label: "Stock faible", value: "\(data.low_stock)", icon: "exclamationmark.triangle", valueColor: data.low_stock > 0 ? .orange : .green)
                KPIReportRow(label: "Ruptures", value: "\(data.out_of_stock)", icon: "xmark.circle", valueColor: data.out_of_stock > 0 ? .red : .green)
                KPIReportRow(label: "Expiration \u{2264} 30j", value: "\(data.expiring_soon)", icon: "calendar", valueColor: data.expiring_soon > 0 ? .orange : .green)
            }
        case .empty:
            ContentUnavailableView("Aucune donn\u{00e9}e", systemImage: "chart.bar")
        case .error(let err):
            StockErrorView(message: err.errorDescription ?? "Erreur", onRetry: { Task { await vm.loadOverview() } })
        default:
            Color.clear
        }
    }

    @ViewBuilder
    private var rotationContent: some View {
        switch vm.rotation {
        case .none:
            Color.clear.listRowBackground(Color.clear)
        case .loading:
            ProgressView().frame(maxWidth: .infinity).listRowBackground(Color.clear)
        case .loaded(let items):
            RotationSection(items: items)
        case .empty(let msg):
            ContentUnavailableView(msg, systemImage: "arrow.triangle.2.circlepath")
        case .error(let err):
            StockErrorView(message: err.errorDescription ?? "Erreur", onRetry: { Task { await vm.loadRotation() } })
        }
    }

    @ViewBuilder
    private var lowStockContent: some View {
        switch vm.lowStock {
        case .none:
            Color.clear.listRowBackground(Color.clear)
        case .loading:
            ProgressView().frame(maxWidth: .infinity).listRowBackground(Color.clear)
        case .loaded(let items):
            LowStockSection(items: items)
        case .empty(let msg):
            ContentUnavailableView(msg, systemImage: "checkmark.circle")
        case .error(let err):
            StockErrorView(message: err.errorDescription ?? "Erreur", onRetry: { Task { await vm.loadLowStock() } })
        }
    }

    @ViewBuilder
    private var categoriesContent: some View {
        switch vm.categories {
        case .none:
            Color.clear.listRowBackground(Color.clear)
        case .loading:
            ProgressView().frame(maxWidth: .infinity).listRowBackground(Color.clear)
        case .loaded(let items):
            CategorySection(items: items)
        case .empty(let msg):
            ContentUnavailableView(msg, systemImage: "square.grid.3x3")
        case .error(let err):
            StockErrorView(message: err.errorDescription ?? "Erreur", onRetry: { Task { await vm.loadCategories() } })
        }
    }

    @ViewBuilder
    private var expiryContent: some View {
        switch vm.expiry {
        case .none:
            Color.clear.listRowBackground(Color.clear)
        case .loading:
            ProgressView().frame(maxWidth: .infinity).listRowBackground(Color.clear)
        case .loaded(let items):
            ExpirySection(items: items)
        case .empty(let msg):
            ContentUnavailableView(msg, systemImage: "calendar")
        case .error(let err):
            StockErrorView(message: err.errorDescription ?? "Erreur", onRetry: { Task { await vm.loadExpiry() } })
        }
    }

    @ViewBuilder
    private var warehousesContent: some View {
        switch vm.warehouses {
        case .none:
            Color.clear.listRowBackground(Color.clear)
        case .loading:
            ProgressView().frame(maxWidth: .infinity).listRowBackground(Color.clear)
        case .loaded(let items):
            WarehouseSection(items: items)
        case .empty(let msg):
            ContentUnavailableView(msg, systemImage: "building.2")
        case .error(let err):
            StockErrorView(message: err.errorDescription ?? "Erreur", onRetry: { Task { await vm.loadWarehouses() } })
        }
    }
}

struct RotationSection: View {
    let items: [RotationReportItem]
    var body: some View {
        Section {
            ForEach(items) { item in
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(item.name).font(.subheadline.weight(.medium))
                        Text("Stock: \(item.quantity) min: \(item.min_quantity)")
                            .font(.caption).foregroundStyle(.secondary)
                    }
                    Spacer()
                    Text("\(item.movements) mvt")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.secondary)
                }
                .padding(.vertical, 4)
            }
        } header: {
            Text("Top Rotation")
        }
    }
}

struct LowStockSection: View {
    let items: [LowStockReportItem]
    var body: some View {
        Section {
            ForEach(items) { item in
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(item.name).font(.subheadline.weight(.medium))
                        Text("Stock: \(item.quantity) / Min: \(item.min_quantity)")
                            .font(.caption).foregroundStyle(.secondary)
                    }
                    Spacer()
                    Text("\(item.needed) manquants")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.red)
                }
                .padding(.vertical, 4)
            }
        } header: {
            Text("Produits sous seuil")
        }
    }
}

struct CategorySection: View {
    let items: [CategoryReportItem]
    var body: some View {
        Section {
            ForEach(items) { item in
                HStack {
                    Text(item.category).font(.subheadline.weight(.medium))
                    Spacer()
                    Text("\(item.count) produits")
                        .font(.caption).foregroundStyle(.secondary)
                    if let value = item.value {
                        Text(String(format: "%.0f MAD", value))
                            .font(.caption.weight(.semibold))
                            .frame(width: 80, alignment: .trailing)
                    }
                }
                .padding(.vertical, 4)
            }
        } header: {
            Text("R\u{00e9}partition par cat\u{00e9}gorie")
        }
    }
}

struct ExpirySection: View {
    let items: [ExpiryReportItem]
    var body: some View {
        Section {
            ForEach(items) { item in
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(item.name).font(.subheadline.weight(.medium))
                        if let lot = item.lot_number {
                            Text("Lot: \(lot)").font(.caption).foregroundStyle(.secondary)
                        }
                    }
                    Spacer()
                    VStack(alignment: .trailing, spacing: 2) {
                        if let expiry = item.expiry_date {
                            Text(expiry).font(.caption).foregroundStyle(.orange)
                        }
                        Text("Qt\u{00e9}: \(item.quantity)").font(.caption2).foregroundStyle(.secondary)
                    }
                }
                .padding(.vertical, 4)
            }
        } header: {
            Text("Produits proches de l'expiration")
        }
    }
}

struct WarehouseSection: View {
    let items: [WarehouseReportItem]
    var body: some View {
        Section {
            ForEach(items) { item in
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.name).font(.subheadline.weight(.medium))
                    if let address = item.address {
                        Text(address).font(.caption).foregroundStyle(.secondary)
                    }
                    HStack(spacing: 16) {
                        Text("\(item.product_count) produits")
                            .font(.caption2).foregroundStyle(.tertiary)
                        Text("\(item.total_quantity) unit\u{00e9}s")
                            .font(.caption2).foregroundStyle(.tertiary)
                        Text(String(format: "%.0f MAD", item.total_value))
                            .font(.caption2.weight(.semibold)).foregroundStyle(.secondary)
                    }
                }
                .padding(.vertical, 4)
            }
        } header: {
            Text("Entrep\u{00f4}ts")
        }
    }
}

struct KPIReportRow: View {
    let label: String
    let value: String
    let icon: String
    var valueColor: Color = .primary

    var body: some View {
        HStack {
            Image(systemName: icon)
                .font(.body)
                .foregroundStyle(.secondary)
                .frame(width: 24)
            Text(label)
                .font(.subheadline)
            Spacer()
            Text(value)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(valueColor)
        }
        .padding(.vertical, 4)
    }
}
