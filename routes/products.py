import json
from flask import Blueprint, request, jsonify
from routes.db import get_db_ctx as get_db, get_price_by_tier, validate_id

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
        if product.get('extra_prices'):
            try:
                product['extra_prices'] = json.loads(product['extra_prices'])
            except (json.JSONDecodeError, TypeError):
                product['extra_prices'] = []
        else:
            product['extra_prices'] = []
        
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
                price_base, price_loyal, price_gros, purchase_price_avg, tax_category, category, warehouse_id, location_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ''', (
                data.get('name', ''), data.get('description', ''), sku, data.get('barcode', ''),
                data.get('quantity', 0), data.get('min_quantity', 5), data.get('max_quantity', 100),
                data.get('price', 0), data.get('price_base', 0), data.get('price_loyal', 0),
                data.get('price_gros', 0), data.get('purchase_price_avg', 0), data.get('tax_category', '20'),
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
        price_base = float(data.get('price_base', 0))
        price = float(data.get('price', price_base))
        price_loyal = float(data.get('price_loyal', 0))
        price_gros = float(data.get('price_gros', 0))

        purchase_price_avg = float(data.get('purchase_price_avg', 0))
        conn.execute('''
            UPDATE products SET name=?, description=?, sku=?, barcode=?, quantity=?, min_quantity=?, max_quantity=?, price=?,
            price_base=?, price_loyal=?, price_gros=?, purchase_price_avg=?, tax_category=?,
            lot_number=?, serial_number=?, expiry_date=?, supplier_id=?, category=?, warehouse_id=?, location_id=?
            WHERE id=?
        ''', (
            data.get('name', ''), data.get('description', ''), data.get('sku', ''), data.get('barcode', ''),
            data.get('quantity', 0), data.get('min_quantity', 5), data.get('max_quantity', 100), price,
            price_base, price_loyal, price_gros, purchase_price_avg,
            data.get('tax_category', '20'), data.get('lot_number', ''), data.get('serial_number', ''),
            data.get('expiry_date', ''), data.get('supplier_id'), data.get('category', 'Général'),
            data.get('warehouse_id', 1), data.get('location_id'), product_id
        ))
        
        extra = data.get('extra_prices')
        if extra and isinstance(extra, list):
            conn.execute("UPDATE products SET extra_prices = ? WHERE id = ?",
                         (json.dumps(extra), product_id))
        
        conn.commit()
        row = conn.execute('SELECT price, price_base, purchase_price_avg FROM products WHERE id=?', (product_id,)).fetchone()
        margin = 0
        if row and row['price'] and row['purchase_price_avg'] and row['price'] > 0:
            margin = round((row['price'] - row['purchase_price_avg']) / row['price'] * 100, 1)
        
        return jsonify({
            'success': True,
            'price': price,
            'margin': margin
        })

@products_bp.route('/api/products/<int:product_id>', methods=['DELETE'])
def delete_product(product_id):
    with get_db() as conn:
        product = conn.execute('SELECT * FROM products WHERE id=?', (product_id,)).fetchone()
        if not product:
            return jsonify({'error': 'Produit non trouvé'}), 404
        
        product = dict(product)
        
        if product.get('is_deleted'):
            return jsonify({'error': 'Produit déjà supprimé'}), 400
        
        details = []
        
        # ── Étape 1 : Détruire le stock restant ──
        if product['quantity'] and product['quantity'] > 0:
            qty = product['quantity']
            conn.execute('UPDATE products SET quantity=0 WHERE id=?', (product_id,))
            conn.execute('DELETE FROM stock WHERE product_id=?', (product_id,))
            conn.execute('''
                INSERT INTO stock_movements (product_id, type, quantity, note)
                VALUES (?, 'destruction', ?, 'Stock détruit - suppression produit')
            ''', (product_id, qty))
            details.append(f"Stock détruit: {qty} unité(s)")
        
        # ── Étape 2 : Factures non payées/non annulées → annulées ──
        unpaid_invoices = conn.execute('''
            SELECT id, invoice_number, status FROM invoices 
            WHERE id IN (SELECT invoice_id FROM invoice_items WHERE product_id=?)
            AND status NOT IN ('payee', 'annulee')
        ''', (product_id,)).fetchall()
        for inv in unpaid_invoices:
            conn.execute("UPDATE invoices SET status='annulee', updated_at=CURRENT_TIMESTAMP WHERE id=?", (inv['id'],))
            details.append(f"Facture {inv['invoice_number']} annulée")
        
        # ── Étape 3 : Commandes brouillon → annulées ──
        draft_orders = conn.execute('''
            SELECT po.id, po.order_number, COUNT(poi2.id) as item_count
            FROM purchase_orders po
            JOIN purchase_order_items poi ON poi.order_id = po.id
            LEFT JOIN purchase_order_items poi2 ON poi2.order_id = po.id
            WHERE poi.product_id = ? AND po.status = 'brouillon'
            GROUP BY po.id
        ''', (product_id,)).fetchall()
        for order in draft_orders:
            conn.execute('DELETE FROM purchase_order_items WHERE order_id=? AND product_id=?', 
                      (order['id'], product_id))
            remaining = conn.execute('SELECT COUNT(*) as c FROM purchase_order_items WHERE order_id=?', 
                                   (order['id'],)).fetchone()
            if remaining['c'] == 0:
                conn.execute("UPDATE purchase_orders SET status='annulee' WHERE id=?", (order['id'],))
            details.append(f"Commande {order['order_number']}: ligne produit retirée")
        
        # ── Étape 4 : Commandes reçues → retournées ──
        received_orders = conn.execute('''
            SELECT DISTINCT po.id, po.order_number, po.warehouse_id
            FROM purchase_orders po
            JOIN purchase_order_items poi ON poi.order_id = po.id
            WHERE poi.product_id = ? AND po.status = 'recue'
        ''', (product_id,)).fetchall()
        for order in received_orders:
            items = conn.execute('SELECT quantity FROM purchase_order_items WHERE order_id=? AND product_id=?',
                               (order['id'], product_id)).fetchall()
            total_qty = sum(int(it['quantity']) for it in items)
            qty = total_qty
            conn.execute('UPDATE products SET quantity = MAX(0, quantity - ?) WHERE id=?', (qty, product_id))
            default_location = conn.execute('SELECT id FROM locations WHERE warehouse_id=? LIMIT 1', 
                                          (order['warehouse_id'],)).fetchone()
            if default_location:
                stock_entry = conn.execute('SELECT id, quantity FROM stock WHERE product_id=? AND location_id=?',
                                         (product_id, default_location['id'])).fetchone()
                if stock_entry:
                    new_qty = max(0, stock_entry['quantity'] - qty)
                    if new_qty > 0:
                        conn.execute('UPDATE stock SET quantity=? WHERE id=?', (new_qty, stock_entry['id']))
                    else:
                        conn.execute('DELETE FROM stock WHERE id=?', (stock_entry['id'],))
            conn.execute('''
                INSERT INTO stock_movements (product_id, type, quantity, source_location_id, note)
                VALUES (?, 'retour', ?, ?, 'Stock retourné - suppression produit')
            ''', (product_id, qty, default_location['id'] if default_location else None))
            conn.execute("UPDATE purchase_orders SET status='retournee' WHERE id=?", (order['id'],))
            details.append(f"Commande {order['order_number']}: stock retourné ({qty} unité(s))")
        
        # ── Étape 5 : Commandes payées → annulées + remboursement ──
        paid_orders = conn.execute('''
            SELECT po.id, po.order_number, po.total, poi.quantity as item_qty, poi.unit_price
            FROM purchase_orders po
            JOIN purchase_order_items poi ON poi.order_id = po.id
            WHERE poi.product_id = ? AND po.status = 'paye'
        ''', (product_id,)).fetchall()
        for item in paid_orders:
            refund_amount = item['item_qty'] * item['unit_price']
            conn.execute('UPDATE main_account SET current_balance = current_balance + ? WHERE id = 1', (refund_amount,))
            conn.execute('''
                INSERT INTO main_account_transactions (type, amount, reason, reference_id, note)
                VALUES ('refund', ?, 'product_deleted', ?, ?)
            ''', (refund_amount, product_id, 
                  f"Remboursement commande {item['order_number']} - produit supprimé"))
            details.append(f"Commande {item['order_number']}: {refund_amount:.2f} DH remboursé")
        
        # ── Étape 6 : Nettoyage données liées ──
        conn.execute('DELETE FROM notifications WHERE product_id=?', (product_id,))
        conn.execute('DELETE FROM reordering_rules WHERE product_id=?', (product_id,))
        
        # ── Étape 7 : Soft-delete du produit ──
        conn.execute('UPDATE products SET is_deleted=1, deleted_at=CURRENT_TIMESTAMP, quantity=0 WHERE id=?', 
                    (product_id,))
        conn.execute('''
            INSERT INTO stock_movements (product_id, type, quantity, note)
            VALUES (?, 'other', 0, 'Produit supprimé (soft-delete)')
        ''', (product_id,))
        details.append("Produit marqué comme supprimé")
        
        conn.commit()
        return jsonify({'success': True, 'message': 'Produit supprimé avec cascade', 'details': details})



@products_bp.route('/api/products/for-sale', methods=['GET'])
def get_products_for_sale():
    warehouse_id = validate_id(request.args.get('warehouse_id'))
    customer_id = validate_id(request.args.get('customer_id'))
    search = request.args.get('search', '')
    
    with get_db() as conn:
        params = []
        base_where = 'is_deleted = 0'
        if warehouse_id:
            query = 'SELECT * FROM products WHERE ' + base_where + ' AND warehouse_id = ?'
            params.append(warehouse_id)
        else:
            query = 'SELECT * FROM products WHERE ' + base_where
        
        if search:
            query += ' AND (name LIKE ? OR sku LIKE ? OR barcode LIKE ?)'
            params.extend([f'%{search}%', f'%{search}%', f'%{search}%'])
        
        query += ' ORDER BY name'
        products = conn.execute(query, params).fetchall()
        
        customer = None
        if customer_id:
            customer = conn.execute('SELECT * FROM customers WHERE id=?', (customer_id,)).fetchone()
        
        result = []
        for p in products:
            prod_dict = dict(p)
            if customer:
                prod_dict['sale_price'] = get_price_by_tier(prod_dict, customer['type'])
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