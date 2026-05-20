import SwiftUI

enum SupplierFormMode {
    case create
    case edit(SupplierListItem)

    var title: String {
        switch self {
        case .create: "Nouveau fournisseur"
        case .edit: "Modifier le fournisseur"
        }
    }

    var buttonLabel: String {
        switch self {
        case .create: "Créer"
        case .edit: "Enregistrer"
        }
    }
}

struct SupplierFormView: View {
    @Environment(\.dismiss) private var dismiss
    let mode: SupplierFormMode

    @State private var name = ""
    @State private var email = ""
    @State private var phone = ""
    @State private var address = ""
    @State private var contactPerson = ""
    @State private var isLoading = false
    @State private var errorMessage: String?

    private let supplierService: SupplierServiceProtocol

    init(mode: SupplierFormMode, supplierService: SupplierServiceProtocol = SupplierService()) {
        self.mode = mode
        self.supplierService = supplierService
        if case .edit(let supplier) = mode {
            _name = State(initialValue: supplier.name)
            _email = State(initialValue: supplier.email)
            _phone = State(initialValue: supplier.phone)
            _address = State(initialValue: supplier.address)
            _contactPerson = State(initialValue: supplier.contactPerson)
        }
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Informations") {
                    StockFormRow("Nom", required: true) {
                        TextField("Nom du fournisseur", text: $name)
                    }
                    StockFormRow("Personne de contact") {
                        TextField("Nom du contact", text: $contactPerson)
                    }
                }

                Section("Coordonnées") {
                    StockFormRow("Email") {
                        TextField("fournisseur@email.ma", text: $email)
                            .keyboardType(.emailAddress)
                            .autocapitalization(.none)
                    }
                    StockFormRow("Téléphone") {
                        TextField("0522000000", text: $phone)
                            .keyboardType(.phonePad)
                    }
                    StockFormRow("Adresse") {
                        TextField("Adresse", text: $address, axis: .vertical)
                            .lineLimit(3)
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
                    .disabled(isLoading || name.isEmpty)
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
                if isLoading { ProgressView() }
            }
        }
    }

    private func save() async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        do {
            switch mode {
            case .create:
                let req = SupplierCreateRequest(
                    name: name,
                    email: email.isEmpty ? nil : email,
                    phone: phone.isEmpty ? nil : phone,
                    address: address.isEmpty ? nil : address,
                    contact_person: contactPerson.isEmpty ? nil : contactPerson
                )
                try await supplierService.createSupplier(req)
            case .edit(let supplier):
                let req = SupplierUpdateRequest(
                    name: name,
                    email: email.isEmpty ? nil : email,
                    phone: phone.isEmpty ? nil : phone,
                    address: address.isEmpty ? nil : address,
                    contact_person: contactPerson.isEmpty ? nil : contactPerson
                )
                try await supplierService.updateSupplier(id: supplier.id, req)
            }
            dismiss()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
