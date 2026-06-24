let currentWarehouse = null;
let products = [];
let warehouses = [];
let locations = [];
let suppliers = [];
let orders = [];
let customers = [];
let deliveryNotes = [];
let invoices = [];

function initTheme() {
    const savedTheme = localStorage.getItem('stockpro-theme');
    if (savedTheme) {
        document.documentElement.setAttribute('data-theme', savedTheme);
        updateThemeIcon(savedTheme);
    } else {
        const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
        if (prefersDark) {
            document.documentElement.setAttribute('data-theme', 'dark');
            updateThemeIcon('dark');
        }
    }
}

function toggleTheme() {
    const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', newTheme);
    localStorage.setItem('stockpro-theme', newTheme);
    updateThemeIcon(newTheme);
}

function updateThemeIcon(theme) {
    const icon = document.getElementById('themeIcon');
    if (icon) {
        icon.className = theme === 'dark' ? 'fas fa-sun' : 'fas fa-moon';
    }
}

initTheme();

document.addEventListener('DOMContentLoaded', () => {
    initApp();
});

async function initApp() {
    try {
        var licRes = await fetch('/api/license/status');
        var licData = await licRes.json();
        if (!licData.admin && !licData.activated) {
            showTab('license');
            return;
        }
    } catch(e) {}
    try {
        const res = await fetch('/api/warehouses');
        const data = await res.json();
        warehouses = data;
        if (warehouses.length > 0) {
            currentWarehouse = warehouses[0].id;
        }
        if (typeof initDashboardDates === 'function') initDashboardDates();
        else loadDashboard();
        loadSuppliers();
    } catch(e) {
        console.error('Init error:', e);
        loadDashboard();
    }
}

function showError(msg) {
    const el = document.getElementById('errorMsg');
    if (el) {
        el.textContent = msg;
        el.style.display = msg ? 'block' : 'none';
        if (msg) setTimeout(() => { el.style.display = 'none'; }, 5000);
    }
}

function showSuccess(msg) {
    const el = document.getElementById('successMsg');
    if (el) {
        el.textContent = msg;
        el.style.display = msg ? 'block' : 'none';
        if (msg) setTimeout(() => { el.style.display = 'none'; }, 5000);
    }
}

function openModal(modalId) {
    const el = document.getElementById(modalId);
    if (el) {
        el.style.display = '';
        el.classList.add('active');
    }
}

function closeModal(modalId) {
    const el = document.getElementById(modalId);
    if (el) {
        el.classList.remove('active');
        el.style.display = 'none';
    }
}

function showTab(tab) {
    const contents = document.querySelectorAll('.tab-content');
    const navItems = document.querySelectorAll('.nav-item');
    for (let i = 0; i < contents.length; i++) { contents[i].classList.remove('active'); }
    for (let i = 0; i < navItems.length; i++) { navItems[i].classList.remove('active'); }
    const tabEl = document.getElementById(tab + 'Tab');
    const titleEl = document.getElementById('pageTitle');
    const breadcrumbEl = document.getElementById('breadcrumbNav');
    if (tabEl) tabEl.classList.add('active');
    const navItem = document.querySelector(`.nav-item[data-arg="${tab}"]`);
    if (navItem) navItem.classList.add('active');
    if (titleEl) titleEl.textContent = i18n.t('nav.' + tab);
    if (breadcrumbEl) breadcrumbEl.textContent = i18n.t('nav.' + tab);
    if (typeof i18n !== 'undefined') i18n.applyDOM();
    if (tab !== 'scanner' && typeof stopScanner === 'function') stopScanner();
    if (tab !== 'pos' && typeof stopPosScanner === 'function') stopPosScanner();
    if (tab !== 'pos' && typeof stopSSE === 'function') stopSSE();
    if (tab !== 'dashboard' && typeof stopDashboardSSE === 'function') stopDashboardSSE();
    if (tab === 'products') { loadProducts(); loadCategories(); }
    if (tab === 'warehouses') loadWarehouses();
    if (tab === 'locations') loadLocations();
    if (tab === 'movements') loadMovements();
    if (tab === 'suppliers') loadSuppliers();
    if (tab === 'orders') loadOrders();
    if (tab === 'customers') loadCustomers();
    if (tab === 'invoices') loadInvoices();
    if (tab === 'dashboard') loadDashboard();
    if (tab === 'pos') { loadProducts(); loadPosRegisters(); loadPosCashMovements(); loadPosTransactions(); if (typeof startSSE === 'function' && typeof posSession !== 'undefined' && posSession) startSSE(); setTimeout(function () { document.getElementById('posSearchInput').focus(); }, 100); }
    if (tab === 'mainAccount') loadMainAccount();
    if (tab === 'reports') { currentReport = 'overview'; applyReportPeriod(); }
    if (tab === 'sessions') loadSessionsHistory();
    if (tab === 'settings') loadSettings();
    if (tab === 'license') loadLicense();
}

