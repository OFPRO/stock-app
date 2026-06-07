import SwiftUI

struct ReorderRuleFormView: View {
    @StateObject private var vm = ReorderRuleFormViewModel()
    @Environment(\.dismiss) private var dismiss
    let onSave: () -> Void

    var body: some View {
        NavigationStack {
            Form {
                Section("Produit") {
                    LabeledContent("ID Produit") {
                        TextField("ID", value: $vm.productId, format: .number)
                            .keyboardType(.numberPad)
                            .multilineTextAlignment(.trailing)
                    }
                    LabeledContent("ID Fournisseur") {
                        TextField("ID (optionnel)", value: $vm.supplierId, format: .number)
                            .keyboardType(.numberPad)
                            .multilineTextAlignment(.trailing)
                    }
                }
                Section("Seuils") {
                    LabeledContent("Quantité Min.") {
                        TextField("Min", text: $vm.minQuantity)
                            .keyboardType(.numberPad)
                            .multilineTextAlignment(.trailing)
                    }
                    LabeledContent("Quantité Max.") {
                        TextField("Max", text: $vm.maxQuantity)
                            .keyboardType(.numberPad)
                            .multilineTextAlignment(.trailing)
                    }
                }
                Section("Déclencheur") {
                    Picker("Type", selection: $vm.triggerType) {
                        Text("Automatique").tag("auto")
                        Text("Manuel").tag("manual")
                    }
                }
                if let error = vm.error {
                    Section { Text(error.errorDescription ?? "Erreur").foregroundStyle(.red) }
                }
            }
            .navigationTitle("Nouvelle Règle")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Annuler") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Ajouter") {
                        Task {
                            if await vm.save() {
                                onSave()
                                dismiss()
                            }
                        }
                    }
                    .disabled(!vm.isValid || vm.saving)
                }
            }
        }
    }
}
