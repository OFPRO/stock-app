import sqlite3
import csv
import random
import html
import os
import sys
import json
import uuid
import queue
import secrets
import argparse
import platform
import webbrowser
from io import StringIO
from datetime import datetime, timedelta, timezone
from flask import Flask, redirect, render_template, request, jsonify, Response, send_from_directory, session
from extensions import limiter
from license_manager import get_mac_address, get_cached_payload, sign_license, validate_license, load_license, save_license
from routes.db import get_db, get_catalog_db, get_db_ctx, get_catalog_db_ctx, get_price_by_tier, DB_NAME, CATALOG_DB, STOCKPRO_DATA_DIR, _safe_int, validate_id, categories_data, resolve_db_path
try:
    from reset_test_db import reset_transactional_data, reset_products_data, reset_products_qty
    _HAS_RESET = True
except ImportError:
    _HAS_RESET = False
from werkzeug.security import generate_password_hash, check_password_hash
from routes.products import products_bp
from routes.kpis import kpis_bp
from routes.customers import customers_bp
from routes.suppliers import suppliers_bp
from routes.warehouses import warehouses_bp
from routes.locations import locations_bp
from routes.stores import stores_bp
from routes.reports import reports_bp
from routes.backup import backup_bp
import threading
from services.printing_service import auto_print_async

# SSE event bus for real-time multi-caisse sync
sse_clients = []

def broadcast_event(event_type, data):
    payload = f"event: {event_type}\ndata: {json.dumps(data)}\n\n"
    dead_clients = []
    for client_queue in sse_clients:
        try:
            client_queue.put_nowait(payload)
        except Exception:
            dead_clients.append(client_queue)
    for q in dead_clients:
        sse_clients.remove(q)


app = Flask(__name__)
_secret_file = os.path.join(STOCKPRO_DATA_DIR, '.secret_key')
if os.environ.get('SECRET_KEY'):
    app.secret_key = os.environ['SECRET_KEY']
elif os.path.exists(_secret_file):
    with open(_secret_file, 'r') as f:
        app.secret_key = f.read().strip()
else:
    app.secret_key = os.urandom(24).hex()
    with open(_secret_file, 'w') as f:
        f.write(app.secret_key)
    os.chmod(_secret_file, 0o600)
app.config['SESSION_COOKIE_SAMESITE'] = 'Lax'
app.config['SESSION_COOKIE_HTTPONLY'] = True
app.config['MAX_CONTENT_LENGTH'] = 5 * 1024 * 1024  # 5MB max upload
limiter.init_app(app)
app.register_blueprint(products_bp)
app.register_blueprint(kpis_bp)
app.register_blueprint(customers_bp)
app.register_blueprint(suppliers_bp)
app.register_blueprint(warehouses_bp)
app.register_blueprint(locations_bp)
app.register_blueprint(stores_bp)
app.register_blueprint(reports_bp)
app.register_blueprint(backup_bp)

UPLOAD_FOLDER = os.path.join(app.static_folder, 'uploads')
os.makedirs(UPLOAD_FOLDER, exist_ok=True)
ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'webp'}


def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS


@app.route('/api/upload', methods=['POST'])
def upload_file():
    if 'file' not in request.files:
        return jsonify({'error': 'Aucun fichier fourni'}), 400
    file = request.files['file']
    if file.filename == '':
        return jsonify({'error': 'Fichier vide'}), 400
    if not allowed_file(file.filename):
        return jsonify({'error': 'Format non autorisé (png, jpg, jpeg, gif, webp)'}), 400
    ext = file.filename.rsplit('.', 1)[1].lower()
    filename = f"{uuid.uuid4().hex}.{ext}"
    file.save(os.path.join(UPLOAD_FOLDER, filename))
    return jsonify({'url': f'/static/uploads/{filename}'})


@app.before_request
def ensure_active_store():
    if 'active_store_id' not in session:
        session['active_store_id'] = 1

@app.before_request
def license_check():
    if platform.system() != 'Windows':
        return
    if request.path.startswith('/static/') or request.path.startswith('/api/license') or request.path == '/license':
        return
    if get_cached_payload() is None:
        if request.path == '/':
            return
        return jsonify({'error': 'Licence requise'}), 403

@app.after_request
def set_security_headers(response):
    response.headers['X-Content-Type-Options'] = 'nosniff'
    response.headers['X-Frame-Options'] = 'DENY'
    response.headers['X-XSS-Protection'] = '1; mode=block'
    response.headers['Referrer-Policy'] = 'strict-origin-when-cross-origin'
    if request.path.startswith('/api/'):
        response.headers['Cache-Control'] = 'no-store, no-cache, must-revalidate'
    return response

@app.before_request
def csrf_protect():
    if request.method in ('GET', 'HEAD', 'OPTIONS'):
        return
    if request.path.startswith('/static/'):
        return
    if request.path.startswith('/api/auth/'):
        return
    if request.path.startswith('/api/') and session.get('csrf_token'):
        token = request.headers.get('X-CSRF-Token') or (request.get_json(silent=True) or {}).get('_csrf_token')
        if not token or token != session.get('csrf_token'):
            return jsonify({'error': 'Token CSRF invalide'}), 403

@app.route('/api/csrf-token', methods=['GET'])
def get_csrf_token():
    if 'csrf_token' not in session:
        session['csrf_token'] = secrets.token_hex(32)
    return jsonify({'csrf_token': session['csrf_token']})

@app.before_request
def pin_protect():
    return

@app.route('/api/auth/pin-status', methods=['GET'])
def pin_status():
    db = get_catalog_db()
    row = db.execute("SELECT value FROM settings WHERE key='app_pin'").fetchone()
    db.close()
    return jsonify({'pin_set': row is not None})

@app.route('/api/auth/verify', methods=['POST'])
@limiter.limit("5 per minute")
def pin_verify():
    data = request.get_json(silent=True) or {}
    pin = data.get('pin', '')
    db = get_catalog_db()
    row = db.execute("SELECT value FROM settings WHERE key='app_pin'").fetchone()
    db.close()
    if not row:
        return jsonify({'error': 'Aucun PIN configuré'}), 400
    if not check_password_hash(row[0], pin):
        return jsonify({'error': 'Code PIN incorrect'}), 401
    session['pin_verified'] = True
    return jsonify({'success': True})

@app.route('/api/auth/change-pin', methods=['POST'])
@limiter.limit("3 per minute")
def pin_change():
    if not session.get('pin_verified'):
        return jsonify({'error': 'Authentification requise'}), 401
    data = request.get_json(silent=True) or {}
    current = data.get('current_pin', '')
    new_pin = data.get('new_pin', '')
    if len(new_pin) < 6:
        return jsonify({'error': 'Le PIN doit faire au moins 6 chiffres'}), 400
    db = get_catalog_db()
    row = db.execute("SELECT value FROM settings WHERE key='app_pin'").fetchone()
    if not row or not check_password_hash(row[0], current):
        db.close()
        return jsonify({'error': 'Code PIN actuel incorrect'}), 401
    db.execute("UPDATE settings SET value=? WHERE key='app_pin'", (generate_password_hash(new_pin),))
    db.commit()
    db.close()
    return jsonify({'success': True})

def _esc(d):
    for k, v in d.items():
        if v is None:
            d[k] = ''
        elif isinstance(v, str):
            d[k] = html.escape(v)

def _safe_err(e, fallback='Erreur interne du serveur'):
    import logging
    logging.getLogger('stockpro').exception('API error')
    if isinstance(e, ValueError):
        return str(e)
    return fallback

def _n(val, default='-'):
    return val if val else default

from services.pdf_utils import _arabic_reshape

_reshape = _arabic_reshape

def next_sequence(conn, name):
    conn.execute('SAVEPOINT seq')
    conn.execute('INSERT OR IGNORE INTO sequences (name, current_value) VALUES (?, 0)', (name,))
    conn.execute('UPDATE sequences SET current_value = current_value + 1 WHERE name = ?', (name,))
    result = conn.execute('SELECT current_value FROM sequences WHERE name = ?', (name,)).fetchone()[0]
    conn.execute('RELEASE seq')
    return result

