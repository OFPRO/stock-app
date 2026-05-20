import SwiftUI

struct WarehouseDetailView: View {
    let warehouseId: Int
    @State private var warehouse: WarehouseListItem?
    @State private var isLoading = true
    @State private var deleted = false

    @Environment(\.dismiss) private var dismiss
    private let warehouseService: WarehouseServiceProtocol = WarehouseService()

    var body: some View {
        Group {
            if deleted {
                ContentUnavailableView("Entrepôt supprimé", systemImage: "building.2.slash", description: Text("Retour à la liste"))
            } else if isLoading {
                VStack(spacing: Spacing.md.rawValue) {
                    StockSkeleton(variant: .card)
                    StockSkeleton(variant: .card)
                }
                .padding()
            } else if let warehouse {
                detailContent(warehouse)
            } else {
                StockErrorView(message: "Entrepôt introuvable")
            }
        }
        .navigationTitle("Entrepôt")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            isLoading = true
            warehouse = try? await warehouseService.fetchWarehouse(id: warehouseId)
            isLoading = false
        }
    }

    private func detailContent(_ warehouse: WarehouseListItem) -> some View {
        ScrollView {
            VStack(spacing: Spacing.md.rawValue) {
                StockCard {
                    VStack(alignment: .leading, spacing: Spacing.sm.rawValue) {
                        HStack {
                            Text(warehouse.name)
                                .font(.title3.weight(.bold))
                            Spacer()
                            if warehouse.isDefault {
                                StockBadge("Défaut", variant: .info)
                            }
                        }
                        if !warehouse.manager.isEmpty {
                            Label(warehouse.manager, systemImage: "person")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                    }
                }

                StockCard {
                    VStack(alignment: .leading, spacing: Spacing.sm.rawValue) {
                        Text("Coordonnées")
                            .font(.subheadline.weight(.semibold))
                        infoRow("Adresse", warehouse.address)
                        infoRow("Téléphone", warehouse.phone)
                    }
                }
            }
            .padding()
        }
        .background(AppColor.background)
    }

    private func infoRow(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label)
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Spacer()
            Text(value.isEmpty ? "—" : value)
                .font(.subheadline)
        }
    }
}
