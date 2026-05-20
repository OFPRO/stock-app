import SwiftUI

struct CustomerListView: View {
    @StateObject private var viewModel = CustomerListViewModel()
    @State private var showCreateSheet = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                StockTextField("Rechercher un client...", text: $viewModel.searchQuery, variant: .search)
                    .padding()

                switch viewModel.state {
                case .loading:
                    Spacer()
                    ProgressView()
                    Spacer()
                case .loaded(let customers):
                    if viewModel.filteredCustomers.isEmpty {
                        Spacer()
                        ContentUnavailableView("Aucun résultat", systemImage: "magnifyingglass", description: Text("Essayez un autre terme de recherche"))
                        Spacer()
                    } else {
                        List {
                            ForEach(viewModel.filteredCustomers) { customer in
                                NavigationLink(destination: CustomerDetailView(customerId: customer.id)) {
                                    CustomerRow(customer: customer)
                                }
                            }
                            .onDelete { indexSet in
                                Task { await viewModel.deleteCustomer(at: indexSet) }
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
                    ContentUnavailableView("Aucun client", systemImage: "person.2", description: Text("Ajoutez un client pour commencer"))
                    Spacer()
                }
            }
            .background(AppColor.background)
            .navigationTitle("Clients")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button { showCreateSheet = true } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $showCreateSheet) {
                CustomerFormView(mode: .create)
            }
            .task { await viewModel.load() }
        }
    }
}

struct CustomerRow: View {
    let customer: CustomerListItem

    var body: some View {
        HStack(spacing: 12) {
            Circle()
                .fill(customerTypeColor(customer.type))
                .frame(width: 40, height: 40)
                .overlay {
                    Image(systemName: customerTypeIcon(customer.type))
                        .font(.caption)
                        .foregroundStyle(.white)
                }

            VStack(alignment: .leading, spacing: 2) {
                Text(customer.name)
                    .font(.body.weight(.medium))
                Text(customer.clientCode)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            if customer.isLoyal {
                StockBadge("Loyal", variant: .success)
            }
        }
        .padding(.vertical, 4)
    }

    private func customerTypeColor(_ type: String) -> Color {
        switch type {
        case "school": AppColor.brand
        case "student": AppColor.info
        case "company": AppColor.accent
        default: .secondary
        }
    }

    private func customerTypeIcon(_ type: String) -> String {
        switch type {
        case "school": "building.columns"
        case "student": "graduationcap"
        case "company": "building"
        default: "person"
        }
    }
}
