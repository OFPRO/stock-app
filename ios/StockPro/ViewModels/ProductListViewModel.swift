import Foundation

@MainActor
final class ProductListViewModel: ObservableObject {
    @Published var state: ViewState<[ProductListItem]> = .loading
    @Published var searchQuery: String = ""

    private let productService: ProductServiceProtocol

    init(productService: ProductServiceProtocol = ProductService()) {
        self.productService = productService
    }

    var filteredProducts: [ProductListItem] {
        guard case .loaded(let products) = state else { return [] }
        if searchQuery.isEmpty { return products }
        return products.filter {
            $0.name.localizedCaseInsensitiveContains(searchQuery) ||
            $0.sku.localizedCaseInsensitiveContains(searchQuery)
        }
    }

    func load() async {
        state = .loading
        do {
            let products = try await productService.fetchProducts()
            state = products.isEmpty ? .empty("") : .loaded(products)
        } catch {
            state = .error(.from(error))
        }
    }

    func deleteProducts(at indexSet: IndexSet) async {
        guard case .loaded(let products) = state else { return }
        let toDelete = indexSet.map { products[$0] }
        for product in toDelete {
            try? await productService.deleteProduct(id: product.id)
        }
        await load()
    }
}
