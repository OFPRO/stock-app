import AVFoundation
#if canImport(UIKit)
import UIKit

final class BarcodeScannerCoordinator: NSObject, AVCaptureMetadataOutputObjectsDelegate {
    var onScan: ((String) -> Void)?
    private var lastScanTime = Date.distantPast

    func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
        guard let object = metadataObjects.first as? AVMetadataMachineReadableCodeObject,
              let code = object.stringValue,
              Date().timeIntervalSince(lastScanTime) > 1.5
        else { return }
        lastScanTime = Date()
        AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
        onScan?(code)
    }
}

struct BarcodeScannerPreview: UIViewRepresentable {
    let session: AVCaptureSession
    let onScan: (String) -> Void

    func makeCoordinator() -> BarcodeScannerCoordinator {
        let coordinator = BarcodeScannerCoordinator()
        coordinator.onScan = onScan
        return coordinator
    }

    func makeUIView(context: Context) -> UIView {
        let view = UIView(frame: .zero)
        view.backgroundColor = .black

        let previewLayer = AVCaptureVideoPreviewLayer(session: session)
        previewLayer.videoGravity = .resizeAspectFill
        previewLayer.frame = view.bounds
        view.layer.addSublayer(previewLayer)

        let output = AVCaptureMetadataOutput()
        guard session.canAddOutput(output) else { return view }
        session.addOutput(output)
        output.setMetadataObjectsDelegate(context.coordinator, queue: .main)
        output.metadataObjectTypes = [.ean8, .ean13, .upce, .code39, .code128, .qr, .pdf417, .aztec]

        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        guard let layer = uiView.layer.sublayers?.first as? AVCaptureVideoPreviewLayer else { return }
        layer.frame = uiView.bounds
    }
}

#endif
