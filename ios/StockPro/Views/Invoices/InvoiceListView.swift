import SwiftUI

struct InvoiceListView: View {
    @StateObject private var vm = InvoiceListViewModel()

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
                        Text("Payée").tag("payee" as String?)
                        Text("Envoyée").tag("envoyee" as String?)
                        Text("Brouillon").tag("brouillon" as String?)
                    }
                    .pickerStyle(.segmented)
                    .listRowBackground(Color.clear)
                }
                ForEach(vm.filteredInvoices) { invoice in
                    NavigationLink(destination: InvoiceDetailView(invoiceId: invoice.id)) {
                        InvoiceRow(invoice: invoice)
                    }
                }
                .onDelete { indexSet in
                    Task { await vm.deleteInvoice(at: indexSet) }
                }
            case .empty:
                ContentUnavailableView(
                    "Aucune facture", systemImage: "doc.plaintext",
                    description: Text("Créez une facture depuis le POS")
                )
            case .error(let err):
                StockErrorView(message: err.errorDescription ?? "Erreur", onRetry: { Task { await vm.load() } })
            default:
                Color.clear
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Factures")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                NavigationLink(destination: InvoiceCreateView()) {
                    Image(systemName: "plus")
                }
            }
        }
        .task { await vm.load() }
        .refreshable { await vm.load() }
    }
}

struct InvoiceRow: View {
    let invoice: InvoiceDTO

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(invoice.invoice_number ?? "FACT #\(invoice.id)")
                    .font(.subheadline.weight(.medium))
                Spacer()
                InvoiceStatusBadge(status: invoice.status)
            }
            if let customer = invoice.customer_name {
                Text(customer).font(.caption).foregroundStyle(.secondary)
            }
            HStack {
                Text(invoice.total.formatted(.currency(code: "MAD")))
                    .font(.caption.weight(.semibold))
                Spacer()
                if let method = invoice.payment_method {
                    Text(methodLabel(method)).font(.caption2).foregroundStyle(.tertiary)
                }
            }
        }
        .padding(.vertical, 4)
    }

    private func methodLabel(_ method: String) -> String {
        switch method {
        case "especes": "Espèces"
        case "carte": "Carte"
        case "cheque": "Chèque"
        case "virement": "Virement"
        default: method
        }
    }
}

struct InvoiceStatusBadge: View {
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
        case "payee": "Payée"
        case "envoyee": "Envoyée"
        case "brouillon": "Brouillon"
        case "annulee": "Annulée"
        case "ticket": "Ticket"
        default: status
        }
    }

    private var color: Color {
        switch status {
        case "payee": .green
        case "envoyee": .blue
        case "brouillon": .orange
        case "annulee": .red
        case "ticket": .purple
        default: .gray
        }
    }
}
