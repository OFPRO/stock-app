import Foundation

@MainActor
final class SessionHistoryViewModel: ObservableObject {
    @Published var state: ViewState<[SessionHistoryItem]> = .loading
    @Published var summary: ViewState<SessionsSummaryDTO> = .loading
    @Published var selectedStatus: String = "closed"

    private let sessionService: SessionServiceProtocol

    init(sessionService: SessionServiceProtocol = SessionService()) {
        self.sessionService = sessionService
    }

    func load() async {
        state = .loading
        summary = .loading
        do {
            async let history = sessionService.fetchHistory(limit: 20, status: selectedStatus)
            async let summaryData = sessionService.fetchSummary(period: 30)
            let (h, s) = try await (history, summaryData)
            state = h.isEmpty ? .empty("Aucune session") : .loaded(h)
            summary = .loaded(s)
        } catch {
            state = .error(.from(error))
            summary = .error(.from(error))
        }
    }
}
