async function loadProducts() {
    try {
        const showArchived = document.getElementById('showArchivedProducts') ? document.getElementById('showArchivedProducts').checked : false;
        let url = '/api/products';
        if (showArchived) url += '?include_archived=true';
        const res = await fetch(url);
        products = await res.json();
        renderProducts();
        loadBestSellers();
    } catch(e) {
        showError('Erreur lors du chargement des produits');
    }
}

async function loadCategories() {
    try {
        const res = await fetch('/api/categories');
        const cats = await res.json();
        const select = document.getElementById('productCategory');
        if (!select) return;
        select.innerHTML = '<option value="">-- Sélectionner --</option>' +
            cats.map(function(c) {
                return '<option value="' + c.name_fr + '">' + c.name_ar + ' / ' + c.name_fr + '</option>';
            }).join('');
    } catch(e) {
        console.error('Erreur chargement catégories:', e);
    }
}

async function loadBestSellers() {
    try {
        const res = await fetch('/api/pos/best-sellers?limit=5');
        const bestSellers = await res.json();
        const container = document.getElementById('bestSellers');
        if (!container || !bestSellers || bestSellers.length === 0) return;
        const html = bestSellers.map(p => {
            return '<span class="pos-best-seller-tag" onclick="addPosProduct(' + p.id + ')">' + p.name + '</span>';
        }).join('');
        container.innerHTML = '<span style="font-size:0.75rem;color:var(--text-light);margin-right:0.5rem;">Les plus vendus:</span>' + html;
    } catch(e) {
        console.error('Error loading best sellers:', e);
    }
}

