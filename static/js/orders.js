async function saveOrder(e) {
    e.preventDefault();
    const supplierId = document.getElementById('orderSupplier').value;
    const notes = document.getElementById('orderNotes').value;
    const orderId = document.getElementById('orderId').value;

    if (!supplierId) {
        showError('Selectionnez un fournisseur');
        return;
    }

    const items = [];
    const rows = document.querySelectorAll('#orderItemsBody .order-item-row');
    rows.forEach(row => {
        const hiddenId = row.querySelector('.order-product-id');
        const productInput = row.querySelector('.product-search-input');
        const qtyInput = row.querySelector('.order-qty');
        const priceInput = row.querySelector('.order-price');
        const productId = hiddenId ? hiddenId.value : (productInput ? productInput.value : null);
        if (productId && productInput && productInput.value) {
            items.push({
                product_id: parseInt(productId),
                quantity: parseInt(qtyInput.value) || 1,
                unit_price: parseFloat(priceInput.value) || 0
            });
        }
    });

    try {
        let res, data;
        if (orderId) {
            res = await fetch('/api/orders/' + orderId, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ supplier_id: parseInt(supplierId), notes: notes, items: items })
            });
        } else {
            res = await fetch('/api/orders', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ supplier_id: parseInt(supplierId), notes: notes, items: items })
            });
        }
        data = await res.json();
        if (data.success) {
            closeModal('orderModal');
            document.getElementById('orderNotes').value = '';
            document.getElementById('orderId').value = '';
            document.getElementById('orderNumber').value = '';
            loadOrders();
        } else {
            showError('Erreur: ' + (data.error || 'Sauvegarde echouee'));
        }
    } catch(e) {
        showError('Erreur sauvegarde commande');
    }
}

function addOrderItem() {
    const container = document.getElementById('orderItemsBody');
    const row = document.createElement('div');
    row.className = 'order-item-row';
    row.style = 'display:grid;grid-template-columns:2fr 80px 100px 100px 40px;gap:0.5rem;padding:0.5rem;border-bottom:1px solid var(--border);align-items:center;';
    row.innerHTML = '<div class="product-search-wrapper" style="position:relative;"><input type="text" class="form-input product-search-input" placeholder="Rechercher..." oninput="filterProductSuggestions(this)" onfocus="filterProductSuggestions(this)" autocomplete="off" style="width:100%;"><input type="hidden" class="order-product-id"></div>' +
        '<input type="number" class="form-input order-qty" value="1" min="1" onchange="updateOrderItemTotal(this)" style="width:80px;">' +
        '<input type="number" class="form-input order-price" step="0.01" value="0" onchange="updateOrderItemTotal(this)" style="width:100px;">' +
        '<span class="order-item-total" style="font-weight:600;">0.00 DH</span>' +
        '<button type="button" onclick="removeOrderItem(this)" style="background:none;border:none;color:var(--danger);cursor:pointer;font-size:1rem;"><i class="fas fa-trash"></i></button>';
    container.appendChild(row);
}

function removeOrderItem(btn) {
    const container = document.getElementById('orderItemsBody');
    if (container.querySelectorAll('.order-item-row').length > 1) {
        btn.closest('.order-item-row').remove();
        calculateOrderTotal();
    }
}

function updateOrderItemTotal(el) {
    const row = el.closest('.order-item-row');
    const qty = parseFloat(row.querySelector('.order-qty').value) || 0;
    const price = parseFloat(row.querySelector('.order-price').value) || 0;
    const total = qty * price;
    row.querySelector('.order-item-total').textContent = total.toFixed(2) + ' DH';
    calculateOrderTotal();
}

function calculateOrderTotal() {
    let total = 0;
    document.querySelectorAll('#orderItemsBody .order-item-row').forEach(row => {
        const qty = parseFloat(row.querySelector('.order-qty').value) || 0;
        const price = parseFloat(row.querySelector('.order-price').value) || 0;
        total += qty * price;
    });
    document.getElementById('orderTotalFinal').textContent = total.toFixed(2) + ' DH';
}

function populateProductSelects() {}

async function ensureProductsLoaded() {
    if (!products || products.length === 0) {
        console.log('ensureProductsLoaded: loading products...');
        const res = await fetch('/api/products');
        products = await res.json();
        console.log('ensureProductsLoaded: products loaded =', products.length);
    }
}

