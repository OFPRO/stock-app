from flask import Blueprint, request, jsonify
from datetime import datetime, timedelta
from routes.db import get_db_ctx as get_db, validate_id, _safe_int

kpis_bp = Blueprint('kpis', __name__)

@kpis_bp.route('/api/kpis', methods=['GET'])
def get_kpis():
    warehouse_id = validate_id(request.args.get('warehouse_id'))
    period = _safe_int(request.args.get('period', 30), 30)
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
            out_of_stock = conn.execute('SELECT COUNT(*) FROM products WHERE (quantity <= 0 OR quantity IS NULL) AND warehouse_id=? AND category=?', all_params).fetchone()[0]
            total_value_purchase = conn.execute("SELECT COALESCE(SUM(quantity * COALESCE(NULLIF(purchase_price_avg, 0), 0)), 0) FROM products WHERE warehouse_id=? AND category=?", all_params).fetchone()[0]
            products_no_pa = conn.execute("SELECT COUNT(*) FROM products WHERE (purchase_price_avg IS NULL OR purchase_price_avg = 0) AND warehouse_id=? AND category=?", all_params).fetchone()[0]
        elif warehouse_id:
            total_products = conn.execute('SELECT COUNT(*) FROM products WHERE warehouse_id=?', (warehouse_id,)).fetchone()[0]
            total_value = conn.execute('SELECT COALESCE(SUM(quantity * price), 0) FROM products WHERE warehouse_id=?', (warehouse_id,)).fetchone()[0]
            avg_price = conn.execute('SELECT COALESCE(AVG(price), 0) FROM products WHERE warehouse_id=?', (warehouse_id,)).fetchone()[0]
            low_stock = conn.execute('SELECT COUNT(*) FROM products WHERE quantity <= min_quantity AND warehouse_id=?', (warehouse_id,)).fetchone()[0]
            out_of_stock = conn.execute('SELECT COUNT(*) FROM products WHERE (quantity <= 0 OR quantity IS NULL) AND warehouse_id=?', (warehouse_id,)).fetchone()[0]
            total_value_purchase = conn.execute("SELECT COALESCE(SUM(quantity * COALESCE(NULLIF(purchase_price_avg, 0), 0)), 0) FROM products WHERE warehouse_id=?", (warehouse_id,)).fetchone()[0]
            products_no_pa = conn.execute("SELECT COUNT(*) FROM products WHERE (purchase_price_avg IS NULL OR purchase_price_avg = 0) AND warehouse_id=?", (warehouse_id,)).fetchone()[0]
        elif category:
            total_products = conn.execute('SELECT COUNT(*) FROM products WHERE category=?', (category,)).fetchone()[0]
            total_value = conn.execute('SELECT COALESCE(SUM(quantity * price), 0) FROM products WHERE category=?', (category,)).fetchone()[0]
            avg_price = conn.execute('SELECT COALESCE(AVG(price), 0) FROM products WHERE category=?', (category,)).fetchone()[0]
            low_stock = conn.execute('SELECT COUNT(*) FROM products WHERE quantity <= min_quantity AND category=?', (category,)).fetchone()[0]
            out_of_stock = conn.execute('SELECT COUNT(*) FROM products WHERE (quantity <= 0 OR quantity IS NULL) AND category=?', (category,)).fetchone()[0]
            total_value_purchase = conn.execute("SELECT COALESCE(SUM(quantity * COALESCE(NULLIF(purchase_price_avg, 0), 0)), 0) FROM products WHERE category=?", (category,)).fetchone()[0]
            products_no_pa = conn.execute("SELECT COUNT(*) FROM products WHERE (purchase_price_avg IS NULL OR purchase_price_avg = 0) AND category=?", (category,)).fetchone()[0]
        else:
            total_products = conn.execute('SELECT COUNT(*) FROM products').fetchone()[0]
            total_value = conn.execute('SELECT COALESCE(SUM(quantity * price), 0) FROM products').fetchone()[0]
            avg_price = conn.execute('SELECT COALESCE(AVG(price), 0) FROM products').fetchone()[0]
            low_stock = conn.execute('SELECT COUNT(*) FROM products WHERE quantity <= min_quantity').fetchone()[0]
            out_of_stock = conn.execute('SELECT COUNT(*) FROM products WHERE (quantity <= 0 OR quantity IS NULL)').fetchone()[0]
            total_value_purchase = conn.execute("SELECT COALESCE(SUM(quantity * COALESCE(NULLIF(purchase_price_avg, 0), 0)), 0) FROM products").fetchone()[0]
            products_no_pa = conn.execute("SELECT COUNT(*) FROM products WHERE (purchase_price_avg IS NULL OR purchase_price_avg = 0)").fetchone()[0]
        
        if date_start and date_end:
            date_params = (date_start, date_end)
            in_movements = conn.execute("SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE type IN ('in','retour') AND date(created_at) BETWEEN ? AND ?", date_params).fetchone()[0]
            out_movements = conn.execute("SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE type IN ('out','sale','destruction') AND date(created_at) BETWEEN ? AND ?", date_params).fetchone()[0]
        elif date_start:
            in_movements = conn.execute("SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE type IN ('in','retour') AND date(created_at) >= ?", (date_start,)).fetchone()[0]
            out_movements = conn.execute("SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE type IN ('out','sale','destruction') AND date(created_at) >= ?", (date_start,)).fetchone()[0]
        elif date_end:
            in_movements = conn.execute("SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE type IN ('in','retour') AND date(created_at) <= ?", (date_end,)).fetchone()[0]
            out_movements = conn.execute("SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE type IN ('out','sale','destruction') AND date(created_at) <= ?", (date_end,)).fetchone()[0]
        else:
            in_movements = conn.execute("SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE type IN ('in','retour') AND date(created_at) >= date('now', ?)", (f'-{period} days',)).fetchone()[0]
            out_movements = conn.execute("SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE type IN ('out','sale','destruction') AND date(created_at) >= date('now', ?)", (f'-{period} days',)).fetchone()[0]
        
        today_movements = conn.execute("SELECT COUNT(*) FROM stock_movements WHERE date(created_at) = date('now')").fetchone()[0]
        total_movements = in_movements + out_movements
        rotation_rate = (out_movements / total_movements * 100) if total_movements > 0 else 0
        
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
            'date_end': date_end,
            'total_value_purchase': round(total_value_purchase, 2),
            'products_no_purchase_price': products_no_pa
        })

