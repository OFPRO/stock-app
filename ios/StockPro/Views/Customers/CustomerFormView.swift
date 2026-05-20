import SwiftUI

enum CustomerFormMode {
    case create
    case edit(CustomerDTO)

    var title: String {
        switch self {
        case .create: "Nouveau client"
        case .edit: "Modifier le client"
        }
    }

    var buttonLabel: String {
        switch self {
        case .create: "Créer"
        case .edit: "Enregistrer"
        }
    }
}

struct CustomerFormView: View {
    @Environment(\.dismiss) private var dismiss
    let mode: CustomerFormMode

    @State private var name = ""
    @State private var type = "particulier"
    @State private var email = ""
    @State private var phone = ""
    @State private var address = ""
    @State private var isLoyal = false
    @State private var notes = ""
    @State private var isLoading = false
    @State private var errorMessage: String?

    private let customerService: CustomerServiceProtocol
    private let customerTypes = [
        ("particulier", "Particulier"),
        ("student", "Étudiant"),
        ("school", "École"),
        ("company", "Société"),
    ]

    init(mode: CustomerFormMode, customerService: CustomerServiceProtocol = CustomerService()) {
        self.mode = mode
        self.customerService = customerService
        if case .edit(let customer) = mode {
            _name = State(initialValue: customer.name)
            _type = State(initialValue: customer.type ?? "particulier")
            _email = State(initialValue: customer.email ?? "")
            _phone = State(initialValue: customer.phone ?? "")
            _address = State(initialValue: customer.address ?? "")
            _isLoyal = State(initialValue: customer.is_loyal ?? false)
            _notes = State(initialValue: customer.notes ?? "")
        }
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Informations") {
                    StockFormRow("Nom", required: true) {
                        TextField("Nom du client", text: $name)
                    }
                    StockPickerRow(label: "Type", selection: $type, options: customerTypes)
                    StockFormRow("Email") {
                        TextField("client@email.ma", text: $email)
                            .keyboardType(.emailAddress)
                            .autocapitalization(.none)
                    }
                    StockFormRow("Téléphone") {
                        TextField("0600000000", text: $phone)
                            .keyboardType(.phonePad)
                    }
                    StockFormRow("Adresse") {
                        TextField("Adresse", text: $address, axis: .vertical)
                            .lineLimit(3)
                    }
                }

                Section {
                    Toggle("Client Loyal", isOn: $isLoyal)
                }

                Section("Notes") {
                    TextField("Notes...", text: $notes, axis: .vertical)
                        .lineLimit(4)
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
                let req = CustomerCreateRequest(
                    name: name, type: type,
                    email: email.isEmpty ? nil : email,
                    phone: phone.isEmpty ? nil : phone,
                    address: address.isEmpty ? nil : address,
                    discount_rate: nil, is_loyal: isLoyal,
                    notes: notes.isEmpty ? nil : notes
                )
                try await customerService.createCustomer(req)
            case .edit(let customer):
                let req = CustomerUpdateRequest(
                    name: name, type: type,
                    email: email.isEmpty ? nil : email,
                    phone: phone.isEmpty ? nil : phone,
                    address: address.isEmpty ? nil : address,
                    discount_rate: nil, is_loyal: isLoyal,
                    notes: notes.isEmpty ? nil : notes
                )
                try await customerService.updateCustomer(id: customer.id, req)
            }
            dismiss()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
