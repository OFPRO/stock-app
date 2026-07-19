from flask import Blueprint, request, jsonify
from routes.db import get_db, validate_id, _safe_err

locations_bp = Blueprint('locations', __name__)

@locations_bp.route('/api/locations', methods=['GET'])
def get_locations():
    warehouse_id = request.args.get('warehouse_id')
    conn = get_db()
    wid = validate_id(warehouse_id)
    if wid:
        locations = conn.execute('SELECT * FROM locations WHERE warehouse_id=? ORDER BY name', (wid,)).fetchall()
    else:
        locations = conn.execute('''
            SELECT l.*, w.name as warehouse_name
            FROM locations l
            JOIN warehouses w ON l.warehouse_id = w.id
            ORDER BY w.name, l.name
        ''').fetchall()
    conn.close()
    return jsonify([dict(l) for l in locations])

@locations_bp.route('/api/locations', methods=['POST'])
def add_location():
    data = request.json
    if not data.get('name') or not data.get('warehouse_id'):
        return jsonify({'error': 'Nom et entrepôt requis'}), 400
    conn = get_db()
    try:
        conn.execute('INSERT INTO locations (warehouse_id, name, type, capacity) VALUES (?, ?, ?, ?)',
                     (data['warehouse_id'], data['name'], data.get('type', 'rack'), data.get('capacity')))
        conn.commit()
        return jsonify({'success': True})
    except Exception as e:
        return jsonify({'error': _safe_err(e)}), 400
    finally:
        conn.close()

@locations_bp.route('/api/locations/<int:location_id>', methods=['PUT'])
def update_location(location_id):
    data = request.json
    conn = get_db()
    try:
        conn.execute('UPDATE locations SET name=?, type=?, capacity=?, warehouse_id=? WHERE id=?',
                     (data.get('name'), data.get('type'), data.get('capacity'), data.get('warehouse_id'), location_id))
        conn.commit()
        return jsonify({'success': True})
    except Exception as e:
        return jsonify({'error': _safe_err(e)}), 400
    finally:
        conn.close()

@locations_bp.route('/api/locations/<int:location_id>', methods=['DELETE'])
def delete_location(location_id):
    conn = get_db()
    stock_count = conn.execute('SELECT COUNT(*) as cnt FROM stock WHERE location_id=?', (location_id,)).fetchone()
    if stock_count and stock_count['cnt'] > 0:
        conn.close()
        return jsonify({'error': 'Impossible de supprimer : des produits sont stockés dans cette zone'}), 400
    try:
        conn.execute('DELETE FROM locations WHERE id=?', (location_id,))
        conn.commit()
        return jsonify({'success': True})
    except Exception as e:
        return jsonify({'error': _safe_err(e)}), 400
    finally:
        conn.close()