function renderProducts(filter) {
    const container = document.getElementById('productsListView');
    if (!container) return;
    filter = filter || '';
    const filtered = products.filter(p => {
        return p.name.toLowerCase().indexOf(filter.toLowerCase()) !== -1 || (p.sku && p.sku.toLowerCase().indexOf(filter.toLowerCase()) !== -1);
    });
    if (filtered.length === 0) {
        container.innerHTML = '<div class="empty">Aucun produit</div>';
        return;
    }
    let html = '<table class="table"><thead><tr><th></th><th>Produit</th><th>SKU</th><th>Qte</th><th>Prix</th><th>Actions</th></tr></thead><tbody>';
    for (let i = 0; i < filtered.length; i++) {
        const p = filtered[i];
        const isDeleted = p.is_deleted == 1;
        const rowClass = isDeleted ? ' style="opacity:0.5;text-decoration:line-through;"' : '';
        const statusClass = p.quantity <= p.min_quantity ? 'badge-danger' : (p.quantity <= p.min_quantity * 1.5 ? 'badge-warning' : 'badge-success');
        const imgSrc = p.image_url || '/static/img/no-image.png';
        html += '<tr' + rowClass + '>';
        html += '<td style="width:44px;"><img src="' + imgSrc + '" alt="" style="width:36px;height:36px;object-fit:cover;border-radius:4px;" onerror="this.src=\'/static/img/no-image.png\'"></td>';
        html += '<td><a href="#" onclick="openProductDetail(' + p.id + ')" class="product-link">' + p.name + (isDeleted ? ' <span class="badge badge-danger">Supprimé</span>' : '') + '</a></td>';
        html += '<td>' + (p.sku || '-') + '</td>';
        html += '<td><span class="badge ' + statusClass + '">' + p.quantity + '</span></td>';
        html += '<td>' + (p.price || 0).toFixed(2) + ' DH</td>';
        html += '<td>';
        if (!isDeleted) {
            html += '<button class="btn btn-sm btn-success" onclick="openStockModal(' + p.id + ', \'in\')" title="Entree">+</button> ';
            html += '<button class="btn btn-sm btn-warning" onclick="openStockModal(' + p.id + ', \'out\')" title="Sortie">-</button> ';
            html += '<button class="btn btn-sm btn-outline" onclick="openProductDetail(' + p.id + ')" title="Details"><i class="fas fa-eye"></i></button> ';
            html += '<button class="btn btn-sm btn-danger" onclick="confirmDeleteProduct(' + p.id + ', \'' + p.name.replace(/'/g, "\\'") + '\')" title="Supprimer"><i class="fas fa-trash"></i></button>';
        } else {
            html += '<button class="btn btn-sm btn-outline" onclick="openProductDetail(' + p.id + ')" title="Details"><i class="fas fa-eye"></i></button>';
        }
        html += '</td></tr>';
    }
    html += '</tbody></table>';
    container.innerHTML = html;
}

let currentProductDetail = null;

async function openProductDetail(productId) {
    try {
        const res = await fetch('/api/products/' + productId);
        const data = await res.json();
        currentProductDetail = data.product;

        const p = data.product;
        document.getElementById('productDetailTitle').textContent = p.name;
        document.getElementById('productDetailSku').textContent = 'SKU: ' + (p.sku || '-') + ' | ' + (p.barcode || 'Sans barcode');
        document.getElementById('productDetailQty').textContent = p.quantity;
        var detailImg = document.getElementById('productDetailImage');
        if (detailImg) {
            detailImg.src = p.image_url || '/static/img/no-image.png';
            detailImg.style.display = '';
        }

        const stockBadge = document.getElementById('productDetailStockBadge');
        stockBadge.className = 'product-stock-badge';
        if (p.quantity <= 0) stockBadge.classList.add('danger');
        else if (p.quantity <= p.min_quantity) stockBadge.classList.add('danger');
        else if (p.quantity <= p.min_quantity * 1.5) stockBadge.classList.add('warning');
        else stockBadge.classList.add('success');

        document.getElementById('productDetailPrice').textContent = (p.price || 0).toFixed(2) + ' DH';
        const statusEl = document.getElementById('productDetailStatus');
        if (p.quantity <= p.min_quantity) {
            statusEl.textContent = 'Stock Faible';
            statusEl.className = 'status-badge danger';
        } else if (p.quantity <= 0) {
            statusEl.textContent = 'Rupture';
            statusEl.className = 'status-badge danger';
        } else {
            statusEl.textContent = 'Normal';
            statusEl.className = 'status-badge success';
        }

        document.getElementById('detail-name').textContent = p.name;
        document.getElementById('detail-name-ar').textContent = p.name_ar || '-';
        var catLabel = p.category || '-';
        if (p.category_ar) catLabel = p.category_ar + ' / ' + p.category;
        document.getElementById('detail-category').textContent = catLabel;
        document.getElementById('detail-barcode').textContent = p.barcode || '-';
        document.getElementById('detail-warehouse').textContent = p.warehouse_name || '-';
        document.getElementById('detail-location').textContent = p.location_name || '-';
        document.getElementById('detail-qty').textContent = p.quantity;
        document.getElementById('detail-min-qty').textContent = p.min_quantity;
        document.getElementById('detail-max-qty').textContent = p.max_quantity;
        document.getElementById('detail-expiry').textContent = p.expiry_date || '-';
        document.getElementById('detail-lot').textContent = p.lot_number || '-';

        const purchasePrice = p.purchase_price_avg || 0;
        const sellingPrice = p.price || 0;
        const margin = sellingPrice > 0 ? ((sellingPrice - purchasePrice) / sellingPrice * 100).toFixed(1) : 0;
        document.getElementById('detail-selling-price').textContent = sellingPrice.toFixed(2) + ' DH';
        document.getElementById('detail-purchase-price').textContent = purchasePrice.toFixed(2) + ' DH';
        document.getElementById('detail-margin').textContent = margin + '%';

        const priceBase = p.price_base || 0;
        document.getElementById('detail-price-loyal').value = (p.price_loyal || 0).toFixed(2);
        document.getElementById('detail-price-gros').value = (p.price_gros || 0).toFixed(2);
        document.getElementById('detail-price-base').value = priceBase.toFixed(2);

        renderCustomPriceTiers(p.extra_prices || []);
        hidePriceSaveButton();

        document.getElementById('detail-supplier-name').textContent = p.supplier_name || '-';
        document.getElementById('detail-supplier-email').textContent = p.supplier_email || '-';
        document.getElementById('detail-supplier-phone').textContent = p.supplier_phone || '-';

        const stats = data.purchase_stats;
        document.getElementById('detail-total-purchases').textContent = (stats && stats.total_purchases) ? Number(stats.total_purchases).toFixed(2) + ' DH' : '0 DH';
        document.getElementById('detail-total-purchase-qty').textContent = (stats && stats.total_qty) || 0;
        const sales = data.sales_stats;
        document.getElementById('detail-total-sales').textContent = (sales && sales.total_sales) ? Number(sales.total_sales).toFixed(2) + ' DH' : '0 DH';
        document.getElementById('detail-total-sale-qty').textContent = (sales && sales.total_qty) || 0;

        let locationsHtml = '';
        if (data.stock_locations && data.stock_locations.length > 0) {
            for (let i = 0; i < data.stock_locations.length; i++) {
                const loc = data.stock_locations[i];
                locationsHtml += '<div class="stock-location-item"><span class="location-name"><i class="fas fa-map-marker"></i> ' + loc.location_name + '</span><span class="location-qty">' + loc.quantity + '</span></div>';
            }
        } else {
            locationsHtml = '<div class="empty">Aucun stock par emplacement</div>';
        }
        document.getElementById('detail-stock-locations').innerHTML = locationsHtml;

        loadProductMovementsList(data.movements);

        switchProductTab('overview');
        openModal('productDetailModal');

    } catch(e) {
        console.error('Error loading product:', e);
        showError('Erreur chargement produit');
    }
}

function loadProductMovementsList(movements) {
    let html = '';
    if (movements && movements.length > 0) {
        for (let i = 0; i < movements.length; i++) {
            const m = movements[i];
            const typeClass = m.type === 'in' ? 'success' : 'warning';
            const icon = m.type === 'in' ? 'fa-arrow-down' : 'fa-arrow-up';
            html += '<div class="movement-item ' + m.type + '">';
            html += '<div class="movement-icon ' + m.type + '"><i class="fas ' + icon + '"></i></div>';
            html += '<div class="movement-details">';
            html += '<span class="movement-qty">' + (m.type === 'in' ? '+' : '-') + m.quantity + '</span>';
            html += ' <span class="movement-date">' + (m.created_at ? m.created_at.substring(0, 16) : '-') + '</span>';
            if (m.note) html += '<div class="movement-note">' + m.note + '</div>';
            html += '</div></div>';
        }
    } else {
        html = '<div class="empty">Aucun mouvement</div>';
    }
    document.getElementById('detail-movements').innerHTML = html;
}

function switchProductTab(tab) {
    document.querySelectorAll('.product-tab').forEach(t => { t.classList.remove('active'); });
    document.querySelectorAll('.product-tab-content').forEach(t => { t.classList.remove('active'); });
    document.querySelector('.product-tab[data-tab="' + tab + '"]').classList.add('active');
    document.getElementById('tab-' + tab).classList.add('active');
}

function updatePriceDisplay() {
    const basePrice = parseFloat(document.getElementById('detail-price-base').value) || 0;
    const purchasePrice = currentProductDetail ? (currentProductDetail.purchase_price_avg || 0) : 0;
    const margin = basePrice > 0 ? ((basePrice - purchasePrice) / basePrice * 100).toFixed(1) : 0;
    document.getElementById('detail-selling-price').textContent = basePrice.toFixed(2) + ' DH';
    document.getElementById('detail-margin').textContent = margin + '%';
}

function showPriceSaveButton() {
    document.getElementById('btn-save-prices').style.display = 'inline-flex';
    document.getElementById('price-save-status').style.display = 'none';
}

function hidePriceSaveButton() {
    document.getElementById('btn-save-prices').style.display = 'none';
    document.getElementById('price-save-status').style.display = 'none';
}

function showPriceSaved() {
    document.getElementById('btn-save-prices').style.display = 'none';
    document.getElementById('price-save-status').style.display = 'inline';
    setTimeout(() => { document.getElementById('price-save-status').style.display = 'none'; }, 3000);
}

async function saveProductPrices() {
    if (!currentProductDetail) return;
    const priceBase = parseFloat(document.getElementById('detail-price-base').value) || 0;
    const priceLoyal = parseFloat(document.getElementById('detail-price-loyal').value) || 0;
    const priceGros = parseFloat(document.getElementById('detail-price-gros').value) || 0;

    const customPriceEls = document.querySelectorAll('#custom-price-tiers .custom-tier-row');
    const extraPrices = [];
    customPriceEls.forEach(row => {
        const label = row.querySelector('.custom-tier-label').value.trim();
        const val = parseFloat(row.querySelector('.custom-tier-value').value) || 0;
        if (label) extraPrices.push({label, price: val});
    });

    try {
        const res = await fetch('/api/products/' + currentProductDetail.id, {
            method: 'PUT',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                name: currentProductDetail.name,
                description: currentProductDetail.description || '',
                sku: currentProductDetail.sku || '',
                barcode: currentProductDetail.barcode || '',
                quantity: currentProductDetail.quantity,
                min_quantity: currentProductDetail.min_quantity,
                max_quantity: currentProductDetail.max_quantity,
                price: priceBase,
                price_base: priceBase,
                price_loyal: priceLoyal,
                price_gros: priceGros,
                tax_category: currentProductDetail.tax_category || '20',
                lot_number: currentProductDetail.lot_number || '',
                serial_number: currentProductDetail.serial_number || '',
                expiry_date: currentProductDetail.expiry_date || '',
                supplier_id: currentProductDetail.supplier_id,
                category: currentProductDetail.category || 'Général',
                warehouse_id: currentProductDetail.warehouse_id || 1,
                location_id: currentProductDetail.location_id,
                extra_prices: extraPrices
            })
        });
        const data = await res.json();
        if (data.success) {
            currentProductDetail.price_base = priceBase;
            currentProductDetail.price_loyal = priceLoyal;
            currentProductDetail.price_gros = priceGros;
            currentProductDetail.price = data.price;
            showPriceSaved();
            loadProducts();
        } else {
            showError('Erreur lors de l\'enregistrement');
        }
    } catch(e) {
        showError('Erreur lors de l\'enregistrement des prix');
    }
}

