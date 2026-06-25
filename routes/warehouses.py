import io
import os
import traceback
from datetime import datetime
from flask import Blueprint, request, jsonify, Response
from fpdf import FPDF
from routes.db import get_db, validate_id

warehouses_bp = Blueprint('warehouses', __name__)

@warehouses_bp.route('/api/warehouses', methods=['GET'])
def get_warehouses():
    conn = get_db()
    warehouses = conn.execute('SELECT * FROM warehouses ORDER BY is_default DESC, name').fetchall()
    conn.close()
    return jsonify([dict(w) for w in warehouses])

@warehouses_bp.route('/api/warehouses', methods=['POST'])
def add_warehouse():
    data = request.json
    conn = get_db()
    try:
        conn.execute('''
            INSERT INTO warehouses (name, address, manager)
            VALUES (?, ?, ?)
        ''', (data['name'], data.get('address', ''), data.get('manager', '')))
        conn.commit()
        return jsonify({'success': True})
    except Exception as e:
        return jsonify({'error': str(e)}), 400
    finally:
        conn.close()

@warehouses_bp.route('/api/warehouses/<int:warehouse_id>', methods=['GET'])
def get_warehouse(warehouse_id):
    conn = get_db()
    warehouse = conn.execute('SELECT * FROM warehouses WHERE id = ?', (warehouse_id,)).fetchone()
    conn.close()
    if warehouse is None:
        return jsonify({'error': 'Entrepôt non trouvé'}), 404
    return jsonify(dict(warehouse))

@warehouses_bp.route('/api/warehouses/<int:warehouse_id>', methods=['PUT'])
def update_warehouse(warehouse_id):
    data = request.json
    conn = get_db()
    cursor = conn.execute('UPDATE warehouses SET name=?, address=?, manager=? WHERE id=?', (
        data.get('name', ''), data.get('address', ''), data.get('manager', ''), warehouse_id
    ))
    conn.commit()
    conn.close()
    if cursor.rowcount == 0:
        return jsonify({'error': 'Entrepôt non trouvé'}), 404
    return jsonify({'success': True})

@warehouses_bp.route('/api/warehouses/<int:warehouse_id>', methods=['DELETE'])
def delete_warehouse(warehouse_id):
    conn = get_db()
    cursor = conn.execute('DELETE FROM warehouses WHERE id = ?', (warehouse_id,))
    conn.commit()
    conn.close()
    if cursor.rowcount == 0:
        return jsonify({'error': 'Entrepôt non trouvé'}), 404
    return jsonify({'success': True})

@warehouses_bp.route('/api/stock/<int:product_id>', methods=['POST'])
def stock_movement(product_id):
    data = request.json
    movement_type = data.get('type')
    quantity = data.get('quantity', 0)
    note = data.get('note', '')
    lot_number = data.get('lot_number', '')
    serial_number = data.get('serial_number', '')
    location_id = data.get('location_id')

    conn = get_db()

    if movement_type == 'in':
        conn.execute('UPDATE products SET quantity = quantity + ? WHERE id=?', (quantity, product_id))
        if location_id:
            existing = conn.execute('SELECT id, quantity FROM stock WHERE product_id=? AND location_id=?',
                                    (product_id, location_id)).fetchone()
            if existing:
                conn.execute('UPDATE stock SET quantity = quantity + ? WHERE id=?', (quantity, existing['id']))
            else:
                conn.execute('INSERT INTO stock (product_id, location_id, quantity) VALUES (?, ?, ?)',
                           (product_id, location_id, quantity))
    elif movement_type == 'out':
        cursor = conn.execute('UPDATE products SET quantity = quantity - ? WHERE id=? AND quantity >= ?', (quantity, product_id, quantity))
        if cursor.rowcount == 0:
            current = conn.execute('SELECT quantity FROM products WHERE id=?', (product_id,)).fetchone()
            stock_qty = current['quantity'] if current else 0
            conn.close()
            return jsonify({'error': f'Stock insuffisant. Disponible: {stock_qty}, demandé: {quantity}'}), 400
        if location_id:
            existing = conn.execute('SELECT id, quantity FROM stock WHERE product_id=? AND location_id=?',
                                    (product_id, location_id)).fetchone()
            if existing:
                new_qty = max(0, existing['quantity'] - quantity)
                conn.execute('UPDATE stock SET quantity = ? WHERE id=?', (new_qty, existing['id']))

    loc_col = 'source_location_id' if movement_type == 'out' else 'dest_location_id'
    conn.execute(f'''
        INSERT INTO stock_movements (product_id, type, quantity, {loc_col}, lot_number, serial_number, note)
        VALUES (?, ?, ?, ?, ?, ?, ?)
    ''', (product_id, movement_type, quantity, location_id, lot_number, serial_number, note))
    conn.commit()
    conn.close()
    return jsonify({'success': True})

