from contextlib import contextmanager
import sqlite3

DB_NAME = 'stock.db'

def validate_id(value):
    if value is None:
        return None
    if isinstance(value, str) and value.isdigit():
        return int(value)
    if isinstance(value, int) and value > 0:
        return value
    return None

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
    finally:
        conn.close()