function renderCustomPriceTiers(extraPrices) {
    const container = document.getElementById('custom-price-tiers');
    if (!container) return;
    if (!extraPrices || extraPrices.length === 0) {
        container.innerHTML = '';
        return;
    }
    let html = '<div class="discount-table">';
    for (let i = 0; i < extraPrices.length; i++) {
        const ep = extraPrices[i];
        html += '<div class="detail-row custom-tier-row">' +
            '<span class="detail-label"><input type="text" class="form-input custom-tier-label" value="' + escapeHtml(ep.label) + '" style="width:140px;" placeholder="Libellé"></span>' +
            '<span class="detail-value">' +
            '<input type="number" class="form-input custom-tier-value" step="0.01" value="' + (ep.price || 0).toFixed(2) + '" style="width:100px;">' +
            ' <button class="btn btn-sm btn-danger" onclick="removeCustomPriceTier(this)" title="Supprimer"><i class="fas fa-times"></i></button>' +
            '</span></div>';
    }
    html += '</div>';
    container.innerHTML = html;
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', attachPriceInputListeners);
} else {
    attachPriceInputListeners();
}
}

function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

function addCustomPriceTier() {
    const container = document.getElementById('custom-price-tiers');
    const inner = container.querySelector('.discount-table') || container;
    if (!inner.querySelector('.discount-table')) {
        const table = document.createElement('div');
        table.className = 'discount-table';
        container.innerHTML = '';
        container.appendChild(table);
    }
    const table = container.querySelector('.discount-table') || inner;
    const row = document.createElement('div');
    row.className = 'detail-row custom-tier-row';
    row.innerHTML = '<span class="detail-label"><input type="text" class="form-input custom-tier-label" style="width:140px;" placeholder="Libellé"></span>' +
        '<span class="detail-value"><input type="number" class="form-input custom-tier-value" step="0.01" value="0.00" style="width:100px;">' +
        ' <button class="btn btn-sm btn-danger" onclick="removeCustomPriceTier(this)" title="Supprimer"><i class="fas fa-times"></i></button></span>';
    table.appendChild(row);
    showPriceSaveButton();
}

