async function loadInvoices() {
    var res = await fetch('/api/invoices');
    invoices = await res.json();
    renderInvoices();
}

function renderInvoices() {
    var tbody = document.getElementById('invoicesTable');
    if (!tbody) return;
    if (invoices.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5">Aucune facture</td></tr>';
        return;
    }
    var html = '';
    for (var i = 0; i < invoices.length; i++) {
        var inv = invoices[i];
        var isTicket = inv.invoice_number && inv.invoice_number.startsWith('Ticket-');
        var isFacture = inv.invoice_number && inv.invoice_number.startsWith('FAC-') && inv.type !== 'fournisseur';
        var isFournisseur = inv.type === 'fournisseur';
        var badgeClass = isTicket ? 'badge badge-primary' : (isFournisseur ? 'badge badge-warning' : 'badge badge-success');
        var label = isTicket ? 'Ticket' : (isFournisseur ? 'Fact-Fourn' : 'Facture');
        var btnLabel = isTicket ? 'Ticket' : 'Voir';
        var partyName = isFournisseur ? (inv.supplier_name || 'Fournisseur') : (inv.customer_name || 'Client Comptoir');
        html += '<tr><td><span class="' + badgeClass + '">' + label + '</span> ' + inv.invoice_number + '</td><td>' + partyName + '</td><td>' + (inv.created_at ? inv.created_at.substring(0, 10) : '-') + '</td><td>' + (inv.total || 0).toFixed(2) + ' DH</td><td><span class="badge badge-' + (inv.status === 'payee' ? 'success' : 'warning') + '">' + (inv.status || 'brouillon') + '</span></td><td><button class="btn btn-sm" onclick="viewInvoice(' + inv.id + ', \'' + inv.invoice_number + '\')">' + btnLabel + '</button></td></tr>';
    }
    tbody.innerHTML = html;
}

function viewInvoice(id, invoiceNumber) {
    // Check if it's a ticket or facture
    if (invoiceNumber && invoiceNumber.startsWith('Ticket-')) {
        window.open('/api/pos/tickets/' + invoiceNumber, '_blank');
    } else if (invoiceNumber && invoiceNumber.startsWith('FAC-')) {
        window.open('/api/invoices/' + id + '/pdf', '_blank');
    } else {
        window.open('/api/invoices/' + id + '/pdf', '_blank');
    }
}
