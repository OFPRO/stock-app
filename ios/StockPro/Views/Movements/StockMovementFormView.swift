import SwiftUI

struct StockMovementFormView: View {
    let onCreated: () -> Void
    @Environment(\.dismiss) private var dismiss

    @State private var movementType = "in"
    @State private var selectedProduct: ForSaleProductDTO?
    @State private var quantity = ""
    @State private var selectedLocationId: Int?
    @State private var selectedFromLocationId: Int?
    @State private var selectedToLocationId: Int?
    @State private var note = ""
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var productSearchText = ""
    @State private var products: [ForSaleProductDTO] = []

    private let stockService: StockServiceProtocol = StockService()
    private let productService: ProductServiceProtocol = ProductService()
    private let locationService: LocationServiceProtocol = LocationService()

    @State private var locations: [LocationListItem] = []

    private let types = [
        ("in", "Entrée"),
        ("out", "Sortie"),
        ("transfer", "Transfert"),
    ]

    var body: some View {
        NavigationStack {
            Form {
                typeSection
                productSection
                quantitySection
                locationSection
                noteSection
                if let error = errorMessage {
                    Section {
                        Text(error)
                            .font(.caption)
                            .foregroundStyle(AppColor.error)
                    }
                }
            }
            .navigationTitle("Nouveau mouvement")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuler") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Créer") {
                        Task { await createMovement() }
                    }
                    .fontWeight(.semibold)
                    .disabled(selectedProduct == nil || quantity.isEmpty || isLoading)
                }
            }
            .overlay {
                if isLoading {
                    ProgressView()
                }
            }
            .task { await loadData() }
        }
    }

    private var typeSection: some View {
        Section("Type de mouvement") {
            Picker("Type", selection: $movementType) {
                ForEach(types, id: \.0) { type, label in
                    Text(label).tag(type)
                }
            }
            .pickerStyle(.segmented)
        }
    }

    private var productSection: some View {
        Section("Produit") {
            if let product = selectedProduct {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(product.name)
                            .font(.subheadline.weight(.medium))
                        Text("Stock: \(product.quantity)")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    Button("Changer") {
                        selectedProduct = nil
                        productSearchText = ""
                    }
                    .font(.caption)
                }
            } else {
                StockTextField("Rechercher un produit...", text: $productSearchText, variant: .search)
                    .onChange(of: productSearchText) { _, _ in searchProducts() }

                if !products.isEmpty {
                    List {
                        ForEach(products) { product in
                            Button {
                                selectedProduct = product
                                productSearchText = ""
                                products = []
                            } label: {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(product.name)
                                        .font(.subheadline.weight(.medium))
                                        .foregroundStyle(.primary)
                                    Text("Stock: \(product.quantity) · \(String(format: "%.2f", product.price)) MAD")
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                }
                            }
                        }
                    }
                    .listStyle(.plain)
                    .frame(maxHeight: 200)
                }
            }
        }
    }

    private var quantitySection: some View {
        Section("Quantité") {
            StockTextField("Quantité", text: $quantity, variant: .default)
                .keyboardType(.numberPad)
        }
    }

    private var locationSection: some View {
        Group {
            if movementType == "in" || movementType == "out" {
                Section("Emplacement") {
                    Picker("Emplacement", selection: $selectedLocationId) {
                        Text("Sélectionner...").tag(nil as Int?)
                        ForEach(locations) { location in
                            Text(location.name).tag(location.id as Int?)
                        }
                    }
                }
            } else if movementType == "transfer" {
                Section("De → Vers") {
                    Picker("De", selection: $selectedFromLocationId) {
                        Text("Sélectionner...").tag(nil as Int?)
                        ForEach(locations) { location in
                            Text(location.name).tag(location.id as Int?)
                        }
                    }
                    Picker("Vers", selection: $selectedToLocationId) {
                        Text("Sélectionner...").tag(nil as Int?)
                        ForEach(locations) { location in
                            Text(location.name).tag(location.id as Int?)
                        }
                    }
                }
            }
        }
    }

    private var noteSection: some View {
        Section("Note (optionnelle)") {
            TextField("Note...", text: $note)
        }
    }

    private func searchProducts() {
        guard !productSearchText.trimmingCharacters(in: .whitespaces).isEmpty else {
            products = []
            return
        }
        Task {
            do {
                let all = try await productService.fetchProductsForSale()
                products = all.filter { $0.name.localizedCaseInsensitiveContains(productSearchText) }
            } catch {}
        }
    }

    private func loadData() async {
        do {
            locations = try await locationService.fetchLocations(warehouseId: nil)
        } catch {
            errorMessage = "Erreur chargement emplacements: \(error.localizedDescription)"
        }
    }

    private func createMovement() async {
        guard let product = selectedProduct, let qty = Int(quantity), qty > 0 else { return }
        isLoading = true
        errorMessage = nil
        do {
            if movementType == "transfer" {
                try await stockService.transferStock(
                    productId: product.id, quantity: qty,
                    fromLocationId: selectedFromLocationId,
                    toLocationId: selectedToLocationId,
                    note: note.isEmpty ? nil : note
                )
            } else {
                try await stockService.createMovement(
                    productId: product.id, type: movementType,
                    quantity: qty,
                    locationId: movementType == "in" ? selectedLocationId : selectedLocationId,
                    note: note.isEmpty ? nil : note
                )
            }
            onCreated()
            dismiss()
        } catch {
            errorMessage = "Erreur: \(error.localizedDescription)"
        }
        isLoading = false
    }
}