function removeCustomPriceTier(btn) {
    const row = btn.closest('.custom-tier-row');
    if (row) {
        row.remove();
        const table = document.getElementById('custom-price-tiers').querySelector('.discount-table');
        if (table && table.children.length === 0) {
            document.getElementById('custom-price-tiers').innerHTML = '';
        }
        showPriceSaveButton();
    }
}

function attachPriceInputListeners() {
    document.querySelectorAll('.price-tier-input, .custom-tier-value, .custom-tier-label').forEach(el => {
        el.removeEventListener('input', onPriceInputChange);
        el.addEventListener('input', onPriceInputChange);
    });
}

function onPriceInputChange() {
    updatePriceDisplay();
    showPriceSaveButton();
}

attachPriceInputListeners();

function openProductEditFromDetail() {
    if (currentProductDetail) {
        closeModal('productDetailModal');
        editProduct(currentProductDetail.id);
    }
}

function openProductPage() {
    if (currentProductDetail) {
        window.open('/product/' + currentProductDetail.id, '_blank');
    }
}

function openStockInFromDetail() {
    if (currentProductDetail) {
        closeModal('productDetailModal');
        openStockModal(currentProductDetail.id, 'in');
    }
}

function openStockOutFromDetail() {
    if (currentProductDetail) {
        closeModal('productDetailModal');
        openStockModal(currentProductDetail.id, 'out');
    }
}

function createOrderFromDetail() {
    closeModal('productDetailModal');
    showTab('orders');
    openOrderModal(null);

    if (currentProductDetail) {
        if (currentProductDetail.supplier_id) {
            document.getElementById('orderSupplier').value = currentProductDetail.supplier_id;
        }

        const tbody = document.getElementById('orderItemsBody');
        tbody.innerHTML = '';

        const row = document.createElement('tr');
        row.className = 'order-item-row';
        row.innerHTML = '<td><div class="product-search-wrapper"><input type="text" class="form-input product-search-input" placeholder="Rechercher un produit..." oninput="filterProductSuggestions(this)" onfocus="filterProductSuggestions(this)" autocomplete="off"><div class="product-suggestions"></div><input type="hidden" class="order-product-id"></div></td>' +
            '<td><input type="number" class="form-input order-qty" value="1" min="1" onchange="updateOrderItemTotal(this)"></td>' +
            '<td><input type="number" class="form-input order-price" step="0.01" value="' + (currentProductDetail.purchase_price_avg || currentProductDetail.price || 0) + '" onchange="updateOrderItemTotal(this)"></td>' +
            '<td class="order-item-total">0.00 DH</td>' +
            '<td><button type="button" class="btn btn-sm btn-danger" onclick="removeOrderItem(this)"><i class="fas fa-trash"></i></button></td>';
        tbody.appendChild(row);

        const firstRow = document.querySelector('#orderItemsBody .order-item-row');
        if (firstRow && currentProductDetail) {
            const hiddenId = firstRow.querySelector('.order-product-id');
            const input = firstRow.querySelector('.product-search-input');
            if (hiddenId) {
                hiddenId.value = currentProductDetail.id;
            }
            if (input) {
                input.value = currentProductDetail.name;
                updateOrderItemTotal(input);
            }
        }
    }
}

