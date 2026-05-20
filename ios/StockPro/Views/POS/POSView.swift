import SwiftUI
import AVFoundation

struct POSView: View {
    @StateObject private var viewModel = POSViewModel()

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                if !Reachability.shared.isConnected {
                    offlineBanner
                }

                sessionHeader

                if viewModel.isLoadingSession {
                    Spacer()
                    ProgressView("Chargement de la caisse...")
                    Spacer()
                } else if viewModel.sessionIsOpen {
                    openSessionContent
                } else {
                    closedSessionContent
                }
            }
            .background(AppColor.background)
            .navigationTitle("Caisse")
            .toolbar {
                if viewModel.sessionIsOpen {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        HStack(spacing: 4) {
                            customerButton
                            scannerButton
                        }
                    }
                }
            }
            .sheet(isPresented: $viewModel.showScanner) {
                POSScannerSheet { code in
                    Task { await viewModel.didScanBarcode(code) }
                }
            }
            .sheet(isPresented: $viewModel.showCustomerPicker) {
                customerPickerSheet
            }
            .sheet(isPresented: $viewModel.showProductSearch) {
                productSearchSheet
            }
            .sheet(isPresented: $viewModel.showCashMovements) {
                cashMovementsSheet
            }
            .sheet(isPresented: $viewModel.showPayment) {
                PaymentView(viewModel: viewModel)
            }
            .sheet(isPresented: $viewModel.showReceipt) {
                receiptSheet
            }
            .alert("Caisse", isPresented: .init(
                get: { viewModel.sessionErrorMessage != nil },
                set: { if !$0 { viewModel.sessionErrorMessage = nil } }
            )) {
                Button("OK") { viewModel.sessionErrorMessage = nil }
            } message: {
                Text(viewModel.sessionErrorMessage ?? "")
            }
            .onAppear { viewModel.onAppear() }
        }
    }

    // MARK: - Offline

    private var offlineBanner: some View {
        HStack {
            Image(systemName: "wifi.slash")
            Text("Connexion requise pour la caisse")
        }
        .font(.caption.weight(.medium))
        .foregroundStyle(.white)
        .frame(maxWidth: .infinity)
        .padding(8)
        .background(AppColor.error)
    }

    // MARK: - Session Header

    private var sessionHeader: some View {
        HStack {
            Image(systemName: viewModel.sessionIsOpen ? "lock.open.fill" : "lock.fill")
                .foregroundStyle(viewModel.sessionIsOpen ? .green : .secondary)
            Text(viewModel.sessionIsOpen ? "Session ouverte" : "Session fermée")
                .font(.caption.weight(.medium))
            Spacer()
            if let s = viewModel.session, s.isOpen {
                Text(s.session_number ?? "")
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
            }
        }
        .padding(.horizontal)
        .padding(.vertical, 8)
        .background(AppColor.surface)
    }

    // MARK: - Open Session (cashier mode)

    private var openSessionContent: some View {
        VStack(spacing: 0) {
            topActionRow

            if viewModel.isEmpty {
                emptyCartContent
            } else {
                cartContent
            }
        }
    }

    private var topActionRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                StockButton("Scanner", variant: .secondary, icon: "barcode.viewfinder") {
                    viewModel.showScanner = true
                }
                .fixedSize()

                StockButton("Rechercher", variant: .secondary, icon: "magnifyingglass") {
                    viewModel.showProductSearch = true
                }
                .fixedSize()

                if !viewModel.bestSellers.isEmpty {
                    ForEach(viewModel.bestSellers.prefix(5)) { seller in
                        Button {
                            viewModel.addBestSeller(seller)
                        } label: {
                            VStack(spacing: 2) {
                                Text(seller.name)
                                    .font(.caption2.weight(.medium))
                                    .lineLimit(1)
                                Text(String(format: "%.2f MAD", seller.price_base))
                                    .font(.caption2)
                                    .foregroundStyle(AppColor.brand)
                            }
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(AppColor.surface)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 8)
        }
    }

    // MARK: - Empty Cart

    private var emptyCartContent: some View {
        VStack(spacing: 16) {
            Spacer()
            Image(systemName: "cart")
                .font(.system(size: 48))
                .foregroundStyle(.tertiary)
            Text("Panier vide")
                .font(.title3.weight(.semibold))
            Text("Scannez ou recherchez un produit")
                .font(.subheadline)
                .foregroundStyle(.secondary)

            HStack(spacing: 12) {
                StockButton("Scanner", variant: .primary, icon: "barcode.viewfinder") {
                    viewModel.showScanner = true
                }
                .frame(maxWidth: 160)

                StockButton("Rechercher", variant: .secondary, icon: "magnifyingglass") {
                    viewModel.showProductSearch = true
                }
                .frame(maxWidth: 160)
            }

            if let customer = viewModel.selectedCustomer {
                HStack(spacing: 4) {
                    Image(systemName: "person.fill")
                        .font(.caption)
                    Text(customer.name)
                        .font(.caption)
                    if let rate = customer.discount_rate, rate > 0 {
                        Text("–\(Int(rate))%")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(AppColor.success)
                    }
                }
                .padding(.top, 8)
                .foregroundStyle(.secondary)
            }

            Spacer()
        }
    }

    // MARK: - Cart

    private var cartContent: some View {
        VStack(spacing: 0) {
            customerBadge

            List {
                ForEach(viewModel.cart) { item in
                    CartItemRowView(
                        item: item,
                        onIncrement: { viewModel.incrementCartItem(item) },
                        onDecrement: { viewModel.decrementCartItem(item) },
                        onRemove: { viewModel.removeCartItem(item) }
                    )
                }
                .onDelete { offsets in
                    for i in offsets { viewModel.removeCartItem(viewModel.cart[i]) }
                }
            }
            .listStyle(.plain)

            cartSummary
        }
    }

    private var customerBadge: some View {
        HStack {
            if let customer = viewModel.selectedCustomer {
                HStack(spacing: 4) {
                    Image(systemName: "person.fill")
                        .font(.caption2)
                    Text(customer.name)
                        .font(.caption2.weight(.medium))
                    if let rate = customer.discount_rate, rate > 0 {
                        Text("remise \(Int(rate))%")
                            .font(.caption2)
                            .foregroundStyle(AppColor.success)
                    }
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(AppColor.success.opacity(0.1))
                .clipShape(Capsule())
            }
            Spacer()
            StockButton("Vider", variant: .ghost, icon: "trash") {
                viewModel.clearCart()
            }
            .font(.caption)
            .frame(width: 80)
        }
        .padding(.horizontal)
        .padding(.vertical, 4)
    }

    private var cartSummary: some View {
        VStack(spacing: 8) {
            Divider()
            VStack(spacing: 4) {
                summaryRow("Sous-total HT", viewModel.formattedSubtotal)
                if viewModel.discountTotal > 0 {
                    summaryRow("Remises", viewModel.formattedDiscount)
                        .foregroundStyle(AppColor.success)
                }
                summaryRow("TVA (20%)", viewModel.formattedTax)
                Divider()
                HStack {
                    Text("Total TTC")
                        .font(.subheadline.weight(.semibold))
                    Spacer()
                    Text(viewModel.formattedTotal)
                        .font(AppFont.posTotal)
                        .foregroundStyle(AppColor.brand)
                }
            }
            .padding(.horizontal)

            StockButton("ENCAISSER", variant: .primary, icon: "eurosign.circle") {
                viewModel.beginCheckout()
            }
            .padding(.horizontal)
            .padding(.bottom, 8)
        }
        .background(AppColor.surface)
    }

    private func summaryRow(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
            Spacer()
            Text(value)
                .font(.subheadline.weight(.medium))
        }
    }

    // MARK: - Closed Session

    private var closedSessionContent: some View {
        VStack(spacing: 20) {
            Spacer()
            Image(systemName: "lock.fill")
                .font(.system(size: 48))
                .foregroundStyle(.tertiary)
            Text("Session fermée")
                .font(.title3.weight(.semibold))
            Text("Ouvrez une session pour commencer à vendre")
                .font(.subheadline)
                .foregroundStyle(.secondary)

            Button {
                viewModel.showOpeningDialog = true
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: "eurosign.circle")
                    Text("Ouvrir une session")
                }
                .frame(maxWidth: 200)
                .padding(.vertical, 14)
                .background(AppColor.brand)
                .foregroundStyle(.white)
                .clipShape(RoundedRectangle(cornerRadius: 10))
            }

            if !viewModel.recentTransactions.isEmpty {
                recentTransactionsList
            }

            Spacer()
        }
        .alert("Ouvrir la caisse", isPresented: $viewModel.showOpeningDialog) {
            TextField("Montant d'ouverture (MAD)", text: $viewModel.openingCashText)
                .keyboardType(.decimalPad)
            Button("Ouvrir") {
                Task { await viewModel.openSession() }
            }
            Button("Annuler", role: .cancel) {}
        } message: {
            Text("Saisissez le montant en caisse pour l'ouverture")
        }
    }

    // MARK: - Recent Transactions

    private var recentTransactionsList: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Dernières transactions")
                .font(.subheadline.weight(.semibold))
                .padding(.horizontal)

            ForEach(Array(viewModel.recentTransactions.prefix(10))) { t in
                HStack {
                    Image(systemName: t.payment_method == "cash" ? "banknote" : "creditcard")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .frame(width: 20)
                    Text(t.ticket_number ?? "")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Spacer()
                    Text(String(format: "%.2f MAD", t.total ?? 0))
                        .font(.caption.weight(.medium))
                }
                .padding(.horizontal)
                .padding(.vertical, 4)
            }
        }
    }

    // MARK: - Toolbar Buttons

    private var scannerButton: some View {
        Button {
            viewModel.showScanner = true
        } label: {
            Image(systemName: "barcode.viewfinder")
        }
        .accessibilityLabel("Scanner un produit")
    }

    private var customerButton: some View {
        Button {
            viewModel.showCustomerPicker = true
        } label: {
            HStack(spacing: 4) {
                Image(systemName: "person.fill")
                if viewModel.selectedCustomer != nil {
                    Text("Changer")
                        .font(.caption)
                }
            }
            .font(.subheadline)
        }
        .accessibilityLabel("Sélectionner un client")
    }

    // MARK: - Customer Picker

    private var customerPickerSheet: some View {
        NavigationStack {
            List {
                Button("Client Comptoir (aucune remise)") {
                    viewModel.selectCustomer(nil)
                    viewModel.showCustomerPicker = false
                }

                Section("Clients") {
                    ForEach(viewModel.customers) { customer in
                        Button {
                            viewModel.selectCustomer(customer)
                            viewModel.showCustomerPicker = false
                        } label: {
                            HStack {
                                VStack(alignment: .leading) {
                                    Text(customer.name)
                                        .font(.subheadline.weight(.medium))
                                    if let code = customer.client_code {
                                        Text(code)
                                            .font(.caption)
                                            .foregroundStyle(.secondary)
                                    }
                                }
                                Spacer()
                                if let rate = customer.discount_rate, rate > 0 {
                                    Text("–\(Int(rate))%")
                                        .font(.caption.weight(.semibold))
                                        .foregroundStyle(AppColor.success)
                                }
                                if viewModel.selectedCustomer?.id == customer.id {
                                    Image(systemName: "checkmark")
                                        .foregroundStyle(AppColor.brand)
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle("Choisir un client")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Fermer") { viewModel.showCustomerPicker = false }
                }
            }
        }
    }

    // MARK: - Product Search

    private var productSearchSheet: some View {
        NavigationStack {
            VStack(spacing: 0) {
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundStyle(.secondary)
                    TextField("Rechercher un produit...", text: $viewModel.productSearchText)
                        .onSubmit {
                            Task { await viewModel.searchProducts(query: viewModel.productSearchText) }
                        }
                    if !viewModel.productSearchText.isEmpty {
                        Button {
                            viewModel.productSearchText = ""
                            viewModel.productSearchResults = []
                        } label: {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundStyle(.tertiary)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(12)
                .background(AppColor.surface)
                .clipShape(RoundedRectangle(cornerRadius: 8))
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(AppColor.border, lineWidth: 0.5)
                )
                .padding()

                if viewModel.isSearchingProducts {
                    Spacer()
                    ProgressView("Recherche...")
                    Spacer()
                } else if viewModel.productSearchResults.isEmpty && !viewModel.productSearchText.isEmpty {
                    Spacer()
                    ContentUnavailableView(
                        "Aucun résultat",
                        systemImage: "magnifyingglass",
                        description: Text("Essayez un autre terme de recherche")
                    )
                    Spacer()
                } else {
                    List(viewModel.productSearchResults) { product in
                        Button {
                            viewModel.addToCart(product: product)
                            viewModel.showProductSearch = false
                            viewModel.productSearchText = ""
                            viewModel.productSearchResults = []
                        } label: {
                            HStack {
                                VStack(alignment: .leading) {
                                    Text(product.name)
                                        .font(.subheadline.weight(.medium))
                                    Text(product.sku)
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                }
                                Spacer()
                                Text(String(format: "%.2f MAD", product.sale_price ?? product.price))
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundStyle(AppColor.brand)
                            }
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle("Ajouter un produit")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Fermer") {
                        viewModel.showProductSearch = false
                        viewModel.productSearchText = ""
                        viewModel.productSearchResults = []
                    }
                }
            }
        }
    }

    // MARK: - Cash Movements

    private var cashMovementsSheet: some View {
        NavigationStack {
            VStack(spacing: 0) {
                HStack {
                    Text("Solde caisse")
                        .font(.subheadline)
                    Spacer()
                    Text(String(format: "%.2f MAD", viewModel.cashBalance))
                        .font(.title3.weight(.bold))
                        .foregroundStyle(AppColor.brand)
                }
                .padding()
                .background(AppColor.surface)

                Form {
                    Section {
                        Picker("Type", selection: $viewModel.cashMovementType) {
                            Text("Entrée").tag("in")
                            Text("Sortie").tag("out")
                        }
                        .pickerStyle(.segmented)

                        HStack {
                            TextField("Montant", text: $viewModel.cashAmountText)
                                .keyboardType(.decimalPad)
                            Text("MAD")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }

                        VStack(alignment: .leading, spacing: 4) {
                            TextField("Motif", text: $viewModel.cashReasonText)
                            if viewModel.cashMovementType == "in" {
                                Text("Ex: Crédit Client, Réapprovisionnement")
                                    .font(.caption2)
                                    .foregroundStyle(.tertiary)
                            } else {
                                Text("Ex: Café, Déjeuner, Eau, Transport, Autre")
                                    .font(.caption2)
                                    .foregroundStyle(.tertiary)
                            }
                        }

                        TextField("Note (optionnelle)", text: $viewModel.cashNoteText)
                    }

                    Button("Enregistrer") {
                        Task { await viewModel.recordCashMovement() }
                    }
                    .disabled(viewModel.cashAmountText.isEmpty || viewModel.cashReasonText.isEmpty)

                    if !viewModel.cashMovements.isEmpty {
                        Section("Mouvements") {
                            ForEach(viewModel.cashMovements) { m in
                                HStack {
                                    Image(systemName: m.type == "in" ? "arrow.down.circle.fill" : "arrow.up.circle.fill")
                                        .foregroundStyle(m.type == "in" ? AppColor.success : AppColor.error)
                                    VStack(alignment: .leading) {
                                        Text(m.reason ?? "")
                                            .font(.subheadline)
                                        Text(m.note ?? "")
                                            .font(.caption)
                                            .foregroundStyle(.secondary)
                                    }
                                    Spacer()
                                    Text(String(format: "%.2f MAD", m.amount ?? 0))
                                        .font(.subheadline.weight(.medium))
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle("Gestion caisse")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Fermer") {
                        viewModel.showCashMovements = false
                    }
                }
            }
        }
    }

    // MARK: - Receipt

    private var receiptSheet: some View {
        NavigationStack {
            VStack(spacing: 16) {
                Spacer()

                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 60))
                    .foregroundStyle(.green)

                Text("Vente effectuée")
                    .font(.title2.weight(.bold))

                if let result = viewModel.checkoutResult {
                    VStack(spacing: 4) {
                        Text(result.document_number ?? "")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        Text(String(format: "Total: %.2f MAD", result.total ?? 0))
                            .font(.title3.weight(.semibold))
                        if let change = result.change_amount, change > 0 {
                            Text("Monnaie rendue: \(String(format: "%.2f MAD", change))")
                                .font(.subheadline)
                                .foregroundStyle(AppColor.success)
                        }
                        if let name = result.customer_name, name != "Client Comptoir" {
                            Text(name)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                }

                Spacer()

                StockButton("Nouvelle vente", variant: .primary) {
                    viewModel.showReceipt = false
                    viewModel.checkoutResult = nil
                }
                .padding(.horizontal)

                StockButton("Fermer", variant: .secondary) {
                    viewModel.showReceipt = false
                    viewModel.checkoutResult = nil
                }
                .padding(.horizontal)
                .padding(.bottom, 16)
            }
        }
    }
}

// MARK: - Cart Item Row

struct CartItemRowView: View {
    let item: CartItem
    let onIncrement: () -> Void
    let onDecrement: () -> Void
    let onRemove: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text(item.productName)
                    .font(.subheadline.weight(.medium))
                    .lineLimit(1)
                Text(item.formattedUnitPrice)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                if item.discountPercent > 0 {
                    Text("–\(Int(item.discountPercent))%")
                        .font(.caption2)
                        .foregroundStyle(AppColor.success)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            HStack(spacing: 8) {
                Button(action: onDecrement) {
                    Image(systemName: "minus.circle")
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Diminuer quantité")

                Text("\(item.quantity)")
                    .font(.body.weight(.medium))
                    .frame(minWidth: 24)

                Button(action: onIncrement) {
                    Image(systemName: "plus.circle")
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Augmenter quantité")
            }

            Text(item.formattedLineTotal)
                .font(.subheadline.weight(.semibold))
                .frame(minWidth: 70, alignment: .trailing)

            Button(action: onRemove) {
                Image(systemName: "xmark.circle.fill")
                    .font(.caption)
                    .foregroundStyle(.tertiary)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Retirer du panier")
        }
        .padding(.vertical, 4)
    }
}

// MARK: - POS Scanner Sheet

struct POSScannerSheet: View {
    let onScan: (String) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var captureSession: AVCaptureSession?
    @State private var hasCameraPermission = false
    @State private var showCameraAlert = false
    @State private var manualCode = ""
    @State private var lastScannedCode: String?

    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                if let session = captureSession, hasCameraPermission {
                    BarcodeScannerPreview(session: session) { code in
                        guard code != lastScannedCode else { return }
                        lastScannedCode = code
                        onScan(code)
                        dismiss()
                    }
                    .frame(height: 260)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .overlay(
                        ScanningFrame()
                            .stroke(AppColor.accent, lineWidth: 2)
                            .frame(width: 200, height: 120)
                    )
                    .padding(.horizontal)
                } else if !hasCameraPermission {
                    cameraPermissionView
                } else {
                    ProgressView("Activation de la caméra...")
                        .frame(height: 260)
                }

                VStack(spacing: 8) {
                    Text("Ou saisissez le code-barres")
                        .font(.caption)
                        .foregroundStyle(.secondary)

                    HStack(spacing: 8) {
                        StockTextField("Code-barres...", text: $manualCode, variant: .barcode)
                            .onSubmit {
                                guard !manualCode.isEmpty else { return }
                                onScan(manualCode)
                                manualCode = ""
                                dismiss()
                            }
                        Button("OK") {
                            guard !manualCode.isEmpty else { return }
                            onScan(manualCode)
                            manualCode = ""
                            dismiss()
                        }
                        .buttonStyle(.borderedProminent)
                    }
                    .padding(.horizontal)
                }

                Spacer()
            }
            .padding(.top)
            .background(AppColor.background)
            .navigationTitle("Scanner")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Fermer") {
                        stopSession()
                        dismiss()
                    }
                }
            }
            .alert("Caméra", isPresented: $showCameraAlert) {
                Button("OK", role: .cancel) {}
            } message: {
                Text("Autorisez l'accès à la caméra dans Réglages pour scanner les codes-barres.")
            }
            .task {
                await requestCameraAccess()
            }
            .onChange(of: hasCameraPermission) { granted in
                if granted { configureSession() }
            }
            .onDisappear {
                stopSession()
            }
        }
    }

    private var cameraPermissionView: some View {
        VStack(spacing: 12) {
            Image(systemName: "camera.metering.unknown")
                .font(.system(size: 40))
                .foregroundStyle(.tertiary)
            Text("Caméra non disponible")
                .font(.subheadline)
            Button("Autoriser dans Réglages") {
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(url)
                }
            }
        }
        .frame(height: 260)
    }

    private func requestCameraAccess() async {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            hasCameraPermission = true
        case .notDetermined:
            hasCameraPermission = await AVCaptureDevice.requestAccess(for: .video)
        default:
            showCameraAlert = true
        }
    }

    private func configureSession() {
        let session = AVCaptureSession()
        session.sessionPreset = .high
        guard let device = AVCaptureDevice.default(for: .video),
              let input = try? AVCaptureDeviceInput(device: device),
              session.canAddInput(input)
        else { return }
        session.addInput(input)
        captureSession = session
        Task.detached(priority: .background) {
            session.startRunning()
        }
    }

    private func stopSession() {
        guard let session = captureSession, session.isRunning else { return }
        Task.detached(priority: .background) {
            session.stopRunning()
        }
    }
}

private struct ScanningFrame: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        let c: CGFloat = 20
        path.move(to: CGPoint(x: rect.minX, y: rect.minY + c))
        path.addLine(to: CGPoint(x: rect.minX, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.minX + c, y: rect.minY))
        path.move(to: CGPoint(x: rect.maxX - c, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.minY + c))
        path.move(to: CGPoint(x: rect.maxX, y: rect.maxY - c))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
        path.addLine(to: CGPoint(x: rect.maxX - c, y: rect.maxY))
        path.move(to: CGPoint(x: rect.minX + c, y: rect.maxY))
        path.addLine(to: CGPoint(x: rect.minX, y: rect.maxY))
        path.addLine(to: CGPoint(x: rect.minX, y: rect.maxY - c))
        return path
    }
}
