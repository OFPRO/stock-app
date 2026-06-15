from contextlib import contextmanager
import os
import sqlite3

DB_NAME = os.environ.get('STOCKPRO_DB_PATH', 'stock.db')

def _safe_int(value, default):
    try:
        return int(value)
    except (ValueError, TypeError):
        return default

def validate_id(value):
    if value is None:
        return None
    if isinstance(value, str) and value.isdigit():
        return int(value)
    if isinstance(value, int) and value > 0:
        return value
    return None

def get_price_by_tier(product, tier):
    base_price = product.get('price_base', 0) or product.get('price', 0)
    if tier in ('fidele', 'price_loyal'):
        return product.get('price_loyal', 0) or base_price
    elif tier in ('gros', 'price_gros'):
        return product.get('price_gros', 0) or base_price
    return base_price

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
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()
