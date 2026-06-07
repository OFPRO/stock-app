import Foundation

@MainActor
final class ProductDetailViewModel: ObservableObject {
    @Published var state: ViewState<ProductDetail> = .loading
    @Published var stockMovements: [StockMovementItem] = []
    @Published var priceTiers: [PriceTierDTO] = []
    @Published var showDeleteConfirmation = false
    @Published var isDeleting = false

    private let productService: ProductServiceProtocol
    private let stockService: StockServiceProtocol
    let productId: Int

    init(productId: Int, productService: ProductServiceProtocol = ProductService(), stockService: StockServiceProtocol = StockService()) {
        self.productId = productId
        self.productService = productService
        self.stockService = stockService
    }

    func load() async {
        state = .loading
        do {
            async let product = productService.fetchProduct(id: productId)
            async let movements = stockService.fetchMovements(productId: productId, warehouseId: nil)
            let (loadedProduct, loadedMovements) = try await (product, movements)
            stockMovements = loadedMovements
            state = .loaded(loadedProduct)
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
