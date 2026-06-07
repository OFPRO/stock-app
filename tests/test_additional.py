"""Tests for previously uncovered routes -- warehouses, POS workflows, payments, transfers."""


class TestWarehousesCRUD:
    def _create_warehouse(self, client, name):
        rv = client.post('/api/warehouses', json={'name': name})
        assert rv.status_code == 200
        whs = client.get('/api/warehouses').get_json()
        return [w for w in whs if w['name'] == name][0]['id']

    def test_get_warehouse_detail(self, client, seed_data):
        wid = seed_data['warehouse_id']
        rv = client.get(f'/api/warehouses/{wid}')
        assert rv.status_code == 200
        assert rv.get_json()['name'] == 'Test Warehouse'

    def test_get_warehouse_not_found(self, client, seed_data):
        rv = client.get('/api/warehouses/99999')
        assert rv.status_code == 404

    def test_update_warehouse(self, client, seed_data):
        wid = seed_data['warehouse_id']
        rv = client.put(f'/api/warehouses/{wid}', json={
            'name': 'Updated Warehouse',
            'address': '456 New St',
            'manager': 'New Manager',
        })
        assert rv.status_code == 200
        assert rv.get_json()['success'] is True

    def test_update_warehouse_not_found(self, client, seed_data):
        rv = client.put('/api/warehouses/99999', json={'name': 'Ghost'})
        assert rv.status_code == 404

    def test_delete_warehouse(self, client, seed_data):
        new_id = self._create_warehouse(client, 'Temp Warehouse')
        rv = client.delete(f'/api/warehouses/{new_id}')
        assert rv.status_code == 200

    def test_delete_warehouse_not_found(self, client, seed_data):
        rv = client.delete('/api/warehouses/99999')
        assert rv.status_code == 404


class TestLocationsExtra:
    def test_list_locations(self, client, seed_data):
        rv = client.get('/api/locations')
        assert rv.status_code == 200
        data = rv.get_json()
        assert isinstance(data, list)
        assert len(data) >= 1

    def test_delete_location(self, client, seed_data):
        rv = client.post('/api/locations', json={
            'name': 'Temp Loc',
            'warehouse_id': seed_data['warehouse_id'],
        })
        locs = client.get('/api/locations').get_json()
        loc = [l for l in locs if l['name'] == 'Temp Loc'][0]
        rv = client.delete(f'/api/locations/{loc["id"]}')
        assert rv.status_code == 200

    def test_delete_location_not_found(self, client, seed_data):
        rv = client.delete('/api/locations/99999')
        assert rv.status_code == 200


class TestReorderRulesExtra:
    def _create_rule(self, client, seed_data):
        rv = client.post('/api/reorder-rules', json={
            'product_id': seed_data['product_id'],
            'warehouse_id': seed_data['warehouse_id'],
            'min_quantity': 10,
        })
        assert rv.status_code == 200
        rules = client.get('/api/reorder-rules').get_json()
        return [r for r in rules if r['product_id'] == seed_data['product_id']][0]['id']

    def test_update_reorder_rule(self, client, seed_data):
        rule_id = self._create_rule(client, seed_data)
        rv = client.put(f'/api/reorder-rules/{rule_id}', json={
            'min_quantity': 20,
        })
        assert rv.status_code == 200
        assert rv.get_json()['success'] is True

    def test_update_reorder_rule_not_found(self, client, seed_data):
        rv = client.put('/api/reorder-rules/99999', json={'min_quantity': 10})
        assert rv.status_code == 200

    def test_delete_reorder_rule(self, client, seed_data):
        rule_id = self._create_rule(client, seed_data)
        rv = client.delete(f'/api/reorder-rules/{rule_id}')
        assert rv.status_code == 200

    def test_delete_reorder_rule_not_found(self, client, seed_data):
        rv = client.delete('/api/reorder-rules/99999')
        assert rv.status_code == 200


class TestNotificationsExtra:
    def test_mark_single_read(self, client, seed_data):
        rv = client.get('/api/notifications')
        notifs = rv.get_json()['notifications']
        if notifs:
            nid = notifs[0]['id']
            rv = client.post(f'/api/notifications/{nid}/read')
            assert rv.status_code == 200

    def test_mark_single_read_not_found(self, client, seed_data):
        rv = client.post('/api/notifications/99999/read')
        assert rv.status_code == 200