function openStockModal(productId, type) {
    document.getElementById('stockProductId').value = productId;
    document.getElementById('stockType').value = type;
    const titleEl = document.getElementById('stockTitle');
    if (titleEl) titleEl.textContent = type === 'in' ? 'Entree Stock' : 'Sortie Stock';
    openModal('stockModal');
}

async function saveStock(e) {
    e.preventDefault();
    const productId = document.getElementById('stockProductId').value;
    const type = document.getElementById('stockType').value;
    const qty = parseInt(document.getElementById('stockQuantity').value) || 1;
    const purchasePrice = parseFloat(document.getElementById('stockPurchasePrice').value) || 0;
    const note = document.getElementById('stockNote').value;

    try {
        await fetch('/api/stock/' + productId, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ type: type, quantity: qty, purchase_price: purchasePrice, note: note })
        });
        closeModal('stockModal');
        loadProducts();
        if (currentProductDetail && currentProductDetail.id == productId) {
            openProductDetail(productId);
        }
    } catch(e) {
        showError('Erreur lors de l\'enregistrement du stock');
    }
}

function editProduct(id) {
    const p = products.find(p => p.id === id);
    if (p) editProductModal(p);
}

async function loadWarehouses() {
    try {
        const res = await fetch('/api/warehouses');
        warehouses = await res.json();
        renderWarehouses();
    } catch(e) {
        showError('Erreur lors du chargement des entrepôts');
    }
}

function renderWarehouses() {
    const tbody = document.getElementById('warehousesTable');
    if (!tbody) return;
    if (warehouses.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4">Aucun entrepôt</td></tr>';
        return;
    }
    let html = '';
    for (let i = 0; i < warehouses.length; i++) {
        const w = warehouses[i];
        html += '<tr><td>' + w.name + '</td><td>' + (w.is_default ? '<span class="badge badge-primary">Par défaut</span>' : '-') + '</td><td>' + (w.address || '-') + '</td><td>' + (w.manager || '-') + '</td><td><button class="btn btn-sm" onclick="editWarehouse(' + w.id + ')">✎</button></td></tr>';
    }
    tbody.innerHTML = html;
}

function editWarehouse(id) {
    const w = warehouses.find(w => w.id === id);
    if (!w) return;
    document.getElementById('warehouseId').value = w.id;
    document.getElementById('warehouseName').value = w.name;
    document.getElementById('warehouseAddress').value = w.address || '';
    document.getElementById('warehouseManager').value = w.manager || '';
    document.getElementById('warehouseModalTitle').textContent = 'Modifier Entrepôt';
    document.getElementById('warehouseModal').style.display = 'flex';
}

function openWarehouseModal() {
    document.getElementById('warehouseId').value = '';
    document.getElementById('warehouseName').value = '';
    document.getElementById('warehouseAddress').value = '';
    document.getElementById('warehouseManager').value = '';
    document.getElementById('warehouseModalTitle').textContent = 'Nouvel Entrepôt';
    document.getElementById('warehouseModal').style.display = 'flex';
}

function openLocationModal() {
    document.getElementById('locationId').value = '';
    document.getElementById('locationName').value = '';
    document.getElementById('locationType').value = 'rack';
    document.getElementById('locationCapacity').value = '';
    document.getElementById('locationModalTitle').textContent = 'Nouvelle Zone';
    const select = document.getElementById('locationWarehouse');
    select.innerHTML = '';
    warehouses.forEach(w => {
        select.innerHTML += '<option value="' + w.id + '">' + w.name + '</option>';
    });
    document.getElementById('locationModal').style.display = 'flex';
}

function openProductModal() {
    document.getElementById('productId').value = '';
    document.getElementById('productForm').reset();
    document.getElementById('productModalTitle').textContent = 'Nouveau Produit';
    document.getElementById('productImagePreview').src = '/static/img/no-image.png';
    document.getElementById('productImageClearBtn').style.display = 'none';
    openModal('barcodeScannerModal');
    stopAddProductScanner();
    document.getElementById('addProductScannerStatus').textContent = 'Cliquez sur "Activer le scan" pour utiliser la caméra';
    var btn = document.getElementById('toggleAddProductScannerBtn');
    if (btn) btn.innerHTML = '<i class="fas fa-camera"></i> Activer le scan';
}

function clearProductImage() {
    document.getElementById('productImageInput').value = '';
    document.getElementById('productImagePreview').src = '/static/img/no-image.png';
    document.getElementById('productImageClearBtn').style.display = 'none';
}

