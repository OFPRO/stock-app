import SwiftUI

struct LocationListView: View {
    @StateObject private var viewModel = LocationListViewModel()
    @State private var showCreateSheet = false
    @State private var warehouses: [WarehouseListItem] = []

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                StockTextField("Rechercher une zone...", text: $viewModel.searchQuery, variant: .search)
                    .padding()

                if !warehouses.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            FilterChip(title: "Tous", isSelected: viewModel.selectedWarehouseId == nil) {
                                viewModel.selectedWarehouseId = nil
                            }
                            ForEach(warehouses) { warehouse in
                                FilterChip(title: warehouse.name, isSelected: viewModel.selectedWarehouseId == warehouse.id) {
                                    viewModel.selectedWarehouseId = warehouse.id
                                }
                            }
                        }
                        .padding(.horizontal)
                        .padding(.bottom, 8)
                    }
                }

                switch viewModel.state {
                case .loading:
                    Spacer()
                    ProgressView()
                    Spacer()
                case .loaded(let locations):
                    if viewModel.filteredLocations.isEmpty {
                        Spacer()
                        ContentUnavailableView("Aucun résultat", systemImage: "magnifyingglass", description: Text("Essayez un autre terme de recherche"))
                        Spacer()
                    } else {
                        List {
                            ForEach(viewModel.filteredLocations) { location in
                                NavigationLink(destination: LocationDetailView(locationId: location.id)) {
                                    LocationRow(location: location)
                                }
                            }
                            .onDelete { indexSet in
                                Task { await viewModel.deleteLocation(at: indexSet) }
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
                    ContentUnavailableView("Aucune zone", systemImage: "square.grid.3x3", description: Text("Ajoutez une zone de stock pour commencer"))
                    Spacer()
                }
            }
            .background(AppColor.background)
            .navigationTitle("Zones de Stock")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button { showCreateSheet = true } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $showCreateSheet) {
                LocationFormView(mode: .create)
            }
            .task {
                await viewModel.load()
                warehouses = (try? await WarehouseService().fetchWarehouses()) ?? []
            }
        }
    }
}

struct LocationRow: View {
    let location: LocationListItem

    var body: some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 8)
                .fill(locationTypeColor(location.type).opacity(0.2))
                .frame(width: 40, height: 40)
                .overlay {
                    Image(systemName: locationTypeIcon(location.type))
                        .font(.caption)
                        .foregroundStyle(locationTypeColor(location.type))
                }

            VStack(alignment: .leading, spacing: 2) {
                Text(location.name)
                    .font(.body.weight(.medium))
                if !location.warehouseName.isEmpty {
                    Text(location.warehouseName)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            Spacer()

            if let capacity = location.capacity {
                Text("\(capacity) u.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            StockBadge(location.typeLabel, variant: .neutral)
        }
        .padding(.vertical, 4)
    }

    private func locationTypeColor(_ type: String) -> Color {
        switch type {
        case "rack": AppColor.brand
        case "shelf": AppColor.info
        case "zone": AppColor.accent
        default: .secondary
        }
    }

    private func locationTypeIcon(_ type: String) -> String {
        switch type {
        case "rack": "square.grid.3x3"
        case "shelf": "square.split.2x2"
        case "zone": "square.dashed"
        default: "square"
        }
    }
}

struct FilterChip: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.subheadline.weight(isSelected ? .semibold : .regular))
                .foregroundStyle(isSelected ? .white : .primary)
                .padding(.horizontal, 14)
                .padding(.vertical, 7)
                .background(isSelected ? AppColor.brand : AppColor.surface)
                .clipShape(Capsule())
                .overlay(
                    Capsule()
                        .stroke(isSelected ? Color.clear : AppColor.border, lineWidth: 0.5)
                )
        }
    }
}
