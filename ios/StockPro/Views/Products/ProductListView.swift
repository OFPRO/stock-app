import SwiftUI

struct ProductListView: View {
    @StateObject private var viewModel = ProductListViewModel()
    @State private var showCreateSheet = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                StockTextField("Rechercher un produit...", text: $viewModel.searchQuery, variant: .search)
                    .padding()

                switch viewModel.state {
                case .loading:
                    Spacer()
                    ProgressView()
                    Spacer()
                case .loaded(let products):
                    if viewModel.filteredProducts.isEmpty {
                        Spacer()
                        ContentUnavailableView("Aucun résultat", systemImage: "magnifyingglass", description: Text("Essayez un autre terme de recherche"))
                        Spacer()
                    } else {
                        List {
                            ForEach(viewModel.filteredProducts) { product in
                                NavigationLink(destination: ProductDetailView(productId: product.id)) {
                                    ProductRow(product: product)
                                }
                            }
                            .onDelete { indexSet in
                                Task { await viewModel.deleteProducts(at: indexSet) }
                            }
                        }
                        .listStyle(.plain)
                        .refreshable {
                            await viewModel.load()
                        }
                    }
                case .error(let error):
                    Spacer()
                    StockErrorView(message: error.errorDescription ?? "Erreur", onRetry: {
                        Task { await viewModel.load() }
                    })
                    Spacer()
                case .empty:
                    Spacer()
                    ContentUnavailableView("Aucun produit", systemImage: "shippingbox", description: Text("Ajoutez un produit pour commencer"))
                    Spacer()
                }
            }
            .background(AppColor.background)
            .navigationTitle("Produits")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        showCreateSheet = true
                    } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $showCreateSheet) {
                ProductFormView(mode: .create)
            }
            .task { await viewModel.load() }
        }
    }
}

struct ProductRow: View {
    let product: ProductListItem

    var body: some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 8)
                .fill(AppColor.brandLight)
                .frame(width: 44, height: 44)
                .overlay {
                    Image(systemName: "shippingbox")
                        .foregroundStyle(.white)
                }

            VStack(alignment: .leading, spacing: 2) {
                Text(product.name)
                    .font(.body.weight(.medium))
                Text(product.sku)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 2) {
                Text(product.price)
                    .font(.subheadline.weight(.semibold))
                StockBadge("Stock: \(product.stock)", variant: product.stock > 5 ? .success : .warning)
            }
        }
        .padding(.vertical, 4)
    }
}

struct ProductListItem: Identifiable {
    let id: Int
    let name: String
    let sku: String
    let price: String
    let stock: Int
    let category: String
}
