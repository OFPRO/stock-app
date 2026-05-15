---
project_name: 'stock-app'
user_name: 'Omar'
date: '2026-05-15'
sections_completed:
  ['technology_stack', 'language_rules', 'framework_rules', 'testing_rules', 'quality_rules', 'workflow_rules', 'anti_patterns']
status: 'complete'
rule_count: 38
optimized_for_llm: true
---

# Project Context for AI Agents

_This file contains critical rules and patterns that AI agents must follow when implementing code in this project. Focus on unobvious details that agents might otherwise miss._

---

## Technology Stack & Versions

**Backend:** Python 3.9+ · Flask 3.1.3 · SQLite3 (WAL mode, busy timeout 30s)
**Frontend:** React 19.2.4 · TypeScript ~5.9.3 · Vite 7.3.1
**UI:** shadcn/ui (Radix 1.4.3) · Tailwind CSS 4.2.1 · Lucide React 1.14.0 · TanStack Table 8.21.3
**Charts:** ApexCharts 5.11.0 + react-apexcharts 2.1.0
**Routing:** react-router-dom 7.15.0
**Font:** Geist Variable (@fontsource-variable/geist 5.2.8)
**Build:** Vite 7.3.1 + @vitejs/plugin-react 5.2.0
**Quality:** ESLint 9.39 (flat config) · Prettier 3.8 (with tailwindcss plugin)
**Python quality:** None (no ruff/black/mypy configured)

## Language-Specific Rules

### Python & Flask
- All SQL queries use `?` parameterized placeholders — NEVER f-string interpolation
- `get_db_ctx()` auto-commits on success, auto-rollbacks on exception — never call `conn.commit()` manually
- `get_db()` returns `sqlite3.Row` objects (access by column name or index); convert via `dict(row)` when needed
- Routes consumed by frontend MUST be prefixed `/api/...` and return JSON via `jsonify()`
- New routes go into the appropriate `routes/*.py` blueprint file — NEVER append to `app.py`
- Blueprint naming: `Blueprint('<name>', __name__, url_prefix='/api/<name>')`
- Error pattern: `except (ValueError, sqlite3.IntegrityError) as e: return jsonify({"error": str(e)}), 400`
- No bare `except:` — always `except Exception as e:`
- Every function signature gets type annotations
- 🚫 NEVER reference code from `*.bak` files (contain intentional SQL injection)

### TypeScript & React
- Strict mode enabled — no `any`, no implicit `any`, no `@ts-ignore`
- `@/` path alias mandatory: `import { x } from "@/lib/utils"` not `"src/lib/utils"`
- `cn()` utility (clsx + tailwind-merge) for all dynamic class merging — never raw template literals
- Vite env vars via `import.meta.env.VITE_*` — never `process.env`
- Components PascalCase, utilities camelCase, directories kebab-case
- Google-style JSDoc for all route functions and React component exports

## Framework-Specific Rules

### Flask
- Blueprints registered in `app.py` (~line 17-23) via `app.register_blueprint()`
- Flask serves dual role: API server (`/api/*` → JSON) AND static file server (serves `frontend/dist/`)
- `get_db_ctx()` for all DB transactions — never raw `sqlite3.connect()` in runtime queries
- Legacy Jinja templates are zombie pages; shrink on migration, delete only when all referrers are gone
- One page = ONE frontend: either React or Jinja, never hybrid

### React
- React Router v7 with `BrowserRouter` — all routes inside `<DashboardLayout>` wrapping `<Outlet />`
- Vite dev: proxies `/api` → `localhost:5001`; production: Flask serves `frontend/dist/`
- Layout pattern: `<PageHeader><Title/><Description/><Actions/></PageHeader>` + `<DataTable/>` or `<div className="grid gap-4">`
- State triad on every page: loading skeleton / empty state + CTA / error `Alert variant="destructive"`
- `DataTable` wrapper around TanStack Table — never create ad-hoc tables
- Dialog pattern: `const [open, setOpen] = useState(false)` on parent, form in separate component with `onSuccess`
- No state management library — prop drilling + React Context only; add abstraction only when 3+ components need the same data
- All API calls via raw fetch in `lib/api.ts` (no React Query/SWR/axios)
- shadcn/ui components are hand-copied in `src/components/ui/` — import from local wrappers, never from `@radix-ui/*`

### Dual Frontend Coexistence
- Before adding a React route, verify the same path isn't served by a Jinja template
- Every new page goes in React; only modify existing Jinja pages in-place
- React talks to Flask via JSON APIs only — zero Jinja context, zero session coupling

## Testing Rules

### Backend (pytest)
- One test file per blueprint: `tests/test_api_{module}.py` with class `Test{Module}API`
- Test naming: `test_{action}_{scenario}_expects_{result}`
- Always test failure modes: 404, 400, 409
- MUST-test business invariants: negative stock, immutable movements, tiered pricing, reorder logic
- Every new route lands with a corresponding test file — no exceptions
- `pip install pytest && python -m pytest tests/` must work from cold clone
- Use real temp file (`tempfile.mkdtemp()`) — `:memory:` + WAL mode = crash
- Each test gets own DB with `init_db()` — never share connections across tests
- Use `freezegun` for any test asserting on dates