function populateRuptureTags() {
    const container = document.getElementById('ruptureTags');
    if (!container) return;
    console.log('populateRuptureTags: products.length =', products ? products.length : 'undefined');
    if (!products || products.length === 0) {
        container.innerHTML = '<span style="font-size:0.75rem;color:var(--text-light);">Chargement...</span>';
        return;
    }
    const outOfStock = products.filter(p => { return p.quantity <= p.min_quantity || p.quantity < 0; }).slice(0, 5);
    if (outOfStock.length === 0) {
        container.innerHTML = '<span style="font-size:0.75rem;color:var(--success);"><i class="fas fa-check-circle"></i> Stock OK</span>';
        return;
    }
    container.innerHTML = '<span style="font-size:0.7rem;font-weight:600;color:var(--danger);margin-right:0.5rem;">🚨 RUPTURE:</span>' +
        outOfStock.map(p => {
            return '<span class="rupture-tag" onclick="addRuptureProduct(' + p.id + ')">' + p.name + '<span class="rupture-qty">' + (p.quantity || 0) + '</span></span>';
        }).join('');
}

function addRuptureProduct(productId) {
    const product = products.find(p => p.id === productId);
    if (!product) return;
    addOrderItem();
    const lastRow = document.querySelector('#orderItemsBody .order-item-row:last-child');
    if (!lastRow) return;
    const wrapper = lastRow.querySelector('.product-search-wrapper');
    if (!wrapper) return;
    const input = wrapper.querySelector('.product-search-input');
    const hiddenId = wrapper.querySelector('.order-product-id');
    const priceInput = lastRow.querySelector('.order-price');
    input.value = product.name;
    hiddenId.value = product.id;
    priceInput.value = product.purchase_price_avg || product.price || 0;
    updateOrderItemTotal(priceInput);
}

function filterProductSuggestions(input) {
    const hiddenId = input.closest('.product-search-wrapper').querySelector('.order-product-id');
    const filter = input.value.toLowerCase().trim();
    const container = document.getElementById('orderSuggestions');

    activeOrderSearchInput = input;

    if (!products || products.length === 0) {
        container.innerHTML = '<div class="product-suggestion-item" style="color:var(--text-light);font-style:italic;">Aucun produit charge</div>';
        positionSuggestions(container, input);
        return;
    }

    if (!filter) {
        container.style.display = 'none';
        return;
    }

    const outOfStock = products.filter(p => p.quantity <= p.min_quantity || p.quantity < 0);
    const inStock = products.filter(p => p.quantity > p.min_quantity && p.quantity >= 0);

    const filteredOutOfStock = outOfStock.filter(p => {
        return p.name.toLowerCase().indexOf(filter) !== -1 || (p.sku && p.sku.toLowerCase().indexOf(filter) !== -1) || (p.barcode && p.barcode.toLowerCase().indexOf(filter) !== -1);
    }).slice(0, 5);

    const filteredInStock = inStock.filter(p => {
        return p.name.toLowerCase().indexOf(filter) !== -1 || (p.sku && p.sku.toLowerCase().indexOf(filter) !== -1) || (p.barcode && p.barcode.toLowerCase().indexOf(filter) !== -1);
    }).slice(0, 5);

    const topRupture = outOfStock.slice(0, 5);
    const allResults = filteredOutOfStock.concat(filteredInStock).slice(0, 5);

    if (allResults.length === 0 && topRupture.length === 0) {
        container.innerHTML = '<div class="product-suggestion-item" style="color:var(--text-light);font-style:italic;">Aucun produit trouve</div>';
        positionSuggestions(container, input);
        return;
    }

    let html = allResults.map(p => {
        const stockClass = p.quantity <= 0 ? 'danger' : (p.quantity <= p.min_quantity ? 'warning' : 'success');
        const stockLabel = p.quantity <= 0 ? 'Rupture' : (p.quantity <= p.min_quantity ? 'Restant: ' + p.quantity : 'Stock: ' + p.quantity);
        const outOfStockClass = p.quantity <= p.min_quantity ? 'out-of-stock' : '';
        const barcodeLabel = p.barcode ? p.barcode : '';
        return '<div class="product-suggestion-item ' + outOfStockClass + '" data-id="' + p.id + '" data-price="' + (p.purchase_price_avg || p.price || 0) + '">' +
            '<span class="product-name">' + p.name + '</span>' +
            '<span class="product-sku">' + (p.sku || '-') + '</span>' +
            '<span class="product-barcode">' + barcodeLabel + '</span>' +
            '<span class="stock-badge ' + stockClass + '">' + stockLabel + '</span>' +
        '</div>';
    }).join('');

    if (topRupture.length > 0) {
        html += '<div style="padding:0.5rem 0.75rem;font-size:0.7rem;color:var(--danger);font-weight:600;border-top:1px solid var(--border);margin-top:0.5rem;">🚨 Also in rupture (click to add):</div>' +
            topRupture.map(p => {
                const barcodeLabel = p.barcode ? p.barcode : '';
                return '<div class="product-suggestion-item out-of-stock" data-id="' + p.id + '" data-price="' + (p.purchase_price_avg || p.price || 0) + '">' +
                    '<span class="product-name">' + p.name + '</span>' +
                    '<span class="product-sku">' + (p.sku || '-') + '</span>' +
                    '<span class="product-barcode">' + barcodeLabel + '</span>' +
                    '<span class="stock-badge danger">Rupture</span>' +
                '</div>';
            }).join('');
    }

    container.innerHTML = html;
    positionSuggestions(container, input);
    attachSuggestionListeners(container, input, hiddenId);
}

