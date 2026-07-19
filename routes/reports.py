from flask import Blueprint, request, jsonify
from datetime import datetime, timedelta
from routes.db import get_db, _safe_int

reports_bp = Blueprint('reports', __name__)


def _build_date_filters(date_start, date_end, period, pos_alias='t', inv_alias='i'):
    """Build date filter SQL and params for POS + invoice queries."""
    pos_filter = ""
    inv_filter = ""
    params = []
    if date_start and date_end:
        pos_filter = f"AND date({pos_alias}.created_at) BETWEEN ? AND ?"
        inv_filter = f"AND date({inv_alias}.paid_at) BETWEEN ? AND ?"
        params = [date_start, date_end]
    elif date_start:
        pos_filter = f"AND date({pos_alias}.created_at) >= ?"
        inv_filter = f"AND date({inv_alias}.paid_at) >= ?"
        params = [date_start]
    elif date_end:
        pos_filter = f"AND date({pos_alias}.created_at) <= ?"
        inv_filter = f"AND date({inv_alias}.paid_at) <= ?"
        params = [date_end]
    elif period:
        pos_filter = f"AND {pos_alias}.created_at >= date('now', ?)"
        inv_filter = f"AND {inv_alias}.paid_at >= date('now', ?)"
        params = ['-' + str(period) + ' days']
    return pos_filter, inv_filter, params


def _ca_subquery(pos_filter, inv_filter, params, sum_expr='SUM(total)'):
    """Return (sql_fragment, params) for CA calculation across POS + invoices."""
    pos_sql = f"SELECT COALESCE({sum_expr}, 0) as ca FROM pos_transactions t WHERE t.status = 'completed' {pos_filter}"
    inv_sql = f"""SELECT COALESCE({sum_expr}, 0) as ca FROM invoices i
        WHERE i.status = 'payee' AND (i.type IS NULL OR i.type != 'fournisseur')
        AND i.is_credit_payment = 0
        AND NOT EXISTS (SELECT 1 FROM pos_transactions pt WHERE pt.invoice_id = i.id)
        {inv_filter}"""
    combined = f"SELECT COALESCE((SELECT ca FROM ({pos_sql})), 0) + COALESCE((SELECT ca FROM ({inv_sql})), 0)"
    all_params = list(params) + list(params)
    return combined, all_params


