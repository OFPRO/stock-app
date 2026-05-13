async function saveOrder(e) {
    e.preventDefault();
    var supplierId = document.getElementById('orderSupplier').value;
    var notes = document.getElementById('orderNotes').value;
    var orderId = document.getElementById('orderId').value;
    
    if (!supplierId) {
        showError('Selectionnez un fournisseur');
        return;
    }
    
    var items = [];
    var rows = document.querySelectorAll('#orderItemsBody .order-item-row');
    rows.forEach(function(row) {
        var hiddenId = row.querySelector('.order-product-id');
        var productInput = row.querySelector('.product-search-input');
        var qtyInput = row.querySelector('.order-qty');
        var priceInput = row.querySelector('.order-price');
        var productId = hiddenId ? hiddenId.value : (productInput ? productInput.value : null);
        if (productId && productInput && productInput.value) {
            items.push({
                product_id: parseInt(productId),
                quantity: parseInt(qtyInput.value) || 1,
                unit_price: parseFloat(priceInput.value) || 0
            });
        }
    });
    
    try {
        var res, data;
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
    var container = document.getElementById('orderItemsBody');
    var row = document.createElement('div');
    row.className = 'order-item-row';
    row.style = 'display:grid;grid-template-columns:2fr 80px 100px 100px 40px;gap:0.5rem;padding:0.5rem;border-bottom:1px solid var(--border);align-items:center;';
    row.innerHTML = '<div class="product-search-wrapper" style="position:relative;"><input type="text" class="form-input product-search-input" placeholder="Rechercher..." oninput="filterProductSuggestions(this)" onfocus="filterProductSuggestions(this)" autocomplete="off" style="width:100%;"><div class="product-suggestions"></div><input type="hidden" class="order-product-id"></div>' +
        '<input type="number" class="form-input order-qty" value="1" min="1" onchange="updateOrderItemTotal(this)" style="width:80px;">' +
        '<input type="number" class="form-input order-price" step="0.01" value="0" onchange="updateOrderItemTotal(this)" style="width:100px;">' +
        '<span class="order-item-total" style="font-weight:600;">0.00 DH</span>' +
        '<button type="button" onclick="removeOrderItem(this)" style="background:none;border:none;color:var(--danger);cursor:pointer;font-size:1rem;"><i class="fas fa-trash"></i></button>';
    container.appendChild(row);
}

function removeOrderItem(btn) {
    var container = document.getElementById('orderItemsBody');
    if (container.querySelectorAll('.order-item-row').length > 1) {
        btn.closest('.order-item-row').remove();
        calculateOrderTotal();
    }
}

function updateOrderItemTotal(el) {
    var row = el.closest('.order-item-row');
    var qty = parseFloat(row.querySelector('.order-qty').value) || 0;
    var price = parseFloat(row.querySelector('.order-price').value) || 0;
    var total = qty * price;
    row.querySelector('.order-item-total').textContent = total.toFixed(2) + ' DH';
    calculateOrderTotal();
}

function calculateOrderTotal() {
    var total = 0;
    document.querySelectorAll('#orderItemsBody .order-item-row').forEach(function(row) {
        var qty = parseFloat(row.querySelector('.order-qty').value) || 0;
        var price = parseFloat(row.querySelector('.order-price').value) || 0;
        total += qty * price;
    });
    document.getElementById('orderTotalFinal').textContent = total.toFixed(2) + ' DH';
}

function populateProductSelects() {}

async function ensureProductsLoaded() {
    if (!products || products.length === 0) {
        console.log('ensureProductsLoaded: loading products...');
        var res = await fetch('/api/products');
        products = await res.json();
        console.log('ensureProductsLoaded: products loaded =', products.length);
    }
}

function populateRuptureTags() {
    var container = document.getElementById('ruptureTags');
    if (!container) return;
    console.log('populateRuptureTags: products.length =', products ? products.length : 'undefined');
    if (!products || products.length === 0) {
        container.innerHTML = '<span style="font-size:0.75rem;color:var(--text-light);">Chargement...</span>';
        return;
    }
    var outOfStock = products.filter(function(p) { return p.quantity <= p.min_quantity || p.quantity < 0; }).slice(0, 5);
    if (outOfStock.length === 0) {
        container.innerHTML = '<span style="font-size:0.75rem;color:var(--success);"><i class="fas fa-check-circle"></i> Stock OK</span>';
        return;
    }
    container.innerHTML = '<span style="font-size:0.7rem;font-weight:600;color:var(--danger);margin-right:0.5rem;">🚨 RUPTURE:</span>' +
        outOfStock.map(function(p) {
            return '<span class="rupture-tag" onclick="addRuptureProduct(' + p.id + ')">' + p.name + '<span class="rupture-qty">' + (p.quantity || 0) + '</span></span>';
        }).join('');
}

function addRuptureProduct(productId) {
    var product = products.find(function(p) { return p.id === productId; });
    if (!product) return;
    addOrderItem();
    var lastRow = document.querySelector('#orderItemsBody .order-item-row:last-child');
    if (!lastRow) return;
    var wrapper = lastRow.querySelector('.product-search-wrapper');
    if (!wrapper) return;
    var input = wrapper.querySelector('.product-search-input');
    var hiddenId = wrapper.querySelector('.order-product-id');
    var priceInput = lastRow.querySelector('.order-price');
    input.value = product.name;
    hiddenId.value = product.id;
    priceInput.value = product.purchase_price_avg || product.price || 0;
    updateOrderItemTotal(priceInput);
}

function filterProductSuggestions(input) {
    var wrapper = input.closest('.product-search-wrapper');
    var suggestionsEl = wrapper.querySelector('.product-suggestions');
    var hiddenId = wrapper.querySelector('.order-product-id');
    var filter = input.value.toLowerCase().trim();
    
    console.log('filterProductSuggestions: filter =', filter, 'products.length =', products ? products.length : 'undefined');
    
    if (!products || products.length === 0) {
        suggestionsEl.innerHTML = '<div class="product-suggestion-item" style="color:var(--text-light);font-style:italic;">Aucun produit charge</div>';
        suggestionsEl.classList.add('show');
        return;
    }
    
    if (!filter) {
        suggestionsEl.classList.remove('show');
        return;
    }
    
    var outOfStock = products.filter(function(p) { return p.quantity <= p.min_quantity || p.quantity < 0; });
    var inStock = products.filter(function(p) { return p.quantity > p.min_quantity && p.quantity >= 0; });
    
    console.log('outOfStock.length =', outOfStock.length, 'inStock.length =', inStock.length);
    
    var filteredOutOfStock = outOfStock.filter(function(p) {
        return p.name.toLowerCase().indexOf(filter) !== -1 || (p.sku && p.sku.toLowerCase().indexOf(filter) !== -1);
    }).slice(0, 5);
    
    var filteredInStock = inStock.filter(function(p) {
        return p.name.toLowerCase().indexOf(filter) !== -1 || (p.sku && p.sku.toLowerCase().indexOf(filter) !== -1);
    }).slice(0, 5);
    
    var topRupture = outOfStock.slice(0, 5);
    var allResults = filteredOutOfStock.concat(filteredInStock).slice(0, 5);
    
    if (allResults.length === 0 && topRupture.length === 0) {
        suggestionsEl.innerHTML = '<div class="product-suggestion-item" style="color:var(--text-light);font-style:italic;">Aucun produit trouve</div>';
        suggestionsEl.classList.add('show');
        return;
    }
    
    var html = allResults.map(function(p) {
        var stockClass = p.quantity <= 0 ? 'danger' : (p.quantity <= p.min_quantity ? 'warning' : 'success');
        var stockLabel = p.quantity <= 0 ? 'Rupture' : (p.quantity <= p.min_quantity ? 'Restant: ' + p.quantity : 'Stock: ' + p.quantity);
        var outOfStockClass = p.quantity <= p.min_quantity ? 'out-of-stock' : '';
        return '<div class="product-suggestion-item ' + outOfStockClass + '" data-id="' + p.id + '" data-price="' + (p.purchase_price_avg || p.price || 0) + '">' +
            '<span class="product-name">' + p.name + '</span>' +
            '<span class="product-sku">' + (p.sku || '-') + '</span>' +
            '<span class="stock-badge ' + stockClass + '">' + stockLabel + '</span>' +
        '</div>';
    }).join('');
    
    if (topRupture.length > 0) {
        html += '<div style="padding:0.5rem 0.75rem;font-size:0.7rem;color:var(--danger);font-weight:600;border-top:1px solid var(--border);margin-top:0.5rem;">🚨 Also in rupture (click to add):</div>' +
            topRupture.map(function(p) {
                return '<div class="product-suggestion-item out-of-stock" data-id="' + p.id + '" data-price="' + (p.purchase_price_avg || p.price || 0) + '">' +
                    '<span class="product-name">' + p.name + '</span>' +
                    '<span class="product-sku">' + (p.sku || '-') + '</span>' +
                    '<span class="stock-badge danger">Rupture</span>' +
                '</div>';
            }).join('');
    }
    
    suggestionsEl.innerHTML = html;
    suggestionsEl.classList.add('show');
    attachSuggestionListeners(suggestionsEl, input, hiddenId);
}

function attachSuggestionListeners(suggestionsEl, input, hiddenId) {
    suggestionsEl.querySelectorAll('.product-suggestion-item').forEach(function(item) {
        item.onclick = function() {
            var productId = this.dataset.id;
            var productPrice = parseFloat(this.dataset.price) || 0;
            var productName = this.querySelector('.product-name').textContent;
            input.value = productName;
            hiddenId.value = productId;
            var row = input.closest('.order-item-row');
            row.querySelector('.order-price').value = productPrice;
            suggestionsEl.classList.remove('show');
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
        var order = orders.find(function(o) { return o.id === orderId; });
        if (order) {
            document.getElementById('orderNumber').value = order.order_number || '';
            document.getElementById('orderSupplier').value = order.supplier_id || '';
            document.getElementById('orderNotes').value = order.notes || '';
            document.getElementById('orderStatus').value = order.status || 'brouillon';
            await loadOrderItemsAndDisableIfPaid(orderId);
        }
    } else {
        enableOrderForm();
        var container = document.getElementById('orderItemsBody');
        container.innerHTML = '';
        addOrderItem();
    }
    
    document.getElementById('orderDate').valueAsDate = new Date();
    await ensureProductsLoaded();
    populateRuptureTags();
    document.getElementById('orderModal').style.display = 'flex';
}

function disableOrderFormForPaidOrder() {
    var modal = document.getElementById('orderModal');
    modal.querySelectorAll('.form-input:not([readonly]), .form-select').forEach(function(el) {
        el.disabled = true;
    });
    modal.querySelectorAll('.order-item-row .btn').forEach(function(btn) {
        btn.disabled = true;
    });
    document.querySelector('#orderModal .btn-success[onclick="sendOrder()"]').style.display = 'none';
    document.querySelector('#orderModal .btn-primary').style.display = 'none';
    document.getElementById('cancelOrderBtn').style.display = 'inline-block';
}

function enableOrderForm() {
    var modal = document.getElementById('orderModal');
    modal.querySelectorAll('.form-input:not([readonly]), .form-select').forEach(function(el) {
        el.disabled = false;
    });
    modal.querySelectorAll('.order-item-row .btn').forEach(function(btn) {
        btn.disabled = false;
    });
    document.querySelector('#orderModal .btn-success[onclick="sendOrder()"]').style.display = 'inline-block';
    document.querySelector('#orderModal .btn-primary').style.display = 'inline-block';
    document.getElementById('cancelOrderBtn').style.display = 'none';
}

async function cancelOrder() {
    var orderId = document.getElementById('orderId').value;
    if (!orderId) return;
    
    if (!confirm('Confirmer l\'annulation de la commande ? Le stock sera ajusté et le montant remboursé au compte principal.')) return;
    
    try {
        var res = await fetch('/api/orders/' + orderId, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: 'annulee' })
        });
        var data = await res.json();
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
        var res = await fetch('/api/orders/' + orderId + '/items');
        var items = await res.json();
        var tbody = document.getElementById('orderItemsBody');
        tbody.innerHTML = '';
        
        items.forEach(function(item) {
            var row = document.createElement('tr');
            row.className = 'order-item-row';
            row.innerHTML = '<td><div class="product-search-wrapper"><input type="text" class="form-input product-search-input" placeholder="Rechercher un produit..." oninput="filterProductSuggestions(this)" onfocus="filterProductSuggestions(this)" autocomplete="off"><div class="product-suggestions"></div><input type="hidden" class="order-product-id"></div></td>' +
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
        items.forEach(function(item, idx) {
            var selects = document.querySelectorAll('.order-product-select');
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
    var order = orders.find(function(o) { return o.id === orderId; });
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
    setTimeout(function() {
        var selects = document.querySelectorAll('.order-product-select');
        if (selects.length > 0) {
            selects[0].value = productId;
            var qtyInputs = document.querySelectorAll('.order-qty');
            if (qtyInputs.length > 0 && suggestedQty) {
                qtyInputs[0].value = suggestedQty;
                updateOrderItemTotal(qtyInputs[0]);
            }
        }
    }, 200);
}

async function sendOrder(orderId) {
    if (!orderId) {
        showError('Enregistrez d\'abord la commande');
        return;
    }
    showSuccess('Cliquez sur "Receptionner" pour recevoir les produits et mettre a jour le stock');
}

var orderToConvert = null;
function openConvertToInvoice(orderId) {
    orderToConvert = orderId;
    loadCustomersForSelect();
    document.getElementById('invoiceDueDate').valueAsDate = new Date(Date.now() + 30 * 86400000);
    document.getElementById('convertToInvoiceModal').classList.add('active');
}

async function loadCustomersForSelect() {
    try {
        var res = await fetch('/api/customers');
        var customers = await res.json();
        var select = document.getElementById('invoiceCustomer');
        if (select) {
            select.innerHTML = '<option value="">Selectionnez un client</option>' +
                customers.map(function(c) { return '<option value="' + c.id + '">' + c.name + '</option>'; }).join('');
        }
    } catch(e) { console.error(e); }
}

async function confirmConvertToInvoice() {
    if (!orderToConvert) return;
    var customerId = document.getElementById('invoiceCustomer').value;
    if (!customerId) {
        showError('Selectionnez un client');
        return;
    }
    try {
        var orderRes = await fetch('/api/orders/' + orderToConvert);
        var order = await orderRes.json();
        var itemsRes = await fetch('/api/orders/' + orderToConvert + '/items');
        var items = await itemsRes.json();
        
        var invoiceItems = items.map(function(item) {
            return {
                product_id: item.product_id,
                quantity: item.quantity,
                unit_price: item.unit_price
            };
        });
        
        var res = await fetch('/api/invoices', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                customer_id: parseInt(customerId),
                notes: 'Converti du bon de commande ' + order.order_number,
                items: invoiceItems,
                due_date: document.getElementById('invoiceDueDate').value
            })
        });
        var data = await res.json();
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
    var res = await fetch('/api/orders');
    orders = await res.json();
    renderOrders();
}

function renderOrders() {
    var tbody = document.getElementById('ordersTable');
    if (!tbody) return;
    if (orders.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7">Aucune commande</td></tr>';
        return;
    }
    var html = '';
    var statusLabels = { 'brouillon': 'Brouillon', 'recue': 'Recu', 'paye': 'Paye' };
    for (var i = 0; i < orders.length; i++) {
        var o = orders[i];
        var statusClass = o.status === 'paye' ? 'primary' : (o.status === 'recue' ? 'success' : 'warning');
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
        var res = await fetch('/api/orders/' + orderId, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: 'recue' })
        });
        var data = await res.json();
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
        var res = await fetch('/api/orders/' + orderId, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: 'paye' })
        });
