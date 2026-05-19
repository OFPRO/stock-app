let scannerReader = null;
let scannerActive = false;
let scannerPaused = false;
let scannerStream = null;
let scanHistory = [];
let scannerAbort = null;
let scannerAudioCtx = null;
let scannerCanvasObs = null;
let lastDetectedCode = '';
let lastDetectedTime = 0;
let clearBoxTimer = null;
const DEDUP_MS = 1500;
const MAX_ERRORS_BEFORE_RESET = 8;
let scannerErrorCount = 0;

function id(el) { return document.getElementById(el); }

function resizeCanvas() {
    const canvas = id('scannerCanvas');
    if (!canvas) return;
    const parent = canvas.parentElement;
    const w = parent.clientWidth;
    const h = parent.clientHeight;
    if (canvas.width !== w || canvas.height !== h) {
        canvas.width = w;
        canvas.height = h;
    }
}

function clearCanvas() {
    const canvas = id('scannerCanvas');
    if (!canvas) return;
    resizeCanvas();
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);
}

function drawBoundingBox(points, video) {
    const canvas = id('scannerCanvas');
    if (!canvas || !points || points.length === 0) return;
    resizeCanvas();
    const ctx = canvas.getContext('2d');
    const vw = video.videoWidth;
    const vh = video.videoHeight;
    if (!vw || !vh) return;
    const scaleX = canvas.width / vw;
    const scaleY = canvas.height / vh;
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.strokeStyle = '#10b981';
    ctx.lineWidth = 3;
    ctx.shadowColor = '#10b981';
    ctx.shadowBlur = 8;
    ctx.fillStyle = 'rgba(16, 185, 129, 0.08)';
    if (points.length === 2) {
        const x1 = points[0].getX() * scaleX;
        const y1 = points[0].getY() * scaleY;
        const x2 = points[1].getX() * scaleX;
        const y2 = points[1].getY() * scaleY;
        const pad = 10;
        const left = Math.min(x1, x2) - pad;
        const top = Math.min(y1, y2) - pad;
        const w = Math.max(x1, x2) - left + pad;
        const h = Math.max(y1, y2) - top + pad;
        ctx.strokeRect(left, top, w, h);
        ctx.fillRect(left, top, w, h);
    } else if (points.length >= 3) {
        ctx.beginPath();
        ctx.moveTo(points[0].getX() * scaleX, points[0].getY() * scaleY);
        for (let i = 1; i < points.length; i++) {
            ctx.lineTo(points[i].getX() * scaleX, points[i].getY() * scaleY);
        }
        ctx.closePath();
        ctx.stroke();
        ctx.fill();
    }
}

function getAudioCtx() {
    if (!scannerAudioCtx) {
        try {
            scannerAudioCtx = new (window.AudioContext || window.webkitAudioContext)();
        } catch (e) {
            return null;
        }
    }
    if (scannerAudioCtx.state === 'suspended') {
        scannerAudioCtx.resume();
    }
    return scannerAudioCtx;
}

function playScanSound() {
    try {
        const ctx = getAudioCtx();
        if (!ctx) return;
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.frequency.value = 1200;
        osc.type = 'sine';
        gain.gain.setValueAtTime(0.25, ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.12);
        osc.start(ctx.currentTime);
        osc.stop(ctx.currentTime + 0.12);
    } catch (e) { /* audio not supported */ }
}

