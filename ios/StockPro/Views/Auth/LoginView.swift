import SwiftUI

struct LoginView: View {
    @Binding var isAuthenticated: Bool
    @Binding var showLogin: Bool
    @State private var pin = ""
    @State private var confirmPIN = ""
    @State private var isSettingUp = false
    @State private var errorMessage: String?
    @State private var lockoutTimer: Timer?
    @State private var lockoutRemaining: TimeInterval = 0

    private let pinManager = PINManager.shared

    var body: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "shippingbox.fill")
                .font(.system(size: 60))
                .foregroundStyle(AppColor.brand)

            Text("StockPro")
                .font(AppFont.largeTitle)
            Text("Bibliotheque Badr — Marrakech")
                .font(.caption)
                .foregroundStyle(.secondary)

            Spacer()

            if pinManager.isLockedOut {
                lockedOutView
            } else {
                PINEntryView(
                    pin: $pin,
                    isSettingUp: isSettingUp,
                    errorMessage: $errorMessage,
                    onPinComplete: handlePINEntry
                )
            }

            Spacer()

            if !isSettingUp && !pinManager.hasPIN {
                Button("Configurer le code PIN") {
                    isSettingUp = true
                    pin = ""
                    errorMessage = nil
                }
                .font(.subheadline)
            }
        }
        .padding()
        .onChange(of: lockoutRemaining) { remaining in
            if remaining <= 0 {
                lockoutTimer?.invalidate()
                lockoutTimer = nil
            }
        }
    }

    private var lockedOutView: some View {
        VStack(spacing: 16) {
            Image(systemName: "lock.shield.fill")
                .font(.largeTitle)
                .foregroundStyle(AppColor.error)
            Text("Trop de tentatives")
                .font(.title3.weight(.semibold))
            Text("Réessayez dans \(Int(lockoutRemaining)) secondes")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .onAppear {
            lockoutRemaining = pinManager.lockoutRemaining
            lockoutTimer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in
                lockoutRemaining = pinManager.lockoutRemaining
                if lockoutRemaining <= 0 {
                    pin = ""
                    errorMessage = nil
                }
            }
        }
    }

    private func handlePINEntry() {
        if isSettingUp {
            guard pin.count >= 4 else {
                errorMessage = "Le PIN doit contenir au moins 4 chiffres"
                return
            }
            try? pinManager.setPIN(pin)
            isSettingUp = false
            proceedToApp()
        } else if pinManager.hasPIN {
            guard pinManager.verifyPIN(pin) else {
                pinManager.recordFailedAttempt()
                pin = ""
                errorMessage = "Code PIN incorrect"
                return
            }
            pinManager.resetAttempts()
            proceedToApp()
        }
    }

    private func proceedToApp() {
        withAnimation {
            isAuthenticated = true
            showLogin = false
        }
    }
}

struct PINEntryView: View {
    @Binding var pin: String
    let isSettingUp: Bool
    @Binding var errorMessage: String?
    let onPinComplete: () -> Void

    let pinLength = 6

    var body: some View {
        VStack(spacing: 20) {
            Text(isSettingUp ? "Créez votre code PIN" : "Entrez votre code PIN")
                .font(.title3.weight(.semibold))

            if let error = errorMessage {
                Text(error)
                    .font(.caption)
                    .foregroundStyle(AppColor.error)
            }

            HStack(spacing: 12) {
                ForEach(0..<pinLength, id: \.self) { index in
                    Circle()
                        .fill(index < pin.count ? AppColor.brand : AppColor.border)
                        .frame(width: 16, height: 16)
                }
            }

            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 3), spacing: 12) {
                ForEach(1...9, id: \.self) { number in
                    NumberButton(number: "\(number)") {
                        if pin.count < pinLength { pin += "\(number)" }
                        if pin.count == pinLength { onPinComplete() }
                    }
                }
                NumberButton(number: "") {}
                NumberButton(number: "0") {
                    if pin.count < pinLength { pin += "0" }
                    if pin.count == pinLength { onPinComplete() }
                }
                NumberButton(number: "⌫") {
                    if !pin.isEmpty { pin.removeLast() }
                }
            }
            .frame(maxWidth: 280)
        }
    }
}

struct NumberButton: View {
    let number: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(number)
                .font(.title2.weight(.medium))
                .frame(maxWidth: .infinity, minHeight: 56)
                .background(AppColor.surface)
                .clipShape(RoundedRectangle(cornerRadius: 10))
        }
        .disabled(number.isEmpty)
        .opacity(number.isEmpty ? 0 : 1)
    }
}
