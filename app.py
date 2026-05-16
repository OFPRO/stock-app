import sqlite3
import csv
import random
import html
from io import StringIO
from datetime import datetime, timedelta
from flask import Flask, render_template, request, jsonify, Response, send_from_directory
from routes.db import get_db, DB_NAME
from routes.products import products_bp
from routes.kpis import kpis_bp
from routes.customers import customers_bp
from routes.suppliers import suppliers_bp
from routes.warehouses import warehouses_bp
from routes.locations import locations_bp

app = Flask(__name__)
app.register_blueprint(products_bp)
app.register_blueprint(kpis_bp)
app.register_blueprint(customers_bp)
app.register_blueprint(suppliers_bp)
app.register_blueprint(warehouses_bp)
app.register_blueprint(locations_bp)

def _safe_int(value, default):
    try:
        return int(value)
    except (ValueError, TypeError):
        return default

def get_price_for_customer(product, customer_type, is_loyal):
    if isinstance(product, dict):
        base_price = product.get('price_base', 0)
        price = product.get('price', 0)
    else:
        base_price = product['price_base'] if 'price_base' in product.keys() else 0
        price = product['price'] if 'price' in product.keys() else 0
    normal_price = base_price if base_price > 0 else price
    if customer_type == 'ecole':
        return round(normal_price * 0.80, 2)
    if is_loyal:
        return round(normal_price * 0.85, 2)
    if customer_type == 'etudiant':
        return round(normal_price * 0.85, 2)
    return round(normal_price, 2)

def next_sequence(conn, name):
    conn.execute('INSERT OR IGNORE INTO sequences (name, current_value) VALUES (?, 0)', (name,))
    conn.execute('UPDATE sequences SET current_value = current_value + 1 WHERE name = ?', (name,))
    return conn.execute('SELECT current_value FROM sequences WHERE name = ?', (name,)).fetchone()[0]

