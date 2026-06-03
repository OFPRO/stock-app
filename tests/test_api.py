def test_index_returns_html(client):
    rv = client.get('/')
    assert rv.status_code == 200
    assert b'StockPro' in rv.data

def test_api_products(client, seed_data):
    rv = client.get('/api/products')
    assert rv.status_code == 200
    data = rv.get_json()
    assert isinstance(data, list)
    assert len(data) > 0
    assert 'name' in data[0]
    assert 'price' in data[0]

def test_api_product_detail(client, seed_data):
    first_id = seed_data['product_id']
    rv = client.get(f'/api/products/{first_id}')
    assert rv.status_code == 200
    data = rv.get_json()
    assert 'product' in data
    assert 'purchase_stats' in data
    assert 'sales_stats' in data

def test_api_warehouses(client):
    rv = client.get('/api/warehouses')
    assert rv.status_code == 200
    data = rv.get_json()
    assert isinstance(data, list)

def test_api_suppliers(client):
    rv = client.get('/api/suppliers')
    assert rv.status_code == 200
    data = rv.get_json()
    assert isinstance(data, list)

def test_api_customers(client):
    rv = client.get('/api/customers')
    assert rv.status_code == 200
    data = rv.get_json()
    assert isinstance(data, list)

def test_api_invoices(client):
    rv = client.get('/api/invoices')
    assert rv.status_code == 200
    data = rv.get_json()
    assert isinstance(data, list)

def test_api_orders(client):
    rv = client.get('/api/orders')
    assert rv.status_code == 200
    data = rv.get_json()
    assert isinstance(data, list)

def test_api_movements(client):
    rv = client.get('/api/movements')
    assert rv.status_code == 200
    data = rv.get_json()
    assert isinstance(data, list)

def test_api_kpis_dashboard(client):
    rv = client.get('/api/kpis/dashboard?period=30')
    assert rv.status_code == 200
    data = rv.get_json()
    assert 'total_products' in data
    assert 'total_value' in data
    assert 'low_stock' in data

def test_api_kpis_sales(client):
    rv = client.get('/api/kpis/sales?period=30')
    assert rv.status_code == 200
    data = rv.get_json()
    assert 'ca_periode' in data

def test_api_kpis_margins(client):
    rv = client.get('/api/kpis/margins')
    assert rv.status_code == 200
    assert rv.get_json() is not None

def test_api_kpis_receivables(client):
    rv = client.get('/api/kpis/receivables')
    assert rv.status_code == 200
    data = rv.get_json()
    assert 'total_creances' in data
    assert 'nb_impayees' in data

def test_api_kpis_trends(client):
    rv = client.get('/api/kpis/trends?period=30')
    assert rv.status_code == 200
    data = rv.get_json()
    assert isinstance(data, list)

def test_api_kpis_alertes(client):
    rv = client.get('/api/kpis/alertes')
    assert rv.status_code == 200
    data = rv.get_json()
    assert 'low_stock' in data
    assert 'expiring' in data

def test_api_categories(client, seed_data):
    rv = client.get('/api/categories')
    assert rv.status_code == 200
    data = rv.get_json()
    assert isinstance(data, list)
    assert len(data) > 0

def test_api_pos_best_sellers(client):
    rv = client.get('/api/pos/best-sellers?limit=5')
    assert rv.status_code == 200
    data = rv.get_json()
    assert isinstance(data, list)

def test_api_main_account(client):
    rv = client.get('/api/main-account')
    assert rv.status_code == 200
    data = rv.get_json()
    assert 'account' in data or 'current_balance' in data

def test_api_notifications(client):
    rv = client.get('/api/notifications')
    assert rv.status_code == 200
    data = rv.get_json()
    assert 'notifications' in data
    assert isinstance(data['notifications'], list)

def test_api_reorder_rules(client):
    rv = client.get('/api/reorder-rules')
    assert rv.status_code == 200
    data = rv.get_json()
    assert isinstance(data, list)
