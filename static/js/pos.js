let posSession = null;
let posCart = [];
let posPaymentMethod = 'cash';
let posTenderedAmount = 0;
let posCashMovementType = 'in';
let posRegisterId = 1;
let posRegisters = [];
let posEventSource = null;
let posDocType = 'bon_de_livraison';

function escapeHtml(str) {
    if (str == null) return '';
    const div = document.createElement('div');
    div.textContent = String(str);
    return div.innerHTML;
}

const POS_REGISTER_KEY = 'stockpro_pos_register_id';
const POS_SESSION_KEY = 'stockpro_pos_session_id';

function posNotify(msg, type) {
    type = type || 'info';
    const container = document.getElementById('posNotifications') || (function() {
        const d = document.createElement('div');
        d.id = 'posNotifications';
        d.style.cssText = 'position:fixed;bottom:1rem;right:1rem;z-index:9999;display:flex;flex-direction:column;gap:0.5rem;max-width:400px;';
        document.body.appendChild(d);
        return d;
    })();
    const el = document.createElement('div');
    el.style.cssText = 'padding:0.75rem 1rem;border-radius:6px;color:#fff;font-size:0.9rem;box-shadow:0 4px 12px rgba(0,0,0,0.15);animation:fadeInUp 0.2s ease;word-break:break-word;';
    el.style.background = type === 'error' ? '#e74c3c' : type === 'success' ? '#27ae60' : '#3498db';
    el.textContent = msg;
    container.appendChild(el);
    setTimeout(function() { if (el.parentNode) el.remove(); }, 5000);
}

function posShowError(msg) { posNotify(msg, 'error'); }

function fetchWithTimeout(url, options, timeout) {
    timeout = timeout || 15000;
    if (!options) options = {};
    const controller = new AbortController();
    const id = setTimeout(function() { controller.abort(); }, timeout);
    options.signal = controller.signal;
    return fetch(url, options).then(function(res) {
        clearTimeout(id);
        return res;
    }).catch(function(err) {
        clearTimeout(id);
        throw err;
    });
}

function debounce(fn, delay) {
    var timer = null;
    return function() {
        var ctx = this, args = arguments;
        if (timer) clearTimeout(timer);
        timer = setTimeout(function() { fn.apply(ctx, args); }, delay);
    };
}

function btnLoading(btn, loading) {
    if (!btn) return;
    if (loading) {
        btn._orig = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>...';
    } else {
        btn.disabled = false;
        if (btn._orig) btn.innerHTML = btn._orig;
    }
}

async function loadPosRegisters() {
    try {
        const res = await fetchWithTimeout('/api/pos/registers');
        posRegisters = await res.json();
        const select = document.getElementById('posRegisterSelect');
        if (select && posRegisters.length > 0) {
            select.innerHTML = posRegisters.map(function(r) {
                return '<option value="' + r.id + '">' + escapeHtml(r.name) + '</option>';
            }).join('');
            var saved = localStorage.getItem(POS_REGISTER_KEY);
            if (saved) {
                var parsed = parseInt(saved);
                if (posRegisters.find(function(r) { return r.id === parsed; })) {
                    posRegisterId = parsed;
                }
            }
            if (!posRegisters.find(function(r) { return r.id === posRegisterId; })) {
                posRegisterId = posRegisters[0].id;
            }
            select.value = posRegisterId;
        }
        onRegisterChange();
    } catch(e) {
        console.error('Error loading registers:', e);
        posShowError('Erreur chargement caisses');
    }
}

function onRegisterChange() {
    var select = document.getElementById('posRegisterSelect');
    if (!select) return;
    posRegisterId = parseInt(select.value);
    localStorage.setItem(POS_REGISTER_KEY, String(posRegisterId));
    document.getElementById('posCurrentRegister').textContent =
        (posRegisters.find(function(r) { return r.id === posRegisterId; }) || {}).name || 'Caisse ' + posRegisterId;
    document.getElementById('posRegisterActions').style.display =
        posSession ? 'none' : '';
    loadPosSession(posRegisterId);
}

