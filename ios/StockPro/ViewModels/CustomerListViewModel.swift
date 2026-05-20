import Foundation

@MainActor
final class CustomerListViewModel: ObservableObject {
    @Published var state: ViewState<[CustomerListItem]> = .loading
    @Published var searchQuery = ""

    private let customerService: CustomerServiceProtocol

    init(customerService: CustomerServiceProtocol = CustomerService()) {
        self.customerService = customerService
    }

    var filteredCustomers: [CustomerListItem] {
        guard case .loaded(let customers) = state else { return [] }
        if searchQuery.isEmpty { return customers }
        return customers.filter {
            $0.name.localizedCaseInsensitiveContains(searchQuery) ||
            $0.clientCode.localizedCaseInsensitiveContains(searchQuery) ||
            $0.email.localizedCaseInsensitiveContains(searchQuery) ||
            $0.phone.contains(searchQuery)
        }
    }

    func load() async {
        state = .loading
        do {
            let customers = try await customerService.fetchCustomers(search: nil)
            state = customers.isEmpty ? .empty("") : .loaded(customers)
        } catch {
            state = .error(.from(error))
        }
    }

    func deleteCustomer(at indexSet: IndexSet) async {
        guard case .loaded(let customers) = state else { return }
        for idx in indexSet {
            try? await customerService.deleteCustomer(id: customers[idx].id)
        }
        await load()
    }
}
