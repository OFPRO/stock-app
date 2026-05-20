import SwiftUI

struct SettingsView: View {
    @State private var showDebug = false
    @State private var debugRevealed = false
    @State private var showResetConfirm = false
    @State private var showSeedConfirm = false
    @State private var toastMessage: String?
    @State private var showToast = false
    @State private var showChangePIN = false

    private let pinManager = PINManager.shared
    private let apiClient = APIClient.shared

    var body: some View {
        NavigationStack {
            List {
                Section {
                    UserProfileHeader()
                }

                Section("Sécurité") {
                    Button {
                        showChangePIN = true
                    } label: {
                        Label("Changer le code PIN", systemImage: "lock")
                    }
                }

                Section("À propos") {
                    HStack {
                        Text("Version")
                        Spacer()
                        Text("1.0.0")
                            .foregroundStyle(.secondary)
                    }
                    HStack {
                        Text("Build")
                        Spacer()
                        Text("1")
                            .foregroundStyle(.secondary)
                    }
                    HStack {
                        Text("Serveur API")
                        Spacer()
                        Text(Bundle.main.infoDictionary?["API_BASE_URL"] as? String ?? "http://localhost:5001")
                            .foregroundStyle(.secondary)
                            .font(.caption)
                    }
                }

                Section {
                    Button(role: .destructive) {
                        pinManager.resetAttempts()
                        try? pinManager.setPIN("")
                        showToast(message: "PIN réinitialisé")
                    } label: {
                        Label("Réinitialiser le PIN", systemImage: "arrow.counterclockwise")
                    }
                }

                if debugRevealed {
                    Section("Debug") {
                        Button(role: .destructive) {
                            showResetConfirm = true
                        } label: {
                            Label("Réinitialiser les données", systemImage: "trash")
                        }
                        .confirmationDialog("Réinitialiser toutes les données transactionnelles ?", isPresented: $showResetConfirm) {
                            Button("Réinitialiser", role: .destructive) {
                                Task { await resetData() }
                            }
                            Button("Annuler", role: .cancel) {}
                        } message: {
                            Text("Cette action efface les ventes, mouvements et transactions.")
                        }

                        Button {
                            showSeedConfirm = true
                        } label: {
                            Label("Générer des données de démo", systemImage: "plus.circle")
                        }
                        .confirmationDialog("Créer 20 produits et 60 mouvements de démonstration ?", isPresented: $showSeedConfirm) {
                            Button("Générer") {
                                Task { await seedData() }
                            }
                            Button("Annuler", role: .cancel) {}
                        } message: {
                            Text("Ajoute des données de test à la base.")
                        }
                    }
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle("Réglages")
            .sheet(isPresented: $showChangePIN) {
                ChangePINView()
            }
            .overlay(alignment: .bottom) {
                if showToast, let message = toastMessage {
                    StockToast(message: message, variant: .success)
                        .padding(.bottom, 32)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                        .onAppear {
                            DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                                withAnimation { showToast = false }
                            }
                        }
                }
            }
            .onLongPressGesture(minimumDuration: 3) {
                withAnimation { debugRevealed = true }
            }
        }
    }

    private func showToast(message: String) {
        toastMessage = message
        withAnimation { showToast = true }
    }

    private func resetData() async {
        do {
            try await apiClient.requestVoid(.resetData)
            showToast(message: "Données réinitialisées")
        } catch {
            showToast(message: "Erreur: \(error.localizedDescription)")
        }
    }

    private func seedData() async {
        do {
            try await apiClient.requestVoid(.seedData)
            showToast(message: "Données de démo créées")
        } catch {
            showToast(message: "Erreur: \(error.localizedDescription)")
        }
    }
}

struct ChangePINView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var oldPIN = ""
    @State private var newPIN = ""
    @State private var confirmPIN = ""
    @State private var errorMessage: String?

    private let pinManager = PINManager.shared

    var body: some View {
        NavigationStack {
            Form {
                Section("Ancien PIN") {
                    SecureField("Ancien code PIN", text: $oldPIN)
                        .keyboardType(.numberPad)
                }
                Section("Nouveau PIN") {
                    SecureField("Nouveau code PIN", text: $newPIN)
                        .keyboardType(.numberPad)
                    SecureField("Confirmer le nouveau PIN", text: $confirmPIN)
                        .keyboardType(.numberPad)
                }
                if let error = errorMessage {
                    Section {
                        Text(error)
                            .foregroundStyle(AppColor.error)
                            .font(.caption)
                    }
                }
            }
            .navigationTitle("Changer le PIN")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuler") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Enregistrer") {
                        do {
                            guard newPIN == confirmPIN else {
                                errorMessage = "Les nouveaux PIN ne correspondent pas"
                                return
                            }
                            guard newPIN.count >= 4 else {
                                errorMessage = "Le PIN doit contenir au moins 4 chiffres"
                                return
                            }
                            try pinManager.changePIN(old: oldPIN, new: newPIN)
                            dismiss()
                        } catch {
                            errorMessage = error.localizedDescription
                        }
                    }
                }
            }
        }
    }
}

struct StockToast: View {
    let message: String
    let variant: Variant

    enum Variant {
        case success, error, warning
    }

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: icon)
                .foregroundStyle(.white)
            Text(message)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(.white)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(backgroundColor)
        .clipShape(Capsule())
        .shadow(radius: 4)
    }

    private var icon: String {
        switch variant {
        case .success: "checkmark.circle.fill"
        case .error: "xmark.circle.fill"
        case .warning: "exclamationmark.triangle.fill"
        }
    }

    private var backgroundColor: Color {
        switch variant {
        case .success: AppColor.success
        case .error: AppColor.error
        case .warning: AppColor.warning
        }
    }
}
