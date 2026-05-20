import Foundation
import CryptoKit

final class PINManager {
    static let shared = PINManager()

    private let keychain = SecurityManager.shared
    private let pinKey = "user_pin_hash"
    private let saltKey = "user_pin_salt"
    private let attemptsKey = "pin_attempts"
    private let lockoutKey = "pin_lockout_until"

    var hasPIN: Bool {
        keychain.exists(key: pinKey) && keychain.exists(key: saltKey)
    }

    func setPIN(_ pin: String) throws {
        let salt = Salt()
        let hash = hashPIN(pin, salt: salt)
        try keychain.store(key: pinKey, data: hash.data)
        try keychain.store(key: saltKey, data: salt.data)
        try keychain.delete(key: attemptsKey)
        try keychain.delete(key: lockoutKey)
    }

    func verifyPIN(_ pin: String) -> Bool {
        guard hasPIN else { return false }
        do {
            let saltData = try keychain.read(key: saltKey)
            let hashData = try keychain.read(key: pinKey)
            let salt = Salt(data: saltData)
            let hash = hashPIN(pin, salt: salt)
            return hash.data == hashData
        } catch {
            return false
        }
    }

    func changePIN(old: String, new: String) throws {
        guard verifyPIN(old) else {
            throw PINError.incorrectPIN
        }
        try setPIN(new)
    }

    var remainingAttempts: Int {
        guard let data = try? keychain.read(key: attemptsKey) else { return 5 }
        let count = withUnsafeBytes(of: UInt32(0)) { _ in
            data.withUnsafeBytes { $0.load(as: UInt32.self) }
        }
        return 5 - Int(count)
    }

    var isLockedOut: Bool {
        guard let data = try? keychain.read(key: lockoutKey) else { return false }
        let lockoutTime = withUnsafeBytes(of: Date().timeIntervalSince1970) { _ in
            data.withUnsafeBytes { $0.load(as: TimeInterval.self) }
        }
        return Date().timeIntervalSince1970 < lockoutTime
    }

    var lockoutRemaining: TimeInterval {
        guard let data = try? keychain.read(key: lockoutKey) else { return 0 }
        let lockoutTime = withUnsafeBytes(of: Date().timeIntervalSince1970) { _ in
            data.withUnsafeBytes { $0.load(as: TimeInterval.self) }
        }
        return max(0, lockoutTime - Date().timeIntervalSince1970)
    }

    func recordFailedAttempt() {
        let current = remainingAttempts
        let newCount = min(5, 5 - current + 1)
        var count = UInt32(newCount)
        try? keychain.store(key: attemptsKey, data: Data(bytes: &count, count: MemoryLayout<UInt32>.size))
        if newCount >= 5 {
            var lockout = Date().addingTimeInterval(30).timeIntervalSince1970
            try? keychain.store(key: lockoutKey, data: Data(bytes: &lockout, count: MemoryLayout<TimeInterval>.size))
        }
    }

    func resetAttempts() {
        try? keychain.delete(key: attemptsKey)
        try? keychain.delete(key: lockoutKey)
    }

    private func hashPIN(_ pin: String, salt: Salt) -> HashResult {
        let input = pin.data(using: .utf8)! + salt.data
        let hash = SHA256.hash(data: input)
        return HashResult(data: Data(hash))
    }
}

private struct Salt {
    let data: Data
    init() {
        var bytes = [UInt8](repeating: 0, count: 32)
        _ = SecRandomCopyBytes(kSecRandomDefault, 32, &bytes)
        self.data = Data(bytes)
    }
    init(data: Data) { self.data = data }
}

private struct HashResult {
    let data: Data
}

enum PINError: LocalizedError {
    case incorrectPIN
    case lockout(TimeInterval)

    var errorDescription: String? {
        switch self {
        case .incorrectPIN: return "Code PIN incorrect"
        case .lockout(let t): return "Trop de tentatives. Réessayez dans \(Int(t))s."
        }
    }
}