async function loadPosSession(registerId, retries) {
    retries = retries || 0;
    try {
        var url = registerId ? '/api/pos/sessions?register_id=' + registerId : '/api/pos/sessions';
        var res = await fetchWithTimeout(url);
        var sessions = await res.json();
        var openSession = sessions.find(function(s) { return s.status === 'open'; });
        if (openSession) {
            posSession = openSession;
            localStorage.setItem(POS_SESSION_KEY, String(openSession.id));
            document.getElementById('posSessionNumber').textContent = 'Session: ' + openSession.session_number;
            document.getElementById('posSessionStatus').textContent = 'Ouverte';
            document.getElementById('posSessionStatus').className = 'badge badge-success';
            document.getElementById('posRegisterActions').style.display = 'none';
            document.getElementById('posSessionActions').style.display = '';
            loadPosCashMovements();
            loadPosCustomers();
            loadPosTransactions();
            startSSE();
        } else {
            resetPosSession();
        }
    } catch(e) {
        console.error('Error loading POS session:', e);
        if (retries < 2) {
            setTimeout(function() { loadPosSession(registerId, retries + 1); }, 1000 * (retries + 1));
        } else {
            posShowError('Impossible de charger la session');
            resetPosSession();
        }
    }
}

function resetPosSession() {
    if (posSession) {
        stopSSE();
    }
    posSession = null;
    localStorage.removeItem(POS_SESSION_KEY);
    document.getElementById('posSessionNumber').textContent = 'Session: ---';
    document.getElementById('posSessionStatus').textContent = 'Fermee';
    document.getElementById('posSessionStatus').className = 'badge badge-warning';
    document.getElementById('posRegisterActions').style.display = '';
    document.getElementById('posSessionActions').style.display = 'none';
    document.getElementById('posTransactionsList').innerHTML = '<p class="text-muted text-center">Aucune transaction</p>';
}

function showOpenSessionModal() {
    document.getElementById('posCashierName').value = '';
    document.getElementById('posOpeningCash').value = '0';
    document.getElementById('passwordPromptError').style.display = 'none';
    openModal('openSessionModal');
    setTimeout(function() {
        document.getElementById('posCashierName').focus();
    }, 100);
}

async function openPosSession() {
    var btn = document.getElementById('btnConfirmOpenSession');
    btnLoading(btn, true);
    try {
        var cashierName = document.getElementById('posCashierName').value.trim();
        var openingCash = parseFloat(document.getElementById('posOpeningCash').value) || 0;
        if (!cashierName) {
            posShowError('Veuillez saisir le nom du caissier');
            btnLoading(btn, false);
            return;
        }
        if (openingCash < 0) {
            posShowError('Le montant initial ne peut pas être négatif');
            btnLoading(btn, false);
            return;
        }
        var res = await fetchWithTimeout('/api/pos/sessions', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                warehouse_id: 1,
                opening_cash: openingCash,
                register_id: posRegisterId,
                cashier_name: cashierName
            })
        });
        var data = await res.json();
        if (data.success) {
            closeModal('openSessionModal');
            showTab('pos');
            loadPosSession(posRegisterId);
            posNotify('Caisse ouverte: ' + data.session_number, 'success');
        } else {
            posShowError(data.error || 'Erreur ouverture caisse');
        }
    } catch(e) {
        if (e.name === 'AbortError') {
            posShowError('Timeout: serveur indisponible');
        } else {
            posShowError('Erreur ouverture caisse');
        }
    } finally {
        btnLoading(btn, false);
    }
}

function startSSE() {
    if (posEventSource) return;
    posEventSource = new EventSource('/api/events');
    posEventSource.addEventListener('stock-update', function(e) {
        try {
            var data = JSON.parse(e.data);
            checkCartConflict(data);
            loadProducts();
        } catch(ex) {}
    });
    posEventSource.addEventListener('transaction', function(e) {
        try {
            var data = JSON.parse(e.data);
            if (data.session_id !== (posSession ? posSession.id : null)) {
                loadPosCashMovements();
            }
        } catch(ex) {}
    });
    posEventSource.onerror = function() {
        setTimeout(startSSE, 3000);
    };
}