@reports_bp.route('/api/reports/overview', methods=['GET'])
def reports_overview():
    date_start = request.args.get('date_start')
    date_end = request.args.get('date_end')
    period = _safe_int(request.args.get('period', 30), 30)

    pos_filter, inv_filter, params = _build_date_filters(date_start, date_end, period)
    tp = tuple(params) if params else ()

    with get_db() as conn:
        ca_sql, ca_params = _ca_subquery(pos_filter, inv_filter, params)
        ca_total = conn.execute(ca_sql, tuple(ca_params) if ca_params else ()).fetchone()[0]

        nb_ventes = conn.execute(
            f"SELECT COUNT(*) FROM pos_transactions t WHERE t.status = 'completed' {pos_filter}", tp
        ).fetchone()[0] + conn.execute(
            f"""SELECT COUNT(*) FROM invoices i WHERE i.status = 'payee'
                AND (i.type IS NULL OR i.type != 'fournisseur') AND i.is_credit_payment = 0
                AND NOT EXISTS (SELECT 1 FROM pos_transactions pt WHERE pt.invoice_id = i.id)
                {inv_filter}""", tp
        ).fetchone()[0]

        ticket_moyen = ca_total / nb_ventes if nb_ventes > 0 else 0

        marges = conn.execute(f"""
            SELECT
                COALESCE(SUM(ii.line_total), 0) as vente,
                COALESCE(SUM(ii.quantity * COALESCE(
                    (SELECT AVG(poi.unit_price) FROM purchase_order_items poi
                     JOIN purchase_orders po ON poi.order_id = po.id
                     WHERE poi.product_id = ii.product_id AND po.status IN ('received', 'recue')),
                    p.purchase_price_avg, p.price_base, 0)), 0) as achat
            FROM invoice_items ii
            JOIN invoices i ON ii.invoice_id = i.id
            JOIN products p ON ii.product_id = p.id
            WHERE i.status != 'annulee' AND (i.type IS NULL OR i.type != 'fournisseur')
            {inv_filter}
        """, tp).fetchone()

        pos_marges = conn.execute(f"""
            SELECT
                COALESCE(SUM(pti.line_total), 0) as vente,
                COALESCE(SUM(pti.quantity * COALESCE(
                    (SELECT AVG(poi.unit_price) FROM purchase_order_items poi
                     JOIN purchase_orders po ON poi.order_id = po.id
                     WHERE poi.product_id = pti.product_id AND po.status IN ('received', 'recue')),
                    p.purchase_price_avg, p.price_base, 0)), 0) as achat
            FROM pos_transaction_items pti
            JOIN pos_transactions t ON pti.transaction_id = t.id
            JOIN products p ON pti.product_id = p.id
            WHERE t.status = 'completed'
            {pos_filter}
        """, tp).fetchone()

        vente_totale = (marges['vente'] or 0) + (pos_marges['vente'] or 0)
        achat_total = (marges['achat'] or 0) + (pos_marges['achat'] or 0)
        marge_pct = ((vente_totale - achat_total) / vente_totale * 100) if vente_totale > 0 else 0

        total_stock = conn.execute("SELECT COALESCE(SUM(quantity * price), 0) FROM products WHERE is_deleted = 0").fetchone()[0]
        total_stock_qty = conn.execute("SELECT COALESCE(SUM(quantity), 0) FROM products WHERE is_deleted = 0").fetchone()[0]

        out_movements = conn.execute(
            f"SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE type IN ('out','sale','destruction') {pos_filter.replace('t.', '')}",
            tp
        ).fetchone()[0]
        avg_daily_out = out_movements / period if period > 0 else 1
        dio = total_stock_qty / avg_daily_out if avg_daily_out > 0 else 0

        prev_period = period * 2
        prev_pos_filter = f"AND t.created_at >= date('now', ?) AND t.created_at < date('now', ?)"
        prev_inv_filter = f"AND i.paid_at >= date('now', ?) AND i.paid_at < date('now', ?)"
        prev_ca_sql, prev_ca_params = _ca_subquery(prev_pos_filter, prev_inv_filter, [f'-{prev_period} days', f'-{period} days'])
        ca_prev = conn.execute(prev_ca_sql, prev_ca_params).fetchone()[0]
        ca_trend = ((ca_total - ca_prev) / ca_prev * 100) if ca_prev > 0 else 0

    return jsonify({
        'ca_total': round(ca_total, 2),
        'nb_ventes': nb_ventes,
        'ticket_moyen': round(ticket_moyen, 2),
        'marge_pct': round(marge_pct, 1),
        'marge_montant': round(vente_totale - achat_total, 2),
        'stock_value': round(total_stock, 2),
        'dio': round(dio, 1),
        'ca_trend': round(ca_trend, 1),
        'period': period,
        'date_start': date_start,
        'date_end': date_end
    })


