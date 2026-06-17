# Changelog

## v1.4.1 (2026-06-17)

### Correctif — Sauvegarde paramètres imprimante

- **Bug** : `PUT /api/settings/printer` écrasait tous les champs avec des valeurs par défaut quand un seul champ était envoyé. Par exemple `{"host": "XP-80"}` réinitialisait `connection_type` à `"network"`.
- **Fix** : la fonction `update_printer_settings()` lit d'abord les réglages existants, fusionne avec les données reçues, puis sauvegarde le résultat. Les champs non envoyés conservent leur valeur précédente.

## v1.4.0 (2026-06-17)

### Reset DB — Réécriture complète

- **Nouvelle fonction** `reset_transactional_data()` dans `reset_test_db.py` : vide TOUTES les données (produits, stock, clients, fournisseurs, factures, POS, commandes, mouvements, notifications, séquences, entrepôts, emplacements, compte principal) en préservant uniquement les catégories
- Reconstruction automatique : entrepôt par défaut + emplacement "Zone principale", Caisse 1 & Caisse 2, compte principal à 0
- `init_store_db()` dans `routes/db.py` : seed des `pos_registers` avec Caisse 1 et Caisse 2

### Nouveau KPI — Produits en Stock

- **Nouveau champ** `products_in_stock` dans `routes/kpis.py` : décompte des produits avec quantité > 0 ET `is_deleted = 0` (filtre entrepôt actif + branche globale)
- **Carte KPI** dans `templates/index.html` : nouvelle carte entre "Valeur Stock PA" et "Ruptures" avec icône `fa-cubes`
- **Dashboard JS** : `static/js/dashboard.js` populate `produitsEnStock` + message de confirmation reset mis à jour

### Mouvement de Stock à la Création Produit

- `routes/products.py` : `add_product()` crée désormais un enregistrement `stock_movements` de type `'in'` avec la note "Entrée initiale - création du produit" lorsque la quantité initiale est > 0

### Correctif Sidebar — Onglet Actif

- `static/js/app.js` : `showTab()` réajoute la classe `active` sur l'élément `.nav-item[data-arg="${tab}"]` après l'avoir retirée de tous les items — le fond dégradé ne disparaît plus au changement d'onglet

### Scanner Code-Barres — Correctif QWERTY→AZERTY

- `static/js/products.js` : Le handler clavier utilise `event.code` (position physique de la touche) au lieu de `event.key` (caractère), ce qui fonctionne quel que soit le pavé clavier (QWERTY, AZERTY...)
- Nettoyage côté serveur : suppression des caractères non-numériques dans les codes-barres scannés

### Imprimantes Thermiques — Support Windows

- **Nouvelle fonction** `_discover_windows()` dans `services/printing_service.py` : scanne le Registre Windows (`HKLM\SYSTEM\CurrentControlSet\Enum\USB`) pour détecter les imprimantes USB utilisant `usbprint.sys` (VID, PID, instance_id, nom)
- `discover_printers()` sélectionne automatiquement la méthode selon la plateforme : Registry Windows → pyusb (fallback)
- `check_printer_status()` gère désormais les connexions USB/Windows sans champ `host`

- **Nouvelle classe** `_WindowsRawPrinter` dans `services/escpos_receipt.py` :
  - Connexion directe via `CreateFile` sur le chemin `\\?\USB#VID_...&PID_...#INSTANCE_...#{GUID_USBPRINT}` avec `FILE_SHARE_WRITE`
  - Fallback `win32print.OpenPrinter()` si l'imprimante est installée dans Windows
  - Implémentation complète : `_raw()`, `text()`, `set()`, `close()`, `cut()`, `qr()`, `barcode()`
  - Toutes les importations `pywin32` sont conditionnelles (try/except) → pas d'impact sur macOS/Linux

- **UI** : `static/js/settings.js` ajoute l'option "Imprimante Windows" dans le menu Type de connexion + champ pour le nom + scan détecte et remplit automatiquement
- **PyInstaller** : `build/pyinstaller.spec` ajoute `win32print` et `win32file` aux `hiddenimports`

