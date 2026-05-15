# StockPro — Gestion de Stock

## Stack
- **Backend:** Python Flask (single `app.py`, port 5001)
- **Database:** SQLite3 (`stock.db`, WAL mode, busy timeout 30s)
- **Frontend:** React (Vite) + shadcn/ui (migrated from monolithic SPA)
- **Charts:** ApexCharts (CDN) · **Icons:** Lucide React
- **UI:** French · Company: "Bibliotheque Badr" — Marrakech, Morocco

## Commands
```bash
python app.py        # Start dev server on localhost:5001
python seed.py       # Re-seed DB with 50 products, 30 customers, 60 invoices
```

DB auto-initializes on first `python app.py` run. No migrations tool.

## Architecture
- `app.py` — all REST routes in one file (~3575 lines). Covers: Products, Stock, Movements, Suppliers, Purchase Orders, Reordering Rules, Notifications, KPIs, Customers, Invoices, POS/Caisse, Main Account, Reports (CSV/PDF)
- `routes/kpis.py`, `routes/products.py` — Blueprint files **defined but NOT registered** in `app.py`. Migration in progress but incomplete.
- `templates/index.html` — legacy monolithic SPA (to be progressively replaced by React frontend)
- `templates/layout.html` — Jinja2 base with sidebar nav
- `templates/product_detail.html` — product detail page
- `add_indexes.py` — DB index optimization script (run manually)
- `stock.db*` — shared memory / WAL files; delete all three to fully reset

## Database
- 18 tables: warehouses, locations, products, stock, stock_movements, suppliers, purchase_orders, purchase_order_items, reordering_rules, notifications, customers, invoices, invoice_items, pos_sessions, pos_transactions, pos_transaction_items, pos_cash_movements, main_account, main_account_transactions
- Tiered pricing: Normal, Loyal (-15%), Student (-15%), School (-20%)
- All queries use parameterized placeholders (`?`) — no f-string interpolation

## Key Gotchas
- **Backup files contain SQL injection vulns** (`app.py.bak` uses raw f-string interpolation). Never copy from `*.bak` files.
- `routes/kpis.py` and `routes/products.py` duplicate DB connection logic and route code from `app.py` but are **not wired in** — they are dead code until registered.
- No tests, no lint config, no CI, no package manager.
- Single git commit (`main` branch), no remotes.
- No `.gitignore` — watch for `*.db*`, `*.pyc`, `*.bak`.
- Seed script cleans all tables before inserting (destructive).

---

# FRONTEND (MODERNIZATION LAYER)

## Frontend Stack (NEW)
- **Framework:** React + Vite (TypeScript)
- **UI System:** shadcn/ui (Radix-based components)
- **Styling:** Tailwind CSS
- **Icons:** Lucide React
- **Animations (optional):** Framer Motion
- **Data Tables:** TanStack Table (for ERP-grade grids)

## UI/UX Principles
The frontend replaces the legacy `templates/index.html` SPA progressively.

It must:
- preserve all existing business workflows
- NOT change backend logic
- NOT remove features
- NOT simplify workflows

But it SHOULD:
- modernize UI structure
- improve usability
- improve navigation
- improve readability of dense ERP data

## Design System Rules
Always use shadcn/ui components from:

frontend/src/components/ui

Never generate raw HTML UI elements such as:
- <button>
- <input>
- <table>
- <dialog>
- <select>

Instead use:
- Button
- Input
- Table
- Dialog
- Select
- Card
- Sheet
- Tabs
- Sidebar

## Layout Guidelines
Target UX is ERP-grade (not marketing UI):

- high-density data tables
- sticky headers in tables
- fast keyboard navigation
- sidebar-driven navigation
- modal-based workflows for edits
- minimal animations (only functional, not decorative)
- dark/light mode support
- responsive but desktop-first

## Migration Strategy
Frontend refactor must be:
- incremental (page-by-page)
- non-breaking
- parallel to Flask backend
- compatible with existing API routes

Order of migration:
1. Dashboard
2. Products module
3. Stock movements
4. Suppliers
5. Invoices
6. POS
7. Reports

## AI Coding Rules (IMPORTANT)
When using OpenCode or any AI assistant:

- ALWAYS reuse existing shadcn components
- NEVER redesign business workflows
- NEVER flatten complex ERP flows into simplified UI
- NEVER remove tables or data density
- ALWAYS prefer composition over rewriting
- ALWAYS keep compatibility with Flask API responses

## Inspiration
- Linear (navigation + clarity)
- Stripe Dashboard (data handling)
- SAP Fiori (enterprise structure)
- Notion (flexible layouts)
