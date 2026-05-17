# Audit SPA Legacy — Rapport Progressif

**Date :** 2026-05-16
**Seed :** 50 produits, 30 clients, 60 factures, 20 POS transactions
**Statut :** ⏳ En cours

## Résumé

| Tab | Statut | Bugs | Fixes | Health Score |
|-----|--------|------|-------|-------------|
| Dashboard | ✅ | 3 | 3 | 10/10 |
| Produits | ⏳ | - | - | - |
| Entrepôts | ⏳ | - | - | - |
| Emplacements | ⏳ | - | - | - |
| Mouvements | ⏳ | - | - | - |
| Fournisseurs | ⏳ | - | - | - |
| Bons de commande | ⏳ | - | - | - |
| Clients | ⏳ | - | - | - |
| Factures | ⏳ | - | - | - |
| Rapports | ⏳ | - | - | - |
| Sessions POS | ⏳ | - | - | - |
| Scanner | ⏳ | - | - | - |
| Compte principal | ⏳ | - | - | - |
| Notifications | ⏳ | - | - | - |

## Détail par Tab

### Tab 1 : Dashboard ✅

**API Tests** — 36/36 passés (0.52s)
- `/api/kpis/sales` ✅ 5 tests (periods 7/30/90, date range, no params)
- `/api/kpis/margins` ✅ 1 test
- `/api/kpis/receivables` ✅ 1 test
- `/api/kpis/invoices-status` ✅ 1 test
- `/api/kpis/dashboard` ✅ 4 tests (warehouse, invalid wh, no params)
- `/api/kpis/sales-daily` ✅ 2 tests (period 7, length verification 30)
- `/api/kpis/categories-distribution` ✅ 1 test
- `/api/kpis/top-selling-products` ✅ 3 tests (limit 10/5, invalid limit)
- `/api/kpis/trends` ✅ 2 tests
- `/api/kpis/alertes` ✅ 2 tests
- `/api/warehouses` ✅ 1 test
- `/api/main-account` ✅ 1 test
- `/api/kpis`, `/api/stats`, `/api/kpis/by-location`, `/api/kpis/warehouse-overview`,
  `/api/kpis/orders-summary`, `/api/kpis/invoices-summary`, `/api/kpis/customers-summary`,
  `/api/kpis/evolution`, `/api/kpis/top-products` ✅ 12 tests

**Browser Tests**
- 9 KPI cards with real data (CA: 3,214.55 DH, Ventes: 4, Ticket: 460.32, Marge: 39.8%)
- 6 charts ApexCharts rendus ✅
- 4 data tables chargées ✅
- Filtre période 7/30/90 ✅
- Filtre entrepôt (valeur stock 53,456→25,472) ✅
- Bouton rafraîchir ✅
- Dark/Light mode toggle ✅
- Console errors: 0 ✅

**Fichier de fix :** `routes/kpis.py`

### Anomalies corrigées

| # | Problème | Cause | Fix |
|---|----------|-------|-----|
| 1 | out_of_stock = 0 malgré un produit avec qty=0 | Condition `quantity < 0` au lieu de `quantity <= 0` | 10 occurrences remplacées ✅ |
| 2 | rotation_rate = 4518% (illogique) | Formule `out_movements / total_products * 100` | Remplacé par `out_movements / total_movements * 100` = 62.7% ✅ |
| 3 | Daily sales = 30 entrées, période = 31 jours | Boucle `range(period-1, -1, -1)` manquait 1 jour | Changé en `range(period, -1, -1)` = 31 entrées ✅ |

**Bugs trouvés :** 3 | **Corrigés :** 3 | **Health Score :** 10/10
