function toggleStoreSwitcherDropdown(e) {
    e.stopPropagation();
    var dd = document.getElementById('storeSwitcherDropdown');
    if (dd) dd.style.display = dd.style.display === 'none' ? 'block' : 'none';
}

document.addEventListener('click', function(e) {
    var dd = document.getElementById('storeSwitcherDropdown');
    var container = document.getElementById('storeSwitcherContainer');
    if (dd && container && !container.contains(e.target)) {
        dd.style.display = 'none';
    }
});

function loadSettings() {
    loadStoresList();
    loadSettingsCategories();
    loadPrinterSettings();
    loadSecuritySettings();
}

function loadStoresList() {
    fetch('/api/stores').then(r => r.json()).then(stores => {
        var html = '';
        var activeId = null;
        fetch('/api/stores/current').then(r => r.json()).then(current => {
            activeId = current.id;
            stores.forEach(function(s) {
                var isActive = s.id === activeId;
                var statusClass = s.is_archived ? 'store-archived' : (isActive ? 'store-active' : 'store-inactive');
                var statusIcon = s.is_archived ? 'fa-archive' : (isActive ? 'fa-check-circle' : 'fa-circle');
                var statusText = s.is_archived ? 'Archivé' : (isActive ? 'Actif' : 'Inactif');

                html += '<div class="store-card ' + statusClass + '">';
                html += '  <div class="store-card-info">';
                html += '    <div class="store-card-name"><i class="fas ' + statusIcon + '"></i> ' + escapeHtml(s.name) + '</div>';
                html += '    <div class="store-card-meta">Code: ' + escapeHtml(s.code) + ' · ' + statusText + '</div>';
                html += '  </div>';
                html += '  <div class="store-card-actions">';

                if (!s.is_archived && !isActive) {
                    html += '    <button class="btn btn-sm btn-primary" onclick="switchStore(' + s.id + ')"><i class="fas fa-sign-in-alt"></i> Activer</button>';
                }
                if (isActive && !s.is_archived) {
                    html += '    <span class="badge badge-active">Magasin actif</span>';
                }
                if (!s.is_archived && s.id !== 1) {
                    html += '    <button class="btn btn-sm btn-outline" onclick="archiveStore(' + s.id + ')" style="color:var(--danger);border-color:var(--danger);"><i class="fas fa-archive"></i> Archiver</button>';
                }
                if (s.is_archived) {
                    html += '    <button class="btn btn-sm btn-primary" onclick="reactivateStore(' + s.id + ')"><i class="fas fa-undo"></i> Réactiver</button>';
                }
                html += '  </div>';
                html += '</div>';
            });
            document.getElementById('storesList').innerHTML = html;
            updateStoreSwitcher(stores, activeId);
        }).catch(function() {});
    }).catch(function() {});
}

function updateStoreSwitcher(stores, activeId) {
    var html = '';
    stores.forEach(function(s) {
        if (s.is_archived) return;
        var icon = s.id === activeId ? 'fa-check-circle' : 'fa-circle';
        var cls = s.id === activeId ? 'store-switcher-item active' : 'store-switcher-item';
        html += '<div class="' + cls + '" onclick="switchStore(' + s.id + ')">';
        html += '  <i class="fas ' + icon + '" style="color:' + (s.id === activeId ? 'var(--success)' : 'var(--text-light)') + ';width:18px;"></i>';
        html += '  ' + escapeHtml(s.name);
        html += '</div>';
    });
    var switcher = document.getElementById('storeSwitcher');
    if (switcher) switcher.innerHTML = html;
}

function switchStore(storeId) {
    var btn = document.querySelector('.store-card-actions .btn-primary');
    fetch('/api/stores/' + storeId + '/switch', { method: 'POST' })
    .then(function(r) { return r.json(); })
    .then(function(data) {
        location.reload();
    })
    .catch(function(err) {
        showError('Erreur: ' + err);
    });
}

function archiveStore(storeId) {
    if (!confirm('Archiver ce magasin ? Ses données seront conservées mais inaccessibles.')) return;
    fetch('/api/stores/' + storeId + '/archive', { method: 'POST' })
    .then(function(r) { return r.json(); })
    .then(function() { loadStoresList(); })
    .catch(function(err) { showError('Erreur: ' + err); });
}

