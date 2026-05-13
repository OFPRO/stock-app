var posSession = null;
var posCart = [];
var posPaymentMethod = 'cash';
var posTenderedAmount = 0;
var posCashMovementType = 'in';

async function loadPosSession() {
    try {
        var res = await fetch('/api/pos/sessions');
        var sessions = await res.json();
        var openSession = sessions.find(function(s) { return s.status === 'open'; });
        if (openSession) {
            posSession = openSession;
            document.getElementById('posSessionNumber').textContent = 'Session: ' + openSession.session_number;
            document.getElementById('posSessionStatus').textContent = 'Ouverte';
            document.getElementById('posSessionStatus').className = 'badge badge-success';
            document.getElementById('btnOpenSession').style.display = 'none';
            document.getElementById('btnCloseSession').style.display = 'inline-flex';
            loadPosCashMovements();
            loadPosCustomers();
        } else {
            resetPosSession();
        }
    } catch(e) {
        console.error('Error loading POS session:', e);
    }
}

function resetPosSession() {
    posSession = null;
    document.getElementById('posSessionNumber').textContent = 'Session: ---';
    document.getElementById('posSessionStatus').textContent = 'Fermee';
    document.getElementById('posSessionStatus').className = 'badge badge-warning';
    document.getElementById('btnOpenSession').style.display = 'inline-flex';
    document.getElementById('btnCloseSession').style.display = 'none';
}

async function openPosSession() {
    try {
        var res = await fetch('/api/pos/sessions', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ warehouse_id: 1, opening_cash: 0 })
        });
        var data = await res.json();
        if (data.success) {
            showTab('pos');
            loadPosSession();
            showError('Caisse ouverte: ' + data.session_number);
        } else {
            showError(data.error || 'Erreur ouverture caisse');
        }
    } catch(e) {
        showError('Erreur ouverture caisse');
    }
}

async function closePosSession() {
    if (!posSession) return;
    var closingCash = prompt('Montant en caisse:', '0');
    if (closingCash === null) return;
    try {
        var res = await fetch('/api/pos/sessions/' + posSession.id + '/close', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ closing_cash: parseFloat(closingCash) || 0 })
        });
        var data = await res.json();
        if (data.success) {
            resetPosSession();
            clearPosCart();
            showError('Caisse fermee avec succes');
        }
    } catch(e) {
        showError('Erreur fermeture caisse');
    }
}

function searchPosProducts() {
    var query = document.getElementById('posSearchInput').value.trim();
    var results = document.getElementById('posSearchResults');
    if (query.length < 2) {
        results.classList.remove('active');
        return;
    }
    var filtered = products.filter(function(p) {
        return p.name.toLowerCase().indexOf(query.toLowerCase()) !== -1 ||
               (p.sku && p.sku.toLowerCase().indexOf(query.toLowerCase()) !== -1) ||
               (p.barcode && p.barcode.toLowerCase().indexOf(query.toLowerCase()) !== -1);
    });
    if (filtered.length === 0) {
        results.innerHTML = '<div style="padding:0.75rem;color:var(--text-light);">Aucun produit trouve</div>';
    } else {
        results.innerHTML = filtered.slice(0, 10).map(function(p) {
            var salePrice = p.price_base > 0 ? p.price_base : p.price;
            var stockClass = p.quantity <= 0 ? 'danger' : (p.quantity <= p.min_quantity ? 'warning' : 'success');
            var stockLabel = p.quantity <= 0 ? 'Rupture' : p.quantity;
            return '<div class="pos-search-item" onclick="addPosProduct(' + p.id + ')">' +
                '<div><div class="pos-search-item-name">' + p.name + '</div><small style="color:var(--text-light)">' + (p.sku || p.barcode || '-') + '</small></div>' +
                '<div style="text-align:right;"><div class="pos-search-item-price">' + (salePrice || 0).toFixed(2) + ' DH</div><span class="badge badge-' + stockClass + '" style="font-size:0.65rem;">Stock: ' + stockLabel + '</span></div></div>';
        }).join('');
    }
    results.classList.add('active');
}

function addPosProductFromSearch() {
    var query = document.getElementById('posSearchInput').value.trim();
    if (query.length < 1) return;
    var byBarcode = products.find(function(p) { return p.barcode && p.barcode === query; });
    if (byBarcode) {
        addPosProduct(byBarcode.id);
    } else {
        var byName = products.filter(function(p) { return p.name.toLowerCase().indexOf(query.toLowerCase()) !== -1; });
        if (byName.length === 1) {
            addPosProduct(byName[0].id);
        } else if (byName.length > 1) {
            showError('Plusieurs produits trouves. Selectionnez un dans la liste.');
        } else {
            showError('Produit non trouve');
        }
    }
}

