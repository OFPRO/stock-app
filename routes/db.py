from contextlib import contextmanager
import os
import sys
import platform
import sqlite3
import flask

def _get_default_data_dir():
    if getattr(sys, 'frozen', False) and platform.system() == 'Windows':
        appdata = os.environ.get('APPDATA')
        if appdata:
            return os.path.join(appdata, 'StockPro')
    return os.getcwd()

DB_NAME = os.environ.get('STOCKPRO_DB_PATH', 'stock.db')
STOCKPRO_DATA_DIR = os.environ.get('STOCKPRO_DATA_DIR', _get_default_data_dir())
os.makedirs(STOCKPRO_DATA_DIR, exist_ok=True)
CATALOG_DB = DB_NAME if os.path.isabs(DB_NAME) else os.path.join(STOCKPRO_DATA_DIR, DB_NAME)
CATALOG_DB = os.path.abspath(CATALOG_DB)

categories_data = [
    ('المصاحف', 'Corans'),
    ('الكتب', 'Livres'),
    ('الدفاتر', 'Cahiers'),
    ('المحافظ', 'Cartables'),
    ('مقلمة', 'Trousses'),
    ('لانش بوكس', 'Launch Box'),
    ('آلة حسابية', 'Calculatrices'),
    ('أدوات الهاتف', 'Accessoires téléphone'),
    ('نوت بوك / أجندة', 'Notebooks agendas'),
    ('القصص عامة', 'Histoires'),
    ('ستيلو/ قلم الرصاص', 'Stylos à bille/Crayons'),
    ('ملونات الخشب / الشمع', 'Crayons couleur cire'),
    ('تلوين + feutres', 'Coloriage feutres'),
    ('بلا نكو + Fluorescent', 'Blanco surligneurs'),
    ('ادوات الصباغة و الرسم', 'Dessins/peinture'),
    ('أدوات مكتبية', 'Fournitures bureau'),
    ('أدوات مدرسية', 'Fournitures scolaires'),
    ('هدايا الكبار', 'Cadeaux adultes'),
    ('لعب الأطفال', 'Jouets'),
    ('روايات', 'Romans'),
    ('اللوحات', 'Tableaux'),
    ('ملفات + classeurs + papier', 'Classeurs chemises papier'),
]

def _safe_int(value, default):
    try:
        return int(value)
    except (ValueError, TypeError):
        return default

def validate_id(value):
    if value is None:
        return None
    if isinstance(value, str) and value.isdigit():
        return int(value)
    if isinstance(value, int) and value > 0:
        return value
    return None

def get_price_by_tier(product, tier):
    base_price = product.get('price_base', 0) or product.get('price', 0)
    if tier in ('fidele', 'price_loyal'):
        return product.get('price_loyal', 0) or base_price
    elif tier in ('gros', 'price_gros'):
        return product.get('price_gros', 0) or base_price
    elif tier in ('ecole', 'price_school'):
        return product.get('price_school', 0) or base_price
    elif tier in ('etudiant', 'price_student'):
        return product.get('price_student', 0) or base_price
    return base_price

def get_price_for_customer(db, product_id, customer_id):
    product = db.execute('SELECT * FROM products WHERE id=?', (product_id,)).fetchone()
    if not product:
        return 0
    product = dict(product)
    if customer_id:
        customer = db.execute('SELECT * FROM customers WHERE id=?', (customer_id,)).fetchone()
        if customer:
            tier = customer['pricing_tier'] or customer.get('type', 'normal')
            return get_price_by_tier(product, tier)
    return product.get('price_base', 0) or product.get('price', 0)

def resolve_db_path(store_id=None):
    if store_id is None:
        try:
            store_id = flask.session.get('active_store_id', 1)
        except RuntimeError:
            store_id = 1
    if store_id == 1:
        return CATALOG_DB
    basedir = os.path.dirname(CATALOG_DB)
    return os.path.join(basedir, f'stock_{store_id}.db')

def _connect(db_path):
    conn = sqlite3.connect(db_path, check_same_thread=False, timeout=30)
    conn.row_factory = sqlite3.Row
    conn.execute('PRAGMA journal_mode=WAL')
    conn.execute('PRAGMA busy_timeout=30000')
    conn.execute('PRAGMA foreign_keys=ON')
    return conn

def get_db(store_id=None):
    return _connect(resolve_db_path(store_id))

@contextmanager
def get_db_ctx(store_id=None):
    conn = _connect(resolve_db_path(store_id))
    try:
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()

def get_catalog_db():
    return _connect(CATALOG_DB)

