function loadBackups() {
    fetch('/api/backup/list').then(r => r.json()).then(backups => {
        renderBackupList(backups);
    }).catch(e => {
        document.getElementById('backupListArea').innerHTML = '<div class="alert alert-danger">Erreur: ' + escapeHtml(e.message) + '</div>';
    });
}

function renderBackupList(backups) {
    const area = document.getElementById('backupListArea');
    if (!backups || backups.length === 0) {
        area.innerHTML = '<div class="empty"><i class="fas fa-shield-alt" style="font-size:2rem;color:var(--text-light);opacity:0.3;"></i><p style="color:var(--text-light);margin-top:0.5rem;">Aucune sauvegarde disponible</p></div>';
        return;
    }
    let html = '<table class="table"><thead><tr><th>Date</th><th>Taille</th><th>Fichiers</th><th style="width:150px">Actions</th></tr></thead><tbody>';
    backups.forEach(b => {
        html += '<tr>';
        html += '<td>' + escapeHtml(b.created_at) + '</td>';
        html += '<td>' + formatBytes(b.total_size) + '</td>';
        html += '<td>' + b.nb_files + ' fichier(s)</td>';
        html += '<td>';
        html += '<button class="btn btn-sm btn-outline" onclick="downloadBackup(\'' + b.id + '\')" title="Télécharger"><i class="fas fa-download"></i></button> ';
        html += '<button class="btn btn-sm btn-outline" onclick="deleteBackup(\'' + b.id + '\')" title="Supprimer" style="color:var(--danger);"><i class="fas fa-trash"></i></button>';
        html += '</td></tr>';
    });
    html += '</tbody></table>';
    area.innerHTML = html;
}

function createBackup() {
    const btn = document.getElementById('btnCreateBackup');
    const status = document.getElementById('backupStatus');
    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Sauvegarde en cours...';
    status.innerHTML = '';
    status.style.display = 'none';

    fetch('/api/backup/create', { method: 'POST' })
        .then(r => r.json())
        .then(data => {
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-plus"></i> Créer une sauvegarde';
            if (data.success) {
                status.innerHTML = '<div class="alert alert-success">Sauvegarde créée (' + formatBytes(data.total_size) + ')</div>';
                status.style.display = 'block';
                loadBackups();
            } else {
                status.innerHTML = '<div class="alert alert-danger">' + escapeHtml(data.error || 'Erreur') + '</div>';
                status.style.display = 'block';
            }
        })
        .catch(e => {
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-plus"></i> Créer une sauvegarde';
            status.innerHTML = '<div class="alert alert-danger">Erreur: ' + escapeHtml(e.message) + '</div>';
            status.style.display = 'block';
        });
}

function downloadBackup(id) {
    window.open('/api/backup/download/' + id, '_blank');
}

function deleteBackup(id) {
    if (!confirm('Supprimer cette sauvegarde ?')) return;
    fetch('/api/backup/' + id, { method: 'DELETE' })
        .then(r => r.json())
        .then(data => {
            if (data.success) loadBackups();
            else showError(data.error || 'Erreur');
        })
        .catch(e => showError(e.message));
}

function restoreBackup() {
    const input = document.getElementById('backupRestoreInput');
    const file = input.files[0];
    if (!file) {
        showError('Sélectionnez un fichier .zip');
        return;
    }
    if (!confirm('ATTENTION : Toutes les données actuelles seront remplacées.\n\nÊtes-vous sûr ?')) return;

    const btn = document.getElementById('btnRestoreBackup');
    const status = document.getElementById('backupStatus');
    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Restauration en cours...';
    status.innerHTML = '';

    const formData = new FormData();
    formData.append('file', file);

    fetch('/api/backup/restore', { method: 'POST', body: formData })
        .then(r => r.json())
        .then(data => {
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-upload"></i> Restaurer';
            if (data.success) {
                status.innerHTML = '<div class="alert alert-success">' + escapeHtml(data.message) + ' — Rechargement...</div>';
                status.style.display = 'block';
                setTimeout(() => location.reload(), 2000);
            } else {
                status.innerHTML = '<div class="alert alert-danger">' + escapeHtml(data.error || 'Erreur') + '</div>';
                status.style.display = 'block';
            }
        })
        .catch(e => {
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-upload"></i> Restaurer';
            status.innerHTML = '<div class="alert alert-danger">Erreur: ' + escapeHtml(e.message) + '</div>';
            status.style.display = 'block';
        });
}

function formatBytes(bytes) {
    if (bytes === 0) return '0 o';
    const k = 1024;
    const sizes = ['o', 'Ko', 'Mo', 'Go'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}