function addPosProduct(productId) {
    var product = products.find(function(p) { return p.id === productId; });
    if (!product) return;
    // Tarif normal = price_base (moyenne d'achat + 40%)
    var normalPrice = product.price_base > 0 ? product.price_base : product.price;
    
    // Lire la remise actuelle
    var discountSelect = document.getElementById('posDiscountType');
    var selectedValue = discountSelect ? discountSelect.value : 'auto';
    
    var customerSelect = document.getElementById('posCustomer');
    var customerId = customerSelect ? parseInt(customerSelect.value) : null;
    var customer = null;
    if (customerId && !isNaN(customerId)) {
        customer = customers.find(function(c) { return c.id === customerId; });
    }
    
    var discountPercent = 0;
    
    if (selectedValue === 'auto') {
        // Mode automatique selon client enregistré
        if (customer && (customer.is_loyal || customer.type === 'etudiant')) {
            discountPercent = 15;
        } else if (customer && customer.type === 'ecole') {
            discountPercent = 20;
        } else {
            discountPercent = 0; // Normal
        }
    } else if (selectedValue === 'fidele-comptoir' || selectedValue === 'etudiant-comptoir') {
        discountPercent = 15; // -15% pour fidele/etudiant comptoir
    } else if (selectedValue === 'ecole-comptoir') {
        discountPercent = 20; // -20% pour ecole comptoir
    } else {
        discountPercent = 0; // Normal (0%)
    }
    
    var unitPrice = normalPrice * (1 - discountPercent / 100);
    var existing = posCart.find(function(item) { return item.product_id === productId; });
    if (existing) {
        existing.quantity += 1;
    } else {
        posCart.push({
            product_id: productId,
            product_name: product.name,
            product_sku: product.sku,
            quantity: 1,
            base_price: normalPrice,
            unit_price: unitPrice,
            discount_percent: discountPercent
        });
    }
     renderPosCart();
 }
 
 function updatePosCartItemQty(productId, delta) {
    var item = posCart.find(function(i) { return i.product_id === productId; });
    if (!item) return;
    item.quantity += delta;
    if (item.quantity <= 0) {
        posCart = posCart.filter(function(i) { return i.product_id !== productId; });
    }
    renderPosCart();
}

function removePosCartItem(productId) {
    posCart = posCart.filter(function(i) { return i.product_id !== productId; });
    renderPosCart();
}

function updatePosCartItemPrice(productId, newPrice) {
    var item = posCart.find(function(i) { return i.product_id === productId; });
    if (item) {
        var newUnitPrice = parseFloat(newPrice) || 0;
        var oldBase = item.base_price || item.unit_price;
        item.unit_price = newUnitPrice;
        item.base_price = oldBase;
        if (oldBase > 0) {
            item.discount_percent = (1 - newUnitPrice / oldBase) * 100;
        }
        renderPosCart();
    }
}

function clearPosCart() {
    posCart = [];
    renderPosCart();
}

function renderPosCart() {
    var container = document.getElementById('posCartItems');
    if (posCart.length === 0) {
        container.innerHTML = '<div class="pos-cart-empty"><i class="fas fa-shopping-basket"></i><p>Aucun produit ajoute</p></div>';
        updatePosTotals(0, 0, 0);
        updatePosPayButton();
        return;
    }
    container.innerHTML = posCart.map(function(item) {
        var lineTotal = item.quantity * item.unit_price * (1 - item.discount_percent / 100);
        return '<div class="pos-cart-item">' +
            '<div class="pos-cart-item-qty">' +
            '<button onclick="updatePosCartItemQty(' + item.product_id + ', -1)">-</button>' +
            '<span>' + item.quantity + '</span>' +
            '<button onclick="updatePosCartItemQty(' + item.product_id + ', 1)">+</button></div>' +
            '<div class="pos-cart-item-name">' + item.product_name + '<br><small style="color:var(--text-light)">' + (item.product_sku || '') + '</small></div>' +
            '<input type="number" class="pos-cart-item-price-input" style="width:80px;padding:4px;border:1px solid var(--border);border-radius:4px;" value="' + item.unit_price.toFixed(2) + '" step="0.01" onchange="updatePosCartItemPrice(' + item.product_id + ', this.value)">' +
            '<i class="fas fa-trash pos-cart-item-remove" onclick="removePosCartItem(' + item.product_id + ')"></i></div>';
    }).join('');
    
    var subtotal = 0;
    var tax = 0;
    var discount = 0;
    posCart.forEach(function(item) {
        var baseTotal = item.quantity * (item.base_price || item.unit_price);
        var lineHt = item.quantity * item.unit_price;
        var lineTva = lineHt * 0.20;
        subtotal += baseTotal;
        tax += lineTva;
        discount += (item.base_price || item.unit_price) * item.quantity * (item.discount_percent / 100);
    });
    
    updatePosTotals(subtotal, tax, discount);
    updatePosPayButton();
}

