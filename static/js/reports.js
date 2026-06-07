async function applyReportPeriod() {
    const periodSelect = document.getElementById('reportPeriod');
    if (!periodSelect) { loadReport(currentReport); return; }
    const period = parseInt(periodSelect.value);
    const today = new Date();
    const startDate = new Date(today);
    startDate.setDate(startDate.getDate() - period);
    const dateStartEl = document.getElementById('reportDateStart');
    const dateEndEl = document.getElementById('reportDateEnd');
    if (dateStartEl) dateStartEl.valueAsDate = startDate;
    if (dateEndEl) dateEndEl.valueAsDate = today;

    const kpiPeriod = document.getElementById('kpiPeriod');
    if (kpiPeriod && kpiPeriod.value !== periodSelect.value) {
        kpiPeriod.value = periodSelect.value;
    }

    loadReport(currentReport);
    loadDashboard();
}

function onReportPeriodChange() {
    const dateStartEl = document.getElementById('reportDateStart');
    const dateEndEl = document.getElementById('reportDateEnd');

    if (dateStartEl) dateStartEl.value = '';
    if (dateEndEl) dateEndEl.value = '';

    applyReportPeriod();
}

function onReportDateChange() {
    const dateStartEl = document.getElementById('reportDateStart');
    const dateEndEl = document.getElementById('reportDateEnd');
    const periodSelect = document.getElementById('reportPeriod');

    if (dateStartEl && dateStartEl.value) {
        if (dateEndEl && dateEndEl.value && periodSelect) {
            periodSelect.value = '';
        }
    }
}

function refreshCurrentReport() {
    loadReport(currentReport);
    loadDashboard();
}

function setActiveReportButton(btnId) {
    document.querySelectorAll('.reports-nav .btn').forEach(b => {
        b.classList.remove('btn-primary');
        b.classList.add('btn-outline');
    });
    const btn = document.getElementById(btnId);
    if (btn) {
        btn.classList.add('btn-primary');
        btn.classList.remove('btn-outline');
    }
}

async function loadReport(reportType) {
    currentReport = reportType;
    const container = document.getElementById('reportContentArea');
    container.innerHTML = '<div style="text-align:center;padding:3rem;"><i class="fas fa-spinner fa-spin" style="font-size:2rem;color:var(--primary);"></i><p style="margin-top:1rem;">Chargement du rapport...</p></div>';
    setActiveReportButton('btnReport' + reportType.charAt(0).toUpperCase() + reportType.slice(1));

    const periodSelect = document.getElementById('reportPeriod');
    const dateStart = document.getElementById('reportDateStart')?.value;
    const dateEnd = document.getElementById('reportDateEnd')?.value;

    let period = '';
    if (dateStart || dateEnd) {
        period = '';
    } else {
        period = periodSelect?.value || '30';
    }

    try {
        if (reportType === 'overview') await loadOverviewReport(container, period, dateStart, dateEnd);
        else if (reportType === 'sales') await loadSalesReport(container, period, dateStart, dateEnd);
        else if (reportType === 'purchases') await loadPurchasesReport(container, period, dateStart, dateEnd);
        else if (reportType === 'stock') await loadStockReport(container, period, dateStart, dateEnd);
        else if (reportType === 'financial') await loadFinancialReport(container, period, dateStart, dateEnd);
    } catch(e) {
        container.innerHTML = '<div class="alert alert-danger">Erreur de chargement: ' + e.message + '</div>';
    }
}

