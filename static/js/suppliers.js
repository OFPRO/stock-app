async function loadSuppliers() {
    var res = await fetch('/api/suppliers');
    suppliers = await res.json();
    renderSuppliers();
    
    var orderSupplier = document.getElementById('orderSupplier');
    if (orderSupplier) {
        orderSupplier.innerHTML = '<option value="">Selectionnez un fournisseur</option>' +
            suppliers.map(function(s) { return '<option value="' + s.id + '">' + s.name + '</option>'; }).join('');
    }
}

function renderSuppliers() {
    var tbody = document.getElementById('suppliersTable');
    if (!tbody) return;
    if (suppliers.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4">Aucun fournisseur</td></tr>';
        return;
    }
    var html = '';
    for (var i = 0; i < suppliers.length; i++) {
        var s = suppliers[i];
        html += '<tr><td>' + s.name + '</td><td>' + (s.email || '-') + '</td><td>' + (s.phone || '-') + '</td><td><button class="btn btn-sm" onclick="editSupplier(' + s.id + ')">✎</button></td></tr>';
    }
    tbody.innerHTML = html;
}

function openSupplierModal() {
    document.getElementById('supplierId').value = '';
    document.getElementById('supplierName').value = '';
    document.getElementById('supplierEmail').value = '';
    document.getElementById('supplierPhone').value = '';
    document.getElementById('supplierAddress').value = '';
    document.getElementById('supplierModalTitle').textContent = 'Nouveau Fournisseur';
    document.getElementById('supplierModal').style.display = 'flex';
}

function editSupplier(id) {
    var s = suppliers.find(function(s) { return s.id === id; });
    if (!s) return;
    document.getElementById('supplierId').value = s.id;
    document.getElementById('supplierName').value = s.name;
    document.getElementById('supplierEmail').value = s.email || '';
    document.getElementById('supplierPhone').value = s.phone || '';
    document.getElementById('supplierAddress').value = s.address || '';
    document.getElementById('supplierModalTitle').textContent = 'Modifier Fournisseur';
    document.getElementById('supplierModal').style.display = 'flex';
}

async function saveSupplier(e) {
    e.preventDefault();
    var id = document.getElementById('supplierId').value;
    var data = {
        name: document.getElementById('supplierName').value,
        email: document.getElementById('supplierEmail').value,
        phone: document.getElementById('supplierPhone').value,
        address: document.getElementById('supplierAddress').value
    };
    try {
        var method = id ? 'PUT' : 'POST';
        var url = id ? '/api/suppliers/' + id : '/api/suppliers';
        var res = await fetch(url, {
            method: method,
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(data)
        });
        var result = await res.json();
        if (result.success) {
            closeModal('supplierModal');
            loadSuppliers();
        } else {
            showError(result.error || 'Erreur');
        }
    } catch(e) {
        showError('Erreur lors de l\'enregistrement');
    }
}

