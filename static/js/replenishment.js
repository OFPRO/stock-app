async function loadReplenishment() {
    try {
        const res = await fetch('/api/replenishment');
        const prods = await res.json();
        const tbody = document.getElementById('replenishmentTable');
        if (!tbody) return;
        if (prods.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4">Aucun produit à commander</td></tr>';
            return;
        }
        let html = '';
        for (let i = 0; i < prods.length; i++) {
            const p = prods[i];
            html += '<tr><td>' + p.name + '</td><td><span class="badge badge-' + (p.current_qty <= p.min_quantity ? 'danger' : 'warning') + '">' + p.current_qty + '</span></td><td>' + p.min_quantity + '</td><td>' + (p.supplier_name || '-') + '</td></tr>';
        }
        tbody.innerHTML = html;
    } catch(e) {
        showError('Erreur lors du chargement du réapprovisionnement');
    }
}