@warehouses_bp.route('/api/stock/transfer', methods=['POST'])
def stock_transfer():
    data = request.json
    product_id = data.get('product_id')
    quantity = data.get('quantity', 0)
    from_location_id = data.get('from_location_id')
    to_location_id = data.get('to_location_id')
    note = data.get('note', 'Transfert')

    conn = get_db()

    if from_location_id:
        from_stock = conn.execute('SELECT id, quantity FROM stock WHERE product_id=? AND location_id=?',
                                  (product_id, from_location_id)).fetchone()
        if not from_stock or from_stock['quantity'] < quantity:
            conn.close()
            return jsonify({'error': 'Stock insuffisant à la source'}), 400
        conn.execute('UPDATE stock SET quantity = quantity - ? WHERE id=? AND quantity >= ?', (quantity, from_stock['id'], quantity))

    if to_location_id:
        to_stock = conn.execute('SELECT id, quantity FROM stock WHERE product_id=? AND location_id=?',
                                (product_id, to_location_id)).fetchone()
        if to_stock:
            conn.execute('UPDATE stock SET quantity = quantity + ? WHERE id=?', (quantity, to_stock['id']))
        else:
            conn.execute('INSERT INTO stock (product_id, location_id, quantity) VALUES (?, ?, ?)',
                       (product_id, to_location_id, quantity))

    conn.execute('''
        INSERT INTO stock_movements (product_id, type, quantity, source_location_id, dest_location_id, note)
        VALUES (?, 'transfer', ?, ?, ?, ?)
    ''', (product_id, quantity, from_location_id, to_location_id, note))

    conn.commit()
    conn.close()
    return jsonify({'success': True})

@warehouses_bp.route('/api/stock/inter-warehouse', methods=['POST'])
def inter_warehouse_transfer():
    data = request.json
    product_id = data.get('product_id')
    quantity = data.get('quantity', 0)
    from_warehouse_id = data.get('from_warehouse_id')
    to_warehouse_id = data.get('to_warehouse_id')
    note = data.get('note', 'Transfert inter-entrepôt')

    conn = get_db()

    from_loc = conn.execute('SELECT id FROM locations WHERE warehouse_id=? LIMIT 1', (from_warehouse_id,)).fetchone()
    to_loc = conn.execute('SELECT id FROM locations WHERE warehouse_id=? LIMIT 1', (to_warehouse_id,)).fetchone()

    if not from_loc or not to_loc:
        conn.close()
        return jsonify({'error': 'Entrepôts non configurés'}), 400

    if from_warehouse_id:
        from_stock = conn.execute('''
            SELECT s.id, s.quantity FROM stock s
            JOIN locations l ON s.location_id = l.id
            WHERE s.product_id=? AND l.warehouse_id=?
        ''', (product_id, from_warehouse_id)).fetchone()

        if not from_stock or from_stock['quantity'] < quantity:
            conn.close()
            return jsonify({'error': "Stock insuffisant dans l'entrepôt source"}), 400
        conn.execute('UPDATE stock SET quantity = quantity - ? WHERE id=? AND quantity >= ?', (quantity, from_stock['id'], quantity))

    to_stock = conn.execute('SELECT id, quantity FROM stock WHERE product_id=? AND location_id=?',
                            (product_id, to_loc['id'])).fetchone()
    if to_stock:
        conn.execute('UPDATE stock SET quantity = quantity + ? WHERE id=?', (quantity, to_stock['id']))
    else:
        conn.execute('INSERT INTO stock (product_id, location_id, quantity) VALUES (?, ?, ?)',
                   (product_id, to_loc['id'], quantity))

    conn.execute('''
        INSERT INTO stock_movements (product_id, type, quantity, source_location_id, dest_location_id, note)
        VALUES (?, 'inter_warehouse', ?, ?, ?, ?)
    ''', (product_id, quantity, from_loc['id'], to_loc['id'], note))

    conn.commit()
    conn.close()
    return jsonify({'success': True})

