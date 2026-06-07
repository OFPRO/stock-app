import Foundation

final class OrderCreateViewModel: ObservableObject {
    private let orderService: OrderServiceProtocol
    private let supplierService: SupplierServiceProtocol
    private let warehouseService: WarehouseServiceProtocol
    private let productService: ProductServiceProtocol

    @Published var suppliers: [SupplierListItem] = []
    @Published var warehouses: [WarehouseListItem] = []
    @Published var products: [ForSaleProductDTO] = []
    @Published var selectedSupplier: SupplierListItem?
    @Published var selectedWarehouse: WarehouseListItem?
    @Published var lineItems: [OrderLineItem] = []
    @Published var notes: String = ""
    @Published var isLoading = false
    @Published var isSaving = false
    @Published var errorMessage: String?
    @Published var successMessage: String?

    var total: Double {
        lineItems.reduce(0) { $0 + $1.lineTotal }
    }

    var isValid: Bool {
        selectedSupplier != nil && selectedWarehouse != nil && !lineItems.isEmpty
    }

    init(orderService: OrderServiceProtocol = OrderService(),
         supplierService: SupplierServiceProtocol = SupplierService(),
         warehouseService: WarehouseServiceProtocol = WarehouseService(),
         productService: ProductServiceProtocol = ProductService()) {
        self.orderService = orderService
        self.supplierService = supplierService
        self.warehouseService = warehouseService
        self.productService = productService
    }

    @MainActor
    func loadInitialData() async {
        isLoading = true
        defer { isLoading = false }
        do {
            async let s = supplierService.fetchSuppliers()
            async let w = warehouseService.fetchWarehouses()
            async let p = productService.fetchProductsForSale()
            (suppliers, warehouses, products) = try await (s, w, p)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func addItem(_ product: ForSaleProductDTO) {
        guard !lineItems.contains(where: { $0.productId == product.id }) else { return }
        lineItems.append(OrderLineItem(
            productId: product.id,
            productName: product.name,
            quantity: 1,
            unitPrice: product.price
        ))
    }

    func removeItem(at index: Int) {
        lineItems.remove(at: index)
    }

    @MainActor
    func submit() async {
        guard let supplier = selectedSupplier else {
            errorMessage = "Sélectionnez un fournisseur"
            return
        }
        guard let warehouse = selectedWarehouse else {
            errorMessage = "Sélectionnez un entrepôt"
            return
        }
        guard !lineItems.isEmpty else {
            errorMessage = "Ajoutez au moins un article"
            return
        }

        isSaving = true
        errorMessage = nil
        successMessage = nil
        defer { isSaving = false }

        let request = OrderCreateRequest(
            supplier_id: supplier.id,
            warehouse_id: warehouse.id,
            notes: notes.isEmpty ? nil : notes,
            items: lineItems.map { OrderItemRequest(product_id: $0.productId, quantity: $0.quantity, price: $0.unitPrice) }
        )

        do {
            try await orderService.createOrder(request)
            successMessage = "Commande créée avec succès"
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

struct OrderLineItem: Identifiable {
    let id = UUID()
    let productId: Int
    let productName: String
    var quantity: Int
    var unitPrice: Double

    var lineTotal: Double { Double(quantity) * unitPrice }
    var formattedTotal: String { String(format: "%.2f MAD", lineTotal) }
    var formattedPrice: String { String(format: "%.2f MAD", unitPrice) }
}
