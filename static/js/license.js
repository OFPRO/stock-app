function loadLicense() {
    var body = document.getElementById('licenseBody');
    body.innerHTML = '<div class="empty"><i class="fas fa-spinner fa-spin"></i><p>Chargement...</p></div>';

    fetch('/api/license/status')
        .then(function(r) { return r.json(); })
        .then(function(data) {
            if (data.admin) {
                renderAdminLicense(body, data);
            } else if (data.activated) {
                renderLicenseActive(body, data);
            } else {
                renderLicenseInactive(body, data);
            }
        })
        .catch(function() {
            body.innerHTML = '<div class="alert alert-error"><i class="fas fa-exclamation-triangle"></i> Erreur de chargement du statut licence</div>';
        });
}

function renderAdminLicense(body, data) {
    body.innerHTML =
        '<p style="margin-bottom:1rem;color:var(--text-light);font-size:0.9rem;"><i class="fas fa-laptop"></i> Mode administrateur — génération de licences</p>' +
        '<div class="form-group">' +
            '<label><i class="fas fa-network-wired"></i> Adresse MAC</label>' +
            '<input type="text" id="adminMac" class="form-input" value="' + escapeHtml(data.mac || '') + '" placeholder="XX:XX:XX:XX:XX:XX">' +
            '<button class="btn btn-sm btn-outline" style="margin-top:6px;" onclick="detectAdminMac()"><i class="fas fa-sync"></i> Détecter ma MAC</button>' +
        '</div>' +
        '<div class="form-group">' +
            '<label><i class="fas fa-user"></i> Nom du client</label>' +
            '<input type="text" id="adminClient" class="form-input" placeholder="Ex: Librairie Badr">' +
        '</div>' +
        '<div class="form-group">' +
            '<label><i class="fas fa-calendar"></i> Durée (jours)</label>' +
            '<input type="number" id="adminDays" class="form-input" value="365" min="1">' +
        '</div>' +
        '<button class="btn btn-primary" onclick="generateLicenseToken()"><i class="fas fa-key"></i> Générer le token</button>' +
        '<div id="tokenOutput" style="display:none;margin-top:16px;">' +
            '<label style="font-size:0.85rem;font-weight:600;color:var(--text);">Token généré</label>' +
            '<div class="token-box" id="tokenText" style="background:var(--bg-muted);border:1px solid var(--border);border-radius:8px;padding:12px;font-family:monospace;font-size:0.75rem;word-break:break-all;max-height:160px;overflow-y:auto;margin-top:8px;"></div>' +
            '<div style="display:flex;gap:8px;margin-top:8px;">' +
                '<button class="btn btn-sm btn-primary" onclick="copyLicenseToken()"><i class="fas fa-copy"></i> Copier</button>' +
                '<button class="btn btn-sm btn-outline" onclick="downloadLicenseToken()"><i class="fas fa-download"></i> Télécharger</button>' +
            '</div>' +
        '</div>' +
        '<div id="tokenError" style="display:none;margin-top:12px;" class="alert alert-error"></div>';
}

function renderLicenseActive(body, data) {
    var expDate = data.expires_at ? new Date(data.expires_at * 1000).toLocaleDateString('fr-FR') : 'N/A';
    var now = Date.now() / 1000;
    var daysLeft = data.expires_at ? Math.max(0, Math.floor((data.expires_at - now) / 86400)) : 'N/A';

    body.innerHTML =
        '<div class="status-badge success" style="display:inline-flex;align-items:center;gap:8px;padding:8px 16px;border-radius:40px;font-size:0.85rem;font-weight:600;margin-bottom:24px;"><i class="fas fa-check-circle"></i> Licence valide</div>' +
        '<div class="info-row" style="display:flex;justify-content:space-between;padding:10px 0;border-bottom:1px solid var(--border);font-size:0.9rem;">' +
            '<span style="color:var(--text-light);">Client</span>' +
            '<span style="font-weight:500;">' + escapeHtml(data.client || 'N/A') + '</span>' +
        '</div>' +
        '<div class="info-row" style="display:flex;justify-content:space-between;padding:10px 0;border-bottom:1px solid var(--border);font-size:0.9rem;">' +
            '<span style="color:var(--text-light);">Adresse MAC</span>' +
            '<span style="font-weight:700;font-family:monospace;letter-spacing:1px;">' + escapeHtml(data.mac || 'N/A') + '</span>' +
        '</div>' +
        '<div class="info-row" style="display:flex;justify-content:space-between;padding:10px 0;border-bottom:1px solid var(--border);font-size:0.9rem;">' +
            '<span style="color:var(--text-light);">Expire le</span>' +
            '<span style="font-weight:500;">' + expDate + '</span>' +
        '</div>' +
        '<div class="info-row" style="display:flex;justify-content:space-between;padding:10px 0;font-size:0.9rem;border-bottom:none;">' +
            '<span style="color:var(--text-light);">Jours restants</span>' +
            '<span style="font-weight:500;">' + daysLeft + '</span>' +
        '</div>';
}