## v1.3.0 (2026-06-15)

### Module Paramètres — Magasins Multi-Base + Catégories

- **Nouvelle architecture** : 1 base de données par magasin (`stock.db` → Papeterie AlQalam, `stock_N.db` → magasin N)
- **Nouveau blueprint** `routes/stores.py` : CRUD magasins, switch, archive, réactivation, gestion catégories
- **Résolution dynamique** : `get_db()` sélectionne automatiquement la base via `session['active_store_id']`
- **Nouveau magasin** : base vierge avec catégories pré-remplies (22), entrepôt + emplacement par défaut, 0 produits/clients/factures
- **Frontend legacy** : onglet "Paramètres" dans la sidebar, sélecteur de magasin stylisé dans le header
- **Catégories** : CRUD complet (ajouter, modifier, supprimer) depuis l'interface paramètres

### Scan Caméra — Toggle Manuel

- **Caméra désactivée par défaut** à l'ouverture du modal "Nouveau Produit"
- **Bouton toggle** "Activer le scan" / "Désactiver le scan" (pas de démarrage automatique)
- **Écran noir caché** : la zone vidéo reste masquée (`display:none`) tant que le scan n'est pas activé
- **Tous les chemins de retour** (annulation, code-barres déjà utilisé) réinitialisent le toggle à OFF

## v1.2.0 (2026-06-15)

### Nettoyage Caisse

- **Registers** : `init_db()` désactive les caisses autres que "Caisse 1" et "Caisse 2"
- **Monnaie** : Affichage "monnaie rendu" retiré du DOM (alerte conservée)
- **Dashboard** : Limite des KPIs caisses passée de 3 à 2

### Catégories Produits — Refonte Complète

- **Nouvelle table** `categories` avec 22 entrées bilingues (`name_ar`, `name_fr`)
- **Migration** : 55 produits reclassés des 8 anciennes catégories vers les 22 nouvelles
- **API** : `GET /api/categories` retourne `[{id, name_ar, name_fr}]` au lieu de `string[]`
- **API** : `GET /api/products` inclut `category_ar` via `LEFT JOIN categories`
- **Seed** : `seed.py` synchronisé avec les nouvelles catégories

### Renommages

| ID | Arabe | Français |
|----|-------|----------|
| 4 | المحافظ | Cartables *(ex: Trousses)* |
| 5 | مقلمة | Trousses *(ex: Carnets)* |
| 6 | لانش بوكس | Launch Box *(ex: Pochettes classeurs)* |
| 7 | آلة حسابية | Calculatrices *(ex: Fournitures calcul)* |
| 11 | ستيلو/ قلم الرصاص | Stylos à bille/Crayons |
| 12 | ملونات الخشب / الشمع | *(inchangé)* |
| 15 | ادوات الصباغة و الرسم | Dessins/peinture |

### Frontend Legacy (Jinja2)

- `products.js` : Nouvelle fonction `loadCategories()` pour dropdown dynamique
- `index.html` : Options catégories hardcodées supprimées du `<select>`
- `product_detail.html` : Affichage bilingue `category_ar / category`
- `dashboard.js` : Limite boucle KPIs caisses `i < 2`

### Frontend React

- **Types** : `Category {id, name_ar, name_fr}` ajouté dans `api.ts`
- **ProductsPage** : Filtre et tableau affichent `name_ar / name_fr`
- **ProductDetailDialog/Page** : Affichage bilingue des catégories
- **Formulaire** : `NativeSelect` des catégories utilise `name_ar / name_fr`

### Base de données & Tests

- `init_db()` refactorisée : `DELETE + INSERT` au lieu de `INSERT OR IGNORE` (évite les lignes périmées)
- `conftest.py` : `PRAGMA foreign_keys=OFF` dans le cleanup, `categories` ajouté à la liste de nettoyage
- **163 tests passent** (100%)
- `test_api_categories` valide le nouveau format `[{id, name_ar, name_fr}]`

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
