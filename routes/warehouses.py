from flask import Blueprint, request, jsonify
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
