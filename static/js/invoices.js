async function loadInvoices() {
    try {
        const dateStart = document.getElementById('invoiceDateStart')?.value || '';
        const dateEnd = document.getElementById('invoiceDateEnd')?.value || '';
        const status = document.getElementById('invoiceStatusFilter')?.value || 'all';

        const params = new URLSearchParams();
        if (dateStart) params.set('date_start', dateStart);
        if (dateEnd) params.set('date_end', dateEnd);
        if (status !== 'all') params.set('status', status);

        const url = '/api/invoices' + (params.toString() ? '?' + params.toString() : '');
        const res = await fetch(url);
        invoices = await res.json();
        renderInvoices();
    } catch(e) {
        showError('Erreur lors du chargement des factures');
    }
}

function renderInvoices() {
    const tbody = document.getElementById('invoicesTable');
    if (!tbody) return;
    if (invoices.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;padding:2rem;color:var(--text-light);">Aucune facture pour cette période</td></tr>';
        return;
    }
    let html = '';
    for (let i = 0; i < invoices.length; i++) {
        const inv = invoices[i];
        const isTicket = inv.invoice_number && inv.invoice_number.startsWith('Ticket-');
        const isBL = inv.type === 'bon_de_livraison';
        const isFournisseur = inv.type === 'fournisseur';
        const badgeClass = isTicket ? 'badge badge-primary' : (isBL ? 'badge badge-purple' : (isFournisseur ? 'badge badge-warning' : 'badge badge-success'));
        const label = isTicket ? 'Ticket' : (isBL ? 'BL' : (isFournisseur ? 'Fact-Fourn' : 'Facture'));
        const btnLabel = isTicket ? 'Ticket' : 'Voir';
        const partyName = isFournisseur ? (inv.supplier_name || 'Fournisseur') : (inv.customer_name || 'Client Comptoir');
        const createdDate = inv.created_at ? inv.created_at.substring(0, 10) : '-';
        const paidDate = inv.paid_at ? inv.paid_at.substring(0, 10) : '-';

        const statusColors = {
            'ticket': 'primary', 'payee': 'success', 'partiellement_payee': 'info',
            'envoyee': 'warning', 'annulee': 'danger', 'brouillon': 'secondary'
        };
        const statusLabels = {
            'ticket': 'Ticket', 'payee': 'Payée', 'partiellement_payee': 'Partielle',
            'envoyee': 'Envoyée', 'annulee': 'Annulée', 'brouillon': 'Brouillon'
        };
        const statColor = statusColors[inv.status] || 'secondary';
        const statLabel = statusLabels[inv.status] || 'Brouillon';

        const amountPaid = inv.amount_paid || 0;
        const total = inv.total || 0;
        const paidDisplay = (inv.status === 'partiellement_payee')
            ? amountPaid.toFixed(2) + ' / ' + total.toFixed(2) + ' DH'
            : ((isTicket || inv.status === 'payee') ? total.toFixed(2) + ' DH' : '-');

        let actionBtn = '<button class="btn btn-sm" onclick="viewInvoice(' + inv.id + ', \'' + inv.invoice_number + '\')">' + btnLabel + '</button>';
        if (isBL && inv.status === 'envoyee') {
            actionBtn += ' <button class="btn btn-sm btn-primary" onclick="convertBLToInvoice(' + inv.id + ')"><i class="fas fa-exchange-alt"></i> Convertir</button>';
        } else if (!isBL && (inv.status === 'partiellement_payee' || inv.status === 'envoyee')) {
            actionBtn += ' <button class="btn btn-sm btn-primary" onclick="openPayCreditModal(' + inv.id + ')"><i class="fas fa-credit-card"></i> Payer</button>';
        }

        html += '<tr>' +
            '<td><span class="' + badgeClass + '">' + label + '</span> ' + inv.invoice_number + '</td>' +
            '<td>' + partyName + '</td>' +
            '<td>' + createdDate + '</td>' +
            '<td>' + paidDate + '</td>' +
            '<td>' + paidDisplay + '</td>' +
            '<td><span class="badge badge-' + statColor + '">' + statLabel + '</span></td>' +
            '<td>' + actionBtn + '</td>' +
            '</tr>';
    }
    tbody.innerHTML = html;
}

function viewInvoice(id, invoiceNumber) {
    if (invoiceNumber && invoiceNumber.startsWith('Ticket-')) {
        window.open('/api/pos/tickets/' + invoiceNumber, '_blank');
    } else if (invoiceNumber && invoiceNumber.startsWith('FAC-')) {
        window.open('/api/invoices/' + id + '/pdf', '_blank');
    } else {
        window.open('/api/invoices/' + id + '/pdf', '_blank');
    }
}

let payCreditInvoiceId = null;

async function openPayCreditModal(invoiceId) {
    try {
        const res = await fetch('/api/invoices/' + invoiceId);
        const inv = await res.json();
        if (!inv || inv.error) {
            showError('Impossible de charger la facture');
            return;
        }

        payCreditInvoiceId = invoiceId;
        const total = inv.total || 0;
        const paid = inv.amount_paid || 0;
        const remaining = total - paid;

        document.getElementById('payCreditInvoiceNumber').textContent = inv.invoice_number || '-';
        document.getElementById('payCreditTotal').textContent = total.toFixed(2) + ' DH';
        document.getElementById('payCreditAlreadyPaid').textContent = paid.toFixed(2) + ' DH';
        document.getElementById('payCreditRemaining').textContent = remaining.toFixed(2) + ' DH';
        document.getElementById('payCreditAmount').value = remaining.toFixed(2);
        document.getElementById('payCreditAmount').max = remaining;
        document.getElementById('payCreditMethod').value = 'cash';

        openModal('modalPayCredit');
    } catch(e) {
        showError('Erreur lors du chargement de la facture');
    }
}

function closePayCreditModal() {
    closeModal('modalPayCredit');
    payCreditInvoiceId = null;
}

function setPayCreditMethod(method) {
    document.getElementById('payCreditMethod').value = method;
    document.querySelectorAll('.pay-credit-method-btn').forEach(function(btn) {
        btn.classList.toggle('active', btn.dataset.method === method);
    });
}

async function submitPayCredit() {
    if (!payCreditInvoiceId) return;

    const amount = parseFloat(document.getElementById('payCreditAmount').value) || 0;
    const method = document.getElementById('payCreditMethod').value;

    if (amount <= 0) {
        showError('Montant invalide');
        return;
    }

    try {
        const res = await fetch('/api/invoices/' + payCreditInvoiceId + '/pay-credit', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ amount: amount, payment_method: method })
        });
        const data = await res.json();

        if (data.success) {
            showError(data.status === 'payee'
                ? 'Facture entierement payee!'
                : 'Paiement partiel enregistre (' + amount.toFixed(2) + ' DH)');
            closePayCreditModal();
            loadInvoices();
        } else {
            showError(data.error || 'Erreur paiement');
        }
    } catch(e) {
        showError('Erreur lors du paiement');
    }
}

async function convertBLToInvoice(id) {
    if (!confirm('Convertir ce bon de livraison en facture ? Le statut passera à "Payée".')) return;
    try {
        var res = await fetch('/api/invoices/' + id + '/convert-to-invoice', { method: 'POST' });
        var data = await res.json();
        if (data.success) {
            showError('Bon de livraison converti avec succès');
            loadInvoices();
        } else {
            showError(data.error || 'Erreur de conversion');
        }
    } catch(e) {
        showError('Erreur de conversion');
    }
}
