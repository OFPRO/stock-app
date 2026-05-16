# S4: Pricing client dans le POS

## Objectif
Appliquer les remises par palier client (loyal -15%, étudiant -15%, école -20%) lors des transactions POS.

## Bug corrigé
- Bug 8 (moyen) : les remises client ne sont pas appliquées dans `create_pos_transaction()`.

## Modifications

### `app.py` — `create_pos_transaction()` (avant ligne 1797)

Après la détermination du `customer_id` et avant le calcul des totaux, charger le client et appliquer `get_price_for_customer()` :

```python
# Après ligne 1777 (fin du bloc client comptoir)
# Charger le client pour appliquer le pricing
customer = None
if customer_id and not is_client_comptoir:
    cust_row = conn.execute('SELECT * FROM customers WHERE id = ?', (int(customer_id),)).fetchone()
    if cust_row:
        customer = dict(cust_row)

# Dans la boucle for item in items (ligne 1797+)
for item in items:
    if customer:
        product = conn.execute('SELECT * FROM products WHERE id = ?', (item['product_id'],)).fetchone()
        if product:
            p = dict(product)
            item['unit_price'] = get_price_for_customer(p, customer['type'], customer['is_loyal'])
    unit_price = item['unit_price']
    ...
```

**Important** : Le prix remisé doit être utilisé à la fois pour la transaction POS (`pos_transaction_items`) et pour l'invoice créée (`invoice_items`).

## Test
```bash
# Vérifier qu'un client "école" paie 80% du prix
# 1. Créer une transaction avec Auto-École (CLI-2026-0012, type='auto_ecole' → is_loyal → -15%)
# 2. Vérifier que unit_price dans la réponse est correct
```

## Critères d'acceptance
- [ ] Un client "fidèle" (is_loyal=1) paie -15% sur le POS
- [ ] Un client "école" (type='ecole') paie -20% sur le POS
- [ ] Un client "étudiant" (type='etudiant') paie -15% sur le POS
- [ ] Un client comptoir (sans ID) paie le prix normal
- [ ] Les montants dans `pos_transaction_items` et `invoice_items` reflètent la remise
