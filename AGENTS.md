# OMNI Framework — Bootstrap

## Règle 0 — Charger le framework OMNI
Immédiatement après avoir lu ce fichier, lire dans cet ordre :
1. `OMNI_AGENTS.md` — Framework de routage complet (8 phases, 62 skills, transitions)
2. `OMNI_STATE.yaml` — État dynamique du projet (phase courante, artefacts, décisions)
3. `docs/project-context.md` — 38 règles business critiques du projet StockPro

Toute action, planification ou décision doit respecter les phases et transitions
définies dans OMNI_AGENTS.md. Ne jamais contourner le framework.

## Règle 1 — Connaître l'état courant du projet
Lire OMNI_STATE.yaml pour savoir :
- Quelle phase est active (0-7)
- Quels artefacts ont été produits (PRD, architecture, epics, etc.)
- Quels blockers existent
- Quelles décisions ont été prises

## Règle 2 — Progression par phases
Avancer phase par phase selon OMNI_AGENTS.md. Ne pas sauter de phase.
Quand une phase est terminée, mettre à jour OMNI_STATE.yaml avec les nouveaux artefacts.

---

# StockPro — Gestion de Stock

> **⚠️ AI Agents: Read `stock-app/docs/project-context.md` before implementing any code.**
> It contains 38 critical rules covering language patterns, framework conventions, testing, code quality, workflow, and business invariants gathered via BMad multi-agent discovery.

## Graphify Knowledge Graph

This project has a knowledge graph at `graphify-out/` built by Graphify (v0.8.14). Use it for structural codebase queries instead of grepping files.

- `graphify-out/graph.json` — full graph (2827 nodes, 6907 edges, 149 communities)
- `graphify-out/graph.html` — interactive visualization (open in browser)
- `graphify-out/GRAPH_REPORT.md` — architecture summary with god nodes, communities, and cross-connections

**Query the graph (preferred over Grep/Grep):**
```
/graphify query "<question>"
/graphify path "NodeA" "NodeB"
/graphify explain "NodeName"
```

**Update after code changes:**
```
graphify update .    # AST-only, no API cost
```

Key god nodes: `constructor()` (153 edges), `decode()` (144), `encode()` (119), `Endpoint` (97)
Key cross-community bridges: `InvoiceItem`, `get_price_for_customer()`, `Endpoint`

### Graphify CLI commands
```bash
graphify query "..."                     # BFS traversal
graphify query "..." --dfs               # DFS traversal  
graphify path "ConceptA" "ConceptB"      # shortest path
graphify explain "FunctionName"          # node + neighbors
graphify update .                        # re-extract changed code
```

---

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
- `app.py` — REST routes in one file (~2270 lines). Covers: Orders, Reorder Rules, Replenishment, Notifications, Invoices, POS/Caisse, Main Account, Reports (CSV/PDF)
- `routes/db.py` — shared DB connection utilities (`get_db`, `get_db_ctx`, `validate_id`)
- `routes/products.py` (8 routes), `routes/kpis.py` (22 routes), `routes/customers.py` (5 routes), `routes/suppliers.py` (4 routes), `routes/warehouses.py` (7 routes), `routes/locations.py` (4 routes) — Blueprint files **registered** in `app.py` (lines 17-23).
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
- `routes/db.py` provides `get_db()` and `get_db_ctx()` — app.py imports `get_db` from here.
- DB initialization (`init_db`) still uses `sqlite3.connect` directly (not shared module).
- No Python linter/type checker (ruff/black/mypy) — self-enforced conventions in `project-context.md`.
- 13 git commits (`main` branch), no remotes.
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
