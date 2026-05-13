from flask import Blueprint, request, jsonify, Response
from contextlib import contextmanager
import sqlite3
import csv
from io import StringIO
from datetime import datetime

DB_NAME = 'stock.db'

@contextmanager
def get_db():
    conn = sqlite3.connect(DB_NAME, check_same_thread=False, timeout=30)
    conn.row_factory = sqlite3.Row
    conn.execute('PRAGMA journal_mode=WAL')
    conn.execute('PRAGMA busy_timeout=30000')
    conn.execute('PRAGMA foreign_keys=ON')
    try:
        yield conn
    finally:
        conn.close()

def validate_id(value):
    if value is None:
        return None
    if isinstance(value, str) and value.isdigit():
        return int(value)
    if isinstance(value, int) and value > 0:
        return value
    return None

kpis_bp = Blueprint('kpis', __name__)

@kpis_bp.route('/api/kpis', methods=['GET'])
def get_kpis():
    warehouse_id = validate_id(request.args.get('warehouse_id'))
    period = int(request.args.get('period', 30))
    category = request.args.get('category', '')
    date_start = request.args.get('date_start')
    date_end = request.args.get('date_end')
    
    with get_db() as conn:
        if warehouse_id and category:
            all_params = (warehouse_id, category)
            total_products = conn.execute('SELECT COUNT(*) FROM products WHERE warehouse_id=? AND category=?', all_params).fetchone()[0]
            total_value = conn.execute('SELECT COALESCE(SUM(quantity * price), 0) FROM products WHERE warehouse_id=? AND category=?', all_params).fetchone()[0]
            avg_price = conn.execute('SELECT COALESCE(AVG(price), 0) FROM products WHERE warehouse_id=? AND category=?', all_params).fetchone()[0]
            low_stock = conn.execute('SELECT COUNT(*) FROM products WHERE quantity <= min_quantity AND warehouse_id=? AND category=?', all_params).fetchone()[0]
            out_of_stock = conn.execute('SELECT COUNT(*) FROM products WHERE (quantity < 0 OR quantity IS NULL) AND warehouse_id=? AND category=?', all_params).fetchone()[0]
        elif warehouse_id:
            total_products = conn.execute('SELECT COUNT(*) FROM products WHERE warehouse_id=?', (warehouse_id,)).fetchone()[0]
            total_value = conn.execute('SELECT COALESCE(SUM(quantity * price), 0) FROM products WHERE warehouse_id=?', (warehouse_id,)).fetchone()[0]
            avg_price = conn.execute('SELECT COALESCE(AVG(price), 0) FROM products WHERE warehouse_id=?', (warehouse_id,)).fetchone()[0]
            low_stock = conn.execute('SELECT COUNT(*) FROM products WHERE quantity <= min_quantity AND warehouse_id=?', (warehouse_id,)).fetchone()[0]
            out_of_stock = conn.execute('SELECT COUNT(*) FROM products WHERE (quantity < 0 OR quantity IS NULL) AND warehouse_id=?', (warehouse_id,)).fetchone()[0]
        elif category:
            total_products = conn.execute('SELECT COUNT(*) FROM products WHERE category=?', (category,)).fetchone()[0]
            total_value = conn.execute('SELECT COALESCE(SUM(quantity * price), 0) FROM products WHERE category=?', (category,)).fetchone()[0]
            avg_price = conn.execute('SELECT COALESCE(AVG(price), 0) FROM products WHERE category=?', (category,)).fetchone()[0]
            low_stock = conn.execute('SELECT COUNT(*) FROM products WHERE quantity <= min_quantity AND category=?', (category,)).fetchone()[0]
            out_of_stock = conn.execute('SELECT COUNT(*) FROM products WHERE (quantity < 0 OR quantity IS NULL) AND category=?', (category,)).fetchone()[0]
        else:
            total_products = conn.execute('SELECT COUNT(*) FROM products').fetchone()[0]
            total_value = conn.execute('SELECT COALESCE(SUM(quantity * price), 0) FROM products').fetchone()[0]
            avg_price = conn.execute('SELECT COALESCE(AVG(price), 0) FROM products').fetchone()[0]
            low_stock = conn.execute('SELECT COUNT(*) FROM products WHERE quantity <= min_quantity').fetchone()[0]
            out_of_stock = conn.execute('SELECT COUNT(*) FROM products WHERE (quantity < 0 OR quantity IS NULL)').fetchone()[0]
        
        if date_start and date_end:
            date_params = (date_start, date_end)
            in_movements = conn.execute("SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE type='in' AND created_at>=? AND created_at<=?", date_params).fetchone()[0]
            out_movements = conn.execute("SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE type='out' AND created_at>=? AND created_at<=?", date_params).fetchone()[0]
        elif date_start:
            in_movements = conn.execute("SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE type='in' AND created_at>=?", (date_start,)).fetchone()[0]
            out_movements = conn.execute("SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE type='out' AND created_at>=?", (date_start,)).fetchone()[0]
        elif date_end:
            in_movements = conn.execute("SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE type='in' AND created_at<=?", (date_end,)).fetchone()[0]
            out_movements = conn.execute("SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE type='out' AND created_at<=?", (date_end,)).fetchone()[0]
        else:
            in_movements = conn.execute("SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE type='in' AND created_at>=date('now', ?)", (f'-{period} days',)).fetchone()[0]
            out_movements = conn.execute("SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE type='out' AND created_at>=date('now', ?)", (f'-{period} days',)).fetchone()[0]
        
        today_movements = conn.execute("SELECT COUNT(*) FROM stock_movements WHERE date(created_at) = date('now')").fetchone()[0]
        rotation_rate = (out_movements / total_products * 100) if total_products > 0 else 0
        
        return jsonify({
            'total_products': total_products,
            'total_value': round(total_value, 2),
            'avg_price': round(avg_price, 2),
            'low_stock': low_stock,
            'out_of_stock': out_of_stock,
            'in_movements': in_movements,
            'out_movements': out_movements,
            'today_movements': today_movements,
            'rotation_rate': round(rotation_rate, 1),
            'period': period,
            'date_start': date_start,
            'date_end': date_end
        })

