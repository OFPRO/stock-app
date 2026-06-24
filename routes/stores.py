import os, time, shutil
from datetime import datetime
from flask import Blueprint, request, jsonify, session
from routes.db import get_catalog_db, get_db, categories_data, init_store_db, resolve_db_path
from services.printing_service import check_printer_status, list_printers, discover_printers
from services.escpos_receipt import EscposPrinter, build_escpos_commands
from werkzeug.security import generate_password_hash, check_password_hash

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


@stores_bp.route('/api/settings/printer', methods=['GET'])
def get_printer_settings():
    conn = get_db()
    settings = conn.execute('SELECT * FROM printer_settings WHERE id = 1').fetchone()
    conn.close()
    if not settings:
        return jsonify({
            'connection_type': 'network', 'host': '', 'port': 9100,
            'usb_vendor_id': '', 'usb_product_id': '',
            'printer_name': '', 'auto_print': True, 'paper_width': 80,
        })
    return jsonify({
        'connection_type': settings['connection_type'],
        'host': settings['host'],
        'port': settings['port'],
        'usb_vendor_id': settings['usb_vendor_id'],
        'usb_product_id': settings['usb_product_id'],
        'printer_name': settings['printer_name'],
        'auto_print': bool(settings['auto_print']),
        'paper_width': settings['paper_width'],
    })


@stores_bp.route('/api/settings/printer', methods=['PUT'])
def update_printer_settings():
    data = request.get_json() or {}
    conn = get_db()
    current = conn.execute('SELECT * FROM printer_settings WHERE id = 1').fetchone()
    if current:
        merged = dict(current)
        merged.update({k: v for k, v in data.items() if k in merged})
    else:
        merged = {
            'connection_type': 'network',
            'host': '', 'port': 9100,
            'usb_vendor_id': '', 'usb_product_id': '',
            'printer_name': '', 'auto_print': 1, 'paper_width': 80,
        }
        merged.update(data)
    conn.execute('''
        UPDATE printer_settings SET
            connection_type = ?, host = ?, port = ?,
            usb_vendor_id = ?, usb_product_id = ?,
            printer_name = ?, auto_print = ?, paper_width = ?,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = 1
    ''', (
        merged.get('connection_type', 'network'),
        merged.get('host', ''),
        int(merged.get('port', 9100)),
        merged.get('usb_vendor_id', ''),
        merged.get('usb_product_id', ''),
        merged.get('printer_name', ''),
        1 if merged.get('auto_print', True) else 0,
        int(merged.get('paper_width', 80)),
    ))
    conn.commit()
    conn.close()
    return jsonify({'message': 'Paramètres imprimante mis à jour'})


@stores_bp.route('/api/settings/printer/status', methods=['GET'])
def printer_status():
    conn = get_db()
    settings = conn.execute('SELECT * FROM printer_settings WHERE id = 1').fetchone()
    conn.close()
    if not settings:
        return jsonify({'status': 'not_configured'})
    result = check_printer_status(dict(settings))
    return jsonify(result)


@stores_bp.route('/api/settings/printer/discover', methods=['GET'])
def discover_printers_endpoint():
    printers = discover_printers()
    return jsonify(printers)


@stores_bp.route('/api/pos/tickets/<ticket_number>/print', methods=['POST'])
def print_pos_ticket(ticket_number):
    conn = get_db()
    transaction = conn.execute('''
        SELECT t.*, s.session_number, s.opened_at
        FROM pos_transactions t
        LEFT JOIN pos_sessions s ON t.session_id = s.id
        WHERE t.transaction_number = ?
    ''', (ticket_number,)).fetchone()
    if not transaction:
        conn.close()
        return jsonify({'error': 'Ticket non trouvé'}), 404
    transaction = dict(transaction)
    items = conn.execute(
        'SELECT * FROM pos_transaction_items WHERE transaction_id = ?',
        (transaction['id'],)
    ).fetchall()
    items = [dict(item) for item in items]
    conn.close()

    ticket_data = {
        'ticket_number': transaction['transaction_number'],
        'created_at': (transaction.get('created_at') or '')[:19],
        'session_number': transaction.get('session_number', ''),
        'items': [
            {
                'product_name': it.get('product_name', 'Article'),
                'quantity': it.get('quantity', 0),
                'unit_price': it.get('unit_price', 0),
                'line_total': it.get('line_total', 0),
            }
            for it in items
        ],
        'subtotal': transaction.get('subtotal', 0) or 0,
        'discount_total': transaction.get('discount_total', 0) or 0,
        'tax_amount': transaction.get('tax_amount', 0) or 0,
        'total': transaction.get('total', 0) or 0,
        'payment_method': transaction.get('payment_method', 'cash'),
        'tendered_amount': transaction.get('tendered_amount', 0) or 0,
        'change_amount': transaction.get('change_given', 0) or 0,
    }

    printer_conn = get_db()
    printer_config = printer_conn.execute(
        'SELECT * FROM printer_settings WHERE id = 1'
    ).fetchone()
    printer_conn.close()

    if not printer_config:
        return jsonify({'error': 'Imprimante non configurée'}), 400

    from services.printing_service import print_ticket_raw
    result = print_ticket_raw(ticket_data, dict(printer_config))
    return jsonify(result)


