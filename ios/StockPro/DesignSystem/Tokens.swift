import SwiftUI

enum Spacing: CGFloat {
    case xs = 4
    case sm = 8
    case md = 12
    case lg = 16
    case xl = 24
    case xxl = 32
}

enum FontSize: CGFloat {
    case caption = 12
    case body = 14
    case bodyLarge = 16
    case subtitle = 18
    case title = 22
    case largeTitle = 28
    case posTotal = 34
}

enum AppColor {
    static let brand = Color(hex: "1B2A4A")
    static let brandLight = Color(hex: "2C3F6B")
    static let accent = Color(hex: "E8A838")
    static let accentPressed = Color(hex: "C98F2E")

    static let success = Color(hex: "2E7D32")
    static let warning = Color(hex: "F57F17")
    static let error = Color(hex: "C62828")
    static let info = Color(hex: "1565C0")

    static let background = Color(.systemBackground)
    static let surface = Color(.secondarySystemBackground)
    static let surfaceElevated = Color(.tertiarySystemBackground)
    static let border = Color(.separator)
}

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 6:
            (a, r, g, b) = (255, (int >> 16) & 0xFF, (int >> 8) & 0xFF, int & 0xFF)
        case 8:
            (a, r, g, b) = ((int >> 24) & 0xFF, (int >> 16) & 0xFF, (int >> 8) & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}

enum AppFont {
    static let largeTitle = Font.system(size: FontSize.largeTitle.rawValue, weight: .bold)
    static let title = Font.system(size: FontSize.title.rawValue, weight: .bold)
    static let subtitle = Font.system(size: FontSize.subtitle.rawValue, weight: .semibold)
    static let bodyLarge = Font.system(size: FontSize.bodyLarge.rawValue, weight: .regular)
    static let body = Font.system(size: FontSize.body.rawValue, weight: .regular)
    static let caption = Font.system(size: FontSize.caption.rawValue, weight: .medium)
    static let posTotal = Font.system(size: FontSize.posTotal.rawValue, weight: .bold)
}