@kpis_bp.route('/api/kpis/dashboard', methods=['GET'])
def get_dashboard_kpis():
    warehouse_id = validate_id(request.args.get('warehouse_id'))
    period = _safe_int(request.args.get('period', 30), 30)
    
    with get_db() as conn:
        if warehouse_id:
            total_products = conn.execute('SELECT COUNT(*) FROM products WHERE warehouse_id=?', (warehouse_id,)).fetchone()[0]
            total_value = conn.execute('SELECT COALESCE(SUM(quantity * price), 0) FROM products WHERE warehouse_id=?', (warehouse_id,)).fetchone()[0]
            low_stock = conn.execute('SELECT COUNT(*) FROM products WHERE quantity < min_quantity AND is_deleted = 0 AND warehouse_id=?', (warehouse_id,)).fetchone()[0]
            out_of_stock = conn.execute('SELECT COUNT(*) FROM products WHERE (quantity <= 0 OR quantity IS NULL) AND is_deleted = 0 AND warehouse_id=?', (warehouse_id,)).fetchone()[0]
            products_to_order = conn.execute('SELECT id, name, sku, quantity, min_quantity, (min_quantity - quantity) as needed FROM products WHERE quantity < min_quantity AND is_deleted = 0 AND warehouse_id=? ORDER BY needed DESC LIMIT 10', (warehouse_id,)).fetchall()
            total_value_purchase = conn.execute("SELECT COALESCE(SUM(quantity * COALESCE(NULLIF(purchase_price_avg, 0), 0)), 0) FROM products WHERE warehouse_id=?", (warehouse_id,)).fetchone()[0]
            products_no_pa = conn.execute("SELECT COUNT(*) FROM products WHERE (purchase_price_avg IS NULL OR purchase_price_avg = 0) AND warehouse_id=?", (warehouse_id,)).fetchone()[0]
            products_in_stock = conn.execute("SELECT COUNT(*) FROM products WHERE quantity > 0 AND is_deleted = 0 AND warehouse_id=?", (warehouse_id,)).fetchone()[0]
        else:
            total_products = conn.execute('SELECT COUNT(*) FROM products').fetchone()[0]
            total_value = conn.execute('SELECT COALESCE(SUM(quantity * price), 0) FROM products').fetchone()[0]
            low_stock = conn.execute('SELECT COUNT(*) FROM products WHERE quantity < min_quantity AND is_deleted = 0').fetchone()[0]
            out_of_stock = conn.execute('SELECT COUNT(*) FROM products WHERE (quantity <= 0 OR quantity IS NULL) AND is_deleted = 0').fetchone()[0]
            products_to_order = conn.execute('SELECT id, name, sku, quantity, min_quantity, (min_quantity - quantity) as needed FROM products WHERE quantity < min_quantity AND is_deleted = 0 ORDER BY needed DESC LIMIT 10').fetchall()
            total_value_purchase = conn.execute("SELECT COALESCE(SUM(quantity * COALESCE(NULLIF(purchase_price_avg, 0), 0)), 0) FROM products").fetchone()[0]
            products_no_pa = conn.execute("SELECT COUNT(*) FROM products WHERE (purchase_price_avg IS NULL OR purchase_price_avg = 0)").fetchone()[0]
            products_in_stock = conn.execute("SELECT COUNT(*) FROM products WHERE quantity > 0 AND is_deleted = 0").fetchone()[0]
        
        avg_price = total_value / total_products if total_products > 0 else 0
        
        in_movements = conn.execute("SELECT COALESCE(SUM(quantity), 0) as qty FROM stock_movements WHERE type IN ('in','retour') AND created_at>=date('now', ?)", (f'-{period} days',)).fetchone()[0]
        out_movements = conn.execute("SELECT COALESCE(SUM(quantity), 0) as qty FROM stock_movements WHERE type IN ('out','sale','destruction') AND created_at>=date('now', ?)", (f'-{period} days',)).fetchone()[0]
        prev_period = period * 2
        prev_in = conn.execute("SELECT COALESCE(SUM(quantity), 0) as qty FROM stock_movements WHERE type IN ('in','retour') AND created_at>=date('now', ?) AND created_at<date('now', ?)", (f'-{prev_period} days', f'-{period} days')).fetchone()[0]
        prev_out = conn.execute("SELECT COALESCE(SUM(quantity), 0) as qty FROM stock_movements WHERE type IN ('out','sale','destruction') AND created_at>=date('now', ?) AND created_at<date('now', ?)", (f'-{prev_period} days', f'-{period} days')).fetchone()[0]
        
        today_movements = conn.execute("SELECT COUNT(*) FROM stock_movements WHERE date(created_at) = date('now')").fetchone()[0]
        avg_daily_out = out_movements / period if period > 0 else 1
        dio = total_products / avg_daily_out if avg_daily_out > 0 else 0
        total_movements = in_movements + out_movements
        rotation_rate = (out_movements / total_movements * 100) if total_movements > 0 else 0
        prev_total = prev_in + prev_out
        prev_rotation = (prev_out / prev_total * 100) if prev_total > 0 else 0
        in_trend = ((in_movements - prev_in) / prev_in * 100) if prev_in > 0 else 0
        out_trend = ((out_movements - prev_out) / prev_out * 100) if prev_out > 0 else 0
        
        if warehouse_id:
            expiring_soon = conn.execute("SELECT COUNT(*) FROM products WHERE expiry_date IS NOT NULL AND expiry_date<=date('now','+30 days') AND warehouse_id=?", (warehouse_id,)).fetchone()[0]
        else:
            expiring_soon = conn.execute("SELECT COUNT(*) FROM products WHERE expiry_date IS NOT NULL AND expiry_date<=date('now','+30 days')").fetchone()[0]
        
        total_alerts = low_stock + out_of_stock + expiring_soon
        
        return jsonify({
            'total_products': total_products,
            'products_in_stock': products_in_stock,
            'total_value': round(total_value, 2),
            'total_value_purchase': round(total_value_purchase, 2),
            'products_no_purchase_price': products_no_pa,
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
            out_of_stock = conn.execute('SELECT id, name, sku, min_quantity, price FROM products WHERE (quantity <= 0 OR quantity IS NULL) AND is_deleted = 0 AND warehouse_id=? ORDER BY min_quantity DESC LIMIT 10', (warehouse_id,)).fetchall()
            expiring = conn.execute('SELECT id, name, lot_number, expiry_date, quantity, CAST(julianday(expiry_date) - julianday(\'now\') AS INTEGER) as days_left FROM products WHERE expiry_date IS NOT NULL AND expiry_date <= date(\'now\', \'+30 days\') AND expiry_date >= date(\'now\') AND is_deleted = 0 AND warehouse_id=? ORDER BY expiry_date LIMIT 10', (warehouse_id,)).fetchall()
            inactive = conn.execute('SELECT p.id, p.name, p.quantity, p.price, MAX(m.created_at) as last_movement FROM products p LEFT JOIN stock_movements m ON p.id = m.product_id WHERE p.is_deleted = 0 AND p.warehouse_id=? GROUP BY p.id HAVING MAX(m.created_at) IS NULL OR MAX(m.created_at) < date(\'now\', \'-90 days\') LIMIT 10', (warehouse_id,)).fetchall()
        else:
            low_stock = conn.execute('SELECT id, name, sku, quantity, min_quantity, price, (min_quantity - quantity) as needed FROM products WHERE quantity < min_quantity AND is_deleted = 0 ORDER BY needed DESC LIMIT 10').fetchall()
            out_of_stock = conn.execute('SELECT id, name, sku, min_quantity, price FROM products WHERE (quantity <= 0 OR quantity IS NULL) AND is_deleted = 0 ORDER BY min_quantity DESC LIMIT 10').fetchall()
            expiring = conn.execute('SELECT id, name, lot_number, expiry_date, quantity, CAST(julianday(expiry_date) - julianday(\'now\') AS INTEGER) as days_left FROM products WHERE expiry_date IS NOT NULL AND expiry_date <= date(\'now\', \'+30 days\') AND expiry_date >= date(\'now\') AND is_deleted = 0 ORDER BY expiry_date LIMIT 10').fetchall()
            inactive = conn.execute('SELECT p.id, p.name, p.quantity, p.price, MAX(m.created_at) as last_movement FROM products p LEFT JOIN stock_movements m ON p.id = m.product_id WHERE p.is_deleted = 0 GROUP BY p.id HAVING MAX(m.created_at) IS NULL OR MAX(m.created_at) < date(\'now\', \'-90 days\') LIMIT 10').fetchall()
        
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
            out_of_stock = conn.execute('SELECT COUNT(*) as count FROM products WHERE (quantity <= 0 OR quantity IS NULL) AND warehouse_id=?', (warehouse_id,)).fetchone()[0]
        else:
            total = conn.execute('SELECT COUNT(*) as count FROM products').fetchone()[0]
            low_stock = conn.execute('SELECT COUNT(*) as count FROM products WHERE quantity <= min_quantity').fetchone()[0]
            total_value = conn.execute('SELECT COALESCE(SUM(quantity * price), 0) as total FROM products').fetchone()[0]
            out_of_stock = conn.execute('SELECT COUNT(*) as count FROM products WHERE (quantity <= 0 OR quantity IS NULL)').fetchone()[0]
        
        warehouses_count = conn.execute('SELECT COUNT(*) FROM warehouses').fetchone()[0]
        
        return jsonify({
            'total': total,
            'low_stock': low_stock,
            'total_value': total_value,
            'out_of_stock': out_of_stock,
            'warehouses_count': warehouses_count
        })

@kpis_bp.route('/api/kpis/sales', methods=['GET'])
def get_kpis_sales():
    date_start = request.args.get('date_start')
    date_end = request.args.get('date_end')
    period = _safe_int(request.args.get('period', 30), 30)

    pos_date_filter = ""
    inv_date_filter = ""
    date_params = []
    if date_start and date_end:
        pos_date_filter = "AND date(created_at) BETWEEN ? AND ?"
        inv_date_filter = "AND date(paid_at) BETWEEN ? AND ?"
        date_params = [date_start, date_end]
    elif date_start:
        pos_date_filter = "AND date(created_at) >= ?"
        inv_date_filter = "AND date(paid_at) >= ?"
        date_params = [date_start]
    elif date_end:
        pos_date_filter = "AND date(created_at) <= ?"
        inv_date_filter = "AND date(paid_at) <= ?"
        date_params = [date_end]
    else:
        pos_date_filter = "AND created_at >= date('now', ?)"
        inv_date_filter = "AND paid_at >= date('now', ?)"
        date_params = ['-' + str(period) + ' days']

    with get_db() as conn:
        ca_periode = conn.execute(
            "SELECT COALESCE(SUM(total), 0) as total FROM pos_transactions WHERE status = 'completed' " + pos_date_filter,
            tuple(date_params)
        ).fetchone()[0] + conn.execute(
            "SELECT COALESCE(SUM(total), 0) as total FROM invoices WHERE status = 'payee' AND (type IS NULL OR type != 'fournisseur') AND is_credit_payment = 0 " + inv_date_filter,
            tuple(date_params)
        ).fetchone()[0]

        nb_ventes_periode = conn.execute(
            "SELECT COUNT(*) as count FROM pos_transactions WHERE status = 'completed' " + pos_date_filter,
            tuple(date_params)
        ).fetchone()[0] + conn.execute(
            "SELECT COUNT(*) as count FROM invoices WHERE status = 'payee' AND (type IS NULL OR type != 'fournisseur') AND is_credit_payment = 0 " + inv_date_filter,
            tuple(date_params)
        ).fetchone()[0]

        ca_trend = 0
        if not date_start and not date_end:
            ca_prev_pos = conn.execute("""
                SELECT COALESCE(SUM(total), 0) as total 
                FROM pos_transactions 
                WHERE status = 'completed' AND created_at >= date('now', '-60 days') AND created_at < date('now', '-30 days')
            """).fetchone()[0]
            ca_prev_inv = conn.execute("""
                SELECT COALESCE(SUM(total), 0) as total 
                FROM invoices 
                WHERE status = 'payee' AND (type IS NULL OR type != 'fournisseur') AND is_credit_payment = 0 AND paid_at >= date('now', '-60 days') AND paid_at < date('now', '-30 days')
            """).fetchone()[0]
            ca_prev = ca_prev_pos + ca_prev_inv
            ca_trend = ((ca_periode - ca_prev) / ca_prev * 100) if ca_prev > 0 else 0

        ca_jour = conn.execute("""
            SELECT COALESCE(SUM(total), 0) as total 
            FROM pos_transactions 
            WHERE status = 'completed' AND date(created_at) = date('now')
        """).fetchone()[0] + conn.execute("""
            SELECT COALESCE(SUM(total), 0) as total 
            FROM invoices 
            WHERE status = 'payee' AND (type IS NULL OR type != 'fournisseur') AND is_credit_payment = 0 AND date(paid_at) = date('now')
        """).fetchone()[0]

        nb_ventes_jour = conn.execute("""
            SELECT COUNT(*) as count 
            FROM pos_transactions 
            WHERE status = 'completed' AND date(created_at) = date('now')
        """).fetchone()[0] + conn.execute("""
            SELECT COUNT(*) as count 
            FROM invoices 
            WHERE status = 'payee' AND (type IS NULL OR type != 'fournisseur') AND is_credit_payment = 0 AND date(paid_at) = date('now')
        """).fetchone()[0]

        ticket_moyen = ca_periode / nb_ventes_periode if nb_ventes_periode > 0 else 0

        return jsonify({
            'ca_jour': round(ca_jour, 2),
            'nb_ventes_jour': nb_ventes_jour,
            'ticket_moyen': round(ticket_moyen, 2),
            'ca_periode': round(ca_periode, 2),
            'nb_ventes_periode': nb_ventes_periode,
            'ca_trend': round(ca_trend, 1),
            'date_filter': {'start': date_start, 'end': date_end, 'period': period}
        })

@kpis_bp.route('/api/kpis/expenses', methods=['GET'])
def get_kpis_expenses():
    period = _safe_int(request.args.get('period', 30), 30)
    date_start = request.args.get('date_start')
    date_end = request.args.get('date_end')

    exp_filter = ""
    date_params = []
    if date_start and date_end:
        exp_filter = "AND date(created_at) BETWEEN ? AND ?"
        date_params = [date_start, date_end]
    elif date_start:
        exp_filter = "AND date(created_at) >= ?"
        date_params = [date_start]
    elif date_end:
        exp_filter = "AND date(created_at) <= ?"
        date_params = [date_end]
    else:
        exp_filter = "AND date(created_at) >= date('now', ?)"
        date_params = ['-' + str(period) + ' days']

    with get_db() as conn:
        total_expenses = conn.execute(
            "SELECT COALESCE(SUM(amount), 0) FROM main_account_transactions WHERE type = 'out' " + exp_filter,
            tuple(date_params)
        ).fetchone()[0]

        expenses_today = conn.execute(
            "SELECT COALESCE(SUM(amount), 0) FROM main_account_transactions WHERE type = 'out' AND date(created_at) = date('now')"
        ).fetchone()[0]

        supplier_orders = conn.execute(
            "SELECT COALESCE(SUM(amount), 0) FROM main_account_transactions WHERE type = 'out' AND reason = 'supplier_order' " + exp_filter,
            tuple(date_params)
        ).fetchone()[0]

        breakdown = conn.execute(
            "SELECT reason, COALESCE(SUM(amount), 0) as total, COUNT(*) as count FROM main_account_transactions WHERE type = 'out' " + exp_filter + " GROUP BY reason ORDER BY total DESC",
            tuple(date_params)
        ).fetchall()

        trend = 0
        if not date_start and not date_end:
            prev_period = period * 2
            prev_exp = conn.execute(
                "SELECT COALESCE(SUM(amount), 0) FROM main_account_transactions WHERE type = 'out' AND created_at >= date('now', ?) AND created_at < date('now', ?)",
                (f'-{prev_period} days', f'-{period} days')
            ).fetchone()[0]
            trend = ((total_expenses - prev_exp) / prev_exp * 100) if prev_exp > 0 else 0

        return jsonify({
            'total_expenses': round(total_expenses, 2),
            'expenses_today': round(expenses_today, 2),
            'supplier_orders': round(supplier_orders, 2),
            'breakdown': [dict(b) for b in breakdown],
            'trend': round(trend, 1)
        })

@kpis_bp.route('/api/kpis/margins', methods=['GET'])
def get_kpis_margins():
    with get_db() as conn:
        purchase_avg = conn.execute("""
            SELECT poi.product_id, 
                   COALESCE(AVG(poi.unit_price), 0) as avg_purchase_price
            FROM purchase_order_items poi
            JOIN purchase_orders po ON poi.order_id = po.id
            WHERE po.status IN ('received', 'recue')
            GROUP BY poi.product_id
        """).fetchall()
        purchase_prices = {p['product_id']: p['avg_purchase_price'] for p in purchase_avg}

        invoice_sales = conn.execute("""
            SELECT ii.product_id, p.category,
                   SUM(ii.quantity * ii.unit_price) as total_selling,
                   SUM(ii.quantity) as total_qty_sold
            FROM invoice_items ii
            JOIN invoices i ON ii.invoice_id = i.id
            JOIN products p ON ii.product_id = p.id
            WHERE i.status != 'annulee' AND (i.type IS NULL OR i.type != 'fournisseur')
            GROUP BY ii.product_id, p.category
        """).fetchall()

        pos_sales = conn.execute("""
            SELECT pti.product_id, p.category,
                   SUM(pti.quantity * pti.unit_price) as total_selling,
                   SUM(pti.quantity) as total_qty_sold
            FROM pos_transaction_items pti
            JOIN pos_transactions t ON pti.transaction_id = t.id
            JOIN products p ON pti.product_id = p.id
            WHERE t.status = 'completed'
            GROUP BY pti.product_id, p.category
        """).fetchall()

        sales_by_product = list(invoice_sales) + list(pos_sales)

        cat_data = {}
        for s in sales_by_product:
            cat = s['category'] or 'Sans categorie'
            purchase_price = purchase_prices.get(s['product_id'], 0)
            if purchase_price == 0:
                purchase_price = conn.execute('SELECT COALESCE(NULLIF(purchase_price_avg, 0), price_base, 0) FROM products WHERE id = ?', (s['product_id'],)).fetchone()[0]

            vente = s['total_selling']
            achat = s['total_qty_sold'] * purchase_price
            marge = ((vente - achat) / vente * 100) if vente > 0 else 0

            if cat not in cat_data:
                cat_data[cat] = {'vente': 0, 'achat': 0}
            cat_data[cat]['vente'] += vente
            cat_data[cat]['achat'] += achat

        categories = []
        total_vente = 0
        total_achat = 0
        for cat, data in cat_data.items():
            vente = data['vente']
            achat = data['achat']
            marge = ((vente - achat) / vente * 100) if vente > 0 else 0
            categories.append({
                'category': cat,
                'vente': round(vente, 2),
                'achat': round(achat, 2),
                'marge_pct': round(marge, 1)
            })
            total_vente += vente
            total_achat += achat

        marge_globale = ((total_vente - total_achat) / total_vente * 100) if total_vente > 0 else 0

        return jsonify({
            'marge_globale': round(marge_globale, 1),
            'categories': categories
        })

@kpis_bp.route('/api/kpis/receivables', methods=['GET'])
def get_kpis_receivables():
    period = _safe_int(request.args.get('period', 30), 30)
    date_start = request.args.get('date_start')
    date_end = request.args.get('date_end')

    date_filter = ""
    date_filter_i = ""
    date_params = []
    if date_start and date_end:
        date_filter = "AND date(created_at) BETWEEN ? AND ?"
        date_filter_i = "AND date(i.created_at) BETWEEN ? AND ?"
        date_params = [date_start, date_end]
    elif date_start:
        date_filter = "AND date(created_at) >= ?"
        date_filter_i = "AND date(i.created_at) >= ?"
        date_params = [date_start]
    elif date_end:
        date_filter = "AND date(created_at) <= ?"
        date_filter_i = "AND date(i.created_at) <= ?"
        date_params = [date_end]
    else:
        date_filter = "AND date(created_at) >= date('now', ?)"
        date_filter_i = "AND date(i.created_at) >= date('now', ?)"
        date_params = ['-' + str(period) + ' days']

    with get_db() as conn:
        total_creances = conn.execute("""
            SELECT COALESCE(SUM(total - amount_paid), 0) as total
            FROM invoices
            WHERE status IN ('envoyee', 'partiellement_payee')
            AND (type IS NULL OR type != 'fournisseur') """ + date_filter,
            tuple(date_params)).fetchone()[0]

        nb_impayees = conn.execute("""
            SELECT COUNT(*) as count
            FROM invoices
            WHERE status IN ('envoyee', 'partiellement_payee')
            AND (type IS NULL OR type != 'fournisseur') """ + date_filter,
            tuple(date_params)).fetchone()[0]

        nb_impayees = conn.execute("""
            SELECT COUNT(*) as count
            FROM invoices
            WHERE status IN ('envoyee', 'partiellement_payee')
            AND (type IS NULL OR type != 'fournisseur') """ + date_filter,
            tuple(date_params)).fetchone()[0]

        creances_par_client = conn.execute("""
            SELECT c.id, c.name, c.client_code,
                   COALESCE(SUM(i.total - i.amount_paid), 0) as montant,
                   COUNT(i.id) as nb_factures,
                   MIN(i.due_date) as premiere_echeance
            FROM customers c
            JOIN invoices i ON c.id = i.customer_id AND i.status IN ('envoyee', 'partiellement_payee') """ + date_filter_i + """
            GROUP BY c.id
            ORDER BY montant DESC
            LIMIT 10
        """, tuple(date_params)).fetchall()

        clients = [dict(c) for c in creances_par_client]

        total_factures = conn.execute("""
            SELECT COALESCE(SUM(total), 0) FROM invoices WHERE status != 'annulee' AND (type IS NULL OR type != 'fournisseur') """ + date_filter,
            tuple(date_params)).fetchone()[0]
        encaisse = conn.execute("""
            SELECT COALESCE(SUM(amount_paid), 0) FROM invoices WHERE status IN ('payee', 'partiellement_payee', 'ticket') AND (type IS NULL OR type != 'fournisseur') """ + date_filter,
            tuple(date_params)).fetchone()[0]
        taux_encaissement = (encaisse / total_factures * 100) if total_factures > 0 else 0

        return jsonify({
            'total_creances': round(total_creances, 2),
            'nb_impayees': nb_impayees,
            'taux_encaissement': round(taux_encaissement, 1),
            'clients': clients
        })

@kpis_bp.route('/api/kpis/invoices-status', methods=['GET'])
def get_kpis_invoices_status():
    period = _safe_int(request.args.get('period', 30), 30)
    date_start = request.args.get('date_start')
    date_end = request.args.get('date_end')

    date_filter = ""
    date_params = []
    if date_start and date_end:
        date_filter = "AND date(created_at) BETWEEN ? AND ?"
        date_params = [date_start, date_end]
    elif date_start:
        date_filter = "AND date(created_at) >= ?"
        date_params = [date_start]
    elif date_end:
        date_filter = "AND date(created_at) <= ?"
        date_params = [date_end]
    else:
        date_filter = "AND date(created_at) >= date('now', ?)"
        date_params = ['-' + str(period) + ' days']

    with get_db() as conn:
        brouillon = conn.execute("SELECT COUNT(*) FROM invoices WHERE status = 'brouillon' AND (type IS NULL OR type != 'fournisseur') " + date_filter, tuple(date_params)).fetchone()[0]
        envoyee = conn.execute("SELECT COUNT(*) FROM invoices WHERE status = 'envoyee' AND (type IS NULL OR type != 'fournisseur') " + date_filter, tuple(date_params)).fetchone()[0]
        partiellement_payee = conn.execute("SELECT COUNT(*) FROM invoices WHERE status = 'partiellement_payee' AND (type IS NULL OR type != 'fournisseur') " + date_filter, tuple(date_params)).fetchone()[0]
        payee = conn.execute("SELECT COUNT(*) FROM invoices WHERE status = 'payee' AND (type IS NULL OR type != 'fournisseur') " + date_filter, tuple(date_params)).fetchone()[0]
        annulee = conn.execute("SELECT COUNT(*) FROM invoices WHERE status = 'annulee' AND (type IS NULL OR type != 'fournisseur') " + date_filter, tuple(date_params)).fetchone()[0]
        return jsonify({
            'brouillon': brouillon,
            'envoyee': envoyee,
            'partiellement_payee': partiellement_payee,
            'payee': payee,
            'annulee': annulee
        })

@kpis_bp.route('/api/kpis/sales-daily', methods=['GET'])
def get_kpis_sales_daily():
    period = _safe_int(request.args.get('period', 30), 30)
    today = datetime.now()

    start_date = (today - timedelta(days=period)).strftime('%Y-%m-%d')
    end_date = today.strftime('%Y-%m-%d')

    with get_db() as conn:
        pos_rows = conn.execute("""
            SELECT date(created_at) as dt, COALESCE(SUM(total), 0) as ca, COUNT(*) as nb
            FROM pos_transactions
            WHERE status = 'completed' AND date(created_at) BETWEEN ? AND ?
            GROUP BY date(created_at)
        """, (start_date, end_date)).fetchall()

        inv_rows = conn.execute("""
            SELECT date(paid_at) as dt, COALESCE(SUM(total), 0) as ca, COUNT(*) as nb
            FROM invoices
            WHERE status = 'payee' AND (type IS NULL OR type != 'fournisseur') AND is_credit_payment = 0 AND date(paid_at) BETWEEN ? AND ?
            GROUP BY date(paid_at)
        """, (start_date, end_date)).fetchall()

    pos_map = {r['dt']: r for r in pos_rows}
    inv_map = {r['dt']: r for r in inv_rows}

    daily_sales = []
    for i in range(period, -1, -1):
        target_date = (today - timedelta(days=i)).strftime('%Y-%m-%d')
        p = pos_map.get(target_date, {'ca': 0, 'nb': 0})
        inv = inv_map.get(target_date, {'ca': 0, 'nb': 0})
        daily_sales.append({
            'date': target_date,
            'ca': round(p['ca'] + inv['ca'], 2),
            'nb_ventes': p['nb'] + inv['nb']
        })

    return jsonify(daily_sales)

@kpis_bp.route('/api/kpis/categories-distribution', methods=['GET'])
def get_kpis_categories_distribution():
    with get_db() as conn:
        inv_cat = conn.execute("""
            SELECT p.category,
                   COALESCE(SUM(ii.quantity), 0) as qty_vendue,
                   COALESCE(SUM(ii.line_total), 0) as ca
            FROM products p
            LEFT JOIN invoice_items ii ON p.id = ii.product_id
            LEFT JOIN invoices i ON ii.invoice_id = i.id AND i.status = 'payee' AND (i.type IS NULL OR i.type != 'fournisseur') AND i.is_credit_payment = 0
            WHERE p.category IS NOT NULL AND p.category != ''
            GROUP BY p.category
        """).fetchall()

        pos_cat = conn.execute("""
            SELECT p.category,
                   COALESCE(SUM(pti.quantity), 0) as qty_vendue,
                   COALESCE(SUM(pti.line_total), 0) as ca
            FROM products p
            LEFT JOIN pos_transaction_items pti ON p.id = pti.product_id
            LEFT JOIN pos_transactions t ON pti.transaction_id = t.id AND t.status = 'completed'
            WHERE p.category IS NOT NULL AND p.category != ''
            GROUP BY p.category
        """).fetchall()

        merged = {}
        for row in list(inv_cat) + list(pos_cat):
            cat = row['category']
            if cat not in merged:
                merged[cat] = {'category': cat, 'qty_vendue': 0, 'ca': 0}
            merged[cat]['qty_vendue'] += row['qty_vendue']
            merged[cat]['ca'] += row['ca']

        result = list(merged.values())
        return jsonify(result)

@kpis_bp.route('/api/kpis/top-selling-products', methods=['GET'])
def get_kpis_top_selling():
    limit = _safe_int(request.args.get('limit', 10), 10)
    period = _safe_int(request.args.get('period', 30), 30)
    date_start = request.args.get('date_start')
    date_end = request.args.get('date_end')

    if date_start and date_end:
        date_filter_pos = "AND date(t.created_at) BETWEEN ? AND ?"
        date_params = (date_start, date_end)
        inv_id_subquery = "SELECT id FROM invoices WHERE status = 'payee' AND (type IS NULL OR type != 'fournisseur') AND is_credit_payment = 0 AND date(paid_at) BETWEEN ? AND ?"
        inv_params = (date_start, date_end)
    elif date_start:
        date_filter_pos = "AND date(t.created_at) >= ?"
        date_params = (date_start,)
        inv_id_subquery = "SELECT id FROM invoices WHERE status = 'payee' AND (type IS NULL OR type != 'fournisseur') AND is_credit_payment = 0 AND date(paid_at) >= ?"
        inv_params = (date_start,)
    elif date_end:
        date_filter_pos = "AND date(t.created_at) <= ?"
        date_params = (date_end,)
        inv_id_subquery = "SELECT id FROM invoices WHERE status = 'payee' AND (type IS NULL OR type != 'fournisseur') AND is_credit_payment = 0 AND date(paid_at) <= ?"
        inv_params = (date_end,)
    else:
        date_filter_pos = "AND t.created_at >= date('now', '-' || ? || ' days')"
        inv_id_subquery = "SELECT id FROM invoices WHERE status = 'payee' AND (type IS NULL OR type != 'fournisseur') AND is_credit_payment = 0 AND paid_at >= date('now', '-' || ? || ' days')"
        date_params = (str(period),)
        inv_params = (str(period),)

    with get_db() as conn:
        pos_products = conn.execute("""
            SELECT p.id, p.name, p.sku, p.category,
                   COALESCE(SUM(pti.quantity), 0) as qty_vendue,
                   COALESCE(SUM(pti.line_total), 0) as ca
            FROM products p
            LEFT JOIN pos_transaction_items pti ON p.id = pti.product_id
            LEFT JOIN pos_transactions t ON pti.transaction_id = t.id AND t.status = 'completed' """ + date_filter_pos + """
            GROUP BY p.id
        """, date_params).fetchall()

        inv_products = conn.execute("""
            SELECT p.id, p.name, p.sku, p.category,
                   COALESCE(SUM(ii.quantity), 0) as qty_vendue,
                   COALESCE(SUM(ii.line_total), 0) as ca
            FROM products p
            LEFT JOIN invoice_items ii ON p.id = ii.product_id
                AND ii.invoice_id IN (""" + inv_id_subquery + """)
            GROUP BY p.id
        """, inv_params).fetchall()

        merged = {}
        for row in list(pos_products) + list(inv_products):
            pid = row['id']
            if pid not in merged:
                merged[pid] = dict(row)
                merged[pid]['qty_vendue'] = 0
                merged[pid]['ca'] = 0
            merged[pid]['qty_vendue'] += row['qty_vendue']
            merged[pid]['ca'] += row['ca']

        result = sorted(merged.values(), key=lambda x: x['qty_vendue'], reverse=True)[:limit]
        return jsonify(result)

@kpis_bp.route('/api/kpis/sessions-history', methods=['GET'])
def get_kpis_sessions_history():
    limit = _safe_int(request.args.get('limit', 20), 20)
    status = request.args.get('status', 'closed')

    with get_db() as conn:
        sessions = conn.execute("""
            SELECT s.*,
                   (SELECT COALESCE(SUM(total), 0) FROM pos_transactions 
                    WHERE session_id = s.id AND status = 'completed') as total_sales,
                   (SELECT COUNT(*) FROM pos_transactions 
                    WHERE session_id = s.id AND status = 'completed') as nb_transactions,
                   (SELECT COALESCE(SUM(amount), 0) FROM pos_cash_movements 
                    WHERE session_id = s.id AND type = 'in') as total_cash_in,
                   (SELECT COALESCE(SUM(amount), 0) FROM pos_cash_movements 
                    WHERE session_id = s.id AND type = 'out') as total_cash_out,
                   (SELECT name FROM warehouses WHERE id = s.warehouse_id) as warehouse_name
            FROM pos_sessions s
            WHERE s.status = ?
            ORDER BY s.closed_at DESC
            LIMIT ?
        """, (status, limit)).fetchall()

        result = [dict(s) for s in sessions]
        return jsonify(result)

@kpis_bp.route('/api/kpis/sessions-summary', methods=['GET'])
def get_kpis_sessions_summary():
    period = _safe_int(request.args.get('period', 30), 30)

    with get_db() as conn:
        summary = conn.execute("""
            SELECT 
                COUNT(*) as total_sessions,
                SUM(CASE WHEN status = 'closed' THEN 1 ELSE 0 END) as closed_sessions,
                SUM(CASE WHEN status = 'open' THEN 1 ELSE 0 END) as open_sessions,
                SUM(closing_cash) as total_closing_cash,
                SUM(expected_cash) as total_expected_cash,
                (SELECT COALESCE(SUM(total), 0) FROM pos_transactions 
                 WHERE created_at >= date('now', '-' || ? || ' days') AND status = 'completed') as total_sales_period,
                (SELECT COUNT(*) FROM pos_transactions 
                 WHERE created_at >= date('now', '-' || ? || ' days') AND status = 'completed') as nb_transactions_period
            FROM pos_sessions
            WHERE opened_at >= date('now', '-' || ? || ' days')
        """, (period, period, period)).fetchone()

        result = dict(summary)
        return jsonify(result)

@kpis_bp.route('/api/kpis/sessions/<int:session_id>/details', methods=['GET'])
def get_kpis_session_details(session_id):
    with get_db() as conn:
        session = conn.execute("SELECT * FROM pos_sessions WHERE id = ?", (session_id,)).fetchone()
        if not session:
            return jsonify({'error': 'Session non trouvée'}), 404

        session_dict = dict(session)
        wh = conn.execute(
            "SELECT name FROM warehouses WHERE id = ?", (session_dict.get('warehouse_id', 1),)
        ).fetchone()
        session_dict['warehouse_name'] = wh[0] if wh else 'N/A'

        session_dict['total_sales'] = conn.execute(
            "SELECT COALESCE(SUM(total), 0) FROM pos_transactions WHERE session_id = ? AND status = 'completed'",
            (session_id,)
        ).fetchone()[0]

        transactions = conn.execute("""
            SELECT t.*, c.name as customer_name,
                   (SELECT COUNT(*) FROM pos_transaction_items WHERE transaction_id = t.id) as items_count
            FROM pos_transactions t
            LEFT JOIN customers c ON t.customer_id = c.id
            WHERE t.session_id = ? AND t.status = 'completed'
            ORDER BY t.created_at DESC
        """, (session_id,)).fetchall()

        cash_movements = conn.execute("""
            SELECT * FROM pos_cash_movements 
            WHERE session_id = ?
            ORDER BY created_at DESC
        """, (session_id,)).fetchall()

        result = {
            'session': session_dict,
            'transactions': [dict(t) for t in transactions],
            'cash_movements': [dict(m) for m in cash_movements]
        }
        return jsonify(result)

@kpis_bp.route('/api/kpis/trends', methods=['GET'])
def get_kpis_trends():
    warehouse_id = request.args.get('warehouse_id')
    period = _safe_int(request.args.get('period', 30), 30)
    today = datetime.now()
    start_date = (today - timedelta(days=period - 1)).strftime('%Y-%m-%d')
    end_date = today.strftime('%Y-%m-%d')

    with get_db() as conn:
        in_rows = conn.execute("""
            SELECT date(created_at) as dt, COALESCE(SUM(quantity), 0) as qty
            FROM stock_movements
            WHERE type IN ('in','retour') AND date(created_at) BETWEEN ? AND ?
            GROUP BY date(created_at)
        """, (start_date, end_date)).fetchall()

        out_rows = conn.execute("""
            SELECT date(created_at) as dt, COALESCE(SUM(quantity), 0) as qty
            FROM stock_movements
            WHERE type IN ('out','sale','destruction') AND date(created_at) BETWEEN ? AND ?
            GROUP BY date(created_at)
        """, (start_date, end_date)).fetchall()

    in_map = {r['dt']: r['qty'] for r in in_rows}
    out_map = {r['dt']: r['qty'] for r in out_rows}

    trends = []
    for i in range(period - 1, -1, -1):
        target_date = (today - timedelta(days=i)).strftime('%Y-%m-%d')
        entry = in_map.get(target_date, 0)
        exit_qty = out_map.get(target_date, 0)
        trends.append({
            'date': target_date,
            'entries': entry,
            'exits': exit_qty,
            'net': entry - exit_qty
        })

    return jsonify(trends)

@kpis_bp.route('/api/kpis/top-products', methods=['GET'])
def get_top_products():
    warehouse_id = request.args.get('warehouse_id')
    period = _safe_int(request.args.get('period', 30), 30)
    limit = _safe_int(request.args.get('limit', 10), 10)
    pd = str(-period) + ' days'

    with get_db() as conn:
        wid = validate_id(warehouse_id)
        if wid:
            products = conn.execute('''
                SELECT p.id, p.name, p.sku, p.quantity, p.price,
                       (SELECT COUNT(*) FROM stock_movements m WHERE m.product_id = p.id AND m.created_at >= date('now', ?)) as movement_count,
                       (SELECT COALESCE(SUM(quantity), 0) FROM stock_movements m WHERE m.product_id = p.id AND m.type IN ('in','retour') AND m.created_at >= date('now', ?)) as total_in,
                       (SELECT COALESCE(SUM(quantity), 0) FROM stock_movements m WHERE m.product_id = p.id AND m.type IN ('out','sale','destruction') AND m.created_at >= date('now', ?)) as total_out
                FROM products p
                WHERE p.warehouse_id = ?
                ORDER BY movement_count DESC
                LIMIT ?
            ''', (pd, pd, pd, wid, limit)).fetchall()
        else:
            products = conn.execute('''
                SELECT p.id, p.name, p.sku, p.quantity, p.price,
                       (SELECT COUNT(*) FROM stock_movements m WHERE m.product_id = p.id AND m.created_at >= date('now', ?)) as movement_count,
                       (SELECT COALESCE(SUM(quantity), 0) FROM stock_movements m WHERE m.product_id = p.id AND m.type IN ('in','retour') AND m.created_at >= date('now', ?)) as total_in,
                       (SELECT COALESCE(SUM(quantity), 0) FROM stock_movements m WHERE m.product_id = p.id AND m.type IN ('out','sale','destruction') AND m.created_at >= date('now', ?)) as total_out
                FROM products p
                ORDER BY movement_count DESC
                LIMIT ?
            ''', (pd, pd, pd, limit)).fetchall()

        return jsonify([dict(p) for p in products])

@kpis_bp.route('/api/kpis/by-location', methods=['GET'])
def get_kpis_by_location():
    warehouse_id = request.args.get('warehouse_id')

    with get_db() as conn:
        wid = validate_id(warehouse_id)
        if wid:
            locations = conn.execute('''
                SELECT l.id, l.name, l.type, w.name as warehouse_name,
                       COALESCE(SUM(s.quantity), 0) as total_qty,
                       COALESCE(SUM(s.quantity * p.price), 0) as total_value
                FROM locations l
                JOIN warehouses w ON l.warehouse_id = w.id
                LEFT JOIN stock s ON s.location_id = l.id
                LEFT JOIN products p ON s.product_id = p.id
                WHERE l.warehouse_id = ?
                GROUP BY l.id
                ORDER BY total_value DESC
            ''', (wid,)).fetchall()
        else:
            locations = conn.execute('''
                SELECT l.id, l.name, l.type, w.name as warehouse_name,
                       COALESCE(SUM(s.quantity), 0) as total_qty,
                       COALESCE(SUM(s.quantity * p.price), 0) as total_value
                FROM locations l
                JOIN warehouses w ON l.warehouse_id = w.id
                LEFT JOIN stock s ON s.location_id = l.id
                LEFT JOIN products p ON s.product_id = p.id
                GROUP BY l.id
                ORDER BY total_value DESC
            ''').fetchall()

        return jsonify([dict(l) for l in locations])

@kpis_bp.route('/api/kpis/warehouse-overview', methods=['GET'])
def get_warehouse_overview():
    with get_db() as conn:
        warehouses_data = conn.execute('''
            SELECT w.id, w.name, w.address,
                   COUNT(DISTINCT p.id) as product_count,
                   COALESCE(SUM(p.quantity), 0) as total_quantity,
                   COALESCE(SUM(p.quantity * p.price), 0) as total_value,
                   COUNT(DISTINCT l.id) as location_count
            FROM warehouses w
            LEFT JOIN products p ON w.id = p.warehouse_id
            LEFT JOIN locations l ON w.id = l.warehouse_id
            GROUP BY w.id
            ORDER BY w.name
        ''').fetchall()

        return jsonify([dict(w) for w in warehouses_data])

@kpis_bp.route('/api/kpis/orders-summary', methods=['GET'])
def get_orders_summary():
    with get_db() as conn:
        brouillon = conn.execute("SELECT COUNT(*) FROM purchase_orders WHERE status='brouillon'").fetchone()[0]
        recu = conn.execute("SELECT COUNT(*) FROM purchase_orders WHERE status='recue'").fetchone()[0]
        paye = conn.execute("SELECT COUNT(*) FROM purchase_orders WHERE status='paye'").fetchone()[0]
        total_value = conn.execute("SELECT COALESCE(SUM(total), 0) FROM purchase_orders WHERE status='paye'").fetchone()[0]
        return jsonify({'brouillon': brouillon, 'recu': recu, 'paye': paye, 'total_value': total_value})

@kpis_bp.route('/api/kpis/invoices-summary', methods=['GET'])
def get_invoices_summary():
    with get_db() as conn:
        unpaid = conn.execute("SELECT COUNT(*) FROM invoices WHERE status='envoyee' AND (type IS NULL OR type != 'fournisseur')").fetchone()[0]
        sent = conn.execute("SELECT COUNT(*) FROM invoices WHERE status='envoyee' AND (type IS NULL OR type != 'fournisseur')").fetchone()[0]
        paid = conn.execute("SELECT COUNT(*) FROM invoices WHERE status='payee' AND (type IS NULL OR type != 'fournisseur')").fetchone()[0]
        unpaid_amount = conn.execute("SELECT COALESCE(SUM(total), 0) FROM invoices WHERE status='envoyee' AND (type IS NULL OR type != 'fournisseur')").fetchone()[0]
        return jsonify({'unpaid': unpaid, 'sent': sent, 'paid': paid, 'unpaid_amount': unpaid_amount})

@kpis_bp.route('/api/kpis/customers-summary', methods=['GET'])
def get_customers_summary():
    with get_db() as conn:
        total = conn.execute('SELECT COUNT(*) FROM customers').fetchone()[0]
        loyal = conn.execute("SELECT COUNT(*) FROM customers WHERE is_loyal=1").fetchone()[0]
        return jsonify({'total': total, 'loyal': loyal})

@kpis_bp.route('/api/kpis/evolution', methods=['GET'])
def get_evolution():
    period = _safe_int(request.args.get('period', 30), 30)
    today = datetime.now()
    start_date = (today - timedelta(days=period - 1)).strftime('%Y-%m-%d')
    end_date = today.strftime('%Y-%m-%d')

    with get_db() as conn:
        in_rows = conn.execute("""
            SELECT date(created_at) as dt, COALESCE(SUM(quantity), 0) as qty
            FROM stock_movements
            WHERE type IN ('in','retour') AND date(created_at) BETWEEN ? AND ?
            GROUP BY date(created_at)
        """, (start_date, end_date)).fetchall()

        out_rows = conn.execute("""
            SELECT date(created_at) as dt, COALESCE(SUM(quantity), 0) as qty
            FROM stock_movements
            WHERE type IN ('out','sale','destruction') AND date(created_at) BETWEEN ? AND ?
            GROUP BY date(created_at)
        """, (start_date, end_date)).fetchall()

    in_map = {r['dt']: r['qty'] for r in in_rows}
    out_map = {r['dt']: r['qty'] for r in out_rows}

    evolution = []
    for i in range(period - 1, -1, -1):
        target_date = (today - timedelta(days=i)).strftime('%Y-%m-%d')
        entry = in_map.get(target_date, 0)
        exit_qty = out_map.get(target_date, 0)
        evolution.append({
            'date': target_date,
            'entries': entry,
            'exits': exit_qty,
            'net': entry - exit_qty
        })

    return jsonify(evolution)


@kpis_bp.route('/api/kpis/payment-methods', methods=['GET'])
def get_kpis_payment_methods():
    period = _safe_int(request.args.get('period', 30), 30)
    date_start = request.args.get('date_start')
    date_end = request.args.get('date_end')

    pos_filter = ""
    inv_filter = ""
    params = []
    if date_start and date_end:
        pos_filter = "AND date(created_at) BETWEEN ? AND ?"
        inv_filter = "AND date(paid_at) BETWEEN ? AND ?"
        params = [date_start, date_end]
    elif date_start:
        pos_filter = "AND date(created_at) >= ?"
        inv_filter = "AND date(paid_at) >= ?"
        params = [date_start]
    elif date_end:
        pos_filter = "AND date(created_at) <= ?"
        inv_filter = "AND date(paid_at) <= ?"
        params = [date_end]
    else:
        pos_filter = "AND created_at >= date('now', ?)"
        inv_filter = "AND paid_at >= date('now', ?)"
        params = ['-' + str(period) + ' days']

    with get_db() as conn:
        pos = conn.execute("""
            SELECT COALESCE(payment_method, 'card') as payment_method,
                   COALESCE(SUM(total), 0) as total, COUNT(*) as nb
            FROM pos_transactions
            WHERE status = 'completed' """ + pos_filter + """
            GROUP BY payment_method
        """, tuple(params)).fetchall()

        inv = conn.execute("""
            SELECT COALESCE(payment_method, 'card') as payment_method,
                   COALESCE(SUM(total), 0) as total, COUNT(*) as nb
            FROM invoices
            WHERE status = 'payee' AND (type IS NULL OR type != 'fournisseur') AND is_credit_payment = 0 """ + inv_filter + """
            GROUP BY payment_method
        """, tuple(params)).fetchall()

    pos_dict = {}
    for r in pos:
        pm = r['payment_method'] or 'card'
        pos_dict[pm] = {'total': r['total'], 'nb': r['nb']}
    inv_dict = {}
    for r in inv:
        pm = r['payment_method'] or 'card'
        inv_dict[pm] = {'total': r['total'], 'nb': r['nb']}

    all_methods = set(list(pos_dict.keys()) + list(inv_dict.keys()))
    methods = {}
    total_pos = 0
    total_inv = 0
    for m in all_methods:
        p = pos_dict.get(m, {'total': 0, 'nb': 0})
        i = inv_dict.get(m, {'total': 0, 'nb': 0})
        total = p['total'] + i['total']
        nb = p['nb'] + i['nb']
        methods[m] = {'total': round(total, 2), 'nb': nb}
        total_pos += p['total']
        total_inv += i['total']

    return jsonify({
        'methods': methods,
        'total_pos': round(total_pos, 2),
        'total_inv': round(total_inv, 2),
        'total': round(total_pos + total_inv, 2)
    })


@kpis_bp.route('/api/kpis/registers-status', methods=['GET'])
def get_kpis_registers_status():
    with get_db() as conn:
        registers = conn.execute('''
            SELECT r.*, s.id as session_id, s.session_number, s.status as session_status,
                   s.opening_cash, s.opened_at, s.cashier_name
            FROM pos_registers r
            LEFT JOIN pos_sessions s ON r.id = s.register_id AND s.status = 'open'
            WHERE r.is_active = 1
            ORDER BY r.id
        ''').fetchall()
        
        result = []
        for reg in registers:
            row = dict(reg)
            if row['session_id']:
                total_sales = conn.execute('''
                    SELECT COALESCE(SUM(total), 0) FROM pos_transactions WHERE session_id = ?
                ''', (row['session_id'],)).fetchone()[0]
                nb_trans = conn.execute('''
                    SELECT COUNT(*) FROM pos_transactions WHERE session_id = ?
                ''', (row['session_id'],)).fetchone()[0]
                cash_in = conn.execute('''
                    SELECT COALESCE(SUM(amount), 0) FROM pos_cash_movements WHERE session_id = ? AND type = 'in'
                ''', (row['session_id'],)).fetchone()[0]
                cash_out = conn.execute('''
                    SELECT COALESCE(SUM(amount), 0) FROM pos_cash_movements WHERE session_id = ? AND type = 'out'
                ''', (row['session_id'],)).fetchone()[0]
                row['total_sales'] = total_sales
                row['nb_transactions'] = nb_trans
                row['cash_balance'] = row['opening_cash'] + cash_in - cash_out
                row['status'] = 'open'
            else:
                row['status'] = 'closed'
                row['total_sales'] = 0
                row['nb_transactions'] = 0
                row['cash_balance'] = 0
            result.append(row)
        
        return jsonify(result)