async function loadOverviewReport(container, period, dateStart, dateEnd) {
    const p = period || '30';
    let params = '?period=' + p;
    if (dateStart) params += '&date_start=' + dateStart;
    if (dateEnd) params += '&date_end=' + dateEnd;

    const [salesRes, ordersRes, stockRes, trendsRes] = await Promise.all([
        fetch('/api/kpis/sales' + params),
        fetch('/api/kpis/orders-summary?period=' + p),
        fetch('/api/kpis/dashboard?period=' + p),
        fetch('/api/kpis/sales-daily?period=' + p)
    ]);

    const sales = await salesRes.json();
    const orders = await ordersRes.json();
    const stock = await stockRes.json();
    const trends = await trendsRes.json();

    container.innerHTML = `
        <div class="report-section">
            <h3><i class="fas fa-chart-line"></i> KPIs Business</h3>
            <div class="report-kpi-grid">
                <div class="report-kpi-card">
                    <div class="label">CA Periode</div>
                    <div class="value primary">${(sales.ca_periode || 0).toLocaleString()} DH</div>
                    <div class="trend ${sales.ca_trend >= 0 ? 'up' : 'down'}">${sales.ca_trend >= 0 ? '↑' : '↓'} ${Math.abs(sales.ca_trend || 0)}% vs periode precedente</div>
                </div>
                <div class="report-kpi-card">
                    <div class="label">Ventes</div>
                    <div class="value">${sales.nb_ventes_periode || 0}</div>
                    <div class="trend">transactions</div>
                </div>
                <div class="report-kpi-card">
                    <div class="label">Ticket Moyen</div>
                    <div class="value success">${(sales.ticket_moyen || 0).toLocaleString()} DH</div>
                </div>
                <div class="report-kpi-card">
                    <div class="label">Commandes Fournisseur</div>
                    <div class="value warning">${(orders.brouillon || 0) + (orders.recu || 0)}</div>
                    <div class="trend">en cours</div>
                </div>
            </div>
        </div>

        <div class="report-grid-2">
            <div class="report-section">
                <h3><i class="fas fa-boxes"></i> Gestion Stock</h3>
                <div class="report-kpi-grid">
                    <div class="report-kpi-card">
                        <div class="label">Valeur Stock</div>
                        <div class="value primary">${(stock.total_value || 0).toLocaleString()} DH</div>
                    </div>
                    <div class="report-kpi-card">
                        <div class="label">Ruptures</div>
                        <div class="value danger">${stock.out_of_stock || 0}</div>
                    </div>
                    <div class="report-kpi-card">
                        <div class="label">Stock Faible</div>
                        <div class="value warning">${stock.low_stock || 0}</div>
                    </div>
                </div>
            </div>
            <div class="report-section">
                <h3><i class="fas fa-exclamation-triangle"></i> Alertes</h3>
                ${stock.out_of_stock > 0 ? '<div class="report-insight negative"><strong>' + stock.out_of_stock + '</strong> produits en rupture de stock</div>' : '<div class="report-insight positive">Aucune rupture de stock</div>'}
                ${stock.low_stock > 0 ? '<div class="report-insight warning"><strong>' + stock.low_stock + '</strong> produits avec stock faible</div>' : ''}
            </div>
        </div>

        <div class="report-section">
            <h3><i class="fas fa-chart-area"></i> Tendances Ventes (${period} jours)</h3>
            <div id="overviewChart" class="report-chart-container"></div>
        </div>
    `;

    if (trends && trends.length > 0) {
        renderSimpleChart('overviewChart', trends, 'date', 'ca', 'Ventes');
    }
}