@stores_bp.route('/api/settings/printer/test', methods=['POST'])
def test_printer():
    conn = get_db()
    settings = conn.execute('SELECT * FROM printer_settings WHERE id = 1').fetchone()
    conn.close()
    if not settings:
        return jsonify({'error': 'Imprimante non configurée'}), 400
    config = dict(settings)
    test_data = {
        'ticket_number': 'TEST-0001',
        'created_at': datetime.now().isoformat(),
        'session_number': 'TEST',
        'items': [
            {'product_name': 'Article test 1', 'quantity': 2, 'unit_price': 15.00, 'line_total': 30.00},
            {'product_name': 'Article test 2', 'quantity': 1, 'unit_price': 25.50, 'line_total': 25.50},
        ],
        'subtotal': 55.50,
        'discount_total': 0,
        'tax_amount': 11.10,
        'total': 66.60,
        'payment_method': 'cash',
        'tendered_amount': 70.00,
        'change_amount': 3.40,
        'customer_name': 'Test',
        'register_name': 'Test',
    }
    try:
        from services.printing_service import print_receipt
        result = print_receipt(test_data, config)
        if result.get('print_status') == 'success':
            return jsonify({'message': 'Impression test réussie', 'details': result})
        else:
            return jsonify({'error': result.get('print_error', 'Échec impression'), 'details': result}), 500
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@stores_bp.route('/api/settings/reset-password', methods=['GET'])
def get_reset_password_status():
    try:
        catalog = get_catalog_db()
        row = catalog.execute("SELECT value FROM settings WHERE key='reset_password'").fetchone()
        catalog.close()
        has_password = row is not None
        is_default = check_password_hash(row['value'], 'admin') if has_password else False
        return jsonify({'has_password': has_password, 'is_default': is_default})
    except Exception as e:
        import traceback
        print(f"[PASSWORD] get_reset_password_status: {e}\n{traceback.format_exc()}", flush=True)
        return jsonify({'has_password': False, 'is_default': False, 'error': str(e)}), 500


@stores_bp.route('/api/settings/reset-password', methods=['PUT'])
def set_reset_password():
    try:
        data = request.get_json(silent=True) or {}
        new_pw = data.get('new_password', '').strip()
        if len(new_pw) < 4:
            return jsonify({'error': 'Le mot de passe doit contenir au moins 4 caractères'}), 400

        catalog = get_catalog_db()
        row = catalog.execute("SELECT value FROM settings WHERE key='reset_password'").fetchone()
        if row:
            old_pw = data.get('current_password', '')
            if not check_password_hash(row['value'], old_pw):
                catalog.close()
                return jsonify({'error': 'Ancien mot de passe incorrect'}), 403

        catalog.execute("INSERT OR REPLACE INTO settings (key, value) VALUES ('reset_password', ?)",
                        (generate_password_hash(new_pw),))
        catalog.commit()
        catalog.close()
        return jsonify({'success': True, 'message': 'Mot de passe mis à jour'})
    except Exception as e:
        import traceback
        print(f"[PASSWORD] set_reset_password: {e}\n{traceback.format_exc()}", flush=True)
        return jsonify({'error': f'Erreur serveur: {str(e)}'}), 500


@stores_bp.route('/api/settings/verify-reset-password', methods=['POST'])
def verify_reset_password():
    try:
        data = request.get_json(silent=True) or {}
        password = data.get('password', '')
        catalog = get_catalog_db()
        row = catalog.execute("SELECT value FROM settings WHERE key='reset_password'").fetchone()
        catalog.close()
        if not row:
            return jsonify({'valid': False, 'error': 'Aucun mot de passe défini'})
        valid = check_password_hash(row['value'], password)
        is_default = check_password_hash(row['value'], 'admin')
        return jsonify({'valid': valid, 'is_default': is_default})
    except Exception as e:
        import traceback
        print(f"[PASSWORD] verify_reset_password: {e}\n{traceback.format_exc()}", flush=True)
        return jsonify({'valid': False, 'error': f'Erreur serveur: {str(e)}'}), 500


@stores_bp.route('/api/stores/<int:store_id>/backup', methods=['POST'])
def backup_store(store_id):
    catalog = get_catalog_db()
    store = catalog.execute('SELECT * FROM stores WHERE id = ?', (store_id,)).fetchone()
    if not store:
        catalog.close()
        return jsonify({'error': 'Magasin introuvable'}), 404

    now = datetime.now()
    name = f"{store['name']} (copie sauvegarde {now.strftime('%d/%m/%Y %H:%M')})"
    code = f"SAVE-{int(now.timestamp() * 1000)}"

    cur = catalog.execute(
        "INSERT INTO stores (name, code, is_active, is_archived) VALUES (?, ?, 0, 1)",
        (name, code)
    )
    new_id = cur.lastrowid
    catalog.commit()

    try:
        source_db = resolve_db_path(store_id)
        dest_db = resolve_db_path(new_id)
        if os.path.exists(dest_db):
            os.remove(dest_db)
        conn = get_db(store_id)
        conn.execute(f"VACUUM INTO '{dest_db}'")
        conn.close()
    except Exception as e:
        try: conn.close()
        except: pass
        catalog.execute('DELETE FROM stores WHERE id = ?', (new_id,))
        catalog.commit()
        catalog.close()
        return jsonify({'error': f'Erreur lors de la sauvegarde: {str(e)}'}), 500

    catalog.close()
    return jsonify({'success': True, 'id': new_id, 'name': name})
