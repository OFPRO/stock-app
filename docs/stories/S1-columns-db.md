# S1: Colonnes DB manquantes

## Objectif
Ajouter les colonnes manquantes dans `init_db()` pour les tables `pos_cash_movements` et `pos_transactions`.

## Bugs corrigés
- Bug 1 (critique) : `pos_cash_movements.source` — route `transfer-to-pos` → 500
- Bug 2 (critique) : `pos_transactions.invoice_id` — route `transaction-by-invoice` → 500

## Modifications

### `app.py` — `init_db()`

**1. Après `CREATE TABLE pos_cash_movements`** (après ligne 440) :
```python
try:
    c.execute('ALTER TABLE pos_cash_movements ADD COLUMN source TEXT DEFAULT \'pos\'')
except Exception:
    pass
```

**2. Après `CREATE TABLE pos_transactions`** (après ligne 396), dans le bloc try/except existant :
```python
try:
    c.execute('ALTER TABLE pos_transactions ADD COLUMN invoice_id INTEGER REFERENCES invoices(id)')
except Exception:
    pass
```

### `app.py` — `create_pos_transaction()` (ligne 1889)
Après la création de l'invoice (`inv_id = conn.execute('SELECT last_insert_rowid()').fetchone()[0]`), mettre à jour `pos_transactions` avec l'`invoice_id` :
```python
conn.execute('UPDATE pos_transactions SET invoice_id = ? WHERE id = ?', (inv_id, trans_id))
```

Ou mieux : inclure `invoice_id` directement dans l'INSERT (ligne 1808-1815).

## Test
```bash
# Bug 1
curl -s -X POST http://localhost:5001/api/main-account/transfer-to-pos \
  -H 'Content-Type: application/json' \
  -d '{"amount": 100, "note": "Test"}' | python3 -m json.tool

# Bug 2
curl -s http://localhost:5001/api/pos/transaction-by-invoice/FAC-20260516-0001 | python3 -m json.tool
```

## Critères d'acceptance
- [ ] `POST /api/main-account/transfer-to-pos` retourne 200 (pas 500)
- [ ] `GET /api/pos/transaction-by-invoice/FAC-XXXXXX-XXXX` retourne une transaction
- [ ] `pos_cash_movements` a une colonne `source` avec des données
- [ ] `pos_transactions` a une colonne `invoice_id` avec des données
