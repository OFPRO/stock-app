import Foundation

@MainActor
final class ReorderRuleListViewModel: ObservableObject {
    @Published var state: ViewState<[ReorderRuleDTO]> = .loading
    @Published var filterActive: Bool = false
    @Published var replenishmentSuggestions: [ReplenishmentSuggestion] = []
    var replenishmentState: ViewState<[ReplenishmentSuggestion]> = .loading {
        didSet { objectWillChange.send() }
    }

    private let ruleService: ReorderRuleServiceProtocol

    init(ruleService: ReorderRuleServiceProtocol = ReorderRuleService()) {
        self.ruleService = ruleService
    }

    var filteredRules: [ReorderRuleDTO] {
        guard case .loaded(let rules) = state else { return [] }
        if filterActive { return rules.filter { $0.is_active } }
        return rules
    }

    func load() async {
        state = .loading
        do {
            let rules = try await ruleService.fetchRules(warehouseId: nil)
            state = rules.isEmpty ? .empty("") : .loaded(rules)
        } catch {
            state = .error(.from(error))
        }
    }

    func deleteRule(at indexSet: IndexSet) async {
        guard case .loaded(let rules) = state else { return }
        for idx in indexSet {
            try? await ruleService.deleteRule(id: rules[idx].id)
        }
        await load()
    }

    func refresh() async {
        state = .loading
        try? await Task.sleep(nanoseconds: 300_000_000)
        await load()
    }

    func loadReplenishment() async {
        replenishmentState = .loading
        do {
            let suggestions = try await ruleService.fetchReplenishment()
            replenishmentSuggestions = suggestions
            replenishmentState = suggestions.isEmpty ? .empty("") : .loaded(suggestions)
        } catch {
            replenishmentState = .error(.from(error))
        }
    }
}
