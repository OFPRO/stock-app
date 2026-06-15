import sys
import os
import shutil
import tempfile
from pathlib import Path

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import pytest


@pytest.fixture(scope="session")
def temp_db_dir():
    tmpdir = tempfile.mkdtemp()
    yield tmpdir
    shutil.rmtree(tmpdir)


@pytest.fixture(scope="session")
def db_path(temp_db_dir):
    return os.path.join(temp_db_dir, "test_stock.db")


@pytest.fixture(scope="session")
def _patch_db(db_path):
    import routes.db
    routes.db.DB_NAME = db_path
    from app import app as flask_app, init_db
    init_db()
    flask_app.config["TESTING"] = True
    flask_app.config["SERVER_NAME"] = "localhost"
    return flask_app


@pytest.fixture(scope="session")
def app(_patch_db):
    return _patch_db


@pytest.fixture(scope="function")
def client(app):
    with app.test_client() as c:
        yield c


@pytest.fixture(scope="function")
def runner(app):
    return app.test_cli_runner()


@pytest.fixture(scope="function")
def _db_cleanup(app, db_path):
    import routes.db
    conn = routes.db.get_db()
    conn.execute("PRAGMA foreign_keys=OFF")
    for table in ["pos_transaction_items", "pos_transactions", "pos_cash_movements",
                   "pos_sessions", "main_account_transactions", "invoice_items",
                   "invoices", "stock_movements", "stock", "notifications",
                   "reordering_rules", "purchase_order_items", "purchase_orders",
                   "products", "customers", "locations", "suppliers", "warehouses",
                   "main_account", "categories"]:
        conn.execute(f"DELETE FROM {table}")
    conn.execute("PRAGMA foreign_keys=ON")
    conn.commit()
    conn.close()
    yield


@pytest.fixture(scope="function")
def seed_data(_db_cleanup, db_path):
    """Seed minimal test data. Returns dict of IDs."""
    import routes.db
    conn = routes.db.get_db()
    c = conn.cursor()

    for ar, fr in [("قرطاسية", "Papeterie"), ("أدوات مكتبية", "Fournitures")]:
        c.execute("INSERT OR IGNORE INTO categories (name_ar, name_fr) VALUES (?, ?)", (ar, fr))

    c.execute("INSERT INTO warehouses (name, address, manager, is_default) VALUES (?, ?, ?, ?)",
              ("Test Warehouse", "123 Test St", "Tester", 1))
    wh_id = c.lastrowid

    c.execute("INSERT INTO locations (warehouse_id, name, type, capacity) VALUES (?, ?, ?, ?)",
              (wh_id, "Zone A", "rack", 100))
    loc_id = c.lastrowid

    c.execute("INSERT INTO suppliers (name, email, phone, address, contact_person) VALUES (?, ?, ?, ?, ?)",
              ("Test Supplier", "supplier@test.ma", "0522-123456", "1 Test Ave", "Contact"))
    sup_id = c.lastrowid

    products = []
    product_defs = [
        ("Stylo test", "TST001", 10.0, 6.0, "Papeterie", 50),
        ("Cahier test", "TST002", 25.0, 18.0, "Papeterie", 30),
        ("Calculatrice test", "TST003", 45.0, 32.0, "Fournitures", 15),
    ]
    for name, sku, price, base, cat, max_qty in product_defs:
        c.execute("""INSERT INTO products (name, sku, quantity, min_quantity, max_quantity,
                    price, price_base, category, supplier_id, warehouse_id, location_id)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                  (name, sku, 20, 5, max_qty, price, base, cat, sup_id, wh_id, loc_id))
        pid = c.lastrowid
        products.append(pid)
        c.execute("INSERT INTO stock (product_id, location_id, quantity) VALUES (?, ?, ?)",
                  (pid, loc_id, 10))

    c.execute("""INSERT INTO customers (name, type, email, phone, address, client_code, is_loyal)
                 VALUES (?, ?, ?, ?, ?, ?, ?)""",
              ("Test Client", "particulier", "client@test.ma", "0612345678",
               "1 Rue Test", "CLI-TEST-0001", 0))
    cust_id = c.lastrowid

    c.execute("""INSERT INTO invoices (invoice_number, customer_id, warehouse_id, status,
                subtotal, discount_total, tax_amount, total, due_date, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
              ("FAC-TEST-00001", cust_id, wh_id, "payee",
               100.0, 0.0, 20.0, 120.0, "2026-06-15", "2026-05-15"))
    inv_id = c.lastrowid

    c.execute("""INSERT INTO invoice_items (invoice_id, product_id, product_name, product_sku,
                quantity, unit_price, discount_percent, tax_rate, line_total)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
              (inv_id, products[0], "Stylo test", "TST001", 2, 10.0, 0, 20, 24.0))

    c.execute("""INSERT INTO main_account (id, name, initial_balance, current_balance)
                 VALUES (1, 'Compte Principal', 10000.00, 10000.00)""")

    conn.commit()
    conn.close()

    return {
        "warehouse_id": wh_id,
        "location_id": loc_id,
        "supplier_id": sup_id,
        "product_ids": products,
        "product_id": products[0],
        "customer_id": cust_id,
        "invoice_id": inv_id,
    }
