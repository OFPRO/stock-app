import SwiftUI

struct StockCard<Content: View>: View {
    let content: Content
    let isPressable: Bool
    let isSelected: Bool
    let accentColor: Color?

    init(isPressable: Bool = false, isSelected: Bool = false, accentColor: Color? = nil, @ViewBuilder content: () -> Content) {
        self.isPressable = isPressable
        self.isSelected = isSelected
        self.accentColor = accentColor
        self.content = content()
    }

    var body: some View {
        content
            .padding(Spacing.md.rawValue)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(isSelected ? AppColor.accent.opacity(0.1) : AppColor.surface)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isSelected ? AppColor.accent : AppColor.border, lineWidth: isSelected ? 1.5 : 0.5)
            )
            .overlay(
                Rectangle()
                    .fill(accentColor ?? .clear)
                    .frame(width: 4)
                    .clipShape(RoundedCorner(corners: [.topLeft, .bottomLeft], radius: 12)),
                alignment: .leading
            )
            .shadow(color: AppColor.shadow, radius: 4, y: 2)
    }
}

struct RoundedCorner: Shape {
    var corners: UIRectCorner
    var radius: CGFloat

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: corners,
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(path.cgPath)
    }
}

struct StockKPICard: View {
    let title: String
    let value: String
    let icon: String
    let color: Color

    var body: some View {
        StockCard(accentColor: color) {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .font(.title3)
                    .foregroundStyle(color)
                    .frame(width: 36, height: 36)
                    .background(color.opacity(0.15))
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text(value)
                        .font(.subheadline.weight(.semibold))
                        .lineLimit(1)
                        .minimumScaleFactor(0.5)
                }
            }
        }
    }
}