function stopSSE() {
    if (posEventSource) {
        posEventSource.close();
        posEventSource = null;
    }
}

function checkCartConflict(data) {
    var item = posCart.find(function(i) { return i.product_id === data.product_id; });
    if (!item) return;
    var product = products.find(function(p) { return p.id === data.product_id; });
    if (product && item.quantity > product.quantity) {
        var regName = data.register_name || 'Autre caisse';
        var overage = item.quantity - product.quantity;
        posShowError(regName + ' a vendu ' + (product.name || 'produit') +
            ' — stock restant: ' + product.quantity +
            (overage > 1 ? ' (excedent: ' + overage + ')' : ''));
    }
}

async function showCloseSessionModal() {
    if (!posSession) return;
    var btn = document.getElementById('btnCloseSession');
    btnLoading(btn, true);
    try {
        var res = await fetchWithTimeout('/api/pos/cash-movements?session_id=' + posSession.id);
        var movements = await res.json();
        var opening = posSession.opening_cash || 0;
        var totalIn = 0;
        var totalOut = 0;
        movements.forEach(function(m) {
            if (m.type === 'in') totalIn += m.amount;
            else totalOut += m.amount;
        });
        var expected = opening + totalIn - totalOut;
        document.getElementById('closeOpeningCash').textContent = opening.toFixed(2) + ' DH';
        document.getElementById('closeCashIn').textContent = totalIn.toFixed(2) + ' DH';
        document.getElementById('closeCashOut').textContent = totalOut.toFixed(2) + ' DH';
        document.getElementById('closeExpectedCash').textContent = expected.toFixed(2) + ' DH';
        document.getElementById('posClosingCash').value = expected > 0 ? expected.toFixed(2) : '0';
        document.getElementById('closeCashGap').style.display = 'none';
        document.getElementById('posDepositToMain').checked = true;
        openModal('closeSessionModal');
    } catch(e) {
        posShowError('Erreur chargement mouvements');
    } finally {
        btnLoading(btn, false);
    }
}

function updateCloseCashGap() {
    var expected = parseFloat(document.getElementById('closeExpectedCash').textContent) || 0;
    var actual = parseFloat(document.getElementById('posClosingCash').value) || 0;
    var gap = actual - expected;
    var el = document.getElementById('closeCashGap');
    if (Math.abs(gap) > 0.01) {
        el.style.display = 'block';
        el.style.background = gap < 0 ? 'rgba(231,76,60,0.15)' : 'rgba(46,204,113,0.15)';
        el.style.color = gap < 0 ? '#e74c3c' : '#27ae60';
        el.textContent = (gap < 0 ? 'Manque: ' : 'Excédent: ') + Math.abs(gap).toFixed(2) + ' DH';
    } else {
        el.style.display = 'none';
    }
}

async function closePosSession() {
    if (!posSession) return;
    var closingCash = parseFloat(document.getElementById('posClosingCash').value) || 0;
    var depositToMain = document.getElementById('posDepositToMain').checked;
    var btn = document.getElementById('btnConfirmCloseSession');
    btnLoading(btn, true);
    try {
        var res = await fetchWithTimeout('/api/pos/sessions/' + posSession.id + '/close', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ closing_cash: closingCash, deposit_to_main: depositToMain })
        });
        var data = await res.json();
        if (data.success) {
            closeModal('closeSessionModal');
            stopSSE();
            resetPosSession();
            clearPosCart();
            posNotify('Caisse fermee avec succes', 'success');
        } else {
            posShowError(data.error || 'Erreur fermeture');
        }
    } catch(e) {
        posShowError('Erreur fermeture caisse');
    } finally {
        btnLoading(btn, false);
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
                '<div><div class="pos-search-item-name">' + escapeHtml(p.name) + '</div><small style="color:var(--text-light)">' + escapeHtml(p.sku || p.barcode || '-') + '</small></div>' +
                '<div style="text-align:right;"><div class="pos-search-item-price">' + (salePrice || 0).toFixed(2) + ' DH</div><span class="badge badge-' + stockClass + '" style="font-size:0.65rem;">Stock: ' + stockLabel + '</span></div></div>';
        }).join('');
    }
    results.classList.add('active');
}

