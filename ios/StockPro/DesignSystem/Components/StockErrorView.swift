import SwiftUI

struct StockErrorView: View {
    let message: String
    let onRetry: (() -> Void)?

    init(message: String, onRetry: (() -> Void)? = nil) {
        self.message = message
        self.onRetry = onRetry
    }

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 40))
                .foregroundStyle(AppColor.warning)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            if let onRetry {
                StockButton("Réessayer", variant: .secondary, icon: "arrow.clockwise") {
                    onRetry()
                }
                .frame(maxWidth: 160)
            }
        }
        .padding()
    }
}
