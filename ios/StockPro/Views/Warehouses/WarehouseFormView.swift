import SwiftUI

enum WarehouseFormMode {
    case create

    var title: String {
        switch self {
        case .create: "Nouvel entrepôt"
        }
    }

    var buttonLabel: String {
        switch self {
        case .create: "Créer"
        }
    }
}

struct WarehouseFormView: View {
    @Environment(\.dismiss) private var dismiss
    let mode: WarehouseFormMode

    @State private var name = ""
    @State private var address = ""
    @State private var manager = ""
    @State private var isLoading = false
    @State private var errorMessage: String?

    private let warehouseService: WarehouseServiceProtocol

    init(mode: WarehouseFormMode, warehouseService: WarehouseServiceProtocol = WarehouseService()) {
        self.mode = mode
        self.warehouseService = warehouseService
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Informations") {
                    StockFormRow("Nom", required: true) {
                        TextField("Nom de l'entrepôt", text: $name)
                    }
                    StockFormRow("Responsable") {
                        TextField("Nom du responsable", text: $manager)
                    }
                }

                Section("Adresse") {
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
            let req = WarehouseCreateRequest(
                name: name,
                address: address.isEmpty ? nil : address,
                manager: manager.isEmpty ? nil : manager
            )
            try await warehouseService.createWarehouse(req)
            dismiss()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