function startScanner(videoElementId, resultCallback) {
    if (scannerActive) return;
    if (typeof ZXing === 'undefined') {
        showError('La bibliothèque de scan n\'est pas chargée.');
        return;
    }
    const video = id(videoElementId);
    if (!video) return;

    const placeholder = id('scannerPlaceholder');

    if (!navigator.mediaDevices || !navigator.mediaDevices.enumerateDevices) {
        setScannerStatus('Navigateur non compatible avec la caméra.', 'error');
        return;
    }

    setScannerStatus('Demande d\'accès à la caméra...', 'info');
    if (placeholder) placeholder.style.display = 'none';

    const hints = new Map();
    hints.set(ZXing.DecodeHintType.POSSIBLE_FORMATS, [
        ZXing.BarcodeFormat.EAN_13,
        ZXing.BarcodeFormat.EAN_8,
        ZXing.BarcodeFormat.CODE_128,
        ZXing.BarcodeFormat.CODE_39,
        ZXing.BarcodeFormat.CODE_93,
        ZXing.BarcodeFormat.ITF,
        ZXing.BarcodeFormat.CODABAR,
        ZXing.BarcodeFormat.QR_CODE,
        ZXing.BarcodeFormat.DATA_MATRIX,
    ]);

    const codeReader = new ZXing.BrowserMultiFormatReader(hints, 500);
    scannerReader = codeReader;

    const abortCtrl = new AbortController();
    scannerAbort = abortCtrl;

    navigator.mediaDevices.enumerateDevices()
        .then(devices => {
            if (abortCtrl.signal.aborted) return;

            const videoInputs = devices.filter(d => d.kind === 'videoinput');
            let deviceId = null;

            const backCamera = videoInputs.find(d => {
                const label = (d.label || '').toLowerCase();
                return label.includes('back') || label.includes('environment') || label.includes('arrière');
            });
            if (backCamera) {
                deviceId = backCamera.deviceId;
            } else if (videoInputs.length > 0) {
                deviceId = videoInputs[0].deviceId;
            }

            setScannerStatus('Caméra détectée, démarrage...', 'info');

            video.addEventListener('playing', function onPlaying() {
                video.removeEventListener('playing', onPlaying);
                scannerStream = video.srcObject;
                resizeCanvas();
                if (scannerCanvasObs) scannerCanvasObs.disconnect();
                try {
                    scannerCanvasObs = new ResizeObserver(function () { resizeCanvas(); });
                    scannerCanvasObs.observe(video.parentElement);
                } catch (e) { /* ResizeObserver not supported */ }
            });

            let scanCount = 0;
            return codeReader.decodeFromVideoDevice(deviceId, video, (result, err) => {
                try {
                    if (abortCtrl.signal.aborted) return;
                    if (scannerPaused) return;
                    if (result) {
                        scannerErrorCount = 0;
                        const barcode = result.getText();
                        const format = result.getBarcodeFormat();
                        const points = result.getResultPoints();
                        const now = Date.now();

                        if (clearBoxTimer) {
                            clearTimeout(clearBoxTimer);
                            clearBoxTimer = null;
                        }
                        drawBoundingBox(points, video);
                        clearBoxTimer = setTimeout(function () { clearCanvas(); }, 500);

                        if (barcode !== lastDetectedCode || now - lastDetectedTime > DEDUP_MS) {
                            lastDetectedCode = barcode;
                            lastDetectedTime = now;
                            playScanSound();
                            if (resultCallback) {
                                try {
                                    resultCallback(barcode, format);
                                } catch (e) {
                                    console.error('resultCallback error:', e);
                                }
                            }
                        }
                    }
                    if (err) {
                        var isNotFound = false;
                        if (typeof ZXing.NotFoundException === 'function') {
                            isNotFound = err instanceof ZXing.NotFoundException;
                        } else if (err.name === 'NotFoundException' || (err.message && err.message.indexOf('NotFound') !== -1)) {
                            isNotFound = true;
                        }
                        if (isNotFound) {
                            scannerErrorCount = 0;
                        } else {
                            scannerErrorCount++;
                            if (scannerErrorCount >= MAX_ERRORS_BEFORE_RESET && scannerReader) {
                                scannerReader.reset();
                                scannerErrorCount = 0;
                                setScannerStatus('Redémarrage du lecteur...', 'info');
                            }
                            scanCount++;
                            if (scanCount <= 3) {
                                console.warn('Scan error:', err);
                            }
                        }
                    }
                } catch (e) {
                    console.error('Scanner callback crashed:', e);
                    if (scannerReader) {
                        try { scannerReader.reset(); } catch (r) { /* ignore */ }
                    }
                }
            });
        })
        .then(() => {
            if (abortCtrl.signal.aborted) return;
            scannerActive = true;
            setScannerStatus('Prêt — placez un code-barres devant la caméra', 'success');
            showScannerControls('started');
        })
        .catch(err => {
            if (abortCtrl.signal.aborted) return;
            console.error('Scanner error:', err);
            if (err.name === 'NotAllowedError' || (err.message && err.message.includes('Permission'))) {
                setScannerStatus('Autorisation caméra refusée. Vérifiez les permissions de votre navigateur.', 'error');
            } else if (err.name === 'NotFoundError') {
                setScannerStatus('Aucune caméra trouvée.', 'error');
            } else {
                setScannerStatus('Erreur caméra: ' + (err.message || 'inconnue'), 'error');
            }
            if (placeholder) placeholder.style.display = '';
            scannerActive = false;
            scannerReader = null;
        });
}