async function loadSalesReport(container, period, dateStart, dateEnd) {
    const p = period || '30';
    let params = '?period=' + p;
    if (dateStart) params += '&date_start=' + dateStart;
    if (dateEnd) params += '&date_end=' + dateEnd;

    let topParams = '?limit=10&period=' + p;
    if (dateStart) topParams += '&date_start=' + dateStart;
    if (dateEnd) topParams += '&date_end=' + dateEnd;

    const [salesRes, dailyRes, topRes] = await Promise.all([
        fetch('/api/kpis/sales' + params),
        fetch('/api/kpis/sales-daily?period=' + p),
        fetch('/api/kpis/top-selling-products' + topParams)
    ]);

    const sales = await salesRes.json();
    const daily = await dailyRes.json();
    const topProducts = await topRes.json();

    container.innerHTML = `
        <div class="report-section">
            <h3><i class="fas fa-cash-register"></i> Synthese Ventes</h3>
            <div class="report-kpi-grid">
                <div class="report-kpi-card">
                    <div class="label">CA Total</div>
                    <div class="value primary">${(sales.ca_periode || 0).toLocaleString()} DH</div>
                </div>
                <div class="report-kpi-card">
                    <div class="label">CA Jour</div>
                    <div class="value">${(sales.ca_jour || 0).toLocaleString()} DH</div>
                </div>
                <div class="report-kpi-card">
                    <div class="label">Nb Ventes</div>
                    <div class="value">${sales.nb_ventes_periode || 0}</div>
                </div>
                <div class="report-kpi-card">
                    <div class="label">Ticket Moyen</div>
                    <div class="value success">${(sales.ticket_moyen || 0).toLocaleString()} DH</div>
                </div>
            </div>
        </div>

        <div class="report-section">
            <h3><i class="fas fa-chart-area"></i> Ventes Quotidiennes</h3>
            <div id="salesDailyChart" class="report-chart-container"></div>
        </div>

        <div class="report-section">
            <h3><i class="fas fa-trophy"></i> Top 10 Produits Vendus</h3>
            <table class="report-table">
                <thead><tr><th>Produit</th><th>Quantite Vendue</th><th>CA</th></tr></thead>
                <tbody>
                    ${topProducts.slice(0, 10).map(p => {
                        return '<tr><td>' + p.name + '</td><td>' + (p.qty_vendue || 0) + '</td><td>' + (p.ca || 0).toFixed(2) + ' DH</td></tr>';
                    }).join('')}
                </tbody>
            </table>
        </div>
    `;

    if (daily && daily.length > 0) {
        renderSimpleChart('salesDailyChart', daily, 'date', 'ca', 'CA');
    }
}

async function loadPurchasesReport(container, period, dateStart, dateEnd) {
    const p = period || '30';
    const params = '?period=' + p;

    const res = await fetch('/api/kpis/orders-summary' + params);
    const orders = await res.json();

    let insightsHtml = '';
    if (orders.brouillon > 0) insightsHtml += '<div class="report-insight warning"><strong>' + orders.brouillon + '</strong> commandes en attente de reception</div>';
    if (orders.recu > 0) insightsHtml += '<div class="report-insight positive"><strong>' + orders.recu + '</strong> commandes recues et traitees</div>';
    insightsHtml += '<div class="report-insight">Cumul des paiements fournisseurs: <strong>' + (orders.total_value || 0).toLocaleString() + ' DH</strong></div>';

    container.innerHTML = `
        <div class="report-section">
            <h3><i class="fas fa-truck"></i> Synthese Achats</h3>
            <div class="report-kpi-grid">
                <div class="report-kpi-card">
                    <div class="label">Brouillon</div>
                    <div class="value warning">${orders.brouillon || 0}</div>
                    <div class="trend">commandes</div>
                </div>
                <div class="report-kpi-card">
                    <div class="label">Recues</div>
                    <div class="value success">${orders.recu || 0}</div>
                    <div class="trend">commandes</div>
                </div>
                <div class="report-kpi-card">
                    <div class="label">Payees</div>
                    <div class="value primary">${orders.paye || 0}</div>
                    <div class="trend">commandes</div>
                </div>
                <div class="report-kpi-card">
                    <div class="label">Total Paye</div>
                    <div class="value danger">${(orders.total_value || 0).toLocaleString()} DH</div>
                </div>
            </div>
        </div>

        <div class="report-section">
            <h3><i class="fas fa-info-circle"></i> Insights Achats</h3>
            ${insightsHtml}
        </div>
    `;
}

