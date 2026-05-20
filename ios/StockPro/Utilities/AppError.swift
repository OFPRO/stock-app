import Foundation

enum AppError: LocalizedError {
    case network(URLError)
    case server(statusCode: Int, message: String?)
    case decoding(Error)
    case authentication(String)
    case validation([ValidationError])
    case notFound(String)
    case businessRuleViolation(String)
    case pinLockout(TimeInterval)
    case unknown(Error)

    struct ValidationError: Decodable {
        let field: String
        let message: String
    }

    var errorDescription: String? {
        switch self {
        case .network:
            return "Connexion perdue. Vérifiez votre réseau."
        case .server(_, let msg):
            return msg ?? "Erreur serveur. Réessayez."
        case .decoding:
            return "Données invalides reçues du serveur."
        case .authentication:
            return "Session expirée. Veuillez vous reconnecter."
        case .validation(let errors):
            return errors.map(\.message).joined(separator: "\n")
        case .notFound(let item):
            return "\(item) introuvable."
        case .businessRuleViolation(let msg):
            return msg
        case .pinLockout(let t):
            return "Trop de tentatives. Réessayez dans \(Int(t))s."
        case .unknown:
            return "Erreur inattendue. Réessayez."
        }
    }

    static func from(_ error: Error) -> AppError {
        if let appError = error as? AppError { return appError }
        if let urlError = error as? URLError { return .network(urlError) }
        if let decodingError = error as? DecodingError { return .decoding(decodingError) }
        return .unknown(error)
    }
}