function showScannerControls(state) {
    const s = id('btnStartScanner');
    const p = id('btnPauseScanner');
    const st = id('btnStopScanner');
    const ov = id('scannerOverlay');
    if (state === 'started') {
        if (s) s.style.display = 'none';
        if (p) { p.style.display = ''; p.innerHTML = '<i class="fas fa-pause"></i> Pause'; }
        if (st) st.style.display = '';
        if (ov) ov.style.display = '';
    } else {
        if (s) s.style.display = '';
        if (p) { p.style.display = 'none'; p.innerHTML = '<i class="fas fa-pause"></i> Pause'; }
        if (st) st.style.display = 'none';
        if (ov) ov.style.display = 'none';
    }
}

function stopScanner() {
    if (clearBoxTimer) {
        clearTimeout(clearBoxTimer);
        clearBoxTimer = null;
    }
    if (scannerCanvasObs) {
        scannerCanvasObs.disconnect();
        scannerCanvasObs = null;
    }
    if (scannerAbort) {
        scannerAbort.abort();
        scannerAbort = null;
    }
    if (scannerReader) {
        try { scannerReader.reset(); } catch (e) { /* ignore */ }
        scannerReader = null;
    }
    if (scannerStream) {
        try {
            if (typeof scannerStream.getTracks === 'function') {
                scannerStream.getTracks().forEach(function (t) { t.stop(); });
            }
        } catch (e) { /* ignore */ }
        scannerStream = null;
    }
    scannerActive = false;
    scannerPaused = false;

    const video = id('scannerVideo');
    if (video) video.srcObject = null;

    clearCanvas();
    showScannerControls('stopped');
    setScannerStatus('Scanner arrêté', 'info');
}

function pauseScanner() {
    if (!scannerActive) return;
    scannerPaused = !scannerPaused;
    updatePauseButton();
    if (scannerPaused) {
        setScannerStatus('En pause', 'info');
        clearCanvas();
    } else {
        setScannerStatus('Prêt', 'success');
    }
}

function updatePauseButton() {
    const btn = id('btnPauseScanner');
    if (!btn) return;
    if (scannerPaused) {
        btn.innerHTML = '<i class="fas fa-play"></i> Reprendre';
    } else {
        btn.innerHTML = '<i class="fas fa-pause"></i> Pause';
    }
}

function setScannerStatus(msg, type) {
    const el = id('scannerStatus');
    if (!el) return;
    el.textContent = msg;
    el.className = 'scanner-status ' + (type || 'info');
}

function onScanSuccess(barcode, format) {
    const inp = id('scanInput');
    if (inp) inp.value = barcode;
    fetchProductByBarcode(barcode, format);
}

function searchByBarcode() {
    const input = id('scanInput');
    if (!input) return;
    const code = input.value.trim();
    if (!code) return;
    fetchProductByBarcode(code, null);
}

