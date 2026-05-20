import OSLog

enum LogCategory: String {
    case networking, viewModel, scanning, persistence, auth, security
}

struct Logger {
    private static let subsystem = "com.bibliothequebadr.stockpro"

    static func log(_ message: String, category: LogCategory, level: OSLogType = .debug) {
        os_log("%{public}@", log: OSLog(subsystem: subsystem, category: category.rawValue), type: level, message)
    }

    static func error(_ message: String, category: LogCategory = .networking) {
        log(message, category: category, level: .error)
    }
}
