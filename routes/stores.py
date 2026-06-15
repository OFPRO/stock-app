from flask import Blueprint, request, jsonify, session
from routes.db import get_catalog_db, get_db, categories_data, init_store_db, resolve_db_path

stores_bp = Blueprint('stores', __name__)

def _get_active_store():
    return session.get('active_store_id', 1)

@stores_bp.route('/api/stores', methods=['GET'])
def get_stores():
    conn = get_catalog_db()
    rows = conn.execute('SELECT * FROM stores ORDER BY id').fetchall()
    conn.close()
    return jsonify([dict(r) for r in rows])

@stores_bp.route('/api/stores/current', methods=['GET'])
def get_current_store():
    store_id = _get_active_store()
    conn = get_catalog_db()
    store = conn.execute('SELECT * FROM stores WHERE id = ?', (store_id,)).fetchone()
    conn.close()
    if not store:
        return jsonify({'error': 'Magasin introuvable'}), 404
    return jsonify(dict(store))

@stores_bp.route('/api/stores', methods=['POST'])
def create_store():
    data = request.get_json()
    name = (data.get('name') or '').strip()
    if not name:
        return jsonify({'error': 'Le nom du magasin est requis'}), 400

    catalog = get_catalog_db()
    try:
        max_id = catalog.execute('SELECT COALESCE(MAX(id), 0) + 1 FROM stores').fetchone()[0]
        catalog.execute(
            'INSERT INTO stores (id, name, code, is_active, is_archived) VALUES (?, ?, ?, 1, 0)',
            (max_id, name, str(max_id))
        )
        catalog.commit()
    except Exception as e:
        catalog.rollback()
        catalog.close()
        return jsonify({'error': str(e)}), 400
    catalog.close()

    try:
        init_store_db(max_id, name)
    except Exception as e:
        catalog = get_catalog_db()
        catalog.execute('DELETE FROM stores WHERE id = ?', (max_id,))
        catalog.commit()
        catalog.close()
        return jsonify({'error': f'Erreur création base: {str(e)}'}), 500

    return jsonify({'id': max_id, 'name': name, 'message': 'Magasin créé avec succès'}), 201

@stores_bp.route('/api/stores/<int:store_id>/switch', methods=['POST'])
def switch_store(store_id):
    catalog = get_catalog_db()
    store = catalog.execute('SELECT * FROM stores WHERE id = ? AND is_archived = 0', (store_id,)).fetchone()
    catalog.close()
    if not store:
        return jsonify({'error': 'Magasin introuvable ou archivé'}), 404
    session['active_store_id'] = store_id
    return jsonify({'id': store_id, 'name': store['name'], 'message': f'Basculé vers {store["name"]}'})

@stores_bp.route('/api/stores/<int:store_id>/archive', methods=['POST'])
def archive_store(store_id):
    catalog = get_catalog_db()
    store = catalog.execute('SELECT * FROM stores WHERE id = ? AND is_archived = 0', (store_id,)).fetchone()
    if not store:
        catalog.close()
        return jsonify({'error': 'Magasin introuvable ou déjà archivé'}), 404
    catalog.execute('UPDATE stores SET is_archived = 1, archived_at = CURRENT_TIMESTAMP WHERE id = ?', (store_id,))
    catalog.commit()
    catalog.close()

    if _get_active_store() == store_id:
        session['active_store_id'] = 1

    return jsonify({'message': 'Magasin archivé', 'id': store_id})

@stores_bp.route('/api/stores/<int:store_id>/reactivate', methods=['POST'])
def reactivate_store(store_id):
    catalog = get_catalog_db()
    store = catalog.execute('SELECT * FROM stores WHERE id = ? AND is_archived = 1', (store_id,)).fetchone()
    if not store:
        catalog.close()
        return jsonify({'error': 'Magasin introuvable ou déjà actif'}), 404
    catalog.execute('UPDATE stores SET is_archived = 0, archived_at = NULL WHERE id = ?', (store_id,))
    catalog.commit()
    catalog.close()

    session['active_store_id'] = store_id
    return jsonify({'message': 'Magasin réactivé', 'id': store_id})

@stores_bp.route('/api/settings/categories', methods=['GET'])
def get_categories():
    conn = get_db()
    rows = conn.execute('SELECT * FROM categories ORDER BY id').fetchall()
    conn.close()
    return jsonify([dict(r) for r in rows])

@stores_bp.route('/api/settings/categories', methods=['POST'])
def add_category():
    data = request.get_json()
    name_fr = (data.get('name_fr') or '').strip()
    name_ar = (data.get('name_ar') or '').strip()
    if not name_fr or not name_ar:
        return jsonify({'error': 'Les deux noms (français et arabe) sont requis'}), 400
    conn = get_db()
    try:
        conn.execute('INSERT INTO categories (name_ar, name_fr) VALUES (?, ?)', (name_ar, name_fr))
        conn.commit()
        cat_id = conn.execute('SELECT last_insert_rowid()').fetchone()[0]
        return jsonify({'id': cat_id, 'name_ar': name_ar, 'name_fr': name_fr}), 201
    except Exception as e:
        conn.rollback()
        return jsonify({'error': str(e)}), 400
    finally:
        conn.close()

@stores_bp.route('/api/settings/categories/<int:cat_id>', methods=['PUT'])
def update_category(cat_id):
    data = request.get_json()
    name_fr = (data.get('name_fr') or '').strip()
    name_ar = (data.get('name_ar') or '').strip()
    if not name_fr or not name_ar:
        return jsonify({'error': 'Les deux noms sont requis'}), 400
    conn = get_db()
    conn.execute('UPDATE categories SET name_ar = ?, name_fr = ? WHERE id = ?', (name_ar, name_fr, cat_id))
    conn.commit()
    conn.close()
    return jsonify({'message': 'Catégorie mise à jour'})

@stores_bp.route('/api/settings/categories/<int:cat_id>', methods=['DELETE'])
def delete_category(cat_id):
    conn = get_db()
    cat = conn.execute('SELECT name_fr FROM categories WHERE id = ?', (cat_id,)).fetchone()
    if not cat:
        conn.close()
        return jsonify({'error': 'Catégorie introuvable'}), 404
    conn.execute('DELETE FROM categories WHERE id = ?', (cat_id,))
    conn.commit()
    conn.close()
    return jsonify({'message': 'Catégorie supprimée'})
