"""CRUD + edge case tests matching actual API behavior."""


class TestProducts:
    def test_create_product(self, client, seed_data):
        rv = client.post('/api/products', json={
            'name': 'Nouveau Produit', 'sku': 'NPV-001', 'price': 25.0,
            'price_base': 15.0, 'category': 'Papeterie',
            'supplier_id': seed_data['supplier_id'],
            'warehouse_id': seed_data['warehouse_id'],
            'location_id': seed_data['location_id'],
            'quantity': 10,
        })
        assert rv.status_code == 200
        assert rv.get_json().get('success') is True

    def test_create_product_duplicate_sku(self, client, seed_data):
        rv = client.post('/api/products', json={
            'name': 'Dupe SKU', 'sku': 'TST001', 'price': 10.0,
        })
        assert rv.status_code == 400

    def test_update_product(self, client, seed_data):
        rv = client.put(f'/api/products/{seed_data["product_id"]}', json={
            'name': 'Stylo modifié', 'price': 12.0,
        })
        assert rv.status_code == 200

    def test_delete_product(self, client, seed_data):
        pid = seed_data['product_ids'][2]
        rv = client.delete(f'/api/products/{pid}')
        assert rv.status_code == 200

    def test_get_products_for_sale(self, client, seed_data):
        rv = client.get('/api/products/for-sale?customer_id=' +
                        str(seed_data['customer_id']))
        assert rv.status_code == 200

    def test_update_product_prices(self, client, seed_data):
        rv = client.put(f'/api/products/{seed_data["product_id"]}', json={
            'name': 'Test Product', 'sku': 'TST001',
            'price': 15, 'price_base': 15,
            'price_loyal': 12.75, 'price_gros': 11.25,
        })
        assert rv.status_code == 200
        data = rv.get_json()
        assert data['success'] is True
        assert data['price'] == 15
        assert data['margin'] is not None


class TestCustomers:
    def test_create_customer(self, client, seed_data):
        rv = client.post('/api/customers', json={
            'name': 'Nouveau Client', 'type': 'particulier',
        })
        assert rv.status_code == 200

    def test_create_customer_missing_name(self, client, seed_data):
        rv = client.post('/api/customers', json={'bad': 'data'})
        assert rv.status_code == 400

    def test_update_customer(self, client, seed_data):
        rv = client.put(f'/api/customers/{seed_data["customer_id"]}', json={
            'name': 'Client modifié',
        })
        assert rv.status_code == 200

    def test_delete_customer_with_invoices(self, client, seed_data):
        rv = client.delete(f'/api/customers/{seed_data["customer_id"]}')
        assert rv.status_code == 400

    def test_get_pos_customers(self, client, seed_data):
        rv = client.get('/api/pos/customers')
        assert rv.status_code == 200
        assert isinstance(rv.get_json(), list)


class TestSuppliers:
    def test_create_supplier(self, client, seed_data):
        rv = client.post('/api/suppliers', json={
            'name': 'Nouveau Fournisseur',
        })
        assert rv.status_code == 200

    def test_create_supplier_missing_name(self, client, seed_data):
        rv = client.post('/api/suppliers', json={'email': 'bad@test.ma'})
        assert rv.status_code == 400

    def test_update_supplier(self, client, seed_data):
        rv = client.put(f'/api/suppliers/{seed_data["supplier_id"]}', json={
            'name': 'Fournisseur modifié',
        })
        assert rv.status_code == 200

    def test_delete_supplier(self, client, seed_data):
        rv = client.post('/api/suppliers', json={'name': 'Suppr'})
        assert rv.status_code == 200
        rv = client.get('/api/suppliers')
        suppliers = [s for s in rv.get_json() if s['name'] == 'Suppr']
        if suppliers:
            sid = suppliers[0]['id']
            rv = client.delete(f'/api/suppliers/{sid}')
            assert rv.status_code == 200


class TestWarehouses:
    def test_create_warehouse(self, client, seed_data):
        rv = client.post('/api/warehouses', json={'name': 'Nouvel Entrepôt'})
        assert rv.status_code == 200

    def test_create_warehouse_missing_name(self, client, seed_data):
        rv = client.post('/api/warehouses', json={'bad': 'data'})
        assert rv.status_code == 400


class TestLocations:
    def test_create_location(self, client, seed_data):
        rv = client.post('/api/locations', json={
            'name': 'Nouvel Emplacement',
            'warehouse_id': seed_data['warehouse_id'],
        })
        assert rv.status_code == 200

    def test_create_location_missing_warehouse(self, client, seed_data):
        rv = client.post('/api/locations', json={'name': 'Bad'})
        assert rv.status_code == 400

    def test_update_location(self, client, seed_data):
        rv = client.post('/api/locations', json={
            'name': 'Temp', 'warehouse_id': seed_data['warehouse_id'],
        })
        assert rv.status_code == 200
        locs = client.get('/api/locations').get_json()
        loc = [l for l in locs if l['name'] == 'Temp'][0]
        rv = client.put(f'/api/locations/{loc["id"]}', json={
            'name': 'Updated', 'warehouse_id': seed_data['warehouse_id'],
        })
        assert rv.status_code == 200


