import json
import re
import io
import os
import traceback
from datetime import datetime
from flask import Blueprint, request, jsonify, Response
from fpdf import FPDF
from services.pdf_utils import setup_pdf, FONT_NAME, _arabic_pdf, _contains_arabic
from routes.db import get_db_ctx as get_db, get_price_by_tier, validate_id

products_bp = Blueprint('products', __name__)

@products_bp.route('/api/products', methods=['GET'])
def get_products():
    warehouse_id = validate_id(request.args.get('warehouse_id'))
    include_archived = request.args.get('include_archived', 'false').lower() == 'true'
    page = request.args.get('page', type=int)
    per_page = request.args.get('per_page', type=int)
    sort_by = request.args.get('sort_by', 'name')
    sort_order = request.args.get('sort_order', 'asc').lower()

    allowed_sorts = {'name', 'price', 'quantity', 'category', 'purchase_price_avg', 'sku', 'barcode'}
    if sort_by not in allowed_sorts:
        sort_by = 'name'
    if sort_order not in ('asc', 'desc'):
        sort_order = 'asc'

    with get_db() as conn:
        params = []
        where_parts = []
        if warehouse_id:
            where_parts.append('p.warehouse_id = ?')
            params.append(warehouse_id)
        if not include_archived:
            where_parts.append('p.is_deleted = 0')
        
        where_clause = ' AND '.join(where_parts) if where_parts else '1=1'
        order_clause = f'ORDER BY p.{sort_by} {sort_order}'

        base_query = '''
            SELECT p.*, s.name as supplier_name, w.name as warehouse_name, l.name as location_name,
                   c.name_ar as category_ar
            FROM products p
            LEFT JOIN suppliers s ON p.supplier_id = s.id
            LEFT JOIN warehouses w ON p.warehouse_id = w.id
            LEFT JOIN locations l ON p.location_id = l.id
            LEFT JOIN categories c ON p.category = c.name_fr
            WHERE ''' + where_clause + ' ' + order_clause

        if page and per_page and page > 0 and per_page > 0:
            count_query = 'SELECT COUNT(*) FROM products p WHERE ' + where_clause
            total = conn.execute(count_query, params).fetchone()[0]
            total_pages = max(1, (total + per_page - 1) // per_page)

            query = base_query + ' LIMIT ? OFFSET ?'
            extended_params = params + [per_page, (page - 1) * per_page]
            products = conn.execute(query, extended_params).fetchall()
            return jsonify({
                'data': [dict(p) for p in products],
                'total': total,
                'page': page,
                'per_page': per_page,
                'total_pages': total_pages
            })

        products = conn.execute(base_query, params).fetchall()
        return jsonify([dict(p) for p in products])

@products_bp.route('/api/products/<int:product_id>', methods=['GET'])
def get_product(product_id):
    with get_db() as conn:
        product = conn.execute('''
            SELECT p.*, s.name as supplier_name, s.email as supplier_email, s.phone as supplier_phone,
                   w.name as warehouse_name, l.name as location_name,
                   c.name_ar as category_ar
            FROM products p
            LEFT JOIN suppliers s ON p.supplier_id = s.id
            LEFT JOIN warehouses w ON p.warehouse_id = w.id
            LEFT JOIN locations l ON p.location_id = l.id
            LEFT JOIN categories c ON p.category = c.name_fr
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
            
            barcode = re.sub(r'[^\x20-\x7E]', '', data.get('barcode', '')).strip()
            image_url = data.get('image_url') or None
            cursor = conn.execute('''
                INSERT INTO products (name, description, sku, barcode, quantity, min_quantity, max_quantity, price,
                price_base, price_loyal, price_gros, purchase_price_avg, tax_category, category, warehouse_id, location_id, image_url)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ''', (
                data.get('name', ''), data.get('description', ''), sku, barcode,
                data.get('quantity', 0), data.get('min_quantity', 5), data.get('max_quantity', 100),
                data.get('price', 0), data.get('price_base', 0), data.get('price_loyal', 0),
                data.get('price_gros', 0), data.get('purchase_price_avg', 0), data.get('tax_category', '20'),
                data.get('category', 'Général'), data.get('warehouse_id', 1), data.get('location_id'),
                image_url
            ))
            new_id = cursor.lastrowid
            qty = int(data.get('quantity', 0))
            if qty > 0:
                conn.execute('''
                    INSERT INTO stock_movements (product_id, type, quantity, dest_location_id, note)
                    VALUES (?, 'in', ?, ?, 'Entrée initiale - création du produit')
                ''', (new_id, qty, data.get('location_id')))
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
        image_url = data.get('image_url') or None
        barcode = re.sub(r'[^\x20-\x7E]', '', data.get('barcode', '')).strip()
        conn.execute('''
            UPDATE products SET name=?, description=?, sku=?, barcode=?, quantity=?, min_quantity=?, max_quantity=?, price=?,
            price_base=?, price_loyal=?, price_gros=?, purchase_price_avg=?, tax_category=?,
            lot_number=?, serial_number=?, expiry_date=?, supplier_id=?, category=?, warehouse_id=?, location_id=?, image_url=?
            WHERE id=?
        ''', (
            data.get('name', ''), data.get('description', ''), data.get('sku', ''), barcode,
            data.get('quantity', 0), data.get('min_quantity', 5), data.get('max_quantity', 100), price,
            price_base, price_loyal, price_gros, purchase_price_avg,
            data.get('tax_category', '20'), data.get('lot_number', ''), data.get('serial_number', ''),
            data.get('expiry_date', ''), data.get('supplier_id'), data.get('category', 'Général'),
            data.get('warehouse_id', 1), data.get('location_id'),
            image_url, product_id
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
            query = 'SELECT p.*, c.name_ar as category_ar FROM products p LEFT JOIN categories c ON p.category = c.name_fr WHERE ' + base_where + ' AND p.warehouse_id = ?'
            params.append(warehouse_id)
        else:
            query = 'SELECT p.*, c.name_ar as category_ar FROM products p LEFT JOIN categories c ON p.category = c.name_fr WHERE ' + base_where
        
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
            'SELECT id, name_ar, name_fr FROM categories ORDER BY id'
        ).fetchall()
        return jsonify([dict(c) for c in categories])

@products_bp.route('/api/products/export/pdf', methods=['GET'])
def export_products_pdf():
    try:
        category = request.args.get('category', '')
        search = request.args.get('search', '')
        include_archived = request.args.get('include_archived', 'false').lower() == 'true'

        with get_db() as conn:
            params = []
            where_parts = []
            if not include_archived:
                where_parts.append('p.is_deleted = 0')
            if category:
                where_parts.append('p.category = ?')
                params.append(category)
            if search:
                where_parts.append('(p.name LIKE ? OR p.sku LIKE ? OR p.barcode LIKE ?)')
                s = f'%{search}%'
                params.extend([s, s, s])

            where_clause = ' AND '.join(where_parts) if where_parts else '1=1'
            products = conn.execute(f'''
                SELECT p.id, p.name, p.sku, p.category, p.quantity, p.purchase_price_avg, p.price,
                       c.name_ar as category_ar
                FROM products p
                LEFT JOIN categories c ON p.category = c.name_fr
                WHERE {where_clause}
                ORDER BY p.name
            ''', params).fetchall()

        pdf = FPDF(orientation='L', unit='mm', format='A4')
        setup_pdf(pdf)
        pdf.set_auto_page_break(auto=True, margin=15)
        pdf.add_page()

        logo_path = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'static', 'img', 'logo.png')
        if os.path.exists(logo_path):
            logo_w = 50
            x = pdf.l_margin + (pdf.epw - logo_w) / 2
            pdf.image(logo_path, x=x, w=logo_w)
            pdf.ln(5)

        pdf.set_font(FONT_NAME, 'B', 16)
        pdf.cell(0, 10, 'Liste des Produits', align='C', new_x='LMARGIN', new_y='NEXT')
        pdf.ln(2)

        pdf.set_font(FONT_NAME, '', 8)
        pdf.cell(0, 5, f"Genere le {datetime.now().strftime('%d/%m/%Y a %H:%M')}", align='C', new_x='LMARGIN', new_y='NEXT')
        if category:
            pdf.cell(0, 5, f"Categorie : {category}", align='C', new_x='LMARGIN', new_y='NEXT')
        pdf.cell(0, 5, f"Total : {len(products)} produit(s)", align='C', new_x='LMARGIN', new_y='NEXT')
        pdf.ln(4)

        col_w = [8, 80, 40, 20, 20, 20, 30]
        headers = ['#', 'Produit', 'Categorie', 'Qte', 'Prix Achat', 'Prix Vente', 'Montant']
        table_w = sum(col_w)
        x_start = (pdf.w - table_w) / 2

        def _draw_header():
            pdf.set_font(FONT_NAME, 'B', 9)
            pdf.set_fill_color(41, 128, 185)
            pdf.set_text_color(255, 255, 255)
            pdf.set_x(x_start)
            for i, h in enumerate(headers):
                pdf.cell(col_w[i], 7, h, border=1, align='C', fill=True)
            pdf.ln()
            pdf.set_text_color(0, 0, 0)
            pdf.set_font(FONT_NAME, '', 9)

        _draw_header()
        total_amount = 0
        for idx, p in enumerate(products, 1):
            if pdf.get_y() > 185:
                pdf.add_page()
                _draw_header()

            name = _arabic_pdf(p['name'] or '-')
            cat_label = _arabic_pdf((p['category'] or '-')[:25])
            amount = (p['purchase_price_avg'] or 0) * (p['quantity'] or 0)
            total_amount += amount

            row_h = 6
            pdf.set_x(x_start)
            align_name = 'R' if _contains_arabic(name) else ''
            align_cat = 'R' if _contains_arabic(cat_label) else ''
            pdf.cell(col_w[0], row_h, str(idx), border=1, align='C')
            pdf.cell(col_w[1], row_h, name[:40], border=1, align=align_name)
            pdf.cell(col_w[2], row_h, cat_label, border=1, align=align_cat)
            pdf.cell(col_w[3], row_h, str(p['quantity'] or 0), border=1, align='C')
            pdf.cell(col_w[4], row_h, f"{(p['purchase_price_avg'] or 0):.2f}", border=1, align='R')
            pdf.cell(col_w[5], row_h, f"{(p['price'] or 0):.2f}", border=1, align='R')
            pdf.cell(col_w[6], row_h, f"{amount:.2f}", border=1, align='R')
            pdf.ln()

        pdf.ln(3)
        pdf.set_font(FONT_NAME, 'B', 10)
        pdf.cell(0, 6, f"Montant total du stock : {total_amount:.2f} DH", align='R', new_x='LMARGIN', new_y='NEXT')

        buf = io.BytesIO()
        pdf.output(buf)
        buf.seek(0)

        return Response(
            buf.getvalue(),
            mimetype='application/pdf',
            headers={'Content-Disposition': 'attachment; filename="produits.pdf"'}
        )
    except Exception as e:
        traceback.print_exc()
        return jsonify({'error': 'Erreur lors de la generation du PDF'}), 500