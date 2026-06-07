import SwiftUI

struct NotificationListView: View {
    @StateObject private var vm = NotificationListViewModel()

    var body: some View {
        List {
            switch vm.state {
            case .loading:
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .listRowBackground(Color.clear)
            case .loaded:
                if vm.unreadCount > 0 {
                    Section {
                        Button("Tout marquer comme lu") { Task { await vm.markAllRead() } }
                            .font(.subheadline)
                    }
                }
                ForEach(notifications, id: \.id) { notification in
                    NotificationRow(notification: notification)
                        .onTapGesture { Task { await vm.markRead(notification) } }
                }
            case .empty:
                ContentUnavailableView(
                    "Aucune notification", systemImage: "bell",
                    description: Text("Vous serez notifié des stocks faibles et des commandes")
                )
            case .error(let err):
                StockErrorView(message: err.errorDescription ?? "Erreur", onRetry: { Task { await vm.load() } })
            default:
                Color.clear
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Notifications")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                if vm.unreadCount > 0 {
                    Button("Tout lu") { Task { await vm.markAllRead() } }
                        .font(.subheadline)
                }
            }
        }
        .task { await vm.load() }
        .refreshable { await vm.load() }
    }

    private var notifications: [NotificationDTO] {
        guard case .loaded(let notifs) = vm.state else { return [] }
        return notifs
    }
}

struct NotificationRow: View {
    let notification: NotificationDTO

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Circle()
                .fill(notification.is_read ? Color.clear : Color.accentColor)
                .frame(width: 10, height: 10)
                .padding(.top, 6)

            VStack(alignment: .leading, spacing: 4) {
                Text(notification.message)
                    .font(.subheadline)
                    .fontWeight(notification.is_read ? .regular : .medium)
                HStack {
                    if let type = notification.type {
                        NotificationTypeBadge(type: type)
                    }
                    Text(notification.created_at)
                        .font(.caption2)
                        .foregroundStyle(.tertiary)
                }
            }
        }
        .padding(.vertical, 4)
        .opacity(notification.is_read ? 0.7 : 1)
    }
}

struct NotificationTypeBadge: View {
    let type: String

    var body: some View {
        Text(type == "warning" ? "Alerte" : type == "info" ? "Info" : type)
            .font(.caption2.weight(.semibold))
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(color.opacity(0.15))
            .foregroundStyle(color)
            .clipShape(Capsule())
    }

    private var color: Color {
        switch type {
        case "warning": .orange
        case "info": .blue
        case "error": .red
        default: .gray
        }
    }
}
