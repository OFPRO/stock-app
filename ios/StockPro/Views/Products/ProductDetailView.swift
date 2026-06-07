import SwiftUI

struct ProductDetailView: View {
    @StateObject private var viewModel: ProductDetailViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var showEditForm = false
    @State private var deleted = false

    init(productId: Int) {
        _viewModel = StateObject(wrappedValue: ProductDetailViewModel(productId: productId))
    }

    var body: some View {
        Group {
            if deleted {
                ContentUnavailableView("Produit supprimé", systemImage: "trash", description: Text("Retour à la liste"))
            } else {
                content
            }
        }
        .navigationTitle("Produit")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    Button { showEditForm = true } label: {
                        Label("Modifier", systemImage: "pencil")
                    }
                    Button(role: .destructive) { viewModel.showDeleteConfirmation = true } label: {
                        Label("Supprimer", systemImage: "trash")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                }
            }
        }
        .sheet(isPresented: $showEditForm) {
            ProductFormView(mode: .edit(productId: viewModel.productId))
        }
        .alert("Supprimer le produit ?", isPresented: $viewModel.showDeleteConfirmation) {
            Button("Annuler", role: .cancel) {}
            Button("Supprimer", role: .destructive) {
                Task {
                    if await viewModel.deleteProduct() {
                        deleted = true
                    }
                }
            }
        } message: {
            Text("Cette action est irréversible.")
        }
        .task { await viewModel.load() }
    }

    @ViewBuilder
    private var content: some View {
        switch viewModel.state {
        case .loading:
            VStack(spacing: Spacing.md.rawValue) {
                StockSkeleton(variant: .card)
                StockSkeleton(variant: .card)
                StockSkeleton(variant: .card)
            }
            .padding()
        case .loaded(let product):
            detailContent(product)
        case .error(let error):
            StockErrorView(message: error.errorDescription ?? "Erreur", onRetry: {
                Task { await viewModel.load() }
            })
        case .empty:
            ContentUnavailableView("Produit introuvable", systemImage: "shippingbox")
        }
    }

    private func detailContent(_ product: ProductDetail) -> some View {
        ScrollView {
            VStack(spacing: Spacing.md.rawValue) {
                headerSection(product)
                pricingSection(product)
                stockSection(product)
                infoSection(product)
                movementsSection
            }
            .padding()
        }
        .background(AppColor.background)
    }

    private func headerSection(_ product: ProductDetail) -> some View {
        StockCard {
            VStack(alignment: .leading, spacing: Spacing.sm.rawValue) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(product.name)
                            .font(.title3.weight(.bold))
                        Text(product.sku)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    StockBadge(product.stockStatus.label, variant: stockBadgeVariant(product.stockStatus))
                }
                if !product.description.isEmpty {
                    Text(product.description)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                if !product.barcode.isEmpty {
                    Label(product.barcode, systemImage: "barcode")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
        }
    }

    private func pricingSection(_ product: ProductDetail) -> some View {
        StockCard {
            VStack(alignment: .leading, spacing: Spacing.sm.rawValue) {
                Text("Prix")
                    .font(.subheadline.weight(.semibold))
                TierRow(label: "Prix normal", price: product.formattedPrice, isDefault: true)
                Divider()
                TierRow(label: "Loyal (-15%)", price: discountedPrice(product.price, 15))
                TierRow(label: "Étudiant (-15%)", price: discountedPrice(product.price, 15))
                TierRow(label: "École (-20%)", price: discountedPrice(product.price, 20))
                if product.wholesalePrice > 0 {
                    Divider()
                    TierRow(label: "Prix de gros", price: product.formattedWholesalePrice)
                }
                if product.purchasePrice > 0 {
                    Divider()
                    TierRow(label: "Prix d'achat", price: product.formattedPurchasePrice)
                    let marge = ((product.price - product.purchasePrice) / product.purchasePrice * 100)
                    TierRow(label: "Marge", price: String(format: "%.1f%%", marge))
                }
            }
        }
    }

    private func stockSection(_ product: ProductDetail) -> some View {
        StockCard {
            VStack(alignment: .leading, spacing: Spacing.sm.rawValue) {
                Text("Stock")
                    .font(.subheadline.weight(.semibold))
                HStack {
                    Text("Quantité")
                    Spacer()
                    Text("\(product.quantity)")
                        .font(.title3.weight(.bold))
                }
                HStack {
                    Text("Stock minimum")
                    Spacer()
                    Text("\(product.minStock)")
                }
                HStack {
                    Text("Stock maximum")
                    Spacer()
                    Text("\(product.maxStock)")
                }
                HStack {
                    Text("Unité")
                    Spacer()
                    Text(product.unit)
                }
                HStack {
                    Text("Catégorie")
                    Spacer()
                    StockBadge(product.category, variant: .info)
                }
            }
            .font(.subheadline)
        }
    }

    private func infoSection(_ product: ProductDetail) -> some View {
        StockCard {
            VStack(alignment: .leading, spacing: Spacing.sm.rawValue) {
                Text("Informations")
                    .font(.subheadline.weight(.semibold))
                if product.weight > 0 {
                    HStack {
                        Text("Poids")
                        Spacer()
                        Text(String(format: "%.2f kg", product.weight))
                    }
                }
                HStack {
                    Text("Actif")
                    Spacer()
                    Image(systemName: product.isActive ? "checkmark.circle.fill" : "xmark.circle.fill")
                        .foregroundStyle(product.isActive ? AppColor.success : AppColor.error)
                }
            }
            .font(.subheadline)
        }
    }

    private var movementsSection: some View {
        StockCard {
            VStack(alignment: .leading, spacing: Spacing.sm.rawValue) {
                Text("Mouvements récents")
                    .font(.subheadline.weight(.semibold))
                if viewModel.stockMovements.isEmpty {
                    Text("Aucun mouvement")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(Array(viewModel.stockMovements.prefix(5))) { movement in
                        HStack(spacing: 8) {
                            RoundedRectangle(cornerRadius: 6)
                                .fill(movement.typeColor.opacity(0.15))
                                .frame(width: 32, height: 32)
                                .overlay {
                                    Image(systemName: movement.typeIcon)
                                        .font(.caption2)
                                        .foregroundStyle(movement.typeColor)
                                }

                            VStack(alignment: .leading, spacing: 1) {
                                Text(movement.typeLabel)
                                    .font(.caption.weight(.medium))
                                Text(movement.locationSummary)
                                    .font(.caption2)
                                    .foregroundStyle(.tertiary)
                            }

                            Spacer()

                            Text(movement.displayQuantity)
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(movement.quantityColor)
                        }
                        if movement.id != viewModel.stockMovements.prefix(5).last?.id {
                            Divider()
                        }
                    }
                }
            }
        }
    }

    private func discountedPrice(_ price: Double, _ percent: Int) -> String {
        String(format: "%.2f MAD", price * (1 - Double(percent) / 100))
    }

    private func stockBadgeVariant(_ status: StockStatus) -> StockBadge.Variant {
        switch status {
        case .inStock: return .success
        case .low: return .warning
        case .outOfStock: return .error
        }
    }
}

private struct TierRow: View {
    let label: String
    let price: String
    var isDefault: Bool = false

    var body: some View {
        HStack {
            Text(label)
                .font(.subheadline)
            Spacer()
            Text(price)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(isDefault ? AppColor.brand : .secondary)
        }
    }
}
