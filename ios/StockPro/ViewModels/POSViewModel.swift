import Foundation
import SwiftUI

@MainActor
final class POSViewModel: ObservableObject {
    @Published var session: POSSessionDTO?
    @Published var isLoadingSession = true
    @Published var sessionErrorMessage: String?

    @Published var cart: [CartItem] = []
    @Published var showProductSearch = false
    @Published var productSearchText = ""
    @Published var productSearchResults: [ForSaleProductDTO] = []
    @Published var isSearchingProducts = false

    @Published var selectedCustomer: POSCustomerDTO?
    @Published var customers: [POSCustomerDTO] = []
    @Published var showCustomerPicker = false

    @Published var bestSellers: [BestSellerDTO] = []
    @Published var isLoadingBestSellers = false

    @Published var showPayment = false
    @Published var paymentMethod: PaymentMethod = .cash
    @Published var cashTenderedText = ""
    @Published var cardAmountText = ""
    @Published var isCredit = false
    @Published var checkoutResult: POSTransactionResponse?
    @Published var showReceipt = false
    @Published var isCheckingOut = false
    @Published var checkoutError: String?

    @Published var cashMovements: [CashMovementDTO] = []
    @Published var showCashMovements = false
    @Published var cashMovementType = "in"
    @Published var cashAmountText = ""
    @Published var cashReasonText = ""
    @Published var cashNoteText = ""

    @Published var recentTransactions: [POSTransactionDTO] = []

    @Published var showScanner = false
    @Published var showOpeningDialog = false
    @Published var openingCashText = "0"

    private let posService: POSServiceProtocol
    private let productService: ProductServiceProtocol

    init(posService: POSServiceProtocol = POSService(), productService: ProductServiceProtocol = ProductService()) {
        self.posService = posService
        self.productService = productService
    }

    func onAppear() {
        Task { await loadInitialData() }
    }

    func loadInitialData() async {
        isLoadingSession = true
        sessionErrorMessage = nil
        do {
            async let sessionTask = posService.fetchSession()
            async let customersTask = posService.fetchPOSCustomers()
            async let bestSellersTask = posService.fetchBestSellers()
            async let transactionsTask = posService.fetchRecentTransactions(sessionId: nil, limit: 20)

            let (fetchedSession, fetchedCustomers, fetchedBestSellers, fetchedTransactions) = try await (sessionTask, customersTask, bestSellersTask, transactionsTask)

            session = fetchedSession
            customers = fetchedCustomers
            bestSellers = fetchedBestSellers
            recentTransactions = fetchedTransactions

            if let s = fetchedSession, s.isOpen {
                await loadCashMovements()
            }
        } catch {
            sessionErrorMessage = "Impossible de charger la caisse. Vérifiez votre connexion."
        }
        isLoadingSession = false
    }

    func loadCashMovements() async {
        do {
            cashMovements = try await posService.fetchCashMovements()
        } catch {}
    }

    // MARK: - Session

    func openSession() async {
        guard let cash = Double(openingCashText), cash >= 0 else {
            sessionErrorMessage = "Montant d'ouverture invalide"
            return
        }
        do {
            let newSession = try await posService.openSession(openingCash: cash)
            session = newSession
            openingCashText = "0"
            await loadCashMovements()
        } catch {
            sessionErrorMessage = AppError.from(error).errorDescription
        }
    }

    func closeSession(closingCash: Double) async {
        guard let sessionId = session?.id else { return }
        do {
            let response = try await posService.closeSession(id: sessionId, closingCash: closingCash)
            if response.success {
                session = nil
                cart.removeAll()
                cashMovements = []
            }
        } catch {
            sessionErrorMessage = AppError.from(error).errorDescription
        }
    }

    // MARK: - Cart

    func addToCart(product: ForSaleProductDTO) {
        let price = product.sale_price ?? product.price
        if let index = cart.firstIndex(where: { $0.productId == product.id }) {
            if selectedCustomer != nil {
                cart[index].unitPrice = price
            }
            cart[index].quantity += 1
        } else {
            cart.append(CartItem(
                productId: product.id,
                productName: product.name,
                productSku: product.sku,
                quantity: 1,
                unitPrice: price,
                discountPercent: 0
            ))
        }
    }