async function loadStockReport(container, period, dateStart, dateEnd) {
    const p = period || '30';

    const res = await fetch('/api/kpis/dashboard?period=' + p);
    const stock = await res.json();

    const rotateRate = stock.rotation_rate || 0;
    const dio = stock.dio || 0;
    const rotationClass = rotateRate > 30 ? 'success' : (rotateRate > 15 ? 'warning' : 'danger');
    const rotationTrend = rotateRate > 30 ? 'Excellente rotation' : (rotateRate > 15 ? 'Rotation normale' : 'Rotation faible');

    let productsToOrderHtml = '';
    if (stock.products_to_order && stock.products_to_order.length > 0) {
        productsToOrderHtml = '<table class="report-table"><thead><tr><th>Produit</th><th>Stock Actuel</th><th>Minimum</th><th>A Commander</th></tr></thead><tbody>';
        stock.products_to_order.slice(0, 10).forEach(p => {
            const qtyClass = p.quantity <= 0 ? 'danger' : 'warning';
            productsToOrderHtml += '<tr><td>' + p.name + '</td><td class="' + qtyClass + '">' + p.quantity + '</td><td>' + p.min_quantity + '</td><td><strong>+' + p.needed + '</strong></td></tr>';
        });
        productsToOrderHtml += '</tbody></table>';
    }

    container.innerHTML = `
        <div class="report-section">
            <h3><i class="fas fa-boxes"></i> Synthese Stock</h3>
            <div class="report-kpi-grid">
                <div class="report-kpi-card">
                    <div class="label">Valeur Stock</div>
                    <div class="value primary">${(stock.total_value || 0).toLocaleString()} DH</div>
                </div>
                <div class="report-kpi-card">
                    <div class="label">Nb Produits</div>
                    <div class="value">${stock.total_products || 0}</div>
                </div>
                <div class="report-kpi-card">
                    <div class="label">Ruptures</div>
                    <div class="value danger">${stock.out_of_stock || 0}</div>
                </div>
                <div class="report-kpi-card">
                    <div class="label">Stock Faible</div>
                    <div class="value warning">${stock.low_stock || 0}</div>
                </div>
            </div>
        </div>

        <div class="report-grid-2">
            <div class="report-section">
                <h3><i class="fas fa-chart-line"></i> Rotation Stock</h3>
                <div class="report-kpi-card">
                    <div class="label">Taux Rotation</div>
                    <div class="value ${rotationClass}">${rotateRate.toFixed(1)}%</div>
                    <div class="trend">${rotationTrend}</div>
                </div>
            </div>
            <div class="report-section">
                <h3><i class="fas fa-clock"></i> Delai Inventory</h3>
                <div class="report-kpi-card">
                    <div class="label">DIO</div>
                    <div class="value">${dio.toFixed(0)}</div>
                    <div class="trend">jours</div>
                </div>
            </div>
        </div>

        <div class="report-section">
            <h3><i class="fas fa-exclamation-triangle"></i> Produits a Commander</h3>
            ${productsToOrderHtml}
        </div>
    `;
}

