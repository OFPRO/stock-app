import SwiftUI

struct MoreMenuView: View {
    @Binding var showSettings: Bool

    let menuItems: [(icon: String, title: String, action: () -> Void)] = []

    var body: some View {
        NavigationStack {
            List {
                Section("Gestion") {
                    NavigationLink(destination: CustomerListView()) {
                        Label("Clients", systemImage: "person.2")
                    }
                    NavigationLink(destination: SupplierListView()) {
                        Label("Fournisseurs", systemImage: "truck")
                    }
                    NavigationLink(destination: WarehouseListView()) {
                        Label("Entrepôts", systemImage: "building.2")
                    }
                    NavigationLink(destination: LocationListView()) {
                        Label("Zones de Stock", systemImage: "square.grid.3x3")
                    }
                    NavigationLink(destination: StockMovementListView()) {
                        Label("Mouvements", systemImage: "arrow.triangle.swap")
                    }
                }

                Section("Commandes & Factures") {
                    NavigationLink(destination: OrderListView()) {
                        Label("Commandes", systemImage: "doc.text")
                    }
                    NavigationLink(destination: InvoiceListView()) {
                        Label("Factures", systemImage: "doc.plaintext")
                    }
                    NavigationLink(destination: NotificationListView()) {
                        Label("Notifications", systemImage: "bell")
                    }
                    NavigationLink(destination: ReorderRuleListView()) {
                        Label("Règles de Réappro", systemImage: "slider.horizontal.3")
                    }
                }

                Section("Rapports") {
                    NavigationLink(destination: ReportsView()) {
                        Label("Rapports", systemImage: "chart.bar")
                    }
                    NavigationLink(destination: SessionHistoryView()) {
                        Label("Historique des Sessions", systemImage: "clock")
                    }
                    NavigationLink(destination: MainAccountView()) {
                        Label("Compte Principal", systemImage: "banknote")
                    }
                }

                Section {
                    UserProfileHeader()
                    Button {
                        showSettings = true
                    } label: {
                        Label("Réglages", systemImage: "gear")
                    }
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle("Plus")
        }
    }
}

struct EmptyPlaceholderView: View {
    let title: String

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "square.stack.3d.up")
                .font(.system(size: 48))
                .foregroundStyle(.tertiary)
            Text(title)
                .font(.title2.weight(.semibold))
            Text("Disponible dans une prochaine mise à jour")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .navigationTitle(title)
    }
}
