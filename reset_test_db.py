#!/usr/bin/env python3
"""
Reset test DB: fresh database with only:
  - 1 warehouse, 4 locations, 10 suppliers
  - 50 products (stock=50 each)
  - Main account = 10,000 DH
  - No customers, no invoices, no transactions
"""
import sqlite3
import os

DB_NAME = 'stock.db'

def reset_transactional_data(conn):
    """Vider toutes les données transactionnelles, garder les références (produits, clients, fournisseurs, etc.)"""
    conn.execute("PRAGMA foreign_keys = OFF")
    tables = [
        'invoice_items',
        'invoices',
        'pos_transaction_items',
        'pos_transactions',
        'pos_cash_movements',
        'pos_sessions',
        'purchase_order_items',
        'purchase_orders',
        'stock_movements',
        'main_account_transactions',
        'notifications',
        'reordering_rules',
    ]
    for table in tables:
        conn.execute(f"DELETE FROM {table}")
    for seq in ['ticket_counter', 'fac_counter', 'purchase_order_counter']:
        conn.execute("DELETE FROM sequences WHERE name = ?", (seq,))
    conn.execute("UPDATE main_account SET current_balance = initial_balance WHERE id = 1")
    conn.execute("UPDATE products SET quantity = 50")
    conn.execute("UPDATE stock SET quantity = 50")
    conn.execute("PRAGMA foreign_keys = ON")
    conn.commit()