@contextmanager
def get_catalog_db_ctx():
    conn = _connect(CATALOG_DB)
    try:
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()

def init_store_db(store_id, name):
    db_path = resolve_db_path(store_id)
    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    c = conn.cursor()

    c.execute('''
        CREATE TABLE IF NOT EXISTS warehouses (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            address TEXT,
            manager TEXT,
            phone TEXT,
            ice TEXT,
            patente TEXT,
            rc TEXT,
            taxe_number TEXT,
            is_default INTEGER DEFAULT 0,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    c.execute('''
        CREATE TABLE IF NOT EXISTS locations (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            warehouse_id INTEGER NOT NULL,
            name TEXT NOT NULL,
            type TEXT DEFAULT 'rack',
            capacity INTEGER,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
        )
    ''')
    c.execute('''
        CREATE TABLE IF NOT EXISTS products (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            description TEXT,
            sku TEXT UNIQUE,
            barcode TEXT,
            quantity INTEGER DEFAULT 0,
            min_quantity INTEGER DEFAULT 5,
            max_quantity INTEGER DEFAULT 100,
            price REAL DEFAULT 0,
            price_base REAL DEFAULT 0,
            price_loyal REAL DEFAULT 0,
            price_gros REAL DEFAULT 0,
            tax_category TEXT DEFAULT '20',
            lot_number TEXT,
            serial_number TEXT,
            expiry_date DATE,
            supplier_id INTEGER,
            category TEXT DEFAULT 'Général',
            warehouse_id INTEGER DEFAULT 1,
            location_id INTEGER,
            is_deleted INTEGER DEFAULT 0,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    c.execute('''
        CREATE TABLE IF NOT EXISTS categories (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name_ar TEXT NOT NULL,
            name_fr TEXT NOT NULL UNIQUE
        )
    ''')
    c.execute('''
        CREATE TABLE IF NOT EXISTS stock (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            product_id INTEGER NOT NULL,
            location_id INTEGER,
            quantity INTEGER DEFAULT 0,
            FOREIGN KEY (product_id) REFERENCES products(id),
            FOREIGN KEY (location_id) REFERENCES locations(id)
        )
    ''')
    c.execute('''CREATE TRIGGER IF NOT EXISTS products_quantity_nonnegative
        BEFORE UPDATE OF quantity ON products
        WHEN NEW.quantity < 0
        BEGIN
            SELECT RAISE(ABORT, 'La quantité ne peut pas être négative');
        END''')
    c.execute('''CREATE TRIGGER IF NOT EXISTS stock_quantity_nonnegative
        BEFORE UPDATE OF quantity ON stock
        WHEN NEW.quantity < 0
        BEGIN
            SELECT RAISE(ABORT, 'La quantité ne peut pas être négative');
        END''')
    c.execute('''
        CREATE TABLE IF NOT EXISTS stock_movements (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            product_id INTEGER,
            type TEXT NOT NULL,
            quantity INTEGER NOT NULL,
            source_location_id INTEGER,
            dest_location_id INTEGER,
            lot_number TEXT,
            serial_number TEXT,
            note TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (product_id) REFERENCES products(id)
        )
    ''')
    c.execute('''
        CREATE TABLE IF NOT EXISTS suppliers (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            email TEXT,
            phone TEXT,
            address TEXT,
            contact_person TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    c.execute('''
        CREATE TABLE IF NOT EXISTS purchase_orders (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            order_number TEXT UNIQUE,
            supplier_id INTEGER,
            warehouse_id INTEGER DEFAULT 1,
            status TEXT DEFAULT 'brouillon',
            total REAL DEFAULT 0,
            notes TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            sent_at TIMESTAMP,
            received_at TIMESTAMP,
            FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
            FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
        )
    ''')
    c.execute('''
        CREATE TABLE IF NOT EXISTS purchase_order_items (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            order_id INTEGER,
            product_id INTEGER,
            quantity INTEGER,
            unit_price REAL DEFAULT 0,
            received_qty INTEGER DEFAULT 0,
            FOREIGN KEY (order_id) REFERENCES purchase_orders(id),
            FOREIGN KEY (product_id) REFERENCES products(id)
        )
    ''')
    c.execute('''
        CREATE TABLE IF NOT EXISTS reordering_rules (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            product_id INTEGER NOT NULL,
            warehouse_id INTEGER DEFAULT 1,
            min_quantity INTEGER DEFAULT 5,
            max_quantity INTEGER DEFAULT 100,
            trigger_type TEXT DEFAULT 'manual',
            supplier_id INTEGER,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (product_id) REFERENCES products(id),
            FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
            FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
        )
    ''')
    c.execute('''
        CREATE TABLE IF NOT EXISTS notifications (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            type TEXT NOT NULL,
            title TEXT NOT NULL,
            message TEXT,
            product_id INTEGER,
            warehouse_id INTEGER,
            is_read INTEGER DEFAULT 0,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    c.execute('''
        CREATE TABLE IF NOT EXISTS customers (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            type TEXT DEFAULT 'particulier',
            email TEXT,
            phone TEXT,
            address TEXT,
            client_code TEXT UNIQUE,
            discount_rate REAL DEFAULT 0,
            is_loyal INTEGER DEFAULT 0,
            is_active INTEGER DEFAULT 1,
            ice TEXT,
            notes TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    c.execute('''
        CREATE TABLE IF NOT EXISTS invoices (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            invoice_number TEXT UNIQUE NOT NULL,
            customer_id INTEGER,
            warehouse_id INTEGER DEFAULT 1,
            status TEXT DEFAULT 'brouillon',
            subtotal REAL DEFAULT 0,
            discount_total REAL DEFAULT 0,
            tax_amount REAL DEFAULT 0,
            total REAL DEFAULT 0,
            notes TEXT,
            due_date DATE,
            paid_at TIMESTAMP,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (customer_id) REFERENCES customers(id),
            FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
        )
    ''')
    c.execute('''
        CREATE TABLE IF NOT EXISTS invoice_items (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            invoice_id INTEGER NOT NULL,
            product_id INTEGER,
            product_name TEXT,
            product_sku TEXT,
            quantity INTEGER NOT NULL,
            unit_price REAL NOT NULL,
            discount_percent REAL DEFAULT 0,
            tax_rate REAL DEFAULT 20,
            line_total REAL NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (invoice_id) REFERENCES invoices(id),
            FOREIGN KEY (product_id) REFERENCES products(id)
        )
    ''')
    c.execute('''
        CREATE TABLE IF NOT EXISTS pos_sessions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            session_number TEXT UNIQUE NOT NULL,
            warehouse_id INTEGER DEFAULT 1,
            user_name TEXT DEFAULT 'Caissier',
            opening_cash REAL DEFAULT 0,
            closing_cash REAL DEFAULT 0,
            expected_cash REAL DEFAULT 0,
            status TEXT DEFAULT 'open',
            opened_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            closed_at TIMESTAMP,
            FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
        )
    ''')
    c.execute('''
        CREATE TABLE IF NOT EXISTS pos_transactions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            ticket_number TEXT UNIQUE NOT NULL,
            session_id INTEGER NOT NULL,
            customer_id INTEGER,
            payment_method TEXT NOT NULL,
            subtotal REAL NOT NULL,
            tax_amount REAL NOT NULL,
            discount_amount REAL DEFAULT 0,
            total REAL NOT NULL,
            tendered_amount REAL DEFAULT 0,
            change_amount REAL DEFAULT 0,
            status TEXT DEFAULT 'completed',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (session_id) REFERENCES pos_sessions(id),
            FOREIGN KEY (customer_id) REFERENCES customers(id)
        )
    ''')
    c.execute('''
        CREATE TABLE IF NOT EXISTS pos_transaction_items (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            transaction_id INTEGER NOT NULL,
            product_id INTEGER NOT NULL,
            product_name TEXT,
            product_sku TEXT,
            quantity INTEGER NOT NULL,
            unit_price REAL NOT NULL,
            discount_percent REAL DEFAULT 0,
            tax_rate REAL DEFAULT 20,
            line_total REAL NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (transaction_id) REFERENCES pos_transactions(id),
            FOREIGN KEY (product_id) REFERENCES products(id)
        )
    ''')
    c.execute('''
        CREATE TABLE IF NOT EXISTS pos_cash_movements (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            session_id INTEGER NOT NULL,
            type TEXT NOT NULL,
            amount REAL NOT NULL,
            reason TEXT,
            note TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (session_id) REFERENCES pos_sessions(id)
        )
    ''')
    c.execute('''
        CREATE TABLE IF NOT EXISTS pos_registers (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL UNIQUE,
            code TEXT NOT NULL UNIQUE,
            warehouse_id INTEGER DEFAULT 1,
            is_active INTEGER DEFAULT 1,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
        )
    ''')
    c.execute('''
        CREATE TABLE IF NOT EXISTS main_account (
            id INTEGER PRIMARY KEY CHECK (id = 1),
            name TEXT DEFAULT 'Compte Principal',
            initial_balance REAL DEFAULT 0,
            current_balance REAL DEFAULT 0,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    c.execute('''
        CREATE TABLE IF NOT EXISTS main_account_transactions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            type TEXT NOT NULL,
            amount REAL NOT NULL,
            reason TEXT,
            reference_id INTEGER,
            note TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    c.execute('''
        CREATE TABLE IF NOT EXISTS sequences (
            name TEXT PRIMARY KEY,
            current_value INTEGER DEFAULT 0
        )
    ''')

    for alter_sql in [
        'ALTER TABLE products ADD COLUMN deleted_at TIMESTAMP',
        'ALTER TABLE products ADD COLUMN is_liquidation INTEGER DEFAULT 0',
        'ALTER TABLE products ADD COLUMN purchase_price_avg REAL DEFAULT 0',
        "ALTER TABLE products ADD COLUMN discount_category TEXT DEFAULT 'aucun'",
        'ALTER TABLE products ADD COLUMN margin_percent REAL DEFAULT 15.0',
        "ALTER TABLE products ADD COLUMN extra_prices TEXT DEFAULT '[]'",
        'ALTER TABLE warehouses ADD COLUMN phone TEXT',
        'ALTER TABLE warehouses ADD COLUMN ice TEXT',
        'ALTER TABLE warehouses ADD COLUMN patente TEXT',
        'ALTER TABLE warehouses ADD COLUMN rc TEXT',
        'ALTER TABLE warehouses ADD COLUMN taxe_number TEXT',
        'ALTER TABLE customers ADD COLUMN ice TEXT',
        "ALTER TABLE purchase_orders ADD COLUMN paid_at TIMESTAMP",
        'ALTER TABLE invoices ADD COLUMN supplier_id INTEGER',
        "ALTER TABLE invoices ADD COLUMN type TEXT DEFAULT 'facture'",
        'ALTER TABLE invoices ADD COLUMN payment_method TEXT',
        'ALTER TABLE invoices ADD COLUMN tendered_amount REAL DEFAULT 0',
        'ALTER TABLE invoices ADD COLUMN change_given REAL DEFAULT 0',
        'ALTER TABLE invoices ADD COLUMN amount_paid REAL DEFAULT 0',
        'ALTER TABLE invoices ADD COLUMN is_credit_payment INTEGER DEFAULT 0',
        'ALTER TABLE pos_transactions ADD COLUMN discount_total REAL DEFAULT 0',
        'ALTER TABLE pos_transactions ADD COLUMN change_given REAL DEFAULT 0',
        'ALTER TABLE pos_transactions ADD COLUMN transaction_number TEXT',
        'ALTER TABLE pos_transactions ADD COLUMN invoice_id INTEGER REFERENCES invoices(id)',
        "ALTER TABLE pos_cash_movements ADD COLUMN source TEXT DEFAULT 'pos'",
        'ALTER TABLE pos_sessions ADD COLUMN register_id INTEGER REFERENCES pos_registers(id)',
        "ALTER TABLE pos_sessions ADD COLUMN cashier_name TEXT DEFAULT ''",
    ]:
        try:
            c.execute(alter_sql)
        except Exception:
            pass

    c.execute('''
        CREATE TABLE IF NOT EXISTS _schema_version (
            version INTEGER PRIMARY KEY,
            applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    c.execute('INSERT OR IGNORE INTO _schema_version (version) VALUES (1)')

    for ar, fr in categories_data:
        c.execute("INSERT OR IGNORE INTO categories (name_ar, name_fr) VALUES (?, ?)", (ar, fr))

    c.execute("INSERT OR IGNORE INTO main_account (id, name, initial_balance, current_balance) VALUES (1, 'Compte Principal', 0, 0)")

    existing = c.execute("SELECT id FROM warehouses WHERE is_default=1").fetchone()
    if not existing:
        c.execute("INSERT INTO warehouses (name, is_default) VALUES (?, 1)", (f'{name} - Principal',))
        wh_id = c.lastrowid
        c.execute("INSERT INTO locations (warehouse_id, name, type) VALUES (?, 'Zone principale', 'zone')", (wh_id,))

    c.execute("INSERT OR IGNORE INTO pos_registers (name, code) VALUES ('Caisse 1', 'CAISSE-01')")
    c.execute("INSERT OR IGNORE INTO pos_registers (name, code) VALUES ('Caisse 2', 'CAISSE-02')")
    c.execute("UPDATE pos_registers SET is_active = 1 WHERE name IN ('Caisse 1', 'Caisse 2')")

    conn.commit()
    conn.close()

def _safe_err(e, fallback='Erreur interne du serveur'):
    import logging
    logging.getLogger('stockpro').exception('API error')
    if isinstance(e, ValueError):
        return str(e)
    return fallback