// Fix barcode scanner keyboard layout mismatch (QWERTY→AZERTY, etc.)
// Intercepts keydown on posSearchInput, maps physical digit key codes to
// correct digit characters, bypassing OS keyboard layout mapping
(function() {
    var input = document.getElementById('posSearchInput');
    if (!input) return;

    input.addEventListener('keydown', function(e) {
        var isDigit = e.code >= 'Digit0' && e.code <= 'Digit9';
        var isNumpad = e.code >= 'Numpad0' && e.code <= 'Numpad9';

        if (isDigit || isNumpad) {
            e.preventDefault();
            var digit = isDigit ? e.code[5] : e.code[6];
            var start = this.selectionStart || 0;
            var end = this.selectionEnd || 0;
            var val = this.value;
            this.value = val.substring(0, start) + digit + val.substring(end);
            var pos = start + 1;
            this.selectionStart = this.selectionEnd = pos;
            this.dispatchEvent(new Event('input', { bubbles: true }));
        }
    });
})();

async function addPosProductFromSearch() {
    var query = document.getElementById('posSearchInput').value.trim();
    if (query.length < 1) return;
    var found = products && products.find(function(p) { return p.barcode && p.barcode === query; });
    if (found) {
        addPosProduct(found.id);
    } else {
        try {
            var res = await fetch('/api/products/for-sale?search=' + encodeURIComponent(query));
            var apiResults = await res.json();
            if (apiResults && apiResults.length > 0) {
                addPosProduct(apiResults[0].id);
            } else {
                var byName = products && products.filter(function(p) { return p.name.toLowerCase().indexOf(query.toLowerCase()) !== -1; });
                if (byName && byName.length === 1) {
                    addPosProduct(byName[0].id);
                } else if (byName && byName.length > 1) {
                    posShowError('Plusieurs produits trouves. Selectionnez un dans la liste.');
                } else {
                    posShowError('Produit non trouve');
                }
            }
        } catch(e) {
            posShowError('Erreur lors de la recherche du produit');
        }
    }
    document.getElementById('posSearchInput').value = '';
    document.getElementById('posSearchInput').focus();
}

function posTierPrice(product, tier, customer) {
    var base = product.price > 0 ? product.price : (product.price_base || 0);
    if (tier === 'price_loyal') return product.price_loyal > 0 ? product.price_loyal : base;
    if (tier === 'price_gros') return product.price_gros > 0 ? product.price_gros : base;
    return base;
}

function addPosProduct(productId) {
    var product = products.find(function(p) { return p.id === productId; });
    if (!product) return;
    var basePrice = product.price > 0 ? product.price : (product.price_base || 0);
    var discountSelect = document.getElementById('posDiscountType');
    var selectedValue = discountSelect ? discountSelect.value : 'price';
    var unitPrice = posTierPrice(product, selectedValue, null);
    var discountPct = basePrice > 0 && unitPrice < basePrice ? Math.round((1 - unitPrice / basePrice) * 10000) / 100 : 0;
    var existing = posCart.find(function(item) { return item.product_id === productId; });
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
    var item = posCart.find(function(i) { return i.product_id === productId; });
    if (!item) return;
    item.quantity += delta;
    if (item.quantity <= 0) {
        posCart = posCart.filter(function(i) { return i.product_id !== productId; });
    }
    renderPosCart();
}

