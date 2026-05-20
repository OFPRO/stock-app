import SwiftUI

struct StockSkeleton: View {
    let variant: Variant

    enum Variant {
        case card, row, circle
    }

    @State private var isAnimating = false

    var body: some View {
        Group {
            switch variant {
            case .card:
                VStack(alignment: .leading, spacing: 12) {
                    RoundedRectangle(cornerRadius: 6)
                        .fill(AppColor.border)
                        .frame(height: 16)
                        .frame(width: 120)
                    RoundedRectangle(cornerRadius: 6)
                        .fill(AppColor.border)
                        .frame(height: 12)
                    RoundedRectangle(cornerRadius: 6)
                        .fill(AppColor.border)
                        .frame(height: 12)
                        .frame(width: 80)
                }
                .padding(16)
                .background(AppColor.surface)
                .clipShape(RoundedRectangle(cornerRadius: 10))
            case .row:
                HStack(spacing: 12) {
                    Circle()
                        .fill(AppColor.border)
                        .frame(width: 44, height: 44)
                    VStack(alignment: .leading, spacing: 6) {
                        RoundedRectangle(cornerRadius: 6)
                            .fill(AppColor.border)
                            .frame(height: 14)
                            .frame(width: 150)
                        RoundedRectangle(cornerRadius: 6)
                            .fill(AppColor.border)
                            .frame(height: 10)
                            .frame(width: 100)
                    }
                }
            case .circle:
                Circle()
                    .fill(AppColor.border)
                    .frame(width: 44, height: 44)
            }
        }
        .opacity(isAnimating ? 0.3 : 1.0)
        .animation(.easeInOut(duration: 1).repeatForever(autoreverses: true), value: isAnimating)
        .onAppear { isAnimating = true }
    }
}