document.addEventListener('DOMContentLoaded', function() {
    var imgInput = document.getElementById('productImageInput');
    if (imgInput) {
        imgInput.addEventListener('change', function(e) {
            var file = e.target.files[0];
            if (file) {
                var reader = new FileReader();
                reader.onload = function(ev) {
                    document.getElementById('productImagePreview').src = ev.target.result;
                    document.getElementById('productImageClearBtn').style.display = '';
                };
                reader.readAsDataURL(file);
            }
        });
    }
});

function cancelBarcodeScan() {
    stopAddProductScanner();
    closeModal('barcodeScannerModal');
}

var addProductScannerStream = null;
var addProductScannerTid = null;

function initAddProductScanner() {
    var wrapper = document.getElementById('addProductScannerVideoWrapper');
    if (wrapper) wrapper.style.display = '';
    var video = document.getElementById('addProductVideo');
    var status = document.getElementById('addProductScannerStatus');
    if (!video) return;
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        status.textContent = 'Caméra non disponible. Saisissez le code-barres manuellement.';
        return;
    }
    navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } }).then(function(stream) {
        addProductScannerStream = stream;
        video.srcObject = stream;
        video.play();
        status.textContent = 'Positionnez le code-barres devant la caméra';
        var btn = document.getElementById('toggleAddProductScannerBtn');
        if (btn) btn.innerHTML = '<i class="fas fa-camera"></i> Désactiver le scan';
        if ('BarcodeDetector' in window) {
            startBarcodeDetectLoop();
        } else {
            status.textContent = 'Scanner auto non supporté. Saisissez le code-barres manuellement.';
        }
    }).catch(function() {
        status.textContent = 'Caméra non disponible. Saisissez le code-barres manuellement.';
    });
}

function startBarcodeDetectLoop() {
    var detector = new BarcodeDetector({ formats: ['ean_13', 'ean_8', 'upc_a', 'upc_e', 'code_128', 'code_39', 'code_93', 'codabar', 'itf', 'qr_code', 'data_matrix', 'pdf417'] });
    var video = document.getElementById('addProductVideo');
    var status = document.getElementById('addProductScannerStatus');

    function detect() {
        if (!video || video.readyState < 2) { addProductScannerTid = setTimeout(detect, 500); return; }
        detector.detect(video).then(function(barcodes) {
            if (barcodes.length > 0) {
                var code = barcodes[0].rawValue;
                status.textContent = 'Code détecté: ' + code;
                onBarcodeScanned(code);
                return;
            }
            addProductScannerTid = setTimeout(detect, 300);
        }).catch(function() {
            addProductScannerTid = setTimeout(detect, 300);
        });
    }
    detect();
}

function stopAddProductScanner() {
    if (addProductScannerTid) { clearTimeout(addProductScannerTid); addProductScannerTid = null; }
    if (addProductScannerStream) {
        addProductScannerStream.getTracks().forEach(function(t) { t.stop(); });
        addProductScannerStream = null;
    }
    var video = document.getElementById('addProductVideo');
    if (video) video.srcObject = null;
    var wrapper = document.getElementById('addProductScannerVideoWrapper');
    if (wrapper) wrapper.style.display = 'none';
}

function toggleAddProductScanner() {
    var btn = document.getElementById('toggleAddProductScannerBtn');
    var status = document.getElementById('addProductScannerStatus');
    if (addProductScannerStream) {
        stopAddProductScanner();
        if (btn) btn.innerHTML = '<i class="fas fa-camera"></i> Activer le scan';
        if (status) status.textContent = 'Cliquez sur "Activer le scan" pour utiliser la caméra';
    } else {
        initAddProductScanner();
        if (btn) btn.innerHTML = '<i class="fas fa-camera"></i> Désactiver le scan';
    }
}

// === Barcode Scanner Buffer (physical scanner) ===
// Uses event.code (QWERTY key position) instead of event.key or .value
// to bypass OS keyboard layout mapping (scanner envoie keycodes US, OS peut être AZERTY)
var _scanBuf = '';
var _scanTimer = null;

function _flushScan(action) {
    if (_scanTimer) { clearTimeout(_scanTimer); _scanTimer = null; }
    if (!_scanBuf) return;
    var code = _scanBuf;
    _scanBuf = '';
    var inp = document.getElementById('addProductBarcodeInput');
    if (inp) inp.value = '';
    if (action === 'add-product') {
        checkBarcodeExists(code);
    }
}