@kpis_bp.route('/api/kpis/dashboard', methods=['GET'])
def get_dashboard_kpis():
    warehouse_id = validate_id(request.args.get('warehouse_id'))
    period = int(request.args.get('period', 30))
    
    with get_db() as conn:
        if warehouse_id:
            total_products = conn.execute('SELECT COUNT(*) FROM products WHERE warehouse_id=?', (warehouse_id,)).fetchone()[0]
            total_value = conn.execute('SELECT COALESCE(SUM(quantity * price), 0) FROM products WHERE warehouse_id=?', (warehouse_id,)).fetchone()[0]
            low_stock = conn.execute('SELECT COUNT(*) FROM products WHERE quantity < min_quantity AND is_deleted = 0 AND warehouse_id=?', (warehouse_id,)).fetchone()[0]
            out_of_stock = conn.execute('SELECT COUNT(*) FROM products WHERE (quantity < 0 OR quantity IS NULL) AND is_deleted = 0 AND warehouse_id=?', (warehouse_id,)).fetchone()[0]
            products_to_order = conn.execute('SELECT id, name, sku, quantity, min_quantity, (min_quantity - quantity) as needed FROM products WHERE quantity < min_quantity AND is_deleted = 0 AND warehouse_id=? ORDER BY needed DESC LIMIT 10', (warehouse_id,)).fetchall()
        else:
            total_products = conn.execute('SELECT COUNT(*) FROM products').fetchone()[0]
            total_value = conn.execute('SELECT COALESCE(SUM(quantity * price), 0) FROM products').fetchone()[0]
            low_stock = conn.execute('SELECT COUNT(*) FROM products WHERE quantity < min_quantity AND is_deleted = 0').fetchone()[0]
            out_of_stock = conn.execute('SELECT COUNT(*) FROM products WHERE (quantity < 0 OR quantity IS NULL) AND is_deleted = 0').fetchone()[0]
            products_to_order = conn.execute('SELECT id, name, sku, quantity, min_quantity, (min_quantity - quantity) as needed FROM products WHERE quantity < min_quantity AND is_deleted = 0 ORDER BY needed DESC LIMIT 10').fetchall()
        
        avg_price = total_value / total_products if total_products > 0 else 0
        
        in_movements = conn.execute("SELECT COALESCE(SUM(quantity), 0) as qty FROM stock_movements WHERE type='in' AND created_at>=date('now', ?)", (f'-{period} days',)).fetchone()[0]
        out_movements = conn.execute("SELECT COALESCE(SUM(quantity), 0) as qty FROM stock_movements WHERE type='out' AND created_at>=date('now', ?)", (f'-{period} days',)).fetchone()[0]
        prev_period = period * 2
        prev_in = conn.execute("SELECT COALESCE(SUM(quantity), 0) as qty FROM stock_movements WHERE type='in' AND created_at>=date('now', ?) AND created_at<date('now', ?)", (f'-{prev_period} days', f'-{period} days')).fetchone()[0]
        prev_out = conn.execute("SELECT COALESCE(SUM(quantity), 0) as qty FROM stock_movements WHERE type='out' AND created_at>=date('now', ?) AND created_at<date('now', ?)", (f'-{prev_period} days', f'-{period} days')).fetchone()[0]
        
        today_movements = conn.execute("SELECT COUNT(*) FROM stock_movements WHERE date(created_at) = date('now')").fetchone()[0]
        avg_daily_out = out_movements / period if period > 0 else 1
        dio = total_products / avg_daily_out if avg_daily_out > 0 else 0
        rotation_rate = (out_movements / total_products * 100) if total_products > 0 else 0
        prev_rotation = (prev_out / total_products * 100) if total_products > 0 else 0
        in_trend = ((in_movements - prev_in) / prev_in * 100) if prev_in > 0 else 0
        out_trend = ((out_movements - prev_out) / prev_out * 100) if prev_out > 0 else 0
        
        if warehouse_id:
            expiring_soon = conn.execute("SELECT COUNT(*) FROM products WHERE expiry_date IS NOT NULL AND expiry_date<=date('now','+30 days') AND warehouse_id=?", (warehouse_id,)).fetchone()[0]
        else:
            expiring_soon = conn.execute("SELECT COUNT(*) FROM products WHERE expiry_date IS NOT NULL AND expiry_date<=date('now','+30 days')").fetchone()[0]
        
        total_alerts = low_stock + out_of_stock + expiring_soon
        
        return jsonify({
            'total_products': total_products,
            'total_value': round(total_value, 2),
            'avg_price': round(avg_price, 2),
            'low_stock': low_stock,
            'out_of_stock': out_of_stock,
            'products_to_order': [dict(p) for p in products_to_order],
            'expiring_soon': expiring_soon,
            'total_alerts': total_alerts,
            'in_movements': in_movements,
            'out_movements': out_movements,
            'today_movements': today_movements,
            'rotation_rate': round(rotation_rate, 1),
            'dio': round(dio, 1),
            'in_trend': round(in_trend, 1),
            'out_trend': round(out_trend, 1),
            'period': period
        })