function toggleSidebar() {
    const sidebar = document.getElementById('sidebar');
    if (sidebar) sidebar.classList.toggle('open');
}

function cssVar(name) {
    return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}

let charts = {};
let chartsInitialized = false;

function renderSalesDailyChart(data) {
    try {
        const chartEl = document.querySelector("#chartSalesDaily");
        if (!chartEl || !data || data.length === 0) { console.log('No data for sales daily chart'); return; }
        if (typeof ApexCharts === 'undefined') { console.warn('ApexCharts not loaded yet'); return; }

        const dates = data.map(d => d.date.substring(5));
        const values = data.map(d => d.ca);

        if (charts.salesDaily) { charts.salesDaily.destroy(); charts.salesDaily = null; }

        charts.salesDaily = new ApexCharts(chartEl, {
            chart: { type: 'area', height: 250, toolbar: { show: false }, animations: { enabled: true, easing: 'easeinout', speed: 800 } },
            series: [{ name: 'CA', data: values }],
            colors: [cssVar('--color-primary')],
            fill: { type: 'gradient', gradient: { shadeIntensity: 1, opacityFrom: 0.4, opacityTo: 0.1 } },
            stroke: { curve: 'smooth', width: 2 },
            xaxis: { categories: dates, labels: { style: { colors: cssVar('--color-text-muted'), fontSize: '11px' } } },
            yaxis: { labels: { formatter: v => v.toLocaleString(), style: { colors: cssVar('--color-text-muted'), fontSize: '11px' } } },
            tooltip: { y: { formatter: v => v.toLocaleString() + ' DH' } },
            grid: { borderColor: cssVar('--color-border-subtle') }
        });
        charts.salesDaily.render();
    } catch(e) { console.error('SalesDaily chart error:', e); }
}