document.addEventListener('keydown', function _scannerKeydown(e) {
    var el = e.target.closest('[data-scanner]');
    if (!el) return;
    var action = el.getAttribute('data-scanner');
    var ch = null;

    if (e.code.startsWith('Digit')) {
        ch = e.code.slice(5);
    } else if (e.code.startsWith('Numpad')) {
        var s = e.code.slice(6);
        if (s === 'Decimal') ch = '.';
        else if (s === 'Add') ch = '+';
        else if (s === 'Subtract') ch = '-';
        else ch = s;
    } else if (e.code.startsWith('Key')) {
        ch = e.code.charAt(3);
    } else if (e.code === 'Minus') {
        ch = '-';
    } else if (e.code === 'Enter') {
        e.preventDefault();
        _flushScan(action);
        return;
    } else if (e.code === 'Tab') {
        _flushScan(action);
        return;
    } else if (e.code === 'Backspace') {
        if (_scanBuf.length > 0) {
            e.preventDefault();
            _scanBuf = _scanBuf.slice(0, -1);
            el.value = _scanBuf;
        }
        return;
    }
    if (ch === null) return;

    e.preventDefault();
    _scanBuf += ch;
    el.value = _scanBuf;

    if (_scanTimer) clearTimeout(_scanTimer);
    _scanTimer = setTimeout(function () {
        if (_scanBuf.length >= 4) _flushScan(action);
    }, 150);
});

function onBarcodeEntered() {
    if (_scanTimer) { clearTimeout(_scanTimer); _scanTimer = null; }
    if (_scanBuf) {
        var code = _scanBuf;
        _scanBuf = '';
        var inp = document.getElementById('addProductBarcodeInput');
        if (inp) inp.value = '';
        onBarcodeScanned(code);
        return;
    }
    var input = document.getElementById('addProductBarcodeInput');
    var code = input.value.replace(/[^\x20-\x7E]/g, '').trim();
    if (!code) return;
    onBarcodeScanned(code);
}

function onBarcodeScanned(code) {
    stopAddProductScanner();
    closeModal('barcodeScannerModal');
    document.getElementById('addProductBarcodeInput').value = '';
    checkBarcodeExists(code);
}

var scannedBarcode = null;

async function checkBarcodeExists(code) {
    scannedBarcode = code;
    try {
        var res = await fetch('/api/products/for-sale?search=' + encodeURIComponent(code));
        var products = await res.json();
        if (products && products.length > 0) {
            var p = products[0];
            if (confirm('Le code-barres "' + code + '" est déjà utilisé par "' + p.name + '".\nVoulez-vous modifier ce produit existant ?')) {
                openProductDetail(p.id);
            } else {
                openModal('barcodeScannerModal');
                var btn = document.getElementById('toggleAddProductScannerBtn');
                if (btn) btn.innerHTML = '<i class="fas fa-camera"></i> Activer le scan';
                document.getElementById('addProductScannerStatus').textContent = 'Cliquez sur "Activer le scan" pour utiliser la caméra';
            }
        } else {
            openModal('barcodeUnknownModal');
        }
    } catch(e) {
        console.error(e);
        openModal('barcodeUnknownModal');
    }
}

function addUnknownBarcodeProduct() {
    closeModal('barcodeUnknownModal');
    document.getElementById('productBarcode').value = scannedBarcode || '';
    openModal('productModal');
}

function cancelUnknownBarcode() {
    closeModal('barcodeUnknownModal');
    openModal('barcodeScannerModal');
    var btn = document.getElementById('toggleAddProductScannerBtn');
    if (btn) btn.innerHTML = '<i class="fas fa-camera"></i> Activer le scan';
    document.getElementById('addProductScannerStatus').textContent = 'Cliquez sur "Activer le scan" pour utiliser la caméra';
}

function editProductModal(product) {
    document.getElementById('productId').value = product.id;
    document.getElementById('productName').value = product.name;
    document.getElementById('productDescription').value = product.description || '';
    document.getElementById('productSku').value = product.sku || '';
    document.getElementById('productBarcode').value = product.barcode || '';
    document.getElementById('productPurchasePrice').value = product.purchase_price_avg || 0;
    document.getElementById('productPrice').value = product.price || 0;
    document.getElementById('productLoyalPrice').value = product.price_loyal || 0;
    document.getElementById('productGrosPrice').value = product.price_gros || 0;
    document.getElementById('productQuantity').value = product.quantity || 0;
    document.getElementById('productCategory').value = product.category || 'Général';
    document.getElementById('productMinQty').value = product.min_quantity || 5;
    document.getElementById('productMaxQty').value = product.max_quantity || 100;
    document.getElementById('productModalTitle').textContent = 'Modifier Produit';
    var imgPreview = document.getElementById('productImagePreview');
    if (imgPreview) {
        imgPreview.src = product.image_url || '/static/img/no-image.png';
        document.getElementById('productImageClearBtn').style.display = product.image_url ? '' : 'none';
    }
    openModal('productModal');
}