@warehouses_bp.route('/api/movements', methods=['GET'])
def get_all_movements():
    product_id = request.args.get('product_id')
    warehouse_id = request.args.get('warehouse_id')
    movement_type = request.args.get('type', '')

    conn = get_db()
    query = '''
        SELECT m.*, p.name as product_name,
               sl.name as source_location, dl.name as dest_location
        FROM stock_movements m
        JOIN products p ON m.product_id = p.id
        LEFT JOIN locations sl ON m.source_location_id = sl.id
        LEFT JOIN locations dl ON m.dest_location_id = dl.id
        WHERE 1=1
    '''
    params = []

    if product_id and product_id.isdigit():
        query += ' AND m.product_id = ?'
        params.append(int(product_id))
    wid = validate_id(warehouse_id)
    if wid:
        query += ' AND (p.warehouse_id = ? OR EXISTS (SELECT 1 FROM locations l JOIN stock s ON s.location_id = l.id WHERE s.product_id = p.id AND l.warehouse_id = ?))'
        params.extend([wid, wid])
    if movement_type:
        query += ' AND m.type = ?'
        params.append(movement_type)

    query += ' ORDER BY m.created_at DESC LIMIT 100'

    movements = conn.execute(query, params).fetchall()
    conn.close()
    return jsonify([dict(m) for m in movements])

@warehouses_bp.route('/api/movements/<int:product_id>', methods=['GET'])
def get_movements(product_id):
    conn = get_db()
    movements = conn.execute('''
        SELECT m.*, p.name as product_name,
               sl.name as source_location, dl.name as dest_location
        FROM stock_movements m
        JOIN products p ON m.product_id = p.id
        LEFT JOIN locations sl ON m.source_location_id = sl.id
        LEFT JOIN locations dl ON m.dest_location_id = dl.id
        WHERE m.product_id = ?
        ORDER BY m.created_at DESC
    ''', (product_id,)).fetchall()
    conn.close()
    return jsonify([dict(m) for m in movements])


def _sanitize_pdf(text, maxlen=0):
    s = ''.join(c if ord(c) < 256 else '?' for c in str(text))
    return s[:maxlen] if maxlen else s


_TYPE_LABELS = {
    'in': 'Entree',
    'out': 'Sortie',
    'transfer': 'Transfert',
    'inter_warehouse': 'Tr. Entrepot',
    'sale': 'Vente',
    'retour': 'Retour',
    'destruction': 'Destruction'
}