def init_db():
    conn = sqlite3.connect(DB_NAME)
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
            price_school REAL DEFAULT 0,
            price_student REAL DEFAULT 0,
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
    
    try:
        c.execute('ALTER TABLE products ADD COLUMN price_base REAL DEFAULT 0')
    except Exception:
        pass
    try:
        c.execute('ALTER TABLE products ADD COLUMN price_loyal REAL DEFAULT 0')
    except Exception:
        pass
    try:
        c.execute('ALTER TABLE products ADD COLUMN price_school REAL DEFAULT 0')
    except Exception:
        pass
    try:
        c.execute('ALTER TABLE products ADD COLUMN price_student REAL DEFAULT 0')
    except Exception:
        pass
    try:
        c.execute("ALTER TABLE products ADD COLUMN tax_category TEXT DEFAULT '20'")
    except Exception:
        pass
    try:
        c.execute('ALTER TABLE products ADD COLUMN is_deleted INTEGER DEFAULT 0')
    except Exception:
        pass
    try:
        c.execute('ALTER TABLE products ADD COLUMN deleted_at TIMESTAMP')
    except Exception:
        pass
    try:
        c.execute('ALTER TABLE products ADD COLUMN is_liquidation INTEGER DEFAULT 0')
    except Exception:
        pass
    try:
        c.execute('ALTER TABLE products ADD COLUMN purchase_price_avg REAL DEFAULT 0')
    except Exception:
        pass
    try:
        c.execute("ALTER TABLE products ADD COLUMN discount_category TEXT DEFAULT 'aucun'")
    except Exception:
        pass
    try:
        c.execute('ALTER TABLE products ADD COLUMN margin_percent REAL DEFAULT 15.0')
    except Exception:
        pass
    for col_def in [
        'ALTER TABLE warehouses ADD COLUMN phone TEXT',
        'ALTER TABLE warehouses ADD COLUMN ice TEXT',
        'ALTER TABLE warehouses ADD COLUMN patente TEXT',
        'ALTER TABLE warehouses ADD COLUMN rc TEXT',
        'ALTER TABLE warehouses ADD COLUMN taxe_number TEXT',
        'ALTER TABLE customers ADD COLUMN ice TEXT',
    ]:
        try:
            c.execute(col_def)
        except Exception:
            pass
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
    
    # Fix negative stock: set to 0 and enforce non-negative going forward
    try:
        c.execute('UPDATE products SET quantity = 0 WHERE quantity < 0')
    except Exception:
        pass
    try:
        c.execute('UPDATE stock SET quantity = 0 WHERE quantity < 0')
    except Exception:
        pass
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
    
    try:
        c.execute('ALTER TABLE invoices ADD COLUMN supplier_id INTEGER')
    except Exception:
        pass
    try:
        c.execute('ALTER TABLE invoices ADD COLUMN type TEXT DEFAULT \'facture\'')
    except Exception:
        pass
    try:
        c.execute('ALTER TABLE invoices ADD COLUMN payment_method TEXT')
    except Exception:
        pass
    try:
        c.execute('ALTER TABLE invoices ADD COLUMN tendered_amount REAL DEFAULT 0')
    except Exception:
        pass
    try:
        c.execute('ALTER TABLE invoices ADD COLUMN change_given REAL DEFAULT 0')
    except Exception:
        pass

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
    
    try:
        c.execute("ALTER TABLE pos_transactions ADD COLUMN discount_total REAL DEFAULT 0")
    except Exception:
        pass
    try:
        c.execute("ALTER TABLE pos_transactions ADD COLUMN change_given REAL DEFAULT 0")
    except Exception:
        pass
    try:
        c.execute("ALTER TABLE pos_transactions ADD COLUMN transaction_number TEXT")
    except Exception:
        pass
    try:
        c.execute("ALTER TABLE pos_transactions ADD COLUMN invoice_id INTEGER REFERENCES invoices(id)")
    except Exception:
        pass
    
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
    try:
        c.execute("ALTER TABLE pos_cash_movements ADD COLUMN source TEXT DEFAULT 'pos'")
    except Exception:
        pass
    
    c.execute('''
        CREATE TABLE IF NOT EXISTS main_account (
            id INTEGER PRIMARY KEY CHECK (id = 1),
            name TEXT DEFAULT 'Compte Principal',
            initial_balance REAL DEFAULT 10000.00,
            current_balance REAL DEFAULT 10000.00,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    
    c.execute('''
        CREATE TABLE IF NOT EXISTS sequences (
            name TEXT PRIMARY KEY,
            current_value INTEGER DEFAULT 0
        )
    ''')
    max_fac = c.execute("SELECT MAX(CAST(SUBSTR(invoice_number, -4) AS INTEGER)) FROM invoices WHERE invoice_number LIKE 'FAC-%'").fetchone()[0]
    if max_fac:
        c.execute('INSERT OR IGNORE INTO sequences (name, current_value) VALUES (?, ?)', ('fac_counter', max_fac))
    max_ticket = c.execute("SELECT MAX(CAST(SUBSTR(transaction_number, -4) AS INTEGER)) FROM pos_transactions WHERE transaction_number LIKE 'Ticket-%'").fetchone()[0]
    if max_ticket:
        c.execute('INSERT OR IGNORE INTO sequences (name, current_value) VALUES (?, ?)', ('ticket_counter', max_ticket))
    
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
    
    existing_account = c.execute('SELECT id FROM main_account WHERE id = 1').fetchone()
    if not existing_account:
        c.execute('''
            INSERT INTO main_account (id, name, initial_balance, current_balance)
            VALUES (1, 'Compte Principal', 10000.00, 10000.00)
        ''')
        c.execute('''
            INSERT INTO main_account_transactions (type, amount, reason, note)
            VALUES ('in', 10000.00, 'initial', 'Solde initial du compte')
        ''')
    
    default_warehouse = c.execute('SELECT id FROM warehouses LIMIT 1').fetchone()
    if not default_warehouse:
        c.execute('''
            INSERT INTO warehouses (name, address, is_default) VALUES ('Entrepôt Principal', 'Adresse par défaut', 1)
        ''')
        warehouse_id = c.execute('SELECT last_insert_rowid()').fetchone()[0]
        c.execute('''
            INSERT INTO locations (warehouse_id, name, type) VALUES (?, 'Zone principale', 'zone')
        ''', (warehouse_id,))
    
    conn.commit()
    conn.close()

@app.route('/product/<int:product_id>')
def product_detail_page(product_id):
    conn = get_db()
    product = conn.execute('''
        SELECT p.*, s.name as supplier_name, w.name as warehouse_name, l.name as location_name
        FROM products p
        LEFT JOIN suppliers s ON p.supplier_id = s.id
        LEFT JOIN warehouses w ON p.warehouse_id = w.id
        LEFT JOIN locations l ON p.location_id = l.id
        WHERE p.id = ?
    ''', (product_id,)).fetchone()
    conn.close()
    if not product:
        return '<h1>Produit non trouvé</h1>', 404
    return render_template('product_detail.html', product=dict(product))

@app.route('/')
def index():
    return render_template('index.html')















@app.route('/api/reports', methods=['GET'])
def get_reports():
    conn = get_db()
    report_type = request.args.get('type', 'overview')
    warehouse_id = request.args.get('warehouse_id')
    
    if report_type == 'overview':
        if warehouse_id and warehouse_id.isdigit():
            wid = int(warehouse_id)
            total_products = conn.execute('SELECT COUNT(*) FROM products WHERE warehouse_id=?', (wid,)).fetchone()[0]
            total_value = conn.execute('SELECT COALESCE(SUM(quantity * price), 0) FROM products WHERE warehouse_id=?', (wid,)).fetchone()[0]
            low_stock = conn.execute('SELECT COUNT(*) FROM products WHERE quantity <= min_quantity AND warehouse_id=?', (wid,)).fetchone()[0]
            out_of_stock = conn.execute('SELECT COUNT(*) FROM products WHERE (quantity < 0 OR quantity IS NULL) AND warehouse_id=?', (wid,)).fetchone()[0]
            expiring_soon = conn.execute('SELECT COUNT(*) FROM products WHERE expiry_date IS NOT NULL AND expiry_date <= date(\'now\', \'+30 days\') AND warehouse_id=?', (wid,)).fetchone()[0]
        else:
            total_products = conn.execute('SELECT COUNT(*) FROM products').fetchone()[0]
            total_value = conn.execute('SELECT COALESCE(SUM(quantity * price), 0) FROM products').fetchone()[0]
            low_stock = conn.execute('SELECT COUNT(*) FROM products WHERE quantity <= min_quantity').fetchone()[0]
            out_of_stock = conn.execute('SELECT COUNT(*) FROM products WHERE (quantity < 0 OR quantity IS NULL)').fetchone()[0]
            expiring_soon = conn.execute('SELECT COUNT(*) FROM products WHERE expiry_date IS NOT NULL AND expiry_date <= date(\'now\', \'+30 days\')').fetchone()[0]
        conn.close()
        return jsonify({
            'total_products': total_products,
            'total_value': total_value,
            'low_stock': low_stock,
            'out_of_stock': out_of_stock,
            'expiring_soon': expiring_soon
        })

    elif report_type == 'rotation':
        if warehouse_id and warehouse_id.isdigit():
            wid = int(warehouse_id)
            products = conn.execute('SELECT p.name, p.quantity, p.min_quantity, (SELECT COUNT(*) FROM stock_movements WHERE product_id = p.id) as movements FROM products p WHERE p.warehouse_id=? ORDER BY movements DESC LIMIT 20', (wid,)).fetchall()
        else:
            products = conn.execute('SELECT p.name, p.quantity, p.min_quantity, (SELECT COUNT(*) FROM stock_movements WHERE product_id = p.id) as movements FROM products p ORDER BY movements DESC LIMIT 20').fetchall()
        conn.close()
        return jsonify([dict(p) for p in products])

    elif report_type == 'expiry':
        if warehouse_id and warehouse_id.isdigit():
            wid = int(warehouse_id)
            products = conn.execute('SELECT name, lot_number, expiry_date, quantity FROM products WHERE expiry_date IS NOT NULL AND expiry_date <= date(\'now\', \'+90 days\') AND warehouse_id=? ORDER BY expiry_date', (wid,)).fetchall()
        else:
            products = conn.execute('SELECT name, lot_number, expiry_date, quantity FROM products WHERE expiry_date IS NOT NULL AND expiry_date <= date(\'now\', \'+90 days\') ORDER BY expiry_date').fetchall()
        conn.close()
        return jsonify([dict(p) for p in products])

    elif report_type == 'categories':
        if warehouse_id and warehouse_id.isdigit():
            wid = int(warehouse_id)
            data = conn.execute('SELECT category, COUNT(*) as count, SUM(quantity) as total_qty, SUM(quantity * price) as value FROM products WHERE warehouse_id=? GROUP BY category', (wid,)).fetchall()
        else:
            data = conn.execute('SELECT category, COUNT(*) as count, SUM(quantity) as total_qty, SUM(quantity * price) as value FROM products GROUP BY category').fetchall()
        conn.close()
        return jsonify([dict(d) for d in data])

    elif report_type == 'low_stock':
        if warehouse_id and warehouse_id.isdigit():
            wid = int(warehouse_id)
            products = conn.execute('SELECT name, quantity, min_quantity, max_quantity, price, (min_quantity - quantity) as needed FROM products WHERE quantity <= min_quantity AND warehouse_id=? ORDER BY needed DESC', (wid,)).fetchall()
        else:
            products = conn.execute('SELECT name, quantity, min_quantity, max_quantity, price, (min_quantity - quantity) as needed FROM products WHERE quantity <= min_quantity ORDER BY needed DESC').fetchall()
        conn.close()
        return jsonify([dict(p) for p in products])

    elif report_type == 'warehouses':
        data = conn.execute('''
            SELECT w.id, w.name, w.address, 
                   COUNT(p.id) as product_count, 
                   COALESCE(SUM(p.quantity), 0) as total_quantity,
                   COALESCE(SUM(p.quantity * p.price), 0) as total_value
            FROM warehouses w
            LEFT JOIN products p ON w.id = p.warehouse_id
            GROUP BY w.id
        ''').fetchall()
        conn.close()
        return jsonify([dict(d) for d in data])

    conn.close()
    return jsonify({})

@app.route('/api/reports/export', methods=['GET'])
def export_report():
    report_type = request.args.get('type', 'products')
    warehouse_id = request.args.get('warehouse_id')
    conn = get_db()

    if report_type == 'products':
        if warehouse_id and warehouse_id.isdigit():
            products = conn.execute('SELECT * FROM products WHERE warehouse_id = ? ORDER BY name', (int(warehouse_id),)).fetchall()
        else:
            products = conn.execute('SELECT * FROM products ORDER BY name').fetchall()
        headers = ['ID', 'Nom', 'Description', 'SKU', 'Code-barres', 'Quantité', 'Seuil Min', 'Max', 'Prix', 'Catégorie', 'Lot', 'N° Série', 'DLC', 'Fournisseur', 'Entrepôt']
        data = [[p['id'], p['name'], p['description'], p['sku'], p['barcode'], p['quantity'],
                p['min_quantity'], p['max_quantity'], p['price'], p['category'], p['lot_number'],
                p['serial_number'], p['expiry_date'], p['supplier_id'], p['warehouse_id']] for p in products]
    elif report_type == 'movements':
        query = '''
            SELECT m.created_at, p.name, m.type, m.quantity, m.lot_number, m.note
            FROM stock_movements m
            JOIN products p ON m.product_id = p.id
        '''
        params = []
        if warehouse_id and warehouse_id.isdigit():
            query += ' WHERE p.warehouse_id = ?'
            params.append(int(warehouse_id))
        query += ' ORDER BY m.created_at DESC LIMIT 500'
        movements = conn.execute(query, params).fetchall()
        headers = ['Date', 'Produit', 'Type', 'Quantité', 'Lot', 'Note']
        data = [[m['created_at'], m['name'], m['type'], m['quantity'], m['lot_number'], m['note']] for m in movements]
    else:
        conn.close()
        return jsonify({'error': 'Invalid report type'}), 400

    conn.close()

    si = StringIO()
    cw = csv.writer(si)
    cw.writerow(headers)
    cw.writerows(data)
    output = si.getvalue()

    safe_type = report_type.replace('/', '_').replace('\\', '_').replace('..', '_') if report_type else 'report'
    return Response(
        output,
        mimetype="text/csv",
        headers={"Content-Disposition": f"attachment;filename=rapport_{safe_type}_{datetime.now().strftime('%Y%m%d')}.csv"}
    )



@app.route('/api/orders', methods=['GET'])
def get_orders():
    warehouse_id = request.args.get('warehouse_id')
    conn = get_db()
    status = request.args.get('status', 'all')
    query = '''
        SELECT o.*, s.name as supplier_name, w.name as warehouse_name
        FROM purchase_orders o
        LEFT JOIN suppliers s ON o.supplier_id = s.id
        LEFT JOIN warehouses w ON o.warehouse_id = w.id
    '''
    conditions = []
    params = []
    if status != 'all':
        conditions.append('o.status = ?')
        params.append(status)
    if warehouse_id and warehouse_id.isdigit():
        conditions.append('o.warehouse_id = ?')
        params.append(int(warehouse_id))
    
    if conditions:
        query += ' WHERE ' + ' AND '.join(conditions)
    query += ' ORDER BY o.created_at DESC'

    orders = conn.execute(query, params).fetchall()
    conn.close()
    return jsonify([dict(o) for o in orders])

@app.route('/api/orders', methods=['POST'])
def create_order():
    data = request.json
    conn = get_db()

    order_number = f"PO-{datetime.now().strftime('%Y%m%d%H%M%S')}"
    supplier_id = data.get('supplier_id')
    warehouse_id = data.get('warehouse_id', 1)
    notes = data.get('notes', '')
    items = data.get('items', [])

    try:
        conn.execute('''
            INSERT INTO purchase_orders (order_number, supplier_id, warehouse_id, notes, status)
            VALUES (?, ?, ?, ?, 'brouillon')
        ''', (order_number, supplier_id, warehouse_id, notes))
        order_id = conn.execute('SELECT last_insert_rowid()').fetchone()[0]

        total = 0
        for item in items:
            conn.execute('''
                INSERT INTO purchase_order_items (order_id, product_id, quantity, unit_price)
                VALUES (?, ?, ?, ?)
            ''', (order_id, item['product_id'], item['quantity'], item.get('unit_price', 0)))
            total += item['quantity'] * item.get('unit_price', 0)

        conn.execute('UPDATE purchase_orders SET total=? WHERE id=?', (total, order_id))
        conn.commit()
        conn.close()
        return jsonify({'success': True, 'order_id': order_id})
    except Exception as e:
        conn.close()
        return jsonify({'error': str(e)}), 400

@app.route('/api/orders/<int:order_id>', methods=['PUT'])
def update_order(order_id):
    data = request.json
    conn = get_db()
    status = data.get('status')

    if status == 'recue':
        conn.execute('''
            UPDATE purchase_orders SET status='recue', received_at=CURRENT_TIMESTAMP WHERE id=?
        ''', (order_id,))
        items = conn.execute('SELECT * FROM purchase_order_items WHERE order_id=?', (order_id,)).fetchall()
        order = conn.execute('SELECT warehouse_id FROM purchase_orders WHERE id=?', (order_id,)).fetchone()
        
        default_location = conn.execute('SELECT id FROM locations WHERE warehouse_id=? LIMIT 1', 
                                        (order['warehouse_id'],)).fetchone()
        
        for item in items:
            conn.execute('UPDATE products SET quantity = quantity + ? WHERE id=?',
                      (item['quantity'], item['product_id']))
            
            if default_location:
                existing = conn.execute('SELECT id FROM stock WHERE product_id=? AND location_id=?', 
                                        (item['product_id'], default_location['id'])).fetchone()
                if existing:
                    conn.execute('UPDATE stock SET quantity = quantity + ? WHERE id=?',
                               (item['quantity'], existing['id']))
                else:
                    conn.execute('INSERT INTO stock (product_id, location_id, quantity) VALUES (?, ?, ?)',
                               (item['product_id'], default_location['id'], item['quantity']))
            
            conn.execute('''
                INSERT INTO stock_movements (product_id, type, quantity, dest_location_id, note)
                VALUES (?, 'in', ?, ?, 'Commande recue')
            ''', (item['product_id'], item['quantity'], default_location['id'] if default_location else None))
            
    elif status == 'paye':
        order = conn.execute('SELECT * FROM purchase_orders WHERE id=?', (order_id,)).fetchone()
        conn.execute('''
            UPDATE purchase_orders SET status='paye', paid_at=CURRENT_TIMESTAMP WHERE id=?
        ''', (order_id,))
        
        if order and order['total']:
            conn.execute('UPDATE main_account SET current_balance = current_balance - ? WHERE id = 1', (order['total'],))
            conn.execute('''
                INSERT INTO main_account_transactions (type, amount, reason, reference_id, note)
                VALUES ('out', ?, 'supplier_order', ?, ?)
            ''', (order['total'], order_id, 'Achat Fournisseur: ' + order['order_number']))
            
            # CREATE SUPPLIER INVOICE
            supplier_inv_count = conn.execute("SELECT COUNT(*) FROM invoices WHERE type='fournisseur'").fetchone()[0]
            today = datetime.now().strftime('%Y%m%d')
            supplier_inv_number = f"FAC-FOU-{today}-{supplier_inv_count + 1:04d}"
            
            conn.execute('''
                INSERT INTO invoices (
                    invoice_number, customer_id, supplier_id, warehouse_id, status, type,
                    subtotal, discount_total, tax_amount, total,
                    paid_at, payment_method, notes
                ) VALUES (?, NULL, ?, ?, 'payee', 'fournisseur', ?, 0, 0, ?, CURRENT_TIMESTAMP, 'transfer', ?)
            ''', (supplier_inv_number, order['supplier_id'], order['warehouse_id'], order['total'], order['total'], 
                   f"Facture fournisseur - Commande: {order['order_number']}"))
            
            new_invoice_id = conn.execute('SELECT last_insert_rowid()').fetchone()[0]
            
            # Add invoice items from purchase order
            order_items = conn.execute('SELECT * FROM purchase_order_items WHERE order_id=?', (order_id,)).fetchall()
            for item in order_items:
                product = conn.execute('SELECT * FROM products WHERE id=?', (item['product_id'],)).fetchone()
                if product:
                    line_total = item['quantity'] * item['unit_price']
                    conn.execute('''
                        INSERT INTO invoice_items (
                            invoice_id, product_id, product_name, product_sku,
                            quantity, unit_price, discount_percent, tax_rate, line_total
                        ) VALUES (?, ?, ?, ?, ?, ?, 0, 0, ?)
                    ''', (new_invoice_id, item['product_id'], product['name'], product['sku'],
                          item['quantity'], item['unit_price'], line_total))
            
    elif status == 'annulee':
        order = conn.execute('SELECT * FROM purchase_orders WHERE id=?', (order_id,)).fetchone()
        
        if order:
            # Reverse stock entries if order was received
            if order['received_at']:
                items = conn.execute('SELECT * FROM purchase_order_items WHERE order_id=?', (order_id,)).fetchall()
                warehouse_id = order['warehouse_id']
                
                for item in items:
                    # Subtract from products
                    conn.execute('UPDATE products SET quantity = quantity - ? WHERE id=? AND quantity >= ?',
                               (item['quantity'], item['product_id'], item['quantity']))
                    
                    # Get default location
                    default_location = conn.execute('SELECT id FROM locations WHERE warehouse_id=? LIMIT 1', 
                                                  (warehouse_id,)).fetchone()
                    
                    if default_location:
                        # Update stock table
                        stock_entry = conn.execute('SELECT id, quantity FROM stock WHERE product_id=? AND location_id=?',
                                                 (item['product_id'], default_location['id'])).fetchone()
                        if stock_entry:
                            new_qty = stock_entry['quantity'] - item['quantity']
                            if new_qty > 0:
                                conn.execute('UPDATE stock SET quantity = quantity - ? WHERE id=? AND quantity >= ?',
                                           (item['quantity'], stock_entry['id'], item['quantity']))
                            else:
                                conn.execute('DELETE FROM stock WHERE id=?', (stock_entry['id'],))
                        
                        # Create OUT movement for cancelled order
                        conn.execute('''
                            INSERT INTO stock_movements (product_id, type, quantity, source_location_id, note)
                            VALUES (?, 'out', ?, ?, ?)
                        ''', (item['product_id'], item['quantity'], default_location['id'], 
                              f"Commande annulée: {order['order_number']}"))
            
            # Refund main account if order was paid
            if order['paid_at'] and order['total']:
                conn.execute('UPDATE main_account SET current_balance = current_balance + ? WHERE id = 1', 
                           (order['total'],))
                conn.execute('''
                    INSERT INTO main_account_transactions (type, amount, reason, reference_id, note)
                    VALUES ('in', ?, 'order_cancelled', ?, ?)
                ''', (order['total'], order_id, f"Commande annulée: {order['order_number']}"))
            
            # Cancel related supplier invoice
            supplier_invoice = conn.execute(
                'SELECT id FROM invoices WHERE notes LIKE ? AND type="fournisseur"', 
                (f"%Commande: {order['order_number']}%",)).fetchone()
            
            if supplier_invoice:
                conn.execute("UPDATE invoices SET status='annulee', updated_at=CURRENT_TIMESTAMP WHERE id=?", 
                           (supplier_invoice['id'],))
        
        conn.execute("UPDATE purchase_orders SET status='annulee' WHERE id=?", (order_id,))
    else:
        conn.execute('UPDATE purchase_orders SET notes=? WHERE id=?', (data.get('notes', ''), order_id))

    conn.commit()
    conn.close()
    return jsonify({'success': True})

@app.route('/api/orders/<int:order_id>/items', methods=['GET'])
def get_order_items(order_id):
    conn = get_db()
    items = conn.execute('''
        SELECT poi.*, p.name as product_name
        FROM purchase_order_items poi
        JOIN products p ON poi.product_id = p.id
        WHERE poi.order_id=?
    ''', (order_id,)).fetchall()
    conn.close()
    return jsonify([dict(i) for i in items])

@app.route('/api/orders/<int:order_id>', methods=['DELETE'])
def delete_order(order_id):
    conn = get_db()
    conn.execute('DELETE FROM purchase_order_items WHERE order_id=?', (order_id,))
    conn.execute('DELETE FROM purchase_orders WHERE id=?', (order_id,))
    conn.commit()
    conn.close()
    return jsonify({'success': True})

@app.route('/api/reorder-rules', methods=['GET'])
def get_reorder_rules():
    warehouse_id = request.args.get('warehouse_id')
    conn = get_db()
    
    if warehouse_id and warehouse_id.isdigit():
        rules = conn.execute('''
            SELECT r.*, p.name as product_name, p.quantity as current_qty, p.min_quantity, s.name as supplier_name
            FROM reordering_rules r
            JOIN products p ON r.product_id = p.id
            LEFT JOIN suppliers s ON r.supplier_id = s.id
            WHERE r.warehouse_id = ?
            ORDER BY p.name
        ''', (int(warehouse_id),)).fetchall()
    else:
        rules = conn.execute('''
            SELECT r.*, p.name as product_name, p.quantity as current_qty, p.min_quantity, s.name as supplier_name
            FROM reordering_rules r
            JOIN products p ON r.product_id = p.id
            LEFT JOIN suppliers s ON r.supplier_id = s.id
            ORDER BY p.name
        ''').fetchall()
    
    conn.close()
    return jsonify([dict(r) for r in rules])

@app.route('/api/reorder-rules', methods=['POST'])
def add_reorder_rule():
    data = request.json
    conn = get_db()
    try:
        conn.execute('''
            INSERT INTO reordering_rules (product_id, warehouse_id, min_quantity, max_quantity, trigger_type, supplier_id)
            VALUES (?, ?, ?, ?, ?, ?)
        ''', (data['product_id'], data.get('warehouse_id', 1), data.get('min_quantity', 5), 
              data.get('max_quantity', 100), data.get('trigger_type', 'manual'), data.get('supplier_id')))
        conn.commit()
        conn.close()
        return jsonify({'success': True})
    except Exception as e:
        conn.close()
        return jsonify({'error': str(e)}), 400

@app.route('/api/reorder-rules/<int:rule_id>', methods=['PUT'])
def update_reorder_rule(rule_id):
    data = request.json
    conn = get_db()
    conn.execute('''
        UPDATE reordering_rules SET min_quantity=?, max_quantity=?, trigger_type=?, supplier_id=?
        WHERE id=?
    ''', (data.get('min_quantity', 5), data.get('max_quantity', 100), data.get('trigger_type', 'manual'),
          data.get('supplier_id'), rule_id))
    conn.commit()
    conn.close()
    return jsonify({'success': True})

@app.route('/api/reorder-rules/<int:rule_id>', methods=['DELETE'])
def delete_reorder_rule(rule_id):
    conn = get_db()
    conn.execute('DELETE FROM reordering_rules WHERE id=?', (rule_id,))
    conn.commit()
    conn.close()
    return jsonify({'success': True})

@app.route('/api/replenishment', methods=['GET'])
def get_replenishment():
    warehouse_id = request.args.get('warehouse_id')
    conn = get_db()
    
    if warehouse_id and warehouse_id.isdigit():
        products = conn.execute('''
            SELECT r.id as rule_id, p.id as product_id, p.name, p.sku, p.quantity as current_qty, r.min_quantity, r.max_quantity, r.trigger_type,
                   r.supplier_id, r.warehouse_id as rule_warehouse_id, s.name as supplier_name,
                   MAX(0, r.max_quantity - p.quantity) as suggested_qty
            FROM reordering_rules r
            JOIN products p ON r.product_id = p.id
            LEFT JOIN suppliers s ON r.supplier_id = s.id
            WHERE r.warehouse_id = ?
            ORDER BY p.name
        ''', (int(warehouse_id),)).fetchall()
    else:
        products = conn.execute('''
            SELECT r.id as rule_id, p.id as product_id, p.name, p.sku, p.quantity as current_qty, r.min_quantity, r.max_quantity, r.trigger_type,
                   r.supplier_id, r.warehouse_id as rule_warehouse_id, s.name as supplier_name,
                   MAX(0, r.max_quantity - p.quantity) as suggested_qty
            FROM reordering_rules r
            JOIN products p ON r.product_id = p.id
            LEFT JOIN suppliers s ON r.supplier_id = s.id
            ORDER BY p.name
        ''').fetchall()
    
    conn.close()
    return jsonify([dict(p) for p in products])

@app.route('/api/notifications', methods=['GET'])
def get_notifications():
    warehouse_id = request.args.get('warehouse_id')
    conn = get_db()
    
    query = '''
        SELECT n.*, p.name as product_name
        FROM notifications n
        LEFT JOIN products p ON n.product_id = p.id
    '''
    params = []
    if warehouse_id and warehouse_id.isdigit():
        query += ' WHERE n.warehouse_id = ? OR n.warehouse_id IS NULL'
        params.append(int(warehouse_id))
    query += ' ORDER BY n.created_at DESC LIMIT 50'
    
    notifications = conn.execute(query, params).fetchall()
    unread = conn.execute('SELECT COUNT(*) FROM notifications WHERE is_read = 0').fetchone()[0]
    
    conn.close()
    return jsonify({'notifications': [dict(n) for n in notifications], 'unread_count': unread})

@app.route('/api/notifications/<int:notification_id>/read', methods=['POST'])
def mark_notification_read(notification_id):
    conn = get_db()
    conn.execute('UPDATE notifications SET is_read = 1 WHERE id=?', (notification_id,))
    conn.commit()
    conn.close()
    return jsonify({'success': True})

@app.route('/api/notifications/mark-all-read', methods=['POST'])
def mark_all_notifications_read():
    conn = get_db()
    conn.execute('UPDATE notifications SET is_read = 1')
    conn.commit()
    conn.close()
    return jsonify({'success': True})

@app.route('/api/seed-data', methods=['POST'])
def seed_data():
    if not app.debug:
        return jsonify({'error': 'Action non autorisée en production'}), 403
    conn = get_db()
    c = conn.cursor()
    
    products_data = [
        ('Clavier USB Standard', 'Clavier filaire 105 touches', 'KB001', '123456789001', 25.99, 45, 10, 'Périphériques'),
        ('Souris Optique Sans Fil', 'Souris wireless 1600 DPI', 'MO001', '123456789002', 19.99, 60, 15, 'Périphériques'),
        ('Écran 24 pouces Full HD', 'Moniteur LED 1920x1080', 'EC001', '123456789003', 149.99, 20, 5, 'Périphériques'),
        ('Webcam HD 1080p', 'Caméra autofocus avec micro', 'WC001', '123456789004', 59.99, 30, 8, 'Périphériques'),
        ('Casque Audio USB', 'Microphone intégré réduction bruit', 'CA001', '123456789005', 45.99, 35, 10, 'Périphériques'),
        ('Clavier Mécanique RGB', 'Switchs bleus rétroéclairé', 'KB002', '123456789006', 89.99, 25, 8, 'Périphériques'),
        ('Tapis de Souris XL', 'Surface 90x40cm antidérapant', 'TS001', '123456789007', 15.99, 80, 20, 'Accessoires'),
        ('Hub USB 7 Ports', 'Alimentation externe 5V/2A', 'HB001', '123456789008', 29.99, 40, 12, 'Accessoires'),
        ('Câble HDMI 2m', '高速 HDMI 4K@60Hz', 'CB001', '123456789009', 12.99, 100, 25, 'Câbles'),
        ('Câble HDMI 5m', '高速 HDMI 4K@60Hz', 'CB002', '123456789010', 19.99, 60, 15, 'Câbles'),
        ('Support Laptop Aluminium', 'Régliable hauteur 15-45cm', 'SL001', '123456789011', 39.99, 30, 10, 'Accessoires'),
        ('Enceinte Bluetooth Portable', '10W batterie 8h', 'EN001', '123456789012', 34.99, 45, 12, 'Périphériques'),
        ('Lampe Bureau LED', '3 niveaux luminosité tactile', 'LB001', '123456789013', 24.99, 55, 15, 'Éclairage'),
        ('Lampe Bureau LED USB', 'Clip sur écran USB', 'LB002', '123456789014', 14.99, 70, 20, 'Éclairage'),
        ('Sac à Dos Tech 15.6"', 'Compartiment laptop antichoc', 'SA001', '123456789015', 49.99, 35, 10, 'Accessoires'),
        ('Agrafeuse Métallique', 'Capacité 50 feuilles', 'AG001', '123456789016', 8.99, 90, 25, 'Bureau'),
        ('Bloc-Notes A4 100 Feuilles', 'Ligné perforation', 'BN001', '123456789017', 5.99, 120, 30, 'Bureau'),
        ('Stylo Bille Lot de 50', 'Bleu noir rouge vert', 'ST001', '123456789018', 9.99, 150, 40, 'Bureau'),
        ('Marqueurs Fluo Lot de 12', 'Couleurs assorted', 'MR001', '123456789019', 7.99, 80, 20, 'Bureau'),
        ('Piles AA Alcaline Lot de 24', 'Longue durée 10 ans', 'BT001', '123456789020', 11.99, 100, 30, 'Accessoires'),
    ]
    
    warehouses = [(1, 'Entrepôt Principal'), (2, 'Entrepot fes')]
    categories_list = ['Périphériques', 'Accessoires', 'Bureau', 'Câbles', 'Éclairage']
    
    c.execute('DELETE FROM stock_movements')
    c.execute('DELETE FROM products')
    conn.commit()
    
    product_ids = []
    for i, (name, desc, sku, barcode, price, qty, min_qty, category) in enumerate(products_data):
        wh = warehouses[i % len(warehouses)]
        c.execute('''
            INSERT INTO products (name, description, sku, barcode, quantity, min_quantity, max_quantity, price, price_base, price_loyal, price_school, price_student, category, warehouse_id, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', (name, desc, sku, barcode, qty, min_qty, qty * 2, price, price * 0.6, price * 0.9, price * 0.85, price * 0.8, category, wh[0], datetime.now().isoformat()))
        product_ids.append(c.lastrowid)
    
    c.execute('UPDATE products SET purchase_price_avg = price_base WHERE purchase_price_avg IS NULL OR purchase_price_avg = 0')
    conn.commit()
    
    movement_types = ['in', 'out', 'in', 'in', 'out', 'in', 'in', 'out']
    for _ in range(60):
        product_id = random.choice(product_ids)
        movement_type = random.choice(movement_types)
        quantity = random.randint(1, 30)
        days_ago = random.randint(0, 29)
        created_at = (datetime.now() - timedelta(days=days_ago, hours=random.randint(0, 23), minutes=random.randint(0, 59))).isoformat()
        
        c.execute('UPDATE products SET quantity = quantity + ? WHERE id=?', (quantity if movement_type == 'in' else -quantity, product_id))
        
        c.execute('''
            INSERT INTO stock_movements (product_id, type, quantity, note, created_at)
            VALUES (?, ?, ?, ?, ?)
        ''', (product_id, movement_type, quantity, f'Mouvement aléatoire - {created_at[:10]}', created_at))
    
    conn.commit()
    conn.close()
    
    return jsonify({'success': True, 'message': '20 produits et 60 mouvements générés avec succès'})





@app.route('/api/invoices', methods=['GET'])
def get_invoices():
    warehouse_id = request.args.get('warehouse_id')
    status = request.args.get('status', 'all')
    conn = get_db()
    
    query = '''
        SELECT i.*, 
               c.name as customer_name, c.client_code,
               s.name as supplier_name,
               w.name as warehouse_name
        FROM invoices i
        LEFT JOIN customers c ON i.customer_id = c.id
        LEFT JOIN suppliers s ON i.supplier_id = s.id
        LEFT JOIN warehouses w ON i.warehouse_id = w.id
        WHERE 1=1
    '''
    params = []
    if warehouse_id and warehouse_id.isdigit():
        query += ' AND i.warehouse_id = ?'
        params.append(int(warehouse_id))
    if status != 'all':
        query += ' AND i.status = ?'
        params.append(status)
    query += ' ORDER BY i.created_at DESC'
    
    invoices = conn.execute(query, params).fetchall()
    invoices_list = [dict(i) for i in invoices]
    
    # Also get POS tickets (client comptoir transactions)
    tickets_query = '''
        SELECT t.id, t.transaction_number as invoice_number, NULL as customer_id, 
               'Client Comptoir' as customer_name, NULL as client_code,
               t.total, t.status, t.created_at, t.payment_method,
               t.subtotal as subtotal, t.discount_total, t.tax_amount,
               t.tendered_amount, t.change_given
        FROM pos_transactions t
        WHERE t.transaction_number LIKE 'Ticket-%'
    '''
    if status != 'all':
        tickets_query += ' AND t.status = ?'
    tickets_query += ' ORDER BY t.created_at DESC'
    
    tickets = conn.execute(tickets_query, params if status != 'all' else []).fetchall()
    
    conn.close()
    
    # Combine invoices and tickets
    all_docs = invoices_list + [dict(t) for t in tickets]
    all_docs.sort(key=lambda x: x.get('created_at', ''), reverse=True)
    
    return jsonify(all_docs)

@app.route('/api/invoices', methods=['POST'])
def create_invoice():
    data = request.json
    conn = get_db()
    
    try:
        seq = next_sequence(conn, 'fac_counter')
        invoice_number = f"FAC-{datetime.now().strftime('%Y%m%d')}-{seq:04d}"
        
        warehouse_id = data.get('warehouse_id', 1)
        customer_id = data.get('customer_id')
        notes = data.get('notes', '')
        items = data.get('items', [])
        
        conn.execute('''
            INSERT INTO invoices (invoice_number, customer_id, warehouse_id, notes, status)
            VALUES (?, ?, ?, ?, 'brouillon')
        ''', (invoice_number, customer_id, warehouse_id, notes))
        invoice_id = conn.execute('SELECT last_insert_rowid()').fetchone()[0]
        
        subtotal = 0
        discount_total = 0
        tax_amount = 0
        
        customer = conn.execute('SELECT * FROM customers WHERE id=?', (customer_id,)).fetchone() if customer_id else None
        
        for item in items:
            product = conn.execute('SELECT * FROM products WHERE id=?', (item['product_id'],)).fetchone()
            if not product:
                continue
            
            p = dict(product)
            if customer:
                unit_price = get_price_for_customer(p, customer['type'], customer['is_loyal'])
            else:
                unit_price = item.get('unit_price', p['price_base'] if p['price_base'] > 0 else p['price'])
            
            discount_percent = item.get('discount_percent', 0)
            qty = item.get('quantity', 1)
            tax_rate = float(item.get('tax_rate', p.get('tax_category', '20') or '20'))
            
            line_subtotal = qty * unit_price
            discount_amount = line_subtotal * (discount_percent / 100)
            line_total = line_subtotal - discount_amount
            
            tax_line = line_total * (tax_rate / 100)
            
            subtotal += line_subtotal
            discount_total += discount_amount
            tax_amount += tax_line
            
            conn.execute('''
                INSERT INTO invoice_items (invoice_id, product_id, product_name, product_sku, quantity, unit_price, discount_percent, tax_rate, line_total)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ''', (invoice_id, item['product_id'], product['name'], product['sku'], qty, unit_price, discount_percent, tax_rate, line_total))
            
            cur = conn.execute('UPDATE products SET quantity = quantity - ? WHERE id=? AND quantity >= ?', (qty, item['product_id'], qty))
            if cur.rowcount == 0:
                current = conn.execute('SELECT quantity FROM products WHERE id = ?', (item['product_id'],)).fetchone()
                stock_qty = current['quantity'] if current else 0
                raise ValueError(f'Stock insuffisant pour {product["name"]}. Disponible: {stock_qty}, demandé: {qty}')
            conn.execute('''
                INSERT INTO stock_movements (product_id, type, quantity, note)
                VALUES (?, 'out', ?, ?)
            ''', (item['product_id'], qty, f'Vente facture {invoice_number}'))
        
        total = subtotal - discount_total + tax_amount
        
        conn.execute('''
            UPDATE invoices SET subtotal=?, discount_total=?, tax_amount=?, total=? WHERE id=?
        ''', (subtotal, discount_total, tax_amount, total, invoice_id))
        
        conn.commit()
        conn.close()
        return jsonify({'success': True, 'invoice_id': invoice_id, 'invoice_number': invoice_number})
    except Exception as e:
        conn.close()
        return jsonify({'error': str(e)}), 400

@app.route('/api/invoices/<int:invoice_id>', methods=['GET'])
def get_invoice(invoice_id):
    conn = get_db()
    invoice = conn.execute('''
        SELECT i.*, c.name as customer_name, c.client_code, c.type as customer_type, c.address as customer_address, c.phone as customer_phone, c.email as customer_email,
               w.name as warehouse_name, w.address as warehouse_address
        FROM invoices i
        LEFT JOIN customers c ON i.customer_id = c.id
        LEFT JOIN warehouses w ON i.warehouse_id = w.id
        WHERE i.id=?
    ''', (invoice_id,)).fetchone()
    
    if not invoice:
        conn.close()
        return jsonify({'error': 'Facture non trouvée'}), 404
    
    items = conn.execute('SELECT * FROM invoice_items WHERE invoice_id=?', (invoice_id,)).fetchall()
    
    result = dict(invoice)
    result['items'] = [dict(i) for i in items]
    
    conn.close()
    return jsonify(result)

@app.route('/api/invoices/<int:invoice_id>', methods=['PUT'])
def update_invoice(invoice_id):
    data = request.json
    conn = get_db()
    status = data.get('status')
    
    if status == 'payee':
        conn.execute('''
            UPDATE invoices SET status='payee', paid_at=CURRENT_TIMESTAMP, updated_at=CURRENT_TIMESTAMP WHERE id=?
        ''', (invoice_id,))
    elif status == 'annulee':
        old_items = conn.execute('SELECT * FROM invoice_items WHERE invoice_id=?', (invoice_id,)).fetchall()
        for item in old_items:
            conn.execute('UPDATE products SET quantity = quantity + ? WHERE id=?', (item['quantity'], item['product_id']))
            conn.execute('''
                INSERT INTO stock_movements (product_id, type, quantity, note)
                VALUES (?, 'in', ?, ?)
            ''', (item['product_id'], item['quantity'], 'Annulation facture'))
        conn.execute("UPDATE invoices SET status='annulee', updated_at=CURRENT_TIMESTAMP WHERE id=?", (invoice_id,))
    else:
        conn.execute('UPDATE invoices SET notes=?, updated_at=CURRENT_TIMESTAMP WHERE id=?', (data.get('notes', ''), invoice_id))
    
    conn.commit()
    conn.close()
    return jsonify({'success': True})

@app.route('/api/invoices/<int:invoice_id>', methods=['DELETE'])
def delete_invoice(invoice_id):
    conn = get_db()
    invoice = conn.execute('SELECT status FROM invoices WHERE id=?', (invoice_id,)).fetchone()
    if invoice and invoice['status'] == 'payee':
        conn.close()
        return jsonify({'error': 'Impossible de supprimer une facture payée'}), 400
    
    conn.execute('DELETE FROM invoice_items WHERE invoice_id=?', (invoice_id,))
    conn.execute('DELETE FROM invoices WHERE id=?', (invoice_id,))
    conn.commit()
    conn.close()
    return jsonify({'success': True})

@app.route('/api/invoices/<int:invoice_id>/items', methods=['GET'])
def get_invoice_items(invoice_id):
    conn = get_db()
    items = conn.execute('SELECT * FROM invoice_items WHERE invoice_id=?', (invoice_id,)).fetchall()
    conn.close()
    return jsonify([dict(i) for i in items])

@app.route('/api/invoices/<int:invoice_id>/items', methods=['POST'])
def add_invoice_item(invoice_id):
    data = request.json
    conn = get_db()
    
    try:
        product_id = data.get('product_id')
        if not product_id:
            return jsonify({'error': 'product_id requis'}), 400
        product_row = conn.execute('SELECT * FROM products WHERE id=?', (product_id,)).fetchone()
        if not product_row:
            return jsonify({'error': 'Produit non trouvé'}), 404
        p = dict(product_row)
        
        invoice = conn.execute('SELECT * FROM invoices WHERE id=?', (invoice_id,)).fetchone()
        if not invoice:
            return jsonify({'error': 'Facture non trouvée'}), 404
        customer = conn.execute('SELECT * FROM customers WHERE id=?', (invoice['customer_id'],)).fetchone() if invoice['customer_id'] else None
        
        if customer:
            unit_price = get_price_for_customer(p, customer['type'], customer['is_loyal'])
        else:
            unit_price = data.get('unit_price', p['price_base'] if p['price_base'] > 0 else p['price'])
        
        qty = data.get('quantity', 1)
        tax_rate = float(data.get('tax_rate', p.get('tax_category', '20') or '20'))
        discount_percent = data.get('discount_percent', 0)
        
        line_total = qty * unit_price * (1 - discount_percent / 100)
        
        conn.execute('''
            INSERT INTO invoice_items (invoice_id, product_id, product_name, product_sku, quantity, unit_price, discount_percent, tax_rate, line_total)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', (invoice_id, product_id, p['name'], p['sku'], qty, unit_price, discount_percent, tax_rate, line_total))
        
        conn.execute('UPDATE products SET quantity = quantity - ? WHERE id=? AND quantity >= ?', (qty, product_id, qty))
        conn.execute('''
            INSERT INTO stock_movements (product_id, type, quantity, note)
            VALUES (?, 'out', ?, ?)
        ''', (product_id, qty, f'Vente facture {invoice["invoice_number"]}'))
        
        recalculate_invoice(invoice_id, conn)
        conn.commit()
        conn.close()
        return jsonify({'success': True})
    except Exception as e:
        conn.close()
        return jsonify({'error': str(e)}), 400

def recalculate_invoice(invoice_id, conn):
    items = conn.execute('SELECT * FROM invoice_items WHERE invoice_id=?', (invoice_id,)).fetchall()
    
    subtotal = 0
    discount_total = 0
    tax_amount = 0
    for item in items:
        qty = item['quantity'] or 0
        uprice = item['unit_price'] or 0
        dpct = (item['discount_percent'] or 0) / 100
        trate = (item['tax_rate'] or 0) / 100
        subtotal += qty * uprice
        discount_total += qty * uprice * dpct
        after_discount = qty * uprice * (1 - dpct)
        tax_amount += after_discount * trate
    total = subtotal - discount_total + tax_amount
    
    conn.execute('''
        UPDATE invoices SET subtotal=?, discount_total=?, tax_amount=?, total=?, updated_at=CURRENT_TIMESTAMP WHERE id=?
    ''', (subtotal, discount_total, tax_amount, total, invoice_id))

@app.route('/api/invoices/<int:invoice_id>/items/<int:item_id>', methods=['DELETE'])
def delete_invoice_item(invoice_id, item_id):
    conn = get_db()
    item = conn.execute('SELECT * FROM invoice_items WHERE id=? AND invoice_id=?', (item_id, invoice_id)).fetchone()
    
    if item:
        conn.execute('UPDATE products SET quantity = quantity + ? WHERE id=?', (item['quantity'], item['product_id']))
        conn.execute('DELETE FROM invoice_items WHERE id=?', (item_id,))
        recalculate_invoice(invoice_id, conn)
        conn.commit()
    
    conn.close()
    return jsonify({'success': True})

@app.route('/api/invoices/<int:invoice_id>/pdf', methods=['GET'])
def generate_invoice_pdf(invoice_id):
    conn = get_db()
    invoice = conn.execute('''
        SELECT i.*, 
               c.name as customer_name, c.client_code, c.type as customer_type, c.address as customer_address, c.phone as customer_phone, c.email as customer_email, c.ice as customer_ice,
               s.name as supplier_name, s.address as supplier_address, s.phone as supplier_phone, s.email as supplier_email,
               w.name as warehouse_name, w.address as warehouse_address, w.manager as warehouse_manager, 
               w.ice as warehouse_ice, w.patente as warehouse_patente, w.rc as warehouse_rc, w.taxe_number as warehouse_tax_number, w.phone as warehouse_phone
        FROM invoices i
        LEFT JOIN customers c ON i.customer_id = c.id
        LEFT JOIN suppliers s ON i.supplier_id = s.id
        LEFT JOIN warehouses w ON i.warehouse_id = w.id
        WHERE i.id=?
    ''', (invoice_id,)).fetchone()
    
    if not invoice:
        conn.close()
        return jsonify({'error': 'Facture non trouvee'}), 404
    
    invoice = dict(invoice)
    items = conn.execute('SELECT * FROM invoice_items WHERE invoice_id=?', (invoice_id,)).fetchall()
    items = [dict(item) for item in items]
    
    customer_type_labels = {
        'particulier': 'Particulier',
        'entreprise': 'Entreprise',
        'ecole': 'Ecole',
        'etudiant': 'Etudiant'
    }
    
    status_labels = {
        'brouillon': 'Brouillon',
        'envoyee': 'Envoyee',
        'payee': 'Payee',
        'annulee': 'Annulee'
    }
    
    invoice_data = dict(invoice)
    items_data = [dict(item) for item in items]
    
    def _esc(d):
        for k, v in d.items():
            if v is None:
                d[k] = ''
            elif isinstance(v, str):
                d[k] = html.escape(v)
    def _n(val, default='-'):
        return val if val else default
    _esc(invoice_data)
    for item in items_data:
        _esc(item)
    
    # Customer info
    customer_name = _n(invoice_data.get('customer_name'), 'Client Comptoir')
    customer_ice = _n(invoice_data.get('customer_ice'), '-')
    
    # Build party HTML based on invoice type
    if _n(invoice_data.get('type'), 'facture') == 'fournisseur':
        party_html = f"""
        <div class="party-box">
            <div class="party-title">Fournisseur</div>
            <div class="party-name">{_n(invoice_data.get('supplier_name'), 'Fournisseur')}</div>
            <div class="party-detail">{_n(invoice_data.get('supplier_address'), '')}</div>
            <div class="party-detail">Tel: {_n(invoice_data.get('supplier_phone'), '-')}</div>
        </div>
        """
    else:
        party_html = f"""
        <div class="party-box">
            <div class="party-title">Client</div>
            <div class="party-name">{customer_name}</div>
            <div class="party-detail">Code: <strong>{_n(invoice_data.get('client_code'), '-')}</strong></div>
            <div class="party-detail">ICE: <strong>{customer_ice}</strong></div>
            <div class="party-detail">{_n(invoice_data.get('customer_address'), '')}</div>
            <div class="party-detail">Tel: {_n(invoice_data.get('customer_phone'), '-')}</div>
        </div>
        """
    
    # Company info
    company_name = 'Bibliotheque Badr'
    company_address = _n(invoice_data.get('warehouse_address'), 'Rue Mohammed V, Gueliz, Marrakech')
    company_ice = _n(invoice_data.get('warehouse_ice'), '001234567000089')
    company_patente = _n(invoice_data.get('warehouse_patente'), '12345678')
    company_rc = _n(invoice_data.get('warehouse_rc'), '12345')
    company_tax = _n(invoice_data.get('warehouse_tax_number'), '3456789012')
    company_phone = _n(invoice_data.get('warehouse_phone'), '0524441234')
    
    # Colors from app (neutral palette matching shadcn/ui)
    primary_color = '#343434'
    primary_dark = '#252525'
    
    # Build HTML with modern design
    pdf_html = f"""<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Facture {invoice_data['invoice_number']}</title>
    <style>
        * {{ margin: 0; padding: 0; box-sizing: border-box; }}
        body {{ font-family: 'Geist Variable', system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; font-size: 11px; padding: 30px; color: #252525; background: #f7f7f7; }}
        .page {{ max-width: 800px; margin: 0 auto; background: white; border-radius: 14px; box-shadow: 0 1px 3px 0 rgba(0,0,0,0.06); overflow: hidden; }}
        .header {{ display: flex; justify-content: space-between; align-items: flex-start; padding: 28px 32px; border-bottom: 1px solid #ebebeb; background: white; }}
        .company-info {{ flex: 1; }}
        .company-name {{ font-size: 22px; font-weight: 700; margin-bottom: 6px; color: #252525; }}
        .company-address {{ font-size: 11px; color: #8e8e8e; line-height: 1.5; }}
        .invoice-info {{ text-align: right; }}
        .invoice-title {{ font-size: 28px; font-weight: 700; margin-bottom: 8px; color: #252525; }}
        .invoice-number {{ display: inline-block; background: #f7f7f7; padding: 6px 14px; border-radius: 10px; font-size: 13px; font-weight: 600; color: #252525; }}
        .meta-info {{ margin-top: 12px; font-size: 11px; color: #8e8e8e; }}
        .meta-info div {{ margin: 2px 0; }}
        .content {{ padding: 28px 32px; }}
        .parties {{ display: flex; gap: 20px; margin-bottom: 28px; }}
        .party-box {{ flex: 1; background: #f7f7f7; padding: 18px 20px; border-radius: 10px; border-left: 4px solid {primary_color}; }}
        .party-title {{ font-size: 10px; font-weight: 600; color: {primary_color}; text-transform: uppercase; letter-spacing: 0.03em; margin-bottom: 10px; }}
        .party-name {{ font-size: 15px; font-weight: 700; margin-bottom: 6px; color: #252525; }}
        .party-detail {{ font-size: 11px; color: #8e8e8e; margin: 3px 0; }}
        .party-detail strong {{ color: #252525; font-weight: 600; }}
        .table-wrapper {{ margin: 24px 0; border-radius: 10px; overflow: hidden; border: 1px solid #ebebeb; }}
        table {{ width: 100%; border-collapse: collapse; }}
        th {{ background: {primary_color}; color: white; padding: 10px 12px; text-align: left; font-weight: 600; font-size: 10px; text-transform: uppercase; letter-spacing: 0.03em; }}
        td {{ padding: 10px 12px; border-bottom: 1px solid #ebebeb; font-size: 11px; }}
        tr:last-child td {{ border-bottom: none; }}
        tr:nth-child(even) {{ background: #fafafa; }}
        .totals-section {{ display: flex; justify-content: flex-end; margin-top: 20px; }}
        .totals-box {{ width: 280px; background: #f7f7f7; padding: 18px 20px; border-radius: 10px; }}
        .totals-row {{ display: flex; justify-content: space-between; padding: 6px 0; font-size: 12px; color: #252525; }}
        .totals-row.grand {{ border-top: 2px solid {primary_color}; margin-top: 8px; padding-top: 10px; font-size: 15px; font-weight: 700; }}
        .footer {{ padding: 20px 32px; border-top: 1px solid #ebebeb; background: #fafafa; }}
        .footer-company {{ display: flex; justify-content: center; gap: 24px; font-size: 10px; color: #8e8e8e; margin-bottom: 12px; text-align: center; flex-wrap: wrap; }}
        .footer-info {{ text-align: center; font-size: 11px; color: #8e8e8e; }}
        .footer-info strong {{ color: #252525; font-weight: 600; }}
        .footer-signature {{ text-align: center; margin-top: 16px; padding-top: 12px; border-top: 1px dashed #d1d5db; font-size: 11px; color: #8e8e8e; }}
        .btn {{ display: block; width: 200px; margin: 24px auto; background: {primary_color}; color: white; border: none; padding: 12px 20px; border-radius: 10px; cursor: pointer; font-size: 12px; font-weight: 600; text-align: center; }}
        .btn:hover {{ background: {primary_dark}; }}
        @media print {{ .btn {{ display: none; }} body {{ background: white; }} .page {{ box-shadow: none; }} }}
    </style>
</head>
<body>
    <div class="page">
        <div class="header">
            <div class="company-info">
                <div class="company-name">{company_name}</div>
                <div class="company-address">{company_address}<br>Tel: {company_phone}</div>
            </div>
            <div class="invoice-info">
                <div class="invoice-title">{_n(invoice_data.get('type'), 'facture') == 'fournisseur' and "FACTURE D'ACHAT" or "FACTURE"}</div>
                <div class="invoice-number">{invoice_data['invoice_number']}</div>
                <div class="meta-info">
                    <div>Date: {invoice_data.get('created_at', '')[:10] if invoice_data.get('created_at') else '-'}</div>
                    <div>Echeance: {_n(invoice_data.get('due_date'), '-')}</div>
                </div>
            </div>
        </div>
        
        <div class="content">
            <div class="parties">
                {party_html}
            </div>
            <div class="party-box" style="border-left-color: #8e8e8e;">
                    <div class="party-title" style="color: #8e8e8e;">Informations</div>
                    <div class="party-detail"><strong>Statut:</strong> {status_labels.get(_n(invoice_data.get('status'), 'payee'), 'Payee').upper()}</div>
                    <div class="party-detail"><strong>Paiement:</strong> {_n(invoice_data.get('payment_method'), 'cash').title()}</div>
                    <div class="party-detail"><strong>Date:</strong> {invoice_data.get('created_at', '')[:10] if invoice_data.get('created_at') else '-'}</div>
                </div>
            </div>
            
            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr>
                            <th>Designation</th>
                            <th>SKU</th>
                            <th style="text-align:center">Qte</th>
                            <th style="text-align:right">Prix Unit.</th>
                            <th style="text-align:right">Remise</th>
                            <th style="text-align:right">Total HT</th>
                        </tr>
                    </thead>
                    <tbody>"""
    
    for item in items_data:
        pdf_html += f"""
                        <tr>
                            <td>{_n(item.get('product_name'), 'Article')}</td>
                            <td>{_n(item.get('product_sku'), '-')}</td>
                            <td style="text-align:center">{item['quantity']}</td>
                            <td style="text-align:right">{item['unit_price']:.2f}</td>
                            <td style="text-align:right">{_n(item.get('discount_percent'), 0)}%</td>
                            <td style="text-align:right">{item['line_total']:.2f}</td>
                        </tr>"""
    
    pdf_html += f"""
                    </tbody>
                </table>
            </div>
            
            <div class="totals-section">
                <div class="totals-box">
                    <div class="totals-row"><span>Sous-total HT:</span><span>{_n(invoice_data.get('subtotal'), 0):.2f} DH</span></div>
                    <div class="totals-row"><span>Remises:</span><span style="color: #dc2626;">{_n(invoice_data.get('discount_total'), 0):.2f} DH</span></div>
                    <div class="totals-row"><span>TVA (20%):</span><span>{_n(invoice_data.get('tax_amount'), 0):.2f} DH</span></div>
                    <div class="totals-row grand"><span>TOTAL TTC:</span><span>{_n(invoice_data.get('total'), 0):.2f} DH</span></div>
                </div>
            </div>
        </div>
        
        <div class="footer">
            <div class="footer-company">
                <span>Patente N° {company_patente}</span>
                <span>RC N° {company_rc}</span>
                <span>ICE N° {company_ice}</span>
                <span>Identifiant Taxe N° {company_tax}</span>
            </div>
            <div class="footer-info">
                <strong>{company_name}</strong> - {company_address} - Tel: {company_phone}
            </div>
            <div class="footer-signature">
                <p>Merci pour votre confiance</p>
            </div>
        </div>
        
        <button class="btn" onclick="window.print()">Imprimer / Telcharger PDF</button>
    </div>
</body>
</html>"""
    
    conn.close()
    return Response(pdf_html, mimetype='text/html')

@app.route('/api/invoice-stats', methods=['GET'])
def get_invoice_stats():
    conn = get_db()
    
    total_invoices = conn.execute('SELECT COUNT(*) FROM invoices').fetchone()[0]
    total_amount = conn.execute('SELECT COALESCE(SUM(total), 0) FROM invoices WHERE status != "annulee"').fetchone()[0]
    paid_amount = conn.execute('SELECT COALESCE(SUM(total), 0) FROM invoices WHERE status = "payee"').fetchone()[0]
    pending_amount = conn.execute('SELECT COALESCE(SUM(total), 0) FROM invoices WHERE status IN ("brouillon", "envoyee")').fetchone()[0]
    
    conn.close()
    return jsonify({
        'total_invoices': total_invoices,
        'total_amount': round(total_amount, 2),
        'paid_amount': round(paid_amount, 2),
        'pending_amount': round(pending_amount, 2)
    })

@app.route('/manifest.json')
def serve_manifest():
    return send_from_directory('.', 'manifest.json')

@app.route('/api/receivables', methods=['GET'])
def get_receivables():
    """Liste détaillée des créances clients"""
    conn = get_db()
    
    receivables = conn.execute("""
        SELECT i.id, i.invoice_number, i.total as montant, i.due_date, i.created_at,
               c.id as customer_id, c.name as customer_name, c.client_code
        FROM invoices i
        JOIN customers c ON i.customer_id = c.id
        WHERE i.status = 'envoyee'
        ORDER BY i.due_date ASC
    """).fetchall()
    
    conn.close()
    return jsonify([dict(r) for r in receivables])

@app.route('/api/pos/sessions', methods=['GET'])
def get_pos_session():
    """Get active POS session"""
    conn = get_db()
    session = conn.execute('''
        SELECT * FROM pos_sessions 
        WHERE status = 'open' 
        ORDER BY opened_at DESC 
        LIMIT 1
    ''').fetchone()
    
    result = [dict(session)] if session else []
    conn.close()
    return jsonify(result)

@app.route('/api/pos/sessions', methods=['POST'])
def open_pos_session():
    """Open a new POS session"""
    data = request.json or {}
    warehouse_id = data.get('warehouse_id', 1)
    opening_cash = data.get('opening_cash', 0)
    
    conn = get_db()
    
    # Check for existing open session
    existing = conn.execute("SELECT id FROM pos_sessions WHERE status = 'open'").fetchone()
    if existing:
        conn.close()
        return jsonify({'error': 'Une session est déjà ouverte'}), 400
    
    # Generate session number
    today = datetime.now().strftime('%Y%m%d')
    last_session = conn.execute('''
        SELECT session_number FROM pos_sessions 
        WHERE session_number LIKE ? 
        ORDER BY id DESC LIMIT 1
    ''', (f'SES-{today}-%',)).fetchone()
    
    if last_session:
        seq = int(last_session[0].split('-')[-1]) + 1
    else:
        seq = 1
    session_number = f'SES-{today}-{seq:04d}'
    
    conn.execute('''
        INSERT INTO pos_sessions (session_number, warehouse_id, opening_cash, status)
        VALUES (?, ?, ?, 'open')
    ''', (session_number, warehouse_id, opening_cash))
    conn.commit()
    
    session_id = conn.execute('SELECT last_insert_rowid()').fetchone()[0]
    session = conn.execute('SELECT * FROM pos_sessions WHERE id = ?', (session_id,)).fetchone()
    
    conn.close()
    return jsonify({
        'success': True,
        'session': dict(session),
        'session_number': session_number
    })

@app.route('/api/pos/sessions/<int:session_id>/close', methods=['POST'])
def close_pos_session(session_id):
    """Close POS session and deposit to main account"""
    data = request.json or {}
    closing_cash = data.get('closing_cash', 0)
    deposit_to_main = data.get('deposit_to_main', True)
    
    conn = get_db()
    
    session = conn.execute('SELECT * FROM pos_sessions WHERE id = ?', (session_id,)).fetchone()
    if not session:
        conn.close()
        return jsonify({'error': 'Session non trouvée'}), 404
    
    if session['status'] != 'open':
        conn.close()
        return jsonify({'error': 'Session déjà fermé'}), 400
    
    # Calculate expected cash
    total_in = conn.execute('''
        SELECT COALESCE(SUM(amount), 0) FROM pos_cash_movements 
        WHERE session_id = ? AND type = 'in'
    ''', (session_id,)).fetchone()[0]
    
    total_out = conn.execute('''
        SELECT COALESCE(SUM(amount), 0) FROM pos_cash_movements 
        WHERE session_id = ? AND type = 'out'
    ''', (session_id,)).fetchone()[0]
    
    expected_cash = session['opening_cash'] + total_in - total_out
    
    conn.execute('''
        UPDATE pos_sessions 
        SET status = 'closed', closing_cash = ?, expected_cash = ?, closed_at = CURRENT_TIMESTAMP
        WHERE id = ?
    ''', (closing_cash, expected_cash, session_id))
    
    # Deposit closing cash to main account
    if deposit_to_main and expected_cash > 0:
        conn.execute('UPDATE main_account SET current_balance = current_balance + ? WHERE id = 1', (expected_cash,))
        conn.execute('''
            INSERT INTO main_account_transactions (type, amount, reason, reference_id, note)
            VALUES ('in', ?, 'session_close', ?, ?)
        ''', (expected_cash, session_id, 'Session: ' + session['session_number']))
    
    conn.commit()
    conn.close()
    
    return jsonify({'success': True, 'expected_cash': expected_cash, 'deposited': deposit_to_main and expected_cash > 0})

@app.route('/api/pos/transactions', methods=['POST'])
def create_pos_transaction():
    """Create a POS transaction and decrement stock"""
    data = request.json or {}
    session_id = data.get('session_id')
    customer_id = data.get('customer_id')
    items = data.get('items', [])
    payment_method = data.get('payment_method', 'cash')
    tendered_amount = data.get('tendered_amount', 0)
    notes = data.get('notes', '')
    
    if not session_id or not items:
        return jsonify({'error': 'Données invalides'}), 400
    
    conn = get_db()
    
    # Verify session is open
    session = conn.execute('SELECT * FROM pos_sessions WHERE id = ?', (session_id,)).fetchone()
    if not session or session['status'] != 'open':
        conn.close()
        return jsonify({'error': 'Session fermée ou inexistante'}), 400
    
    # Determine transaction type based on customer
    today = datetime.now().strftime('%Y%m%d')
    is_client_comptoir = customer_id is None or customer_id == '' or customer_id == 'null'
    
    if is_client_comptoir:
        seq = next_sequence(conn, 'ticket_counter')
        doc_number = f'Ticket-{today}-{seq:04d}'
        doc_type = 'ticket'
    else:
        seq = next_sequence(conn, 'fac_counter')
        doc_number = f'FAC-{today}-{seq:04d}'
        doc_type = 'facture'
    
    # Load customer for pricing
    customer_row = None
    if customer_id and not is_client_comptoir:
        cid = _safe_int(customer_id, None)
        if cid:
            customer_row = conn.execute('SELECT * FROM customers WHERE id = ?', (cid,)).fetchone()
    
    # Calculate totals
    subtotal = 0
    tax = 0
    discount = 0
    for item in items:
        if customer_row:
            product = conn.execute('SELECT * FROM products WHERE id = ?', (item['product_id'],)).fetchone()
            if product:
                item['unit_price'] = get_price_for_customer(dict(product), customer_row['type'], customer_row['is_loyal'])
        unit_price = item.get('unit_price', 0) or 0
        item['unit_price'] = unit_price
        line_ht = item['quantity'] * unit_price
        line_tax = line_ht * 0.20
        subtotal += line_ht
        tax += line_tax
        discount += line_ht * (item.get('discount_percent', 0) / 100)
    
    total = subtotal + tax - discount
    change_amount = max(0, tendered_amount - total) if payment_method == 'cash' else 0
    
    # Insert transaction
    conn.execute('''
        INSERT INTO pos_transactions (
            ticket_number, transaction_number, session_id, customer_id, payment_method,
            subtotal, discount_total, tax_amount, total,
            tendered_amount, change_given, status
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'completed')
    ''', (doc_number, doc_number, session_id, customer_id, payment_method,
          subtotal, discount, tax, total, tendered_amount, change_amount))
    trans_id = conn.execute('SELECT last_insert_rowid()').fetchone()[0]
    
    # Insert items and decrement stock
    for item in items:
        product_id = item.get('product_id') or item.get('id')
        qty = item.get('quantity', 1)
        unit_price = item.get('unit_price', 0) or 0
        discount_pct = item.get('discount_percent', 0) or 0
        line_ht = qty * unit_price * (1 - discount_pct / 100)
        line_total = line_ht * 1.20
        
        conn.execute('''
            INSERT INTO pos_transaction_items (
                transaction_id, product_id, product_name, product_sku,
                quantity, unit_price, discount_percent, tax_rate, line_total
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', (trans_id, product_id, item.get('product_name', ''), item.get('product_sku', ''),
              qty, unit_price, discount_pct, 20, line_total))
        
        cur = conn.execute('''
            UPDATE products SET quantity = quantity - ? WHERE id = ? AND quantity >= ?
        ''', (qty, product_id, qty))
        if cur.rowcount == 0:
            current = conn.execute('SELECT quantity FROM products WHERE id = ?', (product_id,)).fetchone()
            stock_qty = current['quantity'] if current else 0
            conn.close()
            return jsonify({'error': f'Stock insuffisant pour {item.get("product_name", "produit")}. Disponible: {stock_qty}, demandé: {qty}'}), 400
        
        conn.execute('''
            INSERT INTO stock_movements (product_id, type, quantity, note)
            VALUES (?, 'sale', ?, ?)
        ''', (product_id, qty, f'Vente POS: {doc_number}'))
    
    # Record cash movement if cash payment
    if payment_method == 'cash' and tendered_amount > 0:
        conn.execute('''
            INSERT INTO pos_cash_movements (session_id, type, amount, reason, note)
            VALUES (?, 'in', ?, 'sale', ?)
        ''', (session_id, total, doc_number))
        
        if change_amount > 0:
            conn.execute('''
                INSERT INTO pos_cash_movements (session_id, type, amount, reason, note)
                VALUES (?, 'out', ?, 'change', ?)
            ''', (session_id, change_amount, f'Monnaie rendu: {doc_number}'))
    
    # Record card payment in main account
    if payment_method == 'card':
        conn.execute('UPDATE main_account SET current_balance = current_balance + ? WHERE id = 1', (total,))
        conn.execute('''
            INSERT INTO main_account_transactions (type, amount, reason, reference_id, note)
            VALUES ('in', ?, 'card_payment', ?, ?)
        ''', (total, trans_id, f'Paiement carte: {doc_number}'))
    
    conn.commit()
    
    # Get customer name
    customer_name = 'Client Comptoir'
    if customer_id and not is_client_comptoir:
        cust = conn.execute('SELECT name FROM customers WHERE id = ?', (customer_id,)).fetchone()
        if cust:
            customer_name = cust['name']
    
    # Create invoice record for every POS sale
    inv_type = 'ticket' if is_client_comptoir else 'facture'
    conn.execute('''
        INSERT INTO invoices (
            invoice_number, customer_id, warehouse_id, status, type,
            subtotal, discount_total, tax_amount, total, 
            paid_at, payment_method, tendered_amount, change_given
        ) VALUES (?, ?, 1, 'payee', ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?)
    ''', (doc_number, customer_id, inv_type, subtotal, discount, tax, total, 
          payment_method, tendered_amount, change_amount))
    inv_id = conn.execute('SELECT last_insert_rowid()').fetchone()[0]
    
    conn.execute('UPDATE pos_transactions SET invoice_id = ? WHERE id = ?', (inv_id, trans_id))
    
    for item in items:
        qty = item.get('quantity', 1)
        uprice = item.get('unit_price', 0) or 0
        dpct = item.get('discount_percent', 0) or 0
        line_ht = qty * uprice * (1 - dpct / 100)
        line_total = line_ht * 1.20
        conn.execute('''
            INSERT INTO invoice_items (
                invoice_id, product_id, product_name, product_sku,
                quantity, unit_price, discount_percent, tax_rate, line_total
            ) VALUES (?, ?, ?, ?, ?, ?, ?, 20, ?)
        ''', (inv_id, item.get('product_id') or item.get('id'), item.get('product_name', ''), item.get('product_sku', ''),
              qty, uprice, dpct, line_total))
    
    conn.commit()
    conn.close()
    
    return jsonify({
        'success': True,
        'document_number': doc_number,
        'document_id': inv_id,
        'document_type': doc_type,
        'total': total,
        'change_amount': change_amount,
        'customer_name': customer_name
    })

@app.route('/api/pos/cash-movements', methods=['GET'])
def get_pos_cash_movements():
    """Get cash movements for current session"""
    conn = get_db()
    
    session = conn.execute("SELECT id FROM pos_sessions WHERE status = 'open' ORDER BY opened_at DESC LIMIT 1").fetchone()
    if not session:
        conn.close()
        return jsonify([])
    
    movements = conn.execute('''
        SELECT * FROM pos_cash_movements 
        WHERE session_id = ?
        ORDER BY created_at DESC
    ''', (session['id'],)).fetchall()
    
    conn.close()
    return jsonify([dict(m) for m in movements])

@app.route('/api/pos/cash-movements', methods=['POST'])
def create_pos_cash_movement():
    """Add cash movement (in/out)"""
    data = request.json or {}
    movement_type = data.get('type')  # 'in' or 'out'
    amount = data.get('amount', 0)
    reason = data.get('reason', '')
    note = data.get('note', '')
    
    conn = get_db()
    
    session = conn.execute("SELECT id FROM pos_sessions WHERE status = 'open' ORDER BY opened_at DESC LIMIT 1").fetchone()
    if not session:
        conn.close()
        return jsonify({'error': 'Aucune session ouverte'}), 400
    
    conn.execute('''
        INSERT INTO pos_cash_movements (session_id, type, amount, reason, note)
        VALUES (?, ?, ?, ?, ?)
    ''', (session['id'], movement_type, amount, reason, note))
    conn.commit()
    
    movement_id = conn.execute('SELECT last_insert_rowid()').fetchone()[0]
    movement = conn.execute('SELECT * FROM pos_cash_movements WHERE id = ?', (movement_id,)).fetchone()
    
    conn.close()
    return jsonify({'success': True, 'movement': dict(movement)})

@app.route('/api/pos/customers', methods=['GET'])
def get_pos_customers():
    """Get customers for POS (with discount info)"""
    conn = get_db()
    customers = conn.execute('''
        SELECT id, name, client_code, discount_rate
        FROM customers 
        WHERE is_active = 1
        ORDER BY name
    ''').fetchall()
    conn.close()
    return jsonify([dict(c) for c in customers])

@app.route('/api/pos/transactions/recent', methods=['GET'])
def get_pos_recent_transactions():
    """Get recent POS transactions"""
    conn = get_db()
    limit = _safe_int(request.args.get('limit', 20), 20)
    session_id = request.args.get('session_id')
    
    if session_id:
        transactions = conn.execute('''
            SELECT t.*, c.name as customer_name
            FROM pos_transactions t
            LEFT JOIN customers c ON t.customer_id = c.id
            WHERE t.session_id = ?
            ORDER BY t.created_at DESC
            LIMIT ?
        ''', (session_id, limit)).fetchall()
    else:
        transactions = conn.execute('''
            SELECT t.*, c.name as customer_name
            FROM pos_transactions t
            LEFT JOIN customers c ON t.customer_id = c.id
            ORDER BY t.created_at DESC
            LIMIT ?
        ''', (limit,)).fetchall()
    
    conn.close()
    return jsonify([dict(t) for t in transactions])

@app.route('/api/pos/best-sellers', methods=['GET'])
def get_pos_best_sellers():
    """Get best selling products for POS"""
    conn = get_db()
    limit = _safe_int(request.args.get('limit', 5), 5)
    
    products = conn.execute('''
        SELECT p.id, p.name, p.sku, p.price, p.price_base, 
               COALESCE(SUM(ti.quantity), 0) as total_sold
        FROM products p
        LEFT JOIN pos_transaction_items ti ON p.id = ti.product_id
        WHERE p.is_deleted = 0
        GROUP BY p.id
        ORDER BY total_sold DESC
        LIMIT ?
    ''', (limit,)).fetchall()
    
    conn.close()
    return jsonify([dict(p) for p in products])

# ============= MAIN ACCOUNT API =============
@app.route('/api/main-account', methods=['GET'])
def get_main_account():
    """Get main account info and transactions"""
    conn = get_db()
    
    account = conn.execute('SELECT * FROM main_account WHERE id = 1').fetchone()
    if not account:
        conn.close()
        return jsonify({'error': 'Compte principal non trouvé'}), 404
    
    limit = _safe_int(request.args.get('limit', 50), 50)
    transactions = conn.execute('''
        SELECT * FROM main_account_transactions 
        ORDER BY created_at DESC
        LIMIT ?
    ''', (limit,)).fetchall()
    
    conn.close()
    return jsonify({
        'account': dict(account),
        'transactions': [dict(t) for t in transactions]
    })

@app.route('/api/main-account/deposit', methods=['POST'])
def deposit_to_main_account():
    """Deposit money to main account"""
    data = request.json or {}
    amount = data.get('amount', 0)
    reason = data.get('reason', 'deposit')
    reference_id = data.get('reference_id')
    note = data.get('note', '')
    
    if amount <= 0:
        return jsonify({'error': 'Montant invalide'}), 400
    
    conn = get_db()
    
    conn.execute('''
        UPDATE main_account SET current_balance = current_balance + ? WHERE id = 1
    ''', (amount,))
    
    conn.execute('''
        INSERT INTO main_account_transactions (type, amount, reason, reference_id, note)
        VALUES ('in', ?, ?, ?, ?)
    ''', (amount, reason, reference_id, note))
    
    conn.commit()
    
    account = conn.execute('SELECT * FROM main_account WHERE id = 1').fetchone()
    conn.close()
    
    return jsonify({'success': True, 'account': dict(account)})

@app.route('/api/main-account/withdraw', methods=['POST'])
def withdraw_from_main_account():
    """Withdraw money from main account"""
    data = request.json or {}
    amount = data.get('amount', 0)
    reason = data.get('reason', 'withdraw')
    reference_id = data.get('reference_id')
    note = data.get('note', '')
    
    if amount <= 0:
        return jsonify({'error': 'Montant invalide'}), 400
    
    conn = get_db()
    
    account = conn.execute('SELECT current_balance FROM main_account WHERE id = 1').fetchone()
    if account and account['current_balance'] < amount:
        conn.close()
        return jsonify({'error': 'Solde insuffisant'}), 400
    
    conn.execute('''
        UPDATE main_account SET current_balance = current_balance - ? WHERE id = 1
    ''', (amount,))
    
    conn.execute('''
        INSERT INTO main_account_transactions (type, amount, reason, reference_id, note)
        VALUES ('out', ?, ?, ?, ?)
    ''', (amount, reason, reference_id, note))
    
    conn.commit()
    
    account = conn.execute('SELECT * FROM main_account WHERE id = 1').fetchone()
    conn.close()
    
    return jsonify({'success': True, 'account': dict(account)})

@app.route('/api/main-account/transfer-to-pos', methods=['POST'])
def transfer_to_pos_session():
    """Transfer money from main account to POS session"""
    data = request.json or {}
    amount = data.get('amount', 0)
    note = data.get('note', '')
    
    if amount <= 0:
        return jsonify({'error': 'Montant invalide'}), 400
    
    conn = get_db()
    
    account = conn.execute('SELECT current_balance FROM main_account WHERE id = 1').fetchone()
    if account and account['current_balance'] < amount:
        conn.close()
        return jsonify({'error': 'Solde insuffisant'}), 400
    
    session = conn.execute("SELECT id FROM pos_sessions WHERE status = 'open' ORDER BY opened_at DESC LIMIT 1").fetchone()
    if not session:
        conn.close()
        return jsonify({'error': 'Aucune session POS ouverte'}), 400
    
    conn.execute('UPDATE main_account SET current_balance = current_balance - ? WHERE id = 1', (amount,))
    
    conn.execute('''
        INSERT INTO main_account_transactions (type, amount, reason, reference_id, note)
        VALUES ('out', ?, 'transfer_pos', ?, ?)
    ''', (amount, session['id'], note))
    
    conn.execute('''
        INSERT INTO pos_cash_movements (session_id, type, amount, reason, note, source)
        VALUES (?, 'in', ?, 'transfer_from_main', ?, 'main_account')
    ''', (session['id'], amount, note))
    
    conn.commit()
    
    account = conn.execute('SELECT * FROM main_account WHERE id = 1').fetchone()
    conn.close()
    
    return jsonify({'success': True, 'account': dict(account)})

@app.route('/api/pos/tickets/<ticket_number>', methods=['GET'])
def generate_pos_ticket_pdf(ticket_number):
    """Generate ticket receipt PDF for client comptoir sales"""
    conn = get_db()
    
    # Get transaction
    transaction = conn.execute('''
        SELECT t.*, s.session_number, s.opened_at
        FROM pos_transactions t
        LEFT JOIN pos_sessions s ON t.session_id = s.id
        WHERE t.transaction_number = ?
    ''', (ticket_number,)).fetchone()
    
    if not transaction:
        conn.close()
        return jsonify({'error': 'Ticket non trouve'}), 404
    
    transaction = dict(transaction)
    
    # Get items
    items = conn.execute('SELECT * FROM pos_transaction_items WHERE transaction_id = ?', 
                       (transaction['id'],)).fetchall()
    items = [dict(item) for item in items]
    
    def _esc(d):
        for k, v in d.items():
            if v is None:
                d[k] = ''
            elif isinstance(v, str):
                d[k] = html.escape(v)
    def _n(val, default='-'):
        return val if val else default
    _esc(transaction)
    for item in items:
        _esc(item)
    
    conn.close()
    
    # Generate ticket HTML (receipt style, not invoice)
    ticket_html = f"""<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Recu - {ticket_number}</title>
    <style>
        * {{ margin: 0; padding: 0; box-sizing: border-box; }}
        body {{ font-family: 'Courier New', monospace; font-size: 11px; width: 280px; margin: 0 auto; padding: 10px; color: #333; }}
        .header {{ text-align: center; margin-bottom: 15px; border-bottom: 1px dashed #333; padding-bottom: 10px; }}
        .company {{ font-size: 14px; font-weight: bold; margin-bottom: 5px; }}
        .address {{ font-size: 9px; color: #666; }}
        .ticket-title {{ font-size: 16px; font-weight: bold; margin: 10px 0; }}
        .ticket-number {{ font-size: 12px; border: 1px solid #333; padding: 4px 8px; display: inline-block; margin-bottom: 10px; }}
        .info {{ font-size: 9px; margin: 3px 0; }}
        table {{ width: 100%; border-collapse: collapse; margin: 10px 0; font-size: 10px; }}
        th {{ text-align: left; border-bottom: 1px dashed #333; padding: 4px 0; }}
        td {{ padding: 3px 0; }}
        .item-name {{ max-width: 150px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }}
        .qty {{ width: 30px; text-align: center; }}
        .price {{ width: 60px; text-align: right; }}
        .total {{ width: 60px; text-align: right; font-weight: bold; }}
        .divider {{ border-bottom: 1px dashed #333; margin: 8px 0; }}
        .totals {{ margin-top: 10px; }}
        .totals-row {{ display: flex; justify-content: space-between; padding: 2px 0; }}
        .grand-total {{ font-size: 14px; font-weight: bold; border-top: 1px dashed #333; padding-top: 5px; margin-top: 5px; }}
        .payment {{ margin-top: 10px; font-size: 10px; }}
        .footer {{ text-align: center; margin-top: 20px; border-top: 1px dashed #333; padding-top: 10px; }}
        .signature {{ font-size: 10px; margin-top: 15px; }}
        .btn {{ background: #333; color: white; border: none; padding: 8px 16px; cursor: pointer; font-size: 11px; margin-top: 15px; }}
        @media print {{ .btn {{ display: none; }} }}
    </style>
</head>
<body>
    <div class="header">
        <div class="company">Bibliotheque Badr</div>
        <div class="address">Rue Mohammed V, Gueliz</div>
        <div class="address">Marrakech, Maroc</div>
    </div>
    
    <div class="ticket-title">RECU DE CAISSE</div>
    <div class="ticket-number">{ticket_number}</div>
    
    <div class="info">Date: {transaction.get('created_at', '')[:16] if transaction.get('created_at') else '-'}</div>
    <div class="info">Session: {transaction.get('session_number', '-')}</div>
    
    <table>
        <thead>
            <tr>
                <th>Article</th>
                <th class="qty">Qte</th>
                <th class="price">Prix</th>
                <th class="total">Total</th>
            </tr>
        </thead>
        <tbody>"""
    
    for item in items:
        ticket_html += f"""
            <tr>
                <td class="item-name">{_n(item.get('product_name'), 'Article')}</td>
                <td class="qty">{item['quantity']}</td>
                <td class="price">{item['unit_price']:.2f}</td>
                <td class="total">{item['line_total']:.2f}</td>
            </tr>"""
    
    ticket_html += f"""        </tbody>
    </table>
    
    <div class="divider"></div>
    
    <div class="totals">
        <div class="totals-row"><span>Sous-total:</span><span>{_n(transaction.get('subtotal'), 0):.2f} DH</span></div>
        {f'<div class="totals-row"><span>Remise:</span><span>{_n(transaction.get("discount_total"), 0):.2f} DH</span></div>' if _n(transaction.get('discount_total'), 0) > 0 else ''}
        <div class="totals-row"><span>TVA (20%):</span><span>{_n(transaction.get('tax_amount'), 0):.2f} DH</span></div>
        <div class="totals-row grand-total"><span>TOTAL:</span><span>{_n(transaction.get('total'), 0):.2f} DH</span></div>
    </div>
    
    <div class="payment">
        <div class="divider"></div>
        <div class="totals-row"><span>Paiement:</span><span>{_n(transaction.get('payment_method'), 'cash').title()}</span></div>
        <div class="totals-row"><span>Recu:</span><span>{_n(transaction.get('tendered_amount'), 0):.2f} DH</span></div>
        <div class="totals-row"><span>Monnaie:</span><span>{_n(transaction.get('change_given'), 0):.2f} DH</span></div>
    </div>
    
    <div class="footer">
        <p>Merci pour votre visite!</p>
        <div class="signature">
            <p>---------------------------</p>
            <p>Signature Caissier</p>
        </div>
    </div>
    
    <button class="btn" onclick="window.print()">Imprimer / PDF</button>
</body>
</html>"""
    
    return Response(ticket_html, mimetype='text/html')

@app.route('/api/pos/transaction-by-invoice/<invoice_number>', methods=['GET'])
def get_pos_transaction_by_invoice(invoice_number):
    """Get POS transaction by invoice number"""
    conn = get_db()
    
    transaction = conn.execute('''
        SELECT t.*, s.session_number
        FROM pos_transactions t
        LEFT JOIN pos_sessions s ON t.session_id = s.id
        WHERE t.transaction_number = ? OR t.invoice_id IN (
            SELECT id FROM invoices WHERE invoice_number = ?
        )
    ''', (invoice_number, invoice_number)).fetchone()
    
    if not transaction:
        conn.close()
        return jsonify({'error': 'Transaction non trouvee'}), 404
    
    conn.close()
    return jsonify(dict(transaction))

if __name__ == '__main__':
    init_db()
    app.run(debug=False, port=5001, threaded=True)
