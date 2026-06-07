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
    case small = 13
    case body = 15
    case subtitle = 18
    case title = 22
    case largeTitle = 28
    case posTotal = 34
}

enum AppColor {
    static let brand = Color(hex: "1E3A6F")
    static let brandLight = Color(hex: "2C5F9E")
    static let brandGradient = [Color(hex: "1E3A6F"), Color(hex: "2C5F9E")]

    static let accent = Color(hex: "F5A623")
    static let accentPressed = Color(hex: "D4891E")
    static let accentGradient = [Color(hex: "F5A623"), Color(hex: "FFB84D")]

    static let success = Color(hex: "34A853")
    static let warning = Color(hex: "FBBC04")
    static let error = Color(hex: "EA4335")
    static let info = Color(hex: "4285F4")

    static let background = Color(.systemBackground)
    static let surface = Color(.secondarySystemBackground)
    static let surfaceWarm = Color(hex: "FFF8F0")
    static let surfaceElevated = Color(.tertiarySystemBackground)
    static let border = Color(.separator)

    static let shadow = Color.black.opacity(0.08)
    static let shadowStrong = Color.black.opacity(0.15)
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
    static let body = Font.system(size: FontSize.body.rawValue, weight: .regular)
    static let caption = Font.system(size: FontSize.caption.rawValue, weight: .regular)
    static let posTotal = Font.system(size: FontSize.posTotal.rawValue, weight: .bold)
}
