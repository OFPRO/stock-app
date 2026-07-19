function escapeHtml(str) {
    if (str == null) return '';
    const div = document.createElement('div');
    div.textContent = String(str);
    return div.innerHTML;
}

async function loadCustomers() {
    try {
        const res = await fetch('/api/customers');
        customers = await res.json();
        renderCustomers();
    } catch(e) {
        showError('Erreur lors du chargement des clients');
    }
}

function renderCustomers() {
    const tbody = document.getElementById('customersTable');
    if (!tbody) return;
    if (customers.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5">Aucun client</td></tr>';
        return;
    }
    let html = '';
    for (let i = 0; i < customers.length; i++) {
        const c = customers[i];
        html += '<tr><td>' + escapeHtml(c.client_code || '-') + '</td><td>' + escapeHtml(c.name) + '</td><td>' + escapeHtml(c.email || '-') + '</td><td>' + escapeHtml(c.phone || '-') + '</td><td>' + (c.is_loyal ? 'Oui' : 'Non') + '</td><td><button class="btn btn-sm" onclick="editCustomer(' + c.id + ')">✎</button></td></tr>';
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
    const c = customers.find(c => c.id === id);
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
    const id = document.getElementById('customerId').value;
    const data = {
        name: document.getElementById('customerName').value,
        type: document.getElementById('customerType').value,
        email: document.getElementById('customerEmail').value,
        phone: document.getElementById('customerPhone').value,
        address: document.getElementById('customerAddress').value
    };
    try {
        const method = id ? 'PUT' : 'POST';
        const url = id ? '/api/customers/' + id : '/api/customers';
        const res = await fetch(url, {
            method: method,
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(data)
        });
        const result = await res.json();
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
    try {
        const res = await fetch('/api/delivery-notes');
        deliveryNotes = await res.json();
        renderDeliveryNotes();
    } catch(e) {
        showError('Erreur lors du chargement des bons de livraison');
    }
}

function renderDeliveryNotes() {
    const tbody = document.getElementById('deliveryNotesTable');
    if (!tbody) return;
    if (deliveryNotes.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6">Aucun bon</td></tr>';
        return;
    }
    let html = '';
    for (let i = 0; i < deliveryNotes.length; i++) {
        const d = deliveryNotes[i];
        html += '<tr><td>' + escapeHtml(d.note_number || '-') + '</td><td>' + escapeHtml(d.product_name || '-') + '</td><td>' + d.quantity + '</td><td>' + d.type + '</td><td>' + (d.created_at ? d.created_at.substring(0, 10) : '-') + '</td><td>' + (d.status === 'draft' ? '<button class="btn btn-sm">→ Facture</button>' : '-') + '</td></tr>';
    }
    tbody.innerHTML = html;
}

async function convertDeliveryNote(noteId) {
    try {
        await fetch('/api/delivery-notes/' + noteId + '/convert', { method: 'POST' });
        loadDeliveryNotes();
    } catch(e) {
        showError('Erreur lors de la conversion du bon');
    }
}


