import SwiftUI

struct StockTextField: View {
    let placeholder: String
    @Binding var text: String
    let variant: Variant
    let keyboardType: UIKeyboardType

    enum Variant {
        case `default`, search, barcode, currency
    }

    init(_ placeholder: String, text: Binding<String>, variant: Variant = .default, keyboardType: UIKeyboardType = .default) {
        self.placeholder = placeholder
        self._text = text
        self.variant = variant
        self.keyboardType = keyboardType
    }

    var body: some View {
        HStack(spacing: 8) {
            if variant == .search {
                Image(systemName: "magnifyingglass")
                    .foregroundStyle(.secondary)
            }
            if variant == .barcode {
                Image(systemName: "barcode.viewfinder")
                    .foregroundStyle(.secondary)
            }
            if variant == .currency {
                Text("MAD")
                    .font(.caption.weight(.medium))
                    .foregroundStyle(.secondary)
            }
            TextField(placeholder, text: $text)
                .font(.body)
                .keyboardType(keyboardType)
                .autocorrectionDisabled()
            if !text.isEmpty {
                Button {
                    text = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(.tertiary)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 9)
        .background(AppColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(AppColor.border, lineWidth: 0.5)
        )
    }
}
