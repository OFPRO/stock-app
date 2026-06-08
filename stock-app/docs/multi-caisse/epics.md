# Epics — Multi-Caisse StockPro

**Cycle**: OMNI Phase 2g

## Epic 1 — Infrastructure Backend Multi-Caisse

**Objectif**: Ajouter le support multi-caisse dans la base de données et les API REST.

| Story | Description | Fichiers | Effort |
|-------|-------------|----------|--------|
| S1.1 | Table `pos_registers` + CRUD + seed "Caisse 1" | `app.py` (init_db + routes) | M |
| S1.2 | Migration `pos_sessions.register_id` + `cashier_name` | `app.py` (init_db + ALTER TABLE) | S |
| S1.3 | Modifier `open_pos_session()` → sessions par register | `app.py` | S |
| S1.4 | Modifier `get_pos_session()` → filtre par register | `app.py` | S |
| S1.5 | Ajouter `GET /api/pos/sessions/active` (toutes les sessions ouvertes) | `app.py` | S |

## Epic 2 — SSE Temps Réel

**Objectif**: Implémenter le push temps réel entre caisses.

| Story | Description | Fichiers | Effort |
|-------|-------------|----------|--------|
| S2.1 | Moteur SSE : `events_queue` + `broadcast_event()` | `app.py` | M |
| S2.2 | Broadcast après `create_pos_transaction()` | `app.py` | S |
| S2.3 | Endpoint `GET /api/events` avec streaming | `app.py` | S |

## Epic 3 — Frontend Legacy SPA

**Objectif**: Adapter l'interface POS pour supporter plusieurs caisses.

| Story | Description | Fichiers | Effort |
|-------|-------------|----------|--------|
| S3.1 | Sélecteur de caisse + session par register | `index.html`, `pos.js` | M |
| S3.2 | Connexion SSE côté client (`EventSource`) | `pos.js` | M |
| S3.3 | Vérification conflit panier (`checkCartConflicts()`) | `pos.js` | M |
| S3.4 | Affichage du nom de register + caissier dans la barre | `index.html`, `pos.js` | S |

## Epic 4 — Dashboard Multi-Caisse

**Objectif**: Ajouter la vue multi-caisse au tableau de bord.

| Story | Description | Fichiers | Effort |
|-------|-------------|----------|--------|
| S4.1 | API `GET /api/kpis/registers-status` | `routes/kpis.py` | M |
| S4.2 | Carte "Caisses" dans le dashboard (état, ventes, transactions) | `dashboard.js`, `index.html` | M |

## Epic 5 — Tests & QA

| Story | Description | Effort |
|-------|-------------|--------|
| S5.1 | Tests unitaires : CRUD registers, sessions par register | M |
| S5.2 | Tests : SSE broadcast + réception | M |
| S5.3 | Tests : conflit stock entre deux caisses | S |
| S5.4 | QA : ouverture simultanée 2 caisses, ventes parallèles | M |

## Planning de Release

```
Epic 1 ──→ Epic 2 ──→ Epic 3 ──→ Epic 4 ──→ Epic 5
  (DB+API)    (SSE)      (UI)      (Dash)     (Tests)
                                       │
                                       ↓
                                    v1.1.0
```

Les stories sont ordonnées par dépendance. Chaque story est testable isolément.
