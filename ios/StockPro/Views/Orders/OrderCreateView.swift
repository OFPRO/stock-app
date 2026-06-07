import SwiftUI

struct OrderCreateView: View {
    @StateObject private var vm = OrderCreateViewModel()
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        Form {
            supplierSection
            warehouseSection
            itemsSection
            notesSection
            totalSection
        }
        .navigationTitle("Nouvelle Commande")
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Créer") {
                    Task { await vm.submit() }
                }
                .disabled(vm.isSaving || !vm.isValid)
            }
        }
        .task { await vm.loadInitialData() }
        .onChange(of: vm.successMessage) { _ in
            if vm.successMessage != nil {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) { dismiss() }
            }
        }
        .overlay {
            if vm.isSaving {
                ProgressView("Création...")
                    .padding(24)
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 12))
            }
        }
        .alert("Erreur", isPresented: .init(get: { vm.errorMessage != nil }, set: { if !$0 { vm.errorMessage = nil } })) {
            Button("OK") {}
        } message: {
            Text(vm.errorMessage ?? "")
        }
    }

    private var supplierSection: some View {
        Section("Fournisseur") {
            if vm.suppliers.isEmpty {
                ProgressView()
            } else {
                Picker("Fournisseur", selection: $vm.selectedSupplier) {
                    Text("Sélectionner").tag(nil as SupplierListItem?)
                    ForEach(vm.suppliers) { supplier in
                        Text(supplier.name).tag(supplier as SupplierListItem?)
                    }
                }
            }
        }
    }

    private var warehouseSection: some View {
        Section("Entrepôt") {
            if vm.warehouses.isEmpty {
                ProgressView()
            } else {
                Picker("Entrepôt", selection: $vm.selectedWarehouse) {
                    Text("Sélectionner").tag(nil as WarehouseListItem?)
                    ForEach(vm.warehouses) { warehouse in
                        Text(warehouse.name).tag(warehouse as WarehouseListItem?)
                    }
                }
            }
        }
    }

    private var itemsSection: some View {
        Section("Articles") {
            if vm.lineItems.isEmpty {
                Text("Aucun article").foregroundStyle(.secondary)
            }
            ForEach(vm.lineItems.indices, id: \.self) { index in
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(vm.lineItems[index].productName)
                            .font(.subheadline.weight(.medium))
                        HStack(spacing: 4) {
                            TextField("Qté", value: $vm.lineItems[index].quantity, format: .number)
                                .keyboardType(.numberPad)
                                .frame(width: 50)
                                .textFieldStyle(.roundedBorder)
                            Text("×")
                            TextField("Prix", value: $vm.lineItems[index].unitPrice, format: .currency(code: "MAD"))
                                .keyboardType(.decimalPad)
                                .frame(width: 80)
                                .textFieldStyle(.roundedBorder)
                        }
                    }
                    Spacer()
                    Text(vm.lineItems[index].formattedTotal)
                        .font(.subheadline.weight(.semibold))
                }
            }
            .onDelete { vm.lineItems.remove(atOffsets: $0) }

            NavigationLink {
                OrderProductPickerView(products: vm.products) { product in
                    vm.addItem(product)
                }
            } label: {
                Label("Ajouter un article", systemImage: "plus.circle")
            }
        }
    }

    private var notesSection: some View {
        Section("Notes") {
            TextField("Notes (optionnel)", text: $vm.notes, axis: .vertical)
                .lineLimit(3...6)
        }
    }

    private var totalSection: some View {
        Section {
            HStack {
                Text("Total estimé")
                    .font(.headline)
                Spacer()
                Text(vm.total.formatted(.currency(code: "MAD")))
                    .font(.headline.weight(.bold))
            }
        }
    }
}

struct OrderProductPickerView: View {
    let products: [ForSaleProductDTO]
    let onSelect: (ForSaleProductDTO) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var search = ""

    var filtered: [ForSaleProductDTO] {
        if search.isEmpty { return products }
        return products.filter { $0.name.localizedCaseInsensitiveContains(search) || $0.sku.localizedCaseInsensitiveContains(search) }
    }

    var body: some View {
        OrderProductPickerList(filtered: filtered, onSelect: { product in onSelect(product); dismiss() })
            .searchable(text: $search, prompt: "Rechercher un produit")
            .navigationTitle("Choisir un produit")
    }
}

struct OrderProductPickerList: View {
    let filtered: [ForSaleProductDTO]
    let onSelect: (ForSaleProductDTO) -> Void

    var body: some View {
        List {
            OrderProductPickerSection(filtered: filtered, onSelect: onSelect)
        }
        .listStyle(.plain)
    }
}

struct OrderProductPickerSection: View {
    let filtered: [ForSaleProductDTO]
    let onSelect: (ForSaleProductDTO) -> Void

    var body: some View {
        Section {
            ForEach(0..<filtered.count, id: \.self) { index in
                OrderProdPickerRow(product: filtered[index], action: { onSelect(filtered[index]) })
            }
        }
    }
}

struct OrderProdPickerRow: View {
    let product: ForSaleProductDTO
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(product.name).font(.subheadline.weight(.medium))
                    Text(product.sku).font(.caption).foregroundStyle(.secondary)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text(product.formattedPrice).font(.subheadline.weight(.semibold))
                    Text("Stock: \(product.quantity)")
                        .font(.caption2)
                        .foregroundStyle(product.stockStatus == .low ? .orange : .secondary)
                }
            }
            .padding(.vertical, 4)
        }
        .buttonStyle(.plain)
    }
}
