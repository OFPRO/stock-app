from flask import Blueprint, request, jsonify
from contextlib import contextmanager
import sqlite3
import random

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
    """Validate that a value is a valid positive integer."""
    if value is None:
        return None
    if isinstance(value, str) and value.isdigit():
        return int(value)
    if isinstance(value, int) and value > 0:
        return value
    return None

products_bp = Blueprint('products', __name__)

@products_bp.route('/api/products', methods=['GET'])
def get_products():
    warehouse_id = validate_id(request.args.get('warehouse_id'))
    include_archived = request.args.get('include_archived', 'false').lower() == 'true'
    
    with get_db() as conn:
        params = []
        where_parts = []
        if warehouse_id:
            where_parts.append('p.warehouse_id = ?')
            params.append(warehouse_id)
        if not include_archived:
            where_parts.append('p.is_deleted = 0')
        
        where_clause = ' AND '.join(where_parts) if where_parts else '1=1'
        
        query = '''
            SELECT p.*, s.name as supplier_name, w.name as warehouse_name, l.name as location_name
            FROM products p
            LEFT JOIN suppliers s ON p.supplier_id = s.id
            LEFT JOIN warehouses w ON p.warehouse_id = w.id
            LEFT JOIN locations l ON p.location_id = l.id
            WHERE ''' + where_clause + '''
            ORDER BY p.name
        '''
        
        products = conn.execute(query, params).fetchall()
        return jsonify([dict(p) for p in products])

@products_bp.route('/api/products/<int:product_id>', methods=['GET'])
def get_product(product_id):
    with get_db() as conn:
        product = conn.execute('''
            SELECT p.*, s.name as supplier_name, s.email as supplier_email, s.phone as supplier_phone,
                   w.name as warehouse_name, l.name as location_name
            FROM products p
            LEFT JOIN suppliers s ON p.supplier_id = s.id
            LEFT JOIN warehouses w ON p.warehouse_id = w.id
            LEFT JOIN locations l ON p.location_id = l.id
            WHERE p.id = ?
        ''', (product_id,)).fetchone()
        
        if not product:
            return jsonify({'error': 'Produit non trouvé'}), 404
        
        product = dict(product)
        
        purchase_stats = conn.execute('''
            SELECT COALESCE(SUM(poi.quantity), 0) as total_qty,
                   COALESCE(SUM(poi.quantity * poi.unit_price), 0) as total_purchases
            FROM purchase_order_items poi
            JOIN purchase_orders po ON poi.order_id = po.id
            WHERE poi.product_id = ? AND po.status = 'received'
        ''', (product_id,)).fetchone()
        
        sales_stats = conn.execute('''
            SELECT COALESCE(SUM(ii.quantity), 0) as total_qty,
                   COALESCE(SUM(ii.line_total), 0) as total_sales
            FROM invoice_items ii
            JOIN invoices i ON ii.invoice_id = i.id
            WHERE ii.product_id = ? AND i.status != 'annulee'
        ''', (product_id,)).fetchone()
        
        stock_locations = conn.execute('''
            SELECT l.name as location_name, p.quantity
            FROM products p
            LEFT JOIN locations l ON p.location_id = l.id
            WHERE p.id = ?
        ''', (product_id,)).fetchall()
        
        movements = conn.execute('''
            SELECT 'purchase' as source, 'in' as type, poi.quantity, po.created_at, 'Reception' as note
            FROM purchase_order_items poi
            JOIN purchase_orders po ON poi.order_id = po.id
            WHERE poi.product_id = ? AND po.status = 'received'
            UNION ALL
            SELECT 'invoice' as source, 'out' as type, -ii.quantity, i.created_at, 'Facture: ' || i.invoice_number
            FROM invoice_items ii
            JOIN invoices i ON ii.invoice_id = i.id
            WHERE ii.product_id = ? AND i.status != 'annulee'
            ORDER BY created_at DESC LIMIT 20
        ''', (product_id, product_id)).fetchall()
        
        return jsonify({
            'product': product,
            'purchase_stats': dict(purchase_stats) if purchase_stats else {'total_qty': 0, 'total_purchases': 0},
            'sales_stats': dict(sales_stats) if sales_stats else {'total_qty': 0, 'total_sales': 0},
            'stock_locations': [dict(loc) for loc in stock_locations] if stock_locations else [],
            'movements': [dict(m) for m in movements] if movements else []
        })

