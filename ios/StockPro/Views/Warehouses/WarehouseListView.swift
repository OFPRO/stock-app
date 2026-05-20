import SwiftUI

struct WarehouseListView: View {
    @StateObject private var viewModel = WarehouseListViewModel()
    @State private var showCreateSheet = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                StockTextField("Rechercher un entrepôt...", text: $viewModel.searchQuery, variant: .search)
                    .padding()

                switch viewModel.state {
                case .loading:
                    Spacer()
                    ProgressView()
                    Spacer()
                case .loaded(let warehouses):
                    if viewModel.filteredWarehouses.isEmpty {
                        Spacer()
                        ContentUnavailableView("Aucun résultat", systemImage: "magnifyingglass", description: Text("Essayez un autre terme de recherche"))
                        Spacer()
                    } else {
                        List {
                            ForEach(viewModel.filteredWarehouses) { warehouse in
                                NavigationLink(destination: WarehouseDetailView(warehouseId: warehouse.id)) {
                                    WarehouseRow(warehouse: warehouse)
                                }
                            }
                        }
                        .listStyle(.plain)
                        .refreshable { await viewModel.load() }
                    }
                case .error(let error):
                    Spacer()
                    StockErrorView(message: error.errorDescription ?? "Erreur", onRetry: {
                        Task { await viewModel.load() }
                    })
                    Spacer()
                case .empty:
                    Spacer()
                    ContentUnavailableView("Aucun entrepôt", systemImage: "building.2", description: Text("Ajoutez un entrepôt pour commencer"))
                    Spacer()
                }
            }
            .background(AppColor.background)
            .navigationTitle("Entrepôts")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button { showCreateSheet = true } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $showCreateSheet) {
                WarehouseFormView(mode: .create)
            }
            .task { await viewModel.load() }
        }
    }
}

struct WarehouseRow: View {
    let warehouse: WarehouseListItem

    var body: some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 8)
                .fill(AppColor.brand.opacity(0.15))
                .frame(width: 40, height: 40)
                .overlay {
                    Image(systemName: "building.2")
                        .font(.caption)
                        .foregroundStyle(AppColor.brand)
                }

            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Text(warehouse.name)
                        .font(.body.weight(.medium))
                    if warehouse.isDefault {
                        StockBadge("Défaut", variant: .info)
                    }
                }
                if !warehouse.manager.isEmpty {
                    Text(warehouse.manager)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            Spacer()

            if !warehouse.phone.isEmpty {
                Text(warehouse.phone)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}
