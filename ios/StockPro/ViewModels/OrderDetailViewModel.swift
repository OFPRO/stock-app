import Foundation

@MainActor
final class OrderDetailViewModel: ObservableObject {
    @Published var order: OrderDTO?
    @Published var items: [OrderItemDTO] = []
    @Published var state: ViewState<OrderDTO> = .loading
    @Published var itemsState: ViewState<[OrderItemDTO]> = .loading

    private let orderService: OrderServiceProtocol
    let orderId: Int

    init(orderId: Int, orderService: OrderServiceProtocol = OrderService()) {
        self.orderId = orderId
        self.orderService = orderService
    }

    func load() async {
        state = .loading
        itemsState = .loading
        do {
            async let orderTask = orderService.fetchOrderItems(id: orderId)
            async let itemsTask = orderService.fetchOrderItems(id: orderId)
            let (_, fetchedItems) = try await (orderTask, itemsTask)
            items = fetchedItems
            itemsState = .loaded(fetchedItems)
        } catch {
            itemsState = .error(.from(error))
        }
    }
}
