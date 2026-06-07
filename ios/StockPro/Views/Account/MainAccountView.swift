import SwiftUI

struct MainAccountView: View {
    @StateObject private var vm = MainAccountViewModel()
    @State private var showDeposit = false
    @State private var showWithdraw = false
    @State private var showTransfer = false

    var body: some View {
        List {
            switch vm.state {
            case .loading:
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .listRowBackground(Color.clear)
            case .loaded(let account):
                Section {
                    VStack(spacing: 8) {
                        Text("Solde actuel")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        Text(String(format: "%.2f MAD", account.current_balance))
                            .font(.system(size: 34, weight: .bold))
                            .foregroundStyle(.primary)
                        if let initial = account.initial_balance {
                            Text("Solde initial: \(String(format: "%.2f MAD", initial))")
                                .font(.caption)
                                .foregroundStyle(.tertiary)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                }
                Section {
                    Button { showDeposit = true } label: {
                        Label("Déposer", systemImage: "arrow.down.circle")
                    }
                    Button { showWithdraw = true } label: {
                        Label("Retirer", systemImage: "arrow.up.circle")
                    }
                    Button { showTransfer = true } label: {
                        Label("Transférer vers Caisse", systemImage: "arrow.right.circle")
                    }
                }
                if !vm.transactions.isEmpty {
                    Section("Dernières opérations") {
                        ForEach(vm.transactions) { t in
                            TransactionRow(transaction: t)
                        }
                    }
                }
            case .empty:
                ContentUnavailableView("Compte non trouvé", systemImage: "banknote")
            case .error(let err):
                StockErrorView(message: err.errorDescription ?? "Erreur", onRetry: { Task { await vm.load() } })
            default:
                Color.clear
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Compte Principal")
        .task { await vm.load() }
        .refreshable { await vm.load() }
        .sheet(isPresented: $showDeposit) {
            TransactionFormView(mode: .deposit, vm: vm)
        }
        .sheet(isPresented: $showWithdraw) {
            TransactionFormView(mode: .withdraw, vm: vm)
        }
        .sheet(isPresented: $showTransfer) {
            TransactionFormView(mode: .transfer, vm: vm)
        }
    }
}

struct TransactionRow: View {
    let transaction: MainAccountTransactionDTO

    var body: some View {
        HStack {
            Circle()
                .fill(transaction.type == "in" ? Color.green : Color.red)
                .frame(width: 8, height: 8)
            VStack(alignment: .leading, spacing: 2) {
                Text(transaction.reason ?? transaction.type)
                    .font(.subheadline.weight(.medium))
                if let note = transaction.note, !note.isEmpty {
                    Text(note).font(.caption).foregroundStyle(.secondary)
                }
                if let date = transaction.created_at {
                    Text(date).font(.caption2).foregroundStyle(.tertiary)
                }
            }
            Spacer()
            Text(String(format: "%+.2f MAD", transaction.type == "in" ? transaction.amount : -transaction.amount))
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(transaction.type == "in" ? .green : .red)
        }
        .padding(.vertical, 4)
    }
}

enum TransactionMode: String {
    case deposit, withdraw, transfer

    var title: String {
        switch self {
        case .deposit: "Déposer"
        case .withdraw: "Retirer"
        case .transfer: "Transférer vers Caisse"
        }
    }

    var buttonLabel: String {
        switch self {
        case .deposit: "Déposer"
        case .withdraw: "Retirer"
        case .transfer: "Transférer"
        }
    }
}

struct TransactionFormView: View {
    let mode: TransactionMode
    @ObservedObject var vm: MainAccountViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var amount = ""
    @State private var reason = ""
    @State private var note = ""
    @State private var isLoading = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    HStack {
                        Text("Montant")
                        Spacer()
                        TextField("0.00", text: $amount)
                            .keyboardType(.decimalPad)
                            .multilineTextAlignment(.trailing)
                        Text("MAD").foregroundStyle(.secondary)
                    }
                    if mode != .transfer {
                        HStack {
                            Text("Motif")
                            Spacer()
                            TextField("Motif", text: $reason)
                                .multilineTextAlignment(.trailing)
                        }
                    }
                    HStack {
                        Text("Note")
                        Spacer()
                        TextField("Optionnelle", text: $note)
                            .multilineTextAlignment(.trailing)
                    }
                }
                if let error = errorMessage {
                    Section {
                        Text(error).foregroundStyle(.red).font(.caption)
                    }
                }
                Section {
                    Button(mode.buttonLabel) {
                        Task { await submit() }
                    }
                    .disabled(isLoading || amount.isEmpty)
                }
            }
            .navigationTitle(mode.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuler") { dismiss() }
                }
            }
            .disabled(isLoading)
        }
    }

    private func submit() async {
        guard let value = Double(amount.replacingOccurrences(of: ",", with: ".")), value > 0 else {
            errorMessage = "Montant invalide"
            return
        }
        isLoading = true
        errorMessage = nil
        let success: Bool
        switch mode {
        case .deposit:
            success = await vm.deposit(amount: value, reason: reason.isEmpty ? "Dépôt" : reason, note: note.isEmpty ? nil : note)
        case .withdraw:
            success = await vm.withdraw(amount: value, reason: reason.isEmpty ? "Retrait" : reason, note: note.isEmpty ? nil : note)
        case .transfer:
            success = await vm.transferToPOS(amount: value, note: note.isEmpty ? nil : note)
        }
        isLoading = false
        if success {
            dismiss()
        } else {
            errorMessage = "Opération échouée. Vérifiez le solde."
        }
    }
}
