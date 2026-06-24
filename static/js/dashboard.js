let dashboardEventSource = null;

function startDashboardSSE() {
    if (dashboardEventSource) return;
    dashboardEventSource = new EventSource('/api/events');
    dashboardEventSource.addEventListener('transaction', function(e) {
        try {
            const regRes = fetch('/api/kpis/registers-status');
            regRes.then(r => r.json()).then(regs => {
                for (let i = 0; i < regs.length && i < 2; i++) {
                    const r = regs[i];
                    const idx = i + 1;
                    const valEl = document.getElementById('register' + idx + 'Sales');
                    const statusEl = document.getElementById('register' + idx + 'Status');
                    if (valEl) {
                        valEl.textContent = r.status === 'open' ? (r.total_sales || 0).toLocaleString() : '---';
                    }
                    if (statusEl) {
                        if (r.status === 'open') {
                            statusEl.textContent = 'Ouverte · ' + (r.nb_transactions || 0) + ' trans. · ' + (r.cash_balance || 0).toFixed(2) + ' DH';
                        } else {
                            statusEl.textContent = 'Fermée';
                        }
                    }
                }
            }).catch(() => {});
        } catch(ex) {}
    });
    dashboardEventSource.onerror = function() {
        if (dashboardEventSource) {
            dashboardEventSource.close();
            dashboardEventSource = null;
        }
    };
}

function stopDashboardSSE() {
    if (dashboardEventSource) {
        dashboardEventSource.close();
        dashboardEventSource = null;
    }
}