function setPosCartItemQty(productId, newQty) {
    var item = posCart.find(function(i) { return i.product_id === productId; });
    if (!item) return;
    var qty = parseInt(newQty) || 0;
    if (qty <= 0) {
        posCart = posCart.filter(function(i) { return i.product_id !== productId; });
    } else {
        item.quantity = qty;
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
    var container = document.getElementById('posCartItems');
    if (posCart.length === 0) {
        container.innerHTML = '<div class="pos-cart-empty"><i class="fas fa-shopping-basket"></i><p>Aucun produit ajoute</p></div>';
        updatePosTotals(0, 0, 0);
        updatePosPayButton();
        return;
    }
    container.innerHTML = posCart.map(function(item) {
        return '<div class="pos-cart-item">' +
            '<div class="pos-cart-item-qty">' +
            '<button onclick="updatePosCartItemQty(' + item.product_id + ', -1)">-</button>' +
            '<input type="number" class="pos-cart-item-qty-input" value="' + item.quantity + '" min="1" onchange="setPosCartItemQty(' + item.product_id + ', this.value)">' +
            '<button onclick="updatePosCartItemQty(' + item.product_id + ', 1)">+</button></div>' +
            '<div class="pos-cart-item-name">' + escapeHtml(item.product_name) + '<br><small style="color:var(--text-light)">' + escapeHtml(item.product_sku || '') + '</small></div>' +
            '<input type="number" class="pos-cart-item-price-input" style="width:80px;padding:4px;border:1px solid var(--border);border-radius:4px;" value="' + item.unit_price.toFixed(2) + '" step="0.01" onchange="updatePosCartItemPrice(' + item.product_id + ', this.value)">' +
            '<i class="fas fa-trash pos-cart-item-remove" onclick="removePosCartItem(' + item.product_id + ')"></i></div>';
    }).join('');
    var subtotal = 0, tax = 0;
    var applyTax = document.getElementById('posTvaToggle').checked;
    posCart.forEach(function(item) {
        var unitPrice = item.unit_price || 0;
        var lineHt = item.quantity * unitPrice;
        var lineTva = applyTax ? lineHt * 0.20 : 0;
        subtotal += lineHt;
        tax += lineTva;
    });
    updatePosTotals(subtotal, tax);
    updatePosPayButton();
}

function updatePosTotals(subtotal, tax, discount) {
    discount = discount || 0;
    var applyTax = document.getElementById('posTvaToggle').checked;
    document.getElementById('posSubtotal').textContent = subtotal.toFixed(2) + ' DH';
    document.getElementById('posTax').textContent = tax.toFixed(2) + ' DH';
    var discountRow = document.getElementById('posDiscount');
    if (discountRow) {
        var discountContainer = discountRow.parentElement;
        if (discount > 0) {
            discountRow.textContent = '-' + Math.abs(discount).toFixed(2) + ' DH';
            if (discountContainer) discountContainer.style.display = '';
        } else {
            if (discountContainer) discountContainer.style.display = 'none';
        }
    }
    var total = subtotal + tax - discount;
    document.getElementById('posTotal').textContent = total.toFixed(2) + ' DH';
    var totalLabel = document.querySelector('.pos-summary-total span:first-child');
    if (totalLabel) totalLabel.textContent = applyTax ? 'Total TTC:' : 'Total:';
    var posChangeEl = document.getElementById('posChange');
    if (posPaymentMethod === 'cash' && posTenderedAmount > 0 && posChangeEl) {
        var change = Math.max(0, posTenderedAmount - total);
        posChangeEl.textContent = change.toFixed(2) + ' DH';
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
    var posChangeEl = document.getElementById('posChange');
    if (method === 'card' || method === 'mixed') {
        var total = parseFloat(document.getElementById('posTotal').textContent.replace(/[^0-9.]/g, '')) || 0;
        posTenderedAmount = total;
        document.getElementById('posTendered').value = total.toFixed(2);
        if (posChangeEl) posChangeEl.textContent = '0.00 DH';
    } else if (method !== 'cash') {
        posTenderedAmount = 0;
        document.getElementById('posTendered').value = '';
        if (posChangeEl) posChangeEl.textContent = '0.00 DH';
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
    var changeEl = document.getElementById('posChange');
    if (changeEl) {
        var change = Math.max(0, tendered - total);
        changeEl.textContent = change.toFixed(2) + ' DH';
    }
}

async function processPosPayment() {
    if (!posSession || posCart.length === 0) return;
    var btn = document.querySelector('.pos-pay-btn');
    btnLoading(btn, true);
    var totalText = document.getElementById('posTotal').textContent;
    var total = parseFloat(totalText.replace(/[^0-9.]/g, '')) || 0;
    if (posPaymentMethod === 'card') {
        posTenderedAmount = total;
        document.getElementById('posTendered').value = total.toFixed(2);
    }
    var creditCheckbox = document.getElementById('posCreditCheckbox');
    var isCredit = creditCheckbox ? creditCheckbox.checked : false;
    if (posPaymentMethod === 'cash' && posTenderedAmount < total && !isCredit) {
        posShowError('Montant insuffisant');
        btnLoading(btn, false);
        return;
    }
    try {
        var customerId = document.getElementById('posCustomer').value;
        var discountSelect = document.getElementById('posDiscountType');
        var pricingTier = discountSelect ? discountSelect.value : 'price_base';
        var applyTax = document.getElementById('posTvaToggle').checked;
        var res = await fetchWithTimeout('/api/pos/transactions', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                session_id: posSession.id,
                customer_id: customerId ? parseInt(customerId) : null,
                items: posCart,
                payment_method: posPaymentMethod,
                tendered_amount: posTenderedAmount,
                pricing_tier: pricingTier,
                apply_tax: applyTax,
                notes: '',
                is_credit: isCredit,
                doc_type: customerId ? posDocType : undefined
            })
        });
        var data = await res.json();
        if (data.success) {
            if (data.document_type === 'ticket') {
                window.open('/api/pos/tickets/' + data.document_number, 'ticket', 'width=400,height=700,scrollbars=yes');
            }
            document.getElementById('lastTicketNumber').value = data.document_number;
            document.getElementById('btnReprintTicket').disabled = false;
            var docType = data.document_type;
            var docNum = data.document_number;
            var docStatus = data.document_status;
            var msg = (docType === 'bon_de_livraison')
                ? 'Bon de livraison généré: ' + docNum
                : (docType === 'facture')
                ? (docStatus === 'envoyee' ? 'Facture credit generee (en attente): '
                   : docStatus === 'partiellement_payee' ? 'Facture partiellement payee: '
                   : 'Facture payee: ') + docNum
                : 'Ticket genere: ' + docNum;
            posNotify(msg, 'success');
            if (data.print_status === 'success') {
                posNotify('Impression ticket reussie', 'success');
            } else if (data.print_status === 'error') {
                posNotify('Erreur impression: ' + (data.print_error || 'erreur inconnue'), 'error');
            }
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
            posShowError(data.error || 'Erreur transaction');
        }
    } catch(e) {
        if (e.name === 'AbortError') {
            posShowError('Timeout: transaction interrompue. Verifiez votre connexion.');
        } else {
            posShowError('Erreur transaction');
        }
    } finally {
        btnLoading(btn, false);
    }
}