var data = await res.json();
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
        var res = await fetch('/api/main-account');
        var data = await res.json();
        if (!data.account) return;
        
        document.getElementById('mainAccountBalance').textContent = (data.account.current_balance || 0).toFixed(2);
        
        var totalIn = 0;
        var totalOut = 0;
        if (data.transactions) {
            data.transactions.forEach(function(t) {
                if (t.type === 'in') totalIn += t.amount;
                else totalOut += t.amount;
            });
        }
        document.getElementById('mainAccountTotalIn').textContent = totalIn.toFixed(2);
        document.getElementById('mainAccountTotalOut').textContent = totalOut.toFixed(2);
        
        var tbody = document.getElementById('mainAccountTransactions');
        if (!tbody) return;
        
        if (!data.transactions || data.transactions.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5">Aucune transaction</td></tr>';
            return;
        }
        
        var reasonLabels = {
            'initial': 'Solde initial',
            'session_close': 'Fermeture Session',
            'supplier_order': 'Achat Fournisseur',
            'transfer_pos': 'Transfert Caisse',
            'deposit': 'Dépôt',
            'withdraw': 'Retrait',
            'card_payment': 'Paiement Carte'
        };
        
        tbody.innerHTML = data.transactions.map(function(t) {
            var date = t.created_at ? t.created_at.substring(0, 16) : '-';
            var typeClass = t.type === 'in' ? 'success' : 'danger';
            var typeLabel = t.type === 'in' ? 'Entrée' : 'Sortie';
            var reasonIcon = '';
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
    var amount = prompt('Montant à transférer vers la caisse:');
    if (!amount || isNaN(amount) || parseFloat(amount) <= 0) return;
    try {
        var res = await fetch('/api/main-account/transfer-to-pos', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ amount: parseFloat(amount), note: 'Transfert manuel' })
        });
        var data = await res.json();
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
    var order = orders.find(function(o) { return o.id === orderId; });
    if (!order) return;
    alert('Facture Fournisseur: ' + order.order_number + '\nTotal: ' + (order.total || 0).toFixed(2) + ' DH\nDate paiement: ' + (order.paid_at || '-'));
}
