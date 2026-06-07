import SwiftUI

struct StockTag: View {
    enum Variant {
        case brand, surface, success
    }

    let text: String
    let icon: String?
    let variant: Variant

    init(_ text: String, icon: String? = nil, variant: Variant = .surface) {
        self.text = text
        self.icon = icon
        self.variant = variant
    }

    var body: some View {
        HStack(spacing: 4) {
            if let icon {
                Image(systemName: icon)
                    .font(.caption2)
            }
            Text(text)
                .font(.caption2.weight(.medium))
                .lineLimit(1)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 5)
        .background(background)
        .clipShape(Capsule())
        .overlay(
            Capsule().stroke(AppColor.border, lineWidth: 1 / UIScreen.main.scale)
        )
    }

    private var background: Color {
        switch variant {
        case .brand:   AppColor.brand.opacity(0.1)
        case .surface: AppColor.surface
        case .success: AppColor.success.opacity(0.1)
        }
    }
}
