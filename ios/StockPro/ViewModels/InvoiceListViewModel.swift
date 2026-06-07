import Foundation

@MainActor
final class InvoiceListViewModel: ObservableObject {
    @Published var state: ViewState<[InvoiceDTO]> = .loading
    @Published var filterStatus: String?

    private let invoiceService: InvoiceServiceProtocol

    init(invoiceService: InvoiceServiceProtocol = InvoiceService()) {
        self.invoiceService = invoiceService
    }

    var filteredInvoices: [InvoiceDTO] {
        guard case .loaded(let invoices) = state else { return [] }
        if let status = filterStatus {
            return invoices.filter { $0.status == status }
        }
        return invoices
    }

    func load() async {
        state = .loading
        do {
            let invoices = try await invoiceService.fetchInvoices(status: nil, dateStart: nil, dateEnd: nil)
            state = invoices.isEmpty ? .empty("Aucune facture") : .loaded(invoices)
        } catch {
            state = .error(.from(error))
        }
    }

    func deleteInvoice(at indexSet: IndexSet) async {
        guard case .loaded(let invoices) = state else { return }
        for idx in indexSet {
            try? await invoiceService.deleteInvoice(id: invoices[idx].id)
        }
        await load()
    }
}
