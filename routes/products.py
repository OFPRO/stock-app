from flask import Blueprint, request, jsonify
from contextlib import contextmanager
import sqlite3
import random

DB_NAME = 'stock.db'

@contextmanager
def get_db():
    conn = sqlite3.connect(DB_NAME, check_same_thread=False)
    conn.row_factory = sqlite3.Row
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
            SELECT p.*, s.name as supplier_name, w.name as warehouse_name, l.name as location_name
            FROM products p
            LEFT JOIN suppliers s ON p.supplier_id = s.id
            LEFT JOIN warehouses w ON p.warehouse_id = w.id
            LEFT JOIN locations l ON p.location_id = l.id
            WHERE p.id = ?
        ''', (product_id,)).fetchone()
        
        if product:
            return jsonify({'product': dict(product)})
        return jsonify({'error': 'Produit non trouvé'}), 404

@products_bp.route('/api/products', methods=['POST'])
def add_product():
    data = request.json
    with get_db() as conn:
        try:
            conn.execute('''
                INSERT INTO products (name, description, sku, barcode, quantity, min_quantity, max_quantity, price,
                price_base, price_loyal, price_school, price_student, tax_category, category, warehouse_id, location_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ''', (
                data.get('name', ''), data.get('description', ''), data.get('sku', ''), data.get('barcode', ''),
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
        conn.execute('''
            UPDATE products SET name=?, description=?, sku=?, barcode=?, quantity=?, min_quantity=?, max_quantity=?, price=?,
            price_base=?, price_loyal=?, price_school=?, price_student=?, tax_category=?,
            lot_number=?, serial_number=?, expiry_date=?, supplier_id=?, category=?, warehouse_id=?, location_id=?
            WHERE id=?
        ''', (
            data.get('name', ''), data.get('description', ''), data.get('sku', ''), data.get('barcode', ''),
            data.get('quantity', 0), data.get('min_quantity', 5), data.get('max_quantity', 100), data.get('price', 0),
            data.get('price_base', 0), data.get('price_loyal', 0), data.get('price_school', 0), data.get('price_student', 0),
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
            conn.execute('''
                UPDATE products SET is_deleted=1, deleted_at=CURRENT_TIMESTAMP WHERE id=?
            ''', (product_id,))
            conn.commit()
            return jsonify({'success': True, 'message': 'Produit archivé'})
        else:
            conn.execute('DELETE FROM stock WHERE product_id=?', (product_id,))
            conn.execute('DELETE FROM products WHERE id=?', (product_id,))
            conn.commit()
            return jsonify({'success': True, 'message': 'Produit supprimé définitivement'})

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
            if is_loyal and product.get('price_loyal', 0) > 0:
                return product['price_loyal']
            if customer_type == 'ecole' and product.get('price_school', 0) > 0:
                return product['price_school']
            if customer_type == 'etudiant' and product.get('price_student', 0) > 0:
                return product['price_student']
            if product.get('price_base', 0) > 0:
                return product['price_base']
            return product.get('price', 0)
        
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