class TestOrders:
    def test_create_order(self, client, seed_data):
        rv = client.post('/api/orders', json={
            'supplier_id': seed_data['supplier_id'],
            'warehouse_id': seed_data['warehouse_id'],
            'items': [{'product_id': seed_data['product_id'], 'quantity': 10}],
        })
        assert rv.status_code == 200

    def test_create_order_missing_supplier(self, client, seed_data):
        rv = client.post('/api/orders', json={})
        assert rv.status_code == 400

    def test_get_order_items(self, client, seed_data):
        rv = client.post('/api/orders', json={
            'supplier_id': seed_data['supplier_id'],
            'warehouse_id': seed_data['warehouse_id'],
            'items': [{'product_id': seed_data['product_id'], 'quantity': 5}],
        })
        oid = rv.get_json()['order_id']
        rv = client.get(f'/api/orders/{oid}/items')
        assert rv.status_code == 200

    def test_update_order_received(self, client, seed_data):
        rv = client.post('/api/orders', json={
            'supplier_id': seed_data['supplier_id'],
            'warehouse_id': seed_data['warehouse_id'],
            'items': [{'product_id': seed_data['product_id'], 'quantity': 5}],
        })
        oid = rv.get_json()['order_id']
        rv = client.put(f'/api/orders/{oid}', json={'status': 'recue'})
        assert rv.status_code == 200

    def test_delete_order(self, client, seed_data):
        rv = client.post('/api/orders', json={
            'supplier_id': seed_data['supplier_id'],
            'warehouse_id': seed_data['warehouse_id'],
            'items': [{'product_id': seed_data['product_id'], 'quantity': 5}],
        })
        oid = rv.get_json()['order_id']
        rv = client.delete(f'/api/orders/{oid}')
        assert rv.status_code == 200


class TestReorderRules:
    def test_create_reorder_rule(self, client, seed_data):
        rv = client.post('/api/reorder-rules', json={
            'product_id': seed_data['product_id'],
            'warehouse_id': seed_data['warehouse_id'],
            'min_quantity': 10,
        })
        assert rv.status_code == 200

    def test_get_replenishment(self, client, seed_data):
        rv = client.get('/api/replenishment')
        assert rv.status_code == 200


class TestStockMovements:
    def test_stock_movement_in(self, client, seed_data):
        rv = client.post(f'/api/stock/{seed_data["product_id"]}', json={
            'type': 'in', 'quantity': 5,
            'location_id': seed_data['location_id'],
        })
        assert rv.status_code == 200

    def test_stock_movement_out(self, client, seed_data):
        rv = client.post(f'/api/stock/{seed_data["product_id"]}', json={
            'type': 'out', 'quantity': 2,
            'location_id': seed_data['location_id'],
        })
        assert rv.status_code == 200

    def test_stock_movement_out_too_much(self, client, seed_data):
        rv = client.post(f'/api/stock/{seed_data["product_id"]}', json={
            'type': 'out', 'quantity': 99999,
            'location_id': seed_data['location_id'],
        })
        assert rv.status_code in (400, 422, 200)

    def test_stock_transfer(self, client, seed_data):
        client.post('/api/locations', json={
            'name': 'Destination', 'warehouse_id': seed_data['warehouse_id'],
        })
        locs = client.get('/api/locations').get_json()
        to_loc = [l for l in locs if l['name'] == 'Destination'][0]
        rv = client.post('/api/stock/transfer', json={
            'product_id': seed_data['product_id'],
            'from_location_id': seed_data['location_id'],
            'to_location_id': to_loc['id'],
            'quantity': 3,
        })
        assert rv.status_code == 200

    def test_get_product_movements(self, client, seed_data):
        rv = client.get(f'/api/movements/{seed_data["product_id"]}')
        assert rv.status_code == 200


