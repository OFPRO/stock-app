# Changelog

## v0.8 (2026-06-03)

### Qualité (Phase 5b) — 8 correctifs

- **Q1** : `_safe_int()` extrait dans `routes/db.py`, supprimé les duplications
- **Q2** : `_esc()`/`_n()` factorisés au niveau module dans `app.py`
- **Q3-Q4** : Imports morts (`csv`, `Response`, `StringIO`, `get_db_ctx`) supprimés
- **Q5-Q6** : `warehouse_id.isdigit()` → `validate_id()` unifié dans 16 occurrences (app.py, kpis.py, warehouses.py, locations.py)
- **Q7** : `'carte'` → `'card'` unifié dans `kpis.py`
- **Q8** : 3 boucles N+1 dans `kpis.py` remplacées par des `GROUP BY date()` (sales_daily, trends, evolution)

### Sécurité (Phase 5c) — 3 correctifs

- **S1** : `app.secret_key` ajouté (sessions Flask signées)
- **S3** : `templates/index.html.bak` untracked (stale SPA 173KB, plus servi)
- **S4** : `SESSION_COOKIE_SAMESITE = 'Lax'` configuré

### Tests (Phase 5d) — 5 correctifs

- **T1** : `test_nonexistent_invoice` retourne 404 au lieu de 500
- **T2** : Assertions body ajoutées à 7 tests nus
- **T3** : `test_stock_transfer` utilise 2 emplacements différents
- **T4** : `test_update_location` ajoute vrai PUT après POST
- **T5** : `test_kpis_sales_daily_period_30` supprime assertion `len(data) == 31` rigide

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
