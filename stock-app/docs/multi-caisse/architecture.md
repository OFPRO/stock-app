# Architecture — Multi-Caisse StockPro

**Date**: 2026-06-08
**Cycle**: OMNI Phase 2f

## Diagramme de données

```
┌──────────────────────┐
│    pos_registers     │
│ id, name, code,      │
│ warehouse_id, active │
└──────────┬───────────┘
           │ 1
           │
┌──────────┴───────────┐
│    pos_sessions      │  ← register_id ajouté
│ id, session_number,  │     cashier_name ajouté
│ register_id,         │
│ warehouse_id,        │
│ opening_cash, ...,   │
│ status, opened_at    │
└──────────┬───────────┘
           │ 1
           │
┌──────────┴───────────┐
│  pos_transactions    │  ← inchangé
│ id, session_id, ...  │
└──────────┬───────────┘
           │
┌──────────┴───────────┐
│ pos_cash_movements   │  ← inchangé
│ id, session_id, ...  │
└──────────────────────┘
```

## Flux SSE

```
 Caisse 1 (navigateur)         Serveur Flask         Caisse 2 (navigateur)
        │                          │                        │
        │── POST /api/pos/───────→│                        │
        │   transactions          │                        │
        │                         │── broadcast SSE ──────→│
        │                         │   "stock-update"       │
        │                         │                        │
        │                         │── broadcast SSE ──────→│
        │                         │   "transaction"        │
        │                         │                        │
        │←──── SSE "stock-update"─│                        │
        │   (Caisse 2 a vendu)    │                        │
```

## Architecture SSE côté serveur

```python
import queue
import json
from flask import Response

# File d'attente thread-safe pour les événements SSE
sse_clients = []

def broadcast_event(event_type, data):
    """Pousse un événement à tous les clients SSE connectés"""
    payload = f"event: {event_type}\ndata: {json.dumps(data)}\n\n"
    for client_queue in sse_clients:
        client_queue.put(payload)


@app.route('/api/events')
def sse_events():
    """Endpoint SSE — chaque client reçoit une queue dédiée"""
    client_queue = queue.Queue()
    sse_clients.append(client_queue)

    def event_stream():
        try:
            while True:
                payload = client_queue.get()
                yield payload
        except GeneratorExit:
            sse_clients.remove(client_queue)

    return Response(event_stream(), mimetype='text/event-stream')
```

## Isolation par register

Chaque register a :
- Sa propre session (`pos_sessions.register_id`)
- Ses propres transactions (`pos_transactions.session_id` → `pos_sessions.register_id`)
- Ses propres mouvements de caisse (`pos_cash_movements.session_id`)
- Son propre calcul de solde à la fermeture

Seul le stock est partagé (protégé par optimistic locking).

## Rétrocompatibilité

- Si `register_id IS NULL` dans `pos_sessions`, la session est "legacy" (comportement ancien)
- L'endpoint GET `/api/pos/sessions` sans `register_id` renvoie toutes les sessions ouvertes
- Le frontend actuel (sans register_id) continue de fonctionner

## Dépendances

- Aucune dépendance externe (Flask pur, `queue.Queue` de la stdlib)
- Pas de Redis, pas de WebSocket, pas de bibliothèque SSE
