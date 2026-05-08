#!/usr/bin/env python3
"""
Enriched Seed Script - Realistic Data for Dashboard Testing
"""
import sqlite3, random
from datetime import datetime, timedelta

DB_NAME = 'stock.db'

def main():
    conn = sqlite3.connect(DB_NAME)
    conn.row_factory = sqlite3.Row  # Add this for named column access
    print("\n=== ENRICHED SEEDING DATABASE ===\n")
    
    today = datetime.now()
    
    # Clean existing data
    tables = ['invoice_items', 'invoices', 'stock_movements', 'stock', 'notifications', 
              'reordering_rules', 'purchase_order_items', 'purchase_orders', 
              'products', 'customers', 'locations', 'suppliers', 'warehouses']
    for t in tables:
        conn.execute(f'DELETE FROM {t}')
    conn.commit()
    
    # ==== WAREHOUSES ====
    conn.execute('INSERT INTO warehouses (id, name, address, manager, is_default) VALUES (1, "Entrepôt Principal", "Zone Industrielle, Casablanca", "Admin", 1)')
    conn.execute('INSERT INTO warehouses (name, address, manager) VALUES ("Entrepôt Fès", "Zone Artisanale, Fès", "Mohamed")')
    conn.commit()
    wh_ids = [r[0] for r in conn.execute('SELECT id FROM warehouses').fetchall()]
    print(f'✓ {len(wh_ids)} warehouses')
    
    # ==== LOCATIONS ====
    locations = [
        ('Zone A - Rayons', 'rack', 100),
        ('Zone B - Rayons', 'rack', 100),
        ('Zone C - Rayons', 'rack', 100),
        ('Zone D - Étagères', 'shelf', 80),
        ('Stock Principal', 'zone', 200),
        ('Réception', 'zone', 150),
        ('Expédition', 'zone', 150),
        ('Contrôle Qualité', 'zone', 50),
    ]
    for wh in wh_ids:
        for loc in locations:
            conn.execute('INSERT INTO locations (warehouse_id, name, type, capacity) VALUES (?, ?, ?, ?)', (wh, loc[0], loc[1], loc[2]))
    conn.commit()
    loc_ids = [r[0] for r in conn.execute('SELECT id FROM locations').fetchall()]
    print(f'✓ {len(loc_ids)} locations')
    
    # ==== SUPPLIERS ====
    supplier_names = [
        'Papeterie Casablanca', 'Bureautique Maghreb', 'Fournitures Pro', 'Import Export SA',
        'Grossiste Central', 'DistriBureautique', 'TechnoSupply', 'MegaOffice',
        'Premier Fournisseur', 'Spécialiste Papeterie'
    ]
    for i, name in enumerate(supplier_names):
        conn.execute('INSERT INTO suppliers (name, email, phone, address, contact_person) VALUES (?, ?, ?, ?, ?)',
                   (name, f'contact{i+1}@{name.lower().replace(" ", "")}.ma', f'0522-{600000+i*10000}', f'{i+1} Rue Commercial', f'Contact-{i+1}'))
    conn.commit()
    sup_ids = [r[0] for r in conn.execute('SELECT id FROM suppliers').fetchall()]
    print(f'✓ {len(sup_ids)} suppliers')
    
    # ==== PRODUCTS (50) with realistic pricing ====
    products_data = [
        ('Stylo à bille bleu', 'STY001', 5.50, 4.00, 'Papeterie', 50),
        ('Stylo à bille noir', 'STY002', 5.50, 4.00, 'Papeterie', 30),
        ('Stylo fluorescent vert', 'STY003', 8.00, 6.00, 'Papeterie', 45),
        ('Crayon à papier HB', 'CRA001', 2.50, 1.80, 'Papeterie', 100),
        ('Gomme blanche', 'GOM001', 3.00, 2.00, 'Papeterie', 80),
        ('Taille-crayon', 'TAL001', 5.00, 3.50, 'Papeterie', 60),
        ('Règle 30cm', 'REG001', 8.00, 5.50, 'Papeterie', 40),
        ('Paire de ciseaux', 'SCI001', 12.00, 8.00, 'Bureautique', 25),
        ('Agrafeuse', 'AGR001', 25.00, 18.00, 'Bureautique', 15),
        ('Agraffes (boîte)', 'AGR002', 8.00, 5.50, 'Bureautique', 50),
        ('Ruban adhésif transparent', 'RUB001', 6.00, 4.00, 'Bureautique', 70),
        ('Ruban adhésif noir', 'RUB002', 7.00, 5.00, 'Bureautique', 35),
        ('Classeur rigide A4', 'CLA001', 18.00, 12.00, 'Papeterie', 40),
        ('Classeur souple A4', 'CLA002', 12.00, 8.00, 'Papeterie', 50),
        ('Feuilles perforées A4 (500)', 'FEU001', 35.00, 25.00, 'Papeterie', 20),
        ('Bloc-notes A5', 'BLOC001', 8.00, 5.50, 'Papeterie', 60),
        ('Cahier grand format (200p)', 'CAH001', 25.00, 18.00, 'Papeterie', 35),
        ('Cahier petit format (100p)', 'CAH002', 15.00, 10.00, 'Papeterie', 45),
        ('Trousse scolaire', 'TRO001', 35.00, 25.00, 'Fournitures', 20),
        ('Sac à dos scolaires', 'SAC001', 120.00, 85.00, 'Fournitures', 15),
        ('Calculatrice scientifique', 'CAL001', 45.00, 32.00, 'Fournitures', 25),
        ('Compas géométrie', 'COM001', 18.00, 12.00, 'Fournitures', 30),
        ('Equerre plastique', 'EQ001', 8.00, 5.00, 'Fournitures', 50),
        ('Rapporteur 180°', 'RAP001', 6.00, 4.00, 'Fournitures', 40),
        ('Marqueurs fluorescents (lot 6)', 'MAR001', 15.00, 10.00, 'Papeterie', 55),
        ('Stylos gel (lot 10)', 'SG001', 25.00, 18.00, 'Papeterie', 40),
        ('Piles AA (lot 8)', 'BAT001', 18.00, 12.00, 'Accessoires', 30),
        ('Piles AAA (lot 8)', 'BAT002', 15.00, 10.00, 'Accessoires', 30),
        ('Clé USB 16Go', 'USB01', 45.00, 30.00, 'Informatique', 20),
        ('Clé USB 32Go', 'USB02', 65.00, 45.00, 'Informatique', 15),
        ('Souris sans fil', 'SOU001', 35.00, 25.00, 'Informatique', 25),
        ('Clavier USB', 'CLA02', 45.00, 32.00, 'Informatique', 20),
        ('Tapis de souris', 'TAP001', 15.00, 10.00, 'Accessoires', 40),
        ('Hub USB 4 ports', 'HUB001', 35.00, 25.00, 'Accessoires', 15),
        ('Câble HDMI 2m', 'HDM01', 25.00, 18.00, 'Câbles', 30),
        ('Câble HDMI 5m', 'HDM02', 40.00, 28.00, 'Câbles', 20),
        ('Câble VGA 2m', 'VGA01', 20.00, 14.00, 'Câbles', 25),
        ('Support laptop aluminium', 'SUP001', 85.00, 60.00, 'Accessoires', 10),
        ('Webcam HD 720p', 'WEB01', 75.00, 55.00, 'Informatique', 15),
        ('Casque audio', 'CAS001', 65.00, 45.00, 'Informatique', 20),
        ('Enceinte Bluetooth', 'ENC01', 95.00, 70.00, 'Informatique', 12),
        ('Lampe bureau LED', 'LAM001', 55.00, 38.00, 'Éclairage', 18),
        ('Lampe clip USB', 'LAM002', 35.00, 25.00, 'Éclairage', 25),
        ('Support mural écran', 'SUP02', 120.00, 85.00, 'Accessoires', 8),
        ('Perforateur 2 trous', 'PER01', 45.00, 32.00, 'Bureautique', 20),
        ('Trombones (boîte)', 'TRO02', 5.00, 3.00, 'Bureautique', 80),
        ('Poinçon', 'POI01', 8.00, 5.00, 'Bureautique', 40),
        ('Badges magnétiques', 'BAD01', 12.00, 8.00, 'Accessoires', 50),
        ('Étiquettes adhesives', 'ETI01', 8.00, 5.50, 'Papeterie', 60),
        ('Colle stick', 'COL01', 6.00, 4.00, 'Papeterie', 70),
    ]
    
    prod_ids = []
    for i, p in enumerate(products_data):
        # Mix stock levels: some low, some out of stock, some well stocked
        max_qty = max(p[5], 20)  # Ensure at least 20
        if i < 5:
            qty = 0  # Out of stock
        elif i < 10:
            qty = random.randint(1, 4)  # Very low
        elif i < 20:
            qty = random.randint(5, min(15, max_qty))  # Low (need reorder)
        else:
            qty = random.randint(15, max_qty)  # Normal stock
        
        # Some with expiry dates (for DLC testing)
        expiry = None
        if i % 10 == 0:
            expiry = (today + timedelta(days=random.randint(10, 60))).strftime('%Y-%m-%d')
        
        conn.execute('''
            INSERT INTO products (name, sku, barcode, quantity, min_quantity, max_quantity, price,
            price_base, price_loyal, price_school, price_student, tax_category, category, 
            supplier_id, warehouse_id, location_id, lot_number, expiry_date, is_deleted)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
        ''', (p[0], p[1], 1234560001000+i, qty, 10, p[5], p[2], p[2], p[2]*0.9, p[2]*0.85, p[2]*0.8, 
              '20', p[4], random.choice(sup_ids), random.choice(wh_ids), random.choice(loc_ids),
              f'LOT-{2024}{i+1:03d}', expiry))
        prod_ids.append(conn.execute('SELECT last_insert_rowid()').fetchone()[0])
    conn.commit()
    print(f'✓ {len(prod_ids)} products')
    
    # ==== STOCK per location ====
    for p in prod_ids:
        conn.execute('INSERT INTO stock (product_id, location_id, quantity) VALUES (?, ?, ?)',
                    (p, random.choice(loc_ids), random.randint(0, 50)))
    conn.commit()
    print(f'✓ Stock entries created')
    
    # ==== STOCK MOVEMENTS (200) over last 30 days ====
    movement_types = ['in', 'out']
    for _ in range(200):
        days_ago = random.randint(0, 30)
        created_at = (today - timedelta(days=days_ago, hours=random.randint(0, 23), minutes=random.randint(0, 59))).strftime('%Y-%m-%d %H:%M:%S')
        mov_type = random.choice(movement_types)
        qty = random.randint(1, 30)
        product_id = random.choice(prod_ids)
        
        conn.execute('''INSERT INTO stock_movements (product_id, type, quantity, source_location_id, dest_location_id, note, created_at)
                     VALUES (?, ?, ?, ?, ?, ?, ?)''',
                    (product_id, mov_type, qty, random.choice(loc_ids), random.choice(loc_ids), 
                     f'Mouvement {mov_type}', created_at))
        
        # Update product quantity
        if mov_type == 'in':
            conn.execute('UPDATE products SET quantity = quantity + ? WHERE id = ?', (qty, product_id))
        else:
            conn.execute('UPDATE products SET quantity = quantity - ? WHERE id = ?', (qty, product_id))
    conn.commit()
    print(f'✓ 200 stock movements')
    
    # ==== CUSTOMERS (30) with realistic data ====
    customer_types = ['particulier']*12 + ['entreprise']*10 + ['ecole']*5 + ['etudiant']*3
    customer_names = [
        'École Ibn Khaldoun', 'Lycée Mohammed V', 'Centre de Formation Pro',
        'Entreprise ABC', 'SARL Tech Solutions', 'Société Maghreb Commerce',
        'Cabinet d\'Avocats', 'Clinique Santé Plus', 'Hôtel Royal',
        'Restaurant Casablanca', 'Café Marrakech', 'Auto-École',
    ]
    
    cust_ids = []
    for i in range(30):
        if i < len(customer_names):
            name = customer_names[i]
        else:
            name = f'Client-{i+1}'
        
        conn.execute('''
            INSERT INTO customers (name, type, email, phone, address, client_code, discount_rate, is_loyal, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', (name, customer_types[i], f'{name.lower().replace(" ", ".")}@email.ma', 
              f'06{random.randint(10000000,99999999)}', f'{i+1} Rue Example',
              f'CLI-2026-{i+1:04d}', random.choice([0, 5, 10, 15]), 
              1 if customer_types[i] in ['entreprise', 'ecole'] else 0, ''))
        cust_ids.append(conn.execute('SELECT last_insert_rowid()').fetchone()[0])
    conn.commit()
    print(f'✓ {len(cust_ids)} customers')
    
    # ==== INVOICES with realistic sales data (60) ====
    invoice_statuses = ['brouillon']*5 + ['envoyee']*15 + ['payee']*35 + ['annulee']*5
    
    inv_ids = []
    for i in range(60):
        days_ago = random.randint(0, 30)
        created_at = (today - timedelta(days=days_ago)).strftime('%Y-%m-%d')
        status = invoice_statuses[i]
        
        # Calculate realistic total
        items_count = random.randint(1, 8)
        subtotal = sum(random.uniform(20, 300) for _ in range(items_count))
        discount = subtotal * random.choice([0, 0.05, 0.1, 0.15])
        tax = (subtotal - discount) * 0.2
        total = subtotal - discount + tax
        
        # Due date 30 days after creation
        due_date = (datetime.strptime(created_at, '%Y-%m-%d') + timedelta(days=30)).strftime('%Y-%m-%d')
        
        paid_at = None
        if status == 'payee':
            paid_days = random.randint(1, min(30, days_ago + 1))
            paid_at = (today - timedelta(days=days_ago - paid_days)).strftime('%Y-%m-%d')
        
        conn.execute('''
            INSERT INTO invoices (invoice_number, customer_id, warehouse_id, status, subtotal, 
            discount_total, tax_amount, total, notes, due_date, paid_at, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', (f'FAC-2026-{i+1:05d}', random.choice(cust_ids), random.choice(wh_ids), status,
              subtotal, discount, tax, total, '', due_date, paid_at, created_at, created_at))
        inv_ids.append(conn.execute('SELECT last_insert_rowid()').fetchone()[0])
    conn.commit()
    print(f'✓ {len(inv_ids)} invoices')
    
    # ==== INVOICE ITEMS (realistic) ====
    total_sales = 0
    for inv_id in inv_ids[:50]:
        # Skip items for cancelled invoices
        inv_status = conn.execute('SELECT status FROM invoices WHERE id = ?', (inv_id,)).fetchone()[0]
        if inv_status == 'annulee':
            continue
            
        items_count = random.randint(1, 6)
        for _ in range(items_count):
            product_id = random.choice(prod_ids)
            product = conn.execute('SELECT name, sku, price FROM products WHERE id = ?', (product_id,)).fetchone()
            qty = random.randint(1, 10)
            unit_price = product['price']
            discount = random.choice([0, 5, 10, 15])
            tax_rate = 20
            line_total = qty * unit_price * (1 - discount/100) * (1 + tax_rate/100)
            
            conn.execute('''
                INSERT INTO invoice_items (invoice_id, product_id, product_name, product_sku, 
                quantity, unit_price, discount_percent, tax_rate, line_total)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ''', (inv_id, product_id, product['name'], product['sku'], qty, unit_price, discount, tax_rate, line_total))
            
            # Update stock
            if inv_status != 'brouillon':
                conn.execute('UPDATE products SET quantity = quantity - ? WHERE id = ?', (qty, product_id))
                conn.execute('''INSERT INTO stock_movements (product_id, type, quantity, note, created_at)
                             VALUES (?, 'out', ?, ?, ?)''', (product_id, qty, f'Vente FAC-2026-{inv_id:05d}', 
                             (today - timedelta(days=random.randint(0, 30))).strftime('%Y-%m-%d %H:%M:%S')))
            
            total_sales += line_total
    conn.commit()
    print(f'✓ Invoice items created')
    
    # ==== PURCHASE ORDERS (30) ====
    po_statuses = ['brouillon']*5 + ['envoyee']*10 + ['recue']*12 + ['annulee']*3
    for i in range(30):
        days_ago = random.randint(5, 60)
        created_at = (today - timedelta(days=days_ago)).strftime('%Y-%m-%d')
        status = po_statuses[i]
        
        sent_at = None
        received_at = None
        if status == 'envoyee':
            sent_at = (today - timedelta(days=random.randint(1, days_ago))).strftime('%Y-%m-%d')
        elif status == 'recue':
            sent_at = (today - timedelta(days=random.randint(5, days_ago))).strftime('%Y-%m-%d')
            received_at = (today - timedelta(days=random.randint(1, 5))).strftime('%Y-%m-%d')
        
        conn.execute('''
            INSERT INTO purchase_orders (order_number, supplier_id, warehouse_id, status, total, 
            notes, created_at, sent_at, received_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', (f'PO-2026-{i+1:04d}', random.choice(sup_ids), random.choice(wh_ids), status,
              random.uniform(500, 15000), '', created_at, sent_at, received_at))
    conn.commit()
    print(f'✓ 30 purchase orders')
    
    # ==== REORDERING RULES ====
    for p in prod_ids[:25]:
        conn.execute('''
            INSERT INTO reordering_rules (product_id, warehouse_id, min_quantity, max_quantity, trigger_type, supplier_id)
            VALUES (?, ?, ?, ?, ?, ?)
        ''', (p, random.choice(wh_ids), random.randint(5, 15), random.randint(50, 100), 
              random.choice(['manual', 'auto']), random.choice(sup_ids)))
    conn.commit()
    print(f'✓ Reordering rules created')
    
    # ==== NOTIFICATIONS ====
    for _ in range(15):
        conn.execute('''
            INSERT INTO notifications (type, title, message, product_id, warehouse_id, is_read, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        ''', (random.choice(['stock', 'expiry', 'order']), f'Notification {random.randint(1,100)}', 
              'Message de notification', random.choice(prod_ids), random.choice(wh_ids), 
              random.randint(0, 1), (today - timedelta(days=random.randint(0, 7))).strftime('%Y-%m-%d')))
    conn.commit()
    
    # ==== STATS ====
    print('\n=== FINAL STATS ===')
    
    # Sales stats
    paid_invoices = conn.execute("SELECT COUNT(*) FROM invoices WHERE status = 'payee'").fetchone()[0]
    total_invoices = conn.execute("SELECT COUNT(*) FROM invoices").fetchone()[0]
    total_sales_amount = conn.execute("SELECT COALESCE(SUM(total), 0) FROM invoices WHERE status IN ('payee', 'envoyee')").fetchone()[0]
    unpaid_amount = conn.execute("SELECT COALESCE(SUM(total), 0) FROM invoices WHERE status = 'envoyee'").fetchone()[0]
    
    print(f'  Ventes: {paid_invoices} payées / {total_invoices} factures')
    CA = conn.execute("SELECT COALESCE(SUM(total), 0) FROM invoices WHERE status = 'payee'").fetchone()[0]
    print(f'  Chiffre Affaire: {CA:,.2f} DH')
    print(f'  Créances: {unpaid_amount:,.2f} DH')
    
    # Stock stats
    out_of_stock = conn.execute("SELECT COUNT(*) FROM products WHERE quantity <= 0").fetchone()[0]
    low_stock = conn.execute("SELECT COUNT(*) FROM products WHERE quantity > 0 AND quantity <= min_quantity").fetchone()[0]
    total_products = conn.execute("SELECT COUNT(*) FROM products").fetchone()[0]
    total_value = conn.execute("SELECT COALESCE(SUM(quantity * price), 0) FROM products").fetchone()[0]
    
    print(f'  Produits: {total_products} total')
    print(f'  Ruptures: {out_of_stock}')
    print(f'  Stock faible: {low_stock}')
    print(f'  Valeur stock: {total_value:,.2f} DH')
    
    conn.close()
    print('\n✓ ENRICHED SEED COMPLETE!')

if __name__ == '__main__':
    main()