import SwiftUI

struct StockCard<Content: View>: View {
    let content: Content
    let isPressable: Bool
    let isSelected: Bool

    init(isPressable: Bool = false, isSelected: Bool = false, @ViewBuilder content: () -> Content) {
        self.isPressable = isPressable
        self.isSelected = isSelected
        self.content = content()
    }

    var body: some View {
        content
            .padding(Spacing.md.rawValue)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(isSelected ? AppColor.accent.opacity(0.1) : AppColor.surface)
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(isSelected ? AppColor.accent : AppColor.border, lineWidth: isSelected ? 1.5 : 0.5)
            )
    }
}

struct StockKPICard: View {
    let title: String
    let value: String
    let icon: String
    let color: Color

    var body: some View {
        StockCard {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .font(.title3)
                    .foregroundStyle(color)
                    .frame(width: 36, height: 36)
                    .background(color.opacity(0.1))
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text(value)
                        .font(.subheadline.weight(.semibold))
                }
            }
        }
    }
}