@kpis_bp.route('/api/kpis/alertes', methods=['GET'])
def get_alertes():
    warehouse_id = validate_id(request.args.get('warehouse_id'))
    
    with get_db() as conn:
        if warehouse_id:
            low_stock = conn.execute('SELECT id, name, sku, quantity, min_quantity, price, (min_quantity - quantity) as needed FROM products WHERE quantity < min_quantity AND is_deleted = 0 AND warehouse_id=? ORDER BY needed DESC LIMIT 10', (warehouse_id,)).fetchall()
            out_of_stock = conn.execute('SELECT id, name, sku, min_quantity, price FROM products WHERE (quantity < 0 OR quantity IS NULL) AND is_deleted = 0 AND warehouse_id=? ORDER BY min_quantity DESC LIMIT 10', (warehouse_id,)).fetchall()
            expiring = conn.execute('SELECT id, name, lot_number, expiry_date, quantity, CAST(julianday(expiry_date) - julianday(\'now\') AS INTEGER) as days_left FROM products WHERE expiry_date IS NOT NULL AND expiry_date <= date(\'now\', \'+30 days\') AND expiry_date >= date(\'now\') AND warehouse_id=? ORDER BY expiry_date LIMIT 10', (warehouse_id,)).fetchall()
            inactive = conn.execute('SELECT p.id, p.name, p.quantity, p.price, MAX(m.created_at) as last_movement FROM products p LEFT JOIN stock_movements m ON p.id = m.product_id WHERE p.warehouse_id=? GROUP BY p.id HAVING MAX(m.created_at) IS NULL OR MAX(m.created_at) < date(\'now\', \'-90 days\') LIMIT 10', (warehouse_id,)).fetchall()
        else:
            low_stock = conn.execute('SELECT id, name, sku, quantity, min_quantity, price, (min_quantity - quantity) as needed FROM products WHERE quantity < min_quantity AND is_deleted = 0 ORDER BY needed DESC LIMIT 10').fetchall()
            out_of_stock = conn.execute('SELECT id, name, sku, min_quantity, price FROM products WHERE (quantity < 0 OR quantity IS NULL) AND is_deleted = 0 ORDER BY min_quantity DESC LIMIT 10').fetchall()
            expiring = conn.execute('SELECT id, name, lot_number, expiry_date, quantity, CAST(julianday(expiry_date) - julianday(\'now\') AS INTEGER) as days_left FROM products WHERE expiry_date IS NOT NULL AND expiry_date <= date(\'now\', \'+30 days\') AND expiry_date >= date(\'now\') ORDER BY expiry_date LIMIT 10').fetchall()
            inactive = conn.execute('SELECT p.id, p.name, p.quantity, p.price, MAX(m.created_at) as last_movement FROM products p LEFT JOIN stock_movements m ON p.id = m.product_id GROUP BY p.id HAVING MAX(m.created_at) IS NULL OR MAX(m.created_at) < date(\'now\', \'-90 days\') LIMIT 10').fetchall()
        
        return jsonify({
            'low_stock': [dict(p) for p in low_stock],
            'out_of_stock': [dict(p) for p in out_of_stock],
            'expiring': [dict(p) for p in expiring],
            'inactive': [dict(p) for p in inactive]
        })

