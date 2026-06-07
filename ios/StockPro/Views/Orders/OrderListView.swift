import SwiftUI

struct OrderListView: View {
    @StateObject private var vm = OrderListViewModel()

    var body: some View {
        List {
            switch vm.state {
            case .loading:
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .listRowBackground(Color.clear)
            case .loaded:
                Section {
                    Picker("Filtrer", selection: $vm.filterStatus) {
                        Text("Tous").tag(nil as String?)
                        Text("Brouillon").tag("brouillon" as String?)
                        Text("Reçue").tag("recue" as String?)
                        Text("Payée").tag("payee" as String?)
                        Text("Annulée").tag("annulee" as String?)
                    }
                    .pickerStyle(.segmented)
                    .listRowBackground(Color.clear)
                }
                ForEach(vm.filteredOrders) { order in
                    NavigationLink(destination: OrderDetailView(orderId: order.id)) {
                        OrderRow(order: order)
                    }
                }
                .onDelete { indexSet in
                    Task { await vm.deleteOrder(at: indexSet) }
                }
            case .empty:
                ContentUnavailableView(
                    "Aucune commande", systemImage: "doc.text",
                    description: Text("Créez une commande fournisseur")
                )
            case .error(let err):
                StockErrorView(message: err.errorDescription ?? "Erreur", onRetry: { Task { await vm.load() } })
            default:
                Color.clear
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Commandes")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                NavigationLink(destination: OrderCreateView()) {
                    Image(systemName: "plus")
                }
            }
        }
        .task { await vm.load() }
        .refreshable { await vm.load() }
    }
}

struct OrderRow: View {
    let order: OrderDTO

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(order.order_number ?? "CMD #\(order.id)")
                    .font(.subheadline.weight(.medium))
                Spacer()
                OrderStatusBadge(status: order.status)
            }
            if let supplier = order.supplier_name {
                Text(supplier).font(.caption).foregroundStyle(.secondary)
            }
            if let total = order.total {
                Text(total.formatted(.currency(code: "MAD")))
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.primary)
            }
            Text(order.created_at).font(.caption2).foregroundStyle(.tertiary)
        }
        .padding(.vertical, 4)
    }
}

struct OrderStatusBadge: View {
    let status: String

    var body: some View {
        Text(label)
            .font(.caption2.weight(.semibold))
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(color.opacity(0.15))
            .foregroundStyle(color)
            .clipShape(Capsule())
    }

    private var label: String {
        switch status {
        case "brouillon": "Brouillon"
        case "recue": "Reçue"
        case "payee": "Payée"
        case "annulee": "Annulée"
        default: status
        }
    }

    private var color: Color {
        switch status {
        case "brouillon": .orange
        case "recue": .green
        case "payee": .blue
        case "annulee": .red
        default: .gray
        }
    }
}