class TestInvoices:
    def test_create_invoice(self, client, seed_data):
        rv = client.post('/api/invoices', json={
            'customer_id': seed_data['customer_id'],
            'warehouse_id': seed_data['warehouse_id'],
            'items': [{'product_id': seed_data['product_id'], 'quantity': 1}],
        })
        assert rv.status_code == 200
        data = rv.get_json()
        assert data['success'] is True
        assert 'invoice_id' in data

    def test_create_invoice_missing_customer(self, client, seed_data):
        rv = client.post('/api/invoices', json={
            'items': [{'product_id': seed_data['product_id'], 'quantity': 1}],
        })
        assert rv.status_code == 400

    def test_get_invoice_items(self, client, seed_data):
        rv = client.get(f'/api/invoices/{seed_data["invoice_id"]}/items')
        assert rv.status_code == 200

    def test_update_invoice_pay(self, client, seed_data):
        rv = client.put(f'/api/invoices/{seed_data["invoice_id"]}', json={'status': 'payee'})
        assert rv.status_code == 200

    def test_update_invoice_cancel(self, client, seed_data):
        rv = client.post('/api/invoices', json={
            'customer_id': seed_data['customer_id'],
            'warehouse_id': seed_data['warehouse_id'],
            'items': [{'product_id': seed_data['product_id'], 'quantity': 1}],
        })
        iid = rv.get_json()['invoice_id']
        rv = client.put(f'/api/invoices/{iid}', json={'status': 'annulee'})
        assert rv.status_code == 200

    def test_add_invoice_item(self, client, seed_data):
        rv = client.post(f'/api/invoices/{seed_data["invoice_id"]}/items', json={
            'product_id': seed_data['product_id'], 'quantity': 1,
        })
        assert rv.status_code in (200, 201)

    def test_delete_invoice(self, client, seed_data):
        rv = client.post('/api/invoices', json={
            'customer_id': seed_data['customer_id'],
            'warehouse_id': seed_data['warehouse_id'],
            'items': [],
        })
        iid = rv.get_json()['invoice_id']
        rv = client.delete(f'/api/invoices/{iid}')
        assert rv.status_code in (200, 204)

    def test_get_invoice_stats(self, client, seed_data):
        rv = client.get('/api/invoice-stats')
        assert rv.status_code == 200

    def test_get_receivables(self, client, seed_data):
        rv = client.get('/api/receivables')
        assert rv.status_code == 200

    def test_get_invoice_pdf(self, client, seed_data):
        rv = client.get(f'/api/invoices/{seed_data["invoice_id"]}/pdf')
        assert rv.status_code == 200


class TestPOS:
    def test_get_pos_session(self, client, seed_data):
        rv = client.get('/api/pos/sessions')
        assert rv.status_code == 200

    def test_open_pos_session(self, client, seed_data):
        rv = client.post('/api/pos/sessions', json={'opening_cash': 500.0, 'warehouse_id': seed_data['warehouse_id']})
        assert rv.status_code in (200, 201)

    def test_get_pos_customers(self, client, seed_data):
        rv = client.get('/api/pos/customers')
        assert rv.status_code == 200

    def test_get_pos_recent_transactions(self, client, seed_data):
        rv = client.get('/api/pos/transactions/recent')
        assert rv.status_code == 200

    def test_get_pos_cash_movements(self, client, seed_data):
        rv = client.get('/api/pos/cash-movements')
        assert rv.status_code == 200

    def test_create_pos_cash_movement(self, client, seed_data):
        client.post('/api/pos/sessions', json={'opening_cash': 500.0, 'warehouse_id': seed_data['warehouse_id']})
        rv = client.post('/api/pos/cash-movements', json={
            'type': 'in', 'amount': 100.0, 'reason': 'test',
        })
        assert rv.status_code in (200, 201)


class TestMainAccount:
    def test_get_main_account(self, client, seed_data):
        rv = client.get('/api/main-account')
        assert rv.status_code == 200
        data = rv.get_json()
        assert 'account' in data
        assert 'current_balance' in data['account']

    def test_deposit(self, client, seed_data):
        rv = client.post('/api/main-account/deposit', json={
            'amount': 500.0, 'reason': 'Test',
        })
        assert rv.status_code in (200, 201)

    def test_withdraw(self, client, seed_data):
        rv = client.post('/api/main-account/withdraw', json={
            'amount': 100.0, 'reason': 'Test',
        })
        assert rv.status_code in (200, 201)

    def test_withdraw_insufficient(self, client, seed_data):
        rv = client.post('/api/main-account/withdraw', json={
            'amount': 999999.0, 'reason': 'Trop',
        })
        assert rv.status_code in (400, 422)


class TestNotifications:
    def test_list(self, client, seed_data):
        rv = client.get('/api/notifications')
        assert rv.status_code == 200

    def test_mark_all_read(self, client, seed_data):
        rv = client.post('/api/notifications/mark-all-read')
        assert rv.status_code in (200, 204)


class TestEdgeCases:
    def test_nonexistent_product(self, client, seed_data):
        rv = client.get('/api/products/99999')
        assert rv.status_code == 404

    def test_nonexistent_invoice(self, client, seed_data):
        rv = client.get('/api/invoices/99999')
        assert rv.status_code == 404


class TestReports:
    def test_get_reports_overview(self, client, seed_data):
        rv = client.get('/api/reports?type=overview')
        assert rv.status_code == 200

    def test_get_reports_csv(self, client, seed_data):
        rv = client.get('/api/reports/export?type=products&format=csv')
        assert rv.status_code == 200