function renderLicenseInactive(body, data) {
    body.innerHTML =
        '<div class="alert alert-info" style="margin-bottom:16px;"><i class="fas fa-info-circle"></i> Notez l\'adresse MAC ci-dessous et envoyez-la à votre fournisseur pour obtenir votre clé de licence.</div>' +
        '<div class="form-group">' +
            '<label><i class="fas fa-network-wired"></i> Adresse MAC de cette machine</label>' +
            '<input type="text" class="form-input" value="' + escapeHtml(data.mac || 'Détection en cours...') + '" readonly>' +
        '</div>' +
        '<div class="form-group">' +
            '<label><i class="fas fa-key"></i> Clé de licence</label>' +
            '<input type="text" id="activateToken" class="form-input" placeholder="Collez votre token de licence ici">' +
        '</div>' +
        '<button class="btn btn-primary" onclick="activateLicenseToken()"><i class="fas fa-check"></i> Activer</button>' +
        '<div id="activationError" style="display:none;margin-top:12px;" class="alert alert-error"></div>';
}

function detectAdminMac() {
    fetch('/api/license/status')
        .then(function(r) { return r.json(); })
        .then(function(data) {
            var el = document.getElementById('adminMac');
            if (el && data.mac) el.value = data.mac;
        })
        .catch(function() {});
}

function generateLicenseToken() {
    var mac = document.getElementById('adminMac').value.trim();
    var client = document.getElementById('adminClient').value.trim();
    var days = parseInt(document.getElementById('adminDays').value) || 365;
    var errorDiv = document.getElementById('tokenError');
    var outputDiv = document.getElementById('tokenOutput');

    errorDiv.style.display = 'none';
    outputDiv.style.display = 'none';

    if (!mac) {
        errorDiv.textContent = 'Veuillez entrer une adresse MAC';
        errorDiv.style.display = 'block';
        return;
    }
    if (!client) {
        errorDiv.textContent = 'Veuillez entrer un nom de client';
        errorDiv.style.display = 'block';
        return;
    }

    fetch('/api/license/generate', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({mac: mac, client: client, days: days})
    })
    .then(function(r) { return r.json(); })
    .then(function(data) {
        if (data.token) {
            document.getElementById('tokenText').textContent = data.token;
            outputDiv.style.display = 'block';
        } else {
            errorDiv.textContent = data.error || 'Erreur de génération';
            errorDiv.style.display = 'block';
        }
    })
    .catch(function() {
        errorDiv.textContent = 'Erreur réseau';
        errorDiv.style.display = 'block';
    });
}

function copyLicenseToken() {
    var text = document.getElementById('tokenText').textContent;
    navigator.clipboard.writeText(text);
}

function downloadLicenseToken() {
    var text = document.getElementById('tokenText').textContent;
    var blob = new Blob([text], {type: 'text/plain'});
    var a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'license-token.txt';
    a.click();
}

function activateLicenseToken() {
    var token = document.getElementById('activateToken').value.trim();
    var errorDiv = document.getElementById('activationError');
    errorDiv.style.display = 'none';

    if (!token) {
        errorDiv.textContent = 'Veuillez coller votre clé de licence';
        errorDiv.style.display = 'block';
        return;
    }

    fetch('/api/license/activate', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({token: token})
    })
    .then(function(r) { return r.json(); })
    .then(function(data) {
        if (data.ok) {
            loadLicense();
        } else {
            errorDiv.textContent = data.error || 'Token invalide';
            errorDiv.style.display = 'block';
        }
    })
    .catch(function() {
        errorDiv.textContent = 'Erreur réseau';
        errorDiv.style.display = 'block';
    });
}

function escapeHtml(str) {
    if (!str) return '';
    var div = document.createElement('div');
    div.appendChild(document.createTextNode(str));
    return div.innerHTML;
}