@products_bp.route('/api/products', methods=['POST'])
def add_product():
    data = request.json
    with get_db() as conn:
        try:
            sku = data.get('sku', '')
            if not sku:
                count = conn.execute('SELECT COALESCE(MAX(id), 0) + 1 FROM products').fetchone()[0]
                sku = 'SKU-' + str(count).zfill(4)
            
            conn.execute('''
                INSERT INTO products (name, description, sku, barcode, quantity, min_quantity, max_quantity, price,
                price_base, price_loyal, price_school, price_student, tax_category, category, warehouse_id, location_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ''', (
                data.get('name', ''), data.get('description', ''), sku, data.get('barcode', ''),
                data.get('quantity', 0), data.get('min_quantity', 5), data.get('max_quantity', 100),
                data.get('price', 0), data.get('price_base', 0), data.get('price_loyal', 0),
                data.get('price_school', 0), data.get('price_student', 0), data.get('tax_category', '20'),
                data.get('category', 'Général'), data.get('warehouse_id', 1), data.get('location_id')
            ))
            conn.commit()
            return jsonify({'success': True})
        except Exception as e:
            return jsonify({'error': str(e)}), 400

@products_bp.route('/api/products/<int:product_id>', methods=['PUT'])
def update_product(product_id):
    data = request.json
    with get_db() as conn:
        row = conn.execute('SELECT COALESCE(purchase_price_avg, price, 0) FROM products WHERE id=?', (product_id,)).fetchone()
        base_for_calc = row[0] if row else 0
        price_base = base_for_calc * 1.40 if base_for_calc > 0 else 0
        
        conn.execute('''
            UPDATE products SET name=?, description=?, sku=?, barcode=?, quantity=?, min_quantity=?, max_quantity=?, price=?,
            price_base=?, price_loyal=?, price_school=?, price_student=?, tax_category=?,
            lot_number=?, serial_number=?, expiry_date=?, supplier_id=?, category=?, warehouse_id=?, location_id=?
            WHERE id=?
        ''', (
            data.get('name', ''), data.get('description', ''), data.get('sku', ''), data.get('barcode', ''),
            data.get('quantity', 0), data.get('min_quantity', 5), data.get('max_quantity', 100), data.get('price', 0),
            price_base, data.get('price_loyal', 0), data.get('price_school', 0), data.get('price_student', 0),
            data.get('tax_category', '20'), data.get('lot_number', ''), data.get('serial_number', ''),
            data.get('expiry_date', ''), data.get('supplier_id'), data.get('category', 'Général'),
            data.get('warehouse_id', 1), data.get('location_id'), product_id
        ))
        conn.commit()
        return jsonify({'success': True})

@products_bp.route('/api/products/<int:product_id>', methods=['DELETE'])
def delete_product(product_id):
    soft_delete_only = request.args.get('soft', 'true').lower() == 'true'
    
    with get_db() as conn:
        product = conn.execute('SELECT * FROM products WHERE id=?', (product_id,)).fetchone()
        if not product:
            return jsonify({'error': 'Produit non trouvé'}), 404
        
        if soft_delete_only:
            invoices = conn.execute('''
                SELECT id, status FROM invoices 
                WHERE id IN (SELECT invoice_id FROM invoice_items WHERE product_id=?)
            ''', (product_id,)).fetchall()
            
            has_unpaid_invoices = False
            for inv in invoices:
                if inv['status'] not in ['payee', 'envoyee']:
                    has_unpaid_invoices = True
                    break
            
            if has_unpaid_invoices:
                conn.execute('''
                    UPDATE invoices SET status='annule' 
                    WHERE id IN (SELECT invoice_id FROM invoice_items WHERE product_id=?)
                    AND status NOT IN ('payee')
                ''', (product_id,))
                conn.commit()
            
            orders = conn.execute('''
                SELECT id, status FROM purchase_orders 
                WHERE id IN (SELECT order_id FROM purchase_order_items WHERE product_id=?)
            ''', (product_id,)).fetchall()
            
            for order in orders:
                if order['status'] == 'brouillon':
                    conn.execute('DELETE FROM purchase_order_items WHERE order_id=? AND product_id=?', 
                              (order['id'], product_id))
                    conn.execute('DELETE FROM purchase_orders WHERE id=? AND status=?', 
                              (order['id'], 'brouillon'))
            
            conn.execute('DELETE FROM notifications WHERE product_id=?', (product_id,))
            
            if product['quantity'] and product['quantity'] > 0:
                conn.execute('UPDATE products SET is_liquidation=1 WHERE id=?', (product_id,))
            else:
                conn.execute('UPDATE products SET is_deleted=1, deleted_at=CURRENT_TIMESTAMP WHERE id=?', (product_id,))
                conn.execute('''
                    INSERT INTO stock_movements (product_id, type, quantity, note)
                    VALUES (?, 'other', 0, 'Produit supprimé (archivé)')
                ''', (product_id,))
            
            conn.commit()
            return jsonify({'success': True, 'message': 'Produit archivé'})
        else:
            conn.execute('DELETE FROM stock WHERE product_id=?', (product_id,))
            conn.execute('DELETE FROM products WHERE id=?', (product_id,))
            conn.commit()
            return jsonify({'success': True, 'message': 'Produit supprimé définitivement'})