function positionSuggestions(container, input) {
    const rect = input.getBoundingClientRect();
    container.style.left = rect.left + 'px';
    container.style.top = rect.bottom + 'px';
    container.style.width = Math.max(rect.width, 280) + 'px';
    container.style.display = 'block';
}

function attachSuggestionListeners(suggestionsEl, input, hiddenId) {
    suggestionsEl.querySelectorAll('.product-suggestion-item').forEach(item => {
        item.onclick = function(e) {
            e.stopPropagation();
            const productId = this.dataset.id;
            const productPrice = parseFloat(this.dataset.price) || 0;
            const productName = this.querySelector('.product-name').textContent;
            input.value = productName;
            hiddenId.value = productId;
            const row = input.closest('.order-item-row');
            row.querySelector('.order-price').value = productPrice;
            suggestionsEl.style.display = 'none';
            activeOrderSearchInput = null;
            updateOrderItemTotal(input);
        };
    });
}

async function openOrderModal(orderId) {
    document.getElementById('orderId').value = orderId || '';
    document.getElementById('orderNumber').value = '';
    document.getElementById('orderNotes').value = '';
    document.getElementById('orderSupplier').value = '';

    if (orderId) {
        const order = orders.find(o => o.id === orderId);
        if (order) {
            document.getElementById('orderNumber').value = order.order_number || '';
            document.getElementById('orderSupplier').value = order.supplier_id || '';
            document.getElementById('orderNotes').value = order.notes || '';
            document.getElementById('orderStatus').value = order.status || 'brouillon';
            await loadOrderItemsAndDisableIfPaid(orderId);
        }
    } else {
        enableOrderForm();
        const container = document.getElementById('orderItemsBody');
        container.innerHTML = '';
        addOrderItem();
    }

    document.getElementById('orderDate').valueAsDate = new Date();
    await ensureProductsLoaded();
    populateRuptureTags();
    document.getElementById('orderModal').style.display = 'flex';
}

function disableOrderFormForPaidOrder() {
    const modal = document.getElementById('orderModal');
    modal.querySelectorAll('.form-input:not([readonly]), .form-select').forEach(el => {
        el.disabled = true;
    });
    modal.querySelectorAll('.order-item-row .btn').forEach(btn => {
        btn.disabled = true;
    });
    document.getElementById('sendOrderBtn').style.display = 'none';
    document.getElementById('saveOrderBtn').style.display = 'none';
    document.getElementById('cancelOrderBtn').style.display = 'inline-block';
}

