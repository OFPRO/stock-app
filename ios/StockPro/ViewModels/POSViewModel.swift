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
    @Published var productSearchError: String?
    @Published var productCount: Int = 0
    private var cachedProducts: [ForSaleProductDTO] = []

    @Published var selectedCustomer: POSCustomerDTO?
    @Published var customers: [POSCustomerDTO] = []
    @Published var showCustomerPicker = false
    @Published var selectedTier: DiscountTier = .normal
    @Published var showTierPicker = false

    @Published var bestSellers: [BestSellerDTO] = []
    @Published var isLoadingBestSellers = false

    @Published var showPayment = false
    @Published var paymentMethod: PaymentMethod = .cash
    @Published var cashTenderedText = ""
    @Published var cardAmountText = ""
    @Published var isCredit = false
    @Published var checkoutResult: POSTransactionResponse?
    @Published var showReceipt = false
    @Published var lastCheckoutItems: [CartItem] = []
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
    @Published var showCloseSession = false
    @Published var closingCashText = ""

    private let posService: POSServiceProtocol
    private let productService: ProductServiceProtocol
    private var idleTimer: Timer?
    private var lastActivityTime = Date()
    private let idleTimeout: TimeInterval = 3600

    init(posService: POSServiceProtocol = POSService(), productService: ProductServiceProtocol = ProductService()) {
        self.posService = posService
        self.productService = productService
    }

    deinit {
        idleTimer?.invalidate()
    }

    func touchActivity() {
        lastActivityTime = Date()
    }

    private func startIdleTimer() {
        stopIdleTimer()
        idleTimer = Timer.scheduledTimer(withTimeInterval: 60, repeats: true) { [weak self] _ in
            guard let self = self, self.sessionIsOpen else { return }
            if Date().timeIntervalSince(self.lastActivityTime) > self.idleTimeout {
                Task { @MainActor in
                    await self.doCloseSession()
                    self.sessionErrorMessage = "Session fermée automatiquement après 1h d'inactivité."
                }
                self.stopIdleTimer()
            }
        }
    }

    private func stopIdleTimer() {
        idleTimer?.invalidate()
        idleTimer = nil
    }

    private func doCloseSession() async {
        guard let sessionId = session?.id else { return }
        do {
            let response = try await posService.closeSession(id: sessionId, closingCash: cashBalance)
            if response.success {
                session = nil
                cart.removeAll()
                cashMovements = []
            }
        } catch {}
    }

    func onAppear() {
        Task { await loadInitialData() }
    }

    func loadInitialData() async {
        isLoadingSession = true
        sessionErrorMessage = nil

        session = try? await posService.fetchSession()
        customers = (try? await posService.fetchPOSCustomers()) ?? []
        bestSellers = (try? await posService.fetchBestSellers()) ?? []
        recentTransactions = (try? await posService.fetchRecentTransactions(sessionId: nil, limit: 20)) ?? []
        cachedProducts = (try? await productService.fetchProductsForSale()) ?? []
        self.productCount = cachedProducts.count
        NSLog("loadInitialData: cachedProducts.count = \(cachedProducts.count), session open = \(session?.isOpen ?? false)")

        if let s = session, s.isOpen {
            await loadCashMovements()
            touchActivity()
            startIdleTimer()
        }

        if cachedProducts.isEmpty {
            NSLog("cachedProducts is EMPTY - search will find nothing")
            sessionErrorMessage = "Impossible de charger la caisse. Vérifiez votre connexion."
        } else if let first = cachedProducts.first {
            NSLog("First product: name='\(first.name)' id=\(first.id) price=\(first.price)")
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
            touchActivity()
            startIdleTimer()
            await loadCashMovements()
            cachedProducts = (try? await productService.fetchProductsForSale()) ?? cachedProducts
        } catch {
            sessionErrorMessage = AppError.from(error).errorDescription
        }
    }

    func closeSession() async {
        guard let sessionId = session?.id else { return }
        let cash = Double(closingCashText.replacingOccurrences(of: ",", with: ".")) ?? cashBalance
        stopIdleTimer()
        do {
            let response = try await posService.closeSession(id: sessionId, closingCash: cash)
            if response.success {
                session = nil
                cart.removeAll()
                cashMovements = []
                showCloseSession = false
                closingCashText = ""
            }
        } catch {
            sessionErrorMessage = AppError.from(error).errorDescription
        }
    }

    // MARK: - Cart

    func addToCart(product: ForSaleProductDTO) {
        touchActivity()
        let price = product.sale_price ?? product.price
        if let index = cart.firstIndex(where: { $0.productId == product.id }) {
            cart[index].quantity += 1
        } else {
            cart.append(CartItem(
                productId: product.id,
                productName: product.name,
                productSku: product.sku,
                quantity: 1,
                unitPrice: price,
                baseUnitPrice: price,
                priceLoyal: product.price_loyal,
                priceSchool: product.price_school,
                priceStudent: product.price_student,
                discountPercent: 0
            ))
        }
        applyPricing()
    }

    func addBestSeller(_ bestSeller: BestSellerDTO) {
        touchActivity()
        let dto = ForSaleProductDTO(
            id: bestSeller.id,
            name: bestSeller.name,
            sku: bestSeller.sku,
            barcode: nil,
            price: bestSeller.price,
            sale_price: bestSeller.price_base,
            price_loyal: nil,
            price_school: nil,
            price_student: nil,
            quantity: bestSeller.quantity ?? 0,
            min_quantity: nil,
            category: nil,
            warehouse_id: nil
        )
        addToCart(product: dto)
    }

    func incrementCartItem(_ item: CartItem) {
        touchActivity()
        guard let index = cart.firstIndex(where: { $0.id == item.id }) else { return }
        cart[index].quantity += 1
    }

    func decrementCartItem(_ item: CartItem) {
        touchActivity()
        guard let index = cart.firstIndex(where: { $0.id == item.id }) else { return }
        if cart[index].quantity > 1 {
            cart[index].quantity -= 1
        } else {
            cart.remove(at: index)
        }
    }

    func removeCartItem(_ item: CartItem) {
        touchActivity()
        cart.removeAll { $0.id == item.id }
    }

    func clearCart() {
        touchActivity()
        cart.removeAll()
    }

    func updateCartItemPrice(itemId: UUID, newPrice: Double) {
        touchActivity()
        guard let index = cart.firstIndex(where: { $0.id == itemId }) else { return }
        cart[index].unitPrice = newPrice
    }

    // MARK: - Customer

    func selectCustomer(_ customer: POSCustomerDTO?) {
        touchActivity()
        selectedCustomer = customer
        if let rate = customer?.discount_rate, rate >= 20 {
            selectedTier = .school
        } else if let rate = customer?.discount_rate, rate >= 15 {
            selectedTier = .loyal
        } else {
            selectedTier = .normal
        }
        applyPricing()
    }

    func selectTier(_ tier: DiscountTier) {
        touchActivity()
        selectedTier = tier
        applyPricing()
    }

    func applyPricing() {
        for i in cart.indices {
            cart[i].unitPrice = selectedTier.priceFor(item: cart[i])
            cart[i].discountPercent = 0
        }
    }

    // MARK: - Product Search

    func searchProducts(query: String) async {
        touchActivity()
        productSearchError = nil
        NSLog("searchProducts called query='\(query)' cached=\(self.cachedProducts.count)")
        guard !query.trimmingCharacters(in: .whitespaces).isEmpty else {
            productSearchResults = []
            return
        }
        isSearchingProducts = true
        let results = cachedProducts
        productSearchResults = results.filter {
            $0.name.localizedCaseInsensitiveContains(query)
            || ($0.barcode?.localizedCaseInsensitiveContains(query) ?? false)
            || $0.sku.localizedCaseInsensitiveContains(query)
        }
        NSLog("searchProducts filtered \(results.count)->\(self.productSearchResults.count)")
        isSearchingProducts = false
    }

    // MARK: - Barcode Scan

    func didScanBarcode(_ code: String) async {
        touchActivity()
        do {
            if let product = try await productService.fetchProductByBarcode(code) {
                let dto = ForSaleProductDTO(
                    id: product.id,
                    name: product.name,
                    sku: product.sku,
                    barcode: product.barcode,
                    price: parseFrenchPrice(product.price),
                    sale_price: nil,
                    price_loyal: nil,
                    price_school: nil,
                    price_student: nil,
                    quantity: product.stock,
                    min_quantity: nil,
                    category: nil,
                    warehouse_id: nil
                )
                addToCart(product: dto)
            } else {
                sessionErrorMessage = "Aucun produit trouvé pour le code « \(code) »"
            }
        } catch {
            sessionErrorMessage = "Erreur: \(error.localizedDescription)"
        }
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

    var sessionIsOpen: Bool { session?.isOpen == true }

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

        touchActivity()

        do {
            let response = try await posService.createTransaction(request)
            checkoutResult = response
            showPayment = false
            showReceipt = true
            lastCheckoutItems = cart
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
        touchActivity()
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

    var expectedCash: Double { cashBalance }
    var formattedExpectedCash: String { String(format: "%.2f MAD", expectedCash) }
    var closingDifference: Double {
        let cash = Double(closingCashText.replacingOccurrences(of: ",", with: ".")) ?? expectedCash
        return cash - expectedCash
    }
    var formattedClosingDifference: String {
        let diff = closingDifference
        if diff >= 0 {
            return String(format: "+%.2f MAD", diff)
        }
        return String(format: "%.2f MAD", diff)
    }
    var hasClosingDifference: Bool {
        abs(closingDifference) > 0.01
    }
}

private func parseFrenchPrice(_ string: String) -> Double {
    let cleaned = string
        .replacingOccurrences(of: " MAD", with: "")
        .replacingOccurrences(of: " ", with: "")
        .replacingOccurrences(of: ",", with: ".")
    return Double(cleaned) ?? 0
}
