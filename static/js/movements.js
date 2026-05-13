async function loadMovements() {
    var res = await fetch('/api/movements');
    var movements = await res.json();
    var tbody = document.getElementById('movementsTable');
    if (!tbody) return;
    if (movements.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5">Aucun mouvement</td></tr>';
        return;
    }
    var html = '';
    for (var i = 0; i < movements.length; i++) {
        var m = movements[i];
        var typeLabel = m.type === 'in' ? 'Entrée' : m.type === 'out' ? 'Sortie' : m.type;
        var typeClass = m.type === 'in' ? 'success' : m.type === 'out' ? 'warning' : 'primary';
        html += '<tr><td>' + (m.created_at ? m.created_at.substring(0, 16) : '-') + '</td><td>' + (m.product_name || '-') + '</td><td><span class="badge badge-' + typeClass + '">' + typeLabel + '</span></td><td>' + m.quantity + '</td><td>' + (m.note || '-') + '</td></tr>';
    }
    tbody.innerHTML = html;
}

