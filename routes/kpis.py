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
        where_parts = []
        params = []
        if warehouse_id:
            where_parts.append('warehouse_id = ?')
            params.append(warehouse_id)
        if category:
            where_parts.append('category = ?')
            params.append(category)
        
        where_clause = ' AND '.join(where_parts) if where_parts else '1=1'
        where_sql = f'WHERE {where_clause}'
        
        date_filter = ""
        date_params = []
        if date_start:
            date_filter += " AND created_at >= ?"
            date_params.append(date_start)
        if date_end:
            date_filter += " AND created_at <= ?"
            date_params.append(date_end)
        if not date_start and not date_end:
            date_filter = "AND created_at >= date('now', ?)"
            date_params.append(f'-{period} days')
        
        total_products = conn.execute(f'SELECT COUNT(*) FROM products {where_sql}', params).fetchone()[0]
        total_value = conn.execute(f'SELECT COALESCE(SUM(quantity * price), 0) FROM products {where_sql}', params).fetchone()[0]
        avg_price = conn.execute(f'SELECT COALESCE(AVG(price), 0) FROM products {where_sql}', params).fetchone()[0]
        
        params_low = [warehouse_id] if warehouse_id else []
        low_stock = conn.execute(
            f'SELECT COUNT(*) FROM products WHERE quantity <= min_quantity AND {where_clause}', params_low
        ).fetchone()[0]
        out_of_stock = conn.execute(
            f'SELECT COUNT(*) FROM products WHERE (quantity < 0 OR quantity IS NULL) AND {where_clause}', params_low
        ).fetchone()[0]
        
        in_movements = conn.execute(f'''
            SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE type = 'in' {date_filter}
        ''', date_params).fetchone()[0]
        
        out_movements = conn.execute(f'''
            SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE type = 'out' {date_filter}
        ''', date_params).fetchone()[0]
        
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
        params = [warehouse_id] if warehouse_id else []
        where_clause = 'WHERE warehouse_id = ?' if warehouse_id else ''
        prev_period = period * 2
        
        total_products = conn.execute(f'SELECT COUNT(*) FROM products {where_clause}', params).fetchone()[0]
        total_value = conn.execute(f'SELECT COALESCE(SUM(quantity * price), 0) FROM products {where_clause}', params).fetchone()[0]
        avg_price = total_value / total_products if total_products > 0 else 0
        
        params_low = [warehouse_id] if warehouse_id else []
        low_stock = conn.execute(
            f'SELECT COUNT(*) FROM products WHERE quantity < min_quantity AND is_deleted = 0 {"AND warehouse_id = ?" if warehouse_id else ""}',
            params_low
        ).fetchone()[0]
        out_of_stock = conn.execute(
            f'SELECT COUNT(*) FROM products WHERE (quantity < 0 OR quantity IS NULL) AND is_deleted = 0 {"AND warehouse_id = ?" if warehouse_id else ""}',
            params_low
        ).fetchone()[0]
        
        products_to_order = conn.execute(f'''
            SELECT id, name, sku, quantity, min_quantity, (min_quantity - quantity) as needed
            FROM products
            WHERE quantity < min_quantity AND is_deleted = 0 {"AND warehouse_id = ?" if warehouse_id else ""}
            ORDER BY needed DESC LIMIT 10
        ''', params_low).fetchall()
        
        in_movements = conn.execute(f'''
            SELECT COALESCE(SUM(quantity), 0) as qty FROM stock_movements 
            WHERE type = 'in' AND created_at >= date('now', '-{period} days')
        ''').fetchone()[0]
        
        out_movements = conn.execute(f'''
            SELECT COALESCE(SUM(quantity), 0) as qty FROM stock_movements 
            WHERE type = 'out' AND created_at >= date('now', '-{period} days')
        ''').fetchone()[0]
        
        prev_in = conn.execute(f'''
            SELECT COALESCE(SUM(quantity), 0) as qty FROM stock_movements 
            WHERE type = 'in' AND created_at >= date('now', '-{prev_period} days') AND created_at < date('now', '-{period} days')
        ''').fetchone()[0]
        
        prev_out = conn.execute(f'''
            SELECT COALESCE(SUM(quantity), 0) as qty FROM stock_movements 
            WHERE type = 'out' AND created_at >= date('now', '-{prev_period} days') AND created_at < date('now', '-{period} days')
        ''').fetchone()[0]
        
        today_movements = conn.execute("SELECT COUNT(*) FROM stock_movements WHERE date(created_at) = date('now')").fetchone()[0]
        avg_daily_out = out_movements / period if period > 0 else 1
        dio = total_products / avg_daily_out if avg_daily_out > 0 else 0
        rotation_rate = (out_movements / total_products * 100) if total_products > 0 else 0
        prev_rotation = (prev_out / total_products * 100) if total_products > 0 else 0
        in_trend = ((in_movements - prev_in) / prev_in * 100) if prev_in > 0 else 0
        out_trend = ((out_movements - prev_out) / prev_out * 100) if prev_out > 0 else 0
        
        expiring_soon_params = [warehouse_id] if warehouse_id else []
        expiring_soon = conn.execute(f'''
            SELECT COUNT(*) FROM products
            WHERE expiry_date IS NOT NULL AND expiry_date <= date('now', '+30 days') {"AND warehouse_id = ?" if warehouse_id else ""}
        ''', expiring_soon_params).fetchone()[0]
        
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
        params_where = [warehouse_id] if warehouse_id else []
        
        low_stock = conn.execute(f'''
            SELECT id, name, sku, quantity, min_quantity, price, (min_quantity - quantity) as needed
            FROM products
            WHERE quantity < min_quantity AND is_deleted = 0 {"AND warehouse_id = ?" if warehouse_id else ""}
            ORDER BY needed DESC LIMIT 10
        ''', params_where).fetchall()
        
        out_of_stock = conn.execute(f'''
            SELECT id, name, sku, min_quantity, price
            FROM products
            WHERE (quantity < 0 OR quantity IS NULL) AND is_deleted = 0 {"AND warehouse_id = ?" if warehouse_id else ""}
            ORDER BY min_quantity DESC LIMIT 10
        ''', params_where).fetchall()
        
        expiring = conn.execute(f'''
            SELECT id, name, lot_number, expiry_date, quantity,
                   CAST(julianday(expiry_date) - julianday('now') AS INTEGER) as days_left
            FROM products
            WHERE expiry_date IS NOT NULL AND expiry_date <= date('now', '+30 days') AND expiry_date >= date('now')
            {"AND warehouse_id = ?" if warehouse_id else ""}
            ORDER BY expiry_date LIMIT 10
        ''', params_where).fetchall()
        
        inactive = conn.execute(f'''
            SELECT p.id, p.name, p.quantity, p.price, MAX(m.created_at) as last_movement
            FROM products p
            LEFT JOIN stock_movements m ON p.id = m.product_id
            {"WHERE p.warehouse_id = ?" if warehouse_id else ""}
            GROUP BY p.id
            HAVING MAX(m.created_at) IS NULL OR MAX(m.created_at) < date('now', '-90 days')
            LIMIT 10
        ''', params_where).fetchall()
        
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
        params = [warehouse_id] if warehouse_id else []
        where_clause = 'WHERE warehouse_id = ?' if warehouse_id else ''
        
        total = conn.execute(f'SELECT COUNT(*) as count FROM products {where_clause}', params).fetchone()[0]
        
        params_low = [warehouse_id] if warehouse_id else []
        low_stock = conn.execute(
            f'SELECT COUNT(*) as count FROM products WHERE quantity <= min_quantity {"AND warehouse_id = ?" if warehouse_id else ""}',
            params_low
        ).fetchone()[0]
        
        total_value = conn.execute(f'SELECT COALESCE(SUM(quantity * price), 0) as total FROM products {where_clause}', params).fetchone()[0]
        
        out_of_stock = conn.execute(
            f'SELECT COUNT(*) as count FROM products WHERE (quantity < 0 OR quantity IS NULL) {"AND warehouse_id = ?" if warehouse_id else ""}',
            params_low
        ).fetchone()[0]
        
        warehouses_count = conn.execute('SELECT COUNT(*) FROM warehouses').fetchone()[0]
        
        return jsonify({
            'total': total,
            'low_stock': low_stock,
            'total_value': total_value,
            'out_of_stock': out_of_stock,
            'warehouses_count': warehouses_count
        })