function updatePosTotals(subtotal, tax, discount) {
    document.getElementById('posSubtotal').textContent = subtotal.toFixed(2) + ' DH';
    document.getElementById('posTax').textContent = tax.toFixed(2) + ' DH';
    document.getElementById('posDiscount').textContent = '-' + discount.toFixed(2) + ' DH';
    var total = subtotal + tax - discount;
    document.getElementById('posTotal').textContent = total.toFixed(2) + ' DH';
    if (posPaymentMethod === 'cash' && posTenderedAmount > 0) {
        var change = Math.max(0, posTenderedAmount - total);
        document.getElementById('posChange').textContent = change.toFixed(2) + ' DH';
    }
}

function updatePosPayButton() {
    var btn = document.querySelector('.pos-pay-btn');
    var total = parseFloat(document.getElementById('posTotal').textContent) || 0;
    btn.disabled = posCart.length === 0 || total <= 0 || !posSession;
}

function setPosPaymentMethod(method) {
    posPaymentMethod = method;
    document.querySelectorAll('.pos-payment-btn').forEach(function(btn) {
        btn.classList.toggle('active', btn.dataset.method === method);
    });
    document.getElementById('posCashInput').style.display = method === 'cash' ? 'block' : 'none';
    if (method === 'card' || method === 'mixed') {
        var total = parseFloat(document.getElementById('posTotal').textContent.replace(/[^0-9.]/g, '')) || 0;
        posTenderedAmount = total;
        document.getElementById('posTendered').value = total.toFixed(2);
        document.getElementById('posChange').textContent = '0.00 DH';
    } else if (method !== 'cash') {
        posTenderedAmount = 0;
        document.getElementById('posTendered').value = '';
        document.getElementById('posChange').textContent = '0.00 DH';
    }
}

function setPosTenderedQuick() {
    var total = parseFloat(document.getElementById('posTotal').textContent) || 0;
    document.getElementById('posTendered').value = total.toFixed(2);
    posTenderedAmount = total;
    calculatePosChange();
}

function setPosTenderedRound() {
    var total = parseFloat(document.getElementById('posTotal').textContent) || 0;
    var rounded = Math.ceil(total / 10) * 10;
    document.getElementById('posTendered').value = rounded.toFixed(2);
    posTenderedAmount = rounded;
    calculatePosChange();
}

function calculatePosChange() {
    var tendered = parseFloat(document.getElementById('posTendered').value) || 0;
    posTenderedAmount = tendered;
    var totalText = document.getElementById('posTotal').textContent;
    var total = parseFloat(totalText.replace(/[^0-9.]/g, '')) || 0;
    var change = Math.max(0, tendered - total);
    document.getElementById('posChange').textContent = change.toFixed(2) + ' DH';
}

async function processPosPayment() {
    if (!posSession || posCart.length === 0) return;
    var totalText = document.getElementById('posTotal').textContent;
    var total = parseFloat(totalText.replace(/[^0-9.]/g, '')) || 0;
    if (posPaymentMethod === 'card') {
        posTenderedAmount = total;
        document.getElementById('posTendered').value = total.toFixed(2);
    }
    if (posPaymentMethod === 'cash' && posTenderedAmount < total) {
        showError('Montant insuffisant');
        return;
    }
    try {
        var customerId = document.getElementById('posCustomer').value;
        var res = await fetch('/api/pos/transactions', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                session_id: posSession.id,
                customer_id: customerId ? parseInt(customerId) : null,
                items: posCart,
                payment_method: posPaymentMethod,
                tendered_amount: posTenderedAmount,
                notes: ''
            })
        });
        var data = await res.json();
        
        if (data.success) {
            var docType = data.document_type;
            var docNum = data.document_number;
            var msg = (docType === 'facture') 
                ? 'Facture generee: ' + docNum 
                : 'Ticket generee: ' + docNum;
            showError(msg);
            if (data.change_amount > 0) {
                alert('Monnaie a rendre: ' + data.change_amount.toFixed(2) + ' DH');
            }
            clearPosCart();
            posTenderedAmount = 0;
            document.getElementById('posTendered').value = '';
            loadInvoices();
            loadProducts();
            loadPosCashMovements();
            loadPosTransactions();
        } else if (data.insufficient_items) {
            // Afficher alerte stock insuffisant
            var msg = 'STOCK INSUFFISANT:\n\n';
            data.insufficient_items.forEach(function(item) {
                msg += '- ' + item.product_name + '\n';
                msg += '  Demandé: ' + item.requested + ' | Disponible: ' + item.available + '\n\n';
            });
            alert(msg);
            // Mettre a jour le panier avec les quantites disponibles
            data.insufficient_items.forEach(function(item) {
                var cartItem = posCart.find(function(i) { return i.product_id === item.product_id; });
                if (cartItem) {
                    cartItem.quantity = item.available;
                }
            });
            renderPosCart();
        } else {
            showError(data.error || 'Erreur transaction');
        }
    } catch(e) {
        showError('Erreur transaction');
    }
}

