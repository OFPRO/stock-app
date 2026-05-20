import SwiftUI

struct LocationDetailView: View {
    let locationId: Int
    @State private var location: LocationListItem?
    @State private var isLoading = true
    @State private var showEditForm = false
    @State private var showDeleteConfirmation = false
    @State private var deleted = false

    @Environment(\.dismiss) private var dismiss
    private let locationService: LocationServiceProtocol = LocationService()

    var body: some View {
        Group {
            if deleted {
                ContentUnavailableView("Zone supprimée", systemImage: "square.slash", description: Text("Retour à la liste"))
            } else if isLoading {
                VStack(spacing: Spacing.md.rawValue) {
                    StockSkeleton(variant: .card)
                    StockSkeleton(variant: .card)
                }
                .padding()
            } else if let location {
                detailContent(location)
            } else {
                StockErrorView(message: "Zone introuvable")
            }
        }
        .navigationTitle("Zone")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    Button { showEditForm = true } label: {
                        Label("Modifier", systemImage: "pencil")
                    }
                    Button(role: .destructive) { showDeleteConfirmation = true } label: {
                        Label("Supprimer", systemImage: "trash")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                }
            }
        }
        .sheet(isPresented: $showEditForm) {
            if let location {
                LocationFormView(mode: .edit(location))
            }
        }
        .alert("Supprimer cette zone ?", isPresented: $showDeleteConfirmation) {
            Button("Annuler", role: .cancel) {}
            Button("Supprimer", role: .destructive) {
                Task {
                    try? await locationService.deleteLocation(id: locationId)
                    deleted = true
                }
            }
        } message: {
            Text("Cette action est irréversible.")
        }
        .task {
            isLoading = true
            location = try? await locationService.fetchLocation(id: locationId)
            isLoading = false
        }
    }

    private func detailContent(_ location: LocationListItem) -> some View {
        ScrollView {
            VStack(spacing: Spacing.md.rawValue) {
                StockCard {
                    VStack(alignment: .leading, spacing: Spacing.sm.rawValue) {
                        HStack {
                            Text(location.name)
                                .font(.title3.weight(.bold))
                            Spacer()
                            StockBadge(location.typeLabel, variant: .neutral)
                        }
                        if !location.warehouseName.isEmpty {
                            Label(location.warehouseName, systemImage: "building.2")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                    }
                }

                StockCard {
                    VStack(alignment: .leading, spacing: Spacing.sm.rawValue) {
                        Text("Capacité")
                            .font(.subheadline.weight(.semibold))
                        if let capacity = location.capacity {
                            infoRow("Capacité max", "\(capacity) unités")
                        } else {
                            infoRow("Capacité max", "Non définie")
                        }
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
            Text(value)
                .font(.subheadline)
        }
    }
}
