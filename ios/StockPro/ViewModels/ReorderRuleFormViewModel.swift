import Foundation

@MainActor
final class ReorderRuleFormViewModel: ObservableObject {
    @Published var productId: Int = 0
    @Published var supplierId: Int = 0
    @Published var warehouseId: Int = 1
    @Published var minQuantity: String = ""
    @Published var maxQuantity: String = ""
    @Published var triggerType: String = "auto"
    @Published var saving = false
    @Published var error: AppError?

    private let ruleService: ReorderRuleServiceProtocol

    init(ruleService: ReorderRuleServiceProtocol = ReorderRuleService()) {
        self.ruleService = ruleService
    }

    var isValid: Bool {
        productId > 0 && !minQuantity.isEmpty && !maxQuantity.isEmpty
    }

    func save() async -> Bool {
        guard let min = Int(minQuantity), let max = Int(maxQuantity) else { return false }
        saving = true
        error = nil
        do {
            let request = ReorderRuleCreateRequest(
                product_id: productId, supplier_id: supplierId,
                warehouse_id: warehouseId, min_quantity: min, max_quantity: max,
                trigger_type: triggerType
            )
            try await ruleService.createRule(request)
            saving = false
            return true
        } catch let appErr as AppError {
            self.error = appErr
            saving = false
            return false
        } catch let err {
            self.error = .from(err)
            saving = false
            return false
        }
    }
}
