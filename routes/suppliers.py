from flask import Blueprint, request, jsonify
from routes.db import get_db, _safe_err

suppliers_bp = Blueprint('suppliers', __name__)

@suppliers_bp.route('/api/suppliers', methods=['GET'])
def get_suppliers():
    conn = get_db()
    suppliers = conn.execute('SELECT * FROM suppliers ORDER BY name').fetchall()
    conn.close()
    return jsonify([dict(s) for s in suppliers])

@suppliers_bp.route('/api/suppliers', methods=['POST'])
def add_supplier():
    data = request.json
    conn = get_db()
    try:
        conn.execute('''
            INSERT INTO suppliers (name, email, phone, address, contact_person)
            VALUES (?, ?, ?, ?, ?)
        ''', (data['name'], data.get('email', ''), data.get('phone', ''),
              data.get('address', ''), data.get('contact_person', '')))
        conn.commit()
        conn.close()
        return jsonify({'success': True})
    except Exception as e:
        conn.close()
        return jsonify({'error': _safe_err(e)}), 400

@suppliers_bp.route('/api/suppliers/<int:supplier_id>', methods=['PUT'])
def update_supplier(supplier_id):
    data = request.json
    conn = get_db()
    try:
        conn.execute('''
            UPDATE suppliers SET name=?, email=?, phone=?, address=?, contact_person=?
            WHERE id=?
        ''', (data['name'], data.get('email', ''), data.get('phone', ''),
              data.get('address', ''), data.get('contact_person', ''), supplier_id))
        conn.commit()
    finally:
        conn.close()
    return jsonify({'success': True})

@suppliers_bp.route('/api/suppliers/<int:supplier_id>', methods=['DELETE'])
def delete_supplier(supplier_id):
    conn = get_db()
    conn.execute('DELETE FROM suppliers WHERE id=?', (supplier_id,))
    conn.commit()
    conn.close()
    return jsonify({'success': True})