    func addBestSeller(_ bestSeller: BestSellerDTO) {
        let dto = ForSaleProductDTO(
            id: bestSeller.id,
            name: bestSeller.name,
            sku: bestSeller.sku,
            barcode: nil,
            price: bestSeller.price,
            sale_price: bestSeller.price_base,
            quantity: 999,
            category: nil,
            warehouse_id: nil
        )
        addToCart(product: dto)
    }

    func incrementCartItem(_ item: CartItem) {
        guard let index = cart.firstIndex(where: { $0.id == item.id }) else { return }
        cart[index].quantity += 1
    }

    func decrementCartItem(_ item: CartItem) {
        guard let index = cart.firstIndex(where: { $0.id == item.id }) else { return }
        if cart[index].quantity > 1 {
            cart[index].quantity -= 1
        } else {
            cart.remove(at: index)
        }
    }

    func removeCartItem(_ item: CartItem) {
        cart.removeAll { $0.id == item.id }
    }

    func clearCart() {
        cart.removeAll()
    }

    func updateCartItemPrice(itemId: UUID, newPrice: Double) {
        guard let index = cart.firstIndex(where: { $0.id == itemId }) else { return }
        if selectedCustomer == nil {
            cart[index].unitPrice = newPrice
        }
    }

    // MARK: - Customer

    func selectCustomer(_ customer: POSCustomerDTO?) {
        selectedCustomer = customer
        if let cust = customer, let rate = cust.discount_rate, rate > 0 {
            for i in cart.indices {
                let basePrice = bestSellers.first(where: { $0.id == cart[i].productId })?.price_base
                    ?? cart[i].unitPrice
                cart[i].unitPrice = basePrice * (1 - rate / 100)
                cart[i].discountPercent = 0
            }
        } else if customer == nil {
            for i in cart.indices {
                cart[i].discountPercent = 0
            }
        }
    }

    // MARK: - Product Search

    func searchProducts(query: String) async {
        guard !query.trimmingCharacters(in: .whitespaces).isEmpty else {
            productSearchResults = []
            return
        }
        isSearchingProducts = true
        do {
            let results: [ForSaleProductDTO] = try await APIClient.shared.request(.productsForSale)
            productSearchResults = results.filter {
                $0.name.localizedCaseInsensitiveContains(query)
                || ($0.barcode?.localizedCaseInsensitiveContains(query) ?? false)
                || $0.sku.localizedCaseInsensitiveContains(query)
            }
        } catch {
            productSearchResults = []
        }
        isSearchingProducts = false
    }

    // MARK: - Barcode Scan

    func didScanBarcode(_ code: String) async {
        do {
            if let product = try await productService.fetchProductByBarcode(code) {
                let dto = ForSaleProductDTO(
                    id: product.id,
                    name: product.name,
                    sku: product.sku,
                    barcode: product.barcode,
                    price: parseFrenchPrice(product.price),
                    sale_price: nil,
                    quantity: product.stock,
                    category: nil,
                    warehouse_id: nil
                )
                addToCart(product: dto)
            }
        } catch {}
    }

    // MARK: - Checkout

    var subtotal: Double {
        cart.reduce(0) { $0 + Double($1.quantity) * $1.unitPrice }
    }

    var discountTotal: Double {
        cart.reduce(0) { $0 + Double($1.quantity) * $1.unitPrice * $1.discountPercent / 100 }
    }

    var taxableAmount: Double { subtotal - discountTotal }
    var taxAmount: Double { taxableAmount * 0.20 }
    var total: Double { taxableAmount * 1.20 }
    var cartItemCount: Int { cart.reduce(0) { $0 + $1.quantity } }
    var isEmpty: Bool { cart.isEmpty }

