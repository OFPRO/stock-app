import SwiftUI

struct StockButton: View {
    let title: String
    let variant: Variant
    let icon: String?
    let action: () -> Void
    let disabled: Bool
    let compact: Bool

    @State private var pressed = false

    init(_ title: String, variant: Variant = .primary, icon: String? = nil, disabled: Bool = false, compact: Bool = false, action: @escaping () -> Void) {
        self.title = title
        self.variant = variant
        self.icon = icon
        self.disabled = disabled
        self.compact = compact
        self.action = action
    }

    enum Variant {
        case primary, secondary, danger, ghost, search

        var foreground: Color {
            switch self {
            case .primary, .danger, .search: .white
            case .secondary: .primary
            case .ghost: AppColor.brand
            }
        }

        var background: AnyView {
            switch self {
            case .primary:
                AnyView(LinearGradient(colors: AppColor.brandGradient, startPoint: .leading, endPoint: .trailing))
            case .secondary:
                AnyView(AppColor.surface)
            case .danger:
                AnyView(AppColor.error)
            case .ghost:
                AnyView(Color.clear)
            case .search:
                AnyView(LinearGradient(colors: AppColor.accentGradient, startPoint: .leading, endPoint: .trailing))
            }
        }

        var pressedBackground: AnyView {
            switch self {
            case .primary:
                AnyView(LinearGradient(colors: [AppColor.brandLight, AppColor.brandLight], startPoint: .leading, endPoint: .trailing))
            case .secondary:
                AnyView(AppColor.surfaceWarm)
            case .danger:
                AnyView(AppColor.error.opacity(0.8))
            case .ghost:
                AnyView(AppColor.surface)
            case .search:
                AnyView(AppColor.accentPressed)
            }
        }

        var hasShadow: Bool {
            switch self {
            case .primary, .danger, .search: true
            case .secondary, .ghost: false
            }
        }
    }

    var body: some View {
        Button(action: {
            withAnimation(.easeInOut(duration: 0.1)) { pressed = true }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                withAnimation(.easeInOut(duration: 0.1)) { pressed = false }
                action()
            }
        }) {
            HStack(spacing: 8) {
                if let icon {
                    Image(systemName: icon)
                        .font(.body.weight(.semibold))
                }
                Text(title)
                    .font(.body.weight(.semibold))
            }
            .frame(maxWidth: compact ? nil : .infinity)
            .padding(.vertical, compact ? 10 : 14)
            .padding(.horizontal, compact ? 14 : 20)
            .background(
                Group {
                    if disabled {
                        variant.background.opacity(0.4)
                    } else if pressed {
                        variant.pressedBackground
                    } else {
                        variant.background
                    }
                }
            )
            .foregroundStyle(disabled ? variant.foreground.opacity(0.4) : variant.foreground)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(AppColor.border, lineWidth: variant == .secondary ? 1 : 0)
            )
            .shadow(color: variant.hasShadow && !disabled ? AppColor.shadow : .clear, radius: 4, y: 2)
            .scaleEffect(pressed ? 0.97 : 1)
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
