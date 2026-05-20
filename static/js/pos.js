let posSession = null;
let posCart = [];
let posPaymentMethod = 'cash';
let posTenderedAmount = 0;
let posCashMovementType = 'in';

async function loadPosSession() {
    try {
        const res = await fetch('/api/pos/sessions');
        const sessions = await res.json();
        const openSession = sessions.find(s => s.status === 'open');
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
        const res = await fetch('/api/pos/sessions', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ warehouse_id: 1, opening_cash: 0 })
        });
        const data = await res.json();
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
    const closingCash = prompt('Montant en caisse:', '0');
    if (closingCash === null) return;
    try {
        const res = await fetch('/api/pos/sessions/' + posSession.id + '/close', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ closing_cash: parseFloat(closingCash) || 0 })
        });
        const data = await res.json();
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
    const query = document.getElementById('posSearchInput').value.trim();
    const results = document.getElementById('posSearchResults');
    if (query.length < 2) {
        results.classList.remove('active');
        return;
    }
    const filtered = products.filter(p => {
        return p.name.toLowerCase().indexOf(query.toLowerCase()) !== -1 ||
               (p.sku && p.sku.toLowerCase().indexOf(query.toLowerCase()) !== -1) ||
               (p.barcode && p.barcode.toLowerCase().indexOf(query.toLowerCase()) !== -1);
    });
    if (filtered.length === 0) {
        results.innerHTML = '<div style="padding:0.75rem;color:var(--text-light);">Aucun produit trouve</div>';
    } else {
        results.innerHTML = filtered.slice(0, 10).map(p => {
            const salePrice = p.price_base > 0 ? p.price_base : p.price;
            const stockClass = p.quantity <= 0 ? 'danger' : (p.quantity <= p.min_quantity ? 'warning' : 'success');
            const stockLabel = p.quantity <= 0 ? 'Rupture' : p.quantity;
            return '<div class="pos-search-item" onclick="addPosProduct(' + p.id + ')">' +
                '<div><div class="pos-search-item-name">' + p.name + '</div><small style="color:var(--text-light)">' + (p.sku || p.barcode || '-') + '</small></div>' +
                '<div style="text-align:right;"><div class="pos-search-item-price">' + (salePrice || 0).toFixed(2) + ' DH</div><span class="badge badge-' + stockClass + '" style="font-size:0.65rem;">Stock: ' + stockLabel + '</span></div></div>';
        }).join('');
    }
    results.classList.add('active');
}

function addPosProductFromSearch() {
    const query = document.getElementById('posSearchInput').value.trim();
    if (query.length < 1) return;
    const byBarcode = products.find(p => p.barcode && p.barcode === query);
    if (byBarcode) {
        addPosProduct(byBarcode.id);
    } else {
        const byName = products.filter(p => p.name.toLowerCase().indexOf(query.toLowerCase()) !== -1);
        if (byName.length === 1) {
            addPosProduct(byName[0].id);
        } else if (byName.length > 1) {
            showError('Plusieurs produits trouves. Selectionnez un dans la liste.');
        } else {
            showError('Produit non trouve');
        }
    }
    document.getElementById('posSearchInput').value = '';
    document.getElementById('posSearchInput').focus();
}

function posTierPrice(product, tier, customer) {
    const base = product.price_base > 0 ? product.price_base : (product.price || 0);
    if (tier === 'auto') {
        if (customer) {
            if (customer.is_loyal || customer.type === 'fidele') {
                return product.price_loyal > 0 ? product.price_loyal : base;
            }
            if (customer.type === 'etudiant') {
                return product.price_student > 0 ? product.price_student : base;
            }
            if (customer.type === 'ecole') {
                return product.price_school > 0 ? product.price_school : base;
            }
        }
        return base;
    }
    if (tier === 'price_loyal') return product.price_loyal > 0 ? product.price_loyal : base;
    if (tier === 'price_student') return product.price_student > 0 ? product.price_student : base;
    if (tier === 'price_school') return product.price_school > 0 ? product.price_school : base;
    return base;
}

