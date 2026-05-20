(function () {
    'use strict';

    var detector = null;
    var stream = null;
    var animFrame = null;
    var isActive = false;
    var audioCtx = null;
    var lastCode = '';
    var lastTime = 0;
    var resizeObs = null;

    function getAudioCtx() {
        if (!audioCtx) {
            try { audioCtx = new (window.AudioContext || window.webkitAudioContext)(); }
            catch (e) { return null; }
        }
        if (audioCtx.state === 'suspended') audioCtx.resume();
        return audioCtx;
    }

    function playBeep(freq, duration, vol) {
        try {
            var ctx = getAudioCtx();
            if (!ctx) return;
            var osc = ctx.createOscillator();
            var gain = ctx.createGain();
            osc.connect(gain);
            gain.connect(ctx.destination);
            osc.frequency.value = freq || 1200;
            osc.type = 'sine';
            gain.gain.setValueAtTime(vol || 0.25, ctx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + (duration || 0.12));
            osc.start(ctx.currentTime);
            osc.stop(ctx.currentTime + (duration || 0.12));
        } catch (e) { /* audio not supported */ }
    }

    function playSuccess() { playBeep(1200, 0.12, 0.25); }

    function playError() { playBeep(300, 0.25, 0.3); }

    function getEl(id) { return document.getElementById(id); }

    function resizeCanvas() {
        var c = getEl('posScannerCanvas');
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
        var c = getEl('posScannerCanvas');
        if (!c) return;
        resizeCanvas();
        var ctx = c.getContext('2d');
        ctx.clearRect(0, 0, c.width, c.height);
    }

    function drawBox(points) {
        var c = getEl('posScannerCanvas');
        if (!c || !points || points.length < 4) return;
        var ctx = c.getContext('2d');
        ctx.strokeStyle = '#10b981';
        ctx.lineWidth = 3;
        ctx.shadowColor = '#10b981';
        ctx.shadowBlur = 8;
        ctx.beginPath();
        ctx.moveTo(points[0].x, points[0].y);
        for (var i = 1; i < points.length; i++) ctx.lineTo(points[i].x, points[i].y);
        ctx.closePath();
        ctx.stroke();
    }

    function getBoundingBox(points) {
        var minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
        for (var i = 0; i < points.length; i++) {
            if (points[i].x < minX) minX = points[i].x;
            if (points[i].y < minY) minY = points[i].y;
            if (points[i].x > maxX) maxX = points[i].x;
            if (points[i].y > maxY) maxY = points[i].y;
        }
        return { x: minX, y: minY, w: maxX - minX, h: maxY - minY };
    }

    function setStatus(msg, type) {
        var el = getEl('posScannerStatus');
        if (!el) return;
        el.textContent = msg;
        el.className = 'pos-scanner-status ' + (type || 'info');
        el.style.display = msg ? '' : 'none';
    }

    function detectLoop() {
        if (!isActive) return;

        var video = getEl('posScannerVideo');
        var canvas = getEl('posScannerCanvas');
        if (!video || !detector || !canvas) return;

        detector.detect(video).then(function (barcodes) {
            if (!isActive) return;

            resizeCanvas();
            var ctx = canvas.getContext('2d');
            ctx.clearRect(0, 0, canvas.width, canvas.height);

            if (barcodes.length > 0) {
                var bc = barcodes[0];
                var code = bc.rawValue;
                var format = bc.format;
                var now = Date.now();
                var vw = video.videoWidth || 1280;
                var vh = video.videoHeight || 720;
                var scale = Math.max(canvas.width / vw, canvas.height / vh);
                var ox = (canvas.width - vw * scale) / 2;
                var oy = (canvas.height - vh * scale) / 2;

                ctx.fillStyle = 'rgba(0,0,0,0.55)';
                ctx.fillRect(0, 0, canvas.width, canvas.height);

                var bbox = getBoundingBox(bc.cornerPoints);
                var pad = 0.05;
                var sx = Math.max(0, bbox.x - bbox.w * pad);
                var sy = Math.max(0, bbox.y - bbox.h * pad);
                var sw = Math.min(vw - sx, bbox.w * (1 + 2 * pad));
                var sh = Math.min(vh - sy, bbox.h * (1 + 2 * pad));
                ctx.drawImage(video, sx, sy, sw, sh, sx * scale + ox, sy * scale + oy, sw * scale, sh * scale);

                var pts = bc.cornerPoints.map(function (p) {
                    return { x: p.x * scale + ox, y: p.y * scale + oy };
                });
                drawBox(pts);

                if (code !== lastCode || now - lastTime > 1500) {
                    lastCode = code;
                    lastTime = now;
                    handleScan(code, format);
                }
            }
        }).catch(function () {
            clearCanvas();
        }).finally(function () {
            if (isActive) {
                animFrame = requestAnimationFrame(detectLoop);
            }
        });
    }

    function handleScan(code, format) {
        fetch('/api/products/for-sale?search=' + encodeURIComponent(code))
            .then(function (res) {
                if (!res.ok) throw new Error('Erreur ' + res.status);
                return res.json();
            })
            .then(function (data) {
                if (data && data.length > 0) {
                    var product = data[0];
                    if (typeof addPosProduct === 'function') {
                        addPosProduct(Number(product.id));
                    }
                    playSuccess();
                } else {
                    if (typeof showError === 'function') {
                        showError('Article non trouv\u00e9: ' + code);
                    }
                    playError();
                }
            })
            .catch(function (err) {
                if (typeof showError === 'function') {
                    showError('Erreur de recherche: ' + code);
                }
                playError();
            });
    }

    function togglePosScanner() {
        if (isActive) {
            stopPosScanner();
            return;
        }

        if (typeof BarcodeDetector === 'undefined') {
            if (typeof showError === 'function') {
                showError('BarcodeDetector non support\u00e9. Utilisez Chrome ou Edge.');
            }
            return;
        }

        var container = getEl('posScannerContainer');
        if (!container) return;

        container.style.display = '';

        setStatus('D\u00e9marrage...', 'info');

        BarcodeDetector.getSupportedFormats().then(function (supported) {
            var wanted = ['ean_13', 'ean_8', 'code_128', 'code_39', 'qr_code'];
            var available = wanted.filter(function (f) { return supported.indexOf(f) !== -1; });
            if (available.length === 0) {
                setStatus('Aucun format support\u00e9', 'error');
                stopPosScanner();
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
                var video = getEl('posScannerVideo');
                if (!video) { stopPosScanner(); return; }
                video.srcObject = s;
                video.setAttribute('playsinline', '');
                video.play();

                video.addEventListener('playing', function onPlay() {
                    video.removeEventListener('playing', onPlay);
                    isActive = true;
                    lastCode = '';
                    lastTime = 0;
                    resizeCanvas();
                    if (resizeObs) resizeObs.disconnect();
                    resizeObs = new ResizeObserver(function () { resizeCanvas(); });
                    resizeObs.observe(video.parentElement);
                    setStatus('Pr\u00eat', 'success');
                    getEl('posScannerToggle').innerHTML = '<i class="fas fa-stop"></i>';
                    animFrame = requestAnimationFrame(detectLoop);
                });
            }).catch(function (err) {
                if (err.name === 'NotAllowedError') {
                    setStatus('Cam\u00e9ra refus\u00e9e', 'error');
                } else {
                    setStatus('Erreur: ' + (err.message || 'inconnue'), 'error');
                }
                stopPosScanner();
            });
        });
    }

    function stopPosScanner() {
        isActive = false;
        if (animFrame) { cancelAnimationFrame(animFrame); animFrame = null; }
        if (resizeObs) { resizeObs.disconnect(); resizeObs = null; }
        if (stream) {
            stream.getTracks().forEach(function (t) { t.stop(); });
            stream = null;
        }
        var video = getEl('posScannerVideo');
        if (video) video.srcObject = null;
        clearCanvas();
        detector = null;
        var container = getEl('posScannerContainer');
        if (container) container.style.display = 'none';
        var btn = getEl('posScannerToggle');
        if (btn) btn.innerHTML = '<i class="fas fa-camera"></i>';
        setStatus('', '');
    }

    window.togglePosScanner = togglePosScanner;
    window.stopPosScanner = stopPosScanner;
})();
