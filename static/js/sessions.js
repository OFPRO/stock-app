let _sessionsData = [];

function formatCurrency(n) {
    return (n || 0).toFixed(2) + ' DH';
}

function formatDateTime(d) {
    if (!d) return '—';
    const s = String(d);
    return s.length > 16 ? s.substring(0, 16) : s;
}

function setTextIfExists(id, val) {
    const el = document.getElementById(id);
    if (el) el.textContent = val;
}

function downloadFile(content, filename, mime) {
    const blob = new Blob([content], { type: mime });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = filename;
    a.click();
    URL.revokeObjectURL(a.href);
}

function loadSessionsHistory() {
    const start = document.getElementById('sessionDateStart')?.value || '';
    const end = document.getElementById('sessionDateEnd')?.value || '';
    const showOpen = document.getElementById('sessionShowOpen')?.checked || false;
    const params = new URLSearchParams();
    if (start) params.set('date_start', start);
    if (end) params.set('date_end', end);
    if (!showOpen) params.set('status', 'closed');
    params.set('limit', '50');

    fetch('/api/kpis/sessions-history?' + params.toString())
        .then(r => { if (!r.ok) throw new Error('Erreur serveur ' + r.status); return r.json(); })
        .then(data => {
            _sessionsData = data;
            renderSessionsHistory(data);
        })
        .catch(e => {
            document.getElementById('sessionsTableBody').innerHTML =
                '<tr><td colspan="10" style="text-align:center;padding:2rem;color:var(--danger);">Erreur: ' + escapeHtml(e.message) + '</td></tr>';
        });
}

function renderSessionsHistory(sessions) {
    const tbody = document.getElementById('sessionsTableBody');
    if (!tbody) return;

    let totalCa = 0, totalTx = 0, closedCount = 0;
    sessions.forEach(s => {
        totalCa += s.total_sales || 0;
        totalTx += s.nb_transactions || 0;
        if (s.status === 'closed') closedCount++;
    });

    setTextIfExists('sessionTotalSessions', sessions.length);
    setTextIfExists('sessionClosedSessions', closedCount);
    setTextIfExists('sessionTotalCa', formatCurrency(totalCa));
    setTextIfExists('sessionTotalTransactions', totalTx);

    if (!sessions.length) {
        tbody.innerHTML = '<tr><td colspan="10" style="text-align:center;padding:2rem;color:var(--text-light);"><i class="fas fa-inbox" style="margin-right:0.5rem;"></i>Aucune session trouvée</td></tr>';
        return;
    }

    tbody.innerHTML = sessions.map(s => {
        const statusBadge = s.status === 'open'
            ? '<span class="badge badge-success">Ouverte</span>'
            : '<span class="badge badge-secondary">Fermée</span>';
        const cashier = escapeHtml(s.cashier_name || s.user_name || '—');
        const opened = formatDateTime(s.opened_at);
        const closed = formatDateTime(s.closed_at);
        const diff = (s.closing_cash || 0) - (s.expected_cash || 0);
        const diffClass = Math.abs(diff) < 0.01 ? 'text-success' : (diff > 0 ? 'text-primary' : 'text-danger');
        return '<tr>' +
            '<td><span class="mono">' + escapeHtml(s.session_number || '#' + s.id) + '</span></td>' +
            '<td>' + statusBadge + '</td>' +
            '<td>' + cashier + '</td>' +
            '<td>' + opened + '</td>' +
            '<td>' + closed + '</td>' +
            '<td class="text-right">' + formatCurrency(s.total_sales || 0) + '</td>' +
            '<td class="text-right">' + (s.nb_transactions || 0) + '</td>' +
            '<td class="text-right">' + formatCurrency(s.expected_cash || 0) + '</td>' +
            '<td class="text-right ' + diffClass + '">' + formatCurrency(s.closing_cash || 0) + '</td>' +
            '<td><button class="btn btn-sm btn-outline" onclick="showSessionDetails(' + s.id + ')" title="Détails"><i class="fas fa-eye"></i></button></td>' +
            '</tr>';
    }).join('');
}

