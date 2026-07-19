const ReportsEngine = {
    state: {
        activeTab: 'overview',
        charts: {},
        filters: {
            date_start: null,
            date_end: null,
            period: '30',
            category: ''
        }
    },

    filters: {
        init() {
            const saved = localStorage.getItem('reports_filters');
            if (saved) {
                try { Object.assign(ReportsEngine.state.filters, JSON.parse(saved)); } catch(e) {}
            }
            const ps = document.getElementById('reportPeriod');
            const ds = document.getElementById('reportDateStart');
            const de = document.getElementById('reportDateEnd');
            if (ps) ps.value = ReportsEngine.state.filters.period || '30';
            if (ds && ReportsEngine.state.filters.date_start) ds.value = ReportsEngine.state.filters.date_start;
            if (de && ReportsEngine.state.filters.date_end) de.value = ReportsEngine.state.filters.date_end;
        },

        read() {
            const ps = document.getElementById('reportPeriod');
            const ds = document.getElementById('reportDateStart');
            const de = document.getElementById('reportDateEnd');
            const f = ReportsEngine.state.filters;
            f.period = ps?.value || '30';
            f.date_start = ds?.value || null;
            f.date_end = de?.value || null;
            if (f.date_start || f.date_end) f.period = '';
            localStorage.setItem('reports_filters', JSON.stringify(f));
            return f;
        },

        apply() {
            const f = ReportsEngine.filters.read();
            ReportsEngine.load(ReportsEngine.state.activeTab);
        },

        onPeriodChange() {
            const ds = document.getElementById('reportDateStart');
            const de = document.getElementById('reportDateEnd');
            if (ds) ds.value = '';
            if (de) de.value = '';
            ReportsEngine.filters.apply();
        },

        onDateChange() {
            const ps = document.getElementById('reportPeriod');
            const ds = document.getElementById('reportDateStart');
            if (ds?.value && ps) ps.value = '';
        },

        getQueryString() {
            const f = ReportsEngine.state.filters;
            let q = '';
            if (f.date_start) q += '&date_start=' + f.date_start;
            if (f.date_end) q += '&date_end=' + f.date_end;
            if (!f.date_start && !f.date_end && f.period) q += '&period=' + f.period;
            return q ? '?' + q.substring(1) : '?period=30';
        }
    },

    destroyChart(id) {
        if (ReportsEngine.state.charts[id]) {
            try { ReportsEngine.state.charts[id].destroy(); } catch(e) {}
            delete ReportsEngine.state.charts[id];
        }
    },

    render: {
        loading() {
            return '<div class="reports-loading"><i class="fas fa-spinner fa-spin"></i><p>Chargement...</p></div>';
        },

        empty(message) {
            return '<div class="reports-empty"><i class="fas fa-inbox"></i><p>' + (message || 'Aucune donnee') + '</p></div>';
        },

        kpiCard(label, value, trend, trendDir, cssClass) {
            let html = '<div class="report-kpi-card' + (cssClass ? ' ' + cssClass : '') + '">';
            html += '<div class="label">' + label + '</div>';
            html += '<div class="value">' + value + '</div>';
            if (trend !== undefined && trend !== null) {
                const dir = trendDir || (trend >= 0 ? 'up' : 'down');
                html += '<div class="trend ' + dir + '">' + (dir === 'up' ? '+' : '') + trend + '% vs periode precedente</div>';
            }
            html += '</div>';
            return html;
        },

        kpiRow(kpis) {
            return '<div class="report-kpi-grid">' + kpis.map(k => ReportsEngine.render.kpiCard(k.label, k.value, k.trend, k.trendDir, k.cssClass)).join('') + '</div>';
        },

        section(title, icon, content) {
            return '<div class="report-section"><h3><i class="fas fa-' + icon + '"></i> ' + title + '</h3>' + content + '</div>';
        },

        twoCol(left, right) {
            return '<div class="report-grid-2">' + left + right + '</div>';
        },

        table(headers, rows, emptyMsg) {
            if (!rows || rows.length === 0) return ReportsEngine.render.empty(emptyMsg || 'Aucune donnee');
            let html = '<div class="report-table-wrap"><table class="report-table"><thead><tr>';
            headers.forEach(h => { html += '<th>' + h + '</th>'; });
            html += '</tr></thead><tbody>';
            rows.forEach(row => {
                html += '<tr>';
                row.forEach(cell => { html += '<td>' + cell + '</td>'; });
                html += '</tr>';
            });
            html += '</tbody></table></div>';
            return html;
        },

        chart(containerId, data, xKey, yKey, label, type) {
            ReportsEngine.destroyChart(containerId);
            const container = document.getElementById(containerId);
            if (!container || !data || data.length === 0) return;
            if (typeof ApexCharts === 'undefined') return;

            const chartType = type || 'area';
            const chart = new ApexCharts(container, {
                chart: { type: chartType, height: 280, toolbar: { show: false }, animations: { enabled: true } },
                series: [{ name: label, data: data.map(d => d[yKey] || 0) }],
                xaxis: { categories: data.map(d => d[xKey] ? d[xKey].substring(5) : ''), labels: { style: { fontSize: '10px' } } },
                stroke: { curve: 'smooth', width: 2 },
                colors: [getComputedStyle(document.documentElement).getPropertyValue('--color-primary') || '#2980b9'],
                fill: chartType === 'area' ? { type: 'gradient', gradient: { shadeIntensity: 1, opacityFrom: 0.4, opacityTo: 0.1 } } : undefined,
                dataLabels: { enabled: false },
                tooltip: { y: { formatter: val => val.toFixed(2) } }
            });
            chart.render();
            ReportsEngine.state.charts[containerId] = chart;
        },

        doughnut(containerId, labels, values, colors) {
            ReportsEngine.destroyChart(containerId);
            const container = document.getElementById(containerId);
            if (!container || !values || values.length === 0) return;
            if (typeof ApexCharts === 'undefined') return;

            const chart = new ApexCharts(container, {
                chart: { type: 'donut', height: 280 },
                series: values,
                labels: labels,
                colors: colors || ['#2980b9', '#27ae60', '#e74c3c', '#f39c12', '#9b59b6'],
                legend: { position: 'bottom', fontSize: '12px' },
                dataLabels: { enabled: false }
            });
            chart.render();
            ReportsEngine.state.charts[containerId] = chart;
        }
    },

    data: {
        async _get(url) {
            const res = await fetch(url);
            if (!res.ok) throw new Error('GET ' + url + ' → ' + res.status);
            return await res.json();
        },

        async fetchOverview() {
            const q = ReportsEngine.filters.getQueryString();
            const d = ReportsEngine.data;
            const [overview, daily, payments] = await Promise.all([
                d._get('/api/reports/overview' + q),
                d._get('/api/reports/sales-by-day' + q),
                d._get('/api/kpis/payment-methods' + q)
            ]);
            return { overview, daily, payments };
        },

        async fetchByProduct() {
            const q = ReportsEngine.filters.getQueryString();
            return await ReportsEngine.data._get('/api/reports/sales-by-product' + q + '&limit=20');
        },

        async fetchByCustomer() {
            const q = ReportsEngine.filters.getQueryString();
            return await ReportsEngine.data._get('/api/reports/sales-by-customer' + q + '&limit=20');
        },

        async fetchDaily() {
            const q = ReportsEngine.filters.getQueryString();
            return await ReportsEngine.data._get('/api/reports/sales-by-day' + q);
        },

        async fetchMargins() {
            const q = ReportsEngine.filters.getQueryString();
            return await ReportsEngine.data._get('/api/reports/margins' + q);
        },

        async fetchExceptions() {
            return await ReportsEngine.data._get('/api/reports/exceptions');
        }
    },

    views: {
        overview(data) {
            const o = data.overview;
            const kpis = ReportsEngine.render.kpiRow([
                { label: 'CA Periode', value: (o.ca_total || 0).toLocaleString() + ' DH', trend: o.ca_trend, cssClass: 'primary' },
                { label: 'Nb Ventes', value: o.nb_ventes || 0, cssClass: '' },
                { label: 'Ticket Moyen', value: (o.ticket_moyen || 0).toLocaleString() + ' DH', cssClass: 'success' },
                { label: 'Marge Brute', value: (o.marge_pct || 0).toFixed(1) + '%', cssClass: o.marge_pct >= 20 ? 'success' : o.marge_pct >= 10 ? 'warning' : 'danger' }
            ]);

            const chartSection = ReportsEngine.render.section('Evolution CA', 'chart-line',
                '<div id="overviewChart" class="report-chart-container"></div>');

            let paymentHtml = '';
            if (data.payments?.methods) {
                const methods = data.payments.methods;
                const labels = Object.keys(methods);
                const values = labels.map(m => methods[m].total || 0);
                paymentHtml = ReportsEngine.render.section('Repartition Paiements', 'chart-pie',
                    '<div id="paymentChart" class="report-chart-container"></div>');
                setTimeout(() => ReportsEngine.render.doughnut('paymentChart', labels, values), 100);
            }

            const stockHtml = ReportsEngine.render.twoCol(
                ReportsEngine.render.section('Stock', 'boxes',
                    ReportsEngine.render.kpiRow([
                        { label: 'Valeur Stock', value: (o.stock_value || 0).toLocaleString() + ' DH', cssClass: 'primary' },
                        { label: 'DIO', value: (o.dio || 0).toFixed(0) + ' jours', cssClass: '' }
                    ])),
                ReportsEngine.render.section('Alertes', 'exclamation-triangle',
                    '<div class="report-insight">Consultez l\'onglet <strong>Alertes</strong> pour les details</div>')
            );

            let html = kpis + chartSection + paymentHtml + stockHtml;
            const container = document.getElementById('reportContentArea');
            container.innerHTML = html;

            if (data.daily?.length > 0) {
                ReportsEngine.render.chart('overviewChart', data.daily, 'date', 'ca', 'CA');
            }
        },

        products(data) {
            const kpis = ReportsEngine.render.kpiRow([
                { label: 'CA Total', value: data.reduce((s, p) => s + (p.ca || 0), 0).toLocaleString() + ' DH', cssClass: 'primary' },
                { label: 'Qty Vendue', value: data.reduce((s, p) => s + (p.qty_vendue || 0), 0), cssClass: '' },
                { label: 'Nb Produits', value: data.length, cssClass: '' }
            ]);

            const headers = ['Produit', 'SKU', 'Categorie', 'Qte Vendue', 'CA (DH)', 'Prix Unit.'];
            const rows = data.map(p => [
                escapeHtml(p.name || '-'),
                escapeHtml(p.sku || '-'),
                escapeHtml(p.category || '-'),
                p.qty_vendue || 0,
                (p.ca || 0).toFixed(2),
                (p.price || 0).toFixed(2)
            ]);
            const table = ReportsEngine.render.table(headers, rows, 'Aucune vente pour cette periode');

            const container = document.getElementById('reportContentArea');
            container.innerHTML = kpis + ReportsEngine.render.section('Top Produits par CA', 'trophy', table);
        },

        customers(data) {
            const kpis = ReportsEngine.render.kpiRow([
                { label: 'CA Total Clients', value: data.reduce((s, c) => s + (c.ca_total || 0), 0).toLocaleString() + ' DH', cssClass: 'primary' },
                { label: 'Nb Clients', value: data.length, cssClass: '' },
                { label: 'CA Moyen/Client', value: data.length > 0 ? (data.reduce((s, c) => s + (c.ca_total || 0), 0) / data.length).toFixed(0) + ' DH' : '0 DH', cssClass: 'success' }
            ]);

            const headers = ['Client', 'Code', 'Nb Achats', 'CA Total (DH)'];
            const rows = data.map(c => [
                escapeHtml(c.name || '-'),
                escapeHtml(c.client_code || '-'),
                c.nb_achats || 0,
                (c.ca_total || 0).toFixed(2)
            ]);
            const table = ReportsEngine.render.table(headers, rows, 'Aucun client pour cette periode');

            const container = document.getElementById('reportContentArea');
            container.innerHTML = kpis + ReportsEngine.render.section('Top Clients', 'users', table);
        },

        daily(data) {
            const totalCA = data.reduce((s, d) => s + (d.ca || 0), 0);
            const totalNb = data.reduce((s, d) => s + (d.nb_ventes || 0), 0);
            const avgCA = data.length > 0 ? totalCA / data.length : 0;

            const kpis = ReportsEngine.render.kpiRow([
                { label: 'CA Total', value: totalCA.toLocaleString() + ' DH', cssClass: 'primary' },
                { label: 'Nb Jours', value: data.length, cssClass: '' },
                { label: 'CA Moyen/Jour', value: avgCA.toFixed(0) + ' DH', cssClass: 'success' },
                { label: 'Total Ventes', value: totalNb, cssClass: '' }
            ]);

            const chartSection = ReportsEngine.render.section('CA Journalier', 'chart-line',
                '<div id="dailyChart" class="report-chart-container"></div>');

            const headers = ['Date', 'CA (DH)', 'Nb Ventes'];
            const rows = data.slice().reverse().map(d => [
                d.date,
                (d.ca || 0).toFixed(2),
                d.nb_ventes || 0
            ]);
            const table = ReportsEngine.render.table(headers, rows);

            const container = document.getElementById('reportContentArea');
            container.innerHTML = kpis + chartSection + ReportsEngine.render.section('Detail Quotidien', 'calendar', table);

            if (data.length > 0) {
                ReportsEngine.render.chart('dailyChart', data, 'date', 'ca', 'CA');
            }
        },

        margins(data) {
            const kpis = ReportsEngine.render.kpiRow([
                { label: 'Marge Globale', value: (data.marge_globale || 0).toFixed(1) + '%', cssClass: data.marge_globale >= 20 ? 'success' : data.marge_globale >= 10 ? 'warning' : 'danger' },
                { label: 'Marge Montant', value: (data.marge_montant || 0).toLocaleString() + ' DH', cssClass: 'primary' },
                { label: 'Nb Categories', value: data.categories?.length || 0, cssClass: '' }
            ]);

            const headers = ['Categorie', 'Vente (DH)', 'Achat (DH)', 'Marge %', 'Marge (DH)'];
            const rows = (data.categories || []).map(c => [
                escapeHtml(c.category || '-'),
                (c.vente || 0).toFixed(2),
                (c.achat || 0).toFixed(2),
                '<span class="' + (c.marge_pct >= 20 ? 'success' : c.marge_pct >= 10 ? 'warning' : 'danger') + '">' + (c.marge_pct || 0).toFixed(1) + '%</span>',
                (c.marge_montant || 0).toFixed(2)
            ]);
            const table = ReportsEngine.render.table(headers, rows, 'Aucune donnee de marge');

            const container = document.getElementById('reportContentArea');
            container.innerHTML = kpis + ReportsEngine.render.section('Marges par Categorie', 'percentage', table);
        },

        exceptions(data) {
            const kpis = ReportsEngine.render.kpiRow([
                { label: 'Ruptures', value: data.out_of_stock?.length || 0, cssClass: 'danger' },
                { label: 'Stock Faible', value: data.low_stock?.length || 0, cssClass: 'warning' },
                { label: 'Marges Neg.', value: data.negative_margins?.length || 0, cssClass: 'danger' }
            ]);

            let content = '';

            if (data.out_of_stock?.length > 0) {
                const headers = ['Produit', 'SKU', 'Stock Min', 'Prix'];
                const rows = data.out_of_stock.map(p => [
                    escapeHtml(p.name || '-'),
                    escapeHtml(p.sku || '-'),
                    p.min_quantity || 0,
                    (p.price || 0).toFixed(2)
                ]);
                content += ReportsEngine.render.section('Ruptures de Stock', 'times-circle',
                    ReportsEngine.render.table(headers, rows));
            }

            if (data.low_stock?.length > 0) {
                const headers = ['Produit', 'SKU', 'Stock Actuel', 'Minimum', 'A Commander'];
                const rows = data.low_stock.map(p => [
                    escapeHtml(p.name || '-'),
                    escapeHtml(p.sku || '-'),
                    '<span class="warning">' + (p.quantity || 0) + '</span>',
                    p.min_quantity || 0,
                    '<strong>+' + (p.needed || 0) + '</strong>'
                ]);
                content += ReportsEngine.render.section('Stock Faible', 'exclamation-triangle',
                    ReportsEngine.render.table(headers, rows));
            }

            if (data.negative_margins?.length > 0) {
                const headers = ['Produit', 'SKU', 'Vente', 'Achat', 'Marge'];
                const rows = data.negative_margins.map(p => [
                    escapeHtml(p.name || '-'),
                    escapeHtml(p.sku || '-'),
                    (p.vente || 0).toFixed(2),
                    (p.achat || 0).toFixed(2),
                    '<span class="danger">' + ((p.vente || 0) - (p.achat || 0)).toFixed(2) + '</span>'
                ]);
                content += ReportsEngine.render.section('Marges Negatives', 'thumbs-down',
                    ReportsEngine.render.table(headers, rows));
            }

            if (!content) content = ReportsEngine.render.empty('Aucune alerte - tout va bien !');

            const container = document.getElementById('reportContentArea');
            container.innerHTML = kpis + content;
        }
    },

    async load(tab) {
        ReportsEngine.state.activeTab = tab || 'overview';
        const container = document.getElementById('reportContentArea');
        if (!container) return;
        container.innerHTML = ReportsEngine.render.loading();

        document.querySelectorAll('.reports-nav .btn').forEach(b => {
            b.classList.remove('btn-primary');
            b.classList.add('btn-outline');
        });
        const activeBtn = document.getElementById('btnReport' + tab.charAt(0).toUpperCase() + tab.slice(1));
        if (activeBtn) {
            activeBtn.classList.add('btn-primary');
            activeBtn.classList.remove('btn-outline');
        }

        try {
            switch (ReportsEngine.state.activeTab) {
                case 'overview':
                    ReportsEngine.views.overview(await ReportsEngine.data.fetchOverview());
                    break;
                case 'products':
                    ReportsEngine.views.products(await ReportsEngine.data.fetchByProduct());
                    break;
                case 'customers':
                    ReportsEngine.views.customers(await ReportsEngine.data.fetchByCustomer());
                    break;
                case 'daily':
                    ReportsEngine.views.daily(await ReportsEngine.data.fetchDaily());
                    break;
                case 'margins':
                    ReportsEngine.views.margins(await ReportsEngine.data.fetchMargins());
                    break;
                case 'exceptions':
                    ReportsEngine.views.exceptions(await ReportsEngine.data.fetchExceptions());
                    break;
                default:
                    ReportsEngine.views.overview(await ReportsEngine.data.fetchOverview());
            }
        } catch(e) {
            container.innerHTML = '<div class="alert alert-danger">Erreur: ' + escapeHtml(e.message) + '</div>';
        }
    },

    export: {
        csv() {
            const content = document.getElementById('reportContentArea');
            const table = content?.querySelector('table');
            if (!table) { showError('Aucune table a exporter'); return; }
            const csv = [];
            table.querySelectorAll('tr').forEach(row => {
                const cols = [];
                row.querySelectorAll('th, td').forEach(col => {
                    cols.push('"' + col.innerText.replace(/"/g, '""') + '"');
                });
                csv.push(cols.join(','));
            });
            const blob = new Blob([csv.join('\n')], { type: 'text/csv' });
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'rapport_' + ReportsEngine.state.activeTab + '.csv';
            a.click();
        },

        pdf() {
            const content = document.getElementById('reportContentArea');
            if (!content) return;
            const title = 'Rapport - ' + ReportsEngine.state.activeTab;
            const printWindow = window.open('', '_blank');
            if (!printWindow) { showError('Pop-up bloquee'); return; }
            printWindow.document.write('<html><head><title>' + title + '</title>');
            printWindow.document.write('<style>body{font-family:Arial,sans-serif;padding:20px;} .report-section{margin-bottom:20px;border:1px solid #ddd;padding:15px;border-radius:8px;} .report-kpi-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:15px;margin:15px 0;} .report-kpi-card{text-align:center;padding:10px;background:#f8fafc;border-radius:8px;} .report-kpi-card .label{font-size:12px;color:#666;} .report-kpi-card .value{font-size:24px;font-weight:bold;} table{width:100%;border-collapse:collapse;} th,td{border:1px solid #ddd;padding:6px 8px;text-align:left;font-size:12px;} th{background:#f0f0f0;}</style>');
            printWindow.document.write('</head><body>' + content.innerHTML + '</body></html>');
            printWindow.document.close();
            printWindow.print();
        },

        print() {
            ReportsEngine.export.pdf();
        }
    }
};

function loadReport(reportType) {
    ReportsEngine.load(reportType);
}

function refreshCurrentReport() {
    ReportsEngine.filters.apply();
}

function onReportPeriodChange() {
    ReportsEngine.filters.onPeriodChange();
}

function onReportDateChange() {
    ReportsEngine.filters.onDateChange();
}

function applyReportPeriod() {
    ReportsEngine.filters.apply();
}

function exportReport(format) {
    if (format === 'csv') ReportsEngine.export.csv();
    else if (format === 'pdf') ReportsEngine.export.pdf();
    else if (format === 'print') ReportsEngine.export.print();
}

document.addEventListener('DOMContentLoaded', () => {
    ReportsEngine.filters.init();
});