function enableOrderForm() {
    const modal = document.getElementById('orderModal');
    modal.querySelectorAll('.form-input:not([readonly]), .form-select').forEach(el => {
        el.disabled = false;
    });
    modal.querySelectorAll('.order-item-row .btn').forEach(btn => {
        btn.disabled = false;
    });
    document.getElementById('sendOrderBtn').style.display = 'inline-block';
    document.getElementById('saveOrderBtn').style.display = 'inline-block';
    document.getElementById('cancelOrderBtn').style.display = 'none';
}

async function cancelOrder() {
    const orderId = document.getElementById('orderId').value;
    if (!orderId) return;

    if (!confirm('Confirmer l\'annulation de la commande ? Le stock sera ajusté et le montant remboursé au compte principal.')) return;

    try {
        const res = await fetch('/api/orders/' + orderId, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: 'annulee' })
        });
        const data = await res.json();
        if (data.success) {
            showSuccess('Commande annulée - Stock ajusté et compte remboursé');
            closeModal('orderModal');
            loadOrders();
            loadProducts();
            loadMainAccount();
        }
    } catch(e) {
        showError('Erreur annulation commande');
    }
}

async function loadOrderItems(orderId) {
    try {
        const res = await fetch('/api/orders/' + orderId + '/items');
        const items = await res.json();
        const tbody = document.getElementById('orderItemsBody');
        tbody.innerHTML = '';

        items.forEach(item => {
            const row = document.createElement('tr');
            row.className = 'order-item-row';
            row.innerHTML = '<td><div class="product-search-wrapper"><input type="text" class="form-input product-search-input" placeholder="Rechercher un produit..." oninput="filterProductSuggestions(this)" onfocus="filterProductSuggestions(this)" autocomplete="off"><input type="hidden" class="order-product-id"></div></td>' +
                '<td><input type="number" class="form-input order-qty" value="' + item.quantity + '" min="1" onchange="updateOrderItemTotal(this)"></td>' +
                '<td><input type="number" class="form-input order-price" step="0.01" value="' + item.unit_price + '" onchange="updateOrderItemTotal(this)"></td>' +
                '<td class="order-item-total">' + (item.quantity * item.unit_price).toFixed(2) + ' DH</td>' +
                '<td><button type="button" class="btn btn-sm btn-danger" onclick="removeOrderItem(this)"><i class="fas fa-trash"></i></button></td>';
            tbody.appendChild(row);
        });

        if (items.length === 0) {
            addOrderItem();
        }

        populateProductSelects();
        items.forEach((item, idx) => {
            const selects = document.querySelectorAll('.order-product-select');
            if (selects[idx]) selects[idx].value = item.product_id;
        });

        calculateOrderTotal();
    } catch(e) {
        console.error(e);
        addOrderItem();
    }
}

async function loadOrderItemsAndDisableIfPaid(orderId) {
    await loadOrderItems(orderId);
    const order = orders.find(o => o.id === orderId);
    if (order && order.status === 'paye') {
        disableOrderFormForPaidOrder();
    }
}

function editOrder(orderId) {
    showTab('orders');
    openOrderModal(orderId);
}

function openOrderForProduct(productId, suggestedQty) {
    showTab('orders');
    openOrderModal(null);
    setTimeout(() => {
        const selects = document.querySelectorAll('.order-product-select');
        if (selects.length > 0) {
            selects[0].value = productId;
            const qtyInputs = document.querySelectorAll('.order-qty');
            if (qtyInputs.length > 0 && suggestedQty) {
                qtyInputs[0].value = suggestedQty;
                updateOrderItemTotal(qtyInputs[0]);
            }
        }
    }, 200);
}

async function sendOrder() {
    var orderId = document.getElementById('orderId').value;
    if (!orderId) {
        showError('Enregistrez d\'abord la commande');
        return;
    }
    showSuccess('Cliquez sur "Receptionner" pour recevoir les produits et mettre a jour le stock');
}

let orderToConvert = null;
function openConvertToInvoice(orderId) {
    orderToConvert = orderId;
    loadCustomersForSelect();
    document.getElementById('invoiceDueDate').valueAsDate = new Date(Date.now() + 30 * 86400000);
    openModal('convertToInvoiceModal');
}

async function loadCustomersForSelect() {
    try {
        const res = await fetch('/api/customers');
        const customersData = await res.json();
        const select = document.getElementById('invoiceCustomer');
        if (select) {
            select.innerHTML = '<option value="">Selectionnez un client</option>' +
                customersData.map(c => '<option value="' + c.id + '">' + c.name + '</option>').join('');
        }
    } catch(e) { console.error(e); }
}

