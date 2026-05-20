import SwiftUI

enum LocationFormMode {
    case create
    case edit(LocationListItem)

    var title: String {
        switch self {
        case .create: "Nouvelle zone"
        case .edit: "Modifier la zone"
        }
    }

    var buttonLabel: String {
        switch self {
        case .create: "Créer"
        case .edit: "Enregistrer"
        }
    }
}

struct LocationFormView: View {
    @Environment(\.dismiss) private var dismiss
    let mode: LocationFormMode

    @State private var name = ""
    @State private var selectedWarehouseId: Int = 0
    @State private var type = "rack"
    @State private var capacity: String = ""
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var warehouses: [WarehouseListItem] = []

    private let locationService: LocationServiceProtocol
    private let warehouseService: WarehouseServiceProtocol
    private let locationTypes = [
        ("rack", "Rayon"),
        ("shelf", "Étagère"),
        ("zone", "Zone"),
    ]

    init(mode: LocationFormMode,
         locationService: LocationServiceProtocol = LocationService(),
         warehouseService: WarehouseServiceProtocol = WarehouseService()) {
        self.mode = mode
        self.locationService = locationService
        self.warehouseService = warehouseService
        if case .edit(let location) = mode {
            _name = State(initialValue: location.name)
            _selectedWarehouseId = State(initialValue: location.warehouseId)
            _type = State(initialValue: location.type)
            _capacity = State(initialValue: location.capacity.map(String.init) ?? "")
        }
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Informations") {
                    StockFormRow("Nom", required: true) {
                        TextField("Nom de la zone", text: $name)
                    }

                    StockPickerRow(label: "Type", selection: $type, options: locationTypes)
                }

                Section("Entrepôt") {
                    if !warehouses.isEmpty {
                        Picker("Entrepôt", selection: $selectedWarehouseId) {
                            ForEach(warehouses) { warehouse in
                                Text(warehouse.name).tag(warehouse.id)
                            }
                        }
                        .pickerStyle(.menu)
                    } else {
                        Text("Aucun entrepôt disponible")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }

                Section("Capacité") {
                    StockFormRow("Capacité max") {
                        TextField("Nombre d'unités", text: $capacity)
                            .keyboardType(.numberPad)
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
                    .disabled(isLoading || name.isEmpty || selectedWarehouseId == 0)
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
        .task { await loadWarehouses() }
    }

    private func loadWarehouses() async {
        warehouses = (try? await warehouseService.fetchWarehouses()) ?? []
        if selectedWarehouseId == 0, let first = warehouses.first {
            selectedWarehouseId = first.id
        }
    }

    private func save() async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        do {
            let capacityInt = Int(capacity)

            switch mode {
            case .create:
                let req = LocationCreateRequest(
                    name: name,
                    warehouse_id: selectedWarehouseId,
                    type: type,
                    capacity: capacityInt
                )
                try await locationService.createLocation(req)
            case .edit(let location):
                let req = LocationUpdateRequest(
                    name: name,
                    warehouse_id: selectedWarehouseId,
                    type: type,
                    capacity: capacityInt
                )
                try await locationService.updateLocation(id: location.id, req)
            }
            dismiss()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
