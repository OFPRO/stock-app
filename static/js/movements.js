function escapeHtml(str) {
    if (str == null) return '';
    const div = document.createElement('div');
    div.textContent = String(str);
    return div.innerHTML;
}

async function loadMovements() {
    try {
        const typeFilter = document.getElementById('movementsTypeFilter')?.value || '';
        let url = '/api/movements';
        if (typeFilter) url += '?type=' + encodeURIComponent(typeFilter);
        const res = await fetch(url);
        const movements = await res.json();
        const tbody = document.getElementById('movementsTable');
        if (!tbody) return;
        if (movements.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5">Aucun mouvement</td></tr>';
            return;
        }
        let html = '';
        const typeLabels = {
            'in': 'Entrée', 'out': 'Sortie', 'transfer': 'Transfert',
            'inter_warehouse': 'Tr. Entrepôt', 'sale': 'Vente',
            'retour': 'Retour', 'destruction': 'Destruction'
        };
        for (let i = 0; i < movements.length; i++) {
            const m = movements[i];
            const typeLabel = typeLabels[m.type] || m.type;
            const typeClass = m.type === 'in' || m.type === 'retour' ? 'success' : m.type === 'out' || m.type === 'sale' || m.type === 'destruction' ? 'warning' : 'primary';
            html += '<tr><td>' + (m.created_at ? m.created_at.substring(0, 16) : '-') + '</td><td>' + escapeHtml(m.product_name || '-') + '</td><td><span class="badge badge-' + typeClass + '">' + typeLabel + '</span></td><td>' + m.quantity + '</td><td>' + escapeHtml(m.note || '-') + '</td></tr>';
        }
        tbody.innerHTML = html;
    } catch(e) {
        showError('Erreur lors du chargement des mouvements');
    }
}

function exportMovementsPdf() {
    const params = new URLSearchParams();
    const typeFilter = document.getElementById('movementsTypeFilter')?.value;
    if (typeFilter) params.set('type', typeFilter);
    const a = document.createElement('a');
    a.href = '/api/movements/export/pdf?' + params.toString();
    a.download = 'mouvements.pdf';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
}