    func beginCheckout() {
        guard sessionIsOpen else {
            sessionErrorMessage = "Une session doit être ouverte pour encaisser"
            return
        }
        showPayment = true
        cashTenderedText = String(format: "%.0f", ceil(total / 10) * 10)
        cardAmountText = ""
        isCredit = false
        checkoutError = nil
    }

    func executeCheckout() async {
        guard let sessionId = session?.id, !cart.isEmpty else { return }
        isCheckingOut = true
        checkoutError = nil

        let cashTendered = Double(cashTenderedText.replacingOccurrences(of: ",", with: ".")) ?? 0
        let cardAmount = Double(cardAmountText.replacingOccurrences(of: ",", with: ".")) ?? 0
        let effectiveTendered: Double

        switch paymentMethod {
        case .cash:
            effectiveTendered = cashTendered
        case .card:
            effectiveTendered = total
        case .mixed:
            effectiveTendered = cashTendered + cardAmount
        }

        let items = cart.map { item in
            POSTransactionRequest.TransactionItem(
                product_id: item.productId,
                quantity: item.quantity,
                unit_price: item.unitPrice,
                discount_percent: item.discountPercent,
                product_name: item.productName,
                product_sku: item.productSku
            )
        }

        let request = POSTransactionRequest(
            session_id: sessionId,
            customer_id: selectedCustomer?.id,
            items: items,
            payment_method: paymentMethod.rawValue,
            tendered_amount: effectiveTendered,
            notes: "",
            is_credit: isCredit
        )

        do {
            let response = try await posService.createTransaction(request)
            checkoutResult = response
            showPayment = false
            showReceipt = true
            cart.removeAll()
            selectedCustomer = nil
            await loadCashMovements()
            recentTransactions = try await posService.fetchRecentTransactions(sessionId: sessionId, limit: 20)
        } catch {
            checkoutError = AppError.from(error).errorDescription
        }
        isCheckingOut = false
    }

    var changeAmount: Double {
        guard paymentMethod == .cash || paymentMethod == .mixed else { return 0 }
        let cashTendered = Double(cashTenderedText.replacingOccurrences(of: ",", with: ".")) ?? 0
        let cardAmount = Double(cardAmountText.replacingOccurrences(of: ",", with: ".")) ?? 0
        let totalTendered = cashTendered + cardAmount
        return max(0, totalTendered - total)
    }

    // MARK: - Cash Movements

    func recordCashMovement() async {
        let amount = Double(cashAmountText.replacingOccurrences(of: ",", with: ".")) ?? 0
        guard amount > 0, !cashReasonText.isEmpty else { return }
        do {
            try await posService.createCashMovement(type: cashMovementType, amount: amount, reason: cashReasonText, note: cashNoteText.isEmpty ? nil : cashNoteText)
            cashAmountText = ""
            cashReasonText = ""
            cashNoteText = ""
            await loadCashMovements()
        } catch {
            sessionErrorMessage = AppError.from(error).errorDescription
        }
    }

    var cashBalance: Double {
        guard let opening = session?.opening_cash else { return 0 }
        let ins = cashMovements.filter { $0.type == "in" }.reduce(0) { $0 + ($1.amount ?? 0) }
        let outs = cashMovements.filter { $0.type == "out" }.reduce(0) { $0 + ($1.amount ?? 0) }
        return opening + ins - outs
    }
}

extension POSViewModel {
    var formattedSubtotal: String { String(format: "%.2f MAD", subtotal) }
    var formattedDiscount: String { String(format: "–%.2f MAD", discountTotal) }
    var formattedTax: String { String(format: "%.2f MAD", taxAmount) }
    var formattedTotal: String { String(format: "%.2f MAD", total) }
    var formattedChange: String { String(format: "%.2f MAD", changeAmount) }
}

private func parseFrenchPrice(_ string: String) -> Double {
    let cleaned = string
        .replacingOccurrences(of: " MAD", with: "")
        .replacingOccurrences(of: " ", with: "")
        .replacingOccurrences(of: ",", with: ".")
    return Double(cleaned) ?? 0
}