async function loadFinancialReport(container, period, dateStart, dateEnd) {
    const p = period || '30';
    let params = '?period=' + p;
    if (dateStart) params += '&date_start=' + dateStart;
    if (dateEnd) params += '&date_end=' + dateEnd;

    const [marginsRes, receivablesRes, salesRes] = await Promise.all([
        fetch('/api/kpis/margins' + params),
        fetch('/api/kpis/receivables' + params),
        fetch('/api/kpis/sales' + params)
    ]);

    const margins = await marginsRes.json();
    const receivables = await receivablesRes.json();
    const sales = await salesRes.json();

    const margeGlobale = margins.marge_globale || 0;
    const creanceTotale = receivables.total_creances || 0;
    const caPeriode = sales.ca_periode || 0;

    const margeClass = margeGlobale >= 20 ? 'success' : (margeGlobale >= 10 ? 'warning' : 'danger');
    const margeLabel = margeGlobale >= 20 ? 'Marge excelente' : (margeGlobale >= 10 ? 'Marge correcte' : 'Marge a ameliorer');
    const creanceClass = creanceTotale > 0 ? 'danger' : 'success';
    const creanceLabel = creanceTotale > 0 ? 'Creances en attente' : 'Aucune creance';

    let insightsHtml = '';
    if (margeGlobale < 15) insightsHtml += '<div class="report-insight warning"><strong>Alerte:</strong> Marge brute inferieure a 15%. Pensez a renegocier vos prix fournisseurs.</div>';
    if (creanceTotale > 0) insightsHtml += '<div class="report-insight negative"><strong>Action requise:</strong> Follow-up des creances clients pour ameliorer la tresorerie.</div>';
    else insightsHtml += '<div class="report-insight positive">Tresorerie saine: aucune creance en attente.</div>';
    insightsHtml += '<div class="report-insight">Chiffre d affineures periode: <strong>' + caPeriode.toLocaleString() + ' DH</strong></div>';

    container.innerHTML = `
        <div class="report-section">
            <h3><i class="fas fa-chart-pie"></i> Performance Financiere</h3>
            <div class="report-kpi-grid">
                <div class="report-kpi-card">
                    <div class="label">CA Periode</div>
                    <div class="value primary">${caPeriode.toLocaleString()} DH</div>
                </div>
                <div class="report-kpi-card">
                    <div class="label">Marge Brute</div>
                    <div class="value ${margeClass}">${margeGlobale.toFixed(1)}%</div>
                </div>
                <div class="report-kpi-card">
                    <div class="label">Creances</div>
                    <div class="value ${creanceClass}">${creanceTotale.toLocaleString()} DH</div>
                </div>
                <div class="report-kpi-card">
                    <div class="label">Ticket Moyen</div>
                    <div class="value">${(sales.ticket_moyen || 0).toLocaleString()} DH</div>
                </div>
            </div>
        </div>

        <div class="report-grid-2">
            <div class="report-section">
                <h3><i class="fas fa-percentage"></i> Analyse Marges</h3>
                <div class="report-kpi-card">
                    <div class="label">Marge Globale</div>
                    <div class="value ${margeClass}">${margeGlobale.toFixed(1)}%</div>
                    <div class="trend">${margeLabel}</div>
                </div>
            </div>
            <div class="report-section">
                <h3><i class="fas fa-user-clock"></i> Creances Clients</h3>
                <div class="report-kpi-card">
                    <div class="label">Total Creances</div>
                    <div class="value ${creanceClass}">${creanceTotale.toLocaleString()} DH</div>
                    <div class="trend">${creanceLabel}</div>
                </div>
            </div>
        </div>

        <div class="report-section">
            <h3><i class="fas fa-lightbulb"></i> Insights Financiers</h3>
            ${insightsHtml}
        </div>
    `;
}

let sessionsData = [];

async function loadSessionsHistory() {
    const tbody = document.getElementById('sessionsTableBody');
    if (!tbody) return;
    tbody.innerHTML = '<tr><td colspan="10" style="text-align:center;padding:2rem;"><i class="fas fa-spinner fa-spin"></i> Chargement...</td></tr>';

    try {
        const [historyRes, summaryRes] = await Promise.all([
            fetch('/api/kpis/sessions-history?limit=50'),
            fetch('/api/kpis/sessions-summary?period=365')
        ]);

        const sessions = await historyRes.json();
        const summary = await summaryRes.json();

        sessionsData = sessions;

        document.getElementById('sessionTotalSessions').textContent = summary.total_sessions || 0;
        document.getElementById('sessionClosedSessions').textContent = summary.closed_sessions || 0;
        document.getElementById('sessionTotalCa').textContent = (summary.total_sales_period || 0).toLocaleString() + ' DH';
        document.getElementById('sessionTotalTransactions').textContent = summary.nb_transactions_period || 0;

        if (sessions.length === 0) {
            tbody.innerHTML = '<tr><td colspan="10" style="text-align:center;padding:2rem;color:var(--text-light);">Aucune session trouvee</td></tr>';
            return;
        }

        let html = '';
        for (let i = 0; i < sessions.length; i++) {
            const s = sessions[i];
            const statusClass = s.status === 'closed' ? 'success' : 'warning';
            const openedAt = s.opened_at ? s.opened_at.substring(0, 16) : '-';
            const closedAt = s.closed_at ? s.closed_at.substring(0, 16) : '-';
            html += '<tr>';
            html += '<td><strong>' + s.session_number + '</strong></td>';
            html += '<td><span class="badge badge-' + statusClass + '">' + (s.status === 'closed' ? 'Fermee' : 'Ouverte') + '</span></td>';
            html += '<td>' + (s.user_name || 'Caissier') + '</td>';
            html += '<td>' + openedAt + '</td>';
            html += '<td>' + closedAt + '</td>';
            html += '<td><strong>' + Number(s.total_sales || 0).toFixed(2) + '</strong></td>';
            html += '<td>' + s.nb_transactions + '</td>';
            html += '<td>' + Number(s.expected_cash || 0).toFixed(2) + '</td>';
            html += '<td>' + Number(s.closing_cash || 0).toFixed(2) + '</td>';
            html += '<td><button class="btn btn-sm btn-outline" onclick="viewSessionDetails(' + s.id + ')"><i class="fas fa-eye"></i></button></td>';
            html += '</tr>';
        }
        tbody.innerHTML = html;
    } catch(e) {
        tbody.innerHTML = '<tr><td colspan="10" class="alert alert-danger">Erreur: ' + e.message + '</td></tr>';
    }
}

