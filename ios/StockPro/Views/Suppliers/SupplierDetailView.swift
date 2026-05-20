import SwiftUI

struct SupplierDetailView: View {
    let supplierId: Int
    @State private var supplier: SupplierListItem?
    @State private var isLoading = true
    @State private var showEditForm = false
    @State private var showDeleteConfirmation = false
    @State private var deleted = false

    @Environment(\.dismiss) private var dismiss
    private let supplierService: SupplierServiceProtocol = SupplierService()

    var body: some View {
        Group {
            if deleted {
                ContentUnavailableView("Fournisseur supprimé", systemImage: "truck", description: Text("Retour à la liste"))
            } else if isLoading {
                VStack(spacing: Spacing.md.rawValue) {
                    StockSkeleton(variant: .card)
                    StockSkeleton(variant: .card)
                }
                .padding()
            } else if let supplier {
                detailContent(supplier)
            } else {
                StockErrorView(message: "Fournisseur introuvable")
            }
        }
        .navigationTitle("Fournisseur")
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
            if let supplier {
                SupplierFormView(mode: .edit(supplier))
            }
        }
        .alert("Supprimer le fournisseur ?", isPresented: $showDeleteConfirmation) {
            Button("Annuler", role: .cancel) {}
            Button("Supprimer", role: .destructive) {
                Task {
                    try? await supplierService.deleteSupplier(id: supplierId)
                    deleted = true
                }
            }
        } message: {
            Text("Cette action est irréversible.")
        }
        .task {
            isLoading = true
            supplier = try? await supplierService.fetchSupplier(id: supplierId)
            isLoading = false
        }
    }

    private func detailContent(_ supplier: SupplierListItem) -> some View {
        ScrollView {
            VStack(spacing: Spacing.md.rawValue) {
                StockCard {
                    VStack(alignment: .leading, spacing: Spacing.sm.rawValue) {
                        Text(supplier.name)
                            .font(.title3.weight(.bold))
                        if !supplier.contactPerson.isEmpty {
                            Label(supplier.contactPerson, systemImage: "person")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                    }
                }

                StockCard {
                    VStack(alignment: .leading, spacing: Spacing.sm.rawValue) {
                        Text("Coordonnées")
                            .font(.subheadline.weight(.semibold))
                        infoRow("Email", supplier.email)
                        infoRow("Téléphone", supplier.phone)
                        infoRow("Adresse", supplier.address)
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
