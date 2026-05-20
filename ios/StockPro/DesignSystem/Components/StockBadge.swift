import SwiftUI

struct StockBadge: View {
    let text: String
    let variant: Variant

    enum Variant {
        case success, warning, error, info, neutral

        var color: Color {
            switch self {
            case .success: AppColor.success
            case .warning: AppColor.warning
            case .error: AppColor.error
            case .info: AppColor.info
            case .neutral: .secondary
            }
        }
    }

    init(_ text: String, variant: Variant) {
        self.text = text
        self.variant = variant
    }

    var body: some View {
        Text(text)
            .font(.caption.weight(.medium))
            .foregroundStyle(variant.color)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(variant.color.opacity(0.1))
            .clipShape(Capsule())
    }
}
