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

document.addEventListener('DOMContentLoaded', function() {
    fetch('/api/stores').then(function(r) { return r.json(); }).then(function(stores) {
        fetch('/api/stores/current').then(function(r) { return r.json(); }).then(function(current) {
            var label = document.getElementById('storeSwitcherLabel');
            if (label) label.textContent = current.name;
            updateStoreSwitcher(stores, current.id);
        }).catch(function() {});
    }).catch(function() {});
});
