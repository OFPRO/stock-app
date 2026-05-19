(function () {
    'use strict';

    var detector = null;
    var stream = null;
    var animFrame = null;
    var isRunning = false;
    var audioCtx = null;
    var lastCode = '';
    var lastTime = 0;
    var history = [];
    var clearTimer = null;

    function $(id) { return document.getElementById(id); }

    function resizeCanvas() {
        var c = $('proCanvas');
        if (!c) return;
        var p = c.parentElement;
        var w = p.clientWidth;
        var h = p.clientHeight;
        if (c.width !== w || c.height !== h) {
            c.width = w;
            c.height = h;
        }
    }

    function clearCanvas() {
        var c = $('proCanvas');
        if (!c) return;
        resizeCanvas();
        var ctx = c.getContext('2d');
        ctx.clearRect(0, 0, c.width, c.height);
    }

    function drawBox(points) {
        var c = $('proCanvas');
        if (!c || !points || points.length < 4) return;
        resizeCanvas();
        var ctx = c.getContext('2d');
        ctx.clearRect(0, 0, c.width, c.height);
        ctx.strokeStyle = '#10b981';
        ctx.lineWidth = 3;
        ctx.shadowColor = '#10b981';
        ctx.shadowBlur = 8;
        ctx.fillStyle = 'rgba(16, 185, 129, 0.08)';
        ctx.beginPath();
        ctx.moveTo(points[0].x, points[0].y);
        for (var i = 1; i < points.length; i++) {
            ctx.lineTo(points[i].x, points[i].y);
        }
        ctx.closePath();
        ctx.stroke();
        ctx.fill();
    }

    function playBeep() {
        try {
            if (!audioCtx) audioCtx = new (window.AudioContext || window.webkitAudioContext)();
            if (audioCtx.state === 'suspended') audioCtx.resume();
            var osc = audioCtx.createOscillator();
            var gain = audioCtx.createGain();
            osc.connect(gain);
            gain.connect(audioCtx.destination);
            osc.frequency.value = 1200;
            osc.type = 'sine';
            gain.gain.setValueAtTime(0.25, audioCtx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.12);
            osc.start(audioCtx.currentTime);
            osc.stop(audioCtx.currentTime + 0.12);
        } catch (e) { /* audio not supported */ }
    }

    function setStatus(msg, type) {
        var el = $('proStatus');
        if (!el) return;
        el.textContent = msg;
        el.className = 'scanner-status ' + (type || 'info');
    }

    function addHistory(code, format, product) {
        var productId = product ? Number(product.id) : null;
        var now = new Date();
        var date = now.toLocaleDateString('fr-FR');
        var time = now.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });

        for (var i = 0; i < history.length; i++) {
            if (history[i].code === code && history[i].productId === productId) {
                history[i].quantity = (history[i].quantity || 1) + 1;
                history[i].date = date;
                history[i].time = time;
                var entry = history.splice(i, 1)[0];
                history.unshift(entry);
                renderHistory();
                return;
            }
        }

        history.unshift({
            code: code,
            format: format || '\u2014',
            product: product ? product.name : 'Non trouvé',
            productId: productId,
            quantity: 1,
            date: date,
            time: time
        });
        if (history.length > 50) history.pop();
        renderHistory();
    }

    function renderHistory() {
        var el = $('proHistory');
        if (!el) return;
        if (history.length === 0) {
            el.innerHTML = '<p class="text-muted text-center" style="padding:1rem;">Aucun scan</p>';
            return;
        }
        var items = [];
        for (var i = 0; i < history.length; i++) {
            var e = history[i];
            var qty = e.quantity || 1;
            items.push(
                '<div class="scan-history-item' + (e.productId ? '' : ' not-found') + '">' +
                    '<span class="scan-history-date">' + escapeHtml(e.date) + '</span>' +
                    '<span class="scan-history-time">' + escapeHtml(e.time) + '</span>' +
                    '<span class="scan-history-code">' + escapeHtml(e.code) + '</span>' +
                    '<span class="scan-history-format">' + escapeHtml(e.format) + '</span>' +
                    '<span class="scan-history-qty">' + qty + '</span>' +
                    '<span class="scan-history-product">' + escapeHtml(e.product) + '</span>' +
                    '<span class="scan-history-status ' + (e.productId ? 'found' : 'missing') + '">' +
                        (e.productId ? '\u2713' : '\u2717') +
                    '</span>' +
                '</div>'
            );
        }
        el.innerHTML = '<div class="scan-history-list">' + items.join('') + '</div>';
    }

    function escapeHtml(str) {
        if (str === null || str === undefined) return '';
        return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#039;');
    }

    async function fetchProduct(code, format) {
        var el = $('proResult');
        if (!el) return;
        el.innerHTML = '<div class="scanner-result-loading"><i class="fas fa-spinner fa-spin"></i><p>Recherche...</p></div>';

        var controller = new AbortController();
        var timeout = setTimeout(function () { controller.abort(); }, 10000);

        try {
            var res = await fetch('/api/products/for-sale?search=' + encodeURIComponent(code), { signal: controller.signal });
            clearTimeout(timeout);
            if (!res.ok) throw new Error('Erreur ' + res.status);
            var data = await res.json();

            if (data && data.length > 0) {
                var p = data[0];
                var discountEl = $('proDiscountType');
                var price = Number(p.price) || 0;
                if (discountEl) {
                    var dt = discountEl.value;
                    if (dt === 'fidele-comptoir' && p.price_loyal) price = Number(p.price_loyal);
                    else if (dt === 'etudiant-comptoir' && p.price_student) price = Number(p.price_student);
                    else if (dt === 'ecole-comptoir' && p.price_school) price = Number(p.price_school);
                }
                price = Number(price.toFixed(2));

                el.innerHTML =
                    '<div class="scanner-result-found">' +
                        '<div class="scanner-result-icon"><i class="fas fa-check-circle"></i></div>' +
                        '<div class="scanner-result-info">' +
                            '<h4>' + escapeHtml(p.name) + '</h4>' +
                            '<div class="scanner-result-details">' +
                                '<span><strong>Code:</strong> ' + escapeHtml(code) + '</span>' +
                                '<span><strong>Format:</strong> ' + escapeHtml(format) + '</span>' +
                                '<span><strong>Prix:</strong> ' + price.toFixed(2) + ' DH</span>' +
                                '<span><strong>Stock:</strong> ' + (Number(p.quantity) || 0) + '</span>' +
                            '</div>' +
                        '</div>' +
                    '</div>';
                addHistory(code, format, p);
            } else {
                el.innerHTML =
                    '<div class="scanner-result-notfound">' +
                        '<i class="fas fa-times-circle"></i><h4>Produit non trouvé</h4>' +
                        '<p>Code: ' + escapeHtml(code) + '</p>' +
                    '</div>';
                addHistory(code, format, null);
            }
        } catch (err) {
            clearTimeout(timeout);
            el.innerHTML = '<div class="scanner-result-error"><i class="fas fa-exclamation-triangle"></i><p>Erreur de recherche</p></div>';
        }
    }

    function detectLoop(video) {
        if (!isRunning) return;

        detector.detect(video).then(function (barcodes) {
            if (!isRunning) return;

            if (clearTimer) {
                clearTimeout(clearTimer);
                clearTimer = null;
            }

            if (barcodes.length > 0) {
                var bc = barcodes[0];
                var code = bc.rawValue;
                var format = bc.format;
                var now = Date.now();

                drawBox(bc.cornerPoints);
                clearTimer = setTimeout(function () { clearCanvas(); }, 500);

                if (code !== lastCode || now - lastTime > 1500) {
                    lastCode = code;
                    lastTime = now;
                    playBeep();
                    fetchProduct(code, format);
                }
            } else {
                clearCanvas();
            }
        }).catch(function () {
            clearCanvas();
        }).finally(function () {
            if (isRunning) {
                animFrame = requestAnimationFrame(function () { detectLoop(video); });
            }
        });
    }

    function startPro() {
        if (isRunning) return;
        if (typeof BarcodeDetector === 'undefined') {
            setStatus('BarcodeDetector non supporté. Utilisez Chrome ou Edge.', 'error');
            return;
        }

        var video = $('proVideo');
        if (!video) return;

        setStatus('Démarrage de la caméra...', 'info');
        $('proPlaceholder').style.display = 'none';

        BarcodeDetector.getSupportedFormats().then(function (supported) {
            var formats = ['ean_13', 'ean_8', 'code_128', 'code_39', 'qr_code'];
            var available = formats.filter(function (f) { return supported.indexOf(f) !== -1; });

            if (available.length === 0) {
                setStatus('Aucun format de code-barres supporté par ce navigateur.', 'error');
                return;
            }

            detector = new BarcodeDetector({ formats: available });

            navigator.mediaDevices.getUserMedia({
                video: {
                    facingMode: 'environment',
                    width: { ideal: 1280 },
                    height: { ideal: 720 }
                }
            }).then(function (s) {
                stream = s;
                video.srcObject = s;
                video.setAttribute('playsinline', '');
                video.play();

                video.addEventListener('playing', function onPlay() {
                    video.removeEventListener('playing', onPlay);
                    resizeCanvas();
                    var obs = new ResizeObserver(function () { resizeCanvas(); });
                    obs.observe(video.parentElement);
                    isRunning = true;
                    lastCode = '';
                    lastTime = 0;
                    setStatus('Prêt — placez un code-barres devant la caméra', 'success');
                    $('proBtnStart').style.display = 'none';
                    $('proBtnStop').style.display = '';
                    animFrame = requestAnimationFrame(function () { detectLoop(video); });
                });
            }).catch(function (err) {
                if (err.name === 'NotAllowedError') {
                    setStatus('Autorisation caméra refusée.', 'error');
                } else {
                    setStatus('Erreur caméra: ' + (err.message || 'inconnue'), 'error');
                }
                $('proPlaceholder').style.display = '';
            });
        });
    }

    function stopPro() {
        isRunning = false;
        if (animFrame) { cancelAnimationFrame(animFrame); animFrame = null; }
        if (clearTimer) { clearTimeout(clearTimer); clearTimer = null; }
        if (stream) {
            stream.getTracks().forEach(function (t) { t.stop(); });
            stream = null;
        }
        var v = $('proVideo');
        if (v) v.srcObject = null;
        clearCanvas();
        detector = null;
        $('proBtnStart').style.display = '';
        $('proBtnStop').style.display = 'none';
        setStatus('Scanner arrêté', 'info');
        $('proPlaceholder').style.display = '';
    }

    function clearProHistory() {
        history = [];
        renderHistory();
    }

    window.startProScanner = startPro;
    window.stopProScanner = stopPro;
    window.clearProHistory = clearProHistory;
})();
