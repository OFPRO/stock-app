import SwiftUI

struct SessionHistoryView: View {
    @StateObject private var vm = SessionHistoryViewModel()

    var body: some View {
        List {
            switch vm.summary {
            case .loading:
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .listRowBackground(Color.clear)
            case .loaded(let s):
                Section("Résumé (30 jours)") {
                    HStack {
                        KPIItem(label: "Sessions", value: "\(s.total_sessions ?? 0)")
                        KPIItem(label: "Fermées", value: "\(s.closed_sessions ?? 0)")
                        KPIItem(label: "Ouvertes", value: "\(s.open_sessions ?? 0)")
                    }
                    HStack {
                        KPIItem(label: "Ventes", value: String(format: "%.0f MAD", s.total_sales_period ?? 0))
                        KPIItem(label: "Transactions", value: "\(s.nb_transactions_period ?? 0)")
                    }
                }
            case .error(let err):
                StockErrorView(message: err.errorDescription ?? "Erreur", onRetry: { Task { await vm.load() } })
                    .listRowBackground(Color.clear)
            default:
                Color.clear.listRowBackground(Color.clear)
            }

            switch vm.state {
            case .loading:
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .listRowBackground(Color.clear)
            case .loaded(let sessions):
                Section("Historique") {
                    ForEach(sessions) { session in
                        NavigationLink(destination: SessionDetailView(sessionId: session.id, sessionService: SessionService())) {
                            SessionRow(session: session)
                        }
                    }
                }
            case .empty(let msg):
                ContentUnavailableView(msg, systemImage: "clock")
            case .error(let err):
                StockErrorView(message: err.errorDescription ?? "Erreur", onRetry: { Task { await vm.load() } })
            default:
                Color.clear
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Historique des Sessions")
        .task { await vm.load() }
        .refreshable { await vm.load() }
    }
}

struct SessionRow: View {
    let session: SessionHistoryItem

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(session.session_number ?? "Session #\(session.id)")
                    .font(.subheadline.weight(.medium))
                Spacer()
                Text(session.status == "closed" ? "Fermée" : "Ouverte")
                    .font(.caption2.weight(.semibold))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(session.status == "closed" ? Color.green.opacity(0.15) : Color.blue.opacity(0.15))
                    .foregroundStyle(session.status == "closed" ? .green : .blue)
                    .clipShape(Capsule())
            }
            if let name = session.warehouse_name {
                Text(name).font(.caption).foregroundStyle(.secondary)
            }
            HStack(spacing: 16) {
                if let sales = session.total_sales {
                    Label(String(format: "%.0f MAD", sales), systemImage: "dollarsign")
                        .font(.caption).foregroundStyle(.primary)
                }
                if let nb = session.nb_transactions {
                    Label("\(nb)", systemImage: "receipt")
                        .font(.caption).foregroundStyle(.secondary)
                }
            }
            if let opened = session.opened_at {
                Text(opened).font(.caption2).foregroundStyle(.tertiary)
            }
        }
        .padding(.vertical, 4)
    }
}

struct KPIItem: View {
    let label: String
    let value: String

    var body: some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.subheadline.weight(.semibold))
            Text(label)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

struct SessionDetailView: View {
    let sessionId: Int
    let sessionService: SessionServiceProtocol

    @State private var detail: SessionDetailResponse?
    @State private var isLoading = true
    @State private var errorMessage: String?

    var body: some View {
        Group {
            if isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let error = errorMessage {
                StockErrorView(message: error, onRetry: { Task { await load() } })
            } else if let detail = detail {
                List {
                    Section("Session") {
                        if let num = detail.session.session_number {
                            LabeledContent("Numéro", value: num)
                        }
                        if let wh = detail.session.warehouse_name {
                            LabeledContent("Entrepôt", value: wh)
                        }
                        LabeledContent("Statut", value: detail.session.status == "closed" ? "Fermée" : "Ouverte")
                        if let opened = detail.session.opened_at {
                            LabeledContent("Ouverte", value: opened)
                        }
                        if let closed = detail.session.closed_at {
                            LabeledContent("Fermée", value: closed)
                        }
                        if let sales = detail.session.total_sales {
                            LabeledContent("Total ventes", value: String(format: "%.2f MAD", sales))
                        }
                        if let opening = detail.session.opening_cash {
                            LabeledContent("Fonds d'ouverture", value: String(format: "%.2f MAD", opening))
                        }
                        if let closing = detail.session.closing_cash {
                            LabeledContent("Fonds de clôture", value: String(format: "%.2f MAD", closing))
                        }
                    }

                    if !detail.transactions.isEmpty {
                        Section("Transactions (\(detail.transactions.count))") {
                            ForEach(detail.transactions) { t in
                                VStack(alignment: .leading, spacing: 2) {
                                    HStack {
                                        Text(t.ticket_number ?? "Tx #\(t.id)")
                                            .font(.subheadline.weight(.medium))
                                        Spacer()
                                        if let total = t.total {
                                            Text(String(format: "%.2f MAD", total))
                                                .font(.subheadline.weight(.semibold))
                                        }
                                    }
                                    if let name = t.customer_name {
                                        Text(name).font(.caption).foregroundStyle(.secondary)
                                    }
                                    HStack {
                                        if let method = t.payment_method {
                                            Text(method).font(.caption2).foregroundStyle(.tertiary)
                                        }
                                        if let count = t.items_count {
                                            Text("\(count) articles").font(.caption2).foregroundStyle(.tertiary)
                                        }
                                        Spacer()
                                        if let date = t.created_at {
                                            Text(date).font(.caption2).foregroundStyle(.tertiary)
                                        }
                                    }
                                }
                                .padding(.vertical, 2)
                            }
                        }
                    }

                    if !detail.cash_movements.isEmpty {
                        Section("Mouvements de caisse") {
                            ForEach(detail.cash_movements) { m in
                                HStack {
                                    Circle()
                                        .fill(m.type == "in" ? Color.green : Color.red)
                                        .frame(width: 8, height: 8)
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(m.reason ?? m.type ?? "")
                                            .font(.subheadline)
                                        if let note = m.note {
                                            Text(note).font(.caption).foregroundStyle(.secondary)
                                        }
                                    }
                                    Spacer()
                                    if let amount = m.amount {
                                        Text(String(format: "%@%.2f MAD", m.type == "in" ? "+" : "-", amount))
                                            .font(.subheadline.weight(.semibold))
                                            .foregroundStyle(m.type == "in" ? .green : .red)
                                    }
                                }
                                .padding(.vertical, 2)
                            }
                        }
                    }
                }
                .listStyle(.insetGrouped)
            } else {
                Color.clear
            }
        }
        .navigationTitle("Détails Session")
        .task { await load() }
    }

    private func load() async {
        isLoading = true
        errorMessage = nil
        do {
            detail = try await sessionService.fetchDetails(sessionId: sessionId)
        } catch {
            errorMessage = (error as? AppError)?.errorDescription ?? error.localizedDescription
        }
        isLoading = false
    }
}
