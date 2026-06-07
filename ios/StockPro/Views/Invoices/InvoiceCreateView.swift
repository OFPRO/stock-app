import SwiftUI

struct InvoiceCreateView: View {
    @StateObject private var vm = InvoiceCreateViewModel()
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        Form {
            customerSection
            itemsSection
            paymentSection
            notesSection
            totalSection
        }
        .navigationTitle("Nouvelle Facture")
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Créer") {
                    Task { await vm.submit() }
                }
                .disabled(vm.isSaving || vm.lineItems.isEmpty)
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

    private var customerSection: some View {
        Section("Client") {
            if vm.customers.isEmpty {
                ProgressView()
            } else {
                Picker("Client", selection: $vm.selectedCustomer) {
                    Text("Aucun").tag(nil as CustomerListItem?)
                    ForEach(vm.customers) { customer in
                        Text(customer.name).tag(customer as CustomerListItem?)
                    }
                }
                .pickerStyle(.menu)
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
                            Text(vm.lineItems[index].formattedPrice)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                    Spacer()
                    Text(vm.lineItems[index].formattedTotal)
                        .font(.subheadline.weight(.semibold))
                }
            }
            .onDelete { vm.lineItems.remove(atOffsets: $0) }

            NavigationLink {
                ProductPickerView(products: vm.products) { product in
                    vm.addItem(product)
                }
            } label: {
                Label("Ajouter un article", systemImage: "plus.circle")
            }
        }
    }

    private var paymentSection: some View {
        Section("Paiement") {
            Picker("Méthode", selection: $vm.paymentMethod) {
                Text("Espèces").tag("especes")
                Text("Carte").tag("carte")
                Text("Chèque").tag("cheque")
                Text("Virement").tag("virement")
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
                Text("Total")
                    .font(.headline)
                Spacer()
                Text(vm.total.formatted(.currency(code: "MAD")))
                    .font(.headline.weight(.bold))
            }
        }
    }
}

struct ProductPickerView: View {
    let products: [ForSaleProductDTO]
    let onSelect: (ForSaleProductDTO) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var search = ""

    var filtered: [ForSaleProductDTO] {
        if search.isEmpty { return products }
        return products.filter { $0.name.localizedCaseInsensitiveContains(search) || $0.sku.localizedCaseInsensitiveContains(search) }
    }

    var body: some View {
        ProductPickerList(filtered: filtered, onSelect: { product in onSelect(product); dismiss() })
            .searchable(text: $search, prompt: "Rechercher un produit")
            .navigationTitle("Choisir un produit")
    }
}

struct ProductPickerList: View {
    let filtered: [ForSaleProductDTO]
    let onSelect: (ForSaleProductDTO) -> Void

    var body: some View {
        ProductPickerContent(items: filtered, onSelect: onSelect)
    }
}

struct ProductPickerContent: View {
    let items: [ForSaleProductDTO]
    let onSelect: (ForSaleProductDTO) -> Void

    var body: some View {
        List {
            ProductPickerSection(items: items, onSelect: onSelect)
        }
        .listStyle(.plain)
    }
}

struct ProductPickerSection: View {
    let items: [ForSaleProductDTO]
    let onSelect: (ForSaleProductDTO) -> Void

    var body: some View {
        Section {
            ForEach(0..<items.count, id: \.self) { index in
                ProdPickerRow(product: items[index], action: { onSelect(items[index]) })
            }
        }
    }
}

struct ProdPickerRow: View {
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