function reactivateStore(storeId) {
    if (!confirm('Réactiver ce magasin ? Vous serez basculé automatiquement.')) return;
    fetch('/api/stores/' + storeId + '/reactivate', { method: 'POST' })
    .then(function(r) { return r.json(); })
    .then(function() { location.reload(); })
    .catch(function(err) { showError('Erreur: ' + err); });
}

function openCreateStoreModal() {
    openModal('createStoreModal');
}

function createStore() {
    var name = document.getElementById('newStoreName').value.trim();
    if (!name) { showError('Veuillez entrer un nom pour le magasin'); return; }
    fetch('/api/stores', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: name })
    })
    .then(function(r) { return r.json(); })
    .then(function(data) {
        if (data.error) { showError(data.error); return; }
        closeModal('createStoreModal');
        document.getElementById('newStoreName').value = '';
        loadStoresList();
    })
    .catch(function(err) { showError('Erreur: ' + err); });
}

function loadSettingsCategories() {
    fetch('/api/settings/categories').then(function(r) { return r.json(); }).then(function(cats) {
        var html = '';
        cats.forEach(function(c) {
            html += '<tr>';
            html += '  <td>' + escapeHtml(c.name_fr) + '</td>';
            html += '  <td>' + escapeHtml(c.name_ar) + '</td>';
            html += '  <td class="text-right">';
            html += '    <button class="btn btn-sm btn-outline" onclick="editCategory(' + c.id + ', this)" style="margin-right:4px;"><i class="fas fa-edit"></i></button>';
            html += '    <button class="btn btn-sm btn-outline" onclick="deleteCategory(' + c.id + ')" style="color:var(--danger);border-color:var(--danger);"><i class="fas fa-trash"></i></button>';
            html += '  </td>';
            html += '</tr>';
        });
        document.getElementById('categoriesTableBody').innerHTML = html;
    }).catch(function() {});
}

function openAddCategoryModal() {
    document.getElementById('editCategoryId').value = '';
    document.getElementById('editCategoryNameFr').value = '';
    document.getElementById('editCategoryNameAr').value = '';
    document.getElementById('categoryModalTitle').textContent = 'Ajouter une catégorie';
    openModal('editCategoryModal');
}

function editCategory(id, btn) {
    var row = btn ? btn.closest('tr') : document.querySelector('#categoriesTableBody tr');
    var nameFr = row ? row.cells[0].textContent : '';
    var nameAr = row ? row.cells[1].textContent : '';
    document.getElementById('editCategoryId').value = id;
    document.getElementById('editCategoryNameFr').value = nameFr;
    document.getElementById('editCategoryNameAr').value = nameAr;
    document.getElementById('categoryModalTitle').textContent = 'Modifier la catégorie';
    openModal('editCategoryModal');
}

function saveCategory() {
    var id = document.getElementById('editCategoryId').value;
    var nameFr = document.getElementById('editCategoryNameFr').value.trim();
    var nameAr = document.getElementById('editCategoryNameAr').value.trim();
    if (!nameFr || !nameAr) { showError('Les deux noms sont requis'); return; }

    var url = id ? '/api/settings/categories/' + id : '/api/settings/categories';
    var method = id ? 'PUT' : 'POST';

    fetch(url, {
        method: method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name_fr: nameFr, name_ar: nameAr })
    })
    .then(function(r) { return r.json(); })
    .then(function(data) {
        if (data.error) { showError(data.error); return; }
        closeModal('editCategoryModal');
        loadSettingsCategories();
    })
    .catch(function(err) { showError('Erreur: ' + err); });
}

function deleteCategory(id) {
    if (!confirm('Supprimer cette catégorie ?')) return;
    fetch('/api/settings/categories/' + id, { method: 'DELETE' })
    .then(function(r) { return r.json(); })
    .then(function() { loadSettingsCategories(); })
    .catch(function(err) { showError('Erreur: ' + err); });
}

function loadPrinterSettings() {
    fetch('/api/settings/printer').then(function(r) { return r.json(); }).then(function(config) {
        fetch('/api/settings/printer/status').then(function(r) { return r.json(); }).then(function(status) {
            renderPrinterSettings(config, status);
        }).catch(function() { renderPrinterSettings(config, {}); });
    }).catch(function() {
        document.getElementById('printerSettingsBody').innerHTML = '<div class="empty"><i class="fas fa-print"></i><p>Erreur de chargement</p></div>';
    });
}

