from flask import Blueprint, request, jsonify
from routes.db import get_db, validate_id
from datetime import datetime

customers_bp = Blueprint('customers', __name__)

@customers_bp.route('/api/customers', methods=['GET'])
def get_customers():
    search = request.args.get('search', '')
    conn = get_db()
    if search:
        customers = conn.execute('''
            SELECT * FROM customers
            WHERE name LIKE ? OR client_code LIKE ? OR email LIKE ?
            ORDER BY name
        ''', (f'%{search}%', f'%{search}%', f'%{search}%')).fetchall()
    else:
        customers = conn.execute('SELECT * FROM customers ORDER BY name').fetchall()
    conn.close()
    return jsonify([dict(c) for c in customers])

@customers_bp.route('/api/customers', methods=['POST'])
def add_customer():
    data = request.json
    conn = get_db()
    try:
        existing = conn.execute('SELECT COUNT(*) FROM customers').fetchone()[0]
        client_code = f"CLI-{datetime.now().strftime('%Y%m%d')}-{existing + 1:04d}"
        conn.execute('''
            INSERT INTO customers (name, type, email, phone, address, client_code, discount_rate, is_loyal, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', (data['name'], data.get('type', 'normal'), data.get('email', ''),
              data.get('phone', ''), data.get('address', ''), client_code,
              data.get('discount_rate', 0), data.get('is_loyal', 0), data.get('notes', '')))
        conn.commit()
        conn.close()
        return jsonify({'success': True})
    except Exception as e:
        conn.close()
        return jsonify({'error': str(e)}), 400

@customers_bp.route('/api/customers/<int:customer_id>', methods=['GET'])
def get_customer(customer_id):
    conn = get_db()
    customer = conn.execute('SELECT * FROM customers WHERE id=?', (customer_id,)).fetchone()
    conn.close()
    if customer:
        return jsonify(dict(customer))
    return jsonify({'error': 'Client non trouvé'}), 404

@customers_bp.route('/api/customers/<int:customer_id>', methods=['PUT'])
def update_customer(customer_id):
    data = request.json
    conn = get_db()
    try:
        conn.execute('''
            UPDATE customers SET name=?, type=?, email=?, phone=?, address=?, discount_rate=?, is_loyal=?, notes=?, updated_at=CURRENT_TIMESTAMP
            WHERE id=?
        ''', (data['name'], data.get('type', 'normal'), data.get('email', ''),
              data.get('phone', ''), data.get('address', ''), data.get('discount_rate', 0),
              data.get('is_loyal', 0), data.get('notes', ''), customer_id))
        conn.commit()
    finally:
        conn.close()
    return jsonify({'success': True})

@customers_bp.route('/api/customers/<int:customer_id>', methods=['DELETE'])
def delete_customer(customer_id):
    conn = get_db()
    has_invoices = conn.execute('SELECT COUNT(*) FROM invoices WHERE customer_id=?', (customer_id,)).fetchone()[0]
    if has_invoices > 0:
        conn.close()
        return jsonify({'error': 'Impossible de supprimer un client avec des factures'}), 400
    conn.execute('DELETE FROM customers WHERE id=?', (customer_id,))
    conn.commit()
    conn.close()
    return jsonify({'success': True})
