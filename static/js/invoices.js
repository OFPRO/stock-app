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
        tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:2rem;color:var(--text-light);">Aucune facture pour cette période</td></tr>';
        return;
    }
    let html = '';
    for (let i = 0; i < invoices.length; i++) {
        const inv = invoices[i];
        const isTicket = inv.invoice_number && inv.invoice_number.startsWith('Ticket-');
        const isFacture = inv.invoice_number && inv.invoice_number.startsWith('FAC-') && inv.type !== 'fournisseur';
        const isFournisseur = inv.type === 'fournisseur';
        const badgeClass = isTicket ? 'badge badge-primary' : (isFournisseur ? 'badge badge-warning' : 'badge badge-success');
        const label = isTicket ? 'Ticket' : (isFournisseur ? 'Fact-Fourn' : 'Facture');
        const btnLabel = isTicket ? 'Ticket' : 'Voir';
        const partyName = isFournisseur ? (inv.supplier_name || 'Fournisseur') : (inv.customer_name || 'Client Comptoir');
        const createdDate = inv.created_at ? inv.created_at.substring(0, 10) : '-';
        const paidDate = inv.paid_at ? inv.paid_at.substring(0, 10) : '-';
        const statusBadge = 'badge badge-' + (inv.status === 'ticket' ? 'primary' : inv.status === 'payee' ? 'success' : inv.status === 'envoyee' ? 'warning' : inv.status === 'annulee' ? 'danger' : 'secondary');
        const statusLabel = inv.status === 'ticket' ? 'Ticket' : inv.status === 'payee' ? 'Payée' : inv.status === 'envoyee' ? 'Envoyée' : inv.status === 'annulee' ? 'Annulée' : inv.status === 'brouillon' ? 'Brouillon' : (inv.status || 'Brouillon');
        html += '<tr>' +
            '<td><span class="' + badgeClass + '">' + label + '</span> ' + inv.invoice_number + '</td>' +
            '<td>' + partyName + '</td>' +
            '<td>' + createdDate + '</td>' +
            '<td>' + paidDate + '</td>' +
            '<td>' + (inv.total || 0).toFixed(2) + ' DH</td>' +
            '<td><span class="' + statusBadge + '">' + statusLabel + '</span></td>' +
            '<td><button class="btn btn-sm" onclick="viewInvoice(' + inv.id + ', \'' + inv.invoice_number + '\')">' + btnLabel + '</button></td>' +
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
