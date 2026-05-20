import Foundation
import Network

final class Reachability {
    static let shared = Reachability()

    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "com.bibliothequebadr.reachability")

    private(set) var isConnected: Bool = true
    private(set) var connectionType: ConnectionType = .unknown

    enum ConnectionType {
        case wifi, cellular, ethernet, unknown
    }

    private init() {
        monitor.pathUpdateHandler = { [weak self] path in
            self?.isConnected = path.status == .satisfied
            if path.usesInterfaceType(.wifi) { self?.connectionType = .wifi }
            else if path.usesInterfaceType(.cellular) { self?.connectionType = .cellular }
            else if path.usesInterfaceType(.wiredEthernet) { self?.connectionType = .ethernet }
            else { self?.connectionType = .unknown }
        }
        monitor.start(queue: queue)
    }

    deinit {
        monitor.cancel()
    }

    func requiresConnection<T>(_ operation: () async throws -> T) async throws -> T {
        guard isConnected else {
            throw AppError.network(URLError(.notConnectedToInternet))
        }
        return try await operation()
    }
}
