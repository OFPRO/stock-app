from contextlib import contextmanager
import os
import sqlite3

DB_NAME = os.environ.get('STOCKPRO_DB_PATH', 'stock.db')

def validate_id(value):
    if value is None:
        return None
    if isinstance(value, str) and value.isdigit():
        return int(value)
    if isinstance(value, int) and value > 0:
        return value
    return None

def get_price_for_customer(product, customer_type, is_loyal):
    if isinstance(product, dict):
        base_price = product.get('price_base', 0)
        price = product.get('price', 0)
    else:
        base_price = product['price_base'] if 'price_base' in product.keys() else 0
        price = product['price'] if 'price' in product.keys() else 0
    normal_price = base_price if base_price > 0 else price
    if customer_type == 'ecole':
        return round(normal_price * 0.80, 2)
    if is_loyal:
        return round(normal_price * 0.85, 2)
    if customer_type == 'etudiant':
        return round(normal_price * 0.85, 2)
    return round(normal_price, 2)

def get_db():
    conn = sqlite3.connect(DB_NAME, check_same_thread=False, timeout=30)
    conn.row_factory = sqlite3.Row
    conn.execute('PRAGMA journal_mode=WAL')
    conn.execute('PRAGMA busy_timeout=30000')
    conn.execute('PRAGMA foreign_keys=ON')
    return conn

@contextmanager
def get_db_ctx():
    conn = sqlite3.connect(DB_NAME, check_same_thread=False, timeout=30)
    conn.row_factory = sqlite3.Row
    conn.execute('PRAGMA journal_mode=WAL')
    conn.execute('PRAGMA busy_timeout=30000')
    conn.execute('PRAGMA foreign_keys=ON')
    try:
        yield conn
        conn.commit()
    except:
        conn.rollback()
        raise
    finally:
        conn.close()