function reprintTicket() {
    var tn = document.getElementById('lastTicketNumber').value;
    if (tn) {
        window.open('/api/pos/tickets/' + tn, 'ticket', 'width=400,height=700,scrollbars=yes');
    }
}

async function loadPosCustomers() {
    try {
        var res = await fetchWithTimeout('/api/customers');
        var data = await res.json();
        customers = data;
        var select = document.getElementById('posCustomer');
        if (select) {
            select.innerHTML = '<option value="">Client au comptoir (sans facture)</option>' +
                data.map(function(c) { return '<option value="' + c.id + '">' + escapeHtml(c.name) + ' (' + escapeHtml(c.client_code || '-') + ')</option>'; }).join('');
        }
    } catch(e) {
        console.error(e);
    }
}

function onCustomerChange() {
    var discountSelect = document.getElementById('posDiscountType');
    var customerSelect = document.getElementById('posCustomer');
    var customerId = customerSelect ? parseInt(customerSelect.value) : null;
    var creditSection = document.getElementById('posCreditSection');
    var creditCheckbox = document.getElementById('posCreditCheckbox');
    var docTypeSection = document.getElementById('posDocTypeSection');
    var customer = customerId && !isNaN(customerId) ? customers.find(function(c) { return c.id === customerId; }) : null;
    if (customer) {
        if (customer.type === 'fidele') discountSelect.value = 'price_loyal';
        else if (customer.type === 'gros') discountSelect.value = 'price_gros';
        if (creditSection) creditSection.style.display = '';
        if (docTypeSection) docTypeSection.style.display = 'block';
        posDocType = 'bon_de_livraison';
        setPosDocType('bon_de_livraison');
    } else {
        discountSelect.value = 'price';
        if (creditSection) creditSection.style.display = 'none';
        if (creditCheckbox) creditCheckbox.checked = false;
        if (docTypeSection) docTypeSection.style.display = 'none';
    }
    applyPosDiscount();
}

