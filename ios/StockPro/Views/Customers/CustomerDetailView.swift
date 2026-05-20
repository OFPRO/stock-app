import SwiftUI

struct CustomerDetailView: View {
    @StateObject private var viewModel: CustomerDetailViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var showEditForm = false
    @State private var deleted = false

    init(customerId: Int) {
        _viewModel = StateObject(wrappedValue: CustomerDetailViewModel(customerId: customerId))
    }

    var body: some View {
        Group {
            if deleted {
                ContentUnavailableView("Client supprimé", systemImage: "person.slash", description: Text("Retour à la liste"))
            } else {
                content
            }
        }
        .navigationTitle("Client")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    Button { showEditForm = true } label: {
                        Label("Modifier", systemImage: "pencil")
                    }
                    Button(role: .destructive) { viewModel.showDeleteConfirmation = true } label: {
                        Label("Supprimer", systemImage: "trash")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                }
            }
        }
        .sheet(isPresented: $showEditForm) {
            if case .loaded(let customer) = viewModel.state {
                CustomerFormView(mode: .edit(customer))
            }
        }
        .alert("Supprimer le client ?", isPresented: $viewModel.showDeleteConfirmation) {
            Button("Annuler", role: .cancel) {}
            Button("Supprimer", role: .destructive) {
                Task {
                    if await viewModel.deleteCustomer() {
                        deleted = true
                    }
                }
            }
        } message: {
            Text("Cette action est irréversible.")
        }
        .task { await viewModel.load() }
    }

    @ViewBuilder
    private var content: some View {
        switch viewModel.state {
        case .loading:
            VStack(spacing: Spacing.md.rawValue) {
                StockSkeleton(variant: .card)
                StockSkeleton(variant: .card)
            }
            .padding()
        case .loaded(let customer):
            detailContent(customer)
        case .error(let error):
            StockErrorView(message: error.errorDescription ?? "Erreur", onRetry: {
                Task { await viewModel.load() }
            })
        case .empty:
            ContentUnavailableView("Client introuvable", systemImage: "person")
        }
    }

    private func detailContent(_ customer: CustomerDTO) -> some View {
        ScrollView {
            VStack(spacing: Spacing.md.rawValue) {
                StockCard {
                    VStack(alignment: .leading, spacing: Spacing.sm.rawValue) {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(customer.name)
                                    .font(.title3.weight(.bold))
                                Text(customer.client_code ?? "")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            StockBadge(customerTypeLabel(customer.type ?? ""),
                                       variant: customerTypeBadge(customer.type ?? ""))
                        }
                        if customer.is_loyal ?? false {
                            StockBadge("Client Loyal", variant: .success)
                        }
                    }
                }

                StockCard {
                    VStack(alignment: .leading, spacing: Spacing.sm.rawValue) {
                        Text("Coordonnées")
                            .font(.subheadline.weight(.semibold))
                        infoRow("Email", customer.email ?? "")
                        infoRow("Téléphone", customer.phone ?? "")
                        infoRow("Adresse", customer.address ?? "")
                    }
                }

                if let notes = customer.notes, !notes.isEmpty {
                    StockCard {
                        VStack(alignment: .leading, spacing: Spacing.sm.rawValue) {
                            Text("Notes")
                                .font(.subheadline.weight(.semibold))
                            Text(notes)
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
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

    private func customerTypeLabel(_ type: String) -> String {
        switch type {
        case "school": return "École"
        case "student": return "Étudiant"
        case "company": return "Société"
        default: return "Particulier"
        }
    }

    private func customerTypeBadge(_ type: String) -> StockBadge.Variant {
        switch type {
        case "school", "student": return .info
        case "company": return .neutral
        default: return .neutral
        }
    }
}
