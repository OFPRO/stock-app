import Foundation

final class CustomerService: CustomerServiceProtocol {
    private let api = APIClient.shared

    func fetchCustomers(search: String? = nil) async throws -> [CustomerListItem] {
        typealias Response = [CustomerDTO]
        let dtos: Response = try await api.request(.customers(search: search))
        return dtos.map { $0.toItem() }
    }

    func fetchCustomer(id: Int) async throws -> CustomerDTO {
        try await api.request(.customer(id))
    }

    func createCustomer(_ request: CustomerCreateRequest) async throws {
        let _: EmptyResponse = try await api.request(.createCustomer, body: request)
    }

    func updateCustomer(id: Int, _ request: CustomerUpdateRequest) async throws {
        let _: EmptyResponse = try await api.request(.updateCustomer(id), body: request)
    }

    func deleteCustomer(id: Int) async throws {
        try await api.requestVoid(.deleteCustomer(id))
    }
}

final class MockCustomerService: CustomerServiceProtocol {
    func fetchCustomers(search: String? = nil) async throws -> [CustomerListItem] {
        let all = [
            CustomerListItem(id: 1, name: "École Al Mansour", clientCode: "CLI-20260501-001", phone: "0612345678", email: "contact@almansour.ma", type: "school", isLoyal: true),
            CustomerListItem(id: 2, name: "Université Cadi Ayyad", clientCode: "CLI-20260501-002", phone: "0623456789", email: "", type: "school", isLoyal: true),
            CustomerListItem(id: 3, name: "Ahmed Benali", clientCode: "CLI-20260502-003", phone: "0634567890", email: "ahmed@email.ma", type: "student", isLoyal: false),
            CustomerListItem(id: 4, name: "Fatima Zahra", clientCode: "CLI-20260502-004", phone: "0645678901", email: "", type: "particulier", isLoyal: false),
            CustomerListItem(id: 5, name: "Librairie Al Qalam", clientCode: "CLI-20260503-005", phone: "0656789012", email: "contact@alqalam.ma", type: "company", isLoyal: true),
            CustomerListItem(id: 6, name: "Sara El Idrissi", clientCode: "CLI-20260503-006", phone: "0667890123", email: "sara@email.ma", type: "student", isLoyal: false),
        ]
        if let s = search, !s.isEmpty {
            return all.filter { $0.name.localizedCaseInsensitiveContains(s) || $0.clientCode.localizedCaseInsensitiveContains(s) || $0.email.localizedCaseInsensitiveContains(s) }
        }
        return all
    }

    func fetchCustomer(id: Int) async throws -> CustomerDTO {
        CustomerDTO(id: id, name: "Client Exemple", type: "particulier", email: "client@email.ma", phone: "0600000000", address: "15 Rue de la Liberté, Marrakech", client_code: "CLI-20260501-001", discount_rate: 0, is_loyal: false, is_active: true, ice: nil, notes: nil)
    }

    func createCustomer(_ request: CustomerCreateRequest) async throws {}
    func updateCustomer(id: Int, _ request: CustomerUpdateRequest) async throws {}
    func deleteCustomer(id: Int) async throws {}
}

struct EmptyResponse: Decodable {}
