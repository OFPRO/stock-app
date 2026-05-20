import Foundation

@MainActor
final class ScannerViewModel: ObservableObject {
    enum ScanningState: Equatable {
        case idle
        case scanning
        case found(ScannedProduct)
        case notFound(String)
        case error(String)
    }

    struct ScanRecord: Identifiable {
        let id = UUID()
        let code: String
        let productName: String
        let timestamp: Date = Date()
    }

    @Published var scanningState: ScanningState = .idle
    @Published var torchOn = false
    @Published var manualCode = ""
    @Published var scanHistory: [ScanRecord] = []

    private let productService: ProductServiceProtocol

    init(productService: ProductServiceProtocol = ProductService()) {
        self.productService = productService
    }

    func toggleScanning() {
        switch scanningState {
        case .idle, .notFound, .error:
            scanningState = .scanning
        case .scanning:
            scanningState = .idle
        case .found:
            scanningState = .idle
        }
    }

    func toggleTorch() {
        torchOn.toggle()
    }

    func onBarcodeDetected(_ code: String) {
        guard case .scanning = scanningState else { return }
        Task { await lookupBarcode(code) }
    }

    func searchManualCode() async {
        guard !manualCode.isEmpty else { return }
        await lookupBarcode(manualCode)
        manualCode = ""
    }

    private func lookupBarcode(_ code: String) async {
        do {
            guard let product = try await productService.fetchProductByBarcode(code) else {
                scanningState = .notFound(code)
                return
            }
            scanningState = .found(product)
            scanHistory.append(ScanRecord(code: code, productName: product.name))
        } catch {
            scanningState = .error(error.localizedDescription)
        }
    }

    func clearHistory() {
        scanHistory.removeAll()
    }

    func dismissResult() {
        scanningState = .scanning
    }
}