@reports_bp.route('/api/reports/sales-by-product', methods=['GET'])
def reports_sales_by_product():
    date_start = request.args.get('date_start')
    date_end = request.args.get('date_end')
    period = _safe_int(request.args.get('period', 30), 30)
    category = request.args.get('category', '')
    limit = _safe_int(request.args.get('limit', 20), 20)

    pos_filter, inv_filter, params = _build_date_filters(date_start, date_end, period)
    tp = tuple(params) if params else ()
    cat_filter = f"AND p.category = '{category}'" if category else ""

    with get_db() as conn:
        pos_products = conn.execute(f"""
            SELECT p.id, p.name, p.sku, p.category, p.price,
                   COALESCE(SUM(pti.quantity), 0) as qty_vendue,
                   COALESCE(SUM(pti.line_total), 0) as ca
            FROM products p
            LEFT JOIN pos_transaction_items pti ON p.id = pti.product_id
                AND pti.transaction_id IN (SELECT t.id FROM pos_transactions t WHERE t.status = 'completed' {pos_filter})
            WHERE p.is_deleted = 0 {cat_filter}
            GROUP BY p.id
        """, tp).fetchall()

        inv_products = conn.execute(f"""
            SELECT p.id, p.name, p.sku, p.category, p.price,
                   COALESCE(SUM(ii.quantity), 0) as qty_vendue,
                   COALESCE(SUM(ii.line_total), 0) as ca
            FROM products p
            LEFT JOIN invoice_items ii ON p.id = ii.product_id
                AND ii.invoice_id IN (
                    SELECT i.id FROM invoices i WHERE i.status = 'payee'
                    AND (i.type IS NULL OR i.type != 'fournisseur')
                    AND NOT EXISTS (SELECT 1 FROM pos_transactions pt WHERE pt.invoice_id = i.id)
                    {inv_filter}
                )
            WHERE p.is_deleted = 0 {cat_filter}
            GROUP BY p.id
        """, tp).fetchall()

        merged = {}
        for row in list(pos_products) + list(inv_products):
            pid = row['id']
            if pid not in merged:
                merged[pid] = dict(row)
                merged[pid]['qty_vendue'] = 0
                merged[pid]['ca'] = 0
            merged[pid]['qty_vendue'] += row['qty_vendue']
            merged[pid]['ca'] += row['ca']

        result = sorted(merged.values(), key=lambda x: x['ca'], reverse=True)[:limit]
        return jsonify(result)


@reports_bp.route('/api/reports/sales-by-customer', methods=['GET'])
def reports_sales_by_customer():
    date_start = request.args.get('date_start')
    date_end = request.args.get('date_end')
    period = _safe_int(request.args.get('period', 30), 30)
    limit = _safe_int(request.args.get('limit', 20), 20)

    _, inv_filter, params = _build_date_filters(date_start, date_end, period)
    tp = tuple(params) if params else ()

    with get_db() as conn:
        pos_customers = conn.execute(f"""
            SELECT c.id, c.name, c.client_code,
                   COUNT(DISTINCT t.id) as nb_achats,
                   COALESCE(SUM(t.total), 0) as ca_total
            FROM customers c
            JOIN pos_transactions t ON c.id = t.customer_id
            WHERE t.status = 'completed'
            {'AND date(t.created_at) BETWEEN ? AND ?' if date_start and date_end else
             'AND date(t.created_at) >= ?' if date_start else
             'AND date(t.created_at) <= ?' if date_end else
             "AND t.created_at >= date('now', ?)" if period else ''}
            GROUP BY c.id
        """, tp).fetchall()

        inv_customers = conn.execute(f"""
            SELECT c.id, c.name, c.client_code,
                   COUNT(DISTINCT i.id) as nb_achats,
                   COALESCE(SUM(i.total), 0) as ca_total
            FROM customers c
            JOIN invoices i ON c.id = i.customer_id
            WHERE i.status = 'payee' AND (i.type IS NULL OR i.type != 'fournisseur')
            AND i.is_credit_payment = 0
            AND NOT EXISTS (SELECT 1 FROM pos_transactions pt WHERE pt.invoice_id = i.id)
            {inv_filter}
            GROUP BY c.id
        """, tp).fetchall()

        merged = {}
        for row in list(pos_customers) + list(inv_customers):
            cid = row['id']
            if cid not in merged:
                merged[cid] = dict(row)
                merged[cid]['nb_achats'] = 0
                merged[cid]['ca_total'] = 0
            merged[cid]['nb_achats'] += row['nb_achats']
            merged[cid]['ca_total'] += row['ca_total']

        result = sorted(merged.values(), key=lambda x: x['ca_total'], reverse=True)[:limit]
        return jsonify(result)