async function viewSessionDetails(sessionId) {
    try {
        const res = await fetch('/api/kpis/sessions/' + sessionId + '/details');
        const data = await res.json();

        document.getElementById('sessionDetailTitle').textContent = 'Session: ' + data.session.session_number;

        const s = data.session;
        document.getElementById('sessionDetailKpi').innerHTML =
            '<div class="report-kpi-card"><div class="label">Ventes Total</div><div class="value primary">' + (s.total_sales || 0).toLocaleString() + ' DH</div></div>' +
            '<div class="report-kpi-card"><div class="label">Transactions</div><div class="value">' + data.transactions.length + '</div></div>' +
            '<div class="report-kpi-card"><div class="label">Esp. Attendu</div><div class="value">' + (s.expected_cash || 0).toLocaleString() + ' DH</div></div>' +
            '<div class="report-kpi-card"><div class="label">Esp. Reel</div><div class="value">' + (s.closing_cash || 0).toLocaleString() + ' DH</div></div>';

        const openedAt = s.opened_at ? s.opened_at.substring(0, 16).replace('T', ' ') : '-';
        const closedAt = s.closed_at ? s.closed_at.substring(0, 16).replace('T', ' ') : '-';
        const durationHtml = s.opened_at && s.closed_at ? (() => {
            const diff = new Date(s.closed_at) - new Date(s.opened_at);
            const mins = Math.floor(diff / 60000);
            const hrs = Math.floor(mins / 60);
            return (hrs > 0 ? hrs + 'h ' : '') + (mins % 60) + 'min';
        })() : '-';

        document.getElementById('sessionDetailKpi').innerHTML =
            '<div class="report-kpi-card"><div class="label">Ventes</div><div class="value primary">' + Number(s.total_sales || 0).toFixed(2) + ' DH</div></div>' +
            '<div class="report-kpi-card"><div class="label">Transactions</div><div class="value">' + data.transactions.length + '</div></div>' +
            '<div class="report-kpi-card"><div class="label">Esp. Attendu</div><div class="value">' + Number(s.expected_cash || 0).toFixed(2) + ' DH</div></div>' +
            '<div class="report-kpi-card"><div class="label">Esp. Reel</div><div class="value">' + Number(s.closing_cash || 0).toFixed(2) + ' DH</div></div>';

        const infoHtml =
            '<div style="display:flex;flex-wrap:wrap;gap:0.5rem 2rem;padding:1rem;background:var(--color-canvas);border-radius:8px;margin-bottom:1rem;font-size:0.9rem;">' +
            '<div><span style="color:var(--text-light);">Status</span><br><strong>' + (s.status === 'closed' ? 'Fermee' : 'Ouverte') + '</strong></div>' +
            '<div><span style="color:var(--text-light);">Caissier</span><br><strong>' + (s.user_name || 'Caissier') + '</strong></div>' +
            '<div><span style="color:var(--text-light);">Entrepot</span><br><strong>' + (s.warehouse_name || 'Entrepot ' + (s.warehouse_id || 1)) + '</strong></div>' +
            '<div><span style="color:var(--text-light);">Ouverture</span><br><strong>' + openedAt + '</strong></div>' +
            '<div><span style="color:var(--text-light);">Fermeture</span><br><strong>' + closedAt + '</strong></div>' +
            '<div><span style="color:var(--text-light);">Duree</span><br><strong>' + durationHtml + '</strong></div>' +
            '<div><span style="color:var(--text-light);">Ouverture Caisse</span><br><strong>' + Number(s.opening_cash || 0).toFixed(2) + ' DH</strong></div>' +
            '</div>';

        document.getElementById('sessionDetailInfo').innerHTML = infoHtml;

        if (data.transactions.length > 0) {
            let html = '<table class="table table-compact" style="width:100%;"><thead><tr>' +
                '<th>N</th><th>Client</th><th>Total</th><th>Sous-total</th><th>TVA</th><th>Remise</th><th>Mode</th><th>Nb</th><th>Date</th></tr></thead><tbody>';
            for (let i = 0; i < data.transactions.length; i++) {
                const t = data.transactions[i];
                const methodLabel = t.payment_method === 'cash' ? 'Especes' :
                                   t.payment_method === 'card' ? 'Carte' : 'Mixte';
                html += '<tr>' +
                    '<td><strong>' + (t.ticket_number || t.transaction_number || '-') + '</strong></td>' +
                    '<td>' + (t.customer_name || 'Comptoir') + '</td>' +
                    '<td><strong>' + Number(t.total).toFixed(2) + '</strong></td>' +
                    '<td>' + Number(t.subtotal || 0).toFixed(2) + '</td>' +
                    '<td>' + Number(t.tax_amount || 0).toFixed(2) + '</td>' +
                    '<td>' + Number(t.discount_amount || 0).toFixed(2) + '</td>' +
                    '<td>' + methodLabel + '</td>' +
                    '<td>' + (t.items_count || '-') + '</td>' +
                    '<td>' + (t.created_at || '').substring(0, 16) + '</td>' +
                    '</tr>';
            }
            html += '</tbody></table>';
            document.getElementById('sessionDetailTransactions').innerHTML = html;
        } else {
            document.getElementById('sessionDetailTransactions').innerHTML = '<p class="text-muted" style="text-align:center;padding:1rem;">Aucune transaction</p>';
        }

        if (data.cash_movements && data.cash_movements.length > 0) {
            let html = '<table class="table table-compact" style="width:100%;"><thead><tr>' +
                '<th>Type</th><th>Montant</th><th>Motif</th><th>Source</th><th>Date</th></tr></thead><tbody>';
            for (let i = 0; i < data.cash_movements.length; i++) {
                const m = data.cash_movements[i];
                const typeLabel = m.type === 'in' ? '<span style="color:var(--success);">Entree</span>' :
                                 '<span style="color:var(--danger);">Sortie</span>';
                html += '<tr>' +
                    '<td>' + typeLabel + '</td>' +
                    '<td>' + Number(m.amount).toFixed(2) + ' DH</td>' +
                    '<td>' + (m.reason || '-') + '</td>' +
                    '<td>' + (m.source || 'pos') + '</td>' +
                    '<td>' + (m.created_at || '').substring(0, 16) + '</td>' +
                    '</tr>';
            }
            html += '</tbody></table>';
            document.getElementById('sessionDetailCashMovements').innerHTML = html;
        } else {
            document.getElementById('sessionDetailCashMovements').innerHTML = '<p class="text-muted" style="text-align:center;padding:1rem;">Aucun mouvement</p>';
        }

        openModal('sessionDetailModal');
    } catch(e) {
        showError('Erreur: ' + e.message);
    }
}

