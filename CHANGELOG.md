# Changelog

## v0.5 (2026-05-16)

### Bugs corrigés — Module POS

- **Critique** : `POST /api/main-account/transfer-to-pos` retournait 500 (colonne `source` manquante dans `pos_cash_movements`)
- **Critique** : `GET /api/pos/transaction-by-invoice/<num>` retournait 500 (colonne `invoice_id` manquante dans `pos_transactions`)
- **Critique** : PDF des factures POS en 404 sur le frontend React (`document_number` vs `document_id`)
- **Haut** : `get_pos_customers` en 500 (`is_active` manquant dans `customers`) — hotfix manuel
- **Haut** : `generate_invoice_pdf` en 500 (`ice` manquant dans `customers`) — hotfix manuel
- **Haut** : `get_invoices` en erreur (`supplier_id` manquant dans `invoices`) — hotfix manuel
- **Haut** : Colonnes `payment_method`, `type`, `tendered_amount`, `change_given` absentes de `invoices` — hotfix manuel
- **Moyen** : Remises client (loyal -15%, étudiant -15%, école -20%) non appliquées dans les transactions POS
- **Moyen** : Double discount dans `recalculate_invoice` (remise soustraite deux fois)
- **Moyen** : Collision de numérotation FAC entre POS et création manuelle d'invoices

### Améliorations

- **Architecture** : Table `sequences` partagée pour la numérotation FAC centralisée
- **Migration** : `init_db()` gère désormais toutes les colonnes manquantes en try/except
- **Robustesse** : Protection NULL sur `discount_percent` et `tax_rate` dans `recalculate_invoice`
- **Sécurité** : Ajout de `_safe_int()` et `.get()` pour les entrées utilisateur