function addPosProduct(productId) {
    const product = products.find(p => p.id === productId);
    if (!product) return;
    const basePrice = product.price_base > 0 ? product.price_base : (product.price || 0);

    const discountSelect = document.getElementById('posDiscountType');
    const selectedValue = discountSelect ? discountSelect.value : 'auto';

    const customerSelect = document.getElementById('posCustomer');
    const customerId = customerSelect ? parseInt(customerSelect.value) : null;
    let customer = null;
    if (customerId && !isNaN(customerId)) {
        customer = customers.find(c => c.id === customerId);
    }

    const unitPrice = posTierPrice(product, selectedValue, customer);
    const discountPct = basePrice > 0 && unitPrice < basePrice ? Math.round((1 - unitPrice / basePrice) * 10000) / 100 : 0;

    const existing = posCart.find(item => item.product_id === productId);
    if (existing) {
        existing.quantity += 1;
    } else {
        posCart.push({
            product_id: productId,
            product_name: product.name,
            product_sku: product.sku,
            quantity: 1,
            base_price: basePrice,
            unit_price: unitPrice,
            discount_percent: discountPct
        });
    }
    renderPosCart();
}

 function updatePosCartItemQty(productId, delta) {
    const item = posCart.find(i => i.product_id === productId);
    if (!item) return;
    item.quantity += delta;
    if (item.quantity <= 0) {
        posCart = posCart.filter(i => i.product_id !== productId);
    }
    renderPosCart();
}

function removePosCartItem(productId) {
    posCart = posCart.filter(i => i.product_id !== productId);
    renderPosCart();
}

function updatePosCartItemPrice(productId, newPrice) {
    const item = posCart.find(i => i.product_id === productId);
    if (item) {
        const newUnitPrice = parseFloat(newPrice) || 0;
        const oldBase = item.base_price || item.unit_price;
        item.unit_price = newUnitPrice;
        item.base_price = oldBase;
        if (oldBase > 0) {
            item.discount_percent = Math.round((1 - newUnitPrice / oldBase) * 10000) / 100;
        }
        renderPosCart();
    }
}

function clearPosCart() {
    posCart = [];
    renderPosCart();
}