@reports_bp.route('/api/reports/sales-by-day', methods=['GET'])
def reports_sales_by_day():
    date_start = request.args.get('date_start')
    date_end = request.args.get('date_end')
    period = _safe_int(request.args.get('period', 30), 30)

    if date_start and date_end:
        start_date = date_start
        end_date = date_end
    else:
        today = datetime.now()
        start_date = (today - timedelta(days=period)).strftime('%Y-%m-%d')
        end_date = today.strftime('%Y-%m-%d')

    with get_db() as conn:
        pos_rows = conn.execute("""
            SELECT date(created_at) as dt, COALESCE(SUM(total), 0) as ca, COUNT(*) as nb
            FROM pos_transactions WHERE status = 'completed' AND date(created_at) BETWEEN ? AND ?
            GROUP BY date(created_at)
        """, (start_date, end_date)).fetchall()

        inv_rows = conn.execute("""
            SELECT date(paid_at) as dt, COALESCE(SUM(total), 0) as ca, COUNT(*) as nb
            FROM invoices WHERE status = 'payee' AND (type IS NULL OR type != 'fournisseur')
            AND is_credit_payment = 0
            AND NOT EXISTS (SELECT 1 FROM pos_transactions pt WHERE pt.invoice_id = invoices.id)
            AND date(paid_at) BETWEEN ? AND ?
            GROUP BY date(paid_at)
        """, (start_date, end_date)).fetchall()

    pos_map = {r['dt']: r for r in pos_rows}
    inv_map = {r['dt']: r for r in inv_rows}

    from datetime import date as dt_date
    sd = datetime.strptime(start_date, '%Y-%m-%d').date()
    ed = datetime.strptime(end_date, '%Y-%m-%d').date()
    delta = (ed - sd).days

    daily = []
    for i in range(delta + 1):
        d = (sd + timedelta(days=i)).strftime('%Y-%m-%d')
        p = pos_map.get(d, {'ca': 0, 'nb': 0})
        inv = inv_map.get(d, {'ca': 0, 'nb': 0})
        daily.append({
            'date': d,
            'ca': round((p['ca'] or 0) + (inv['ca'] or 0), 2),
            'nb_ventes': (p['nb'] or 0) + (inv['nb'] or 0)
        })

    return jsonify(daily)


