import SwiftUI

@main
struct StockProApp: App {
    @State private var isAuthenticated = false
    @State private var showLogin = true

    var body: some Scene {
        WindowGroup {
            if showLogin {
                LoginView(isAuthenticated: $isAuthenticated, showLogin: $showLogin)
            } else {
                MainTabView()
                    .environment(\.diContainer, DIContainer.live)
            }
        }
    }
}

struct MainTabView: View {
    @State private var selectedTab: Tab = .dashboard
    @State private var showSettings = false

    enum Tab: String, CaseIterable {
        case dashboard = "Tableau de Bord"
        case products = "Produits"
        case scanner = "Scanner"
        case pos = "Caisse"
        case more = "Plus"

        var icon: String {
            switch self {
            case .dashboard: "square.grid.2x2"
            case .products: "shippingbox"
            case .scanner: "barcode.viewfinder"
            case .pos: "eurosign.circle"
            case .more: "ellipsis.circle"
            }
        }
    }

    var body: some View {
        TabView(selection: $selectedTab) {
            ForEach(Tab.allCases, id: \.self) { tab in
                tabView(for: tab)
                    .tabItem {
                        Label(tab.rawValue, systemImage: tab.icon)
                    }
                    .tag(tab)
            }
        }
        .sheet(isPresented: $showSettings) {
            SettingsView()
        }
    }

    @ViewBuilder
    private func tabView(for tab: Tab) -> some View {
        switch tab {
        case .dashboard: DashboardView()
        case .products: ProductListView()
        case .scanner: ScannerView()
        case .pos: POSView()
        case .more: MoreMenuView(showSettings: $showSettings)
        }
    }
}

struct UserProfileHeader: View {
    var body: some View {
        HStack {
            Image(systemName: "person.circle.fill")
                .font(.title2)
            VStack(alignment: .leading, spacing: 2) {
                Text("Utilisateur")
                    .font(.subheadline.weight(.medium))
                Text("Gestionnaire de Stock")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
        }
        .padding(.horizontal)
        .padding(.vertical, 8)
    }
}