function renderPosCart() {
    const container = document.getElementById('posCartItems');
    if (posCart.length === 0) {
        container.innerHTML = '<div class="pos-cart-empty"><i class="fas fa-shopping-basket"></i><p>Aucun produit ajoute</p></div>';
        updatePosTotals(0, 0, 0);
        updatePosPayButton();
        return;
    }
    container.innerHTML = posCart.map(item => {
        return '<div class="pos-cart-item">' +
            '<div class="pos-cart-item-qty">' +
            '<button onclick="updatePosCartItemQty(' + item.product_id + ', -1)">-</button>' +
            '<span>' + item.quantity + '</span>' +
            '<button onclick="updatePosCartItemQty(' + item.product_id + ', 1)">+</button></div>' +
            '<div class="pos-cart-item-name">' + item.product_name + '<br><small style="color:var(--text-light)">' + (item.product_sku || '') + '</small></div>' +
            '<input type="number" class="pos-cart-item-price-input" style="width:80px;padding:4px;border:1px solid var(--border);border-radius:4px;" value="' + item.unit_price.toFixed(2) + '" step="0.01" onchange="updatePosCartItemPrice(' + item.product_id + ', this.value)">' +
            '<i class="fas fa-trash pos-cart-item-remove" onclick="removePosCartItem(' + item.product_id + ')"></i></div>';
    }).join('');

    let subtotal = 0;
    let tax = 0;
    let discount = 0;
    posCart.forEach(item => {
        const baseTotal = item.quantity * (item.base_price || item.unit_price);
        const lineHt = item.quantity * item.unit_price;
        const lineTva = lineHt * 0.20;
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
    document.getElementById('posDiscount').textContent = (discount > 0 ? '-' : '') + Math.abs(discount).toFixed(2) + ' DH';
    const total = subtotal + tax - discount;
    document.getElementById('posTotal').textContent = total.toFixed(2) + ' DH';
    if (posPaymentMethod === 'cash' && posTenderedAmount > 0) {
        const change = Math.max(0, posTenderedAmount - total);
        document.getElementById('posChange').textContent = change.toFixed(2) + ' DH';
    }
}

function updatePosPayButton() {
    const btn = document.querySelector('.pos-pay-btn');
    const total = parseFloat(document.getElementById('posTotal').textContent) || 0;
    btn.disabled = posCart.length === 0 || total <= 0 || !posSession;
}

function setPosPaymentMethod(method) {
    posPaymentMethod = method;
    document.querySelectorAll('.pos-payment-btn').forEach(btn => {
        btn.classList.toggle('active', btn.dataset.method === method);
    });
    document.getElementById('posCashInput').style.display = method === 'cash' ? 'block' : 'none';
    if (method === 'card' || method === 'mixed') {
        const total = parseFloat(document.getElementById('posTotal').textContent.replace(/[^0-9.]/g, '')) || 0;
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
    const total = parseFloat(document.getElementById('posTotal').textContent) || 0;
    document.getElementById('posTendered').value = total.toFixed(2);
    posTenderedAmount = total;
    calculatePosChange();
}

function setPosTenderedRound() {
    const total = parseFloat(document.getElementById('posTotal').textContent) || 0;
    const rounded = Math.ceil(total / 10) * 10;
    document.getElementById('posTendered').value = rounded.toFixed(2);
    posTenderedAmount = rounded;
    calculatePosChange();
}

function calculatePosChange() {
    const tendered = parseFloat(document.getElementById('posTendered').value) || 0;
    posTenderedAmount = tendered;
    const totalText = document.getElementById('posTotal').textContent;
    const total = parseFloat(totalText.replace(/[^0-9.]/g, '')) || 0;
    const change = Math.max(0, tendered - total);
    document.getElementById('posChange').textContent = change.toFixed(2) + ' DH';
}

async function processPosPayment() {
    if (!posSession || posCart.length === 0) return;
    const totalText = document.getElementById('posTotal').textContent;
    const total = parseFloat(totalText.replace(/[^0-9.]/g, '')) || 0;
    if (posPaymentMethod === 'card') {
        posTenderedAmount = total;
        document.getElementById('posTendered').value = total.toFixed(2);
    }
    if (posPaymentMethod === 'cash' && posTenderedAmount < total) {
        showError('Montant insuffisant');
        return;
    }
    try {
        const customerId = document.getElementById('posCustomer').value;
        const creditCheckbox = document.getElementById('posCreditCheckbox');
        const isCredit = creditCheckbox ? creditCheckbox.checked : false;
        const res = await fetch('/api/pos/transactions', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                session_id: posSession.id,
                customer_id: customerId ? parseInt(customerId) : null,
                items: posCart,
                payment_method: posPaymentMethod,
                tendered_amount: posTenderedAmount,
                notes: '',
                is_credit: isCredit
            })
        });
        const data = await res.json();

        if (data.success) {
            if (data.document_type === 'ticket') {
                window.open('/api/pos/tickets/' + data.document_number, 'ticket', 'width=400,height=700,scrollbars=yes');
            }
            const docType = data.document_type;
            const docNum = data.document_number;
            const docStatus = data.document_status;
            let msg = (docType === 'facture')
                ? (docStatus === 'envoyee' ? 'Facture credit generee (en attente): ' : 'Facture generee: ') + docNum
                : 'Ticket generee: ' + docNum;
            showError(msg);
            if (data.change_amount > 0) {
                alert('Monnaie a rendre: ' + data.change_amount.toFixed(2) + ' DH');
            }
            clearPosCart();
            posTenderedAmount = 0;
            document.getElementById('posTendered').value = '';
            if (creditCheckbox) creditCheckbox.checked = false;
            loadInvoices();
            loadProducts();
            loadPosCashMovements();
            loadPosTransactions();
        } else {
            showError(data.error || 'Erreur transaction');
        }
    } catch(e) {
        showError('Erreur transaction');
    }
}

async function loadPosCustomers() {
    try {
        const res = await fetch('/api/customers');
        const data = await res.json();
        const select = document.getElementById('posCustomer');
        if (select) {
            select.innerHTML = '<option value="">Client au comptoir (sans facture)</option>' +
                data.map(c => '<option value="' + c.id + '">' + c.name + ' (' + (c.client_code || '-') + ')</option>').join('');
        }
    } catch(e) { console.error(e); }
}

function onCustomerChange() {
    const discountSelect = document.getElementById('posDiscountType');
    const customerSelect = document.getElementById('posCustomer');
    const customerId = customerSelect ? parseInt(customerSelect.value) : null;
    const creditSection = document.getElementById('posCreditSection');
    const creditCheckbox = document.getElementById('posCreditCheckbox');

    if (!customerId) {
        if (discountSelect) {
            const autoOption = discountSelect.querySelector('option[value="auto"]');
            if (autoOption) autoOption.disabled = true;
            discountSelect.value = 'price_base';
        }
        if (creditSection) creditSection.style.display = 'none';
        if (creditCheckbox) creditCheckbox.checked = false;
    } else {
        if (discountSelect) {
            const autoOption = discountSelect.querySelector('option[value="auto"]');
            if (autoOption) autoOption.disabled = false;
            discountSelect.value = 'auto';
        }
        if (creditSection) creditSection.style.display = '';
    }
    applyPosDiscount();
}