@kpis_bp.route('/api/stats', methods=['GET'])
def get_stats():
    warehouse_id = validate_id(request.args.get('warehouse_id'))
    
    with get_db() as conn:
        if warehouse_id:
            total = conn.execute('SELECT COUNT(*) as count FROM products WHERE warehouse_id=?', (warehouse_id,)).fetchone()[0]
            low_stock = conn.execute('SELECT COUNT(*) as count FROM products WHERE quantity <= min_quantity AND warehouse_id=?', (warehouse_id,)).fetchone()[0]
            total_value = conn.execute('SELECT COALESCE(SUM(quantity * price), 0) as total FROM products WHERE warehouse_id=?', (warehouse_id,)).fetchone()[0]
            out_of_stock = conn.execute('SELECT COUNT(*) as count FROM products WHERE (quantity < 0 OR quantity IS NULL) AND warehouse_id=?', (warehouse_id,)).fetchone()[0]
        else:
            total = conn.execute('SELECT COUNT(*) as count FROM products').fetchone()[0]
            low_stock = conn.execute('SELECT COUNT(*) as count FROM products WHERE quantity <= min_quantity').fetchone()[0]
            total_value = conn.execute('SELECT COALESCE(SUM(quantity * price), 0) as total FROM products').fetchone()[0]
            out_of_stock = conn.execute('SELECT COUNT(*) as count FROM products WHERE (quantity < 0 OR quantity IS NULL)').fetchone()[0]
        
        warehouses_count = conn.execute('SELECT COUNT(*) FROM warehouses').fetchone()[0]
        
        return jsonify({
            'total': total,
            'low_stock': low_stock,
            'total_value': total_value,
            'out_of_stock': out_of_stock,
            'warehouses_count': warehouses_count
        })