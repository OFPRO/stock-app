import Foundation

@MainActor
final class OrderListViewModel: ObservableObject {
    @Published var state: ViewState<[OrderDTO]> = .loading
    @Published var filterStatus: String?

    private let orderService: OrderServiceProtocol

    init(orderService: OrderServiceProtocol = OrderService()) {
        self.orderService = orderService
    }

    var filteredOrders: [OrderDTO] {
        guard case .loaded(let orders) = state else { return [] }
        if let status = filterStatus {
            return orders.filter { $0.status == status }
        }
        return orders
    }

    func load() async {
        state = .loading
        do {
            let orders = try await orderService.fetchOrders(warehouseId: nil, status: nil)
            state = orders.isEmpty ? .empty("") : .loaded(orders)
        } catch {
            state = .error(.from(error))
        }
    }

    func deleteOrder(at indexSet: IndexSet) async {
        guard case .loaded(let orders) = state else { return }
        for idx in indexSet {
            try? await orderService.deleteOrder(id: orders[idx].id)
        }
        await load()
    }
}
