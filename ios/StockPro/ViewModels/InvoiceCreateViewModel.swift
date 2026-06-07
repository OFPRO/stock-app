import Foundation

final class InvoiceCreateViewModel: ObservableObject {
    private let invoiceService: InvoiceServiceProtocol
    private let customerService: CustomerServiceProtocol
    private let productService: ProductServiceProtocol

    @Published var customers: [CustomerListItem] = []
    @Published var products: [ForSaleProductDTO] = []
    @Published var selectedCustomer: CustomerListItem?
    @Published var lineItems: [InvoiceLineItem] = []
    @Published var paymentMethod: String = "especes"
    @Published var notes: String = ""
    @Published var isLoading = false
    @Published var isSaving = false
    @Published var errorMessage: String?
    @Published var successMessage: String?

    var total: Double {
        lineItems.reduce(0) { $0 + $1.lineTotal }
    }

    init(invoiceService: InvoiceServiceProtocol = InvoiceService(),
         customerService: CustomerServiceProtocol = CustomerService(),
         productService: ProductServiceProtocol = ProductService()) {
        self.invoiceService = invoiceService
        self.customerService = customerService
        self.productService = productService
    }

    @MainActor
    func loadInitialData() async {
        isLoading = true
        defer { isLoading = false }
        do {
            async let c = customerService.fetchCustomers(search: nil)
            async let p = productService.fetchProductsForSale()
            (customers, products) = try await (c, p)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func addItem(_ product: ForSaleProductDTO) {
        guard !lineItems.contains(where: { $0.productId == product.id }) else { return }
        lineItems.append(InvoiceLineItem(
            productId: product.id,
            productName: product.name,
            quantity: 1,
            unitPrice: product.sale_price ?? product.price
        ))
    }

    func removeItem(at index: Int) {
        lineItems.remove(at: index)
    }

    @MainActor
    func submit() async {
        guard !lineItems.isEmpty else {
            errorMessage = "Ajoutez au moins un article"
            return
        }
        isSaving = true
        errorMessage = nil
        successMessage = nil
        defer { isSaving = false }

        let request = InvoiceCreateRequest(
            customer_id: selectedCustomer?.id,
            items: lineItems.map { InvoiceItemRequest(product_id: $0.productId, quantity: $0.quantity, price: $0.unitPrice) },
            payment_method: paymentMethod,
            notes: notes.isEmpty ? nil : notes
        )

        do {
            try await invoiceService.createInvoice(request)
            successMessage = "Facture créée avec succès"
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

struct InvoiceLineItem: Identifiable {
    let id = UUID()
    let productId: Int
    let productName: String
    var quantity: Int
    var unitPrice: Double

    var lineTotal: Double { Double(quantity) * unitPrice }
    var formattedTotal: String { String(format: "%.2f MAD", lineTotal) }
    var formattedPrice: String { String(format: "%.2f MAD", unitPrice) }
}
