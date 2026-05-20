import SwiftUI
import AVFoundation

struct ScannerView: View {
    @StateObject private var viewModel = ScannerViewModel()
    @State private var captureSession: AVCaptureSession?
    @State private var hasCameraPermission = false
    @State private var showCameraAlert = false

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                cameraPreview
                    .animation(.default, value: viewModel.scanningState)
                manualEntry
                scanHistory
            }
            .padding()
        }
        .background(AppColor.background)
        .navigationTitle("Scanner")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                if case .scanning = viewModel.scanningState {
                    Button {
                        viewModel.toggleTorch()
                    } label: {
                        Image(systemName: viewModel.torchOn ? "flashlight.on.fill" : "flashlight.off.fill")
                    }
                }
            }
        }
        .alert("Caméra", isPresented: $showCameraAlert) {
            Button("OK", role: .cancel) {}
        } message: {
            Text("Autorisez l'accès à la caméra dans Réglages pour scanner les codes-barres.")
        }
        .task {
            await requestCameraAccess()
        }
        .onChange(of: hasCameraPermission) { granted in
            if granted { configureSession() }
        }
        .onDisappear {
            stopSession()
        }
    }

    // MARK: - Camera

    private func requestCameraAccess() async {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            hasCameraPermission = true
        case .notDetermined:
            if await AVCaptureDevice.requestAccess(for: .video) {
                hasCameraPermission = true
            }
        default:
            showCameraAlert = true
        }
    }

    private func configureSession() {
        let session = AVCaptureSession()
        session.sessionPreset = .high
        guard let device = AVCaptureDevice.default(for: .video),
              let input = try? AVCaptureDeviceInput(device: device),
              session.canAddInput(input)
        else { return }
        session.addInput(input)
        captureSession = session
    }

    private func startSession() {
        guard let session = captureSession, !session.isRunning else { return }
        Task.detached(priority: .background) {
            session.startRunning()
        }
    }

    private func stopSession() {
        guard let session = captureSession, session.isRunning else { return }
        Task.detached(priority: .background) {
            session.stopRunning()
        }
    }

    private var cameraPreview: some View {
        StockCard {
            VStack(spacing: 12) {
                if case .scanning = viewModel.scanningState, let session = captureSession {
                    ZStack(alignment: .bottom) {
                        BarcodeScannerPreview(session: session) { code in
                            viewModel.onBarcodeDetected(code)
                        }
                        .frame(height: 240)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .overlay(scanningFrameOverlay)

                        scanningOverlay
                    }

                    Button(viewModel.torchOn ? "Éteindre lampe" : "Allumer lampe") {
                        viewModel.toggleTorch()
                    }
                    .buttonStyle(.bordered)
                } else if case .found(let product) = viewModel.scanningState {
                    scannedProductCard(product)
                } else if case .notFound(let code) = viewModel.scanningState {
                    notFoundView(code)
                } else if case .error(let message) = viewModel.scanningState {
                    errorView(message)
                } else {
                    idleCameraView
                }
            }
            .padding(.vertical, 12)
        }
    }

    private var scanningFrameOverlay: some View {
        ScanningFrame()
            .stroke(AppColor.accent, lineWidth: 2)
            .frame(width: 200, height: 120)
            .accessibilityHidden(true)
    }

    private var idleCameraView: some View {
        VStack(spacing: 12) {
            Image(systemName: "barcode.viewfinder")
                .font(.system(size: 54))
                .foregroundStyle(AppColor.accent)

            Text("Placez le code-barres dans le cadre")
                .font(.subheadline)
                .foregroundStyle(.secondary)

            Button("Lancer le scan") {
                viewModel.toggleScanning()
                startSession()
            }
            .buttonStyle(.borderedProminent)
            .accessibilityHint("Active la caméra pour scanner un code-barres")
        }
        .padding(.vertical, 20)
    }

    private var scanningOverlay: some View {
        VStack(spacing: 6) {
            Spacer()
            HStack(spacing: 16) {
                Button {
                    viewModel.toggleScanning()
                    stopSession()
                } label: {
                    Label("Arrêter", systemImage: "stop.circle")
                        .font(.callout)
                }
                .buttonStyle(.bordered)
                .tint(.white)
                .accessibilityLabel("Arrêter le scan")
                .accessibilityHint("Ferme la caméra et revient à l'écran d'accueil")
            }
            .padding(.bottom, 8)
        }
    }

    private func scannedProductCard(_ product: ScannedProduct) -> some View {
        VStack(spacing: 12) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 40))
                .foregroundStyle(.green)

            Text(product.name)
                .font(.title3.weight(.semibold))

            HStack(spacing: 20) {
                Label(product.price, systemImage: "tag")
                    .font(.subheadline)
                Label("\(product.stock) en stock", systemImage: "shippingbox")
                    .font(.subheadline)
            }
            .foregroundStyle(.secondary)

            HStack(spacing: 12) {
                Button {
                    viewModel.dismissResult()
                    startSession()
                } label: {
                    Text("Continuer")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
            }
        }
        .padding(.vertical, 16)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Produit trouvé: \(product.name), \(product.price), \(product.stock) en stock")
    }

    private func notFoundView(_ code: String) -> some View {
        VStack(spacing: 12) {
            Image(systemName: "questionmark.circle")
                .font(.system(size: 40))
                .foregroundStyle(AppColor.warning)

            Text("Code non trouvé")
                .font(.subheadline.weight(.semibold))
            Text(code)
                .font(.caption).foregroundStyle(.secondary)

            Button("Réessayer") {
                viewModel.dismissResult()
                startSession()
            }
            .buttonStyle(.bordered)
            .accessibilityHint("Revient à la caméra pour scanner à nouveau")
        }
        .padding(.vertical, 16)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Code-barres \(code) non trouvé")
    }

    private func errorView(_ message: String) -> some View {
        VStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 40))
                .foregroundStyle(AppColor.error)

            Text(message)
                .font(.caption).foregroundStyle(.secondary)

            Button("Réessayer") {
                viewModel.dismissResult()
                startSession()
            }
            .buttonStyle(.bordered)
        }
        .padding(.vertical, 16)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Erreur: \(message)")
    }

    // MARK: - Manual Entry

    private var manualEntry: some View {
        HStack(spacing: 8) {
            StockTextField("Saisir un code-barres...", text: $viewModel.manualCode, variant: StockTextField.Variant.barcode)
                .onSubmit {
                    Task { await viewModel.searchManualCode() }
                }
            Button("Chercher") {
                Task { await viewModel.searchManualCode() }
            }
            .buttonStyle(.borderedProminent)
            .accessibilityHint("Recherche le produit par code-barres saisi")
        }
    }

    // MARK: - History

    private var scanHistory: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Historique")
                    .font(.subheadline.weight(.semibold))
                Spacer()
                if !viewModel.scanHistory.isEmpty {
                    Button("Effacer") {
                        viewModel.clearHistory()
                    }
                    .font(.caption)
                }
            }

            if viewModel.scanHistory.isEmpty {
                StockCard {
                    VStack(spacing: 4) {
                        Text("Aucun scan")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        Text("Scannez un article ou saisissez un code")
                            .font(.caption2)
                            .foregroundStyle(.tertiary)
                    }
                    .padding(.vertical, 4)
                }
            } else {
                ForEach(viewModel.scanHistory.reversed()) { scan in
                    StockCard {
                        HStack {
                            VStack(alignment: .leading) {
                                Text(scan.productName)
                                    .font(.subheadline.weight(.medium))
                                Text(scan.code)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            Text(scan.timestamp, style: .time)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                    .accessibilityElement(children: .combine)
                    .accessibilityLabel("\(scan.productName), code \(scan.code)")
                }
            }
        }
    }
}

private struct ScanningFrame: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        let cornerLength: CGFloat = 20

        path.move(to: CGPoint(x: rect.minX, y: rect.minY + cornerLength))
        path.addLine(to: CGPoint(x: rect.minX, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.minX + cornerLength, y: rect.minY))

        path.move(to: CGPoint(x: rect.maxX - cornerLength, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.minY + cornerLength))

        path.move(to: CGPoint(x: rect.maxX, y: rect.maxY - cornerLength))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
        path.addLine(to: CGPoint(x: rect.maxX - cornerLength, y: rect.maxY))

        path.move(to: CGPoint(x: rect.minX + cornerLength, y: rect.maxY))
        path.addLine(to: CGPoint(x: rect.minX, y: rect.maxY))
        path.addLine(to: CGPoint(x: rect.minX, y: rect.maxY - cornerLength))

        return path
    }
}