function renderCategoriesChart(data) {
    try {
        const chartEl = document.querySelector("#chartCategories");
        if (!chartEl || !data || data.length === 0) { console.log('No data for categories chart'); return; }
        if (typeof ApexCharts === 'undefined') { console.warn('ApexCharts not loaded yet'); return; }

        const labels = data.map(d => d.category);
        const values = data.map(d => d.qty_vendue);
        const colors = ['#2563eb', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#06b6d4'];

        if (charts.categories) { charts.categories.destroy(); charts.categories = null; }

        charts.categories = new ApexCharts(chartEl, {
            chart: { type: 'donut', height: 250, animations: { enabled: true, easing: 'easeinout', speed: 800 } },
            series: values,
            labels: labels,
            colors: colors.slice(0, labels.length),
            legend: { position: 'right', fontSize: '12px' },
            plotOptions: { pie: { donut: { size: '65%' } } },
            dataLabels: { enabled: false },
            tooltip: { y: { formatter: v => v + ' unites' } }
        });
        charts.categories.render();
    } catch(e) { console.error('Categories chart error:', e); }
}

function renderTopProductsChart(data) {
    try {
        const chartEl = document.querySelector("#chartTopProducts");
        if (!chartEl || !data || data.length === 0) { console.log('No data for top products chart'); return; }
        if (typeof ApexCharts === 'undefined') { console.warn('ApexCharts not loaded yet'); return; }

        const topProducts = data.slice(0, 10).reverse();
        const names = topProducts.map(p => p.name.substring(0, 20) + (p.name.length > 20 ? '...' : ''));
        const values = topProducts.map(p => p.qty_vendue);

        if (charts.topProducts) { charts.topProducts.destroy(); charts.topProducts = null; }

        charts.topProducts = new ApexCharts(chartEl, {
            chart: { type: 'bar', height: 250, toolbar: { show: false }, animations: { enabled: true, easing: 'easeinout', speed: 800 } },
            series: [{ name: 'Qte', data: values }],
            colors: [cssVar('--color-success')],
            plotOptions: { bar: { horizontal: true, borderRadius: 4, barHeight: '60%' } },
            xaxis: { categories: names, labels: { style: { colors: cssVar('--color-text-muted'), fontSize: '10px' } } },
            yaxis: { labels: { style: { colors: cssVar('--color-text-muted'), fontSize: '11px' } } },
            grid: { borderColor: cssVar('--color-border-subtle') },
            tooltip: { y: { formatter: v => v + ' unites' } }
        });
        charts.topProducts.render();
    } catch(e) { console.error('TopProducts chart error:', e); }
}

function renderInvoicesStatusChart(data) {
    try {
        const chartEl = document.querySelector("#chartInvoicesStatus");
        if (!chartEl || !data) { console.log('No data for invoices status chart'); return; }
        if (typeof ApexCharts === 'undefined') { console.warn('ApexCharts not loaded yet'); return; }

        if (charts.invoicesStatus) { charts.invoicesStatus.destroy(); charts.invoicesStatus = null; }

        charts.invoicesStatus = new ApexCharts(chartEl, {
            chart: { type: 'donut', height: 250, animations: { enabled: true, easing: 'easeinout', speed: 800 } },
            series: [data.payee || 0, data.envoyee || 0, data.brouillon || 0, data.annulee || 0],
            labels: ['Payees', 'Envoyees', 'Brouillon', 'Annulees'],
            colors: [cssVar('--color-success'), cssVar('--color-warning'), cssVar('--color-text-muted'), cssVar('--color-danger')],
            legend: { position: 'right', fontSize: '12px' },
            plotOptions: { pie: { donut: { size: '65%' } } },
            dataLabels: { enabled: false }
        });
        charts.invoicesStatus.render();
    } catch(e) { console.error('InvoicesStatus chart error:', e); }
}

function renderMovementsChart(data) {
    try {
        const chartEl = document.querySelector("#chartMovements");
        if (!chartEl || !data || data.length === 0) { console.log('No data for movements chart'); return; }
        if (typeof ApexCharts === 'undefined') { console.warn('ApexCharts not loaded yet'); return; }

        const dates = data.slice(-14).map(d => d.date.substring(5));
        const entries = data.slice(-14).map(d => d.entries);
        const exits = data.slice(-14).map(d => d.exits);

        if (charts.movements) { charts.movements.destroy(); charts.movements = null; }

        charts.movements = new ApexCharts(chartEl, {
            chart: { type: 'area', height: 250, stacked: false, toolbar: { show: false }, animations: { enabled: true, easing: 'easeinout', speed: 800 } },
            series: [{ name: 'Entrees', data: entries }, { name: 'Sorties', data: exits }],
            colors: [cssVar('--color-success'), cssVar('--color-warning')],
            fill: { type: 'gradient', gradient: { shadeIntensity: 1, opacityFrom: 0.3, opacityTo: 0.1 } },
            stroke: { curve: 'smooth', width: 2 },
            xaxis: { categories: dates, labels: { style: { colors: cssVar('--color-text-muted'), fontSize: '11px' } } },
            yaxis: { labels: { style: { colors: cssVar('--color-text-muted'), fontSize: '11px' } } },
            legend: { fontSize: '12px' },
            grid: { borderColor: cssVar('--color-border-subtle') }
        });
        charts.movements.render();
    } catch(e) { console.error('Movements chart error:', e); }
}

function renderMarginsChart(data) {
    const categoryColors = ['#2563eb', '#7c3aed', '#db2777', '#ea580c', '#0891b2', '#4f46e5', '#0d9488', '#65a30d', '#dc2626', '#f59e0b'];
    try {
        const chartEl = document.querySelector("#chartMargins");
        if (!chartEl || !data || data.length === 0) { console.log('No data for margins chart'); return; }
        if (typeof ApexCharts === 'undefined') { console.warn('ApexCharts not loaded yet'); return; }

        const filteredData = data.filter(d => Math.abs(d.marge_pct) > 0.01);
        if (filteredData.length === 0) {
            chartEl.innerHTML = '<div style="text-align:center;padding:80px 0;color:var(--text-light);"><i class="fas fa-chart-pie" style="font-size:3rem;opacity:0.3;"></i><p style="margin-top:1rem;">Aucune donnee de marge</p></div>';
            return;
        }

        if (charts.margins) { charts.margins.destroy(); charts.margins = null; }

        const labels = filteredData.map(d => d.category);
        const values = filteredData.map(d => Math.abs(d.marge_pct));
        const colors = filteredData.map((d, i) => categoryColors[i % categoryColors.length]);

        charts.margins = new ApexCharts(chartEl, {
            chart: { type: 'donut', height: 250, animations: { enabled: true, easing: 'easeinout', speed: 800 } },
            series: values,
            labels: labels,
            colors: colors,
            legend: { position: 'right', fontSize: '12px' },
            plotOptions: { pie: { donut: { size: '65%' } } },
            dataLabels: { enabled: true, formatter: (val, opts) => val.toFixed(1) + '%' },
            tooltip: { y: { formatter: (val, opts) => { const original = filteredData[opts.seriesIndex]; return (original.marge_pct >= 0 ? '+' : '') + original.marge_pct + '%'; } } }
        });
        charts.margins.render();
    } catch(e) { console.error('Margins chart error:', e); }
}

// === EVENT DELEGATION ===
document.addEventListener('click', e => {
    const el = e.target.closest('[data-click]');
    if (!el) return;
    const action = el.getAttribute('data-click');
    const arg = el.getAttribute('data-arg');
    const arg2 = el.getAttribute('data-arg2');
    switch (action) {
        case 'show-tab': showTab(arg); break;
        case 'close-modal': closeModal(arg); break;
        case 'toggle-theme': toggleTheme(); break;
        case 'toggle-sidebar': toggleSidebar(); break;
        case 'preset-period':
            if (typeof setPresetPeriod === 'function') setPresetPeriod(parseInt(arg));
            break;
        case 'reset-data': resetAllTransactionalData(); break;
        case 'load-dashboard': loadDashboard(); break;
        case 'load-products': loadProducts(); break;
        case 'load-movements': loadMovements(); break;
        case 'load-orders': loadOrders(); break;
        case 'load-customers': loadCustomers(); break;
        case 'load-invoices': loadInvoices(); break;
        case 'load-suppliers': loadSuppliers(); break;
        case 'load-warehouses': loadWarehouses(); break;
        case 'load-locations': loadLocations(); break;
        case 'load-sessions': loadSessionsHistory(); break;
        case 'open-product-modal': openProductModal(); break;
        case 'open-warehouse-modal': openWarehouseModal(); break;
        case 'open-location-modal': openLocationModal(); break;
        case 'open-supplier-modal': openSupplierModal(); break;
        case 'open-customer-modal': openCustomerModal(); break;
        case 'open-order-modal': openOrderModal(arg); break;
        case 'refresh-report': refreshCurrentReport(); break;
        case 'export-report': exportReport(arg); break;
        case 'export-sessions': exportSessions(arg); break;
        case 'start-scanner': startScanner('scannerVideo', onScanSuccess); break;
        case 'stop-scanner': stopScanner(); break;
        case 'pause-scanner': pauseScanner(); break;
        case 'clear-scan-history': clearScanHistory(); break;
        case 'add-scanned-to-cart': handleAddScannedToCart(e); break;
        case 'rescan-from-history': handleRescanFromHistory(e); break;
        case 'search-barcode': searchByBarcode(); break;
        case 'open-pos-session': openPosSession(); break;
        case 'close-pos-session': closePosSession(); break;
        case 'add-pos-from-search': addPosProductFromSearch(); break;
        case 'toggle-pos-scanner': togglePosScanner(); break;
        case 'clear-pos-cart': clearPosCart(); break;
        case 'set-pos-payment': setPosPaymentMethod(arg); break;
        case 'process-pos-payment': processPosPayment(); break;
        case 'pos-tendered-quick': setPosTenderedQuick(); break;
        case 'pos-tendered-round': setPosTenderedRound(); break;
        case 'save-pos-cash-in': savePosCashIn(); break;
        case 'save-pos-cash-out': savePosCashOut(); break;
        case 'load-pos-transactions': loadPosTransactions(); break;
        case 'open-transfer-to-pos': openTransferToPosModal(); break;
        case 'switch-product-tab': switchProductTab(arg); break;
        case 'load-report': loadReport(arg, arg2); break;
        case 'add-order-item': addOrderItem(); break;
        case 'toggle-order-scanner': toggleOrderScanner(); break;
        case 'cancel-order': cancelOrder(); break;
        case 'send-order': sendOrder(); break;
        case 'confirm-convert-invoice': confirmConvertToInvoice(); break;
        case 'edit-product': editProduct(parseInt(arg)); break;
        case 'edit-warehouse': editWarehouse(parseInt(arg)); break;
        case 'edit-location': editLocation(parseInt(arg)); break;
        case 'edit-supplier': editSupplier(parseInt(arg)); break;
        case 'edit-customer': editCustomer(parseInt(arg)); break;
        case 'open-stock-in': openStockModal(parseInt(arg), 'in'); break;
        case 'open-stock-out': openStockModal(parseInt(arg), 'out'); break;
        case 'open-product-detail': openProductDetail(parseInt(arg)); break;
        case 'delete-product': confirmDeleteProduct(parseInt(arg), arg2); break;
        case 'delete-product-from-detail': deleteProductFromDetail(); break;
        case 'save-product-prices': saveProductPrices(); break;
        case 'add-custom-price-tier': addCustomPriceTier(); break;
        case 'open-product-edit-from-detail': openProductEditFromDetail(); break;
        case 'open-product-page': openProductPage(); break;
        case 'create-order-from-detail': createOrderFromDetail(); break;
        case 'load-product-movements-list': loadProductMovementsList(); break;
        case 'open-stock-from-detail':
            if (currentProductDetail) {
                if (arg === 'in') openStockInFromDetail();
                else openStockOutFromDetail();
            }
            break;
        case 'save-stock': document.getElementById('stockForm').dispatchEvent(new Event('submit')); break;
    }
});

document.addEventListener('change', e => {
    const el = e.target.closest('[data-change]');
    if (!el) return;
    const action = el.getAttribute('data-change');
    switch (action) {
        case 'load-dashboard': loadDashboard(); break;
        case 'load-products': loadProducts(); break;
        case 'on-customer-change': onCustomerChange(); break;
        case 'apply-pos-discount': applyPosDiscount(); break;
        case 'handle-pos-cash-in': handlePosCashIn(); break;
        case 'handle-pos-cash-out': handlePosCashOut(); break;
        case 'load-invoices': loadInvoices(); break;
        case 'report-period': onReportPeriodChange(); break;
        case 'report-date': onReportDateChange(); break;
    }
});

document.addEventListener('input', e => {
    const el = e.target.closest('[data-input]');
    if (!el) return;
    const action = el.getAttribute('data-input');
    switch (action) {
        case 'filter-products': renderProducts(el.value); break;
        case 'search-pos': searchPosProducts(); break;
        case 'calc-pos-change': calculatePosChange(); break;
    }
});

document.addEventListener('keypress', e => {
    const el = e.target.closest('[data-keypress]');
    if (!el) return;
    const action = el.getAttribute('data-keypress');
    if (e.key !== 'Enter') return;
    switch (action) {
        case 'search-barcode': searchByBarcode(); break;
        case 'search-pos-add': if (el.value.length > 0) addPosProductFromSearch(); break;
    }
});

document.addEventListener('submit', e => {
    const form = e.target.closest('[data-submit]');
    if (!form) return;
    const action = form.getAttribute('data-submit');
    switch (action) {
        case 'save-product': saveProduct(e); break;
        case 'save-warehouse': saveWarehouse(e); break;
        case 'save-location': saveLocation(e); break;
        case 'save-supplier': saveSupplier(e); break;
        case 'save-customer': saveCustomer(e); break;
        case 'save-order': saveOrder(e); break;
        case 'save-stock': saveStock(e); break;
    }
});
