#!/usr/bin/env python3
"""Database optimization script - Add indexes for performance."""

import sqlite3

DB_NAME = 'stock.db'

def add_indexes():
    conn = sqlite3.connect(DB_NAME)
    c = conn.cursor()
    
    indexes = [
        # Products indexes
        ('idx_products_warehouse', 'CREATE INDEX IF NOT EXISTS idx_products_warehouse ON products(warehouse_id)'),
        ('idx_products_category', 'CREATE INDEX IF NOT EXISTS idx_products_category ON products(category)'),
        ('idx_products_is_deleted', 'CREATE INDEX IF NOT EXISTS idx_products_is_deleted ON products(is_deleted)'),
        ('idx_products_supplier', 'CREATE INDEX IF NOT EXISTS idx_products_supplier ON products(supplier_id)'),
        
        # Stock movements indexes
        ('idx_movements_product', 'CREATE INDEX IF NOT EXISTS idx_movements_product ON stock_movements(product_id)'),
        ('idx_movements_created', 'CREATE INDEX IF NOT EXISTS idx_movements_created ON stock_movements(created_at)'),
        ('idx_movements_type', 'CREATE INDEX IF NOT EXISTS idx_movements_type ON stock_movements(type)'),
        
        # Stock indexes
        ('idx_stock_product', 'CREATE INDEX IF NOT EXISTS idx_stock_product ON stock(product_id)'),
        ('idx_stock_location', 'CREATE INDEX IF NOT EXISTS idx_stock_location ON stock(location_id)'),
        
        # Invoices indexes
        ('idx_invoices_customer', 'CREATE INDEX IF NOT EXISTS idx_invoices_customer ON invoices(customer_id)'),
        ('idx_invoices_status', 'CREATE INDEX IF NOT EXISTS idx_invoices_status ON invoices(status)'),
        
        # Locations indexes
        ('idx_locations_warehouse', 'CREATE INDEX IF NOT EXISTS idx_locations_warehouse ON locations(warehouse_id)'),
        
        # Purchase orders indexes
        ('idx_orders_supplier', 'CREATE INDEX IF NOT EXISTS idx_orders_supplier ON purchase_orders(supplier_id)'),
        ('idx_orders_status', 'CREATE INDEX IF NOT EXISTS idx_orders_status ON purchase_orders(status)'),
    ]
    
    print("Adding database indexes...")
    for name, sql in indexes:
        try:
            c.execute(sql)
            print(f"  ✓ {name}")
        except Exception as e:
            print(f"  ✗ {name}: {e}")
    
    conn.commit()
    
    # Show index stats
    c.execute("SELECT name FROM sqlite_master WHERE type='index'")
    indexes = c.fetchall()
    print(f"\nTotal indexes: {len(indexes)}")
    
    conn.close()
    print("\n✓ Index optimization complete!")

if __name__ == '__main__':
    add_indexes()