function applyPosDiscount() {
    const discountSelect = document.getElementById('posDiscountType');
    const selectedValue = discountSelect ? discountSelect.value : 'auto';

    const customerSelect = document.getElementById('posCustomer');
    const customerId = customerSelect ? parseInt(customerSelect.value) : null;
    let customer = null;
    if (customerId && !isNaN(customerId)) {
        customer = customers.find(c => c.id === customerId);
    }

    posCart.forEach(item => {
        const product = products.find(p => p.id === item.product_id);
        if (!product) return;
        const basePrice = product.price_base > 0 ? product.price_base : (product.price || 0);
        const unitPrice = posTierPrice(product, selectedValue, customer);
        const discountPct = basePrice > 0 && unitPrice < basePrice ? Math.round((1 - unitPrice / basePrice) * 10000) / 100 : 0;
        item.base_price = basePrice;
        item.unit_price = unitPrice;
        item.discount_percent = discountPct;
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
        const res = await fetch('/api/pos/cash-movements');
        const movements = await res.json();
        const container = document.getElementById('posCashMovementsList');

        movements.sort((a, b) => new Date(a.created_at) - new Date(b.created_at));
        let balance = 0;
        const opening = posSession.opening_cash || 0;
        movements.forEach(m => {
            if (m.type === 'in') balance += m.amount;
            else balance -= m.amount;
        });
        balance += opening;
        document.getElementById('posCashBalance').textContent = balance.toFixed(2) + ' DH';

        if (movements.length === 0) {
            container.innerHTML = '<p class="text-muted text-center">Aucun mouvement</p>';
        } else {
            container.innerHTML = movements.map(m => {
                const date = new Date(m.created_at);
                const timeStr = date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
                const dateStr = date.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' });

                let icon = 'coins';
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
        const res = await fetch('/api/pos/transactions/recent?session_id=' + posSession.id + '&limit=20');
        const transactions = await res.json();
        const container = document.getElementById('posTransactionsList');

        if (!transactions || transactions.length === 0) {
            container.innerHTML = '<p class="text-muted text-center">Aucune transaction</p>';
            return;
        }

        container.innerHTML = transactions.map(t => {
            const date = new Date(t.created_at);
            const timeStr = date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
            const dateStr = date.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' });
            const customer = t.customer_name || 'Client Comptoir';
            const isInvoice = t.source === 'invoice';
            const methodIcon = t.payment_method === 'cash' ? '<i class="fas fa-money-bill-wave"></i>' :
                            t.payment_method === 'card' ? '<i class="fas fa-credit-card"></i>' :
                            '<i class="fas fa-wallet"></i>';
            const methodText = t.payment_method === 'cash' ? 'Especes' :
                            t.payment_method === 'card' ? 'Carte' :
                            'Mixed';
            const icon = isInvoice ? '<i class="fas fa-file-invoice"></i>' : '<i class="fas fa-receipt"></i>';
            const label = isInvoice ? 'Facture' : (t.ticket_number || t.transaction_number || '-');

            return '<div class="pos-transaction-item">' +
                '<div class="pos-transaction-info">' +
                '<span class="pos-transaction-number">' + icon + ' ' + label + '</span>' +
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
    const reason = document.getElementById('posCashInReason').value;
    const amount = parseFloat(document.getElementById('posCashInAmount').value) || 0;
    if (!reason || amount <= 0) {
        showError('Selectionnez une raison et un montant');
        return;
    }
    try {
        const res = await fetch('/api/pos/cash-movements', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                type: 'in',
                amount: amount,
                reason: reason
            })
        });
        const data = await res.json();
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
    const reason = document.getElementById('posCashOutReason').value;
    const amount = parseFloat(document.getElementById('posCashOutAmount').value) || 0;
    if (!reason || amount <= 0) {
        showError('Selectionnez un motif et un montant');
        return;
    }
    try {
        const res = await fetch('/api/pos/cash-movements', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                type: 'out',
                amount: amount,
                reason: 'expense',
                note: reason
            })
        });
        const data = await res.json();
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