async function loadPosCustomers() {
    try {
        var res = await fetch('/api/customers');
        var data = await res.json();
        var select = document.getElementById('posCustomer');
        if (select) {
            select.innerHTML = '<option value="">Client au comptoir (sans facture)</option>' +
                data.map(function(c) { return '<option value="' + c.id + '">' + c.name + ' (' + (c.client_code || '-') + ')</option>'; }).join('');
        }
    } catch(e) { console.error(e); }
}

function onCustomerChange() {
    var discountSelect = document.getElementById('posDiscountType');
    var customerSelect = document.getElementById('posCustomer');
    var customerId = customerSelect ? parseInt(customerSelect.value) : null;
    
    if (!customerId) {
        // Client comptoir : disable "Automatique", default to "normal"
        if (discountSelect) {
            var autoOption = discountSelect.querySelector('option[value="auto"]');
            if (autoOption) autoOption.disabled = true;
            discountSelect.value = 'normal';
        }
    } else {
        // Client enregistré : réactiver "Automatique"
        if (discountSelect) {
            var autoOption = discountSelect.querySelector('option[value="auto"]');
            if (autoOption) autoOption.disabled = false;
            discountSelect.value = 'auto';
        }
    }
    applyPosDiscount();
}

function applyPosDiscount() {
    var discountSelect = document.getElementById('posDiscountType');
    var selectedValue = discountSelect ? discountSelect.value : 'auto';
    
    var customerSelect = document.getElementById('posCustomer');
    var customerId = customerSelect ? parseInt(customerSelect.value) : null;
    var customer = null;
    if (customerId && !isNaN(customerId)) {
        customer = customers.find(function(c) { return c.id === customerId; });
    }
    
    var discountPercent = 0;
    
    if (selectedValue === 'auto') {
        // Mode automatique selon client enregistré
        if (customer && (customer.is_loyal || customer.type === 'etudiant')) {
            discountPercent = 15;
        } else if (customer && customer.type === 'ecole') {
            discountPercent = 20;
        } else {
            discountPercent = 0; // Normal
        }
    } else if (selectedValue === 'fidele-comptoir' || selectedValue === 'etudiant-comptoir') {
        discountPercent = 15; // -15% pour fidele/etudiant comptoir
    } else if (selectedValue === 'ecole-comptoir') {
        discountPercent = 20; // -20% pour ecole comptoir
    } else {
        discountPercent = 0; // Normal (0%)
    }
    
    // Appliquer au panier
    posCart.forEach(function(item) {
        var basePrice = item.base_price || item.unit_price / (1 - (item.discount_percent || 0) / 100);
        item.unit_price = basePrice * (1 - discountPercent / 100);
        item.discount_percent = discountPercent;
    });
    
    renderPosCart();
}

function handlePosCashIn() {}

function formatReason(reason, note) {
    if (!reason) return 'Mouvement';
    if (reason === 'sale') return 'Vente';
    if (reason === 'change') return 'Monnaie rendu';
    if (reason === 'expense' && note) return note;
    if (reason === 'in') return 'Entrée';
    return reason;
}