async function loadDashboard() {
    try {
        document.getElementById('dashboardLoader').classList.add('active');

        const dateStart = document.getElementById('kpiDateStart')?.value || '';
        const dateEnd = document.getElementById('kpiDateEnd')?.value || '';
        const warehouse = document.getElementById('warehouseFilter')?.value || '';

        let period = parseInt(document.getElementById('kpiPeriod')?.value) || 30;
        if (dateStart && dateEnd) {
            const diff = Math.round((new Date(dateEnd) - new Date(dateStart)) / (1000 * 60 * 60 * 24));
            period = Math.max(1, diff || 1);
        }

        const params = new URLSearchParams();
        if (dateStart) params.set('date_start', dateStart);
        if (dateEnd) params.set('date_end', dateEnd);
        if (!dateStart || !dateEnd) params.set('period', period);

        const [salesRes, marginsRes, receivablesRes, invoicesRes, dashboardRes,
             dailySalesRes, categoriesRes, topProductsRes, trendsRes, alertsRes, warehousesRes,
             paymentRes, expensesRes] = await Promise.all([
            fetch('/api/kpis/sales?' + params.toString()),
            fetch('/api/kpis/margins'),
            fetch('/api/kpis/receivables?' + params.toString()),
            fetch('/api/kpis/invoices-status?' + params.toString()),
            fetch('/api/kpis/dashboard?period=' + period + (warehouse ? '&warehouse_id=' + warehouse : '')),
            fetch('/api/kpis/sales-daily?period=' + period),
            fetch('/api/kpis/categories-distribution?' + params.toString()),
            fetch('/api/kpis/top-selling-products?limit=10&' + params.toString()),
            fetch('/api/kpis/trends?period=' + period),
            fetch('/api/kpis/alertes' + (warehouse ? '?warehouse_id=' + warehouse : '')),
            fetch('/api/warehouses'),
            fetch('/api/kpis/payment-methods?' + params.toString()),
            fetch('/api/kpis/expenses?' + params.toString())
        ]);

        const sales = await salesRes.json();
        const margins = await marginsRes.json();
        const receivables = await receivablesRes.json();
        const invoicesStatus = await invoicesRes.json();
        const dashboard = await dashboardRes.json();
        const dailySales = await dailySalesRes.json();
        const categories = await categoriesRes.json();
        const topProducts = await topProductsRes.json();
        const trends = await trendsRes.json();
        const alerts = await alertsRes.json();
        const warehousesData = await warehousesRes.json();
        const paymentMethods = await paymentRes.json();
        const expenses = await expensesRes.json();

        const whSelect = document.getElementById('warehouseFilter');
        if (whSelect && warehousesData.length > 0) {
            whSelect.innerHTML = '<option value="">Tous les entrepots</option>' +
                warehousesData.map(w => '<option value="' + w.id + '">' + w.name + '</option>').join('');
            whSelect.value = warehouse;
        }

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
        document.getElementById('valeurStockPA').textContent = (dashboard.total_value_purchase || 0).toLocaleString();
        var noPa = dashboard.products_no_purchase_price || 0;
        document.getElementById('valeurStockPANote').textContent = noPa > 0 ? noPa + ' produit(s) sans PA' : '';
        document.getElementById('produitsEnStock').textContent = dashboard.products_in_stock || 0;

        const methods = paymentMethods.methods || {};
        document.getElementById('posTotalEncaissement').textContent = (paymentMethods.total || 0).toLocaleString();
        document.getElementById('posCashTotal').textContent = ((methods.cash && methods.cash.total) || 0).toLocaleString();
        document.getElementById('posCardTotal').textContent = ((methods.card && methods.card.total) || 0).toLocaleString();

        try {
            const accRes = await fetch('/api/main-account');
            const accData = await accRes.json();
            if (accData.account) {
                document.getElementById('mainAccountKpi').textContent = (accData.account.current_balance || 0).toFixed(2);
            }
        } catch(e) { console.error('Main account KPI error:', e); }

        try {
            const regRes = await fetch('/api/kpis/registers-status');
            const regs = await regRes.json();
            for (let i = 0; i < regs.length && i < 2; i++) {
                const r = regs[i];
                const idx = i + 1;
                const valEl = document.getElementById('register' + idx + 'Sales');
                const statusEl = document.getElementById('register' + idx + 'Status');
                const card = valEl ? valEl.closest('.kpi-card') : null;
                if (valEl) {
                    valEl.textContent = r.status === 'open' ? (r.total_sales || 0).toLocaleString() : '---';
                    valEl.className = 'kpi-card-value ' + (r.status === 'open' ? 'success' : '');
                }
                if (statusEl) {
                    if (r.status === 'open') {
                        statusEl.textContent = 'Ouverte · ' + (r.nb_transactions || 0) + ' trans. · ' + (r.cash_balance || 0).toFixed(2) + ' DH';
                        statusEl.className = 'kpi-card-sub badge badge-success';
                    } else {
                        statusEl.textContent = 'Fermée';
                        statusEl.className = 'kpi-card-sub';
                    }
                }
                if (card) {
                    card.style.cursor = r.status === 'open' ? 'pointer' : '';
                    card.onclick = r.status === 'open' ? function() { showTab('pos'); } : null;
                }
            }
        } catch(e) { console.error('Register status error:', e); }

        document.getElementById('totalDepenses').textContent = (expenses.total_expenses || 0).toLocaleString();
        document.getElementById('depensesJour').textContent = (expenses.expenses_today || 0).toLocaleString();
        document.getElementById('depensesFournisseur').textContent = (expenses.supplier_orders || 0).toLocaleString();
        const trendEl = document.getElementById('depensesTrend');
        if (trendEl) {
            trendEl.innerHTML = expenses.trend >= 0 ?
                '<i class="fas fa-arrow-up"></i> ' + Math.abs(expenses.trend) + '%' :
                '<i class="fas fa-arrow-down"></i> ' + Math.abs(expenses.trend) + '%';
            trendEl.className = 'kpi-card-trend ' + (expenses.trend >= 0 ? 'up' : 'down');
        }

        updateTableToOrder(dashboard.products_to_order || []);
        updateTableCreances(receivables.clients || []);
        updateTableRuptures(alerts.out_of_stock || []);
        updateTableCategories();

        renderSalesDailyChart(dailySales);
        renderCategoriesChart(categories);
        renderTopProductsChart(topProducts);
        renderInvoicesStatusChart(invoicesStatus);
        renderMovementsChart(trends);
        renderMarginsChart(margins.categories || []);

        document.getElementById('dashboardLoader').classList.remove('active');
        startDashboardSSE();

    } catch(e) {
        console.error('Dashboard error:', e);
        document.getElementById('dashboardLoader').classList.remove('active');
        showError('Erreur de chargement du dashboard');
    }
}

function updateTableToOrder(products) {
    let html = '<table class="mini-table"><thead><tr><th>Produit</th><th>Stock</th><th>Min</th><th>Action</th></tr></thead><tbody>';
    if (products.length === 0) {
        html += '<tr><td colspan="4" style="text-align:center;color:var(--text-light)">Aucun produit</td></tr>';
    } else {
        for (const p of products.slice(0, 8)) {
            const qtyClass = p.quantity <= 0 ? 'danger' : (p.quantity <= p.min_quantity ? 'warning' : 'success');
            html += '<tr><td class="product-name">' + p.name + '</td>' +
                '<td><span class="qty ' + qtyClass + '">' + p.quantity + '</span></td>' +
                '<td>' + p.min_quantity + '</td>' +
                '<td><button class="btn btn-sm btn-primary" onclick="openOrderWithProduct(' + p.id + ')">Commander</button></td></tr>';
        }
    }
    html += '</tbody></table>';
    document.getElementById('tableToOrder').innerHTML = html;
    document.getElementById('toOrderCount').textContent = products.length;
}

