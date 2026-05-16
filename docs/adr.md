# Architecture Decision Records — Module POS

## ADR 1: Migration des colonnes manquantes

**Contexte** : `pos_cash_movements` n'a pas de colonne `source`, `pos_transactions` n'a pas de colonne `invoice_id`. Les ALTER TABLE en try/except de `init_db()` ne couvrent pas ces deux colonnes.

**Décision** : Ajouter les ALTER TABLE manquants dans `init_db()` avec la même convention try/except existante. Pas de framework de migration.

**Détail** :
- `pos_cash_movements` → `source TEXT DEFAULT 'pos'`
- `pos_transactions` → `invoice_id INTEGER` (référence à `invoices.id`)

**Fichier** : `app.py` dans `init_db()`, après chaque `CREATE TABLE` concerné.

---

## ADR 2: Correction Bug 1 — transfer-to-pos 500

**Contexte** : La route `/api/main-account/transfer-to-pos` fait `INSERT INTO pos_cash_movements (..., source) VALUES (..., 'main_account')` mais la colonne `source` n'existe pas.

**Décision** : Après ADR 1 (colonne `source` ajoutée), le INSERT fonctionnera. Aucune autre modification nécessaire.

**Risque** : La colonne `reason` manque aussi ? Vérifier — non, `reason` existe déjà dans le CREATE TABLE.

---

## ADR 3: Correction Bug 2 — transaction-by-invoice 500

**Contexte** : La route `/api/pos/transaction-by-invoice/<invoice_number>` fait `WHERE t.invoice_id IN (...)` mais `pos_transactions` n'a pas de colonne `invoice_id`.

**Décision** : Après ADR 1 (colonne `invoice_id` ajoutée), mettre à jour la route `create_pos_transaction` pour stocker `invoice_id` dans `pos_transactions` lors de la création d'une transaction POS.

**Détail** : Dans `create_pos_transaction()` (app.py:1808-1815), ajouter `invoice_id` à la clause INSERT et stocker l'ID de la facture créée.

---

## ADR 4: Correction Bug 3 — PDF URL dans React frontend

**Contexte** : `frontend/src/components/pos/PosPage.tsx:153` utilise `/api/invoices/${res.document_number}/pdf` mais la route Flask attend `/api/invoices/<int:id>/pdf`.

**Décision** : Modifier la réponse JSON de `create_pos_transaction()` pour inclure `document_id` (l'ID de la facture créée). Le frontend utilisera `document_id` au lieu de `document_number` pour l'URL PDF.

**Détail backend** : Ajouter `'document_id': inv_id` dans le return JSON (app.py:1905-1909).
**Détail frontend** : Remplacer `res.document_number` par `res.document_id` dans l'URL.

---

## ADR 5: Correction Bug 8 — Remises client dans le POS

**Contexte** : La route `create_pos_transaction()` n'applique pas `get_price_for_customer()`. Elle utilise directement l'`unit_price` envoyé par le frontend, sans tenir compte du type de client (loyal/student/école).

**Décision** : Appliquer `get_price_for_customer()` dans `create_pos_transaction()` quand un `customer_id` est fourni et que ce n'est pas "client comptoir".

**Détail** :
```python
customer = conn.execute('SELECT * FROM customers WHERE id=?', (customer_id,)).fetchone() if customer_id and not is_client_comptoir else None
if customer:
    unit_price = get_price_for_customer({'price_base': ..., 'price': ...}, customer['type'], customer['is_loyal'])
```

**Impact** : Le prix final (stocké dans `pos_transaction_items` et dans la facture) sera celui du palier du client, pas le prix standard.

---

## ADR 6: Correction Bug 9 — Double discount dans recalculate_invoice

**Contexte** : `recalculate_invoice()` (app.py:1334-1344) calcule `subtotal = sum(item['line_total'])` où `line_total` est déjà après remise, puis soustrait à nouveau `discount_total` dans `total = subtotal - discount_total + tax_amount`.

**Décision** : Corriger `recalculate_invoice` pour calculer `subtotal` à partir du prix brut (qty × unit_price), pas du `line_total` déjà remisé.

**Avant:**
```python
subtotal = sum(item['line_total'] for item in items)
```
**Après:**
```python
subtotal = sum(item['quantity'] * item['unit_price'] for item in items)
```

---

## ADR 7: Correction Bug 10 — Collision numérotation FAC

**Contexte** : Le POS et la création manuelle d'invoices (via le formulaire de facturation) utilisent tous deux le préfixe `FAC-{YYYYMMDD}-{SEQ:04d}`. POS séquence auto-incrémentée, création manuelle utilise `MAX(id)` qui peut diverger.

**Décision** : Créer une table `sequences` pour centraliser le compteur FAC :

```sql
CREATE TABLE IF NOT EXISTS sequences (
    name TEXT PRIMARY KEY,
    current_value INTEGER DEFAULT 0
);
```

- Le POS lit `sequences WHERE name = 'fac_counter'` et incrémente atomiquement
- La création manuelle d'invoice fait de même
- La clé est `FAC-{date}-{seq:04d}`

**Alternative rejetée** : Utiliser `MAX()` sur `invoice_number` — race condition sous charge concurrente.

---

## ADR 8: Ordre d'implémentation

**Priorité** :
1. **S1** : ADR 1 (colonnes DB) — prérequis pour Bugs 1-2
2. **S2** : ADR 2, 3, 6 (Bugs 1, 2, 9 — backend) — corrections routes
3. **S3** : ADR 4 (Bug 3 — frontend React) — PDF URL
4. **S4** : ADR 5 (Bug 8 — pricing client POS)
5. **S5** : ADR 7 (Bug 10 — séquence FAC)

Chaque story suit la boucle : implémentation → test gstack browse → validation → suivant.