function setPosDocType(type) {
    posDocType = type;
    var btnBL = document.getElementById('btnBL');
    var btnFacture = document.getElementById('btnFacture');
    if (btnBL) btnBL.className = type === 'bon_de_livraison' ? 'btn btn-sm btn-primary' : 'btn btn-sm btn-outline';
    if (btnFacture) btnFacture.className = type === 'facture' ? 'btn btn-sm btn-primary' : 'btn btn-sm btn-outline';
}

function applyPosDiscount() {
    var discountSelect = document.getElementById('posDiscountType');
    var selectedValue = discountSelect ? discountSelect.value : 'price';
    posCart.forEach(function(item) {
        var product = products.find(function(p) { return p.id === item.product_id; });
        if (!product) return;
        var basePrice = product.price > 0 ? product.price : (product.price_base || 0);
        var unitPrice = posTierPrice(product, selectedValue, null);
        var discountPct = basePrice > 0 && unitPrice < basePrice ? Math.round((1 - unitPrice / basePrice) * 10000) / 100 : 0;
        item.base_price = basePrice;
        item.unit_price = unitPrice;
        item.discount_percent = discountPct;
    });
    renderPosCart();
}

function handlePosCashIn() {}
function handlePosCashOut() {}

function formatReason(reason, note) {
    if (!reason) return 'Mouvement';
    if (reason === 'sale') return 'Vente';
    if (reason === 'change') return 'Monnaie rendu';
    if (reason === 'expense' && note) return note;
    if (reason === 'in') return 'Entree';
    return reason;
}

async function loadPosCashMovements() {
    try {
        var container = document.getElementById('posCashMovementsList');
        container.innerHTML = '<p class="text-muted text-center"><i class="fas fa-spinner fa-spin"></i> Chargement...</p>';
        var movements = [];
        var balance = 0;
        if (posSession && posSession.id) {
            var res = await fetchWithTimeout('/api/pos/cash-movements?session_id=' + posSession.id);
            movements = await res.json();
            var opening = posSession.opening_cash || 0;
            movements.forEach(function(m) {
                if (m.type === 'in') balance += m.amount;
                else balance -= m.amount;
            });
            balance += opening;
        }
        document.getElementById('posCashBalance').textContent =
            posSession ? balance.toFixed(2) + ' DH' : '---';
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
                var registerTag = m.register_name ? '<span class="badge badge-info" style="margin-right:0.25rem;">' + escapeHtml(m.register_name) + '</span>' : '';
                return '<div class="pos-cash-movement-item ' + m.type + '">' +
                    '<div class="pos-cash-movement-info">' +
                    '<span class="pos-cash-movement-reason">' + registerTag + '<i class="fas fa-' + icon + '"></i> ' + escapeHtml(formatReason(m.reason, m.note)) + '</span>' +
                    '<span class="pos-cash-movement-time"><i class="fas fa-clock"></i> ' + dateStr + ' ' + timeStr + '</span></div>' +
                    '<span class="pos-cash-movement-amount ' + m.type + '">' + (m.type === 'in' ? '+' : '-') + m.amount.toFixed(2) + ' DH</span></div>';
            }).join('');
        }
    } catch(e) { console.error(e); }
}

