async function loadMovements() {
    try {
        const res = await fetch('/api/movements');
        const movements = await res.json();
        const tbody = document.getElementById('movementsTable');
        if (!tbody) return;
        if (movements.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5">Aucun mouvement</td></tr>';
            return;
        }
        let html = '';
        for (let i = 0; i < movements.length; i++) {
            const m = movements[i];
            const typeLabel = m.type === 'in' ? 'Entrée' : m.type === 'out' ? 'Sortie' : m.type;
            const typeClass = m.type === 'in' ? 'success' : m.type === 'out' ? 'warning' : 'primary';
            html += '<tr><td>' + (m.created_at ? m.created_at.substring(0, 16) : '-') + '</td><td>' + (m.product_name || '-') + '</td><td><span class="badge badge-' + typeClass + '">' + typeLabel + '</span></td><td>' + m.quantity + '</td><td>' + (m.note || '-') + '</td></tr>';
        }
        tbody.innerHTML = html;
    } catch(e) {
        showError('Erreur lors du chargement des mouvements');
    }
}
