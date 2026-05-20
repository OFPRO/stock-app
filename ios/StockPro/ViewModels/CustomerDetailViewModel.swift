import Foundation

@MainActor
final class CustomerDetailViewModel: ObservableObject {
    @Published var state: ViewState<CustomerDTO> = .loading
    @Published var showDeleteConfirmation = false

    private let customerService: CustomerServiceProtocol
    let customerId: Int

    init(customerId: Int, customerService: CustomerServiceProtocol = CustomerService()) {
        self.customerId = customerId
        self.customerService = customerService
    }

    func load() async {
        state = .loading
        do {
            let customer = try await customerService.fetchCustomer(id: customerId)
            state = .loaded(customer)
        } catch {
            state = .error(.from(error))
        }
    }

    func deleteCustomer() async -> Bool {
        do {
            try await customerService.deleteCustomer(id: customerId)
            return true
        } catch {
            if case .loaded = state {
                // Keep current state
            }
            return false
        }
    }
}