@reports_bp.route('/api/reports/margins', methods=['GET'])
def reports_margins():
    date_start = request.args.get('date_start')
    date_end = request.args.get('date_end')
    period = _safe_int(request.args.get('period', 30), 30)
    category = request.args.get('category', '')

    pos_filter, inv_filter, params = _build_date_filters(date_start, date_end, period)
    tp = tuple(params) if params else ()
    cat_filter = f"AND p.category = '{category}'" if category else ""

    with get_db() as conn:
        margins = conn.execute(f"""
            SELECT p.category,
                   SUM(ii.line_total) as vente,
                   SUM(ii.quantity * COALESCE(
                       (SELECT AVG(poi.unit_price) FROM purchase_order_items poi
                        JOIN purchase_orders po ON poi.order_id = po.id
                        WHERE poi.product_id = ii.product_id AND po.status IN ('received', 'recue')),
                       p.purchase_price_avg, p.price_base, 0)) as achat
            FROM invoice_items ii
            JOIN invoices i ON ii.invoice_id = i.id
            JOIN products p ON ii.product_id = p.id
            WHERE i.status != 'annulee' AND (i.type IS NULL OR i.type != 'fournisseur')
            {inv_filter} {cat_filter}
            GROUP BY p.category
        """, tp).fetchall()

        pos_margins = conn.execute(f"""
            SELECT p.category,
                   SUM(pti.line_total) as vente,
                   SUM(pti.quantity * COALESCE(
                       (SELECT AVG(poi.unit_price) FROM purchase_order_items poi
                        JOIN purchase_orders po ON poi.order_id = po.id
                        WHERE poi.product_id = pti.product_id AND po.status IN ('received', 'recue')),
                       p.purchase_price_avg, p.price_base, 0)) as achat
            FROM pos_transaction_items pti
            JOIN pos_transactions t ON pti.transaction_id = t.id
            JOIN products p ON pti.product_id = p.id
            WHERE t.status = 'completed'
            {pos_filter} {cat_filter}
            GROUP BY p.category
        """, tp).fetchall()

        cat_data = {}
        for row in list(margins) + list(pos_margins):
            cat = row['category'] or 'Sans categorie'
            if cat not in cat_data:
                cat_data[cat] = {'vente': 0, 'achat': 0}
            cat_data[cat]['vente'] += (row['vente'] or 0)
            cat_data[cat]['achat'] += (row['achat'] or 0)

        result = []
        total_vente = 0
        total_achat = 0
        for cat, data in cat_data.items():
            v = data['vente']
            a = data['achat']
            marge = ((v - a) / v * 100) if v > 0 else 0
            result.append({
                'category': cat,
                'vente': round(v, 2),
                'achat': round(a, 2),
                'marge_pct': round(marge, 1),
                'marge_montant': round(v - a, 2)
            })
            total_vente += v
            total_achat += a

        marge_globale = ((total_vente - total_achat) / total_vente * 100) if total_vente > 0 else 0

        return jsonify({
            'marge_globale': round(marge_globale, 1),
            'marge_montant': round(total_vente - total_achat, 2),
            'categories': sorted(result, key=lambda x: x['marge_montant'], reverse=True)
        })


@reports_bp.route('/api/reports/velocity', methods=['GET'])
def reports_velocity():
    date_start = request.args.get('date_start')
    date_end = request.args.get('date_end')
    period = _safe_int(request.args.get('period', 30), 30)

    pos_filter, inv_filter, params = _build_date_filters(date_start, date_end, period)
    tp = tuple(params) if params else ()

    with get_db() as conn:
        total_stock_qty = conn.execute("SELECT COALESCE(SUM(quantity), 0) FROM products WHERE is_deleted = 0").fetchone()[0]
        total_stock_value = conn.execute("SELECT COALESCE(SUM(quantity * price), 0) FROM products WHERE is_deleted = 0").fetchone()[0]

        out_movements = conn.execute(
            f"SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE type IN ('out','sale','destruction') {pos_filter.replace('t.', '')}",
            tp
        ).fetchone()[0]
        in_movements = conn.execute(
            f"SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE type IN ('in','retour') {pos_filter.replace('t.', '')}",
            tp
        ).fetchone()[0]

        avg_daily_out = out_movements / period if period > 0 else 1
        dio = total_stock_qty / avg_daily_out if avg_daily_out > 0 else 0
        rotation_rate = (out_movements / total_stock_qty * 100) if total_stock_qty > 0 else 0

        dusty = conn.execute("""
            SELECT p.id, p.name, p.sku, p.quantity, p.price,
                   MAX(m.created_at) as last_movement
            FROM products p
            LEFT JOIN stock_movements m ON p.id = m.product_id
            WHERE p.is_deleted = 0 AND p.quantity > 0
            GROUP BY p.id
            HAVING MAX(m.created_at) IS NULL OR MAX(m.created_at) < date('now', '-90 days')
            ORDER BY p.quantity * p.price DESC
            LIMIT 10
        """).fetchall()

        return jsonify({
            'total_stock_qty': total_stock_qty,
            'total_stock_value': round(total_stock_value, 2),
            'out_movements': out_movements,
            'in_movements': in_movements,
            'dio': round(dio, 1),
            'rotation_rate': round(rotation_rate, 1),
            'dusty_stock': [dict(d) for d in dusty]
        })