function exportSessions(format) {
    if (sessionsData.length === 0) { showError('Aucune session a exporter'); return; }

    const csv = [];
    csv.push('Session,Statut,Ouverture,Fermeture,Ventes,Transactions,Attendu,Reel');
    for (let i = 0; i < sessionsData.length; i++) {
        const s = sessionsData[i];
        const esc = v => String(v).replace(/"/g, '""');
        csv.push('"' + esc(s.session_number) + '","' + esc(s.status) + '","' + esc(s.opened_at) + '","' + esc(s.closed_at || '') + '",' + esc(s.total_sales) + ',' + esc(s.nb_transactions) + ',' + esc(s.expected_cash || 0) + ',' + esc(s.closing_cash || 0));
    }

    const blob = new Blob([csv.join('\n')], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'sessions_history.csv';
    a.click();
}

function renderSimpleChart(containerId, data, xKey, yKey, label) {
    const container = document.getElementById(containerId);
    if (!container || !data || data.length === 0) return;

    if (typeof ApexCharts !== 'undefined') {
        const chart = new ApexCharts(container, {
            chart: { type: 'area', height: 280, toolbar: { show: false }, animations: { enabled: true } },
            series: [{ name: label, data: data.map(d => d[yKey] || 0) }],
            xaxis: { categories: data.map(d => d[xKey] ? d[xKey].substring(5) : ''), labels: { style: { fontSize: '10px' } } },
            stroke: { curve: 'smooth', width: 2 },
            colors: [cssVar('--color-primary')],
            fill: { type: 'gradient', gradient: { shadeIntensity: 1, opacityFrom: 0.4, opacityTo: 0.1 } },
            dataLabels: { enabled: false },
            tooltip: { y: { formatter: val => val.toFixed(2) } }
        });
        chart.render();
    }
}

function exportReport(format) {
    const content = document.getElementById('reportContentArea');
    const title = 'Rapport - ' + currentReport.charAt(0).toUpperCase() + currentReport.slice(1);

    if (format === 'print') {
        const printWindow = window.open('', '_blank');
        printWindow.document.write('<html><head><title>' + title + '</title>');
        printWindow.document.write('<style>body{font-family:Arial,sans-serif;padding:20px;} .report-section{margin-bottom:20px;border:1px solid #ddd;padding:15px;border-radius:8px;} .report-kpi-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:15px;margin:15px 0;} .report-kpi-card{text-align:center;padding:10px;background:#f8fafc;border-radius:8px;} .report-kpi-card .label{font-size:12px;color:#666;} .report-kpi-card .value{font-size:24px;font-weight:bold;}</style>');
        printWindow.document.write('</head><body>' + content.innerHTML + '</body></html>');
        printWindow.document.close();
        printWindow.print();
    } else if (format === 'csv') {
        const tables = content.querySelectorAll('table');
        if (tables.length === 0) { showError('Aucune table a exporter'); return; }
        const table = tables[0];
        const csv = [];
        const rows = table.querySelectorAll('tr');
        rows.forEach(row => {
            const cols = row.querySelectorAll('th, td');
            const rowData = [];
            cols.forEach(col => { rowData.push('"' + col.innerText.replace(/"/g, '""') + '"'); });
            csv.push(rowData.join(','));
        });
        const blob = new Blob([csv.join('\n')], { type: 'text/csv' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = title + '.csv';
        a.click();
    } else if (format === 'pdf') {
        showSuccess('Generation PDF en cours...');
        const printWindow = window.open('', '_blank');
        printWindow.document.write('<html><head><title>' + title + '</title>');
        printWindow.document.write('<script src="https://cdnjs.cloudflare.com/ajax/libs/html2pdf.js/0.10.1/html2pdf.bundle.min.js"><\/script>');
        printWindow.document.write('<style>body{font-family:Arial,sans-serif;padding:20px;} .report-section{margin-bottom:20px;}</style>');
        printWindow.document.write('</head><body><div id="pdfContent">' + content.innerHTML + '</div>');
        printWindow.document.write('<script>html2pdf().from(document.getElementById("pdfContent")).save("' + title + '.pdf");<\/script>');
        printWindow.document.close();
    }
}

document.addEventListener('DOMContentLoaded', () => {
    applyReportPeriod();
});