@products_bp.route('/api/products/calculate-prices', methods=['POST'])
def calculate_product_prices():
    data = request.json
    product_id = data.get('product_id')
    if not product_id:
        return jsonify({'error': 'product_id requis'}), 400
    
    with get_db() as conn:
        product = conn.execute('SELECT price, price_base, purchase_price_avg FROM products WHERE id=?', (product_id,)).fetchone()
        if not product:
            return jsonify({'error': 'Produit non trouvé'}), 404
        
        base = product['price_base'] if product['price_base'] and product['price_base'] > 0 else (product['purchase_price_avg'] or product['price'] or 0) * 1.40
        price_loyal = base * 0.85
        price_student = base * 0.85
        price_school = base * 0.80
        
        conn.execute('UPDATE products SET price_base=?, price_loyal=?, price_student=?, price_school=? WHERE id=?',
                     (base, price_loyal, price_student, price_school, product_id))
        conn.commit()
        
        return jsonify({
            'success': True,
            'prices': {
                'price_base': round(base, 2),
                'price_loyal': round(price_loyal, 2),
                'price_student': round(price_student, 2),
                'price_school': round(price_school, 2)
            }
        })

@products_bp.route('/api/products/for-sale', methods=['GET'])
def get_products_for_sale():
    warehouse_id = validate_id(request.args.get('warehouse_id'))
    customer_id = validate_id(request.args.get('customer_id'))
    search = request.args.get('search', '')
    
    with get_db() as conn:
        params = []
        if warehouse_id:
            query = 'SELECT * FROM products WHERE quantity > 0 AND warehouse_id = ?'
            params.append(warehouse_id)
        else:
            query = 'SELECT * FROM products WHERE quantity > 0'
        
        if search:
            query += ' AND (name LIKE ? OR sku LIKE ? OR barcode LIKE ?)'
            params.extend([f'%{search}%', f'%{search}%', f'%{search}%'])
        
        query += ' ORDER BY name'
        products = conn.execute(query, params).fetchall()
        
        customer = None
        if customer_id:
            customer = conn.execute('SELECT * FROM customers WHERE id=?', (customer_id,)).fetchone()
        
        def get_price_for_customer(product, customer_type, is_loyal):
            normal_price = product['price_base'] if product['price_base'] > 0 else product['price']
            if customer_type == 'ecole':
                return round(normal_price * 0.80, 2)
            if is_loyal:
                return round(normal_price * 0.85, 2)
            if customer_type == 'etudiant':
                return round(normal_price * 0.85, 2)
            return round(normal_price, 2)
        
        result = []
        for p in products:
            prod_dict = dict(p)
            if customer:
                prod_dict['sale_price'] = get_price_for_customer(prod_dict, customer['type'], customer['is_loyal'])
            else:
                prod_dict['sale_price'] = prod_dict.get('price_base', prod_dict.get('price', 0))
            result.append(prod_dict)
        
        return jsonify(result)

@products_bp.route('/api/categories', methods=['GET'])
def get_categories():
    with get_db() as conn:
        categories = conn.execute(
            'SELECT DISTINCT category FROM products WHERE category IS NOT NULL AND category != "" ORDER BY category'
        ).fetchall()
        return jsonify([c['category'] for c in categories])