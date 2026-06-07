import Foundation

@MainActor
final class InvoiceDetailViewModel: ObservableObject {
    @Published var invoice: InvoiceDTO?
    @Published var items: [InvoiceItemDTO] = []
    @Published var state: ViewState<InvoiceDTO> = .loading
    @Published var itemsState: ViewState<[InvoiceItemDTO]> = .loading

    private let invoiceService: InvoiceServiceProtocol
    let invoiceId: Int

    init(invoiceId: Int, invoiceService: InvoiceServiceProtocol = InvoiceService()) {
        self.invoiceId = invoiceId
        self.invoiceService = invoiceService
    }

    func load() async {
        state = .loading
        itemsState = .loading
        do {
            async let invoiceTask = invoiceService.fetchInvoice(id: invoiceId)
            async let itemsTask = invoiceService.fetchInvoiceItems(id: invoiceId)
            let (fetchedInvoice, fetchedItems) = try await (invoiceTask, itemsTask)
            invoice = fetchedInvoice
            items = fetchedItems
            state = .loaded(fetchedInvoice)
            itemsState = .loaded(fetchedItems)
        } catch {
            state = .error(.from(error))
            itemsState = .error(.from(error))
        }
    }
}