async function saveProduct(e) {
    e.preventDefault();
    const id = document.getElementById('productId').value;
    var priceNormal = parseFloat(document.getElementById('productPrice').value) || 0;
    var pricePurchase = parseFloat(document.getElementById('productPurchasePrice').value) || 0;
    var priceLoyal = parseFloat(document.getElementById('productLoyalPrice').value) || 0;
    var priceGros = parseFloat(document.getElementById('productGrosPrice').value) || 0;
    var data = {
        name: document.getElementById('productName').value,
        description: document.getElementById('productDescription').value,
        sku: document.getElementById('productSku').value,
        barcode: document.getElementById('productBarcode').value,
        purchase_price_avg: pricePurchase,
        price: priceNormal,
        price_base: priceNormal,
        price_loyal: priceLoyal,
        price_gros: priceGros,
        quantity: parseInt(document.getElementById('productQuantity').value) || 0,
        category: document.getElementById('productCategory').value,
        min_quantity: parseInt(document.getElementById('productMinQty').value) || 5,
        max_quantity: parseInt(document.getElementById('productMaxQty').value) || 100
    };
    var imgInput = document.getElementById('productImageInput');
    var currentPreview = document.getElementById('productImagePreview').src;
    if (imgInput && imgInput.files && imgInput.files[0]) {
        var formData = new FormData();
        formData.append('file', imgInput.files[0]);
        try {
            var uploadRes = await fetch('/api/upload', { method: 'POST', body: formData });
            var uploadResult = await uploadRes.json();
            if (uploadResult.url) {
                data.image_url = uploadResult.url;
            }
        } catch(e) {
            console.error('Upload failed:', e);
        }
    } else if (currentPreview && currentPreview.indexOf('/static/uploads/') !== -1) {
        data.image_url = currentPreview;
    } else {
        data.image_url = null;
    }
    try {
        const method = id ? 'PUT' : 'POST';
        const url = id ? '/api/products/' + id : '/api/products';
        const res = await fetch(url, {
            method: method,
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(data)
        });
        const result = await res.json();
        if (result.success) {
            closeModal('productModal');
            loadProducts();
            showSuccess(id ? 'Produit modifié avec succès' : 'Produit créé avec succès');
        } else {
            showError(result.error || 'Erreur lors de l\'enregistrement');
        }
    } catch(e) {
        showError('Erreur lors de l\'enregistrement du produit');
    }
}

function confirmDeleteProduct(productId, productName) {
    if (!confirm('Supprimer définitivement "' + productName + '" ?\n\nCette action va :\n- Détruire le stock restant\n- Annuler les commandes brouillon\n- Retourner le stock des commandes reçues\n- Rembourser le compte principal pour les commandes payées\n- Annuler les factures impayées liées\n\nCette action est irréversible.')) return;
    deleteProduct(productId);
}

function deleteProductFromDetail() {
    if (!currentProductDetail) return;
    confirmDeleteProduct(currentProductDetail.id, currentProductDetail.name);
}

async function deleteProduct(productId) {
    try {
        const res = await fetch('/api/products/' + productId, { method: 'DELETE' });
        const data = await res.json();
        if (data.success) {
            showSuccess(data.message || 'Produit supprimé');
            if (data.details) {
                const detailStr = data.details.join('\\n');
                showSuccess('Détails : ' + detailStr);
            }
            closeModal('productDetailModal');
            loadProducts();
        } else {
            showError(data.error || 'Erreur lors de la suppression');
        }
    } catch(e) {
        showError('Erreur lors de la suppression du produit');
    }
}

async function loadLocations() {
    try {
        const res = await fetch('/api/locations?warehouse_id=' + (currentWarehouse || 1));
        locations = await res.json();
        renderLocations();
    } catch(e) {
        showError('Erreur lors du chargement des zones');
    }
}

function renderLocations() {
    const tbody = document.getElementById('locationsTable');
    if (!tbody) return;
    if (locations.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4">Aucune zone</td></tr>';
        return;
    }
    let html = '';
    for (let i = 0; i < locations.length; i++) {
        const l = locations[i];
        html += '<tr><td>' + l.name + '</td><td>' + (l.type || '-') + '</td><td>' + (l.capacity || '-') + '</td><td><button class="btn btn-sm" onclick="editLocation(' + l.id + ')">✎</button></td></tr>';
    }
    tbody.innerHTML = html;
}

function editLocation(id) {
    const l = locations.find(l => l.id === id);
    if (!l) return;
    document.getElementById('locationId').value = l.id;
    document.getElementById('locationName').value = l.name;
    document.getElementById('locationType').value = l.type || 'rack';
    document.getElementById('locationCapacity').value = l.capacity || '';
    document.getElementById('locationModalTitle').textContent = 'Modifier Zone';
    const select = document.getElementById('locationWarehouse');
    select.innerHTML = '';
    warehouses.forEach(w => {
        select.innerHTML += '<option value="' + w.id + '"' + (w.id === l.warehouse_id ? ' selected' : '') + '>' + w.name + '</option>';
    });
    document.getElementById('locationModal').style.display = 'flex';
}