async function confirmConvertToInvoice() {
    if (!orderToConvert) return;
    const customerId = document.getElementById('invoiceCustomer').value;
    if (!customerId) {
        showError('Selectionnez un client');
        return;
    }
    try {
        const orderRes = await fetch('/api/orders/' + orderToConvert);
        const order = await orderRes.json();
        const itemsRes = await fetch('/api/orders/' + orderToConvert + '/items');
        const items = await itemsRes.json();

        const invoiceItems = items.map(item => {
            return {
                product_id: item.product_id,
                quantity: item.quantity,
                unit_price: item.unit_price
            };
        });

        const res = await fetch('/api/invoices', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                customer_id: parseInt(customerId),
                notes: 'Converti du bon de commande ' + order.order_number,
                items: invoiceItems,
                due_date: document.getElementById('invoiceDueDate').value
            })
        });
        const data = await res.json();
        if (data.success) {
            closeModal('convertToInvoiceModal');
            showTab('invoices');
            showError('Facture creee: ' + data.invoice_number);
            loadInvoices();
        } else {
            showError('Erreur: ' + (data.error || 'Conversion echouee'));
        }
    } catch(e) {
        showError('Erreur conversion');
    }
    orderToConvert = null;
}

async function loadOrders() {
    try {
        const res = await fetch('/api/orders');
        orders = await res.json();
        renderOrders();
    } catch(e) {
        showError('Erreur lors du chargement des commandes');
    }
}

function renderOrders() {
    const tbody = document.getElementById('ordersTable');
    if (!tbody) return;
    if (orders.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7">Aucune commande</td></tr>';
        return;
    }
    let html = '';
    const statusLabels = { 'brouillon': 'Brouillon', 'recue': 'Recu', 'paye': 'Paye' };
    for (let i = 0; i < orders.length; i++) {
        const o = orders[i];
        const statusClass = o.status === 'paye' ? 'primary' : (o.status === 'recue' ? 'success' : 'warning');
        html += '<tr>';
        html += '<td>' + (o.order_number || '-') + '</td>';
        html += '<td>' + (o.created_at ? o.created_at.substring(0, 10) : '-') + '</td>';
        html += '<td>' + (o.supplier_name || '-') + '</td>';
        html += '<td>' + (o.total || 0).toFixed(2) + ' DH</td>';
        html += '<td><span class="badge badge-' + statusClass + '">' + (statusLabels[o.status] || o.status) + '</span></td>';
        html += '<td>' + (o.received_at ? o.received_at.substring(0, 10) : '-') + '</td>';
        html += '<td>';
        html += '<button class="btn btn-sm btn-outline" onclick="editOrder(' + o.id + ')" title="Modifier"><i class="fas fa-edit"></i></button> ';
        if (o.status === 'brouillon') {
            html += '<button class="btn btn-sm btn-success" onclick="receiveOrder(' + o.id + ')" title="Receptionner"><i class="fas fa-truck-loading"></i></button> ';
        }
        if (o.status === 'recue') {
            html += '<button class="btn btn-sm btn-primary" onclick="payOrder(' + o.id + ')" title="Payer"><i class="fas fa-money-bill-wave"></i></button> ';
        }
        if (o.status === 'paye') {
            html += '<button class="btn btn-sm btn-info" onclick="viewSupplierInvoice(' + o.id + ')" title="Facture Fournisseur"><i class="fas fa-file-invoice"></i></button>';
        }
        html += '</td></tr>';
    }
    tbody.innerHTML = html;
}

async function receiveOrder(orderId) {
    if (!confirm('Confirmer la reception de la commande? Le stock sera mis a jour.')) return;
    try {
        const res = await fetch('/api/orders/' + orderId, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: 'recue' })
        });
        const data = await res.json();
        if (data.success) {
            showSuccess('Commande recue et stock mis a jour');
            loadOrders();
            loadProducts();
        }
    } catch(e) {
        showError('Erreur reception commande');
    }
}

