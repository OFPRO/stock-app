"""Tests for Dashboard API endpoints (Tab 1)."""
import pytest


class TestDashboardKPIs:
    """Dashboard uses 12 API endpoints loaded in parallel by loadDashboard()."""

    def test_kpis_sales_happy_path(self, client, seed_data):
        rv = client.get("/api/kpis/sales?period=30")
        assert rv.status_code == 200
        data = rv.get_json()
        for key in ("ca_jour", "nb_ventes_jour", "ticket_moyen", "ca_periode", "nb_ventes_periode", "ca_trend"):
            assert key in data

    def test_kpis_sales_with_date_range(self, client, seed_data):
        rv = client.get("/api/kpis/sales?date_start=2026-01-01&date_end=2026-12-31")
        assert rv.status_code == 200

    def test_kpis_sales_period_7(self, client, seed_data):
        rv = client.get("/api/kpis/sales?period=7")
        assert rv.status_code == 200

    def test_kpis_sales_period_90(self, client, seed_data):
        rv = client.get("/api/kpis/sales?period=90")
        assert rv.status_code == 200

    def test_kpis_sales_no_params(self, client, seed_data):
        rv = client.get("/api/kpis/sales")
        assert rv.status_code == 200

    def test_kpis_margins_happy_path(self, client, seed_data):
        rv = client.get("/api/kpis/margins")
        assert rv.status_code == 200
        data = rv.get_json()
        assert "marge_globale" in data
        assert "categories" in data

    def test_kpis_receivables_happy_path(self, client, seed_data):
        rv = client.get("/api/kpis/receivables")
        assert rv.status_code == 200
        data = rv.get_json()
        for key in ("total_creances", "nb_impayees", "taux_encaissement", "clients"):
            assert key in data

    def test_kpis_invoices_status(self, client, seed_data):
        rv = client.get("/api/kpis/invoices-status")
        assert rv.status_code == 200
        data = rv.get_json()
        for key in ("brouillon", "envoyee", "payee", "annulee", "partiellement_payee"):
            assert key in data

    def test_kpis_dashboard_happy_path(self, client, seed_data):
        rv = client.get("/api/kpis/dashboard?period=30")
        assert rv.status_code == 200
        data = rv.get_json()
        for key in ("total_products", "total_value", "low_stock", "out_of_stock",
                     "products_to_order", "expiring_soon", "rotation_rate"):
            assert key in data

    def test_kpis_dashboard_with_warehouse(self, client, seed_data):
        wid = seed_data["warehouse_id"]
        rv = client.get(f"/api/kpis/dashboard?period=30&warehouse_id={wid}")
        assert rv.status_code == 200

    def test_kpis_dashboard_invalid_warehouse(self, client, seed_data):
        rv = client.get("/api/kpis/dashboard?warehouse_id=99999")
        assert rv.status_code == 200

    def test_kpis_dashboard_no_params(self, client, seed_data):
        rv = client.get("/api/kpis/dashboard")
        assert rv.status_code == 200

    def test_kpis_sales_daily_happy_path(self, client, seed_data):
        rv = client.get("/api/kpis/sales-daily?period=7")
        assert rv.status_code == 200
        data = rv.get_json()
        assert isinstance(data, list)
        if len(data) > 0:
            assert "date" in data[0]
            assert "ca" in data[0]

    def test_kpis_sales_daily_period_30(self, client, seed_data):
        rv = client.get("/api/kpis/sales-daily?period=30")
        assert rv.status_code == 200
        data = rv.get_json()
        assert len(data) > 0

    def test_kpis_categories_distribution(self, client, seed_data):
        rv = client.get("/api/kpis/categories-distribution")
        assert rv.status_code == 200
        data = rv.get_json()
        assert isinstance(data, list)

    def test_kpis_top_selling_products(self, client, seed_data):
        rv = client.get("/api/kpis/top-selling-products?limit=10")
        assert rv.status_code == 200
        data = rv.get_json()
        assert isinstance(data, list)
        assert len(data) <= 10

    def test_kpis_top_selling_products_limit_5(self, client, seed_data):
        rv = client.get("/api/kpis/top-selling-products?limit=5")
        assert rv.status_code == 200
        data = rv.get_json()
        assert len(data) <= 5

    def test_kpis_top_selling_products_invalid_limit(self, client, seed_data):
        rv = client.get("/api/kpis/top-selling-products?limit=-1")
        assert rv.status_code == 200

    def test_kpis_trends_happy_path(self, client, seed_data):
        rv = client.get("/api/kpis/trends?period=7")
        assert rv.status_code == 200
        data = rv.get_json()
        assert isinstance(data, list)
        if len(data) > 0:
            assert "date" in data[0]
            assert "entries" in data[0]
            assert "exits" in data[0]

    def test_kpis_trends_default_period(self, client, seed_data):
        rv = client.get("/api/kpis/trends")
        assert rv.status_code == 200

    def test_kpis_alertes_happy_path(self, client, seed_data):
        rv = client.get("/api/kpis/alertes")
        assert rv.status_code == 200
        data = rv.get_json()
        for key in ("low_stock", "out_of_stock", "expiring", "inactive"):
            assert key in data
        assert isinstance(data["low_stock"], list)

    def test_kpis_alertes_with_warehouse(self, client, seed_data):
        wid = seed_data["warehouse_id"]
        rv = client.get(f"/api/kpis/alertes?warehouse_id={wid}")
        assert rv.status_code == 200

    def test_warehouses_list(self, client, seed_data):
        rv = client.get("/api/warehouses")
        assert rv.status_code == 200
        data = rv.get_json()
        assert isinstance(data, list)
        assert len(data) >= 1

    def test_main_account(self, client, seed_data):
        rv = client.get("/api/main-account")
        assert rv.status_code == 200
        data = rv.get_json()
        assert "account" in data

    def test_kpis_generic_with_category(self, client, seed_data):
        rv = client.get('/api/kpis?category=Papeterie')
        assert rv.status_code == 200
        data = rv.get_json()
        for key in ("total_products", "total_value", "low_stock", "out_of_stock"):
            assert key in data

    def test_kpis_generic_with_warehouse_and_category(self, client, seed_data):
        wid = seed_data["warehouse_id"]
        rv = client.get(f'/api/kpis?warehouse_id={wid}&category=Papeterie')
        assert rv.status_code == 200

    def test_kpis_generic_with_dates(self, client, seed_data):
        rv = client.get('/api/kpis?date_start=2026-01-01&date_end=2026-12-31')
        assert rv.status_code == 200

    def test_stats_happy_path(self, client, seed_data):
        rv = client.get('/api/stats')
        assert rv.status_code == 200
        data = rv.get_json()
        for key in ("total", "low_stock", "total_value", "out_of_stock", "warehouses_count"):
            assert key in data

    def test_stats_with_warehouse(self, client, seed_data):
        wid = seed_data["warehouse_id"]
        rv = client.get(f'/api/stats?warehouse_id={wid}')
        assert rv.status_code == 200

    def test_kpis_by_location(self, client, seed_data):
        rv = client.get('/api/kpis/by-location')
        assert rv.status_code == 200
        data = rv.get_json()
        assert isinstance(data, list)

    def test_kpis_warehouse_overview(self, client, seed_data):
        rv = client.get('/api/kpis/warehouse-overview')
        assert rv.status_code == 200
        data = rv.get_json()
        assert isinstance(data, list)

    def test_kpis_orders_summary(self, client, seed_data):
        rv = client.get('/api/kpis/orders-summary')
        assert rv.status_code == 200
        data = rv.get_json()
        for key in ("brouillon", "recu", "paye", "total_value"):
            assert key in data

    def test_kpis_invoices_summary(self, client, seed_data):
        rv = client.get('/api/kpis/invoices-summary')
        assert rv.status_code == 200
        data = rv.get_json()
        for key in ("unpaid", "sent", "paid", "unpaid_amount"):
            assert key in data

    def test_kpis_customers_summary(self, client, seed_data):
        rv = client.get('/api/kpis/customers-summary')
        assert rv.status_code == 200
        data = rv.get_json()
        for key in ("total", "loyal"):
            assert key in data

    def test_kpis_evolution(self, client, seed_data):
        rv = client.get('/api/kpis/evolution?period=7')
        assert rv.status_code == 200
        data = rv.get_json()
        assert isinstance(data, list)

    def test_kpis_top_products(self, client, seed_data):
        rv = client.get('/api/kpis/top-products?limit=5')
        assert rv.status_code == 200
        data = rv.get_json()
        assert isinstance(data, list)
        assert len(data) <= 5
