import SwiftUI

enum ProductFormMode {
    case create
    case edit(productId: Int)

    var title: String {
        switch self {
        case .create: "Nouveau produit"
        case .edit: "Modifier le produit"
        }
    }

    var buttonLabel: String {
        switch self {
        case .create: "Créer"
        case .edit: "Enregistrer"
        }
    }
}

struct ProductFormView: View {
    @Environment(\.dismiss) private var dismiss
    let mode: ProductFormMode

    @State private var name = ""
    @State private var sku = ""
    @State private var description = ""
    @State private var price: String = ""
    @State private var purchasePrice: String = ""
    @State private var wholesalePrice: String = ""
    @State private var category = ""
    @State private var barcode = ""
    @State private var minStock: String = ""
    @State private var maxStock: String = ""
    @State private var weight: String = ""
    @State private var unit = "pièce"
    @State private var categories: [CategoryDTO] = []
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var showCategoryPicker = false

    private let productService: ProductServiceProtocol

    init(mode: ProductFormMode, productService: ProductServiceProtocol = ProductService()) {
        self.mode = mode
        self.productService = productService
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Informations générales") {
                    StockFormRow("Nom", required: true) {
                        TextField("Nom du produit", text: $name)
                    }
                    StockFormRow("SKU", required: true) {
                        TextField("Référence", text: $sku)
                    }
                    StockFormRow("Description") {
                        TextField("Description", text: $description, axis: .vertical)
                            .lineLimit(3)
                    }
                }

                Section("Prix") {
                    StockFormRow("Prix de vente") {
                        TextField("0,00", text: $price)
                            .keyboardType(.decimalPad)
                    }
                    StockFormRow("Prix d'achat") {
                        TextField("0,00", text: $purchasePrice)
                            .keyboardType(.decimalPad)
                    }
                    StockFormRow("Prix de gros") {
                        TextField("0,00", text: $wholesalePrice)
                            .keyboardType(.decimalPad)
                    }
                }

                Section("Stock") {
                    StockFormRow("Stock minimum") {
                        TextField("0", text: $minStock)
                            .keyboardType(.numberPad)
                    }
                    StockFormRow("Stock maximum") {
                        TextField("0", text: $maxStock)
                            .keyboardType(.numberPad)
                    }
                    StockFormRow("Unité") {
                        TextField("pièce", text: $unit)
                    }
                }

                Section("Classification") {
                    Button {
                        showCategoryPicker = true
                    } label: {
                        HStack {
                            Text("Catégorie")
                            Spacer()
                            Text(category.isEmpty ? "Sélectionner" : category)
                                .foregroundStyle(category.isEmpty ? .tertiary : .primary)
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundStyle(.tertiary)
                        }
                    }
                    StockFormRow("Code-barres") {
                        TextField("Code-barres", text: $barcode)
                            .keyboardType(.numberPad)
                    }
                    StockFormRow("Poids (kg)") {
                        TextField("0", text: $weight)
                            .keyboardType(.decimalPad)
                    }
                }

                if let error = errorMessage {
                    Section {
                        Text(error)
                            .font(.caption)
                            .foregroundStyle(AppColor.error)
                    }
                }

                Section {
                    Button(mode.buttonLabel) {
                        Task { await save() }
                    }
                    .frame(maxWidth: .infinity)
                    .fontWeight(.semibold)
                    .disabled(isLoading || name.isEmpty || sku.isEmpty)
                }
            }
            .navigationTitle(mode.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuler") { dismiss() }
                }
            }
            .overlay {
                if isLoading {
                    ProgressView()
                }
            }
            .sheet(isPresented: $showCategoryPicker) {
                categoryPickerSheet
            }
            .task {
                await loadCategories()
            }
        }
    }

    private var categoryPickerSheet: some View {
        NavigationStack {
            List(categories) { cat in
                Button {
                    category = cat.name
                    showCategoryPicker = false
                } label: {
                    HStack {
                        Text(cat.name)
                        Spacer()
                        if cat.name == category {
                            Image(systemName: "checkmark")
                                .foregroundStyle(AppColor.accent)
                        }
                    }
                }
            }
            .navigationTitle("Catégorie")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Fermer") { showCategoryPicker = false }
                }
            }
        }
    }

    private func loadCategories() async {
        do {
            categories = try await productService.fetchCategories()
        } catch {
            categories = [
                CategoryDTO(name: "Fournitures", count: nil),
                CategoryDTO(name: "Informatique", count: nil),
                CategoryDTO(name: "Papeterie", count: nil),
                CategoryDTO(name: "Mobilier", count: nil),
            ]
        }
    }

    private func save() async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        let priceVal = Double(price.replacingOccurrences(of: ",", with: ".")) ?? 0
        let purchaseVal = Double(purchasePrice.replacingOccurrences(of: ",", with: "."))
        let wholesaleVal = Double(wholesalePrice.replacingOccurrences(of: ",", with: "."))
        let minVal = Int(minStock)
        let maxVal = Int(maxStock)
        let weightVal = Double(weight.replacingOccurrences(of: ",", with: "."))

        do {
            switch mode {
            case .create:
                let req = ProductCreateRequest(
                    name: name, sku: sku, description: description.isEmpty ? nil : description,
                    price: priceVal, category: category.isEmpty ? nil : category,
                    barcode: barcode.isEmpty ? nil : barcode,
                    min_stock: minVal, max_stock: maxVal,
                    weight: weightVal, unit: unit,
                    purchase_price: purchaseVal, wholesale_price: wholesaleVal
                )
                _ = try await productService.createProduct(req)
            case .edit(let id):
                let req = ProductUpdateRequest(
                    name: name, sku: sku, description: description.isEmpty ? nil : description,
                    price: priceVal, category: category.isEmpty ? nil : category,
                    barcode: barcode.isEmpty ? nil : barcode,
                    min_stock: minVal, max_stock: maxVal,
                    is_active: nil, weight: weightVal, unit: unit,
                    purchase_price: purchaseVal, wholesale_price: wholesaleVal
                )
                _ = try await productService.updateProduct(id: id, req)
            }
            dismiss()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
