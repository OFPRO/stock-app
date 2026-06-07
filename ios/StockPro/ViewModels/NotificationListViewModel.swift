import Foundation

@MainActor
final class NotificationListViewModel: ObservableObject {
    @Published var state: ViewState<[NotificationDTO]> = .loading

    private let notificationService: NotificationServiceProtocol

    init(notificationService: NotificationServiceProtocol = NotificationService()) {
        self.notificationService = notificationService
    }

    var unreadCount: Int {
        guard case .loaded(let notifications) = state else { return 0 }
        return notifications.filter { !$0.is_read }.count
    }

    func load() async {
        state = .loading
        do {
            let notifications = try await notificationService.fetchNotifications(warehouseId: nil)
            state = notifications.isEmpty ? .empty("") : .loaded(notifications)
        } catch {
            state = .error(.from(error))
        }
    }

    func markRead(_ notification: NotificationDTO) async {
        guard !notification.is_read else { return }
        try? await notificationService.markRead(id: notification.id)
        await load()
    }

    func markAllRead() async {
        try? await notificationService.markAllRead()
        await load()
    }
}
