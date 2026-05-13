async function loadCustomers() {
    var res = await fetch('/api/customers');
    customers = await res.json();
    renderCustomers();
}

function renderCustomers() {
    var tbody = document.getElementById('customersTable');
    if (!tbody) return;
    if (customers.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5">Aucun client</td></tr>';
        return;
    }
    var html = '';
    for (var i = 0; i < customers.length; i++) {
        var c = customers[i];
        html += '<tr><td>' + (c.client_code || '-') + '</td><td>' + c.name + '</td><td>' + (c.email || '-') + '</td><td>' + (c.phone || '-') + '</td><td>' + (c.is_loyal ? 'Oui' : 'Non') + '</td><td><button class="btn btn-sm" onclick="editCustomer(' + c.id + ')">✎</button></td></tr>';
    }
    tbody.innerHTML = html;
}

function openCustomerModal() {
    document.getElementById('customerId').value = '';
    document.getElementById('customerName').value = '';
    document.getElementById('customerType').value = 'particulier';
    document.getElementById('customerEmail').value = '';
    document.getElementById('customerPhone').value = '';
    document.getElementById('customerAddress').value = '';
    document.getElementById('customerModalTitle').textContent = 'Nouveau Client';
    document.getElementById('customerModal').style.display = 'flex';
}

function editCustomer(id) {
    var c = customers.find(function(c) { return c.id === id; });
    if (!c) return;
    document.getElementById('customerId').value = c.id;
    document.getElementById('customerName').value = c.name;
    document.getElementById('customerType').value = c.type || 'particulier';
    document.getElementById('customerEmail').value = c.email || '';
    document.getElementById('customerPhone').value = c.phone || '';
    document.getElementById('customerAddress').value = c.address || '';
    document.getElementById('customerModalTitle').textContent = 'Modifier Client';
    document.getElementById('customerModal').style.display = 'flex';
}

async function saveCustomer(e) {
    e.preventDefault();
    var id = document.getElementById('customerId').value;
    var data = {
        name: document.getElementById('customerName').value,
        type: document.getElementById('customerType').value,
        email: document.getElementById('customerEmail').value,
        phone: document.getElementById('customerPhone').value,
        address: document.getElementById('customerAddress').value
    };
    try {
        var method = id ? 'PUT' : 'POST';
        var url = id ? '/api/customers/' + id : '/api/customers';
        var res = await fetch(url, {
            method: method,
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(data)
        });
        var result = await res.json();
        if (result.success) {
            closeModal('customerModal');
            loadCustomers();
        } else {
            showError(result.error || 'Erreur');
        }
    } catch(e) {
        showError('Erreur lors de l\'enregistrement');
    }
}

async function loadDeliveryNotes() {
    var res = await fetch('/api/delivery-notes');
    deliveryNotes = await res.json();
    renderDeliveryNotes();
}

function renderDeliveryNotes() {
    var tbody = document.getElementById('deliveryNotesTable');
    if (!tbody) return;
    if (deliveryNotes.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6">Aucun bon</td></tr>';
        return;
    }
    var html = '';
    for (var i = 0; i < deliveryNotes.length; i++) {
        var d = deliveryNotes[i];
        html += '<tr><td>' + (d.note_number || '-') + '</td><td>' + (d.product_name || '-') + '</td><td>' + d.quantity + '</td><td>' + d.type + '</td><td>' + (d.created_at ? d.created_at.substring(0, 10) : '-') + '</td><td>' + (d.status === 'draft' ? '<button class="btn btn-sm">→ Facture</button>' : '-') + '</td></tr>';
    }
    tbody.innerHTML = html;
}

async function convertDeliveryNote(noteId) {
    await fetch('/api/delivery-notes/' + noteId + '/convert', { method: 'POST' });
    loadDeliveryNotes();
}

// ============= REPORTS FUNCTIONS =============
var currentReport = 'overview';
var reportData = {};