async function fetchProductByBarcode(code, format) {
    const resultEl = id('scanResult');
    if (!resultEl) return;

    resultEl.innerHTML = '<div class="scanner-result-loading"><i class="fas fa-spinner fa-spin"></i><p>Recherche...</p></div>';

    const controller = new AbortController();
    const timeout = setTimeout(function () { controller.abort(); }, 10000);

    try {
        const res = await fetch('/api/products/for-sale?search=' + encodeURIComponent(code), {
            signal: controller.signal
        });
        clearTimeout(timeout);
        if (!res.ok) throw new Error('Erreur ' + res.status);
        const data = await res.json();

        if (data && data.length > 0) {
            displayScanResult(data[0], code, format);
            addScanHistory(code, format, data[0]);
        } else {
            displayScanNotFound(code, format);
            addScanHistory(code, format, null);
        }
    } catch (err) {
        clearTimeout(timeout);
        console.error('Search error:', err);
        if (err.name === 'AbortError') {
            resultEl.innerHTML = '<div class="scanner-result-error"><i class="fas fa-clock"></i><p>Délai de recherche dépassé</p></div>';
        } else {
            resultEl.innerHTML = '<div class="scanner-result-error"><i class="fas fa-exclamation-triangle"></i><p>Erreur de recherche</p></div>';
        }
    }
}

function displayScanResult(product, code, format) {
    const el = id('scanResult');
    if (!el) return;

    const discountEl = id('posDiscountType');
    let price = Number(product.price) || 0;
    if (discountEl) {
        const discountType = discountEl.value;
        if (discountType === 'fidele-comptoir' && product.price_loyal) price = Number(product.price_loyal);
        else if (discountType === 'etudiant-comptoir' && product.price_student) price = Number(product.price_student);
        else if (discountType === 'ecole-comptoir' && product.price_school) price = Number(product.price_school);
    }
    price = Number(price.toFixed(2));

    const safeId = Number(product.id);
    const safeName = escapeHtml(product.name);
    const safeCode = escapeHtml(code);
    const safeFormat = format ? escapeHtml(format) : '';
    const stockQty = Number(product.quantity) || 0;

    el.innerHTML = '<div class="scanner-result-found">' +
        '<div class="scanner-result-icon"><i class="fas fa-check-circle"></i></div>' +
        '<div class="scanner-result-info">' +
            '<h4>' + safeName + '</h4>' +
            '<div class="scanner-result-details">' +
                '<span><strong>Code:</strong> ' + safeCode + '</span>' +
                (format ? '<span><strong>Format:</strong> ' + safeFormat + '</span>' : '') +
                '<span><strong>Prix:</strong> ' + price.toFixed(2) + ' DH</span>' +
                '<span><strong>Stock:</strong> ' + stockQty + '</span>' +
            '</div>' +
        '</div>' +
        '<button class="btn btn-sm btn-success" data-product-id="' + safeId + '" data-product-price="' + price + '" data-click="add-scanned-to-cart">' +
            '<i class="fas fa-cart-plus"></i> Ajouter au panier' +
        '</button>' +
    '</div>';
}

function displayScanNotFound(code, format) {
    const el = id('scanResult');
    if (!el) return;
    el.innerHTML = '<div class="scanner-result-notfound">' +
        '<i class="fas fa-times-circle"></i>' +
        '<h4>Produit non trouvé</h4>' +
        '<p>Code: ' + escapeHtml(code) + '</p>' +
        (format ? '<p>Format: ' + escapeHtml(format) + '</p>' : '') +
        '<p class="text-muted">Vérifiez le code-barres ou ajoutez le produit manuellement</p>' +
    '</div>';
}

function handleAddScannedToCart(e) {
    const btn = e.target.closest('[data-product-id]');
    if (!btn) return;
    const id = Number(btn.getAttribute('data-product-id'));
    const price = Number(btn.getAttribute('data-product-price')) || 0;
    if (!id) return;
    stopScanner();
    showTab('pos');
    if (typeof addPosProduct === 'function') {
        addPosProduct(id, '', price, 1);
    }
}

