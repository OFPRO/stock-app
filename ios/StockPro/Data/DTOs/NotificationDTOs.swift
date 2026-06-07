import Foundation

struct NotificationDTO: Decodable, Identifiable {
    let id: Int
    let message: String
    let type: String?
    let is_read: Bool
    let warehouse_id: Int?
    let warehouse_name: String?
    let link_type: String?
    let link_id: Int?
    let created_at: String
}
