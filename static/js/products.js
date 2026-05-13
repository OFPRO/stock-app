async function loadProducts() {
    var showArchived = document.getElementById('showArchivedProducts') ? document.getElementById('showArchivedProducts').checked : false;
    var url = '/api/products';
    if (showArchived) url += '?include_archived=true';
    var res = await fetch(url);
    products = await res.json();
    renderProducts();
    loadBestSellers();
}

async function loadBestSellers() {
    var res = await fetch('/api/pos/best-sellers?limit=5');
    var bestSellers = await res.json();
    var container = document.getElementById('bestSellers');
    if (!container || !bestSellers || bestSellers.length === 0) return;
    var html = bestSellers.map(function(p) {
        return '<span class="pos-best-seller-tag" onclick="addPosProduct(' + p.id + ')">' + p.name + '</span>';
    }).join('');
    container.innerHTML = '<span style="font-size:0.75rem;color:var(--text-light);margin-right:0.5rem;">Les plus vendus:</span>' + html;
}

function renderProducts(filter) {
    var container = document.getElementById('productsListView');
    if (!container) return;
    filter = filter || '';
    var filtered = products.filter(function(p) {
        return p.name.toLowerCase().indexOf(filter.toLowerCase()) !== -1 || (p.sku && p.sku.toLowerCase().indexOf(filter.toLowerCase()) !== -1);
    });
    if (filtered.length === 0) {
        container.innerHTML = '<div class="empty">Aucun produit</div>';
        return;
    }
    var html = '<table class="table"><thead><tr><th>Produit</th><th>SKU</th><th>Qte</th><th>Prix</th><th>Actions</th></tr></thead><tbody>';
    for (var i = 0; i < filtered.length; i++) {
        var p = filtered[i];
        var isDeleted = p.is_deleted == 1;
        var rowClass = isDeleted ? ' style="opacity:0.5;text-decoration:line-through;"' : '';
        var statusClass = p.quantity <= p.min_quantity ? 'badge-danger' : (p.quantity <= p.min_quantity * 1.5 ? 'badge-warning' : 'badge-success');
        html += '<tr' + rowClass + '>';
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

var currentProductDetail = null;

async function openProductDetail(productId) {
    try {
        var res = await fetch('/api/products/' + productId);
        var data = await res.json();
        currentProductDetail = data.product;
        
        var p = data.product;
        document.getElementById('productDetailTitle').textContent = p.name;
        document.getElementById('productDetailSku').textContent = 'SKU: ' + (p.sku || '-') + ' | ' + (p.barcode || 'Sans barcode');
        document.getElementById('productDetailQty').textContent = p.quantity;
        
        var stockBadge = document.getElementById('productDetailStockBadge');
        stockBadge.className = 'product-stock-badge';
        if (p.quantity <= 0) stockBadge.classList.add('danger');
        else if (p.quantity <= p.min_quantity) stockBadge.classList.add('danger');
        else if (p.quantity <= p.min_quantity * 1.5) stockBadge.classList.add('warning');
        else stockBadge.classList.add('success');
        
        document.getElementById('productDetailPrice').textContent = (p.price || 0).toFixed(2) + ' DH';
        var statusEl = document.getElementById('productDetailStatus');
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
        document.getElementById('detail-category').textContent = p.category || '-';
        document.getElementById('detail-barcode').textContent = p.barcode || '-';
        document.getElementById('detail-warehouse').textContent = p.warehouse_name || '-';
        document.getElementById('detail-location').textContent = p.location_name || '-';
        document.getElementById('detail-qty').textContent = p.quantity;
        document.getElementById('detail-min-qty').textContent = p.min_quantity;
        document.getElementById('detail-max-qty').textContent = p.max_quantity;
        document.getElementById('detail-expiry').textContent = p.expiry_date || '-';
        document.getElementById('detail-lot').textContent = p.lot_number || '-';
        
        document.getElementById('detail-selling-price').textContent = (p.price || 0).toFixed(2) + ' DH';
        document.getElementById('detail-purchase-price').textContent = (p.purchase_price_avg || 0).toFixed(2) + ' DH';
        document.getElementById('detail-margin').textContent = (p.margin_percent || 0) + '%';
        document.getElementById('detail-discount').textContent = (p.discount_rate || 0) + '%';
        document.getElementById('detail-discount-category').textContent = p.discount_category || '-';
        document.getElementById('detail-discount-rate').textContent = (p.discount_rate || 0) + '%';
        document.getElementById('detail-calculated-price').textContent = (p.calculated_price || 0).toFixed(2) + ' DH';
        document.getElementById('detail-tax').textContent = p.tax_category || '-';
        
        document.getElementById('detail-price-student').value = (p.price_student || 0).toFixed(2);
        document.getElementById('detail-price-school').value = (p.price_school || 0).toFixed(2);
        document.getElementById('detail-price-loyal').value = (p.price_loyal || 0).toFixed(2);
        document.getElementById('detail-price-base').value = (p.price_base || 0).toFixed(2);
        
        document.getElementById('detail-supplier-name').textContent = p.supplier_name || '-';
        document.getElementById('detail-supplier-email').textContent = p.supplier_email || '-';
        document.getElementById('detail-supplier-phone').textContent = p.supplier_phone || '-';
        
        var stats = data.purchase_stats;
        document.getElementById('detail-total-purchases').textContent = (stats && stats.total_purchases) || 0;
        document.getElementById('detail-total-purchase-qty').textContent = (stats && stats.total_qty) || 0;
        var sales = data.sales_stats;
        document.getElementById('detail-total-sales').textContent = (sales && sales.total_sales) || 0;
        document.getElementById('detail-total-sale-qty').textContent = (sales && sales.total_qty) || 0;
        
        var locationsHtml = '';
        if (data.stock_locations && data.stock_locations.length > 0) {
            for (var i = 0; i < data.stock_locations.length; i++) {
                var loc = data.stock_locations[i];
                locationsHtml += '<div class="stock-location-item"><span class="location-name"><i class="fas fa-map-marker"></i> ' + loc.location_name + '</span><span class="location-qty">' + loc.quantity + '</span></div>';
            }
        } else {
            locationsHtml = '<div class="empty">Aucun stock par emplacement</div>';
        }
        document.getElementById('detail-stock-locations').innerHTML = locationsHtml;
        
        loadProductMovementsList(data.movements);
        
        switchProductTab('overview');
        document.getElementById('productDetailModal').classList.add('active');
        
    } catch(e) {
        console.error('Error loading product:', e);
        showError('Erreur chargement produit');
    }
}

function loadProductMovementsList(movements) {
    var html = '';
    if (movements && movements.length > 0) {
        for (var i = 0; i < movements.length; i++) {
            var m = movements[i];
            var typeClass = m.type === 'in' ? 'success' : 'warning';
            var icon = m.type === 'in' ? 'fa-arrow-down' : 'fa-arrow-up';
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
    document.querySelectorAll('.product-tab').forEach(function(t) { t.classList.remove('active'); });
    document.querySelectorAll('.product-tab-content').forEach(function(t) { t.classList.remove('active'); });
    document.querySelector('.product-tab[data-tab="' + tab + '"]').classList.add('active');
    document.getElementById('tab-' + tab).classList.add('active');
}

async function calculateProductPrices() {
    if (!currentProductDetail) return;
    try {
        var res = await fetch('/api/products/calculate-prices', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({product_id: currentProductDetail.id})
        });
        var data = await res.json();
        if (data.success && data.prices) {
            document.getElementById('detail-price-student').value = data.prices.price_student.toFixed(2);
            document.getElementById('detail-price-school').value = data.prices.price_school.toFixed(2);
            document.getElementById('detail-price-loyal').value = data.prices.price_loyal.toFixed(2);
            document.getElementById('detail-price-base').value = data.prices.price_base.toFixed(2);
            currentProductDetail.price_student = data.prices.price_student;
            currentProductDetail.price_school = data.prices.price_school;
            currentProductDetail.price_loyal = data.prices.price_loyal;
            currentProductDetail.price_base = data.prices.price_base;
            showSuccess('Prix calculés et enregistrés');
        } else if (data.error) {
            showError(data.error);
        }
    } catch(e) {
        showError('Erreur lors du calcul des prix');
    }
}

async function updateProductPrice(field, value) {
    if (!currentProductDetail) return;
    var priceValue = parseFloat(value) || 0;
    currentProductDetail[field] = priceValue;
    try {
        var res = await fetch('/api/products/' + currentProductDetail.id, {
            method: 'PUT',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(currentProductDetail)
        });
        var data = await res.json();
        if (data.success) {
            currentProductDetail[field] = priceValue;
        }
    } catch(e) {
        console.error('Error saving price:', e);
    }
}

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
        
        var tbody = document.getElementById('orderItemsBody');
        tbody.innerHTML = '';
        
        var row = document.createElement('tr');
        row.className = 'order-item-row';
row.innerHTML = '<td><div class="product-search-wrapper"><input type="text" class="form-input product-search-input" placeholder="Rechercher un produit..." oninput="filterProductSuggestions(this)" onfocus="filterProductSuggestions(this)" autocomplete="off"><div class="product-suggestions"></div><input type="hidden" class="order-product-id"></div></td>' +
            '<td><input type="number" class="form-input order-qty" value="1" min="1" onchange="updateOrderItemTotal(this)"></td>' +
            '<td><input type="number" class="form-input order-price" step="0.01" value="' + (currentProductDetail.purchase_price_avg || currentProductDetail.price || 0) + '" onchange="updateOrderItemTotal(this)"></td>' +
            '<td class="order-item-total">0.00 DH</td>' +
            '<td><button type="button" class="btn btn-sm btn-danger" onclick="removeOrderItem(this)"><i class="fas fa-trash"></i></button></td>';
        tbody.appendChild(row);
        
        // populateProductSelects() is empty, skip it
        var firstRow = document.querySelector('#orderItemsBody .order-item-row');
        if (firstRow && currentProductDetail) {
            var hiddenId = firstRow.querySelector('.order-product-id');
            var input = firstRow.querySelector('.product-search-input');
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
    var titleEl = document.getElementById('stockTitle');
    if (titleEl) titleEl.textContent = type === 'in' ? 'Entree Stock' : 'Sortie Stock';
    document.getElementById('stockModal').classList.add('active');
}

async function saveStock(e) {
    e.preventDefault();
    var productId = document.getElementById('stockProductId').value;
    var type = document.getElementById('stockType').value;
    var qty = parseInt(document.getElementById('stockQuantity').value) || 1;
    var purchasePrice = parseFloat(document.getElementById('stockPurchasePrice').value) || 0;
    var note = document.getElementById('stockNote').value;
    
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
}

function editProduct(id) {
    var p = products.find(function(p) { return p.id === id; });
    if (p) editProductModal(p);
}

async function loadWarehouses() {
    var res = await fetch('/api/warehouses');
    warehouses = await res.json();
    renderWarehouses();
}

function renderWarehouses() {
    var tbody = document.getElementById('warehousesTable');
    if (!tbody) return;
    if (warehouses.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4">Aucun entrepôt</td></tr>';
        return;
    }
    var html = '';
    for (var i = 0; i < warehouses.length; i++) {
        var w = warehouses[i];
        html += '<tr><td>' + w.name + '</td><td>' + (w.is_default ? '<span class="badge badge-primary">Par défaut</span>' : '-') + '</td><td>' + (w.address || '-') + '</td><td>' + (w.manager || '-') + '</td><td><button class="btn btn-sm" onclick="editWarehouse(' + w.id + ')">✎</button></td></tr>';
    }
    tbody.innerHTML = html;
}

function editWarehouse(id) {
    var w = warehouses.find(function(w) { return w.id === id; });
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
    var select = document.getElementById('locationWarehouse');
    select.innerHTML = '';
    warehouses.forEach(function(w) {
        select.innerHTML += '<option value="' + w.id + '">' + w.name + '</option>';
    });
    document.getElementById('locationModal').style.display = 'flex';
}

function openProductModal() {
    document.getElementById('productId').value = '';
    document.getElementById('productForm').reset();
    document.getElementById('productModalTitle').textContent = 'Nouveau Produit';
    document.getElementById('productModal').classList.add('active');
}

function editProductModal(product) {
    document.getElementById('productId').value = product.id;
    document.getElementById('productName').value = product.name;
    document.getElementById('productDescription').value = product.description || '';
    document.getElementById('productSku').value = product.sku || '';
    document.getElementById('productBarcode').value = product.barcode || '';
    document.getElementById('productPrice').value = product.price || 0;
    document.getElementById('productQuantity').value = product.quantity || 0;
    document.getElementById('productCategory').value = product.category || 'Général';
    document.getElementById('productMinQty').value = product.min_quantity || 5;
    document.getElementById('productMaxQty').value = product.max_quantity || 100;
    document.getElementById('productModalTitle').textContent = 'Modifier Produit';
    document.getElementById('productModal').classList.add('active');
}

async function saveProduct(e) {
    e.preventDefault();
    var id = document.getElementById('productId').value;
    var data = {
        name: document.getElementById('productName').value,
        description: document.getElementById('productDescription').value,
        sku: document.getElementById('productSku').value,
        barcode: document.getElementById('productBarcode').value,
        price: parseFloat(document.getElementById('productPrice').value) || 0,
        quantity: parseInt(document.getElementById('productQuantity').value) || 0,
        category: document.getElementById('productCategory').value,
        min_quantity: parseInt(document.getElementById('productMinQty').value) || 5,
        max_quantity: parseInt(document.getElementById('productMaxQty').value) || 100
    };
    try {
        var method = id ? 'PUT' : 'POST';
        var url = id ? '/api/products/' + id : '/api/products';
        var res = await fetch(url, {
            method: method,
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(data)
        });
        var result = await res.json();
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
        var res = await fetch('/api/products/' + productId, { method: 'DELETE' });
        var data = await res.json();
        if (data.success) {
            showSuccess(data.message || 'Produit supprimé');
            if (data.details) {
                var detailStr = data.details.join('\\n');
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
    var res = await fetch('/api/locations?warehouse_id=' + (currentWarehouse || 1));
    locations = await res.json();
    renderLocations();
}

function renderLocations() {
    var tbody = document.getElementById('locationsTable');
    if (!tbody) return;
    if (locations.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4">Aucune zone</td></tr>';
        return;
    }
    var html = '';
    for (var i = 0; i < locations.length; i++) {
        var l = locations[i];
        html += '<tr><td>' + l.name + '</td><td>' + (l.type || '-') + '</td><td>' + (l.capacity || '-') + '</td><td><button class="btn btn-sm" onclick="editLocation(' + l.id + ')">✎</button></td></tr>';
    }
    tbody.innerHTML = html;
}

function editLocation(id) {
    var l = locations.find(function(l) { return l.id === id; });
    if (!l) return;
    document.getElementById('locationId').value = l.id;
    document.getElementById('locationName').value = l.name;
    document.getElementById('locationType').value = l.type || 'rack';
    document.getElementById('locationCapacity').value = l.capacity || '';
    document.getElementById('locationModalTitle').textContent = 'Modifier Zone';
    var select = document.getElementById('locationWarehouse');
    select.innerHTML = '';
    warehouses.forEach(function(w) {
        select.innerHTML += '<option value="' + w.id + '"' + (w.id === l.warehouse_id ? ' selected' : '') + '>' + w.name + '</option>';
    });
    document.getElementById('locationModal').style.display = 'flex';
}