function renderPrinterSettings(config, status) {
    var badge = document.getElementById('printerStatusBadge');
    if (status.status === 'online') {
        badge.style.display = 'inline-block';
        badge.className = 'badge badge-success';
        badge.innerHTML = '<i class="fas fa-check-circle"></i> Connectée';
    } else if (status.status === 'offline') {
        badge.style.display = 'inline-block';
        badge.className = 'badge badge-danger';
        badge.innerHTML = '<i class="fas fa-exclamation-circle"></i> Hors ligne';
    } else {
        badge.style.display = 'inline-block';
        badge.className = 'badge badge-secondary';
        badge.innerHTML = '<i class="fas fa-question-circle"></i> Non configurée';
    }

    var html = '';
    html += '<div class="form-group" style="display:flex;align-items:center;gap:12px;margin-bottom:16px;">';
    html += '  <label style="margin:0;font-weight:600;">Impression automatique</label>';
    html += '  <input type="checkbox" id="printerAutoPrint" ' + (config.auto_print ? 'checked' : '') + ' onchange="savePrinterSetting(\'auto_print\', this.checked)" style="width:18px;height:18px;cursor:pointer;">';
    html += '</div>';

    html += '<div class="form-group">';
    html += '  <label>Type de connexion</label>';
    html += '  <select id="printerConnectionType" class="form-control" onchange="onPrinterTypeChange(); savePrinterSetting(\'connection_type\', this.value)" style="max-width:250px;">';
    html += '    <option value="network"' + (config.connection_type === 'network' ? ' selected' : '') + '>WiFi / Réseau</option>';
    html += '    <option value="usb"' + (config.connection_type === 'usb' ? ' selected' : '') + '>USB</option>';
    html += '    <option value="windows"' + (config.connection_type === 'windows' ? ' selected' : '') + '>Imprimante Windows</option>';
    html += '  </select>';
    html += '</div>';

    if (config.connection_type === 'network') {
        html += '<div class="form-row" style="display:flex;gap:12px;flex-wrap:wrap;">';
        html += '  <div class="form-group" style="flex:2;min-width:200px;">';
        html += '    <label>Adresse IP</label>';
        html += '    <input type="text" class="form-control" id="printerHost" value="' + escapeHtml(config.host || '') + '" placeholder="192.168.1.100" onchange="savePrinterSetting(\'host\', this.value)">';
        html += '  </div>';
        html += '  <div class="form-group" style="flex:1;min-width:100px;">';
        html += '    <label>Port</label>';
        html += '    <input type="number" class="form-control" id="printerPort" value="' + (config.port || 9100) + '" onchange="savePrinterSetting(\'port\', parseInt(this.value) || 9100)">';
        html += '  </div>';
        html += '</div>';
    } else if (config.connection_type === 'windows') {
        html += '<div class="form-group">';
        html += '  <label>Nom de l\'imprimante (Windows)</label>';
        html += '  <input type="text" class="form-control" id="printerHost" value="' + escapeHtml(config.host || '') + '" placeholder="XP-80" onchange="savePrinterSetting(\'host\', this.value)" style="max-width:350px;">';
        html += '  <p style="color:var(--text-light);font-size:12px;margin-top:4px;">Le nom apparaît dans Paramètres &gt; Périphériques &gt; Imprimantes</p>';
        html += '</div>';
        html += '<div class="form-group">';
        html += '  <label>Imprimantes USB détectées</label>';
        html += '  <div id="usbPrinterList" style="margin-bottom:8px;">';
        html += '    <p style="color:var(--text-light);font-size:13px;">Cliquez sur "Rechercher" pour lister les imprimantes USB branchées</p>';
        html += '  </div>';
        html += '  <button class="btn btn-sm btn-outline" onclick="scanUsbPrinters()"><i class="fas fa-search"></i> Rechercher les imprimantes</button>';
        html += '</div>';
    } else {
        html += '<div class="form-group">';
        html += '  <label>Imprimantes USB détectées</label>';
        html += '  <div id="usbPrinterList" style="margin-bottom:8px;">';
        html += '    <p style="color:var(--text-light);font-size:13px;">Cliquez sur "Rechercher" pour lister les imprimantes USB branchées</p>';
        html += '  </div>';
        html += '  <button class="btn btn-sm btn-outline" onclick="scanUsbPrinters()"><i class="fas fa-search"></i> Rechercher les imprimantes</button>';
        html += '</div>';
    }

    html += '<div class="form-group" style="margin-top:12px;">';
    html += '  <button class="btn btn-primary" onclick="testPrinterConnection()" id="printerTestBtn"><i class="fas fa-play"></i> Tester l\'impression</button>';
    html += '  <span id="printerTestResult" style="margin-left:12px;font-size:13px;"></span>';
    html += '</div>';

    document.getElementById('printerSettingsBody').innerHTML = html;
}

