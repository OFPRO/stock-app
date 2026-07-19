from flask import Blueprint, request, jsonify, send_file
from datetime import datetime
from routes.db import CATALOG_DB, resolve_db_path, get_db, get_catalog_db
import os
import re
import sqlite3
import zipfile
import tempfile

backup_bp = Blueprint('backup', __name__)

MAX_BACKUPS = 10
BACKUP_DIR = os.path.join(os.path.dirname(CATALOG_DB), 'backups')


def _get_all_store_ids():
    catalog = get_catalog_db()
    rows = catalog.execute(
        "SELECT id FROM stores WHERE is_active = 1 AND is_archived = 0"
    ).fetchall()
    catalog.close()
    return [r['id'] for r in rows]


def _backup_name():
    return 'backup_' + datetime.now().strftime('%Y%m%d_%H%M%S')


def _parse_backup_id(backup_id):
    return re.match(r'^backup_(\d{8}_\d{6})$', backup_id)


def _db_files_in(backup_path):
    return sorted([
        f for f in os.listdir(backup_path)
        if f.endswith('.db')
    ])


@backup_bp.route('/api/backup/create', methods=['POST'])
def create_backup():
    try:
        name = _backup_name()
        backup_path = os.path.join(BACKUP_DIR, name)
        os.makedirs(backup_path, exist_ok=True)

        backed_up = []

        conn = get_db(1)
        dest = os.path.join(backup_path, 'stock.db')
        conn.execute("VACUUM INTO ?", (dest,))
        conn.close()
        backed_up.append({'file': 'stock.db', 'size': os.path.getsize(dest)})

        for store_id in _get_all_store_ids():
            if store_id == 1:
                continue
            src_path = resolve_db_path(store_id)
            if not os.path.exists(src_path):
                continue
            filename = f'stock_{store_id}.db'
            dest = os.path.join(backup_path, filename)
            conn = get_db(store_id)
            conn.execute("VACUUM INTO ?", (dest,))
            conn.close()
            backed_up.append({'file': filename, 'size': os.path.getsize(dest)})

        total_size = sum(f['size'] for f in backed_up)

        _rotate_backups()

        return jsonify({
            'success': True,
            'id': name,
            'created_at': datetime.now().isoformat(),
            'files': backed_up,
            'total_size': total_size
        })
    except Exception as e:
        return jsonify({'error': 'Erreur lors de la sauvegarde: ' + str(e)}), 500


@backup_bp.route('/api/backup/list', methods=['GET'])
def list_backups():
    if not os.path.exists(BACKUP_DIR):
        return jsonify([])

    backups = []
    for name in sorted(os.listdir(BACKUP_DIR), reverse=True):
        if not _parse_backup_id(name):
            continue
        bp_path = os.path.join(BACKUP_DIR, name)
        if not os.path.isdir(bp_path):
            continue
        files = _db_files_in(bp_path)
        total_size = sum(
            os.path.getsize(os.path.join(bp_path, f)) for f in files
        )
        created = name.replace('backup_', '')
        backups.append({
            'id': name,
            'created_at': f'{created[:4]}-{created[4:6]}-{created[6:8]} {created[9:11]}:{created[11:13]}:{created[13:15]}',
            'total_size': total_size,
            'nb_files': len(files),
            'files': files
        })

    return jsonify(backups)


@backup_bp.route('/api/backup/download/<backup_id>', methods=['GET'])
def download_backup(backup_id):
    if not _parse_backup_id(backup_id):
        return jsonify({'error': 'ID invalide'}), 400

    backup_path = os.path.join(BACKUP_DIR, backup_id)
    if not os.path.isdir(backup_path):
        return jsonify({'error': 'Sauvegarde introuvable'}), 404

    zip_path = os.path.join(tempfile.gettempdir(), f'{backup_id}.zip')
    try:
        with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zf:
            for f in _db_files_in(backup_path):
                zf.write(os.path.join(backup_path, f), f)

        return send_file(
            zip_path,
            mimetype='application/zip',
            as_attachment=True,
            download_name=f'{backup_id}.zip'
        )
    except Exception as e:
        return jsonify({'error': 'Erreur téléchargement: ' + str(e)}), 500


@backup_bp.route('/api/backup/restore', methods=['POST'])
def restore_backup():
    if 'file' not in request.files:
        return jsonify({'error': 'Aucun fichier envoyé'}), 400

    file = request.files['file']
    if not file.filename.endswith('.zip'):
        return jsonify({'error': 'Format invalide (.zip requis)'}), 400

    try:
        tmp_dir = tempfile.mkdtemp()
        zip_path = os.path.join(tmp_dir, file.filename)
        file.save(zip_path)

        with zipfile.ZipFile(zip_path, 'r') as zf:
            db_files = [n for n in zf.namelist() if n.endswith('.db')]
            if not db_files:
                return jsonify({'error': 'Aucune base de données dans le fichier'}), 400

            for db_name in db_files:
                header = zf.read(db_name)[:16]
                if header != b'SQLite format 3\x00':
                    return jsonify({'error': f'{db_name} n\'est pas une base SQLite valide'}), 400

            zf.extractall(tmp_dir)

        catalog_restored = False
        for db_name in db_files:
            src = os.path.join(tmp_dir, db_name)

            if db_name == 'stock.db':
                dest = CATALOG_DB
                catalog_restored = True
            else:
                match = re.match(r'stock_(\d+)\.db', db_name)
                if match:
                    store_id = int(match.group(1))
                    dest = resolve_db_path(store_id)
                else:
                    continue

            conn = sqlite3.connect(dest, timeout=30)
            conn.execute('PRAGMA wal_checkpoint(TRUNCATE)')
            conn.close()

            import shutil
            shutil.copy2(src, dest)

        _cleanup_dir(tmp_dir)

        return jsonify({
            'success': True,
            'message': f'{len(db_files)} base(s) restaurée(s)',
            'files_restored': db_files
        })
    except Exception as e:
        _cleanup_dir(tmp_dir)
        return jsonify({'error': 'Erreur restauration: ' + str(e)}), 500


@backup_bp.route('/api/backup/<backup_id>', methods=['DELETE'])
def delete_backup(backup_id):
    if not _parse_backup_id(backup_id):
        return jsonify({'error': 'ID invalide'}), 400

    backup_path = os.path.join(BACKUP_DIR, backup_id)
    if not os.path.isdir(backup_path):
        return jsonify({'error': 'Sauvegarde introuvable'}), 404

    import shutil
    shutil.rmtree(backup_path)
    return jsonify({'success': True, 'message': 'Sauvegarde supprimée'})


def _rotate_backups():
    if not os.path.exists(BACKUP_DIR):
        return
    dirs = sorted([
        d for d in os.listdir(BACKUP_DIR)
        if os.path.isdir(os.path.join(BACKUP_DIR, d)) and _parse_backup_id(d)
    ])
    while len(dirs) > MAX_BACKUPS:
        oldest = dirs.pop(0)
        import shutil
        shutil.rmtree(os.path.join(BACKUP_DIR, oldest))


def _cleanup_dir(path):
    try:
        import shutil
        if os.path.isdir(path):
            shutil.rmtree(path)
    except Exception:
        pass