### Frontend (roadmap)
- Phase 0: No frontend tests — documented gap
- Phase 1: Vitest + @testing-library/react + jsdom (3-line vitest.config.ts)
- Phase 2: MSW for API mocking
- Phase 3: Playwright E2E for critical flows (POS, invoice, stock entry)
- 3 tests per page: data loads → render, API fails → error state, empty → empty state
- No snapshot tests, no Storybook, no visual regression (low ROI for ERP)

## Code Quality & Style Rules

### Python (self-enforced — no linter)
- Imports order: stdlib → third‑party → local, grouped with blank line between
- All route functions get Google-style docstrings (Args/Returns for complex, one-line for trivial)
- Always `jsonify()` explicitly — never bare dict returns
- No function exceeds 80 lines; no route file exceeds 400 lines
- `request.json.get("key")` consistently — never bare `["key"]` without `.get()`
- `validate_id()` must early-return — never fall through
- URL converters: use `<int:id>` on all route params that are integer IDs
- All DB utility imports from `routes/db.py` only — no duplication

### TypeScript (ESLint + Prettier enforced)
- ESLint: typescript-eslint strict + react-hooks + react-refresh recommended configs
- Prettier: no semi, double quotes, trailingComma es5, printWidth 80, tabWidth 2
- Prettier tailwindcss plugin for automatic class sorting

### API Contract
- Every response should follow: `{"ok": bool, "data": ..., "error": ..., "meta": {...}}`
- Error responses always include `"error": str` — never `"message"` or other keys
- All secrets, DB paths, ports in a single `config.py` read from `.env` — never hardcoded

## Development Workflow Rules

### Pre-commit Gate
Before every commit in order:
1. `python app.py` boots without import error
2. `npx tsc --noEmit` in `frontend/` — zero tolerance
3. `npx lint` in `frontend/` — no errors
4. `python seed.py` runs clean — verifies DB round-trips

### Git
- Commit format: `<type>(<scope>): <imperative summary>` — body bullets with file:line refs
- Types: `feat`, `fix`, `refactor`, `chore`, `docs`, `seed`
- Atomic commits: one logical change per commit — no `"wip"` or `"fix"` commits
- Include delivery note: `Delivery: <feature> | Impact: <user change> | To verify: <test step>`
- Never commit: `*.bak`, `*.db*`, `__pycache__/`, `node_modules/`
- Delete backup branch and all `*.bak` files — git history is the rollback mechanism

### Remote & Versioning
- Add a remote (GitHub/GitLab) and push after every meaningful change — local disk is not a backup
- Maintain `VERSION` (semver) at project root + `CHANGELOG.md` with date + scope per entry
- Add schema version comment in `init_db()` and bump on each DDL change
- Schema changes additive-only (`ALTER TABLE ADD COLUMN`) — never DROP without sign-off

## Critical Don't-Miss Rules

### Blocked Patterns (cause production incidents)
- 🚫 `get_db_ctx()` nested re-entrance: never open a second `with get_db_ctx() as db:` inside an existing one
- 🚫 `sqlite3.Row` DECIMAL columns return strings — always cast: `float(row['price']) * int(row['qty'])`
- 🚫 `INSERT OR REPLACE` banned — it's `DELETE + INSERT` under the hood, child rows silently deleted. Use `ON CONFLICT DO UPDATE`
- 🚫 Never add a new `sqlite3.connect()` — always route through `routes/db.py`
- 🚫 Never reference `*.bak` files — contain intentional SQL injection via f-string

### Business Invariants (never compromise)
- ⛔ Pricing tiers (Normal / Loyal -15% / Student -15% / School -20%) = revenue model — never flatten
- ⛔ Stock validation: `SELECT qty >= amount` in the SAME transaction as the INSERT
- ⛔ POS immutable: no "edit transaction" — void/refund is a separate operation with audit trail
- ⛔ Prices and totals server-side only — never client-side computation
- ⛔ Invoice totals stored denormalized — never computed from line items at read time
- ⛔ WAL + no migration: every schema change needs `rm -f stock.db stock.db-wal stock.db-shm`

### UI/Frontend Hard Rules
- 🇫🇷 All user-facing strings in French — no i18n, no English defaults
- Never raw HTML (`<button>`, `<input>`, etc.) — always shadcn/ui components
- Never ad-hoc tables — always extend existing `DataTable` (TanStack Table)
- Every page implements state triad: skeleton ↔ empty state + CTA ↔ error Alert
- English in code (vars, comments), French in UI (labels, errors, toasts, dates)

---

## Usage Guidelines

**For AI Agents:**
- Read this file before implementing any code — know what would be accepted before you write
- Follow all rules exactly as documented; these represent the project's hard-won conventions
- When in doubt, prefer the more restrictive option — it's easier to relax than to fix
- Update this file if new patterns emerge that all agents should follow

**For Humans:**
- Keep this file lean and focused on agent needs — remove rules that become obvious over time
- Update when technology stack changes or new patterns emerge
- Review quarterly for outdated rules

Last Updated: 2026-05-15
