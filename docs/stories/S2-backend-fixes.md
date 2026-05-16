# S2: Corrections backend — bugs 1, 2, 9

## Objectif
Corriger les routes backend : transfer-to-pos, transaction-by-invoice, et le double discount dans `recalculate_invoice`.

## Bugs corrigés
- Bug 1 (critique) : transfer-to-pos — le INSERT contient `source` mais la colonne n'existe pas
- Bug 2 (critique) : transaction-by-invoice — `invoice_id` manquant dans `pos_transactions`
- Bug 9 (moyen) : double discount dans `recalculate_invoice`

## Modifications

### Bug 1 — `app.py` ligne 2141 (déjà fonctionnel après S1)

Après S1, la colonne `source` existe, donc le INSERT fonctionnera. Vérifier que ce INSERT ne cause pas d'erreur.

### Bug 2 — `app.py` ligne 2289-2308

La route `transaction-by-invoice` a déjà la colonne `invoice_id` après S1. Vérifier que la requête fonctionne.

Aussi, dans `create_pos_transaction()` ligne 1808-1815, s'assurer que `invoice_id` est stocké :
```python
conn.execute('''
    UPDATE pos_transactions SET invoice_id = ? WHERE id = ?
''', (inv_id, trans_id))
```

### Bug 9 — `app.py` ligne 1334-1344

`recalculate_invoice` doit être corrigée :

**Avant :**
```python
subtotal = sum(item['line_total'] for item in items)
discount_total = sum(item['quantity'] * item['unit_price'] * item['discount_percent'] / 100 for item in items)
tax_amount = sum(item['line_total'] * item['tax_rate'] / 100 for item in items)
total = subtotal - discount_total + tax_amount
```

**Après :**
```python
subtotal = sum(item['quantity'] * item['unit_price'] for item in items)
discount_total = sum(item['quantity'] * item['unit_price'] * item['discount_percent'] / 100 for item in items)
tax_amount = sum((item['quantity'] * item['unit_price'] * (1 - item['discount_percent'] / 100)) * item['tax_rate'] / 100 for item in items)
total = subtotal - discount_total + tax_amount
```

## Test
```bash
# Bug 9 - test recalculate_invoice
curl -s http://localhost:5001/api/invoices | python3 -c "import sys,json; d=json.load(sys.stdin); print(d[0]['total'], d[0]['discount_total'])"
```

## Critères d'acceptance
- [ ] `POST /api/main-account/transfer-to-pos` ne retourne pas 500
- [ ] `GET /api/pos/transaction-by-invoice/<num>` retourne une transaction valide
- [ ] `recalculate_invoice` ne double plus la remise
