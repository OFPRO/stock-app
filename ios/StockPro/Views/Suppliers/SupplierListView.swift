import SwiftUI

struct SupplierListView: View {
    @StateObject private var viewModel = SupplierListViewModel()
    @State private var showCreateSheet = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                StockTextField("Rechercher un fournisseur...", text: $viewModel.searchQuery, variant: .search)
                    .padding()

                switch viewModel.state {
                case .loading:
                    Spacer()
                    ProgressView()
                    Spacer()
                case .loaded(let suppliers):
                    if viewModel.filteredSuppliers.isEmpty {
                        Spacer()
                        ContentUnavailableView("Aucun résultat", systemImage: "magnifyingglass", description: Text("Essayez un autre terme de recherche"))
                        Spacer()
                    } else {
                        List {
                            ForEach(viewModel.filteredSuppliers) { supplier in
                                NavigationLink(destination: SupplierDetailView(supplierId: supplier.id)) {
                                    SupplierRow(supplier: supplier)
                                }
                            }
                            .onDelete { indexSet in
                                Task { await viewModel.deleteSupplier(at: indexSet) }
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
                    ContentUnavailableView("Aucun fournisseur", systemImage: "truck", description: Text("Ajoutez un fournisseur"))
                    Spacer()
                }
            }
            .background(AppColor.background)
            .navigationTitle("Fournisseurs")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button { showCreateSheet = true } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $showCreateSheet) {
                SupplierFormView(mode: .create)
            }
            .task { await viewModel.load() }
        }
    }
}

struct SupplierRow: View {
    let supplier: SupplierListItem

    var body: some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 8)
                .fill(AppColor.accent.opacity(0.2))
                .frame(width: 40, height: 40)
                .overlay {
                    Image(systemName: "truck")
                        .font(.caption)
                        .foregroundStyle(AppColor.accent)
                }

            VStack(alignment: .leading, spacing: 2) {
                Text(supplier.name)
                    .font(.body.weight(.medium))
                if !supplier.contactPerson.isEmpty {
                    Text(supplier.contactPerson)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            Spacer()

            if !supplier.phone.isEmpty {
                Text(supplier.phone)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}