async function loadPosTransactions() {
    try {
        var container = document.getElementById('posTransactionsList');
        container.innerHTML = '<p class="text-muted text-center"><i class="fas fa-spinner fa-spin"></i> Chargement...</p>';
        var url = '/api/pos/transactions/recent?limit=50';
        if (typeof posSession !== 'undefined' && posSession && posSession.id) {
            url += '&session_id=' + posSession.id;
        }
        var res = await fetchWithTimeout(url);
        var transactions = await res.json();
        if (!transactions || transactions.length === 0) {
            container.innerHTML = '<p class="text-muted text-center">Aucune transaction</p>';
            return;
        }
        container.innerHTML = transactions.map(function(t) {
            var date = new Date(t.created_at);
            var timeStr = date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
            var dateStr = date.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' });
            var customer = t.customer_name || 'Client Comptoir';
            var isInvoice = t.source === 'invoice';
            var methodIcon = t.payment_method === 'cash' ? '<i class="fas fa-money-bill-wave"></i>' :
                            t.payment_method === 'card' ? '<i class="fas fa-credit-card"></i>' :
                            '<i class="fas fa-wallet"></i>';
            var methodText = t.payment_method === 'cash' ? 'Especes' :
                            t.payment_method === 'card' ? 'Carte' : 'Mixed';
            var icon = isInvoice ? '<i class="fas fa-file-invoice"></i>' : '<i class="fas fa-receipt"></i>';
            var label = isInvoice ? 'Facture' : (t.ticket_number || t.transaction_number || '-');
            var registerTag = t.register_name ? '<span class="badge badge-info" style="margin-right:0.25rem;">' + escapeHtml(t.register_name) + '</span>' : '';
            return '<div class="pos-transaction-item">' +
                '<div class="pos-transaction-info">' +
                '<span class="pos-transaction-number">' + registerTag + icon + ' ' + escapeHtml(label) + '</span>' +
                '<span class="pos-transaction-time"><i class="fas fa-clock"></i> ' + dateStr + ' ' + timeStr + ' | ' + escapeHtml(customer) + ' | ' + methodIcon + ' ' + escapeHtml(methodText) + '</span></div>' +
                '<span class="pos-transaction-total">' + t.total.toFixed(2) + ' DH</span></div>';
        }).join('');
    } catch(e) { console.error('Error loading transactions:', e); }
}

async function savePosCashIn() {
    if (!posSession) { posShowError('Aucune session ouverte'); return; }
    var reason = document.getElementById('posCashInReason').value;
    var amount = parseFloat(document.getElementById('posCashInAmount').value) || 0;
    if (!reason || amount <= 0) { posShowError('Selectionnez une raison et un montant'); return; }
    var btn = document.querySelector('.pos-cash-inline-row .btn-success');
    btnLoading(btn, true);
    try {
        var res = await fetchWithTimeout('/api/pos/cash-movements', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ type: 'in', amount: amount, reason: reason, session_id: posSession.id })
        });
        var data = await res.json();
        if (data.success) {
            document.getElementById('posCashInAmount').value = '';
            document.getElementById('posCashInReason').value = '';
            loadPosCashMovements();
            posNotify('Entree enregistree', 'success');
        } else {
            posShowError(data.error || 'Erreur');
        }
    } catch(e) { posShowError('Erreur'); }
    finally { btnLoading(btn, false); }
}

async function savePosCashOut() {
    if (!posSession) { posShowError('Aucune session ouverte'); return; }
    var reason = document.getElementById('posCashOutReason').value;
    var amount = parseFloat(document.getElementById('posCashOutAmount').value) || 0;
    if (!reason || amount <= 0) { posShowError('Selectionnez un motif et un montant'); return; }
    var btn = document.querySelector('.pos-cash-inline-row .btn-danger');
    btnLoading(btn, true);
    try {
        var res = await fetchWithTimeout('/api/pos/cash-movements', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ type: 'out', amount: amount, reason: 'expense', note: reason, session_id: posSession.id })
        });
        var data = await res.json();
        if (data.success) {
            document.getElementById('posCashOutAmount').value = '';
            document.getElementById('posCashOutReason').value = '';
            loadPosCashMovements();
            posNotify('Sortie enregistree', 'success');
        } else {
            posShowError(data.error || 'Erreur');
        }
    } catch(e) { posShowError('Erreur'); }
    finally { btnLoading(btn, false); }
}