function showSessionDetails(sessionId) {
    fetch('/api/kpis/sessions/' + sessionId + '/details')
        .then(r => { if (!r.ok) throw new Error('Session non trouvée'); return r.json(); })
        .then(data => {
            const s = data.session;
            const txs = data.transactions || [];
            const moves = data.cash_movements || [];

            document.getElementById('sessionDetailTitle').innerHTML =
                '<i class="fas fa-cash-register"></i> ' + escapeHtml(s.session_number || 'Session #' + s.id);

            document.getElementById('sessionDetailKpi').innerHTML =
                '<div class="report-kpi-card"><div class="label">Magasin</div><div class="value" style="font-size:0.95rem;">' + escapeHtml(s.warehouse_name || 'N/A') + '</div></div>' +
                '<div class="report-kpi-card"><div class="label">Caissier</div><div class="value" style="font-size:0.95rem;">' + escapeHtml(s.cashier_name || s.user_name || '—') + '</div></div>' +
                '<div class="report-kpi-card"><div class="label">CA Total</div><div class="value success">' + formatCurrency(s.total_sales || 0) + '</div></div>' +
                '<div class="report-kpi-card"><div class="label">Transactions</div><div class="value">' + txs.length + '</div></div>';

            let info = '<p style="margin-top:0.5rem;color:var(--text-light);font-size:0.9rem;">' +
                'Ouverture: ' + formatDateTime(s.opened_at) +
                (s.closed_at ? ' — Fermeture: ' + formatDateTime(s.closed_at) : ' — En cours') +
                '</p>';
            document.getElementById('sessionDetailInfo').innerHTML = info;

            if (txs.length) {
                let h = '<table class="table table-compact"><thead><tr><th>#</th><th>Client</th><th>Heure</th><th class="text-right">Articles</th><th class="text-right">Total</th></tr></thead><tbody>';
                txs.forEach((t, i) => {
                    h += '<tr>' +
                        '<td>' + (i + 1) + '</td>' +
                        '<td>' + escapeHtml(t.customer_name || 'Client comptoir') + '</td>' +
                        '<td>' + formatDateTime(t.created_at) + '</td>' +
                        '<td class="text-right">' + (t.items_count || 0) + '</td>' +
                        '<td class="text-right">' + formatCurrency(t.total || 0) + '</td>' +
                        '</tr>';
                });
                h += '</tbody></table>';
                document.getElementById('sessionDetailTransactions').innerHTML = h;
            } else {
                document.getElementById('sessionDetailTransactions').innerHTML =
                    '<p style="color:var(--text-light);">Aucune transaction.</p>';
            }

            if (moves.length) {
                let h = '<table class="table table-compact"><thead><tr><th>Type</th><th>Motif</th><th>Heure</th><th class="text-right">Montant</th></tr></thead><tbody>';
                moves.forEach(m => {
                    const typeBadge = m.type === 'in'
                        ? '<span class="badge badge-success">Entrée</span>'
                        : '<span class="badge badge-danger">Sortie</span>';
                    h += '<tr>' +
                        '<td>' + typeBadge + '</td>' +
                        '<td>' + escapeHtml(m.reason || '—') + '</td>' +
                        '<td>' + formatDateTime(m.created_at) + '</td>' +
                        '<td class="text-right">' + formatCurrency(m.amount || 0) + '</td>' +
                        '</tr>';
                });
                h += '</tbody></table>';
                document.getElementById('sessionDetailCashMovements').innerHTML = h;
            } else {
                document.getElementById('sessionDetailCashMovements').innerHTML =
                    '<p style="color:var(--text-light);">Aucun mouvement de caisse.</p>';
            }

            openModal('sessionDetailModal');
        })
        .catch(e => showError(e.message));
}

function exportSessions() {
    if (!_sessionsData.length) { showError('Aucune donnée à exporter'); return; }
    const rows = [['Session', 'Statut', 'Caissier', 'Ouverture', 'Fermeture', 'Ventes', 'Nb Transactions', 'Attendu', 'Réel']];
    _sessionsData.forEach(s => {
        rows.push([
            s.session_number || '#' + s.id,
            s.status === 'open' ? 'Ouverte' : 'Fermée',
            s.cashier_name || s.user_name || '',
            s.opened_at || '',
            s.closed_at || '',
            (s.total_sales || 0).toFixed(2),
            s.nb_transactions || 0,
            (s.expected_cash || 0).toFixed(2),
            (s.closing_cash || 0).toFixed(2)
        ]);
    });
    const csv = rows.map(r => r.map(c => '"' + String(c).replace(/"/g, '""') + '"').join(',')).join('\n');
    downloadFile(csv, 'sessions_export.csv', 'text/csv');
}