function savePrinterSetting(key, value) {
    var data = {};
    data[key] = value;
    fetch('/api/settings/printer', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    }).then(function(r) { return r.json(); }).then(function(res) {
        if (res.error) showError(res.error);
    }).catch(function(err) { showError('Erreur: ' + err); });
}

function scanUsbPrinters() {
    var container = document.getElementById('usbPrinterList');
    container.innerHTML = '<p style="color:var(--text-light);font-size:13px;"><i class="fas fa-spinner fa-spin"></i> Recherche en cours...</p>';
    fetch('/api/settings/printer/discover')
        .then(function(r) { return r.json(); })
        .then(function(printers) {
            if (!printers.length) {
                container.innerHTML = '<p style="color:var(--text-muted);font-size:13px;">Aucune imprimante USB détectée. Branchez-en une et réessayez.</p>';
                return;
            }
            var html = '<div style="max-height:200px;overflow-y:auto;border:1px solid var(--border);border-radius:6px;">';
            printers.forEach(function(p, i) {
                var connType = p.connection_type || 'usb';
                var instId = p.instance_id || '';
                html += '<div class="printer-item" onclick="selectUsbPrinter(\'' + p.vendor_id + '\', \'' + p.product_id + '\', \'' + connType + '\', \'' + escapeHtml(p.name) + '\', \'' + escapeHtml(instId) + '\', this)" ';
                html += 'style="padding:8px 12px;cursor:pointer;border-bottom:1px solid var(--border);display:flex;align-items:center;gap:10px;transition:background 0.15s;"';
                html += ' onmouseover="this.style.background=\'var(--bg-hover)\'" onmouseout="this.style.background=\'\'"';
                html += '>';
                html += '  <i class="fas fa-print" style="color:var(--primary);font-size:18px;"></i>';
                html += '  <div style="flex:1;">';
                html += '    <div style="font-weight:600;font-size:14px;">' + escapeHtml(p.name) + '</div>';
                html += '    <div style="font-size:12px;color:var(--text-light);">VID: ' + p.vendor_id + ' &nbsp; PID: ' + p.product_id + ' &nbsp; ' + escapeHtml(p.manufacturer || '') + '</div>';
                html += '  </div>';
                html += '  <i class="fas fa-check-circle" style="color:var(--success);display:none;"></i>';
                html += '</div>';
            });
            html += '</div>';
            container.innerHTML = html;
        })
        .catch(function(err) {
            container.innerHTML = '<p style="color:var(--danger);font-size:13px;">Erreur: ' + err + '</p>';
        });
}

function selectUsbPrinter(vendorId, productId, connType, printerName, instanceId, el) {
    var items = document.querySelectorAll('.printer-item');
    items.forEach(function(i) { i.style.background = ''; i.querySelector('.fa-check-circle').style.display = 'none'; });
    el.style.background = 'var(--bg-hover)';
    el.querySelector('.fa-check-circle').style.display = 'inline-block';
    if (connType === 'windows') {
        var hostInput = document.getElementById('printerHost');
        if (hostInput) hostInput.value = printerName;
        savePrinterSetting('connection_type', 'windows');
        savePrinterSetting('host', printerName);
    }
    if (vendorId) {
        savePrinterSetting('usb_vendor_id', vendorId);
    }
    if (productId) {
        savePrinterSetting('usb_product_id', productId);
    }
    if (instanceId) {
        savePrinterSetting('instance_id', instanceId);
    }
}

