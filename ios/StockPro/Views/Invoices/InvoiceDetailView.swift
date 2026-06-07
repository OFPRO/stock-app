import SwiftUI

struct InvoiceDetailView: View {
    @StateObject private var vm: InvoiceDetailViewModel

    init(invoiceId: Int) {
        _vm = StateObject(wrappedValue: InvoiceDetailViewModel(invoiceId: invoiceId))
    }

    var body: some View {
        List {
            Section("Détails") {
                LabeledContent("Facture", value: vm.invoice?.invoice_number ?? "-")
                LabeledContent("Statut") {
                    if let invoice = vm.invoice {
                        InvoiceStatusBadge(status: invoice.status)
                    }
                }
                if let customer = vm.invoice?.customer_name {
                    LabeledContent("Client", value: customer)
                }
                let totalText = (vm.invoice?.total ?? 0).formatted(.currency(code: "MAD"))
                LabeledContent("Total", value: totalText)
                if let method = vm.invoice?.payment_method {
                    LabeledContent("Paiement", value: method)
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
                                    Text("Qté: \(item.quantity) × \(item.price.formatted(.currency(code: "MAD")))")
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
        .navigationTitle(vm.invoice?.invoice_number ?? "Facture")
        .task { await vm.load() }
    }
}
