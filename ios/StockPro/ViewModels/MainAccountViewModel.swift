import Foundation

@MainActor
final class MainAccountViewModel: ObservableObject {
    @Published var state: ViewState<MainAccountDTO> = .loading
    @Published var transactions: [MainAccountTransactionDTO] = []

    private let mainAccountService: MainAccountServiceProtocol

    init(mainAccountService: MainAccountServiceProtocol = MainAccountService()) {
        self.mainAccountService = mainAccountService
    }

    func load() async {
        state = .loading
        do {
            let response = try await mainAccountService.fetchMainAccount()
            state = .loaded(response.account)
            transactions = response.transactions
        } catch {
            state = .error(.from(error))
        }
    }

    func deposit(amount: Double, reason: String, note: String?) async -> Bool {
        do {
            let response = try await mainAccountService.deposit(amount: amount, reason: reason, note: note)
            state = .loaded(response.account)
            await load()
            return true
        } catch {
            return false
        }
    }

    func withdraw(amount: Double, reason: String, note: String?) async -> Bool {
        do {
            let response = try await mainAccountService.withdraw(amount: amount, reason: reason, note: note)
            state = .loaded(response.account)
            await load()
            return true
        } catch {
            return false
        }
    }

    func transferToPOS(amount: Double, note: String?) async -> Bool {
        do {
            let response = try await mainAccountService.transferToPOS(amount: amount, note: note)
            state = .loaded(response.account)
            await load()
            return true
        } catch {
            return false
        }
    }
}