function onPrinterTypeChange() {
    var type = document.getElementById('printerConnectionType').value;
    var config = { connection_type: type, host: '', port: 9100, usb_vendor_id: '', usb_product_id: '', auto_print: true };
    renderPrinterSettings(config, {});
    if (type === 'windows') {
        savePrinterSetting('connection_type', 'windows');
    }
}

function testPrinterConnection() {
    var btn = document.getElementById('printerTestBtn');
    var result = document.getElementById('printerTestResult');
    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Impression...';
    result.textContent = '';
    result.style.color = '';

    fetch('/api/settings/printer/test', { method: 'POST' })
    .then(function(r) { return r.json(); })
    .then(function(data) {
        if (data.error) {
            result.textContent = 'Échec: ' + data.error;
            result.style.color = 'var(--danger)';
        } else {
            result.textContent = 'Test réussi!';
            result.style.color = 'var(--success)';
        }
    })
    .catch(function(err) {
        result.textContent = 'Erreur: ' + err;
        result.style.color = 'var(--danger)';
    })
    .finally(function() {
        btn.disabled = false;
        btn.innerHTML = '<i class="fas fa-play"></i> Tester l\'impression';
    });
}

function loadSecuritySettings() {
    fetch('/api/settings/reset-password')
        .then(function(r) { return r.json(); })
        .then(function(data) {
            var html = '';
            if (!data.has_password) {
                html += '<p style="color:var(--text-light);margin-bottom:12px;">Définissez un mot de passe pour protéger la réinitialisation des données.</p>';
                html += '<div class="form-group">';
                html += '  <label>Nouveau mot de passe</label>';
                html += '  <input type="password" id="newResetPassword" class="form-input" placeholder="Minimum 4 caractères" style="max-width:250px;">';
                html += '</div>';
                html += '<button class="btn btn-primary" onclick="saveResetPassword()"><i class="fas fa-save"></i> Enregistrer</button>';
            } else {
                if (data.is_default) {
                    html += '<div class="alert alert-warning" style="margin-bottom:12px;"><i class="fas fa-exclamation-triangle"></i> <strong>Mot de passe par défaut</strong> — Veuillez le changer pour sécuriser l\'application.</div>';
                }
                html += '<div class="form-group">';
                html += '  <label>Ancien mot de passe</label>';
                html += '  <input type="password" id="currentResetPassword" class="form-input" placeholder="Ancien mot de passe" style="max-width:250px;">';
                html += '</div>';
                html += '<div class="form-group">';
                html += '  <label>Nouveau mot de passe</label>';
                html += '  <input type="password" id="newResetPassword" class="form-input" placeholder="Minimum 4 caractères" style="max-width:250px;">';
                html += '</div>';
                html += '<button class="btn btn-primary" onclick="saveResetPassword()"><i class="fas fa-save"></i> Changer le mot de passe</button>';
            }
            html += '<div id="securitySettingsMsg" style="margin-top:8px;"></div>';
            document.getElementById('securitySettingsBody').innerHTML = html;
        })
        .catch(function(err) {
            document.getElementById('securitySettingsBody').innerHTML = '<div class="empty"><i class="fas fa-exclamation-triangle"></i><p>Erreur: ' + err + '</p></div>';
        });
}

function saveResetPassword() {
    var newPw = document.getElementById('newResetPassword').value;
    if (!newPw || newPw.length < 4) {
        showError('Le mot de passe doit contenir au moins 4 caractères');
        return;
    }

    var body = { new_password: newPw };
    var currentPw = document.getElementById('currentResetPassword');
    if (currentPw && currentPw.value) {
        body.current_password = currentPw.value;
    }

    fetch('/api/settings/reset-password', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    })
    .then(function(r) { return r.json(); })
    .then(function(data) {
        if (data.error) {
            showError(data.error);
        } else {
            showSuccess('Mot de passe mis à jour');
            loadSecuritySettings();
        }
    })
    .catch(function(err) {
        showError('Erreur: ' + err);
    });
}

document.addEventListener('DOMContentLoaded', function() {
    fetch('/api/stores').then(function(r) { return r.json(); }).then(function(stores) {
        fetch('/api/stores/current').then(function(r) { return r.json(); }).then(function(current) {
            var label = document.getElementById('storeSwitcherLabel');
            if (label) label.textContent = current.name;
            updateStoreSwitcher(stores, current.id);
        }).catch(function() {});
    }).catch(function() {});
});
