#!/usr/bin/env python3
"""
Reset test DB: fresh database with only:
  - 1 warehouse (Entrepôt Principal), 1 location (Zone principale)
  - Caisse 1 & Caisse 2
  - Main account = 0 DH
  - Categories preserved
  - No products, no customers, no suppliers, no transactions
"""
import sqlite3
import os

DB_NAME = 'stock.db'

def reset_transactional_data(conn):
    """Vider TOUTES les données, ne garder que les catégories + structure minimale"""
    conn.execute("PRAGMA foreign_keys = OFF")

    tables = [
        'invoice_items', 'invoices',
        'pos_transaction_items', 'pos_transactions', 'pos_cash_movements', 'pos_sessions',
        'purchase_order_items', 'purchase_orders',
        'stock_movements', 'stock',
        'main_account_transactions',
        'notifications', 'reordering_rules',
        'products',
        'customers', 'suppliers',
        'locations', 'warehouses',
    ]
    for table in tables:
        conn.execute(f"DELETE FROM {table}")

    conn.execute("DELETE FROM sequences")
    conn.execute("DELETE FROM sqlite_sequence")

    conn.execute("DELETE FROM main_account")
    conn.execute("INSERT INTO main_account (id, name, initial_balance, current_balance) VALUES (1, 'Compte Principal', 0, 0)")

    conn.execute("INSERT INTO warehouses (name, address, is_default) VALUES ('Entrepôt Principal', 'Adresse par défaut', 1)")
    wh_id = conn.execute("SELECT last_insert_rowid()").fetchone()[0]
    conn.execute("INSERT INTO locations (warehouse_id, name, type) VALUES (?, 'Zone principale', 'zone')", (wh_id,))

    conn.execute("DELETE FROM pos_registers WHERE name NOT IN ('Caisse 1', 'Caisse 2')")
    conn.execute("INSERT OR IGNORE INTO pos_registers (name, code) VALUES ('Caisse 1', 'CAISSE-01')")
    conn.execute("INSERT OR IGNORE INTO pos_registers (name, code) VALUES ('Caisse 2', 'CAISSE-02')")
    conn.execute("UPDATE pos_registers SET is_active = 1 WHERE name IN ('Caisse 1', 'Caisse 2')")

    conn.execute("PRAGMA foreign_keys = ON")
    conn.commit()

def main():
    os.system("kill $(lsof -ti:5001) 2>/dev/null")

    for f in [DB_NAME, DB_NAME + '-wal', DB_NAME + '-shm']:
        try:
            os.remove(f)
        except FileNotFoundError:
            pass

    from app import init_db
    init_db()

    conn = sqlite3.connect(DB_NAME)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = OFF")
    print("\n=== RESET DATABASE FOR TESTING ===\n")

    reset_transactional_data(conn)
    print('  Entrepôt Principal + Zone principale')
    print('  Caisse 1 & Caisse 2')
    print('  Compte Principal: 0 DH')
    print('  Catégories conservées')

    conn.execute("PRAGMA foreign_keys = ON")
    conn.close()

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
    print(f'  POS registers:{conn.execute("SELECT COUNT(*) FROM pos_registers").fetchone()[0]}')
    bal = conn.execute("SELECT current_balance FROM main_account WHERE id = 1").fetchone()[0]
    print(f'  Bank balance: {bal:.2f} DH')
    conn.close()
    print('\n  OK. Base réinitialisée.')

if __name__ == '__main__':
    main()
