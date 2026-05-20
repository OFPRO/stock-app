import SwiftUI

struct PaymentView: View {
    @ObservedObject var viewModel: POSViewModel
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    totalSection
                    paymentMethodSection
                    tenderedSection
                    if viewModel.paymentMethod == .cash || viewModel.paymentMethod == .mixed {
                        changeSection
                    }
                    creditSection
                    checkoutButton
                }
                .padding()
            }
            .background(AppColor.background)
            .navigationTitle("Encaissement")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuler") { dismiss() }
                }
            }
        }
    }

    // MARK: - Total

    private var totalSection: some View {
        VStack(spacing: 8) {
            HStack {
                Text("Total TTC")
                    .font(.subheadline)
                Spacer()
                Text(viewModel.formattedTotal)
                    .font(AppFont.posTotal)
                    .foregroundStyle(AppColor.brand)
            }

            HStack {
                Text("Sous-total HT")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Spacer()
                Text(viewModel.formattedSubtotal)
                    .font(.caption)
            }

            if viewModel.discountTotal > 0 {
                HStack {
                    Text("Remises")
                        .font(.caption)
                        .foregroundStyle(AppColor.success)
                    Spacer()
                    Text(viewModel.formattedDiscount)
                        .font(.caption)
                        .foregroundStyle(AppColor.success)
                }
            }

            HStack {
                Text("TVA (20%)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Spacer()
                Text(viewModel.formattedTax)
                    .font(.caption)
            }
        }
        .padding()
        .background(AppColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    // MARK: - Payment Method

    private var paymentMethodSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Mode de paiement")
                .font(.subheadline.weight(.semibold))

            HStack(spacing: 8) {
                ForEach(PaymentMethod.allCases, id: \.self) { method in
                    Button {
                        viewModel.paymentMethod = method
                    } label: {
                        VStack(spacing: 4) {
                            Image(systemName: method.icon)
                                .font(.title3)
                            Text(method.label)
                                .font(.caption2)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(viewModel.paymentMethod == method ? AppColor.brand : AppColor.surface)
                        .foregroundStyle(viewModel.paymentMethod == method ? .white : .primary)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .padding()
        .background(AppColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    // MARK: - Tendered Amount

    private var tenderedSection: some View {
        VStack(spacing: 12) {
            if viewModel.paymentMethod == .cash || viewModel.paymentMethod == .mixed {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Montant reçu (espèces)")
                        .font(.caption.weight(.medium))
                    StockTextField("0,00", text: $viewModel.cashTenderedText, variant: .currency, keyboardType: .decimalPad)
                }

                HStack(spacing: 8) {
                    quickCashButton("Montant exact", total: viewModel.total)
                    quickCashButton("Arrondi +10", total: ceil(viewModel.total / 10) * 10)
                }
            }

            if viewModel.paymentMethod == .card || viewModel.paymentMethod == .mixed {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Montant carte bancaire")
                        .font(.caption.weight(.medium))

                    StockTextField("0,00", text: $viewModel.cardAmountText, variant: .currency, keyboardType: .decimalPad)
                }
            }
        }
        .padding()
        .background(AppColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func quickCashButton(_ label: String, total: Double) -> some View {
        Button {
            viewModel.cashTenderedText = String(format: "%.0f", total)
        } label: {
            Text(label)
                .font(.caption.weight(.medium))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
                .background(AppColor.brandLight.opacity(0.1))
                .clipShape(RoundedRectangle(cornerRadius: 6))
        }
        .buttonStyle(.plain)
    }

    // MARK: - Change

    private var changeSection: some View {
        HStack {
            Text("Monnaie à rendre")
                .font(.subheadline.weight(.medium))
            Spacer()
            Text(viewModel.formattedChange)
                .font(.title2.weight(.bold))
                .foregroundStyle(AppColor.success)
        }
        .padding()
        .background(AppColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    // MARK: - Credit

    private var creditSection: some View {
        Toggle(isOn: $viewModel.isCredit) {
            VStack(alignment: .leading, spacing: 2) {
                Text("Vente à crédit")
                    .font(.subheadline.weight(.medium))
                Text("La facture sera en attente de paiement")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .tint(AppColor.brand)
        .padding()
        .background(AppColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .disabled(viewModel.selectedCustomer == nil)
    }

    // MARK: - Checkout Button

    private var checkoutButton: some View {
        VStack(spacing: 4) {
            StockButton("ENCAISSER", variant: .primary, icon: "eurosign.circle", disabled: viewModel.isCheckingOut) {
                Task { await viewModel.executeCheckout() }
            }

            if viewModel.isCheckingOut {
                ProgressView("Traitement en cours...")
                    .font(.caption)
            }

            if let error = viewModel.checkoutError {
                Text(error)
                    .font(.caption)
                    .foregroundStyle(AppColor.error)
                    .multilineTextAlignment(.center)
            }
        }
    }
}
