import SwiftUI

struct OrderDetailView: View {
    @StateObject private var vm: OrderDetailViewModel

    init(orderId: Int) {
        _vm = StateObject(wrappedValue: OrderDetailViewModel(orderId: orderId))
    }

    var body: some View {
        List {
            Section("Détails") {
                LabeledContent("Numéro", value: vm.order?.order_number ?? "-")
                LabeledContent("Statut") {
                    if let order = vm.order {
                        OrderStatusBadge(status: order.status)
                    }
                }
                if let supplier = vm.order?.supplier_name {
                    LabeledContent("Fournisseur", value: supplier)
                }
                    if let total = vm.order?.total {
                        let totalText = total.formatted(.currency(code: "MAD"))
                        LabeledContent("Total", value: totalText)
                    }
            }
            Section("Articles") {
                switch vm.itemsState {
                case .loading:
                    ProgressView().frame(maxWidth: .infinity)
                case .loaded(let items):
                    if items.isEmpty {
                        Text("Aucun article").foregroundStyle(.secondary)
                    } else {
                        ForEach(items) { item in
                            HStack {
                                VStack(alignment: .leading) {
                                    Text(item.product_name ?? "Produit #\(item.product_id)")
                                        .font(.subheadline)
                                    Text("Qté: \(item.quantity)")
                                        .font(.caption).foregroundStyle(.secondary)
                                }
                                Spacer()
                                if let total = item.total {
                                    let amount = total.formatted(.currency(code: "MAD"))
                                    Text(amount)
                                        .font(.subheadline.weight(.medium))
                                }
                            }
                            .padding(.vertical, 2)
                        }
                    }
                case .error(let err):
                    Text(err.errorDescription ?? "Erreur").foregroundStyle(.red)
                default:
                    Color.clear
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle(vm.order?.order_number ?? "Commande")
        .task { await vm.load() }
    }
}
