import SwiftUI

struct StockMovementListView: View {
    @StateObject private var viewModel = StockMovementListViewModel()

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                filtersSection

                if viewModel.isLoading && viewModel.movements.isEmpty {
                    Spacer()
                    ProgressView()
                    Spacer()
                } else if let error = viewModel.errorMessage {
                    Spacer()
                    StockErrorView(message: error, onRetry: { Task { await viewModel.load() } })
                    Spacer()
                } else if viewModel.filteredMovements.isEmpty {
                    Spacer()
                    ContentUnavailableView(
                        "Aucun mouvement",
                        systemImage: "arrow.triangle.swap",
                        description: Text(viewModel.movements.isEmpty ? "Aucun mouvement enregistré" : "Essayez d'autres filtres")
                    )
                    Spacer()
                } else {
                    List {
                        ForEach(viewModel.filteredMovements) { movement in
                            MovementRow(movement: movement)
                        }
                    }
                    .listStyle(.plain)
                    .refreshable { await viewModel.load() }
                }
            }
            .background(AppColor.background)
            .navigationTitle("Mouvements")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button { viewModel.showCreateSheet = true } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $viewModel.showCreateSheet) {
                StockMovementFormView(onCreated: { Task { await viewModel.load() } })
            }
            .task { await viewModel.load() }
        }
    }

    private var filtersSection: some View {
        VStack(spacing: 8) {
            StockTextField("Rechercher un produit...", text: $viewModel.filterProduct, variant: .search)
                .padding(.horizontal)
                .padding(.top, 8)

            if !viewModel.warehouses.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        FilterChip(title: "Tous", isSelected: viewModel.filterWarehouse == nil) {
                            viewModel.filterWarehouse = nil
                            Task { await viewModel.load() }
                        }
                        ForEach(viewModel.warehouses) { warehouse in
                            FilterChip(title: warehouse.name, isSelected: viewModel.filterWarehouse == warehouse.id) {
                                viewModel.filterWarehouse = warehouse.id
                                Task { await viewModel.load() }
                            }
                        }
                    }
                    .padding(.horizontal)
                }
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    FilterChip(title: "Tous", isSelected: viewModel.filterType == nil) {
                        viewModel.filterType = nil
                    }
                    FilterChip(title: "Entrées", isSelected: viewModel.filterType == "in") {
                        viewModel.filterType = "in"
                    }
                    FilterChip(title: "Sorties", isSelected: viewModel.filterType == "out") {
                        viewModel.filterType = "out"
                    }
                    FilterChip(title: "Ventes", isSelected: viewModel.filterType == "sale") {
                        viewModel.filterType = "sale"
                    }
                    FilterChip(title: "Transferts", isSelected: viewModel.filterType == "transfer") {
                        viewModel.filterType = "transfer"
                    }
                    FilterChip(title: "Inter-Entrepôt", isSelected: viewModel.filterType == "inter_warehouse") {
                        viewModel.filterType = "inter_warehouse"
                    }
                }
                .padding(.horizontal)
                .padding(.bottom, 8)
            }
        }
    }
}

struct MovementRow: View {
    let movement: StockMovementItem

    var body: some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 8)
                .fill(movement.typeColor.opacity(0.15))
                .frame(width: 40, height: 40)
                .overlay {
                    Image(systemName: movement.typeIcon)
                        .font(.caption)
                        .foregroundStyle(movement.typeColor)
                }

            VStack(alignment: .leading, spacing: 2) {
                Text(movement.productName)
                    .font(.subheadline.weight(.medium))
                    .lineLimit(1)

                HStack(spacing: 4) {
                    StockBadge(movement.typeLabel, variant: .neutral)
                    Text(movement.locationSummary)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 2) {
                Text(movement.displayQuantity)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(movement.quantityColor)

                Text(movement.createdAt)
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
            }
        }
        .padding(.vertical, 4)
    }
}
