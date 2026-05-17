# Dashboard — Rapport de Test

## API Tests
- **Fichier :** `tests/test_dashboard.py`
- **Tests :** 36 tests — 36 passés (0 en échec)
- **Temps :** 0.52s
- **Couverture :**
  - `/api/kpis/sales` — 5 tests (period 7/30/90, date range, no params)
  - `/api/kpis/margins` — 1 test
  - `/api/kpis/receivables` — 1 test
  - `/api/kpis/invoices-status` — 1 test
  - `/api/kpis/dashboard` — 4 tests (warehouse, invalid wh, no params)
  - `/api/kpis/sales-daily` — 2 tests (period 7, period 30 exact length)
  - `/api/kpis/categories-distribution` — 1 test
  - `/api/kpis/top-selling-products` — 3 tests (limit 10/5, invalid limit)
  - `/api/kpis/trends` — 2 tests
  - `/api/kpis/alertes` — 2 tests (with/without warehouse)
  - `/api/warehouses` — 1 test
  - `/api/main-account` — 1 test
  - `/api/kpis` — 3 tests (category, warehouse+category, dates)
  - `/api/stats` — 2 tests (with/without warehouse)
  - `/api/kpis/by-location` — 1 test
  - `/api/kpis/warehouse-overview` — 1 test
  - `/api/kpis/orders-summary` — 1 test
  - `/api/kpis/invoices-summary` — 1 test
  - `/api/kpis/customers-summary` — 1 test
  - `/api/kpis/evolution` — 1 test
  - `/api/kpis/top-products` — 1 test

## Browser Tests
<!-- À remplir après gstack browse -->