async function payOrder(orderId) {
    if (!confirm('Confirmer le paiement de la commande? Une sortie de caisse sera enregistree.')) return;
    try {
        const res = await fetch('/api/orders/' + orderId, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: 'paye' })
        });
        const data = await res.json();
        if (data.success) {
            showSuccess('Paiement enregistre - Facture Fournisseur creee');
            loadOrders();
            loadMainAccount();
        }
    } catch(e) {
        showError('Erreur paiement commande');
    }
}

async function loadMainAccount() {
    try {
        const res = await fetch('/api/main-account');
        const data = await res.json();
        if (!data.account) return;

        document.getElementById('mainAccountBalance').textContent = (data.account.current_balance || 0).toFixed(2);

        let totalIn = 0;
        let totalOut = 0;
        if (data.transactions) {
            data.transactions.forEach(t => {
                if (t.type === 'in') totalIn += t.amount;
                else totalOut += t.amount;
            });
        }
        document.getElementById('mainAccountTotalIn').textContent = totalIn.toFixed(2);
        document.getElementById('mainAccountTotalOut').textContent = totalOut.toFixed(2);

        const tbody = document.getElementById('mainAccountTransactions');
        if (!tbody) return;

        if (!data.transactions || data.transactions.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5">Aucune transaction</td></tr>';
            return;
        }

        const reasonLabels = {
            'initial': 'Solde initial',
            'session_close': 'Fermeture Session',
            'supplier_order': 'Achat Fournisseur',
            'transfer_pos': 'Transfert Caisse',
            'deposit': 'Dépôt',
            'withdraw': 'Retrait',
            'card_payment': 'Paiement Carte'
        };

        tbody.innerHTML = data.transactions.map(t => {
            const date = t.created_at ? t.created_at.substring(0, 16) : '-';
            const typeClass = t.type === 'in' ? 'success' : 'danger';
            const typeLabel = t.type === 'in' ? 'Entrée' : 'Sortie';
            let reasonIcon = '';
            if (t.reason === 'card_payment') {
                reasonIcon = '<i class="fas fa-credit-card" style="color: var(--color-info); margin-right: 5px;"></i>';
            } else if (t.reason === 'session_close') {
                reasonIcon = '<i class="fas fa-cash-register" style="color: var(--color-success); margin-right: 5px;"></i>';
            } else if (t.reason === 'supplier_order') {
                reasonIcon = '<i class="fas fa-truck" style="color: var(--color-warning); margin-right: 5px;"></i>';
            } else if (t.reason === 'deposit') {
                reasonIcon = '<i class="fas fa-plus-circle" style="color: var(--color-success); margin-right: 5px;"></i>';
            } else if (t.reason === 'withdraw') {
                reasonIcon = '<i class="fas fa-minus-circle" style="color: var(--color-danger); margin-right: 5px;"></i>';
            }
            return '<tr>' +
                '<td>' + date + '</td>' +
                '<td><span class="badge badge-' + typeClass + '">' + typeLabel + '</span></td>' +
                '<td' + (t.type === 'out' ? ' style="color:var(--danger);"' : ' style="color:var(--success);"') + '>' +
                (t.type === 'in' ? '+' : '-') + t.amount.toFixed(2) + ' DH</td>' +
                '<td>' + reasonIcon + (reasonLabels[t.reason] || t.reason || '-') + '</td>' +
                '<td>' + (t.note || '-') + '</td>' +
            '</tr>';
        }).join('');
    } catch(e) {
        console.error('Error loading main account:', e);
    }
}

async function openTransferToPosModal() {
    const amount = prompt('Montant à transférer vers la caisse:');
    if (!amount || isNaN(amount) || parseFloat(amount) <= 0) return;
    try {
        const res = await fetch('/api/main-account/transfer-to-pos', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ amount: parseFloat(amount), note: 'Transfert manuel' })
        });
        const data = await res.json();
        if (data.success) {
            showSuccess('Transfert effectué vers la caisse');
            loadMainAccount();
            loadPosSession();
        } else {
            showError(data.error || 'Erreur de transfert');
        }
    } catch(e) {
        showError('Erreur de transfert');
    }
}

function viewSupplierInvoice(orderId) {
    const order = orders.find(o => o.id === orderId);
    if (!order) return;
    alert('Facture Fournisseur: ' + order.order_number + '\nTotal: ' + (order.total || 0).toFixed(2) + ' DH\nDate paiement: ' + (order.paid_at || '-'));
}