function updateTableCreances(clients) {
    let html = '<table class="mini-table"><thead><tr><th>Client</th><th>Montant</th><th>Echeance</th></tr></thead><tbody>';
    if (clients.length === 0) {
        html += '<tr><td colspan="3" style="text-align:center;color:var(--text-light)">Aucune creance</td></tr>';
    } else {
        for (const c of clients.slice(0, 8)) {
            const isOverdue = c.premiere_echeance && new Date(c.premiere_echeance) < new Date();
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
    let html = '<table class="mini-table"><thead><tr><th>Produit</th><th>Stock</th><th>Min</th></tr></thead><tbody>';
    if (products.length === 0) {
        html += '<tr><td colspan="3" style="text-align:center;color:var(--text-light)">Aucune rupture</td></tr>';
    } else {
        for (const p of products.slice(0, 8)) {
            html += '<tr><td class="product-name">' + p.name + '</td>' +
                '<td><span class="qty danger">0</span></td>' +
                '<td>' + p.min_quantity + '</td></tr>';
        }
    }
    html += '</tbody></table>';
    document.getElementById('tableRuptures').innerHTML = html;
    document.getElementById('rupturesCount').textContent = products.length;
}

async function updateTableCategories() {
    try {
        const warehouse = document.getElementById('warehouseFilter')?.value || '';
        const params = new URLSearchParams();
        if (warehouse) params.set('warehouse_id', warehouse);
        const res = await fetch('/api/kpis/categories-stock?' + params.toString());
        const cats = await res.json();
        let html = '<table class="mini-table"><thead><tr><th>Catégorie</th><th class="qty">Qté</th><th class="price">Valeur Achat</th><th class="total">Valeur Vente</th></tr></thead><tbody>';
        if (cats.length === 0) {
            html += '<tr><td colspan="4" style="text-align:center;color:var(--text-light)">Aucune catégorie</td></tr>';
        } else {
            for (const c of cats) {
                html += '<tr>'
                    + '<td class="product-name">' + c.category + '</td>'
                    + '<td class="qty">' + c.total_qty + '</td>'
                    + '<td class="price">' + Number(c.total_purchase_value).toFixed(2) + ' DH</td>'
                    + '<td class="total">' + Number(c.total_sale_value).toFixed(2) + ' DH</td>'
                    + '</tr>';
            }
        }
        html += '</tbody></table>';
        document.getElementById('tableCategories').innerHTML = html;
        document.getElementById('categoriesCount').textContent = cats.length;
    } catch(e) {
        console.error('Categories stock error:', e);
    }
}

function setPresetPeriod(days) {
    const end = new Date();
    const start = new Date();
    start.setDate(start.getDate() - days);
    const fmt = d => d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
    document.getElementById('kpiDateStart').value = fmt(start);
    document.getElementById('kpiDateEnd').value = fmt(end);
    document.getElementById('kpiPeriod').value = days;
    document.querySelectorAll('.preset-btn').forEach(b => b.classList.remove('active'));
    const btn = document.querySelector('.preset-btn[data-period="' + days + '"]');
    if (btn) btn.classList.add('active');
    loadDashboard();
}

function initDashboardDates() {
    const ds = document.getElementById('kpiDateStart');
    const de = document.getElementById('kpiDateEnd');
    if (!ds.value && !de.value) setPresetPeriod(30);
}

async function resetAllTransactionalData() {
    const valid = await showPasswordPrompt('🔒 Réinitialisation des données', 'Entrez le mot de passe de réinitialisation pour continuer');
    if (!valid) return;

    openModal('resetConfirmModal');
    const submitBtn = document.getElementById('resetConfirmSubmit');
    submitBtn.disabled = true;
    setTimeout(() => { submitBtn.disabled = false; }, 2000);
}

async function executeReset() {
    const submitBtn = document.getElementById('resetConfirmSubmit');
    submitBtn.disabled = true;

    const doBackup = document.getElementById('backupBeforeReset').checked;

    try {
        if (doBackup) {
            const storeRes = await fetch('/api/stores/current');
            const storeData = await storeRes.json();
            if (storeData.id) {
                const backupRes = await fetch('/api/stores/' + storeData.id + '/backup', { method: 'POST' });
                const backupData = await backupRes.json();
                if (backupData.error) {
                    showError('Sauvegarde échouée: ' + backupData.error);
                    submitBtn.disabled = false;
                    return;
                }
            }
        }

        const res = await fetch('/api/reset-data', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ keep_products: true })
        });
        const data = await res.json();
        if (data.success) {
            closeModal('resetConfirmModal');
            location.reload();
        } else {
            document.getElementById('resetConfirmError').textContent = data.error || 'Erreur lors de la réinitialisation';
            document.getElementById('resetConfirmError').style.display = 'block';
            submitBtn.disabled = false;
        }
    } catch (err) {
        document.getElementById('resetConfirmError').textContent = 'Erreur réseau: ' + err.message;
        document.getElementById('resetConfirmError').style.display = 'block';
        submitBtn.disabled = false;
    }
}
