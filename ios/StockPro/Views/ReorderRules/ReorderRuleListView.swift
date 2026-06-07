import SwiftUI

struct ReorderRuleListView: View {
    @StateObject private var vm = ReorderRuleListViewModel()
    @State private var showForm = false
    @State private var showReplenishment = false

    var body: some View {
        List {
            switch vm.state {
            case .loading:
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .listRowBackground(Color.clear)
            case .loaded:
                Section {
                    Toggle("Actives uniquement", isOn: $vm.filterActive)
                        .onChange(of: vm.filterActive) { _ in Task { await vm.load() } }
                }
                ForEach(vm.filteredRules) { rule in
                    ReorderRuleRow(rule: rule)
                }
                .onDelete { indexSet in
                    if case .loaded = vm.state {
                        Task { await vm.deleteRule(at: indexSet) }
                    }
                }
            case .empty:
                ContentUnavailableView(
                    "Aucune règle", systemImage: "slider.horizontal.3",
                    description: Text("Ajoutez une règle de réapprovisionnement")
                )
            case .error(let err):
                StockErrorView(message: err.errorDescription ?? "Erreur", onRetry: { Task { await vm.load() } })
            default:
                Color.clear
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Règles de Réappro")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                HStack {
                    Button("Réappro.") { showReplenishment = true }
                        .font(.subheadline)
                    Button("", systemImage: "plus") { showForm = true }
                }
            }
        }
        .task { await vm.load() }
        .refreshable { await vm.refresh() }
        .sheet(isPresented: $showForm) { ReorderRuleFormView { Task { await vm.load() } } }
        .sheet(isPresented: $showReplenishment) { ReplenishmentListView(suggestions: vm.replenishmentSuggestions) }
    }
}

struct ReorderRuleRow: View {
    let rule: ReorderRuleDTO

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(rule.product_name ?? "Produit #\(rule.product_id)")
                    .font(.subheadline.weight(.medium))
                Spacer()
                if rule.is_active {
                    Text("Actif").font(.caption2).foregroundStyle(.green)
                } else {
                    Text("Inactif").font(.caption2).foregroundStyle(.secondary)
                }
            }
            HStack(spacing: 12) {
                Label("Min: \(rule.min_quantity)", systemImage: "arrow.down")
                Label("Max: \(rule.max_quantity)", systemImage: "arrow.up")
            }
            .font(.caption)
            .foregroundStyle(.secondary)
            if let supplier = rule.supplier_name {
                Text(supplier).font(.caption2).foregroundStyle(.tertiary)
            }
            if let warehouse = rule.warehouse_name {
                Text(warehouse).font(.caption2).foregroundStyle(.tertiary)
            }
        }
        .padding(.vertical, 4)
    }
}

struct ReplenishmentListView: View {
    let suggestions: [ReplenishmentSuggestion]

    var body: some View {
        NavigationStack {
            List(suggestions) { suggestion in
                VStack(alignment: .leading, spacing: 4) {
                    Text(suggestion.product_name).font(.subheadline.weight(.medium))
                    HStack {
                        Label("Stock: \(suggestion.current_stock)", systemImage: "cube.box")
                        Label("Suggéré: \(suggestion.suggested_quantity)", systemImage: "plus.circle")
                    }
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    if let supplier = suggestion.supplier_name {
                        Text(supplier).font(.caption2).foregroundStyle(.tertiary)
                    }
                }
                .padding(.vertical, 4)
            }
            .navigationTitle("Suggestions")
        }
    }
}