// --- Order barcode scanner ---
let orderScannerStream = null;
let orderScannerReader = null;

function stopOrderScanner() {
    if (orderScannerReader) {
        try { orderScannerReader.reset(); } catch(e) {}
        orderScannerReader = null;
    }
    if (orderScannerStream) {
        orderScannerStream.getTracks().forEach(function(t) { t.stop(); });
        orderScannerStream = null;
    }
}

async function toggleOrderScanner() {
    const area = document.getElementById('orderScannerArea');
    if (!area) return;
    if (area.style.display !== 'none') {
        stopOrderScanner();
        area.style.display = 'none';
        return;
    }
    area.style.display = 'block';
    const video = document.getElementById('orderScannerVideo');
    if (!video) return;
    try {
        const stream = await navigator.mediaDevices.getUserMedia({
            video: { facingMode: 'environment', width: { ideal: 640 }, height: { ideal: 480 } }
        });
        orderScannerStream = stream;
        video.srcObject = stream;
        video.play();
        const hints = new Map();
        hints.set(ZXing.DecodeHintType.POSSIBLE_FORMATS, [
            ZXing.BarcodeFormat.EAN_13, ZXing.BarcodeFormat.EAN_8,
            ZXing.BarcodeFormat.CODE_128, ZXing.BarcodeFormat.CODE_39,
            ZXing.BarcodeFormat.QR_CODE,
        ]);
        hints.set(ZXing.DecodeHintType.TRY_HARDER, true);
        const reader = new ZXing.BrowserMultiFormatReader(hints, 150);
        orderScannerReader = reader;
        reader.decodeFromVideoDevice(null, video, function(result, err) {
            if (result) {
                handleOrderScanResult(result.text);
                stopOrderScanner();
                var area = document.getElementById('orderScannerArea');
                if (area) area.style.display = 'none';
            }
        });
    } catch(e) {
        showError('Erreur d\'accès à la caméra: ' + e.message);
        area.style.display = 'none';
    }
}

function handleOrderScanResult(barcode) {
    const product = products.find(function(p) { return String(p.barcode) === String(barcode); });
    if (!product) {
        showError('Produit avec le code-barres ' + barcode + ' non trouvé');
        return;
    }
    var rows = document.querySelectorAll('#orderItemsBody .order-item-row');
    var targetRow = null;
    for (var i = 0; i < rows.length; i++) {
        var hiddenId = rows[i].querySelector('.order-product-id');
        if (!hiddenId || !hiddenId.value) {
            targetRow = rows[i];
            break;
        }
    }
    if (!targetRow) {
        addOrderItem();
        var allRows = document.querySelectorAll('#orderItemsBody .order-item-row');
        targetRow = allRows[allRows.length - 1];
    }
    var input = targetRow.querySelector('.product-search-input');
    var hiddenId = targetRow.querySelector('.order-product-id');
    var priceInput = targetRow.querySelector('.order-price');
    if (input) input.value = product.name;
    if (hiddenId) hiddenId.value = product.id;
    if (priceInput) priceInput.value = product.purchase_price_avg || product.price || 0;
    if (priceInput) updateOrderItemTotal(priceInput);
    else if (input) updateOrderItemTotal(input);
}

let activeOrderSearchInput = null;

document.addEventListener('click', function(e) {
    const container = document.getElementById('orderSuggestions');
    const input = activeOrderSearchInput;
    if (container && container.style.display === 'block' && input && !container.contains(e.target) && e.target !== input) {
        container.style.display = 'none';
        activeOrderSearchInput = null;
    }
});

(function() {
    const observer = new MutationObserver(function() {
        const body = document.querySelector('.order-modal-body');
        if (body && !body._orderSuggestScrollAttached) {
            body._orderSuggestScrollAttached = true;
            body.addEventListener('scroll', function() {
                const container = document.getElementById('orderSuggestions');
                if (container && container.style.display === 'block') {
                    container.style.display = 'none';
                    activeOrderSearchInput = null;
                }
            }, { passive: true });
        }
    });
    observer.observe(document.body, { childList: true, subtree: true });
})();