@warehouses_bp.route('/api/movements/export/pdf', methods=['GET'])
def export_movements_pdf():
    try:
        movement_type = request.args.get('type', '')
        product_id = request.args.get('product_id')
        warehouse_id = request.args.get('warehouse_id')

        with get_db() as conn:
            params = []
            query = '''
                SELECT m.*, p.name as product_name,
                       sl.name as source_location, dl.name as dest_location
                FROM stock_movements m
                JOIN products p ON m.product_id = p.id
                LEFT JOIN locations sl ON m.source_location_id = sl.id
                LEFT JOIN locations dl ON m.dest_location_id = dl.id
                WHERE 1=1
            '''
            if product_id and product_id.isdigit():
                query += ' AND m.product_id = ?'
                params.append(int(product_id))
            wid = validate_id(warehouse_id)
            if wid:
                query += ' AND (p.warehouse_id = ? OR EXISTS (SELECT 1 FROM locations l JOIN stock s ON s.location_id = l.id WHERE s.product_id = p.id AND l.warehouse_id = ?))'
                params.extend([wid, wid])
            if movement_type:
                query += ' AND m.type = ?'
                params.append(movement_type)
            query += ' ORDER BY m.created_at DESC'
            movements = conn.execute(query, params).fetchall()

        pdf = FPDF(orientation='L', unit='mm', format='A4')
        pdf.set_auto_page_break(auto=True, margin=15)
        pdf.add_page()

        logo_path = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'static', 'img', 'logo.png')
        if os.path.exists(logo_path):
            logo_w = 50
            x = pdf.l_margin + (pdf.epw - logo_w) / 2
            pdf.image(logo_path, x=x, w=logo_w)
            pdf.ln(5)

        pdf.set_font('Helvetica', 'B', 16)
        pdf.cell(0, 10, 'Liste des Mouvements de Stock', align='C', new_x='LMARGIN', new_y='NEXT')
        pdf.ln(2)

        pdf.set_font('Helvetica', '', 8)
        pdf.cell(0, 5, f"Genere le {datetime.now().strftime('%d/%m/%Y a %H:%M')}", align='C', new_x='LMARGIN', new_y='NEXT')
        if movement_type:
            label = _TYPE_LABELS.get(movement_type, movement_type)
            pdf.cell(0, 5, f"Type : {label}", align='C', new_x='LMARGIN', new_y='NEXT')
        pdf.cell(0, 5, f"Total : {len(movements)} mouvement(s)", align='C', new_x='LMARGIN', new_y='NEXT')
        pdf.ln(4)

        col_w = [8, 40, 70, 30, 20, 60]
        headers = ['#', 'Date', 'Produit', 'Type', 'Qte', 'Note']
        table_w = sum(col_w)
        x_start = (pdf.w - table_w) / 2

        def _draw_header():
            pdf.set_font('Helvetica', 'B', 9)
            pdf.set_fill_color(41, 128, 185)
            pdf.set_text_color(255, 255, 255)
            pdf.set_x(x_start)
            for i, h in enumerate(headers):
                pdf.cell(col_w[i], 7, h, border=1, align='C', fill=True)
            pdf.ln()
            pdf.set_text_color(0, 0, 0)
            pdf.set_font('Helvetica', '', 9)

        _draw_header()
        for idx, m in enumerate(movements, 1):
            if pdf.get_y() > 185:
                pdf.add_page()
                _draw_header()

            type_label = _TYPE_LABELS.get(m['type'], m['type'])
            type_class_style = 'success' if m['type'] in ('in', 'retour') else 'warning'

            row_h = 6
            pdf.set_x(x_start)
            pdf.cell(col_w[0], row_h, str(idx), border=1, align='C')
            pdf.cell(col_w[1], row_h, _sanitize_pdf(m['created_at'][:16] if m['created_at'] else '-', 18), border=1)
            pdf.cell(col_w[2], row_h, _sanitize_pdf(m['product_name'] or '-', 35), border=1)
            pdf.cell(col_w[3], row_h, type_label, border=1, align='C')
            pdf.cell(col_w[4], row_h, str(m['quantity'] or 0), border=1, align='C')
            pdf.cell(col_w[5], row_h, _sanitize_pdf(m['note'] or '-', 35), border=1)
            pdf.ln()

        pdf.ln(3)
        pdf.set_font('Helvetica', 'B', 10)
        pdf.cell(0, 6, f"Total mouvements : {len(movements)}", align='R', new_x='LMARGIN', new_y='NEXT')

        buf = io.BytesIO()
        pdf.output(buf)
        buf.seek(0)

        return Response(
            buf.getvalue(),
            mimetype='application/pdf',
            headers={'Content-Disposition': 'attachment; filename="mouvements.pdf"'}
        )
    except Exception as e:
        traceback.print_exc()
        return jsonify({'error': 'Erreur lors de la generation du PDF'}), 500
