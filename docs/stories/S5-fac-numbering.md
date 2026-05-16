# S5: Séquence FAC centralisée

## Objectif
Éliminer la collision de numérotation FAC entre le POS et la création manuelle d'invoices en utilisant une table de séquence partagée.

## Bug corrigé
- Bug 10 (moyen) : le POS et la création manuelle d'invoices peuvent générer le même FAC-{date}-{seq}.

## Modifications

### 1. `app.py` — `init_db()` — Ajouter table `sequences`

```python
c.execute('''
    CREATE TABLE IF NOT EXISTS sequences (
        name TEXT PRIMARY KEY,
        current_value INTEGER DEFAULT 0
    )
''')
```

### 2. Fonction utilitaire `next_sequence()` (nouvelle)

```python
def next_sequence(conn, name):
    """Atomically increment and return a sequence value"""
    conn.execute('INSERT OR IGNORE INTO sequences (name, current_value) VALUES (?, 0)', (name,))
    conn.execute('UPDATE sequences SET current_value = current_value + 1 WHERE name = ?', (name,))
    seq = conn.execute('SELECT current_value FROM sequences WHERE name = ?', (name,)).fetchone()[0]
    return seq
```

### 3. `create_pos_transaction()` — Utiliser la séquence partagée

Remplacer le bloc de génération FAC (lignes 1780-1790) :
```python
seq = next_sequence(conn, 'fac_counter')
today = datetime.now().strftime('%Y%m%d')
doc_number = f'FAC-{today}-{seq:04d}'
```

### 4. Routes de création manuelle d'invoice — Utiliser la séquence partagée

Identifier la route de création d'invoice (probablement vers ligne 1140) et remplacer la génération de `invoice_number` par `next_sequence(conn, 'fac_counter')`.

## Migration des données existantes

Initialiser le compteur FAC avec le max existant :
```python
max_fac = conn.execute("SELECT MAX(CAST(SUBSTR(invoice_number, -4) AS INTEGER)) FROM invoices WHERE invoice_number LIKE 'FAC-%'").fetchone()[0] or 0
conn.execute('INSERT OR REPLACE INTO sequences (name, current_value) VALUES (?, ?)', ('fac_counter', max_fac))
```

## Test
```bash
# Créer une facture manuelle et une facture POS
# Vérifier que les numéros FAC ne se chevauchent pas
```

## Critères d'acceptance
- [ ] POS et création manuelle d'invoice partagent le même compteur FAC
- [ ] Pas de doublon de FAC-{date}-{seq}
- [ ] Les tickets (Ticket-{date}-{seq}) ne sont pas affectés
- [ ] Migration des données existantes fonctionne
