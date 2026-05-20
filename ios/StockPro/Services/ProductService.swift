import Foundation

final class ProductService: ProductServiceProtocol {
    private let api = APIClient.shared

    func fetchProducts() async throws -> [ProductListItem] {
        typealias Response = [ProductDTO]
        let dtos: Response = try await api.request(.products(includeArchived: false))
        return dtos.map { $0.toListItem() }
    }

    func fetchProduct(id: Int) async throws -> ProductDetail {
        let dto: ProductDetailDTO = try await api.request(.product(id))
        return dto.toDetail()
    }

    func createProduct(_ request: ProductCreateRequest) async throws -> ProductDetail {
        let dto: ProductDetailDTO = try await api.request(.createProduct, body: request)
        return dto.toDetail()
    }

    func updateProduct(id: Int, _ request: ProductUpdateRequest) async throws -> ProductDetail {
        let dto: ProductDetailDTO = try await api.request(.updateProduct(id), body: request)
        return dto.toDetail()
    }

    func deleteProduct(id: Int) async throws {
        try await api.requestVoid(.deleteProduct(id))
    }

    func fetchCategories() async throws -> [CategoryDTO] {
        try await api.request(.categories)
    }

    func fetchProductByBarcode(_ barcode: String) async throws -> ScannedProduct? {
        typealias Response = [ForSaleProductDTO]
        let results: Response = try await api.request(.productByBarcode(barcode))
        return results.first(where: {
            $0.barcode?.trimmingCharacters(in: .whitespaces) == barcode.trimmingCharacters(in: .whitespaces)
        })?.toScannedProduct() ?? results.first?.toScannedProduct()
    }
}

final class MockProductService: ProductServiceProtocol {
    func fetchProducts() async throws -> [ProductListItem] {
        [
            ProductListItem(id: 1, name: "Ramette A4", sku: "PAP-A4-001", price: "45,00 MAD", stock: 120, category: "Fournitures"),
            ProductListItem(id: 2, name: "Stylo Bleu", sku: "STY-BLE-002", price: "3,50 MAD", stock: 500, category: "Fournitures"),
            ProductListItem(id: 3, name: "Dossier Suspendu", sku: "DOS-SUS-003", price: "12,00 MAD", stock: 45, category: "Fournitures"),
            ProductListItem(id: 4, name: "Clavier USB", sku: "CLV-USB-004", price: "120,00 MAD", stock: 8, category: "Informatique"),
            ProductListItem(id: 5, name: "Souris Optique", sku: "SRL-OPT-005", price: "65,00 MAD", stock: 15, category: "Informatique"),
            ProductListItem(id: 6, name: "Écran 24\"", sku: "ECR-24-006", price: "1 200,00 MAD", stock: 3, category: "Informatique"),
        ]
    }

    func fetchProduct(id: Int) async throws -> ProductDetail {
        ProductDetail(
            id: id, name: "Exemple Produit", sku: "EX-001",
            description: "Description détaillée du produit de démonstration.",
            price: 45.0, quantity: 120, category: "Fournitures",
            barcode: "123456789", minStock: 10, maxStock: 200,
            isActive: true, weight: 0.5, unit: "pièce",
            purchasePrice: 30.0, wholesalePrice: 40.0
        )
    }

    func createProduct(_ request: ProductCreateRequest) async throws -> ProductDetail {
        ProductDetail(id: Int.random(in: 100...999), name: request.name, sku: request.sku,
            description: request.description ?? "", price: request.price, quantity: 0,
            category: request.category ?? "Général", barcode: request.barcode ?? "",
            minStock: request.min_stock ?? 0, maxStock: request.max_stock ?? 0,
            isActive: true, weight: request.weight ?? 0, unit: request.unit ?? "pièce",
            purchasePrice: request.purchase_price ?? 0, wholesalePrice: request.wholesale_price ?? 0)
    }

    func updateProduct(id: Int, _ request: ProductUpdateRequest) async throws -> ProductDetail {
        ProductDetail(id: id, name: request.name ?? "Mis à jour", sku: request.sku ?? "UPD",
            description: request.description ?? "", price: request.price ?? 0, quantity: 0,
            category: request.category ?? "Général", barcode: request.barcode ?? "",
            minStock: request.min_stock ?? 0, maxStock: request.max_stock ?? 0,
            isActive: request.is_active ?? true, weight: request.weight ?? 0,
            unit: request.unit ?? "pièce", purchasePrice: request.purchase_price ?? 0,
            wholesalePrice: request.wholesale_price ?? 0)
    }

    func deleteProduct(id: Int) async throws {}

    func fetchCategories() async throws -> [CategoryDTO] {
        [
            CategoryDTO(name: "Fournitures", count: 25),
            CategoryDTO(name: "Informatique", count: 12),
            CategoryDTO(name: "Papeterie", count: 8),
            CategoryDTO(name: "Mobilier", count: 5),
        ]
    }

    func fetchProductByBarcode(_ barcode: String) async throws -> ScannedProduct? {
        let all = try await fetchProducts()
        guard let match = all.first(where: { $0.sku == barcode }) else { return nil }
        return ScannedProduct(id: match.id, name: match.name, sku: match.sku, price: match.price, stock: match.stock, barcode: barcode)
    }
}

struct ProductDTO: Decodable {
    let id: Int
    let name: String
    let sku: String
    let price: Double
    let stock_quantity: Int
    let category: String?

    func toListItem() -> ProductListItem {
        ProductListItem(
            id: id,
            name: name,
            sku: sku,
            price: String(format: "%.2f MAD", price),
            stock: stock_quantity,
            category: category ?? "Général"
        )
    }
}
