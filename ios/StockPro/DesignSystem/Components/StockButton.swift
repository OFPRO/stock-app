import SwiftUI

struct StockButton: View {
    let title: String
    let variant: Variant
    let icon: String?
    let action: () -> Void
    let disabled: Bool

    init(_ title: String, variant: Variant = .primary, icon: String? = nil, disabled: Bool = false, action: @escaping () -> Void) {
        self.title = title
        self.variant = variant
        self.icon = icon
        self.disabled = disabled
        self.action = action
    }

    enum Variant {
        case primary, secondary, danger, ghost

        var foreground: Color {
            switch self {
            case .primary, .danger: .white
            case .secondary: .primary
            case .ghost: .primary
            }
        }

        var background: Color {
            switch self {
            case .primary: AppColor.brand
            case .secondary: AppColor.surface
            case .danger: AppColor.error
            case .ghost: .clear
            }
        }

        var pressedBackground: Color {
            switch self {
            case .primary: AppColor.brandLight
            case .secondary: AppColor.border
            case .danger: AppColor.error.opacity(0.8)
            case .ghost: AppColor.surface
            }
        }
    }

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if let icon {
                    Image(systemName: icon)
                        .font(.body.weight(.semibold))
                }
                Text(title)
                    .font(.body.weight(.semibold))
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .padding(.horizontal, 20)
            .background(disabled ? variant.background.opacity(0.4) : variant.background)
            .foregroundStyle(disabled ? variant.foreground.opacity(0.4) : variant.foreground)
            .clipShape(RoundedRectangle(cornerRadius: 10))
        }
        .disabled(disabled)
    }
}

struct StockIconButton: View {
    let icon: String
    let label: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: icon)
                .font(.body)
                .frame(width: 44, height: 44)
        }
        .accessibilityLabel(label)
    }
}
