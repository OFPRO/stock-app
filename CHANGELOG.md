# Changelog

## v0.6.2 (2026-05-19)

### Features — Scanners (POS & ZXing)

- **Nouveau** : Scanner code-barres intégré dans le POS (BarcodeDetector API) — compact 180px, toggle button, auto-add au panier
- **Nouveau** : `/scanner-pro` — page test autonome BarcodeDetector en 1280×720 pour petits codes-barres
- **Nouveau** : Brouillard caméra — `filter: blur(12px)` sur la vidéo + foncé semi-transparent sur le canvas, seule la zone du code-barre est révélée en net. Visage masqué
- **Amélioration** : `TRY_HARDER` hint activé pour les petits codes-barres denses (produits cosmétiques)

### Bugs corrigés — Module Scanner

- **Critique** : `non-ReaderException` de ZXing v0.23.0 après scans répétés — try/catch + reset auto après 8 erreurs consécutives
- **Moyen** : Double scan d'un même code dans la fenêtre de déduplication — `scanHistory` avec incrément de quantité

### Performances

- Interval de détection 500ms → 150ms (×3.3)
- Formats réduits de 9 à 5 (QR, EAN-13, EAN-8, CODE-128, CODE-39)

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
