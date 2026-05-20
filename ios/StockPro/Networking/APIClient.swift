import Foundation

actor APIClient {
    static let shared = APIClient()

    private let session: URLSession
    private let decoder: JSONDecoder
    private let encoder: JSONEncoder

    private var baseURL: URL {
        guard let urlString = Bundle.main.infoDictionary?["API_BASE_URL"] as? String,
              let url = URL(string: urlString) else {
            return URL(string: "http://localhost:5001")!
        }
        return url
    }

    private init() {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 15
        config.timeoutIntervalForResource = 30
        self.session = URLSession(configuration: config)
        self.decoder = JSONDecoder()
        self.encoder = JSONEncoder()
    }

    func request<T: Decodable>(_ endpoint: Endpoint) async throws -> T {
        let url = baseURL.appendingPathComponent(endpoint.path)
        var request = URLRequest(url: url)
        request.httpMethod = endpoint.method.rawValue
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        Logger.log("\(endpoint.method.rawValue) \(endpoint.path)", category: .networking)

        let (data, response) = try await session.data(for: request)
        return try handleResponse(data: data, response: response)
    }

    func request<T: Decodable, U: Encodable>(_ endpoint: Endpoint, body: U) async throws -> T {
        let url = baseURL.appendingPathComponent(endpoint.path)
        var request = URLRequest(url: url)
        request.httpMethod = endpoint.method.rawValue
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.httpBody = try encoder.encode(body)

        Logger.log("\(endpoint.method.rawValue) \(endpoint.path)", category: .networking)

        let (data, response) = try await session.data(for: request)
        return try handleResponse(data: data, response: response)
    }

    func requestVoid(_ endpoint: Endpoint) async throws {
        let url = baseURL.appendingPathComponent(endpoint.path)
        var request = URLRequest(url: url)
        request.httpMethod = endpoint.method.rawValue
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        Logger.log("\(endpoint.method.rawValue) \(endpoint.path)", category: .networking)

        let (_, response) = try await session.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw AppError.server(statusCode: 0, message: "Réponse invalide")
        }
        guard (200...299).contains(httpResponse.statusCode) else {
            throw AppError.server(statusCode: httpResponse.statusCode, message: nil)
        }
    }

    private func handleResponse<T: Decodable>(data: Data, response: URLResponse) throws -> T {
        guard let httpResponse = response as? HTTPURLResponse else {
            throw AppError.server(statusCode: 0, message: "Réponse invalide")
        }

        guard (200...299).contains(httpResponse.statusCode) else {
            if let errorBody = try? decoder.decode(ServerError.self, from: data) {
                throw AppError.server(statusCode: httpResponse.statusCode, message: errorBody.message ?? errorBody.error)
            }
            throw AppError.server(statusCode: httpResponse.statusCode, message: nil)
        }

        do {
            return try decoder.decode(T.self, from: data)
        } catch {
            Logger.log("Decoding error for \(T.self): \(error)", category: .networking)
            throw AppError.decoding(error)
        }
    }
}

struct ServerError: Decodable {
    let error: String?
    let message: String?
}
