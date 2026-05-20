import SwiftUI

struct StockFormRow<Content: View>: View {
    let label: String
    let required: Bool
    let content: Content

    init(_ label: String, required: Bool = false, @ViewBuilder content: () -> Content) {
        self.label = label
        self.required = required
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 4) {
                Text(label)
                    .font(.caption.weight(.medium))
                    .foregroundStyle(.secondary)
                if required {
                    Text("*")
                        .font(.caption)
                        .foregroundStyle(AppColor.error)
                }
            }
            content
        }
    }
}

struct StockPickerRow: View {
    let label: String
    @Binding var selection: String
    let options: [(String, String)]

    var body: some View {
        StockFormRow(label) {
            Picker(label, selection: $selection) {
                ForEach(options, id: \.0) { key, value in
                    Text(value).tag(key)
                }
            }
            .pickerStyle(.menu)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(AppColor.surface)
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(AppColor.border, lineWidth: 0.5)
            )
        }
    }
}
