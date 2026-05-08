from contextlib import contextmanager
import sqlite3
from typing import Generator

DB_NAME = 'stock.db'

@contextmanager
def get_db() -> Generator[sqlite3.Connection, None, None]:
    """Context manager for database connections.
    
    Usage:
        with get_db() as conn:
            conn.execute(...)
    """
    conn = sqlite3.connect(DB_NAME, check_same_thread=False)
    conn.row_factory = sqlite3.Row
    try:
        yield conn
    finally:
        conn.close()

def get_db_connection() -> sqlite3.Connection:
    """Get a database connection (legacy support).
    
    Note: Prefer using get_db() context manager instead.
    """
    conn = sqlite3.connect(DB_NAME, check_same_thread=False)
    conn.row_factory = sqlite3.Row
    return conn

def init_db():
    """Initialize database schema."""
    from app import app
    with app.app_context():
        from app import init_db as _init_db
        _init_db()