def main():
    # Kill running Flask first
    os.system("kill $(lsof -ti:5001) 2>/dev/null")

    # Delete existing DB
    for f in [DB_NAME, DB_NAME + '-wal', DB_NAME + '-shm']:
        try:
            os.remove(f)
        except FileNotFoundError:
            pass

    # Init schema
    from app import init_db
    init_db()

    conn = sqlite3.connect(DB_NAME)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = OFF")
    print("\n=== RESET DATABASE FOR TESTING ===\n")

    # 1. Warehouse (init_db already created one — update it)
    wh = conn.execute("SELECT id FROM warehouses LIMIT 1").fetchone()
    wh_id = wh['id'] if wh else 1
    conn.execute("UPDATE warehouses SET name=?, address=?, manager=?, is_default=1 WHERE id=?",
                 ('Entrepôt Principal', 'Zone Industrielle, Casablanca', 'Admin', wh_id))
    print('  Warehouse: Entrepôt Principal')

    # 2. Locations (4)
    conn.execute("DELETE FROM locations")
    locs = [
        'Zone A - Rayons',
        'Zone B - Rayons',
        'Stock Principal',
        'Réception',
    ]
    for name in locs:
        conn.execute(
            'INSERT INTO locations (warehouse_id, name, type, capacity) VALUES (?, ?, ?, ?)',
            (wh_id, name, 'rack', 100)
        )
    loc_ids = [r[0] for r in conn.execute("SELECT id FROM locations ORDER BY id").fetchall()]
    print(f'  Locations: {len(loc_ids)} ({", ".join(locs)})')

    # 3. Suppliers (10)
    supplier_names = [
        'Papeterie Casablanca', 'Bureautique Maghreb', 'Fournitures Pro', 'Import Export SA',
        'Grossiste Central', 'DistriBureautique', 'TechnoSupply', 'MegaOffice',
        'Premier Fournisseur', 'Spécialiste Papeterie'
    ]
    for i, name in enumerate(supplier_names):
        conn.execute(
            'INSERT INTO suppliers (name, email, phone, address, contact_person) VALUES (?, ?, ?, ?, ?)',
            (name, f'contact{i+1}@{name.lower().replace(" ", "")}.ma',
             f'0522-{600000+i*10000}', f'{i+1} Rue Commercial', f'Contact-{i+1}')
        )
    print(f'  Suppliers: {len(supplier_names)}')

    # 4. Products (50) — all stock = 50
    products_data = [
        ('Stylo à bille bleu', 'STY001', 5.50, 4.00, 'Papeterie'),
        ('Stylo à bille noir', 'STY002', 5.50, 4.00, 'Papeterie'),
        ('Stylo fluorescent vert', 'STY003', 8.00, 6.00, 'Papeterie'),
        ('Crayon à papier HB', 'CRA001', 2.50, 1.80, 'Papeterie'),
        ('Gomme blanche', 'GOM001', 3.00, 2.00, 'Papeterie'),
        ('Taille-crayon', 'TAL001', 5.00, 3.50, 'Papeterie'),
        ('Règle 30cm', 'REG001', 8.00, 5.50, 'Papeterie'),
        ('Paire de ciseaux', 'SCI001', 12.00, 8.00, 'Bureautique'),
        ('Agrafeuse', 'AGR001', 25.00, 18.00, 'Bureautique'),
        ('Agraffes (boîte)', 'AGR002', 8.00, 5.50, 'Bureautique'),
        ('Ruban adhésif transparent', 'RUB001', 6.00, 4.00, 'Bureautique'),
        ('Ruban adhésif noir', 'RUB002', 7.00, 5.00, 'Bureautique'),
        ('Classeur rigide A4', 'CLA001', 18.00, 12.00, 'Papeterie'),
        ('Classeur souple A4', 'CLA002', 12.00, 8.00, 'Papeterie'),
        ('Feuilles perforées A4 (500)', 'FEU001', 35.00, 25.00, 'Papeterie'),
        ('Bloc-notes A5', 'BLOC001', 8.00, 5.50, 'Papeterie'),
        ('Cahier grand format (200p)', 'CAH001', 25.00, 18.00, 'Papeterie'),
        ('Cahier petit format (100p)', 'CAH002', 15.00, 10.00, 'Papeterie'),
        ('Trousse scolaire', 'TRO001', 35.00, 25.00, 'Fournitures'),
        ('Sac à dos scolaires', 'SAC001', 120.00, 85.00, 'Fournitures'),
        ('Calculatrice scientifique', 'CAL001', 45.00, 32.00, 'Fournitures'),
        ('Compas géométrie', 'COM001', 18.00, 12.00, 'Fournitures'),
        ('Equerre plastique', 'EQ001', 8.00, 5.00, 'Fournitures'),
        ('Rapporteur 180°', 'RAP001', 6.00, 4.00, 'Fournitures'),
        ('Marqueurs fluorescents (lot 6)', 'MAR001', 15.00, 10.00, 'Papeterie'),
        ('Stylos gel (lot 10)', 'SG001', 25.00, 18.00, 'Papeterie'),
        ('Piles AA (lot 8)', 'BAT001', 18.00, 12.00, 'Accessoires'),
        ('Piles AAA (lot 8)', 'BAT002', 15.00, 10.00, 'Accessoires'),
        ('Clé USB 16Go', 'USB01', 45.00, 30.00, 'Informatique'),
        ('Clé USB 32Go', 'USB02', 65.00, 45.00, 'Informatique'),
        ('Souris sans fil', 'SOU001', 35.00, 25.00, 'Informatique'),
        ('Clavier USB', 'CLA02', 45.00, 32.00, 'Informatique'),
        ('Tapis de souris', 'TAP001', 15.00, 10.00, 'Accessoires'),
        ('Hub USB 4 ports', 'HUB001', 35.00, 25.00, 'Accessoires'),
        ('Câble HDMI 2m', 'HDM01', 25.00, 18.00, 'Câbles'),
        ('Câble HDMI 5m', 'HDM02', 40.00, 28.00, 'Câbles'),
        ('Câble VGA 2m', 'VGA01', 20.00, 14.00, 'Câbles'),
        ('Support laptop aluminium', 'SUP001', 85.00, 60.00, 'Accessoires'),
        ('Webcam HD 720p', 'WEB01', 75.00, 55.00, 'Informatique'),
        ('Casque audio', 'CAS001', 65.00, 45.00, 'Informatique'),
        ('Enceinte Bluetooth', 'ENC01', 95.00, 70.00, 'Informatique'),
        ('Lampe bureau LED', 'LAM001', 55.00, 38.00, 'Éclairage'),
        ('Lampe clip USB', 'LAM002', 35.00, 25.00, 'Éclairage'),
        ('Support mural écran', 'SUP02', 120.00, 85.00, 'Accessoires'),
        ('Perforateur 2 trous', 'PER01', 45.00, 32.00, 'Bureautique'),
        ('Trombones (boîte)', 'TRO02', 5.00, 3.00, 'Bureautique'),
        ('Poinçon', 'POI01', 8.00, 5.00, 'Bureautique'),
        ('Badges magnétiques', 'BAD01', 12.00, 8.00, 'Accessoires'),
        ('Étiquettes adhesives', 'ETI01', 8.00, 5.50, 'Papeterie'),
        ('Colle stick', 'COL01', 6.00, 4.00, 'Papeterie'),
    ]

    sup_ids = [r[0] for r in conn.execute("SELECT id FROM suppliers ORDER BY id").fetchall()]
    for i, p in enumerate(products_data):
        sup_id = sup_ids[i % len(sup_ids)]
        loc_id = loc_ids[i % len(loc_ids)]
        price_base = round(p[2] * 0.6, 2)
        conn.execute("""
            INSERT INTO products (name, sku, barcode, quantity, min_quantity, max_quantity,
                price, price_base, price_loyal, price_school, price_student,
                tax_category, category, supplier_id, warehouse_id, location_id, purchase_price_avg, is_deleted)
            VALUES (?, ?, ?, 50, 10, 100,
                ?, ?, ?, ?, ?,
                '20', ?, ?, ?, ?, ?, 0)
        """, (
            p[0], p[1], 1234560001000 + i, p[2], price_base,
            round(p[2] * 0.9, 2), round(p[2] * 0.85, 2), round(p[2] * 0.8, 2),
            p[4], sup_id, wh_id, loc_id, price_base
        ))
    conn.commit()
    print(f'  Products: {len(products_data)} (all stock=50)')

    # 5. Main account
    conn.execute("UPDATE main_account SET initial_balance = 10000, current_balance = 10000 WHERE id = 1")
    print('  Main account: 10,000 DH')

    conn.execute("PRAGMA foreign_keys = ON")
    conn.close()

    # 6. Report
    conn = sqlite3.connect(DB_NAME)
    conn.row_factory = sqlite3.Row
    print('\n=== FINAL STATE ===')
    print(f'  Products:     {conn.execute("SELECT COUNT(*) FROM products").fetchone()[0]}')
    print(f'  Warehouses:   {conn.execute("SELECT COUNT(*) FROM warehouses").fetchone()[0]}')
    print(f'  Locations:    {conn.execute("SELECT COUNT(*) FROM locations").fetchone()[0]}')
    print(f'  Suppliers:    {conn.execute("SELECT COUNT(*) FROM suppliers").fetchone()[0]}')
    print(f'  Customers:    {conn.execute("SELECT COUNT(*) FROM customers").fetchone()[0]}')
    print(f'  Invoices:     {conn.execute("SELECT COUNT(*) FROM invoices").fetchone()[0]}')
    print(f'  POS trans:    {conn.execute("SELECT COUNT(*) FROM pos_transactions").fetchone()[0]}')
    print(f'  Stock moves:  {conn.execute("SELECT COUNT(*) FROM stock_movements").fetchone()[0]}')
    bal = conn.execute("SELECT current_balance FROM main_account WHERE id = 1").fetchone()[0]
    print(f'  Bank balance: {bal:.2f} DH')
    val = conn.execute("SELECT COALESCE(SUM(quantity * price), 0) FROM products").fetchone()[0]
    print(f'  Stock value:  {val:,.2f} DH')
    rupts = conn.execute("SELECT COUNT(*) FROM products WHERE quantity <= 0").fetchone()[0]
    print(f'  Ruptures:     {rupts}')
    conn.close()
    print('\n  OK. Prête pour les tests de production !')

if __name__ == '__main__':
    main()
