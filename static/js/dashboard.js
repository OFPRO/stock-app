async function loadDashboard() {
    try {
        document.getElementById('dashboardLoader').classList.add('active');
        
        var period = document.getElementById('kpiPeriod')?.value || 30;
        var warehouse = document.getElementById('warehouseFilter')?.value || '';
        
        // Load all data in parallel
        var [salesRes, marginsRes, receivablesRes, invoicesRes, dashboardRes, 
             dailySalesRes, categoriesRes, topProductsRes, trendsRes, alertsRes, warehousesRes] = await Promise.all([
            fetch('/api/kpis/sales'),
            fetch('/api/kpis/margins'),
            fetch('/api/kpis/receivables'),
            fetch('/api/kpis/invoices-status'),
            fetch('/api/kpis/dashboard?period=' + period + (warehouse ? '&warehouse_id=' + warehouse : '')),
            fetch('/api/kpis/sales-daily?period=' + period),
            fetch('/api/kpis/categories-distribution'),
            fetch('/api/kpis/top-selling-products?limit=10'),
            fetch('/api/kpis/trends?period=' + period),
            fetch('/api/kpis/alertes' + (warehouse ? '?warehouse_id=' + warehouse : '')),
            fetch('/api/warehouses')
        ]);
        
        var sales = await salesRes.json();
        var margins = await marginsRes.json();
        var receivables = await receivablesRes.json();
        var invoicesStatus = await invoicesRes.json();
        var dashboard = await dashboardRes.json();
        var dailySales = await dailySalesRes.json();
        var categories = await categoriesRes.json();
        var topProducts = await topProductsRes.json();
        var trends = await trendsRes.json();
        var alerts = await alertsRes.json();
        var warehouses = await warehousesRes.json();
        
        // Populate warehouse filter
        var whSelect = document.getElementById('warehouseFilter');
        if (whSelect && warehouses.length > 0) {
            whSelect.innerHTML = '<option value="">Tous les entrepots</option>' + 
                warehouses.map(w => '<option value="' + w.id + '">' + w.name + '</option>').join('');
            whSelect.value = warehouse;
        }
        
        // === UPDATE KPI CARDS ===
        document.getElementById('caJour').textContent = (sales.ca_jour || 0).toLocaleString();
        document.getElementById('nbVentesJour').textContent = sales.nb_ventes_jour || 0;
        document.getElementById('ticketMoyen').textContent = (sales.ticket_moyen || 0).toLocaleString();
        document.getElementById('margeBrute').textContent = margins.marge_globale || 0;
        document.getElementById('caTrend').innerHTML = sales.ca_trend >= 0 ? 
            '<i class="fas fa-arrow-up"></i> ' + Math.abs(sales.ca_trend) + '%' : 
            '<i class="fas fa-arrow-down"></i> ' + Math.abs(sales.ca_trend) + '%';
        document.getElementById('caTrend').className = 'kpi-card-trend ' + (sales.ca_trend >= 0 ? 'up' : 'down');
        
        document.getElementById('totalCreances').textContent = (receivables.total_creances || 0).toLocaleString();
        document.getElementById('tauxEncaissement').textContent = receivables.taux_encaissement || 0;
        document.getElementById('valeurStock').textContent = (dashboard.total_value || 0).toLocaleString();
        document.getElementById('rupturesStock').textContent = dashboard.out_of_stock || 0;
        
        // Load main account KPI
        try {
            var accRes = await fetch('/api/main-account');
            var accData = await accRes.json();
            if (accData.account) {
                document.getElementById('mainAccountKpi').textContent = (accData.account.current_balance || 0).toFixed(2);
            }
        } catch(e) { console.error('Main account KPI error:', e); }
        
        // === UPDATE TABLES ===
        updateTableToOrder(dashboard.products_to_order || []);
        updateTableCreances(receivables.clients || []);
        updateTableRuptures(alerts.out_of_stock || []);
        updateTableExpiry(alerts.expiring || []);
        
        // === CHARTS ===
        renderSalesDailyChart(dailySales);
        renderCategoriesChart(categories);
        renderTopProductsChart(topProducts);
        renderInvoicesStatusChart(invoicesStatus);
        renderMovementsChart(trends);
        renderMarginsChart(margins.categories || []);
        
        document.getElementById('dashboardLoader').classList.remove('active');
        
    } catch(e) {
        console.error('Dashboard error:', e);
        document.getElementById('dashboardLoader').classList.remove('active');
        showError('Erreur de chargement du dashboard');
    }
}