class TestInvoicesExtra:
    def _set_invoice_status(self, inv_id, status):
        import routes.db
        conn = routes.db.get_db()
        conn.execute('UPDATE invoices SET status=? WHERE id=?', (status, inv_id))
        conn.commit()
        conn.close()

    def test_pay_credit_full(self, client, seed_data):
        rv = client.post('/api/invoices', json={
            'customer_id': seed_data['customer_id'],
            'warehouse_id': seed_data['warehouse_id'],
            'items': [{'product_id': seed_data['product_id'], 'quantity': 1}],
        })
        assert rv.status_code == 200
        inv_id = rv.get_json()['invoice_id']
        self._set_invoice_status(inv_id, 'envoyee')
        inv_data = client.get(f'/api/invoices/{inv_id}').get_json()
        rv = client.post(f'/api/invoices/{inv_id}/pay-credit', json={
            'amount': inv_data['total'],
            'payment_method': 'cash',
        })
        assert rv.status_code == 200
        data = rv.get_json()
        assert data['status'] == 'payee'

    def test_pay_credit_invalid_amount(self, client, seed_data):
        rv = client.post(f'/api/invoices/{seed_data["invoice_id"]}/pay-credit', json={
            'amount': -5,
        })
        assert rv.status_code == 400

    def test_pay_credit_not_found(self, client, seed_data):
        rv = client.post('/api/invoices/99999/pay-credit', json={'amount': 10})
        assert rv.status_code == 404

    def test_delete_invoice_item(self, client, seed_data):
        rv = client.get(f'/api/invoices/{seed_data["invoice_id"]}/items')
        items = rv.get_json()
        if items:
            item_id = items[0]['id']
            rv = client.delete(f'/api/invoices/{seed_data["invoice_id"]}/items/{item_id}')
            assert rv.status_code == 200

    def test_delete_invoice_item_not_found(self, client, seed_data):
        rv = client.delete(f'/api/invoices/{seed_data["invoice_id"]}/items/99999')
        assert rv.status_code == 200


class TestPOSWorkflow:
    def _ensure_wh1(self, client):
        import routes.db
        conn = routes.db.get_db()
        existing = conn.execute('SELECT id FROM warehouses WHERE id=1').fetchone()
        if not existing:
            conn.execute('INSERT INTO warehouses (id, name) VALUES (1, "WH-1")')
            conn.commit()
        conn.close()

    def _open_session(self, client, seed_data):
        self._ensure_wh1(client)
        rv = client.post('/api/pos/sessions', json={
            'opening_cash': 500.0,
            'warehouse_id': seed_data['warehouse_id'],
        })
        data = rv.get_json()
        if rv.status_code == 400 and 'déjà ouverte' in data.get('error', ''):
            rv = client.get('/api/pos/sessions')
            sessions = rv.get_json()
            open_sessions = [s for s in sessions if s['status'] == 'open']
            if open_sessions:
                return open_sessions[0]['id']
        return data['session']['id']

    def test_close_pos_session(self, client, seed_data):
        sid = self._open_session(client, seed_data)
        rv = client.post(f'/api/pos/sessions/{sid}/close', json={
            'closing_cash': 500.0,
            'deposit_to_main': True,
        })
        assert rv.status_code == 200
        assert rv.get_json()['success'] is True

    def test_close_pos_session_not_found(self, client, seed_data):
        rv = client.post('/api/pos/sessions/99999/close', json={'closing_cash': 0})
        assert rv.status_code == 404

    def test_close_pos_session_already_closed(self, client, seed_data):
        sid = self._open_session(client, seed_data)
        client.post(f'/api/pos/sessions/{sid}/close', json={'closing_cash': 500.0})
        rv = client.post(f'/api/pos/sessions/{sid}/close', json={'closing_cash': 500.0})
        assert rv.status_code == 400

    def test_create_pos_transaction(self, client, seed_data):
        sid = self._open_session(client, seed_data)
        rv = client.post('/api/pos/transactions', json={
            'session_id': sid,
            'items': [{'product_id': seed_data['product_id'], 'quantity': 1}],
            'payment_method': 'cash',
            'tendered_amount': 50.0,
        })
        assert rv.status_code == 200
        assert rv.get_json()['success'] is True

    def test_create_pos_transaction_no_session(self, client, seed_data):
        rv = client.post('/api/pos/transactions', json={
            'session_id': 99999,
            'items': [{'product_id': seed_data['product_id'], 'quantity': 1}],
        })
        assert rv.status_code == 400

    def test_create_pos_transaction_missing_fields(self, client, seed_data):
        rv = client.post('/api/pos/transactions', json={})
        assert rv.status_code == 400

    def test_create_pos_transaction_insufficient_stock(self, client, seed_data):
        sid = self._open_session(client, seed_data)
        rv = client.post('/api/pos/transactions', json={
            'session_id': sid,
            'items': [{'product_id': seed_data['product_id'], 'quantity': 99999}],
        })
        assert rv.status_code == 400

    def test_create_pos_transaction_card(self, client, seed_data):
        sid = self._open_session(client, seed_data)
        rv = client.post('/api/pos/transactions', json={
            'session_id': sid,
            'items': [{'product_id': seed_data['product_id'], 'quantity': 1}],
            'payment_method': 'card',
            'tendered_amount': 0,
        })
        assert rv.status_code == 200
        assert rv.get_json()['success'] is True

    def test_transfer_to_pos(self, client, seed_data):
        self._open_session(client, seed_data)
        rv = client.post('/api/main-account/transfer-to-pos', json={
            'amount': 200.0,
            'note': 'Test transfer',
        })
        assert rv.status_code == 200

    def test_transfer_to_pos_invalid_amount(self, client, seed_data):
        rv = client.post('/api/main-account/transfer-to-pos', json={
            'amount': -50,
        })
        assert rv.status_code == 400