def init_db():
    conn = get_catalog_db()
    conn.execute("PRAGMA foreign_keys=OFF")
    c = conn.cursor()

    c.execute('''
        CREATE TABLE IF NOT EXISTS stores (
            id INTEGER PRIMARY KEY,
            name TEXT NOT NULL,
            code TEXT NOT NULL UNIQUE,
            is_active INTEGER DEFAULT 1,
            is_archived INTEGER DEFAULT 0,
            archived_at TIMESTAMP,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    c.execute("INSERT OR IGNORE INTO stores (id, name, code) VALUES (1, 'Papeterie AlQalam', '1')")

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
        c.execute('ALTER TABLE products ADD COLUMN price_gros REAL DEFAULT 0')
    except Exception:
        pass
    try:
        c.execute("ALTER TABLE products ADD COLUMN tax_category TEXT DEFAULT '20'")
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
    try:
        c.execute("ALTER TABLE products ADD COLUMN extra_prices TEXT DEFAULT '[]'")
    except Exception:
        pass
    try:
        c.execute("ALTER TABLE products ADD COLUMN image_url TEXT DEFAULT NULL")
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
        CREATE TABLE IF NOT EXISTS categories (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name_ar TEXT NOT NULL,
            name_fr TEXT NOT NULL UNIQUE
        )
    ''')

    # 1. Migrate products to final category names before refreshing categories
    c.execute("UPDATE products SET category='Cartables' WHERE category='Trousses' AND is_deleted=0")
    c.execute("UPDATE products SET category='Trousses' WHERE category='Carnets' AND is_deleted=0")
    c.execute("UPDATE products SET category='Launch Box' WHERE category='Pochettes classeurs' AND is_deleted=0")
    c.execute("UPDATE products SET category='Calculatrices' WHERE category='Fournitures calcul' AND is_deleted=0")
    c.execute("UPDATE products SET category='Stylos à bille/Crayons' WHERE category='Stylos correcteurs crayons' AND is_deleted=0")
    c.execute("UPDATE products SET category='Dessins/peinture' WHERE category='Dessin arts plastiques' AND is_deleted=0")

    # 2. Ensure default categories exist without wiping user-added ones
    for ar, fr in categories_data:
        c.execute("INSERT OR IGNORE INTO categories (name_ar, name_fr) VALUES (?, ?)", (ar, fr))

    mapping = {
        'Accessoires': 'Fournitures bureau',
        'Bureautique': 'Fournitures bureau',
        'Câbles': 'Accessoires téléphone',
        'Fournitures': 'Fournitures bureau',
        'Général': 'Cadeaux adultes',
        'Informatique': 'Fournitures bureau',
        'Papeterie': 'Fournitures scolaires',
        'Éclairage': 'Fournitures bureau',
        'Bureautique ': 'Fournitures bureau',
    }
    for old_cat, new_cat in mapping.items():
        c.execute('UPDATE products SET category = ? WHERE category = ?', (new_cat, old_cat))
    # Specific product overrides
    c.execute("UPDATE products SET category = 'Calculatrices' WHERE name IN ('Calculatrice scientifique', 'Compas géométrie', 'Equerre plastique', 'Rapporteur 180°')")
    c.execute("UPDATE products SET category = 'Trousses' WHERE name IN ('Livre briquet 100p', 'livret 30p black', 'Livre camel 30p')")
    c.execute("UPDATE products SET category = 'Cartables' WHERE name = 'Trousse scolaire'")
    c.execute("UPDATE products SET category = 'Cahiers' WHERE name LIKE 'Cahier%'")
    c.execute("UPDATE products SET category = 'Notebooks agendas' WHERE name = 'Bloc-notes A5'")
    c.execute("UPDATE products SET category = 'Cadeaux adultes' WHERE name IN ('Deep Darkwood', 'Bouteil Eau Aquafina', 'Badges magnétiques')")
    c.execute("UPDATE products SET category = 'Classeurs chemises papier' WHERE name IN ('Classeur rigide A4', 'Classeur souple A4', 'Feuilles perforées A4 (500)')")
    c.execute("UPDATE products SET category = 'Stylos à bille/Crayons' WHERE name = 'Crayon à papier HB'")
    c.execute("UPDATE products SET category = 'Blanco surligneurs' WHERE name IN ('Marqueurs fluorescents (lot 6)', 'Stylo fluorescent vert')")
    c.execute("UPDATE products SET category = 'Accessoires téléphone' WHERE name IN ('Enceinte Bluetooth', 'Câble HDMI 2m', 'Câble HDMI 5m', 'Câble VGA 2m')")

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
    try:
        c.execute("ALTER TABLE purchase_orders ADD COLUMN paid_at TIMESTAMP")
    except:
        pass
    
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
    try:
        c.execute('ALTER TABLE invoices ADD COLUMN amount_paid REAL DEFAULT 0')
    except Exception:
        pass
    try:
        c.execute("UPDATE invoices SET amount_paid = total WHERE status IN ('payee', 'ticket') AND amount_paid = 0")
    except Exception:
        pass
    try:
        c.execute('ALTER TABLE invoices ADD COLUMN is_credit_payment INTEGER DEFAULT 0')
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

    try:
        c.execute("ALTER TABLE pos_sessions ADD COLUMN register_id INTEGER REFERENCES pos_registers(id)")
    except Exception:
        pass
    try:
        c.execute("ALTER TABLE pos_sessions ADD COLUMN cashier_name TEXT DEFAULT ''")
    except Exception:
        pass

    try:
        c.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_pos_sessions_open ON pos_sessions(register_id) WHERE status = 'open'")
    except Exception:
        pass
    try:
        c.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_pos_sessions_open_global ON pos_sessions(status) WHERE status = 'open' AND register_id IS NULL")
    except Exception:
        pass

    c.execute("UPDATE pos_registers SET is_active = 0 WHERE name NOT IN ('Caisse 1', 'Caisse 2')")
    c.execute("INSERT OR IGNORE INTO pos_registers (name, code) VALUES ('Caisse 1', 'CAISSE-01')")
    c.execute("INSERT OR IGNORE INTO pos_registers (name, code) VALUES ('Caisse 2', 'CAISSE-02')")
    c.execute("UPDATE pos_registers SET is_active = 1 WHERE name IN ('Caisse 1', 'Caisse 2')")

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
            VALUES (1, 'Compte Principal', 0, 0)
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
    
    c.execute('''
        CREATE TABLE IF NOT EXISTS _schema_version (
            version INTEGER PRIMARY KEY,
            applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    current = c.execute('SELECT MAX(version) FROM _schema_version').fetchone()[0]
    if not current:
        c.execute('INSERT INTO _schema_version (version) VALUES (1)')

    c.execute('''
        CREATE TABLE IF NOT EXISTS printer_settings (
            id INTEGER PRIMARY KEY CHECK (id = 1),
            connection_type TEXT DEFAULT 'network',
            host TEXT DEFAULT '',
            port INTEGER DEFAULT 9100,
            usb_vendor_id TEXT DEFAULT '',
            usb_product_id TEXT DEFAULT '',
            printer_name TEXT DEFAULT '',
            auto_print INTEGER DEFAULT 1,
            paper_width INTEGER DEFAULT 80,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    c.execute("INSERT OR IGNORE INTO printer_settings (id, connection_type, host) VALUES (1, 'network', '')")

    c.execute('''
        CREATE TABLE IF NOT EXISTS settings (
            key TEXT PRIMARY KEY,
            value TEXT NOT NULL
        )
    ''')
    c.execute("SELECT COUNT(*) FROM settings")
    if c.fetchone()[0] == 0:
        import secrets
        default_pw = secrets.token_urlsafe(16)
        app_pin = f'{secrets.randbelow(1000000):06d}'
        c.execute(
            "INSERT INTO settings (key, value) VALUES (?, ?)",
            ('reset_password', generate_password_hash(default_pw))
        )
        c.execute(
            "INSERT INTO settings (key, value) VALUES (?, ?)",
            ('app_pin', generate_password_hash(app_pin))
        )
        print(f'\n=== StockPro — Identifiants par défaut ===')
        print(f'Mot de passe reset : {default_pw}')
        print(f'Code PIN application: {app_pin}')
        print(f'Changez-les dans Réglages > Sécurité')
        print(f'==========================================\n')

    conn.execute("PRAGMA foreign_keys=ON")
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


@app.route('/scanner-pro')
def scanner_pro():
    return render_template('scanner-pro.html')


@app.route('/license')
def license_page():
    return redirect(url_for('index'))


@app.route('/api/license/status')
def license_status():
    mac = get_mac_address()
    payload = get_cached_payload()
    is_admin = platform.system() != 'Windows'

    if is_admin:
        return jsonify({'admin': True, 'mac': mac or ''})

    if payload:
        return jsonify({
            'admin': False,
            'activated': True,
            'mac': mac or '',
            'client': payload.get('client', ''),
            'expires_at': payload.get('exp', 0),
        })

    return jsonify({
        'admin': False,
        'activated': False,
        'mac': mac or '',
        'error': 'Aucune licence valide trouvée',
    })


@app.route('/api/license/activate', methods=['POST'])
def license_activate():
    if platform.system() != 'Windows':
        return jsonify({'error': 'Réservé aux clients Windows'}), 403

    data = request.get_json(silent=True) or {}
    token = data.get('token', '').strip()

    if not token:
        return jsonify({'error': 'Token requis'}), 400

    payload = validate_license(token)
    if not payload:
        return jsonify({'error': 'Token invalide ou expiré'}), 400

    save_license(token)
    load_license()

    return jsonify({'ok': True})


@app.route('/api/license/generate', methods=['POST'])
@limiter.limit("3 per minute")
def license_generate():
    if platform.system() == 'Windows':
        return jsonify({'error': 'Réservé au développeur'}), 403

    data = request.get_json(silent=True) or {}
    mac = data.get('mac', '').strip()
    client = data.get('client', '').strip()
    days = int(data.get('days', 365))

    if not mac:
        return jsonify({'error': 'MAC requise'}), 400
    if not client:
        return jsonify({'error': 'Nom client requis'}), 400

    try:
        token = sign_license(mac, client, days)
        return jsonify({'token': token, 'mac': mac, 'client': client, 'days': days})
    except Exception as e:
        return jsonify({'error': _safe_err(e)}), 500















@app.route('/api/reports', methods=['GET'])
def get_reports():
    conn = get_db()
    report_type = request.args.get('type', 'overview')
    warehouse_id = request.args.get('warehouse_id')
    
    if report_type == 'overview':
        wid = validate_id(warehouse_id)
        if wid:
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
        wid = validate_id(warehouse_id)
        if wid:
            products = conn.execute('SELECT p.name, p.quantity, p.min_quantity, (SELECT COUNT(*) FROM stock_movements WHERE product_id = p.id) as movements FROM products p WHERE p.warehouse_id=? ORDER BY movements DESC LIMIT 20', (wid,)).fetchall()
        else:
            products = conn.execute('SELECT p.name, p.quantity, p.min_quantity, (SELECT COUNT(*) FROM stock_movements WHERE product_id = p.id) as movements FROM products p ORDER BY movements DESC LIMIT 20').fetchall()
        conn.close()
        return jsonify([dict(p) for p in products])

    elif report_type == 'expiry':
        wid = validate_id(warehouse_id)
        if wid:
            products = conn.execute('SELECT name, lot_number, expiry_date, quantity FROM products WHERE expiry_date IS NOT NULL AND expiry_date <= date(\'now\', \'+90 days\') AND warehouse_id=? ORDER BY expiry_date', (wid,)).fetchall()
        else:
            products = conn.execute('SELECT name, lot_number, expiry_date, quantity FROM products WHERE expiry_date IS NOT NULL AND expiry_date <= date(\'now\', \'+90 days\') ORDER BY expiry_date').fetchall()
        conn.close()
        return jsonify([dict(p) for p in products])

    elif report_type == 'categories':
        wid = validate_id(warehouse_id)
        if wid:
            data = conn.execute('SELECT category, COUNT(*) as count, SUM(quantity) as total_qty, SUM(quantity * price) as value FROM products WHERE warehouse_id=? GROUP BY category', (wid,)).fetchall()
        else:
            data = conn.execute('SELECT category, COUNT(*) as count, SUM(quantity) as total_qty, SUM(quantity * price) as value FROM products GROUP BY category').fetchall()
        conn.close()
        return jsonify([dict(d) for d in data])

    elif report_type == 'low_stock':
        wid = validate_id(warehouse_id)
        if wid:
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
        wid = validate_id(warehouse_id)
        if wid:
            products = conn.execute('SELECT * FROM products WHERE warehouse_id = ? ORDER BY name', (wid,)).fetchall()
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
        wid = validate_id(warehouse_id)
        if wid:
            query += ' WHERE p.warehouse_id = ?'
            params.append(wid)
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
    wid = validate_id(warehouse_id)
    if wid:
        conditions.append('o.warehouse_id = ?')
        params.append(wid)
    
    if conditions:
        query += ' WHERE ' + ' AND '.join(conditions)
    query += ' ORDER BY o.created_at DESC'

    orders = conn.execute(query, params).fetchall()
    conn.close()
    return jsonify([dict(o) for o in orders])

@app.route('/api/orders/<int:order_id>', methods=['GET'])
def get_order(order_id):
    conn = get_db()
    order = conn.execute('''
        SELECT o.*, s.name as supplier_name, w.name as warehouse_name
        FROM purchase_orders o
        LEFT JOIN suppliers s ON o.supplier_id = s.id
        LEFT JOIN warehouses w ON o.warehouse_id = w.id
        WHERE o.id = ?
    ''', (order_id,)).fetchone()
    conn.close()
    if order is None:
        return jsonify({'error': 'Commande introuvable'}), 404
    return jsonify(dict(order))

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
        return jsonify({'error': _safe_err(e, 'Données invalides')}), 400

@app.route('/api/orders/<int:order_id>', methods=['PUT'])
def update_order(order_id):
    conn = get_db()
    try:
        data = request.json
        if data is None:
            return jsonify({'success': False, 'error': 'Données JSON requises'}), 400
        status = data.get('status')
        if not status:
            return jsonify({'success': False, 'error': 'Champ status requis'}), 400

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
            if not order:
                return jsonify({'success': False, 'error': 'Commande introuvable'}), 404
            if order['status'] == 'paye':
                return jsonify({'success': False, 'error': 'Commande déjà payée'}), 400
            conn.execute('''
                UPDATE purchase_orders SET status='paye', paid_at=CURRENT_TIMESTAMP WHERE id=?
            ''', (order_id,))
            
            if order and order['total']:
                conn.execute('UPDATE main_account SET current_balance = current_balance - ? WHERE id = 1', (order['total'],))
                balance = conn.execute('SELECT current_balance FROM main_account WHERE id=1').fetchone()
                note = 'Achat Fournisseur: ' + order['order_number']
                if balance and balance['current_balance'] < 0:
                    note += f" (solde après: {balance['current_balance']:.2f} DH)"
                conn.execute('''
                    INSERT INTO main_account_transactions (type, amount, reason, reference_id, note)
                    VALUES ('out', ?, 'supplier_order', ?, ?)
                ''', (order['total'], order_id, note))
                
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
                if order['received_at']:
                    items = conn.execute('SELECT * FROM purchase_order_items WHERE order_id=?', (order_id,)).fetchall()
                    warehouse_id = order['warehouse_id']
                    
                    for item in items:
                        conn.execute('UPDATE products SET quantity = quantity - ? WHERE id=? AND quantity >= ?',
                                   (item['quantity'], item['product_id'], item['quantity']))
                        
                        default_location = conn.execute('SELECT id FROM locations WHERE warehouse_id=? LIMIT 1', 
                                                      (warehouse_id,)).fetchone()
                        
                        if default_location:
                            stock_entry = conn.execute('SELECT id, quantity FROM stock WHERE product_id=? AND location_id=?',
                                                     (item['product_id'], default_location['id'])).fetchone()
                            if stock_entry:
                                new_qty = stock_entry['quantity'] - item['quantity']
                                if new_qty > 0:
                                    conn.execute('UPDATE stock SET quantity = quantity - ? WHERE id=? AND quantity >= ?',
                                               (item['quantity'], stock_entry['id'], item['quantity']))
                                else:
                                    conn.execute('DELETE FROM stock WHERE id=?', (stock_entry['id'],))
                            
                            conn.execute('''
                                INSERT INTO stock_movements (product_id, type, quantity, source_location_id, note)
                                VALUES (?, 'out', ?, ?, ?)
                            ''', (item['product_id'], item['quantity'], default_location['id'], 
                                  f"Commande annulée: {order['order_number']}"))
                
                if order['status'] == 'paye' and order['total']:
                    conn.execute('UPDATE main_account SET current_balance = current_balance + ? WHERE id = 1', 
                               (order['total'],))
                    conn.execute('''
                        INSERT INTO main_account_transactions (type, amount, reason, reference_id, note)
                        VALUES ('in', ?, 'order_cancelled', ?, ?)
                    ''', (order['total'], order_id, f"Commande annulée: {order['order_number']}"))
                
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
        return jsonify({'success': True})
    except sqlite3.Error as e:
        conn.rollback()
        return jsonify({'success': False, 'error': 'Erreur de base de données'}), 500
    except Exception as e:
        conn.rollback()
        return jsonify({'success': False, 'error': 'Erreur inattendue'}), 500
    finally:
        conn.close()

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
    try:
        conn.execute('DELETE FROM purchase_order_items WHERE order_id=?', (order_id,))
        conn.execute('DELETE FROM purchase_orders WHERE id=?', (order_id,))
        conn.commit()
        return jsonify({'success': True})
    except Exception as e:
        conn.rollback()
        return jsonify({'error': _safe_err(e)}), 500
    finally:
        conn.close()

@app.route('/api/reorder-rules', methods=['GET'])
def get_reorder_rules():
    warehouse_id = request.args.get('warehouse_id')
    conn = get_db()
    
    wid = validate_id(warehouse_id)
    if wid:
        rules = conn.execute('''
            SELECT r.*, p.name as product_name, p.quantity as current_qty, p.min_quantity, s.name as supplier_name
            FROM reordering_rules r
            JOIN products p ON r.product_id = p.id
            LEFT JOIN suppliers s ON r.supplier_id = s.id
            WHERE r.warehouse_id = ?
            ORDER BY p.name
        ''', (wid,)).fetchall()
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
        return jsonify({'error': _safe_err(e, 'Données invalides')}), 400

@app.route('/api/reorder-rules/<int:rule_id>', methods=['PUT'])
def update_reorder_rule(rule_id):
    data = request.json
    conn = get_db()
    try:
        conn.execute('''
            UPDATE reordering_rules SET min_quantity=?, max_quantity=?, trigger_type=?, supplier_id=?
            WHERE id=?
        ''', (data.get('min_quantity', 5), data.get('max_quantity', 100), data.get('trigger_type', 'manual'),
              data.get('supplier_id'), rule_id))
        conn.commit()
        return jsonify({'success': True})
    except Exception as e:
        conn.rollback()
        return jsonify({'error': _safe_err(e)}), 500
    finally:
        conn.close()

@app.route('/api/reorder-rules/<int:rule_id>', methods=['DELETE'])
def delete_reorder_rule(rule_id):
    conn = get_db()
    try:
        conn.execute('DELETE FROM reordering_rules WHERE id=?', (rule_id,))
        conn.commit()
        return jsonify({'success': True})
    except Exception as e:
        conn.rollback()
        return jsonify({'error': _safe_err(e)}), 500
    finally:
        conn.close()

@app.route('/api/replenishment', methods=['GET'])
def get_replenishment():
    warehouse_id = request.args.get('warehouse_id')
    conn = get_db()
    
    wid = validate_id(warehouse_id)
    if wid:
        products = conn.execute('''
            SELECT r.id as rule_id, p.id as product_id, p.name, p.sku, p.quantity as current_qty, r.min_quantity, r.max_quantity, r.trigger_type,
                   r.supplier_id, r.warehouse_id as rule_warehouse_id, s.name as supplier_name,
                   MAX(0, r.max_quantity - p.quantity) as suggested_qty
            FROM reordering_rules r
            JOIN products p ON r.product_id = p.id
            LEFT JOIN suppliers s ON r.supplier_id = s.id
            WHERE r.warehouse_id = ?
            ORDER BY p.name
        ''', (wid,)).fetchall()
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
    wid = validate_id(warehouse_id)
    if wid:
        query += ' WHERE n.warehouse_id = ? OR n.warehouse_id IS NULL'
        params.append(wid)
    query += ' ORDER BY n.created_at DESC LIMIT 50'
    
    notifications = conn.execute(query, params).fetchall()
    unread = conn.execute('SELECT COUNT(*) FROM notifications WHERE is_read = 0').fetchone()[0]
    
    conn.close()
    return jsonify({'notifications': [dict(n) for n in notifications], 'unread_count': unread})

@app.route('/api/notifications/<int:notification_id>/read', methods=['POST'])
def mark_notification_read(notification_id):
    conn = get_db()
    try:
        conn.execute('UPDATE notifications SET is_read = 1 WHERE id=?', (notification_id,))
        conn.commit()
        return jsonify({'success': True})
    except Exception as e:
        conn.rollback()
        return jsonify({'error': _safe_err(e)}), 500
    finally:
        conn.close()

@app.route('/api/notifications/mark-all-read', methods=['POST'])
def mark_all_notifications_read():
    conn = get_db()
    try:
        conn.execute('UPDATE notifications SET is_read = 1')
        conn.commit()
        return jsonify({'success': True})
    except Exception as e:
        conn.rollback()
        return jsonify({'error': _safe_err(e)}), 500
    finally:
        conn.close()

@app.route('/api/reset-data', methods=['POST'])
def reset_data():
    if not _HAS_RESET:
        return jsonify({'error': 'Module reset_test_db non disponible'}), 500
    data = request.get_json(silent=True) or {}
    keep_products = data.get('keep_products', False)
    conn = get_db()
    reset_transactional_data(conn, keep_products=keep_products)
    conn.close()
    return jsonify({'success': True, 'message': 'Toutes les données ont été réinitialisées'})

@app.route('/api/reset-products', methods=['POST'])
def reset_products_api():
    if not _HAS_RESET:
        return jsonify({'error': 'Module reset_test_db non disponible'}), 500
    data = request.get_json(silent=True) or {}
    ids = data.get('ids')
    conn = get_db()
    conn.execute("PRAGMA foreign_keys = OFF")
    if isinstance(ids, list) and len(ids) > 0:
        ph = ','.join('?' * len(ids))
        conn.execute(f"DELETE FROM reordering_rules WHERE product_id IN ({ph})", ids)
        conn.execute(f"DELETE FROM stock_movements WHERE product_id IN ({ph})", ids)
        conn.execute(f"DELETE FROM stock WHERE product_id IN ({ph})", ids)
        conn.execute(f"DELETE FROM products WHERE id IN ({ph})", ids)
    else:
        reset_products_data(conn)
    conn.execute("PRAGMA foreign_keys = ON")
    conn.close()
    return jsonify({'success': True, 'message': 'Produits supprimés'})

@app.route('/api/reset-products-qty', methods=['POST'])
def reset_products_qty_api():
    if not _HAS_RESET:
        return jsonify({'error': 'Module reset_test_db non disponible'}), 500
    conn = get_db()
    reset_products_qty(conn)
    conn.close()
    return jsonify({'success': True, 'message': 'Toutes les quantités ont été réinitialisées à 0'})

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
            INSERT INTO products (name, description, sku, barcode, quantity, min_quantity, max_quantity, price, price_base, price_loyal, price_gros, category, warehouse_id, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', (name, desc, sku, barcode, qty, min_qty, qty * 2, price, price * 0.6, price * 0.9, price * 0.75, category, wh[0], datetime.now().isoformat()))
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
    date_start = request.args.get('date_start')
    date_end = request.args.get('date_end')
    conn = get_db()
    
    inv_query = '''
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
    inv_params = []
    wid = validate_id(warehouse_id)
    if wid:
        inv_query += ' AND i.warehouse_id = ?'
        inv_params.append(wid)
    if date_start:
        inv_query += ' AND date(COALESCE(i.paid_at, i.created_at)) >= ?'
        inv_params.append(date_start)
    if date_end:
        inv_query += ' AND date(COALESCE(i.paid_at, i.created_at)) <= ?'
        inv_params.append(date_end)
    if status != 'all':
        inv_query += ' AND i.status = ?'
        inv_params.append(status)
    inv_query += ' ORDER BY i.created_at DESC'
    
    invoices = conn.execute(inv_query, inv_params).fetchall()
    
    conn.close()
    
    return jsonify([dict(i) for i in invoices])

@app.route('/api/invoices', methods=['POST'])
def create_invoice():
    data = request.json
    conn = get_db()
    
    try:
        doc_type = data.get('type', 'facture')
        
        if doc_type == 'bon_de_livraison':
            seq = next_sequence(conn, 'bl_counter')
            invoice_number = f"BL-{datetime.now().strftime('%Y%m%d')}-{seq:04d}"
        else:
            seq = next_sequence(conn, 'fac_counter')
            invoice_number = f"FAC-{datetime.now().strftime('%Y%m%d')}-{seq:04d}"
        
        warehouse_id = data.get('warehouse_id', 1)
        customer_id = data.get('customer_id')
        notes = data.get('notes', '')
        items = data.get('items', [])
        
        conn.execute('''
            INSERT INTO invoices (invoice_number, customer_id, warehouse_id, notes, status, type)
            VALUES (?, ?, ?, ?, 'brouillon', ?)
        ''', (invoice_number, customer_id, warehouse_id, notes, doc_type))
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
                unit_price = get_price_by_tier(p, customer['type'])
            else:
                unit_price = item.get('unit_price', p['price_base'] if p['price_base'] > 0 else p['price'])
            
            discount_percent = item.get('discount_percent', 0)
            qty = item.get('quantity', 1)
            tax_rate = float(item.get('tax_rate', p.get('tax_category', '20') or '20'))
            
            line_subtotal = qty * unit_price
            discount_amount = line_subtotal * (discount_percent / 100)
            line_total = line_subtotal - discount_amount
            
            if doc_type == 'bon_de_livraison':
                tax_line = 0
                tax_rate = 0
            else:
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
            ''', (item['product_id'], qty, f'{"Bon de livraison" if doc_type == "bon_de_livraison" else "Vente facture"} {invoice_number}'))
        
        total = subtotal - discount_total + tax_amount
        
        conn.execute('''
            UPDATE invoices SET subtotal=?, discount_total=?, tax_amount=?, total=? WHERE id=?
        ''', (subtotal, discount_total, tax_amount, total, invoice_id))
        
        conn.commit()
        conn.close()
        return jsonify({'success': True, 'invoice_id': invoice_id, 'invoice_number': invoice_number})
    except Exception as e:
        conn.close()
        return jsonify({'error': _safe_err(e, 'Données invalides')}), 400

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
    try:
        status = data.get('status')
        
        if status == 'payee':
            conn.execute('''
                UPDATE invoices SET status='payee', amount_paid=total, paid_at=CURRENT_TIMESTAMP, updated_at=CURRENT_TIMESTAMP WHERE id=?
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
        return jsonify({'success': True})
    except Exception as e:
        conn.rollback()
        return jsonify({'error': _safe_err(e)}), 500
    finally:
        conn.close()

@app.route('/api/invoices/<int:invoice_id>/pay-credit', methods=['POST'])
def pay_invoice_credit(invoice_id):
    """Pay remaining credit on an invoice (partial or full)"""
    data = request.json or {}
    amount = float(data.get('amount', 0))
    payment_method = data.get('payment_method', 'cash')

    if amount <= 0:
        return jsonify({'error': 'Montant invalide'}), 400

    conn = get_db()
    try:
        invoice = conn.execute('SELECT * FROM invoices WHERE id=?', (invoice_id,)).fetchone()
        if not invoice:
            return jsonify({'error': 'Facture non trouvée'}), 404
        if invoice['status'] not in ('envoyee', 'partiellement_payee'):
            return jsonify({'error': 'Seules les factures en attente ou partiellement payées peuvent recevoir un paiement'}), 400

        old_paid = invoice['amount_paid'] or 0
        new_paid = old_paid + amount
        total = invoice['total'] or 0

        if new_paid > total:
            return jsonify({'error': f'Le montant total à payer est de {total - old_paid:.2f} DH. Montant saisi: {amount:.2f} DH'}), 400

        is_now_fully_paid = new_paid >= total

        conn.execute('''
            UPDATE invoices SET amount_paid=?, status=?, paid_at=CURRENT_TIMESTAMP, updated_at=CURRENT_TIMESTAMP, is_credit_payment=? WHERE id=?
        ''', (total if is_now_fully_paid else new_paid,
              'payee' if is_now_fully_paid else 'partiellement_payee',
              1 if is_now_fully_paid else 0,
              invoice_id))

        # Record cash movement
        if payment_method == 'cash':
            session = conn.execute("SELECT id FROM pos_sessions WHERE status = 'open' ORDER BY opened_at DESC LIMIT 1").fetchone()
            if session:
                conn.execute('''
                    INSERT INTO pos_cash_movements (session_id, type, amount, reason, note)
                    VALUES (?, 'in', ?, 'sale', ?)
                ''', (session['id'], amount, f'Paiement crédit facture {invoice["invoice_number"]}'))
        elif payment_method == 'card':
            conn.execute('UPDATE main_account SET current_balance = current_balance + ? WHERE id = 1', (amount,))
            conn.execute('''
                INSERT INTO main_account_transactions (type, amount, reason, note)
                VALUES ('in', ?, 'card_payment', ?)
            ''', (amount, f'Paiement carte - crédit facture {invoice["invoice_number"]}'))

        conn.commit()
        return jsonify({
            'success': True,
            'status': 'payee' if is_now_fully_paid else 'partiellement_payee',
            'amount_paid': total if is_now_fully_paid else new_paid,
            'remaining': 0 if is_now_fully_paid else total - new_paid
        })
    except Exception as e:
        return jsonify({'error': _safe_err(e, 'Données invalides')}), 400
    finally:
        conn.close()

@app.route('/api/invoices/<int:invoice_id>', methods=['DELETE'])
def delete_invoice(invoice_id):
    conn = get_db()
    try:
        invoice = conn.execute('SELECT status FROM invoices WHERE id=?', (invoice_id,)).fetchone()
        if invoice and invoice['status'] == 'payee':
            return jsonify({'error': 'Impossible de supprimer une facture payée'}), 400
        
        old_items = conn.execute('SELECT * FROM invoice_items WHERE invoice_id=?', (invoice_id,)).fetchall()
        for item in old_items:
            conn.execute('UPDATE products SET quantity = quantity + ? WHERE id=?', (item['quantity'], item['product_id']))
            conn.execute('''
                INSERT INTO stock_movements (product_id, type, quantity, note)
                VALUES (?, 'in', ?, 'Rétablissement stock - suppression facture')
            ''', (item['product_id'], item['quantity']))
        
        conn.execute('DELETE FROM invoice_items WHERE invoice_id=?', (invoice_id,))
        conn.execute('DELETE FROM invoices WHERE id=?', (invoice_id,))
        conn.commit()
        return jsonify({'success': True})
    except Exception as e:
        conn.rollback()
        return jsonify({'error': _safe_err(e)}), 500
    finally:
        conn.close()

@app.route('/api/invoices/<int:invoice_id>/convert-to-invoice', methods=['POST'])
def convert_to_invoice(invoice_id):
    conn = get_db()
    try:
        bl = conn.execute('SELECT * FROM invoices WHERE id=?', (invoice_id,)).fetchone()
        if not bl:
            return jsonify({'error': 'Bon de livraison non trouvé'}), 404
        
        if bl['type'] != 'bon_de_livraison':
            return jsonify({'error': 'Ce document n\'est pas un bon de livraison'}), 400
        
        if bl['status'] == 'annulee':
            return jsonify({'error': 'Impossible de convertir un bon de livraison annulé'}), 400
        
        if bl['status'] == 'payee':
            return jsonify({'error': 'Ce bon de livraison est déjà converti'}), 400
        
        conn.execute('''
            UPDATE invoices SET status='payee', paid_at=CURRENT_TIMESTAMP WHERE id=?
        ''', (invoice_id,))
        
        conn.commit()
        return jsonify({'success': True, 'invoice_id': invoice_id, 'invoice_number': bl['invoice_number']})
    except Exception as e:
        conn.rollback()
        return jsonify({'error': _safe_err(e)}), 500
    finally:
        conn.close()

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
            unit_price = get_price_by_tier(p, customer['type'])
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
        return jsonify({'error': _safe_err(e, 'Données invalides')}), 400

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
    try:
        item = conn.execute('SELECT * FROM invoice_items WHERE id=? AND invoice_id=?', (item_id, invoice_id)).fetchone()
        
        if item:
            conn.execute('UPDATE products SET quantity = quantity + ? WHERE id=?', (item['quantity'], item['product_id']))
            conn.execute('DELETE FROM invoice_items WHERE id=?', (item_id,))
            recalculate_invoice(invoice_id, conn)
            conn.commit()
        
        return jsonify({'success': True})
    except Exception as e:
        conn.rollback()
        return jsonify({'error': _safe_err(e)}), 500
    finally:
        conn.close()

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
            <div class="party-name">{_reshape(customer_name)}</div>
            <div class="party-detail">Code: <strong>{_reshape(_n(invoice_data.get('client_code'), '-'))}</strong></div>
            <div class="party-detail">ICE: <strong>{_reshape(customer_ice)}</strong></div>
            <div class="party-detail">{_reshape(_n(invoice_data.get('customer_address'), ''))}</div>
            <div class="party-detail">Tel: {_reshape(_n(invoice_data.get('customer_phone'), '-'))}</div>
        </div>
        """
    
    # Company info
    company_name = 'Bibliotheque Badr'
    company_address = 'Papeterie Al Qalam, Rte de Sefrou, Fès 30050'
    company_ice = '001234567000089'
    company_patente = '12345678'
    company_rc = '12345'
    company_tax = '3456789012'
    company_phone = '0524441234'
    
    # Colors
    accent = '#1a56db'
    accent_light = '#eef2ff'
    text_primary = '#1f2937'
    text_secondary = '#6b7280'
    border_color = '#e5e7eb'
    bg_card = '#f9fafb'
    
    status_badges = {
        'payee': ('#059669', '#ecfdf5'),
        'envoyee': ('#2563eb', '#eff6ff'),
        'brouillon': ('#d97706', '#fffbeb'),
        'annulee': ('#dc2626', '#fef2f2'),
    }
    st = _n(invoice_data.get('status'), 'payee')
    badge_text = status_labels.get(st, 'Payee').upper()
    badge_fg, badge_bg = status_badges.get(st, ('#6b7280', '#f3f4f6'))
    
    watermark_class = 'watermark' if st in ('brouillon', 'annulee') else 'watermark hidden'
    watermark_text = 'BROUILLON' if st == 'brouillon' else 'ANNULEE'
    
    show_company_info = _n(invoice_data.get('type'), 'facture') == 'fournisseur'
    
    # Build party HTML
    if show_company_info:
        party_html = f"""
        <div class="party-card">
            <div class="party-title">Fournisseur</div>
            <div class="party-name" dir="auto">{_reshape(_n(invoice_data.get('supplier_name'), 'Fournisseur'))}</div>
            <div class="party-detail" dir="auto">{_reshape(_n(invoice_data.get('supplier_address'), ''))}</div>
            <div class="party-detail">Tel: <span dir="auto">{_reshape(_n(invoice_data.get('supplier_phone'), '-'))}</span></div>
        </div>"""
    else:
        party_html = f"""
        <div class="party-card">
            <div class="party-title">Client</div>
            <div class="party-name" dir="auto">{_reshape(customer_name)}</div>
            <div class="party-detail">Code: <span class="highlight" dir="auto">{_reshape(_n(invoice_data.get('client_code'), '-'))}</span></div>
            <div class="party-detail">ICE: <span class="highlight" dir="auto">{_reshape(customer_ice)}</span></div>
            <div class="party-detail" dir="auto">{_reshape(_n(invoice_data.get('customer_address'), ''))}</div>
            <div class="party-detail">Tel: <span dir="auto">{_reshape(_n(invoice_data.get('customer_phone'), '-'))}</span></div>
        </div>"""
    
    pdf_html = f"""<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Facture {invoice_data['invoice_number']}</title>
    <style>
        @font-face {{ font-family: 'DejaVu Sans'; src: url('/static/fonts/DejaVuSans.ttf') format('truetype'); font-weight: normal; font-style: normal; font-display: swap; }}
        @font-face {{ font-family: 'DejaVu Sans'; src: url('/static/fonts/DejaVuSans-Bold.ttf') format('truetype'); font-weight: bold; font-style: normal; font-display: swap; }}
        * {{ margin: 0; padding: 0; box-sizing: border-box; }}
        body {{ font-family: 'DejaVu Sans', 'Geist Variable', system-ui, -apple-system, 'Segoe UI', sans-serif; font-size: 11px; padding: 30px; color: {text_primary}; background: #f3f4f6; }}
        .page {{ position: relative; max-width: 800px; margin: 0 auto; background: white; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); overflow: hidden; }}
        .watermark {{ position: absolute; top: 40%; left: 50%; transform: translate(-50%, -50%) rotate(-30deg); font-size: 80px; font-weight: 800; color: rgba(0,0,0,0.04); letter-spacing: 0.1em; pointer-events: none; white-space: nowrap; user-select: none; z-index: 0; }}
        .watermark.hidden {{ display: none; }}
        .top-bar {{ background: {accent}; padding: 6px 32px; }}
        .header {{ padding: 20px 32px 10px; }}
        .header-main {{ display: flex; justify-content: space-between; align-items: flex-start; }}
        .header-id-frame {{ background: {bg_card}; border: 1px solid {border_color}; border-radius: 8px; padding: 10px 16px; min-width: 210px; }}
        .id-frame-title {{ font-size: 8px; font-weight: 700; color: {text_secondary}; text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 5px; }}
        .id-frame-row {{ font-size: 10px; color: {text_primary}; padding: 1px 0; font-variant-numeric: tabular-nums; }}
        .id-frame-row .id-label {{ color: {text_secondary}; font-weight: 600; display: inline-block; width: 50px; }}
        .header-right {{ text-align: right; }}
        .invoice-title {{ font-size: 22px; font-weight: 700; color: {text_primary}; line-height: 1.2; }}
        .invoice-number {{ font-size: 13px; font-weight: 600; color: {accent}; margin-top: 3px; }}
        .status-badge {{ display: inline-block; margin-top: 6px; padding: 2px 12px; border-radius: 999px; font-size: 9px; font-weight: 700; letter-spacing: 0.03em; color: {badge_fg}; background: {badge_bg}; }}
        .meta-info {{ margin-top: 8px; font-size: 10px; color: {text_secondary}; line-height: 1.5; }}
        .header-logo-row {{ text-align: center; padding: 18px 0 6px; }}
        .header-logo {{ height: 80px; width: auto; }}
        .header-address {{ text-align: center; font-size: 10px; color: {text_secondary}; padding: 0 0 14px; border-bottom: 1px solid {border_color}; }}
        .content {{ padding: 0 32px 24px; position: relative; z-index: 1; }}
        .parties {{ display: flex; gap: 16px; margin-bottom: 24px; }}
        .party-card {{ flex: 1; background: {bg_card}; padding: 16px 20px; border-radius: 8px; border: 1px solid {border_color}; }}
        .party-title {{ font-size: 9px; font-weight: 600; color: {accent}; text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 8px; }}
        .party-name {{ font-size: 14px; font-weight: 700; color: {text_primary}; margin-bottom: 6px; }}
        .party-detail {{ font-size: 10px; color: {text_secondary}; margin: 2px 0; }}
        .party-detail .highlight {{ color: {text_primary}; font-weight: 600; }}
        .info-divider {{ height: 1px; background: {border_color}; margin-bottom: 24px; }}
        .table-wrapper {{ border: 1px solid {border_color}; border-radius: 8px; overflow: hidden; }}
        table {{ width: 100%; border-collapse: collapse; }}
        th {{ background: {bg_card}; color: {text_secondary}; padding: 10px 14px; text-align: left; font-weight: 600; font-size: 9px; text-transform: uppercase; letter-spacing: 0.05em; border-bottom: 1px solid {border_color}; }}
        td {{ padding: 10px 14px; border-bottom: 1px solid #f3f4f6; font-size: 11px; color: {text_primary}; }}
        tr:last-child td {{ border-bottom: none; }}
        tr:nth-child(even) td {{ background: #fafafa; }}
        .totals-section {{ display: flex; justify-content: flex-end; margin-top: 20px; }}
        .totals-box {{ width: 280px; background: {bg_card}; padding: 16px 20px; border-radius: 8px; border: 1px solid {border_color}; }}
        .totals-row {{ display: flex; justify-content: space-between; padding: 5px 0; font-size: 11px; color: {text_primary}; }}
        .totals-row.discount {{ color: #dc2626; }}
        .totals-row.grand {{ border-top: 2px solid {accent}; margin-top: 8px; padding-top: 10px; font-size: 15px; font-weight: 800; color: {accent}; }}
        .footer {{ padding: 16px 32px; border-top: 1px solid {border_color}; background: {bg_card}; }}
        .footer-grid {{ display: grid; grid-template-columns: 1fr 1fr; gap: 8px 24px; font-size: 10px; color: {text_secondary}; margin-bottom: 10px; }}
        .footer-label {{ color: {text_secondary}; font-weight: 600; }}
        .footer-message {{ text-align: center; font-size: 10px; color: {text_secondary}; padding-top: 10px; border-top: 1px dashed {border_color}; }}
        .btn {{ display: block; width: 200px; margin: 24px auto 0; background: {accent}; color: white; border: none; padding: 12px 20px; border-radius: 8px; cursor: pointer; font-size: 12px; font-weight: 600; text-align: center; }}
        .btn:hover {{ background: #1d4ed8; }}
        @media print {{ .btn {{ display: none; }} body {{ background: white; padding: 0; }} .page {{ box-shadow: none; border-radius: 0; }} }}
    </style>
</head>
<body>
    <div class="page">
        <div class="{watermark_class}">{watermark_text}</div>
        <div class="top-bar"></div>
    <div class="header">
        <div class="header-logo-row">
            <img src="/static/img/logo.png" class="header-logo" alt="Bibliotheque Badr">
        </div>
        <div class="header-main">
            <div class="header-id-frame">
                <div class="id-frame-title">Identifiant fiscal</div>
                <div class="id-frame-row"><span class="id-label">ICE</span> {company_ice}</div>
                <div class="id-frame-row"><span class="id-label">RC</span> {company_rc}</div>
                <div class="id-frame-row"><span class="id-label">Patente</span> {company_patente}</div>
                <div class="id-frame-row"><span class="id-label">Taxe</span> {company_tax}</div>
            </div>
            <div class="header-right">
                <div class="invoice-title">{show_company_info and "FACTURE D'ACHAT" or ("BON DE LIVRAISON" if _n(invoice_data.get('type'), 'facture') == 'bon_de_livraison' else "FACTURE")}</div>
                <div class="invoice-number">{invoice_data['invoice_number']}</div>
                <div class="status-badge">{badge_text}</div>
                <div class="meta-info">
                    <div>Date: {invoice_data.get('created_at', '')[:10] if invoice_data.get('created_at') else '-'}</div>
                    <div>Echéance: {_n(invoice_data.get('due_date'), '-')}</div>
                </div>
            </div>
        </div>
        <div class="header-address">{company_address} — Tel: {company_phone}</div>
    </div>
        
        <div class="content">
            <div class="parties">
                {party_html}
                <div class="party-card">
                    <div class="party-title">Informations</div>
                    <div class="party-detail">Paiement: <span class="highlight">{_n(invoice_data.get('payment_method'), 'cash').title()}</span></div>
                    <div class="party-detail">Statut: <span class="highlight">{badge_text.title()}</span></div>
                </div>
            </div>
            <div class="info-divider"></div>
            
            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr>
                            <th style="width:36%">Désignation</th>
                            <th style="width:14%">SKU</th>
                            <th style="width:10%;text-align:center">Qté</th>
                            <th style="width:15%;text-align:right">Prix unit.</th>
                            <th style="width:10%;text-align:right">Remise</th>
                            <th style="width:15%;text-align:right">Total HT</th>
                        </tr>
                    </thead>
                    <tbody>"""
    
    for item in items_data:
        pdf_html += f"""
                        <tr>
                            <td dir="auto">{_reshape(_n(item.get('product_name'), 'Article'))}</td>
                            <td style="color:{text_secondary}" dir="auto">{_reshape(_n(item.get('product_sku'), '-'))}</td>
                            <td style="text-align:center">{item['quantity']}</td>
                            <td style="text-align:right;font-variant-numeric:tabular-nums">{item['unit_price']:.2f}</td>
                            <td style="text-align:right;font-variant-numeric:tabular-nums">{_n(item.get('discount_percent'), 0)}%</td>
                            <td style="text-align:right;font-weight:600;font-variant-numeric:tabular-nums">{item['line_total']:.2f}</td>
                        </tr>"""
    
    pdf_html += f"""
                    </tbody>
                </table>
            </div>
            
            <div class="totals-section">
                <div class="totals-box">
                    <div class="totals-row"><span>Sous-total HT</span><span>{_n(invoice_data.get('subtotal'), 0):.2f} DH</span></div>
                    <div class="totals-row discount"><span>Remises</span><span>-{_n(invoice_data.get('discount_total'), 0):.2f} DH</span></div>
                    {"" if _n(invoice_data.get('type'), 'facture') == 'bon_de_livraison' else f'<div class="totals-row"><span>TVA (20%)</span><span>{_n(invoice_data.get("tax_amount"), 0):.2f} DH</span></div>'}
                    <div class="totals-row grand"><span>{"TOTAL HT" if _n(invoice_data.get('type'), 'facture') == 'bon_de_livraison' else "TOTAL TTC"}</span><span>{_n(invoice_data.get('total'), 0):.2f} DH</span></div>
                </div>
            </div>
        </div>
        
        <div class="footer">
            <div class="footer-grid">
                <div><span class="footer-label">ICE:</span> {company_ice}</div>
                <div><span class="footer-label">RC:</span> {company_rc}</div>
                <div><span class="footer-label">Patente:</span> {company_patente}</div>
                <div><span class="footer-label">Taxe:</span> {company_tax}</div>
                <div><span class="footer-label">Adresse:</span> {company_address}</div>
                <div><span class="footer-label">Tel:</span> {company_phone}</div>
            </div>
            <div class="footer-message">
                Merci pour votre confiance
            </div>
        </div>
        
        <button class="btn" onclick="window.print()">Imprimer / Télécharger PDF</button>
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
    paid_amount = conn.execute('SELECT COALESCE(SUM(total), 0) FROM invoices WHERE status IN ("payee", "ticket")').fetchone()[0]
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
        SELECT i.id, i.invoice_number, i.total, i.total - i.amount_paid as montant_restant,
               i.amount_paid as deja_paye, i.status, i.due_date, i.created_at,
               c.id as customer_id, c.name as customer_name, c.client_code
        FROM invoices i
        JOIN customers c ON i.customer_id = c.id
        WHERE i.status IN ('envoyee', 'partiellement_payee')
        ORDER BY i.due_date ASC
    """).fetchall()
    
    conn.close()
    return jsonify([dict(r) for r in receivables])

@app.route('/api/pos/sessions', methods=['GET'])
def get_pos_session():
    """Get active POS session, optionally filtered by register_id"""
    conn = get_db()
    register_id = request.args.get('register_id')
    
    if register_id:
        # Auto-close orphaned older sessions on this register
        orphans = conn.execute('''
            SELECT id FROM pos_sessions 
            WHERE status = 'open' AND register_id = ?
            ORDER BY opened_at ASC
        ''', (register_id,)).fetchall()
        if len(orphans) > 1:
            # Keep only the newest, close the rest
            for orphan in orphans[:-1]:
                total_in = conn.execute(
                    "SELECT COALESCE(SUM(amount), 0) FROM pos_cash_movements WHERE session_id = ? AND type = 'in'",
                    (orphan['id'],)
                ).fetchone()[0]
                total_out = conn.execute(
                    "SELECT COALESCE(SUM(amount), 0) FROM pos_cash_movements WHERE session_id = ? AND type = 'out'",
                    (orphan['id'],)
                ).fetchone()[0]
                sess_row = conn.execute('SELECT opening_cash FROM pos_sessions WHERE id = ?', (orphan['id'],)).fetchone()
                opening = sess_row['opening_cash'] if sess_row else 0
                expected = opening + total_in - total_out
                conn.execute(
                    "UPDATE pos_sessions SET status = 'closed', expected_cash = ?, closing_cash = ?, closed_at = CURRENT_TIMESTAMP WHERE id = ?",
                    (expected, expected, orphan['id'])
                )
            conn.commit()
        session = conn.execute('''
            SELECT s.*, r.name as register_name
            FROM pos_sessions s
            LEFT JOIN pos_registers r ON s.register_id = r.id
            WHERE s.status = 'open' AND s.register_id = ?
            ORDER BY s.opened_at DESC
            LIMIT 1
        ''', (register_id,)).fetchone()
    else:
        session = conn.execute('''
            SELECT s.*, r.name as register_name
            FROM pos_sessions s
            LEFT JOIN pos_registers r ON s.register_id = r.id
            WHERE s.status = 'open'
            ORDER BY s.opened_at DESC
            LIMIT 1
        ''').fetchone()
    
    result = [dict(session)] if session else []
    conn.close()
    return jsonify(result)

@app.route('/api/pos/sessions', methods=['POST'])
def open_pos_session():
    """Open a new POS session for a specific register"""
    data = request.json or {}
    warehouse_id = data.get('warehouse_id', 1)
    try:
        opening_cash = max(0, float(str(data.get('opening_cash', 0) or 0)))
    except (ValueError, TypeError):
        opening_cash = 0
    register_id = data.get('register_id')
    cashier_name = data.get('cashier_name', '')
    
    conn = get_db()
    try:
        if register_id:
            reg = conn.execute('SELECT * FROM pos_registers WHERE id = ? AND is_active = 1', (register_id,)).fetchone()
            if not reg:
                return jsonify({'error': 'Caisse invalide ou désactivée'}), 400
            warehouse_id = reg['warehouse_id']
            # Validate warehouse exists (registers may have stale FK values)
            wh = conn.execute('SELECT id FROM warehouses WHERE id = ?', (warehouse_id,)).fetchone()
            if not wh:
                wh = conn.execute('SELECT id FROM warehouses LIMIT 1').fetchone()
                warehouse_id = wh['id'] if wh else 1
            # Auto-close any orphaned open session on this register
            orphan = conn.execute(
                "SELECT id, session_number FROM pos_sessions WHERE status = 'open' AND register_id = ? ORDER BY opened_at ASC LIMIT 1",
                (register_id,)
            ).fetchone()
            if orphan:
                # Force-close orphan: compute expected_cash and set closed
                total_in = conn.execute(
                    "SELECT COALESCE(SUM(amount), 0) FROM pos_cash_movements WHERE session_id = ? AND type = 'in'",
                    (orphan['id'],)
                ).fetchone()[0]
                total_out = conn.execute(
                    "SELECT COALESCE(SUM(amount), 0) FROM pos_cash_movements WHERE session_id = ? AND type = 'out'",
                    (orphan['id'],)
                ).fetchone()[0]
                sess_row = conn.execute('SELECT opening_cash FROM pos_sessions WHERE id = ?', (orphan['id'],)).fetchone()
                opening = sess_row['opening_cash'] if sess_row else 0
                expected = opening + total_in - total_out
                conn.execute(
                    "UPDATE pos_sessions SET status = 'closed', expected_cash = ?, closing_cash = ?, closed_at = CURRENT_TIMESTAMP WHERE id = ?",
                    (expected, expected, orphan['id'])
                )
        else:
            existing = conn.execute("SELECT id FROM pos_sessions WHERE status = 'open'").fetchone()
            if existing:
                return jsonify({'error': 'Une session est déjà ouverte'}), 400
        
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
            INSERT INTO pos_sessions (session_number, warehouse_id, opening_cash, status, register_id, cashier_name)
            VALUES (?, ?, ?, 'open', ?, ?)
        ''', (session_number, warehouse_id, opening_cash, register_id, cashier_name))
        
        session_id = conn.execute('SELECT last_insert_rowid()').fetchone()[0]
        
        # Debit opening_cash from main account
        if opening_cash > 0:
            conn.execute('UPDATE main_account SET current_balance = current_balance - ? WHERE id = 1', (opening_cash,))
            conn.execute('''
                INSERT INTO main_account_transactions (type, amount, reason, note)
                VALUES ('out', ?, 'session_open', ?)
            ''', (opening_cash, f'Ouverture caisse: {session_number}'))
        
        conn.commit()
        
        session = conn.execute('SELECT * FROM pos_sessions WHERE id = ?', (session_id,)).fetchone()
        
        return jsonify({
            'success': True,
            'session': dict(session),
            'session_number': session_number
        })
    except sqlite3.Error as e:
        conn.rollback()
        return jsonify({'error': 'Erreur de base de données'}), 500
    except Exception as e:
        conn.rollback()
        return jsonify({'error': 'Erreur inattendue'}), 500
    finally:
        conn.close()

@app.route('/api/pos/sessions/<int:session_id>/close', methods=['POST'])
def close_pos_session(session_id):
    """Close POS session and deposit to main account"""
    data = request.json or {}
    try:
        closing_cash = max(0, float(str(data.get('closing_cash', 0) or 0)))
    except (ValueError, TypeError):
        closing_cash = 0
    deposit_to_main = data.get('deposit_to_main', True)
    
    conn = get_db()
    try:
        session = conn.execute('SELECT * FROM pos_sessions WHERE id = ?', (session_id,)).fetchone()
        if not session:
            return jsonify({'error': 'Session non trouvée'}), 404
        
        if session['status'] != 'open':
            return jsonify({'error': 'Session déjà fermée'}), 400
        
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
        
        net_to_deposit = expected_cash
        if deposit_to_main and net_to_deposit > 0:
            conn.execute('UPDATE main_account SET current_balance = current_balance + ? WHERE id = 1', (net_to_deposit,))
            conn.execute('''
                INSERT INTO main_account_transactions (type, amount, reason, reference_id, note)
                VALUES ('in', ?, 'session_close', ?, ?)
            ''', (net_to_deposit, session_id, 'Session: ' + session['session_number']))
        
        conn.commit()
        return jsonify({'success': True, 'expected_cash': expected_cash, 'deposited': deposit_to_main and net_to_deposit > 0})
    except sqlite3.Error as e:
        conn.rollback()
        return jsonify({'error': 'Erreur de base de données'}), 500
    except Exception as e:
        conn.rollback()
        return jsonify({'error': 'Erreur inattendue'}), 500
    finally:
        conn.close()

@app.route('/api/pos/transactions', methods=['POST'])
@limiter.limit("30 per minute")
def create_pos_transaction():
    """Create a POS transaction and decrement stock"""
    data = request.json or {}
    session_id = data.get('session_id')
    customer_id = data.get('customer_id')
    items = data.get('items', [])
    payment_method = data.get('payment_method', 'cash')
    tendered_amount = data.get('tendered_amount', 0)
    pricing_tier = data.get('pricing_tier', 'normal')
    apply_tax = data.get('apply_tax', True)
    notes = data.get('notes', '')
    is_credit = data.get('is_credit', False)
    doc_type = data.get('doc_type', 'bon_de_livraison')
    
    if not session_id or not items:
        return jsonify({'error': 'Données invalides'}), 400
    
    conn = get_db()
    try:
        # Verify session is open
        session = conn.execute('''
            SELECT s.*, r.name as register_name
            FROM pos_sessions s
            LEFT JOIN pos_registers r ON s.register_id = r.id
            WHERE s.id = ?
        ''', (session_id,)).fetchone()
        if not session or session['status'] != 'open':
            return jsonify({'error': 'Session fermée ou inexistante'}), 400
        
        # Determine transaction type based on customer
        today = datetime.now().strftime('%Y%m%d')
        is_client_comptoir = customer_id is None or customer_id == '' or customer_id == 'null'
        
        if is_client_comptoir:
            seq = next_sequence(conn, 'ticket_counter')
            doc_number = f'Ticket-{today}-{seq:04d}'
            doc_type = 'ticket'
        elif doc_type == 'bon_de_livraison':
            seq = next_sequence(conn, 'bl_counter')
            doc_number = f'BL-{today}-{seq:04d}'
        else:
            seq = next_sequence(conn, 'fac_counter')
            doc_number = f'FAC-{today}-{seq:04d}'
            doc_type = 'facture'
        
        # Load customer for invoice
        customer_row = None
        if customer_id and not is_client_comptoir:
            cid = _safe_int(customer_id, None)
            if cid:
                customer_row = conn.execute('SELECT * FROM customers WHERE id = ?', (cid,)).fetchone()
                if customer_row and customer_row['type'] != 'normal':
                    pricing_tier = customer_row['type']
        
        # Calculate totals
        subtotal = 0
        tax = 0
        discount = 0
        for item in items:
            product = conn.execute('SELECT * FROM products WHERE id = ?', (item['product_id'],)).fetchone()
            if product:
                item['unit_price'] = get_price_by_tier(dict(product), pricing_tier)
            unit_price = item.get('unit_price', 0) or 0
            item['unit_price'] = unit_price
            line_ht = item['quantity'] * unit_price
            line_tax = line_ht * 0.20
            subtotal += line_ht
            if apply_tax:
                tax += line_tax
            discount += line_ht * (item.get('discount_percent', 0) / 100)
    
        total = subtotal + (tax if apply_tax else 0) - discount
        if doc_type == 'bon_de_livraison':
            tax = 0
            total = subtotal - discount
        change_amount = max(0, tendered_amount - total) if payment_method == 'cash' else 0
    
        # Determine amount actually paid (credit logic)
        if is_credit:
            tendered = float(tendered_amount or 0)
            if tendered <= 0:
                amount_paid = 0
            elif tendered >= total:
                amount_paid = total
            else:
                amount_paid = tendered
        else:
            amount_paid = total
    
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
            tax_mult = 1.0 if doc_type == 'bon_de_livraison' else (1.20 if data.get('apply_tax', True) else 1.0)
            line_total = line_ht * tax_mult
        
            prod = conn.execute('SELECT name, sku FROM products WHERE id = ?', (product_id,)).fetchone()
            product_name = prod['name'] if prod else item.get('product_name', '')
            product_sku = prod['sku'] if prod else item.get('product_sku', '')
        
            conn.execute('''
                INSERT INTO pos_transaction_items (
                    transaction_id, product_id, product_name, product_sku,
                    quantity, unit_price, discount_percent, tax_rate, line_total
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ''', (trans_id, product_id, product_name, product_sku,
                  qty, unit_price, discount_pct, 0 if doc_type == 'bon_de_livraison' else (20 if apply_tax else 0), line_total))
        
            cur = conn.execute('''
                UPDATE products SET quantity = quantity - ? WHERE id = ? AND quantity >= ?
            ''', (qty, product_id, qty))
            if cur.rowcount == 0:
                current = conn.execute('SELECT quantity FROM products WHERE id = ?', (product_id,)).fetchone()
                stock_qty = current['quantity'] if current else 0
                conn.close()
                return jsonify({'error': f'Stock insuffisant pour {product_name}. Disponible: {stock_qty}, demandé: {qty}'}), 400
        
            conn.execute('''
                INSERT INTO stock_movements (product_id, type, quantity, note)
                VALUES (?, 'sale', ?, ?)
            ''', (product_id, qty, f'Vente POS: {doc_number}'))
    
        # Record net cash collected (amount_paid = total for non-credit sales)
        # Change is NOT recorded separately — net cash inflow already accounts for it.
        if payment_method == 'cash' and amount_paid > 0:
            conn.execute('''
                INSERT INTO pos_cash_movements (session_id, type, amount, reason, note)
                VALUES (?, 'in', ?, 'sale', ?)
            ''', (session_id, amount_paid, doc_number))
    
        # Record card payment in main account (only the amount actually collected)
        if payment_method == 'card' and amount_paid > 0:
            conn.execute('UPDATE main_account SET current_balance = current_balance + ? WHERE id = 1', (amount_paid,))
            conn.execute('''
                INSERT INTO main_account_transactions (type, amount, reason, reference_id, note)
                VALUES ('in', ?, 'card_payment', ?, ?)
            ''', (amount_paid, trans_id, f'Paiement carte: {doc_number}'))
    
        conn.commit()
    
        # Get customer name
        customer_name = 'Client Comptoir'
        if customer_id and not is_client_comptoir:
            cust = conn.execute('SELECT name FROM customers WHERE id = ?', (customer_id,)).fetchone()
            if cust:
                customer_name = cust['name']
    
        # Create invoice for all sales (tickets and factures)
        inv_id = None
        inv_status = None
        if is_client_comptoir:
            inv_status = 'ticket'
            conn.execute('''
                INSERT INTO invoices (
                    invoice_number, customer_id, warehouse_id, status, type,
                    subtotal, discount_total, tax_amount, total, 
                    payment_method, tendered_amount, change_given, amount_paid
                ) VALUES (?, NULL, ?, 'ticket', 'ticket', ?, ?, ?, ?, ?, ?, ?, ?)
            ''', (doc_number, session['warehouse_id'], subtotal, discount, tax, total,
                  payment_method, tendered_amount, change_amount, amount_paid))
            inv_id = conn.execute('SELECT last_insert_rowid()').fetchone()[0]
            conn.execute('UPDATE pos_transactions SET invoice_id = ? WHERE id = ?', (inv_id, trans_id))

            for item in items:
                qty = item.get('quantity', 1)
                uprice = item.get('unit_price', 0) or 0
                dpct = item.get('discount_percent', 0) or 0
                line_ht = qty * uprice * (1 - dpct / 100)
                tax_mult = 1.0 if doc_type == 'bon_de_livraison' else (1.20 if apply_tax else 1.0)
                line_total = line_ht * tax_mult
                pid = item.get('product_id') or item.get('id')
                prod = conn.execute('SELECT name, sku FROM products WHERE id = ?', (pid,)).fetchone()
                product_name = prod['name'] if prod else item.get('product_name', '')
                product_sku = prod['sku'] if prod else item.get('product_sku', '')
                conn.execute('''
                    INSERT INTO invoice_items (
                        invoice_id, product_id, product_name, product_sku,
                        quantity, unit_price, discount_percent, tax_rate, line_total
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ''', (inv_id, pid, product_name, product_sku, qty, uprice, dpct,
                      0 if doc_type == 'bon_de_livraison' else (20 if apply_tax else 0), line_total))
        else:
            inv_type = doc_type
            if doc_type == 'bon_de_livraison':
                inv_status = 'envoyee'
            elif is_credit:
                tend = float(tendered_amount or 0)
                if tend <= 0:
                    inv_status = 'envoyee'
                elif tend >= total:
                    inv_status = 'payee'
                else:
                    inv_status = 'partiellement_payee'
            else:
                inv_status = 'payee'
        
            if inv_status == 'payee':
                conn.execute('''
                    INSERT INTO invoices (
                        invoice_number, customer_id, warehouse_id, status, type,
                        subtotal, discount_total, tax_amount, total, 
                        paid_at, payment_method, tendered_amount, change_given, amount_paid
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?)
                ''', (doc_number, customer_id, session['warehouse_id'], inv_status, inv_type, subtotal, discount, tax, total,
                      payment_method, tendered_amount, change_amount, amount_paid))
            else:
                conn.execute('''
                    INSERT INTO invoices (
                        invoice_number, customer_id, warehouse_id, status, type,
                        subtotal, discount_total, tax_amount, total, 
                        payment_method, tendered_amount, change_given, amount_paid
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ''', (doc_number, customer_id, session['warehouse_id'], inv_status, inv_type, subtotal, discount, tax, total,
                      payment_method, tendered_amount, change_amount, amount_paid))
            inv_id = conn.execute('SELECT last_insert_rowid()').fetchone()[0]
        
            conn.execute('UPDATE pos_transactions SET invoice_id = ? WHERE id = ?', (inv_id, trans_id))
        
            for item in items:
                qty = item.get('quantity', 1)
                uprice = item.get('unit_price', 0) or 0
                dpct = item.get('discount_percent', 0) or 0
                line_ht = qty * uprice * (1 - dpct / 100)
                tax_mult = 1.0 if doc_type == 'bon_de_livraison' else (1.20 if apply_tax else 1.0)
                line_total = line_ht * tax_mult
                pid = item.get('product_id') or item.get('id')
                prod = conn.execute('SELECT name, sku FROM products WHERE id = ?', (pid,)).fetchone()
                product_name = prod['name'] if prod else item.get('product_name', '')
                product_sku = prod['sku'] if prod else item.get('product_sku', '')
                conn.execute('''
                    INSERT INTO invoice_items (
                        invoice_id, product_id, product_name, product_sku,
                        quantity, unit_price, discount_percent, tax_rate, line_total
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ''', (inv_id, pid, product_name, product_sku,
                      qty, uprice, dpct, 0 if doc_type == 'bon_de_livraison' else (20 if apply_tax else 0), line_total))
    
        conn.commit()
    except sqlite3.Error as e:
        conn.rollback()
        return jsonify({'error': 'Erreur de base de données'}), 500
    except Exception as e:
        conn.rollback()
        return jsonify({'error': 'Erreur inattendue'}), 500
    finally:
        conn.close()

    # --- Auto-print hook ---
    print_status = None
    print_error = None
    try:
        _pconn = get_db()
        printer_config = _pconn.execute('SELECT * FROM printer_settings WHERE id = 1').fetchone()
        _pconn.close()
        if printer_config and printer_config['auto_print']:
            ticket_data = {
                'ticket_number': doc_number,
                'created_at': datetime.now().isoformat(),
                'session_number': session['session_number'] if session else '',
                'items': [{
                    'product_name': it.get('product_name', ''),
                    'product_sku': it.get('product_sku', ''),
                    'quantity': it.get('quantity', 1),
                    'unit_price': it.get('unit_price', 0),
                    'discount_percent': it.get('discount_percent', 0),
                    'line_total': it.get('line_total', 0),
                } for it in items],
                'subtotal': subtotal,
                'discount_total': discount,
                'tax_amount': tax,
                'total': total,
                'payment_method': payment_method,
                'tendered_amount': tendered_amount,
                'change_amount': change_amount,
                'customer_name': customer_name if not is_client_comptoir else 'Client Comptoir',
                'register_name': session['register_name'] if session and session['register_name'] else (session['cashier_name'] if session else ''),
            }
            print_result = auto_print_async(ticket_data, dict(printer_config))
            if print_result:
                print_status = print_result.get('print_status')
                print_error = print_result.get('print_error')
    except Exception as e:
        print_status = 'error'
        print_error = 'Erreur d\'impression'

    register_name = session['register_name'] if session['register_name'] else (session['cashier_name'] or '')
    stock_updates = []
    for item in items:
        stock_updates.append({
            'product_id': item.get('product_id') or item.get('id'),
            'product_name': item.get('product_name', ''),
            'quantity': item.get('quantity', 1)
        })
    broadcast_event('transaction', {
        'session_id': session_id,
        'register_name': register_name,
        'total': total,
        'document_number': doc_number,
        'payment_method': payment_method
    })
    for update in stock_updates:
        broadcast_event('stock-update', {
            'product_id': update['product_id'],
            'register_name': register_name,
            'timestamp': datetime.now().isoformat()
        })

    return jsonify({
        'success': True,
        'document_number': doc_number,
        'document_id': inv_id,
        'document_type': doc_type,
        'document_status': inv_status,
        'total': total,
        'change_amount': change_amount,
        'customer_name': customer_name,
        'print_status': print_status,
        'print_error': print_error,
    })

@app.route('/api/pos/cash-movements', methods=['GET'])
def get_pos_cash_movements():
    """Get cash movements — all registers or filtered by session_id"""
    conn = get_db()
    session_id = request.args.get('session_id')
    limit = _safe_int(request.args.get('limit', 50), 50)
    
    if session_id:
        movements = conn.execute('''
            SELECT m.*, r.name as register_name, s.session_number
            FROM pos_cash_movements m
            LEFT JOIN pos_sessions s ON m.session_id = s.id
            LEFT JOIN pos_registers r ON s.register_id = r.id
            WHERE m.session_id = ?
            ORDER BY m.created_at DESC
        ''', (session_id,)).fetchall()
    else:
        movements = conn.execute('''
            SELECT m.*, r.name as register_name, s.session_number
            FROM pos_cash_movements m
            LEFT JOIN pos_sessions s ON m.session_id = s.id
            LEFT JOIN pos_registers r ON s.register_id = r.id
            ORDER BY m.created_at DESC
            LIMIT ?
        ''', (limit,)).fetchall()
    
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
    session_id = data.get('session_id')
    
    conn = get_db()
    
    if session_id:
        session = conn.execute("SELECT id FROM pos_sessions WHERE id = ? AND status = 'open'", (session_id,)).fetchone()
    else:
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
    movement = conn.execute('''
        SELECT m.*, r.name as register_name, s.session_number
        FROM pos_cash_movements m
        LEFT JOIN pos_sessions s ON m.session_id = s.id
        LEFT JOIN pos_registers r ON s.register_id = r.id
        WHERE m.id = ?
    ''', (movement_id,)).fetchone()
    
    conn.close()
    return jsonify({'success': True, 'movement': dict(movement)})

@app.route('/api/pos/customers', methods=['GET'])
def get_pos_customers():
    """Get customers for POS (with discount info)"""
    conn = get_db()
    customers = conn.execute('''
        SELECT id, name, client_code, discount_rate, type
        FROM customers 
        WHERE is_active = 1
        ORDER BY name
    ''').fetchall()
    conn.close()
    return jsonify([dict(c) for c in customers])

@app.route('/api/pos/transactions/recent', methods=['GET'])
def get_pos_recent_transactions():
    """Get recent transactions: POS tickets + paid invoices"""
    conn = get_db()
    limit = _safe_int(request.args.get('limit', 20), 20)
    session_id = request.args.get('session_id')
    
    if session_id:
        transactions = conn.execute('''
            SELECT t.*, c.name as customer_name, 'ticket' as source,
                   r.name as register_name, s.session_number,
                   i.id as invoice_id, i.type as invoice_type
            FROM pos_transactions t
            LEFT JOIN customers c ON t.customer_id = c.id
            LEFT JOIN pos_sessions s ON t.session_id = s.id
            LEFT JOIN pos_registers r ON s.register_id = r.id
            LEFT JOIN invoices i ON t.invoice_id = i.id
            WHERE t.session_id = ?
            ORDER BY t.created_at DESC
        ''', (session_id,)).fetchall()
    else:
        transactions = conn.execute('''
            SELECT t.*, c.name as customer_name, 'ticket' as source,
                   r.name as register_name, s.session_number,
                   i.id as invoice_id, i.type as invoice_type
            FROM pos_transactions t
            LEFT JOIN customers c ON t.customer_id = c.id
            LEFT JOIN pos_sessions s ON t.session_id = s.id
            LEFT JOIN pos_registers r ON s.register_id = r.id
            LEFT JOIN invoices i ON t.invoice_id = i.id
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
        SELECT p.id, p.name, p.sku, p.price, p.price_base, p.quantity,
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
        @font-face {{ font-family: 'DejaVu Sans'; src: url('/static/fonts/DejaVuSans.ttf') format('truetype'); font-weight: normal; font-style: normal; font-display: swap; }}
        @font-face {{ font-family: 'DejaVu Sans'; src: url('/static/fonts/DejaVuSans-Bold.ttf') format('truetype'); font-weight: bold; font-style: normal; font-display: swap; }}
        * {{ margin:0; padding:0; box-sizing:border-box; }}
        body {{ font-family:'DejaVu Sans','Courier New',monospace; font-size:14px; font-weight:900; width:300px; margin:0 auto; padding:8px; color:#000; }}
        .header {{ text-align:center; margin-bottom:12px; border-bottom:1px dashed #000; padding-bottom:8px; }}
        .ticket-title {{ font-size:22px; font-weight:900; margin:8px 0; }}
        .ticket-number {{ font-size:14px; font-weight:900; border:1px solid #000; padding:4px 8px; display:inline-block; margin-bottom:8px; }}
        .info {{ font-size:12px; font-weight:900; margin:2px 0; }}
        table {{ width:100%; border-collapse:collapse; margin:8px 0; font-size:12px; }}
        th {{ text-align:left; border-bottom:1px dashed #000; padding:3px 0; font-weight:900; }}
        td {{ padding:2px 0; font-weight:900; }}
        .item-name {{ max-width:150px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }}
        .qty {{ width:30px; text-align:center; }}
        .price {{ width:65px; text-align:right; }}
        .total {{ width:65px; text-align:right; font-weight:900; }}
        .divider {{ border-bottom:1px dashed #000; margin:6px 0; }}
        .totals {{ margin-top:8px; }}
        .totals-row {{ display:flex; justify-content:space-between; padding:2px 0; font-weight:900; }}
        .grand-total {{ font-size:20px; font-weight:900; border-top:2px solid #000; padding-top:6px; margin-top:6px; }}
        .payment {{ margin-top:8px; font-size:12px; }}
        .footer {{ text-align:center; margin-top:16px; border-top:1px dashed #000; padding-top:8px; }}
        .signature {{ font-size:12px; margin-top:12px; font-weight:900; }}
        .ticket-logo {{ height:55px; margin-bottom:10px; }}
        .btn {{ background:#333; color:#fff; border:none; padding:8px 16px; cursor:pointer; font-size:13px; font-weight:900; margin-top:10px; }}
        .btn-success {{ background:#2563eb; }}
        .btn-secondary {{ background:#666; }}
        .btn:disabled {{ opacity:0.6; cursor:not-allowed; }}
        #printStatus {{ font-size:12px; margin-top:8px; text-align:center; font-weight:900; }}
        @media print {{ .btn {{ display:none; }} }}
    </style>
</head>
<body>
    <div class="header">
        <img src="/static/img/logo.png" class="ticket-logo" alt="Bibliotheque Badr">
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
                <td class="item-name" dir="auto">{_reshape(_n(item.get('product_name'), 'Article'))}</td>
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
    
    <div style="text-align:center;">
        <button class="btn btn-success" onclick="printTicket(this)">🖨️ Imprimer</button>
        <button class="btn btn-secondary" onclick="window.print()" style="margin-top:5px;">PDF / Enregistrer</button>
        <div id="printStatus"></div>
    </div>
    <script>
    async function printTicket(btn) {{
        btn.disabled = true;
        var orig = btn.textContent;
        btn.textContent = 'Impression...';
        var status = document.getElementById('printStatus');
        status.textContent = '';
        status.style.color = '';
        try {{
            var res = await fetch('/api/pos/tickets/{ticket_number}/print', {{method: 'POST'}});
            var data = await res.json();
            if (data.print_status === 'success') {{
                status.style.color = '#22c55e';
                status.textContent = '✓ Impression réussie';
            }} else {{
                status.style.color = '#ef4444';
                status.textContent = '✗ Erreur: ' + (data.print_error || 'échec inconnu');
            }}
        }} catch(e) {{
            status.style.color = '#ef4444';
            status.textContent = '✗ Erreur réseau: ' + e.message;
        }}
        btn.disabled = false;
        btn.textContent = orig;
    }}
    </script>
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

# ─── POS Registers CRUD ───────────────────────────────────────

@app.route('/api/pos/registers', methods=['GET'])
def get_pos_registers():
    conn = get_db()
    registers = conn.execute(
        'SELECT * FROM pos_registers WHERE is_active = 1 ORDER BY id LIMIT 2'
    ).fetchall()
    conn.close()
    return jsonify([dict(r) for r in registers])


@app.route('/api/pos/registers', methods=['POST'])
def create_pos_register():
    data = request.json or {}
    name = data.get('name', '').strip()
    code = data.get('code', '').strip()
    if not name or not code:
        return jsonify({'error': 'Nom et code requis'}), 400
    conn = get_db()
    try:
        conn.execute(
            'INSERT INTO pos_registers (name, code) VALUES (?, ?)',
            (name, code)
        )
        conn.commit()
        reg_id = conn.execute('SELECT last_insert_rowid()').fetchone()[0]
        reg = conn.execute('SELECT * FROM pos_registers WHERE id = ?', (reg_id,)).fetchone()
        conn.close()
        return jsonify({'success': True, 'register': dict(reg)})
    except sqlite3.IntegrityError:
        conn.close()
        return jsonify({'error': 'Nom ou code déjà utilisé'}), 400


@app.route('/api/pos/registers/<int:reg_id>', methods=['PUT'])
def update_pos_register(reg_id):
    data = request.json or {}
    conn = get_db()
    reg = conn.execute('SELECT * FROM pos_registers WHERE id = ?', (reg_id,)).fetchone()
    if not reg:
        conn.close()
        return jsonify({'error': 'Caisse non trouvée'}), 404
    name = data.get('name', reg['name']).strip()
    is_active = data.get('is_active', reg['is_active'])
    try:
        conn.execute(
            'UPDATE pos_registers SET name = ?, is_active = ? WHERE id = ?',
            (name, is_active, reg_id)
        )
        conn.commit()
        reg = conn.execute('SELECT * FROM pos_registers WHERE id = ?', (reg_id,)).fetchone()
        conn.close()
        return jsonify({'success': True, 'register': dict(reg)})
    except sqlite3.IntegrityError:
        conn.close()
        return jsonify({'error': 'Nom déjà utilisé'}), 400


@app.route('/api/pos/registers/<int:reg_id>', methods=['DELETE'])
def delete_pos_register(reg_id):
    conn = get_db()
    open_session = conn.execute(
        'SELECT id FROM pos_sessions WHERE register_id = ? AND status = ?',
        (reg_id, 'open')
    ).fetchone()
    if open_session:
        conn.close()
        return jsonify({'error': 'Impossible de supprimer une caisse avec une session ouverte'}), 400
    conn.execute('UPDATE pos_registers SET is_active = 0 WHERE id = ?', (reg_id,))
    conn.commit()
    conn.close()
    return jsonify({'success': True})


# ─── POS Register Status (for dashboard) ─────────────────────

@app.route('/api/pos/sessions/active', methods=['GET'])
def get_active_sessions():
    conn = get_db()
    sessions = conn.execute('''
        SELECT s.*, r.name as register_name
        FROM pos_sessions s
        LEFT JOIN pos_registers r ON s.register_id = r.id
        WHERE s.status = 'open'
        ORDER BY s.opened_at DESC
    ''').fetchall()
    result = []
    for s in sessions:
        row = dict(s)
        total_sales = conn.execute('''
            SELECT COALESCE(SUM(total), 0) FROM pos_transactions WHERE session_id = ?
        ''', (s['id'],)).fetchone()[0]
        nb_trans = conn.execute('''
            SELECT COUNT(*) FROM pos_transactions WHERE session_id = ?
        ''', (s['id'],)).fetchone()[0]
        cash_in = conn.execute('''
            SELECT COALESCE(SUM(amount), 0) FROM pos_cash_movements WHERE session_id = ? AND type = 'in'
        ''', (s['id'],)).fetchone()[0]
        cash_out = conn.execute('''
            SELECT COALESCE(SUM(amount), 0) FROM pos_cash_movements WHERE session_id = ? AND type = 'out'
        ''', (s['id'],)).fetchone()[0]
        row['total_sales'] = total_sales
        row['nb_transactions'] = nb_trans
        row['cash_balance'] = s['opening_cash'] + cash_in - cash_out
        result.append(row)
    conn.close()
    return jsonify(result)


# ─── SSE Events ──────────────────────────────────────────────

@app.route('/api/events')
def sse_events():
    client_queue = queue.Queue()
    sse_clients.append(client_queue)
    def event_stream():
        try:
            while True:
                payload = client_queue.get()
                yield payload
        except GeneratorExit:
            if client_queue in sse_clients:
                sse_clients.remove(client_queue)
    return Response(event_stream(), mimetype='text/event-stream')


def parse_args():
    parser = argparse.ArgumentParser(description='StockPro - Gestion de Stock')
    parser.add_argument('--host', type=str, default='127.0.0.1',
                        help='Adresse d\'écoute (0.0.0.0 = réseau local)')
    parser.add_argument('--port', type=int, default=5001, help='Port du serveur')
    parser.add_argument('--data-dir', type=str, default=None,
                        help='Dossier de données (DB, logs)')
    parser.add_argument('--open-browser', action='store_true',
                        help='Ouvrir le navigateur au démarrage')
    parser.add_argument('--service', action='store_true',
                        help='Mode service Windows (pas de console)')
    return parser.parse_args()

if __name__ == '__main__':
    args = parse_args()

    if args.data_dir:
        os.makedirs(args.data_dir, exist_ok=True)
        from routes import db as _routes_db
        db_path = os.path.join(args.data_dir, 'stock.db')
        os.environ['STOCKPRO_DB_PATH'] = db_path
        os.environ['STOCKPRO_DATA_DIR'] = args.data_dir
        _routes_db.STOCKPRO_DATA_DIR = args.data_dir
        _routes_db.DB_NAME = db_path
        _routes_db.CATALOG_DB = os.path.abspath(db_path)

    init_db()

    if platform.system() == 'Windows':
        payload = load_license()
        if payload:
            print(f"[LICENCE] Valide — {payload.get('client', 'inconnu')} jusqu'au "
                  f"{datetime.fromtimestamp(payload['exp'], tz=timezone.utc).strftime('%d/%m/%Y')}")
        else:
            print("[LICENCE] Aucune licence valide — ouvrez /license pour activer")
    else:
        print("[LICENCE] Mode développement (macOS) — admin panel actif sur /license")

    if args.service:
        pid_file = os.path.join(args.data_dir or os.getcwd(), 'stockpro.pid')
        with open(pid_file, 'w') as f:
            f.write(str(os.getpid()))
        log_file = os.path.join(args.data_dir or os.getcwd(), 'stockpro.log')
        sys.stdout = open(log_file, 'a')
        sys.stderr = open(log_file, 'a')

    url = f'http://localhost:{args.port}'
    print(f'\n=== StockPro prêt ===')
    print(f'Accès local  : {url}')
    if args.host == '0.0.0.0':
        print(f'Accès réseau : http://<IP_DU_PC>:{args.port}')
    print(f'=====================\n')

    if args.open_browser:
        def _open_browser():
            import time
            time.sleep(1.5)
            webbrowser.open(url)
        import threading
        threading.Thread(target=_open_browser, daemon=True).start()

    app.run(host=args.host, debug=False, port=args.port, threaded=True)
