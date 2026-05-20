import Foundation

@MainActor
final class SupplierListViewModel: ObservableObject {
    @Published var state: ViewState<[SupplierListItem]> = .loading
    @Published var searchQuery = ""

    private let supplierService: SupplierServiceProtocol

    init(supplierService: SupplierServiceProtocol = SupplierService()) {
        self.supplierService = supplierService
    }

    var filteredSuppliers: [SupplierListItem] {
        guard case .loaded(let suppliers) = state else { return [] }
        if searchQuery.isEmpty { return suppliers }
        return suppliers.filter {
            $0.name.localizedCaseInsensitiveContains(searchQuery) ||
            $0.email.localizedCaseInsensitiveContains(searchQuery) ||
            $0.contactPerson.localizedCaseInsensitiveContains(searchQuery)
        }
    }

    func load() async {
        state = .loading
        do {
            let suppliers = try await supplierService.fetchSuppliers()
            state = suppliers.isEmpty ? .empty("") : .loaded(suppliers)
        } catch {
            state = .error(.from(error))
        }
    }

    func deleteSupplier(at indexSet: IndexSet) async {
        guard case .loaded(let suppliers) = state else { return }
        for idx in indexSet {
            try? await supplierService.deleteSupplier(id: suppliers[idx].id)
        }
        await load()
    }
}