function addScanHistory(code, format, product) {
    var productId = product ? Number(product.id) : null;
    var now = new Date();
    var date = now.toLocaleDateString('fr-FR');
    var time = now.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });

    for (var i = 0; i < scanHistory.length; i++) {
        if (scanHistory[i].code === code && scanHistory[i].productId === productId) {
            scanHistory[i].quantity = (scanHistory[i].quantity || 1) + 1;
            scanHistory[i].date = date;
            scanHistory[i].time = time;
            var entry = scanHistory.splice(i, 1)[0];
            scanHistory.unshift(entry);
            renderScanHistory();
            return;
        }
    }

    var entry = {
        code: code,
        format: format || '\u2014',
        product: product ? product.name : 'Non trouvé',
        productId: productId,
        quantity: 1,
        date: date,
        time: time
    };
    scanHistory.unshift(entry);
    if (scanHistory.length > 50) scanHistory.pop();
    renderScanHistory();
}

function renderScanHistory() {
    const el = id('scanHistory');
    const btn = id('btnClearHistory');
    if (!el) return;

    if (scanHistory.length === 0) {
        el.innerHTML = '<p class="text-muted text-center" style="padding:2rem;">Aucun scan</p>';
        if (btn) btn.style.display = 'none';
        return;
    }

    if (btn) btn.style.display = '';
    var items = [];
    for (var i = 0; i < scanHistory.length; i++) {
        var entry = scanHistory[i];
        var qty = entry.quantity || 1;
        var clickAttr = entry.productId ? ' data-click="rescan-from-history" data-product-id="' + entry.productId + '"' : '';
        items.push(
            '<div class="scan-history-item' + (entry.productId ? '' : ' not-found') + '"' + clickAttr + '>' +
                '<span class="scan-history-date">' + escapeHtml(entry.date) + '</span>' +
                '<span class="scan-history-time">' + escapeHtml(entry.time) + '</span>' +
                '<span class="scan-history-code">' + escapeHtml(entry.code) + '</span>' +
                '<span class="scan-history-format">' + escapeHtml(entry.format) + '</span>' +
                '<span class="scan-history-qty">' + qty + '</span>' +
                '<span class="scan-history-product">' + escapeHtml(entry.product) + '</span>' +
                '<span class="scan-history-status ' + (entry.productId ? 'found' : 'missing') + '">' +
                    (entry.productId ? '\u2713' : '\u2717') +
                '</span>' +
            '</div>'
        );
    }
    el.innerHTML = '<div class="scan-history-list">' + items.join('') + '</div>';
}

function clearScanHistory() {
    scanHistory = [];
    renderScanHistory();
}

function handleRescanFromHistory(e) {
    const el = e.target.closest('[data-product-id]');
    if (!el) return;
    const productId = Number(el.getAttribute('data-product-id'));
    if (!productId) return;
    stopScanner();
    showTab('pos');
    var product = null;
    if (typeof products !== 'undefined' && products && products.length > 0) {
        for (var i = 0; i < products.length; i++) {
            if (Number(products[i].id) === productId) {
                product = products[i];
                break;
            }
        }
    }
    if (!product) {
        setScannerStatus('Produit introuvable dans la liste.', 'error');
        return;
    }
    if (typeof addPosProduct !== 'function') return;
    var price = Number(product.price) || 0;
    var discountEl = id('posDiscountType');
    if (discountEl) {
        var dt = discountEl.value;
        if (dt === 'fidele-comptoir' && product.price_loyal) price = Number(product.price_loyal);
        else if (dt === 'etudiant-comptoir' && product.price_student) price = Number(product.price_student);
        else if (dt === 'ecole-comptoir' && product.price_school) price = Number(product.price_school);
    }
    addPosProduct(productId, product.name, Number(price.toFixed(2)), 1);
}

function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