class TestStockInterWarehouse:
    def _create_wh_with_loc(self, client, name):
        rv = client.post('/api/warehouses', json={'name': name})
        whs = client.get('/api/warehouses').get_json()
        wh = [w for w in whs if w['name'] == name][0]
        client.post('/api/locations', json={'name': name + ' Loc', 'warehouse_id': wh['id']})
        return wh['id']

    def test_inter_warehouse_transfer(self, client, seed_data):
        dest_wh_id = self._create_wh_with_loc(client, 'Destination WH')
        rv = client.post('/api/stock/inter-warehouse', json={
            'product_id': seed_data['product_id'],
            'quantity': 3,
            'from_warehouse_id': seed_data['warehouse_id'],
            'to_warehouse_id': dest_wh_id,
        })
        assert rv.status_code == 200
        assert rv.get_json()['success'] is True

    def test_inter_warehouse_insufficient_stock(self, client, seed_data):
        dest_wh_id = self._create_wh_with_loc(client, 'Dest WH2')
        rv = client.post('/api/stock/inter-warehouse', json={
            'product_id': seed_data['product_id'],
            'quantity': 99999,
            'from_warehouse_id': seed_data['warehouse_id'],
            'to_warehouse_id': dest_wh_id,
        })
        assert rv.status_code == 400

    def test_inter_warehouse_no_location(self, client, seed_data):
        rv = client.post('/api/warehouses', json={'name': 'NoLoc WH'})
        whs = client.get('/api/warehouses').get_json()
        wh = [w for w in whs if w['name'] == 'NoLoc WH'][0]
        rv = client.post('/api/stock/inter-warehouse', json={
            'product_id': seed_data['product_id'],
            'quantity': 1,
            'from_warehouse_id': wh['id'],
            'to_warehouse_id': seed_data['warehouse_id'],
        })
        assert rv.status_code == 400


class TestReports:
    def test_reports_overview(self, client, seed_data):
        rv = client.get('/api/reports?type=overview')
        assert rv.status_code == 200
        data = rv.get_json()
        assert 'total_products' in data or 'total_value' in data

    def test_reports_rotation(self, client, seed_data):
        rv = client.get('/api/reports?type=rotation')
        assert rv.status_code == 200

    def test_reports_low_stock(self, client, seed_data):
        rv = client.get('/api/reports?type=low_stock')
        assert rv.status_code == 200

    def test_reports_expiry(self, client, seed_data):
        rv = client.get('/api/reports?type=expiry')
        assert rv.status_code == 200

    def test_reports_categories(self, client, seed_data):
        rv = client.get('/api/reports?type=categories')
        assert rv.status_code == 200

    def test_reports_warehouse(self, client, seed_data):
        rv = client.get('/api/reports?type=warehouses')
        assert rv.status_code == 200

    def test_reports_export_csv_products(self, client, seed_data):
        rv = client.get('/api/reports/export?type=products&format=csv')
        assert rv.status_code == 200

    def test_reports_export_csv_movements(self, client, seed_data):
        rv = client.get('/api/reports/export?type=movements&format=csv')
        assert rv.status_code == 200

    def test_reports_export_invalid_type(self, client, seed_data):
        rv = client.get('/api/reports/export?type=invalid&format=csv')
        assert rv.status_code == 400


class TestCustomerDetail:
    def test_get_customer_detail(self, client, seed_data):
        cid = seed_data['customer_id']
        rv = client.get(f'/api/customers/{cid}')
        assert rv.status_code == 200
        assert rv.get_json()['name'] == 'Test Client'

    def test_get_customer_not_found(self, client, seed_data):
        rv = client.get('/api/customers/99999')
        assert rv.status_code == 404


class TestKPIExtra:
    def test_kpis_expenses(self, client, seed_data):
        rv = client.get('/api/kpis/expenses')
        assert rv.status_code == 200
        data = rv.get_json()
        assert 'total_expenses' in data

    def test_kpis_sessions_history(self, client, seed_data):
        rv = client.get('/api/kpis/sessions-history')
        assert rv.status_code == 200

    def test_kpis_sessions_summary(self, client, seed_data):
        rv = client.get('/api/kpis/sessions-summary')
        assert rv.status_code == 200

    def test_kpis_payment_methods(self, client, seed_data):
        rv = client.get('/api/kpis/payment-methods')
        assert rv.status_code == 200

    def test_kpis_session_details_not_found(self, client, seed_data):
        rv = client.get('/api/kpis/sessions/99999/details')
        assert rv.status_code == 404