function updateTableToOrder(products) {
    var html = '<table class="mini-table"><thead><tr><th>Produit</th><th>Stock</th><th>Min</th><th>A cmd</th></tr></thead><tbody>';
    if (products.length === 0) {
        html += '<tr><td colspan="4" style="text-align:center;color:var(--text-light)">Aucun produit</td></tr>';
    } else {
        for (var p of products.slice(0, 8)) {
            var qtyClass = p.quantity <= 0 ? 'danger' : (p.quantity <= p.min_quantity ? 'warning' : 'success');
            html += '<tr><td class="product-name">' + p.name + '</td>' +
                '<td><span class="qty ' + qtyClass + '">' + p.quantity + '</span></td>' +
                '<td>' + p.min_quantity + '</td>' +
                '<td><strong>+' + p.needed + '</strong></td></tr>';
        }
    }
    html += '</tbody></table>';
    document.getElementById('tableToOrder').innerHTML = html;
    document.getElementById('toOrderCount').textContent = products.length;
}

function updateTableCreances(clients) {
    var html = '<table class="mini-table"><thead><tr><th>Client</th><th>Montant</th><th>Echeance</th></tr></thead><tbody>';
    if (clients.length === 0) {
        html += '<tr><td colspan="3" style="text-align:center;color:var(--text-light)">Aucune creance</td></tr>';
    } else {
        for (var c of clients.slice(0, 8)) {
            var isOverdue = c.premiere_echeance && new Date(c.premiere_echeance) < new Date();
            html += '<tr><td>' + c.name + '</td>' +
                '<td class="amount negative">' + c.montant.toLocaleString() + ' DH</td>' +
                '<td class="due-date ' + (isOverdue ? 'overdue' : '') + '">' + (c.premiere_echeance || '-') + '</td></tr>';
        }
    }
    html += '</tbody></table>';
    document.getElementById('tableCreances').innerHTML = html;
    document.getElementById('creancesCount').textContent = clients.length;
}

function updateTableRuptures(products) {
    var html = '<table class="mini-table"><thead><tr><th>Produit</th><th>Stock</th><th>Min</th></tr></thead><tbody>';
    if (products.length === 0) {
        html += '<tr><td colspan="3" style="text-align:center;color:var(--text-light)">Aucune rupture</td></tr>';
    } else {
        for (var p of products.slice(0, 8)) {
            html += '<tr><td class="product-name">' + p.name + '</td>' +
                '<td><span class="qty danger">0</span></td>' +
                '<td>' + p.min_quantity + '</td></tr>';
        }
    }
    html += '</tbody></table>';
    document.getElementById('tableRuptures').innerHTML = html;
    document.getElementById('rupturesCount').textContent = products.length;
}

function updateTableExpiry(products) {
    var html = '<table class="mini-table"><thead><tr><th>Produit</th><th>DLC</th><th>Jours</th></tr></thead><tbody>';
    if (products.length === 0) {
        html += '<tr><td colspan="3" style="text-align:center;color:var(--text-light)">Aucun perime</td></tr>';
    } else {
        for (var p of products.slice(0, 8)) {
            var daysClass = p.days_left <= 7 ? 'danger' : (p.days_left <= 15 ? 'warning' : 'success');
            html += '<tr><td class="product-name">' + p.name + '</td>' +
                '<td>' + (p.expiry_date || '-') + '</td>' +
                '<td><span class="qty ' + daysClass + '">' + p.days_left + 'j</span></td></tr>';
        }
    }
    html += '</tbody></table>';
    document.getElementById('tableExpiry').innerHTML = html;
    document.getElementById('expiryCount').textContent = products.length;
}

// === CHART FUNCTIONS ===