async function loadPosCashMovements() {
    if (!posSession) return;
    try {
        var res = await fetch('/api/pos/cash-movements');
        var movements = await res.json();
        var container = document.getElementById('posCashMovementsList');
        
        var balance = 0;
        var opening = posSession.opening_cash || 0;
        movements.forEach(function(m) {
            if (m.type === 'in') balance += m.amount;
            else balance -= m.amount;
        });
        balance += opening;
        document.getElementById('posCashBalance').textContent = balance.toFixed(2) + ' DH';
        
        if (movements.length === 0) {
            container.innerHTML = '<p class="text-muted text-center">Aucun mouvement</p>';
        } else {
            container.innerHTML = movements.map(function(m) {
                var date = new Date(m.created_at);
                var timeStr = date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
                var dateStr = date.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' });
                
                var icon = 'coins';
                if (m.reason === 'sale') icon = 'shopping-cart';
                else if (m.reason === 'change') icon = 'receipt';
                else if (m.reason === 'expense') icon = 'coffee';
                
                return '<div class="pos-cash-movement-item ' + m.type + '">' +
                    '<div class="pos-cash-movement-info">' +
                    '<span class="pos-cash-movement-reason"><i class="fas fa-' + icon + '"></i> ' + formatReason(m.reason, m.note) + '</span>' +
                    '<span class="pos-cash-movement-time"><i class="fas fa-clock"></i> ' + dateStr + ' ' + timeStr + '</span></div>' +
                    '<span class="pos-cash-movement-amount ' + m.type + '">' + (m.type === 'in' ? '+' : '-') + m.amount.toFixed(2) + ' DH</span></div>';
            }).join('');
        }
    } catch(e) { console.error(e); }
}

async function loadPosTransactions() {
    if (!posSession) return;
    try {
        var res = await fetch('/api/pos/transactions/recent?session_id=' + posSession.id + '&limit=20');
        var transactions = await res.json();
        var container = document.getElementById('posTransactionsList');
        
        if (!transactions || transactions.length === 0) {
            container.innerHTML = '<p class="text-muted text-center">Aucune transaction</p>';
            return;
        }
        
        container.innerHTML = transactions.map(function(t) {
            var date = new Date(t.created_at);
            var timeStr = date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
            var dateStr = date.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' });
            var customer = t.customer_name || 'Client Comptoir';
            var methodIcon = t.payment_method === 'cash' ? '<i class="fas fa-money-bill-wave"></i>' : 
                            t.payment_method === 'card' ? '<i class="fas fa-credit-card"></i>' : 
                            '<i class="fas fa-wallet"></i>';
            var methodText = t.payment_method === 'cash' ? 'Especes' : 
                            t.payment_method === 'card' ? 'Carte' : 
                            'Mixed';
            
            return '<div class="pos-transaction-item">' +
                '<div class="pos-transaction-info">' +
                '<span class="pos-transaction-number"><i class="fas fa-receipt"></i> ' + (t.ticket_number || t.transaction_number || '-') + '</span>' +
                '<span class="pos-transaction-time"><i class="fas fa-clock"></i> ' + dateStr + ' ' + timeStr + ' | ' + customer + ' | ' + methodIcon + ' ' + methodText + '</span></div>' +
                '<span class="pos-transaction-total">' + t.total.toFixed(2) + ' DH</span></div>';
        }).join('');
    } catch(e) { console.error('Error loading transactions:', e); }
}

function handlePosCashOut() {}

async function savePosCashIn() {
    if (!posSession) {
        showError('Aucune session ouverte');
        return;
    }
    var reason = document.getElementById('posCashInReason').value;
    var amount = parseFloat(document.getElementById('posCashInAmount').value) || 0;
    if (!reason || amount <= 0) {
        showError('Selectionnez une raison et un montant');
        return;
    }
    try {
        var res = await fetch('/api/pos/cash-movements', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                type: 'in',
                amount: amount,
                reason: reason
            })
        });
        var data = await res.json();
        if (data.success) {
            document.getElementById('posCashInAmount').value = '';
            document.getElementById('posCashInReason').value = '';
            loadPosCashMovements();
            showError('Entree enregistree');
        }
    } catch(e) {
        showError('Erreur');
    }
}

async function savePosCashOut() {
    if (!posSession) {
        showError('Aucune session ouverte');
        return;
    }
    var reason = document.getElementById('posCashOutReason').value;
    var amount = parseFloat(document.getElementById('posCashOutAmount').value) || 0;
    if (!reason || amount <= 0) {
        showError('Selectionnez un motif et un montant');
        return;
    }
    try {
        var res = await fetch('/api/pos/cash-movements', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                type: 'out',
                amount: amount,
                reason: 'expense',
                note: reason
            })
        });
        var data = await res.json();
        if (data.success) {
            document.getElementById('posCashOutAmount').value = '';
            document.getElementById('posCashOutReason').value = '';
            loadPosCashMovements();
            showError('Sortie enregistree');
        }
    } catch(e) {
        showError('Erreur');
    }
}
