import Foundation

@MainActor
final class ProductDetailViewModel: ObservableObject {
    @Published var state: ViewState<ProductDetail> = .loading
    @Published var stockMovements: [StockMovementDTO] = []
    @Published var priceTiers: [PriceTierDTO] = []
    @Published var showDeleteConfirmation = false
    @Published var isDeleting = false

    private let productService: ProductServiceProtocol
    let productId: Int

    init(productId: Int, productService: ProductServiceProtocol = ProductService()) {
        self.productId = productId
        self.productService = productService
    }

    func load() async {
        state = .loading
        do {
            let product = try await productService.fetchProduct(id: productId)
            state = .loaded(product)
        } catch {
            state = .error(.from(error))
        }
    }

    func deleteProduct() async -> Bool {
        isDeleting = true
        defer { isDeleting = false }
        do {
            try await productService.deleteProduct(id: productId)
            return true
        } catch {
            state = .error(.from(error))
            return false
        }
    }
}
