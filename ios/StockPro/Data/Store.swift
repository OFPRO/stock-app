import SwiftData
import Foundation

final class Store {
    static let shared = Store()

    let container: ModelContainer

    private init() {
        do {
            let schema = Schema([
                CachedProduct.self,
                CachedCustomer.self,
                CachedSupplier.self,
                CachedWarehouse.self,
                CachedKPI.self,
                CachedNotification.self
            ])
            let config = ModelConfiguration(
                schema: schema,
                isStoredInMemoryOnly: false,
                allowsSave: true
            )
            self.container = try ModelContainer(for: schema, configurations: [config])
        } catch {
            fatalError("SwiftData initialization failed: \(error)")
        }
    }
}