@reports_bp.route('/api/reports/exceptions', methods=['GET'])
def reports_exceptions():
    with get_db() as conn:
        out_of_stock = conn.execute("""
            SELECT id, name, sku, min_quantity, price
            FROM products WHERE (quantity <= 0 OR quantity IS NULL) AND is_deleted = 0
            ORDER BY min_quantity DESC LIMIT 10
        """).fetchall()

        low_stock = conn.execute("""
            SELECT id, name, sku, quantity, min_quantity, price,
                   (min_quantity - quantity) as needed
            FROM products WHERE quantity < min_quantity AND is_deleted = 0
            ORDER BY needed DESC LIMIT 10
        """).fetchall()

        negative_margins = conn.execute("""
            SELECT p.name, p.sku,
                   SUM(ii.line_total) as vente,
                   SUM(ii.quantity * COALESCE(p.purchase_price_avg, p.price_base, 0)) as achat
            FROM invoice_items ii
            JOIN invoices i ON ii.invoice_id = i.id
            JOIN products p ON ii.product_id = p.id
            WHERE i.status != 'annulee' AND (i.type IS NULL OR i.type != 'fournisseur')
            GROUP BY p.id
            HAVING vente < achat
            ORDER BY (vente - achat) ASC
            LIMIT 10
        """).fetchall()

        return jsonify({
            'out_of_stock': [dict(r) for r in out_of_stock],
            'low_stock': [dict(r) for r in low_stock],
            'negative_margins': [dict(r) for r in negative_margins]
        })


@reports_bp.route('/api/reports/compare', methods=['GET'])
def reports_compare():
    a_from = request.args.get('period_a_from')
    a_to = request.args.get('period_a_to')
    b_from = request.args.get('period_b_from')
    b_to = request.args.get('period_b_to')

    if not all([a_from, a_to, b_from, b_to]):
        return jsonify({'error': '4 periodes requises: period_a_from, period_a_to, period_b_from, period_b_to'}), 400

    def _calc_period(conn, df, dt):
        pos_f = f"AND date(t.created_at) BETWEEN '{df}' AND '{dt}'"
        inv_f = f"AND date(i.paid_at) BETWEEN '{df}' AND '{dt}'"
        ca = conn.execute(f"""
            SELECT COALESCE(SUM(total), 0) FROM pos_transactions WHERE status = 'completed' {pos_f}
        """).fetchone()[0] + conn.execute(f"""
            SELECT COALESCE(SUM(total), 0) FROM invoices WHERE status = 'payee'
            AND (type IS NULL OR type != 'fournisseur') AND is_credit_payment = 0
            AND NOT EXISTS (SELECT 1 FROM pos_transactions pt WHERE pt.invoice_id = invoices.id)
            {inv_f}
        """).fetchone()[0]
        nb = conn.execute(f"SELECT COUNT(*) FROM pos_transactions WHERE status = 'completed' {pos_f}").fetchone()[0] + \
             conn.execute(f"""SELECT COUNT(*) FROM invoices WHERE status = 'payee'
                AND (type IS NULL OR type != 'fournisseur') AND is_credit_payment = 0
                AND NOT EXISTS (SELECT 1 FROM pos_transactions pt WHERE pt.invoice_id = invoices.id)
                {inv_f}""").fetchone()[0]
        return {'ca': round(ca, 2), 'nb_ventes': nb, 'ticket_moyen': round(ca / nb, 2) if nb > 0 else 0}

    with get_db() as conn:
        period_a = _calc_period(conn, a_from, a_to)
        period_b = _calc_period(conn, b_from, b_to)

    ca_trend = ((period_a['ca'] - period_b['ca']) / period_b['ca'] * 100) if period_b['ca'] > 0 else 0
    nb_trend = ((period_a['nb_ventes'] - period_b['nb_ventes']) / period_b['nb_ventes'] * 100) if period_b['nb_ventes'] > 0 else 0

    return jsonify({
        'period_a': period_a,
        'period_b': period_b,
        'ca_trend': round(ca_trend, 1),
        'nb_trend': round(nb_trend, 1)
    })
