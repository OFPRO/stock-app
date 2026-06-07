# StockPro iOS — Architecture & Implementation Plan

> **Authored**: AI agent orchestrating 69 skills (43 BMad + 14 wshobson/agents + 12 gstack operational)
> **Target**: Native iOS companion app for StockPro inventory management system
> **Backend**: Flask + SQLite (stock-app) — untouched, all features consumed via API
> **iOS Min**: iOS 17+ | **UI**: French (fr-FR) | **Auth**: Local PIN in Keychain (no backend changes)
> **Tooling**: gstack operational layer — review, QA, security, benchmark, ship, retro

---

## Table of Contents

1. [Product Strategy](#1-product-strategy)
2. [iOS Architecture](#2-ios-architecture)
3. [Design System](#3-design-system)
4. [Feature Architecture](#4-feature-architecture)
5. [Security & Auth](#5-security--auth)
6. [API Integration Layer](#6-api-integration-layer)
7. [Offline Strategy](#7-offline-strategy)
8. [Quality & Testing](#8-quality--testing)
9. [Phased Roadmap](#9-phased-roadmap)
10. [Skill Orchestration Map](#10-skill-orchestration-map)
11. [Quality & Operations Gates](#11-quality--operations-gates)
12. [Appendix A: API Reference](#appendix-a-api-reference--all-97-endpoints)
13. [Appendix B: Business Rules](#appendix-b-key-business-rules-ios-must-enforce)

---

## 1. Product Strategy

### 1.1 Vision

A native iOS companion that reproduces every feature, tab, and workflow of the StockPro desktop ERP in a mobile-native experience. The iPhone becomes a barcode scanner, a POS terminal, a real-time inventory dashboard, and an admin console — purpose-built for "Bibliotheque Badr" staff in Marrakech.

The iOS app is **independent**: it consumes the existing Flask API but requires zero changes to the backend. All 15 sidebar tabs from the legacy SPA are preserved, plus 2 new iOS-native screens (Settings, User Profile).

### 1.2 Target Users

| Persona | Primary Action | Device |
|---------|---------------|--------|
| Warehouse staff | Scan incoming/outgoing stock, move between locations | iPhone 15+ with LiDAR |
| Cashier | POS checkout with tiered pricing, cash management | iPhone + Bluetooth printer |
| Store manager | Dashboard KPIs, reports, reorder alerts, invoice approval | iPhone / iPad |
| Admin | Supplier/Customer/Warehouse CRUD, data management | iPad (larger canvas) |

### 1.3 Key Differentiators

- **Complete feature parity**: All 15 legacy tabs + 2 new iOS-native screens in one app
- **Scan-to-sell**: POS workflow starts with a barcode scan, ends with a receipt — 3 taps
- **Real-time stock**: Every scan validates against DB; negative stock is impossible (mirrors backend invariant)
- **French-first**: Every label, error, and notification in French from day one
- **Offline-tolerant**: Read dashboards, products, and catalogs offline; writes require connection
- **Tiered pricing baked in**: Normal, Loyal (-15%), Student (-15%), School (-20%) — visible at POS
- **Zero backend changes**: All 97 API routes consumed as-is with no modifications to `app.py`

### 1.4 Non-Goals

- Full backend replacement (Flask remains the source of truth)
- User management (local PIN only; no backend auth table)
- Real-time sync (polling-based; future consideration)
- Android support (future consideration)
- Backend modifications of any kind

---

## 2. iOS Architecture

### 2.1 Architectural Pattern: MVVM + Service Layer

```
┌─────────────────────────────────────────────────┐
│  View Layer (SwiftUI)                            │
│  - ProductListView, POSView, DashboardView        │
│  - Zero business logic                            │
│  - @ObservedObject / @StateObject for ViewModels  │
├─────────────────────────────────────────────────┤
│  ViewModel Layer                                  │
│  - DashboardViewModel, ProductListViewModel, ...   │
│  - Transforms API models → ViewState (enums)       │
│  - Handles user actions → calls Service layer      │
│  - Published properties for SwiftUI binding        │
├─────────────────────────────────────────────────┤
│  Service Layer                                    │
│  - ProductService, StockService, POSService, ...   │
│  - URLSession async/await networking               │
│  - SwiftData persistence (offline cache)           │
│  - Result<Success, AppError> return types          │
├─────────────────────────────────────────────────┤
│  Data Layer                                       │
│  - APIClient (URLSession + async/await)            │
│  - SwiftData models (local cache)                  │
│  - DTOs & Mappers (API JSON ↔ Domain models)      │
└─────────────────────────────────────────────────┘
```

**Rules:**
- Views import only ViewModels
- ViewModels import only Services
- Services import only APIClient + SwiftData
- APIClient has zero knowledge of Views or ViewModels
- Every layer boundary crossed via protocol

### 2.2 Dependency Flow

```
View → ViewModel (protocol) → Service (protocol) → APIClient / SwiftData
```

Manual DI via `@Environment`:

```swift
struct DIContainer {
    let productService: ProductServiceProtocol
    let stockService: StockServiceProtocol
    let posService: POSServiceProtocol
    let reportService: ReportServiceProtocol
    let authService: AuthServiceProtocol
}

struct DIContainerKey: EnvironmentKey {
    static let defaultValue: DIContainer = .preview
}

extension EnvironmentValues {
    var diContainer: DIContainer {
        get { self[DIContainerKey.self] }
        set { self[DIContainerKey.self] = newValue }
    }
}
```

### 2.3 Directory Structure (Full)

```
StockPro/
├── App/
│   ├── StockProApp.swift              # @main entry point
│   └── DIContainer.swift              # Manual DI setup
├── Views/
│   ├── Dashboard/                     # Full KPI dashboard
│   ├── Products/                      # List + Detail + CRUD forms
│   ├── Customers/                     # CRUD
│   ├── Suppliers/                     # CRUD
│   ├── Warehouses/                    # CRUD
│   ├── Locations/                     # CRUD
│   ├── StockMovements/                # History list
│   ├── Scanner/                       # Barcode + manual entry + history
│   ├── POS/                           # Full caisse module
│   ├── Orders/                        # Purchase orders workflow
│   ├── Invoices/                      # Invoice list + detail + mgmt
│   ├── Notifications/                 # Notification center
│   ├── Reports/                       # 5 report types + export
│   ├── Sessions/                      # POS session history
│   ├── MainAccount/                   # Compte Principal
│   ├── Settings/                      # PIN change, data mgmt, about
│   ├── Auth/                          # PIN login, profile card
│   └── Common/                        # Reusable view components
├── ViewModels/
│   ├── DashboardViewModel.swift
│   ├── ProductListViewModel.swift
│   ├── ProductDetailViewModel.swift
│   ├── ProductFormViewModel.swift
│   ├── CustomerListViewModel.swift
│   ├── CustomerFormViewModel.swift
│   ├── SupplierListViewModel.swift
│   ├── SupplierFormViewModel.swift
│   ├── WarehouseListViewModel.swift
│   ├── WarehouseFormViewModel.swift
│   ├── LocationListViewModel.swift
│   ├── LocationFormViewModel.swift
│   ├── StockMovementViewModel.swift
│   ├── ScannerViewModel.swift
│   ├── POSViewModel.swift
│   ├── OrderListViewModel.swift
│   ├── OrderFormViewModel.swift
│   ├── InvoiceListViewModel.swift
│   ├── InvoiceDetailViewModel.swift
│   ├── NotificationViewModel.swift
│   ├── ReportViewModel.swift
│   ├── SessionHistoryViewModel.swift
│   ├── MainAccountViewModel.swift
│   ├── SettingsViewModel.swift
│   └── AuthViewModel.swift
├── Services/
│   ├── ProductService.swift
│   ├── CustomerService.swift
│   ├── SupplierService.swift
│   ├── WarehouseService.swift
│   ├── LocationService.swift
│   ├── StockService.swift
│   ├── POSService.swift
│   ├── OrderService.swift
│   ├── InvoiceService.swift
│   ├── NotificationService.swift
│   ├── ReportService.swift
│   ├── SessionService.swift
│   ├── MainAccountService.swift
│   ├── KPIProvider.swift
│   └── AuthService.swift
├── Networking/
│   ├── APIClient.swift                # URLSession wrapper
│   ├── Endpoint.swift                 # All 97 endpoints enum
│   ├── DTOs/                          # Decodable API response types
│   └── Mappers/                       # DTO → Domain model
├── Data/
│   ├── Models/                        # SwiftData @Model types
│   ├── Store.swift                    # SwiftData container manager
│   └── CachePolicy.swift              # Staleness logic
├── Security/
│   ├── SecurityManager.swift          # Keychain wrapper
│   └── PINManager.swift               # Hash + verify local PIN
├── DesignSystem/
│   ├── Tokens.swift                   # Colors, spacing, typography
│   ├── Components/                    # Reusable UI components
│   └── Extensions/                    # ViewModifiers, helpers
└── Utilities/
    ├── AppError.swift                 # Unified error enum
    ├── Logger.swift                   # os_log wrapper
    ├── BarcodeScanner.swift           # AVFoundation wrapper
    └── Reachability.swift             # Network monitor
```

### 2.4 State Management

**No global state store.** Each ViewModel owns its own state.
- `@EnvironmentObject` for auth state (logged-in user, role)
- `@Binding` for parent ↔ child component state
- `NotificationCenter` for rare cross-cutting events (stock updated → refresh product list)

```swift
enum ViewState<T> {
    case loading
    case loaded(T)
    case error(AppError)
    case empty(String)
}
```

---

## 3. Design System

*(Unchanged from original plan — see full reference in prior version)*

### 3.1 Design Tokens

Inspired by **Linear** (clarity) + **SAP Fiori** (enterprise density). French labels throughout.

```swift
enum Spacing: CGFloat {
    case xs = 4; case sm = 8; case md = 12
    case lg = 16; case xl = 24; case xxl = 32
}

enum FontSize: CGFloat {
    case caption = 12; case body = 14; case bodyLarge = 16
    case subtitle = 18; case title = 22; case largeTitle = 28
}

enum FontWeight: String {
    case regular = "Inter-Regular"; case medium = "Inter-Medium"
    case semibold = "Inter-SemiBold"; case bold = "Inter-Bold"
}
```

### 3.2 Color System

```swift
enum AppColor {
    static let brand = Color(hex: "1B2A4A")        // Deep navy
    static let brandLight = Color(hex: "2C3F6B")
    static let accent = Color(hex: "E8A838")        // Moroccan amber
    static let accentPressed = Color(hex: "C98F2E")
    static let success = Color(hex: "2E7D32")
    static let warning = Color(hex: "F57F17")
    static let error = Color(hex: "C62828")
    static let info = Color(hex: "1565C0")
}
```

### 3.3 Component Hierarchy

| Component | Variants |
|-----------|----------|
| `StockButton` | primary, secondary, danger, ghost |
| `StockTextField` | default, search, barcode, currency |
| `StockCard` | default, pressable, selected |
| `StockBadge` | success, warning, error, info, neutral |
| `StockTable` | single-select, multi-select, expandable |
| `StockSkeleton` | card, row, circle |
| `StockEmptyState` | icon + title + message + action |
| `StockErrorView` | error + retry button |
| `StockToast` | success, error, warning (auto-dismiss) |
| `StockFormRow` | label + input for CRUD forms |
| `StockPicker` | type/status/category selector |
| `StockConfirmSheet` | destructive confirmation with reason |

### 3.4 Accessibility

- All interactive elements: minimum `44x44pt` hit target
- Dynamic Type: `UIFontMetrics` scaling on all text
- VoiceOver: `.accessibilityLabel` in French
- Reduce Motion: Respect `@Environment(\.accessibilityReduceMotion)`
- Contrast: All text meets WCAG AA 4.5:1

### 3.5 Motion & Interaction

| Trigger | Duration | Easing |
|---------|----------|--------|
| Button press | 100ms | spring(stiffness: 400, damping: 20) |
| Screen transition | 300ms | easeInOut |
| Modal present | 350ms | spring(stiffness: 300, damping: 25) |
| Toast appear | 250ms + 3s display | easeOut |
| Barcode scan success | 150ms | spring(stiffness: 500, damping: 15) |

---

## 4. Feature Architecture

### 4.1 Dashboard (Full Legacy Parity)

**Complete KPI hierarchy matching the legacy SPA:**

**Section 1 — Performance des Ventes (4 KPIs):**
| KPI | Endpoint | Chart |
|-----|----------|-------|
| CA Aujourd'hui (MAD) | `/api/kpis/sales?period=today` | — |
| Ventes Aujourd'hui | `/api/kpis/sales?period=today` | — |
| Ticket Moyen (MAD) | `/api/kpis/sales` | — |
| Marge Brute (%) | `/api/kpis/margins` | — |

**Section 2 — Situation Financière (5 KPIs):**
| KPI | Endpoint |
|-----|----------|
| Créances (MAD) | `/api/kpis/receivables` |
| Taux d'Encaissement (%) | `/api/kpis/receivables` |
| Valeur du Stock (MAD) | `/api/kpis/stock_value` |
| Produits en Rupture | `/api/kpis/alertes` |
| Compte Principal (MAD) | `/api/main-account` |

**Section 3 — Encaissements POS (3 KPIs):**
| KPI | Endpoint |
|-----|----------|
| Total Encaissé (MAD) | `/api/kpis/sessions-summary` |
| Dont Espèces | `/api/kpis/payment-methods` |
| Dont Carte Bancaire | `/api/kpis/payment-methods` |

**Charts (6 total, Swift Charts):**
1. Ventes Journalières (area) — `/api/kpis/sales-daily`
2. Ventes par Catégorie (pie) — `/api/kpis/categories-distribution`
3. Top 10 Produits (bar) — `/api/kpis/top-selling-products`
4. État des Factures (bar) — `/api/kpis/invoices-status`
5. Mouvements de Stock (line) — `/api/kpis/trends`
6. Répartition Marge (bar) — `/api/kpis/margins`

**Data Tables (4 total):**
1. À Commander — products below reorder threshold with suggested qty
2. Créances Clients — unpaid invoices grouped by customer
3. Ruptures de Stock — out-of-stock products
4. Périmés DLC < 30j — products expiring within 30 days

**Controls:** Date range picker (7j/30j/90j presets), warehouse filter dropdown, refresh button.

### 4.2 Products (Full CRUD)

**List** → `GET /api/products`
- Search by name or SKU (client-side filter)
- Category filter dropdown (from `GET /api/categories`)
- Show archived toggle (`?include_archived=true`)
- Row: image thumb, name, SKU, stock qty, price, category badge, actions

**Detail** → `GET /api/products/<id>`
- Header: image, name, SKU, Arabic name (if available), category, barcode
- Stock per location
- Price tiers (Normal / Loyal / Student / School) with editable values
- Purchase stats (total purchases, qty bought, avg purchase price)
- Sales stats (total sales, qty sold)
- Supplier info
- Movement history (last 20)
- Lot number + expiry date (if tracked)

**Create/Edit** → `POST` / `PUT /api/products/<id>`
- Modal form: Name, Arabic name, Description, SKU (auto), Barcode, Price
- Initial quantity, Min stock, Max stock
- Category picker (General, IT, Office, Stationery, Supplies, Cables, Accessories, Lighting)
- Extra price tiers (add/remove custom tiers)
- Expiry date, Lot number

**Delete** → `DELETE /api/products/<id>`
- Confirmation sheet with warning about cascade effects
- Soft delete (archived)

### 4.3 Customers (Full CRUD)

**List** → `GET /api/customers`
- Search by name, code, email
- Badge for "Fidèle" (Loyal) status
- Row: client code, name, type badge, email, phone

**Create/Edit** → `POST` / `PUT /api/customers/<id>`
- Form: Name, Type (particulier / entreprise / ecole), Email, Phone, Address
- Client code auto-generated

**Delete** → `DELETE /api/customers/<id>` (blocked if invoices exist)

### 4.4 Suppliers (Full CRUD)

**List** → `GET /api/suppliers`
- Row: name, email, phone, actions

**Create/Edit** → `POST` / `PUT /api/suppliers/<id>`
- Form: Name, Email, Phone, Address

**Delete** → `DELETE /api/suppliers/<id>`

### 4.5 Warehouses (Full CRUD)

**List** → `GET /api/warehouses`
- Row: name, address, manager, product count, actions

**Create/Edit** → `POST` / `PUT /api/warehouses/<id>`
- Form: Name, Address, Manager

**Delete** → `DELETE /api/warehouses/<id>`

### 4.6 Stock Locations (Full CRUD)

**List** → `GET /api/locations?warehouse_id=X`
- Row: name, type badge (Rayon / Étagère / Zone), capacity, current stock

**Create/Edit** → `POST` / `PUT /api/locations/<id>`
- Form: Warehouse picker, Name, Type picker, Capacity

**Delete** → `DELETE /api/locations/<id>` (blocked if stock exists)

### 4.7 Stock Movements

**History** → `GET /api/movements`
- Filterable by product, warehouse, date range, type (in/out/transfer)
- Table: date, product, type badge, quantity, location, note

**Entry** → `POST /api/movements` (type: "in")
**Exit** → `POST /api/movements` (type: "out")
**Transfer** → `POST /api/stock/transfer` (within warehouse)
**Inter-warehouse** → `POST /api/stock/inter-warehouse`

### 4.8 Scanner

**Camera** (AVFoundation):
- Video preview with overlay frame
- Start/Pause/Stop controls
- Flash/torch toggle
- Haptic feedback on successful scan
- Instant product card display

**Manual entry:**
- Text field barcode input with search button
- Enter key triggers lookup
- Useful for damaged barcodes

**Scan history:**
- Persistent list during session
- Product info for each scan
- Clear history button

### 4.9 POS / Caisse (HIGHEST PRIORITY)

**Session Management:**
- Open session with opening cash amount
- Close session with expected vs actual cash reconciliation
- Session status badge (open/closed)
- Auto-close on idle > 1 hour

**Product Entry:**
- Camera scan → add to cart
- Manual barcode entry
- Search bar with autocomplete
- Best-sellers quick-add chips (`GET /api/pos/best-sellers`)

**Cart Management:**
- Product list: name, unit price (per pricing tier), quantity (+/-), line total, remove
- Per-item price override (edit field)
- Clear cart button
- Empty cart state

**Customer & Pricing:**
- Customer dropdown → influences pricing tier
- Discount type selector: Auto (suit client), Normal, Fidèle, Étudiant, École
- Pricing tiers: Normal (0%), Loyal (-15%), Student (-15%), School (-20%)

**Cart Summary:**
- Sous-total HT, TVA, Remises, Total TTC

**Payment:**
- Methods: Espèces, Carte, Mixte (cash + card)
- Cash tendered input with change calculation
- Quick-cash buttons: "Montant exact", "Arrondi +10"
- Change display
- Credit checkbox (invoice pending payment) — visible when customer selected

**Cash Management:**
- Cash In: reasons — Crédit Client, Réapprovisionnement
- Cash Out: reasons — Café, Déjeuner, Eau, Transport, Autre
- Cash balance display
- Cash movements list

**Processing:**
- "ENCAISSER" button (green, disabled when cart empty or session closed)
- Creates POS transaction (`POST /api/pos/transactions`)
- Auto-creates invoice record
- Receipt preview + AirPrint

**Recent Transactions:**
- Last 20 transactions for current session
- Payment method icon, amount, timestamp, document number
- Refresh button

### 4.10 Purchase Orders (Full Workflow)

**List** → `GET /api/orders`
- Filterable by warehouse, status
- Columns: Order #, Date, Supplier, Total, Status badge, Received date, Actions

**Create/Edit** → `POST` / `PUT /api/orders`
- Order number (auto-generated)
- Date picker
- Status selector: Brouillon, Reçue, Payée
- Supplier dropdown
- **Rupture Tags**: tappable chips for out-of-stock products to quick-add
- Line items with:
  - Product search/autocomplete
  - Quantity input
  - Price input
  - Auto-calculated line total
  - Remove button
  - Add item button
- Running total
- Notes textarea

**Receive** → `PUT /api/orders/<id>` (status: "recue") — receives stock + records movement
**Pay** → `PUT /api/orders/<id>` (status: "paye") — debits main account, creates supplier invoice
**Cancel** → `DELETE` or `PUT /api/orders/<id>` (status: "annulee") — reverses stock, refunds main account
**Convert to Invoice** → opens conversion modal with customer picker + due date

### 4.11 Invoices (Full Management)

**List** → `GET /api/invoices`
- Filters: date range, status (Tous, Ticket de caisse, Brouillon, Envoyée, Payée, Annulée)
- Columns: Invoice #, Client, Created, Paid, Total, Status badge, Actions
- Status colors: ticket (blue), brouillon (gray), envoyée (amber), payée (green), annulée (red)

**Detail** → `GET /api/invoices/<id>`
- Full invoice: header, line items, totals, payment method, status history

**Create** → `POST /api/invoices` (from POS checkout, or standalone)

**Items CRUD:**
- Add item: `POST /api/invoices/<id>/items`
- Remove item: `DELETE /api/invoices/<id>/items/<item_id>`
- Auto-recalculates totals on change

**Status Management:**
- Mark as paid: `PUT /api/invoices/<id>` (status: "payee")
- Cancel: `PUT /api/invoices/<id>` (status: "annulee") — reverses stock
- Delete (blocked if paid)

**PDF** → `GET /api/invoices/<id>/pdf`

### 4.12 Reorder & Replenishment

- List reorder rules (`GET /api/reorder-rules`)
- Create/edit rules: product, supplier, min quantity, max quantity, trigger type
- Delete rule
- Replenishment suggestions (`GET /api/replenishment`) with auto-calculated order quantities

### 4.13 Notifications

**List** → `GET /api/notifications`
- Last 50 notifications with unread count
- Filterable by warehouse
- Row: icon, message, date, read/unread indicator

**Actions:**
- Mark single as read: `POST /api/notifications/<id>/read`
- Mark all as read: `POST /api/notifications/mark-all-read`
- Tap → navigate to relevant screen (product detail, order, etc.)
- Badge count on sidebar tab

**Push notifications** (Phase 6): APNs for low stock alerts, order received

### 4.14 Reports

**5 Report Types** — each with KPIs + charts + data table + export:

| Report | Endpoint | Content |
|--------|----------|---------|
| **Dashboard Business** | `/api/reports?type=overview` | CA, margins, stock KPIs, daily trend |
| **Ventes POS** | `/api/reports?type=sales` | Sales KPIs, daily chart, top products |
| **Achats** | `/api/reports?type=purchases` | Purchase analysis by supplier |
| **Stocks** | `/api/reports?type=rotation` | Rotation rate, DIO, products to order |
| **Performance** | `/api/reports?type=financial` | Margins, receivables, ticket moyen |

**Controls:** Period presets (7j / 30j / 90j / Année) + manual date range.

**Export:**
- PDF — native `UIPrintPageRenderer` or PDFKit
- CSV — share sheet with `.csv` file
- Print — `UIPrintInteractionController`

### 4.15 POS Sessions History

**List** → `GET /api/kpis/sessions-history`
- Filters: date range
- Search session number

**Summary KPIs:**
- Total Sessions, Sessions Fermées, CA Total, Transactions

**Detail** → `GET /api/kpis/sessions/<id>/details`
- Transactions list + cash movements
- Expected vs actual cash

**Export:** CSV share sheet

### 4.16 Main Account (Compte Principal)

**Dashboard:**
- Solde Actuel balance (MAD)
- Total Entrées (period)
- Total Sorties (period)

**Actions:**
- Deposit: `POST /api/main-account/deposit` — reason + amount
- Withdraw: `POST /api/main-account/withdraw` — reason + amount (checks balance)
- Transfer to POS: `POST /api/main-account/transfer-to-pos` — amount

**History:** Transaction table: date, type, amount, reason, note

### 4.17 Settings & Data Management

**Settings Screen** (sidebar, below history):
- User profile card: name, role (from local PIN)
- Change PIN: enter old → new → confirm (Keychain update)
- About: app version, build number, backend URL
- Debug section (hidden, long-press activated):
  - Reset demo data: `POST /api/reset-data` — clears transactional data
  - Seed demo data: `POST /api/seed-data` — inserts 20 products + 60 movements
- Logout button → returns to PIN login

### 4.18 Auth (Local PIN)

**Login Screen:**
- Numeric keypad (4-6 digit PIN)
- "Déverrouiller" button
- App lockout after 5 failed attempts (30s delay)

**PIN Manager** (CryptoKit + Keychain):
```swift
final class PINManager {
    func setPIN(_ pin: String) throws        // Hash + store in Keychain
    func verifyPIN(_ pin: String) -> Bool    // Hash + compare
    func changePIN(old: String, new: String) throws
    func hasPIN() -> Bool
}
```

**Keychain storage** via `SecurityManager`:
- Hashed PIN (SHA-256 + salt)
- Simulated user profile (name + role) for display
- No backend auth calls

---

## 5. Security & Auth

### 5.1 Current State

Backend has **ZERO authentication**. All 97 API routes are open. Acceptable for LAN-only deployment. The iOS app isolates itself with **device-level PIN only** — no backend modifications.

### 5.2 Local PIN Authentication

- PIN stored in iOS Keychain using CryptoKit (SHA-256 hashed + random salt)
- No backend involvement — purely device-local access control
- 5 failed attempts → 30-second lockout
- Biometric opt-in (Face ID / Touch ID) as convenience after initial PIN unlock

### 5.3 iOS Security

- Keychain storage for PIN hash (not UserDefaults)
- `Bundle.main.infoDictionary` for API base URL (configurable per environment)
- Certificate pinning (Phase 3+)
- App Transport Security (ATS) enabled — HTTPS only
- No sensitive data in UserDefaults

---

## 6. API Integration Layer

### 6.1 APIClient Design

```swift
actor APIClient {
    private let session: URLSession
    private let baseURL: URL
    private let decoder: JSONDecoder

    func request<T: Decodable>(
        _ endpoint: Endpoint,
        method: HTTPMethod = .get,
        body: Encodable? = nil
    ) async throws -> T { ... }
}
```

### 6.2 Endpoint Enum (All 97 Routes)

```swift
enum Endpoint {
    // Auth (no backend — local only)

    // Products (7 routes)
    case products(includeArchived: Bool)
    case product(Int)
    case createProduct
    case updateProduct(Int)
    case deleteProduct(Int)
    case productsForSale
    case categories

    // Customers (5 routes)
    case customers(search: String?)
    case createCustomer
    case customer(Int)
    case updateCustomer(Int)
    case deleteCustomer(Int)

    // Suppliers (4 routes)
    case suppliers
    case createSupplier
    case updateSupplier(Int)
    case deleteSupplier(Int)

    // Warehouses (2 routes)
    case warehouses
    case createWarehouse
    case warehouse(Int)
    case deleteWarehouse(Int)

    // Locations (4 routes)
    case locations(warehouseId: Int?)
    case createLocation
    case updateLocation(Int)
    case deleteLocation(Int)

    // Stock & Movements (6 routes)
    case movements(productId: Int?, warehouseId: Int?)
    case productMovements(Int)
    case createMovement
    case transferStock
    case interWarehouseTransfer
    case stockForProduct(Int)

    // POS (11 routes)
    case posSessions
    case openSession
    case closeSession(Int)
    case createTransaction
    case recentTransactions(sessionId: Int?, limit: Int?)
    case transactionByInvoice(String)
    case cashMovements
    case createCashMovement
    case posCustomers
    case bestSellers
    case ticketPDF(String)

    // Orders (5 routes)
    case orders(warehouseId: Int?, status: String?)
    case createOrder
    case updateOrder(Int)
    case orderItems(Int)
    case deleteOrder(Int)

    // Reorder Rules (4 routes)
    case reorderRules(warehouseId: Int?)
    case createReorderRule
    case updateReorderRule(Int)
    case deleteReorderRule(Int)

    // Replenishment (1 route)
    case replenishment

    // Invoices (9 routes)
    case invoices(status: String?, dateStart: String?, dateEnd: String?)
    case createInvoice
    case invoice(Int)
    case updateInvoice(Int)
    case deleteInvoice(Int)
    case invoiceItems(Int)
    case addInvoiceItem(Int)
    case deleteInvoiceItem(Int, itemId: Int)
    case invoicePDF(Int)

    // Invoice Stats (2 routes)
    case invoiceStats
    case receivables

    // Notifications (3 routes)
    case notifications(warehouseId: Int?)
    case markNotificationRead(Int)
    case markAllNotificationsRead

    // KPIs (23 routes)
    case mainKPIs
    case dashboardKPIs
    case alertes
    case stats
    case salesKPIs
    case margins
    case receivablesKPIs
    case invoicesStatus
    case salesDaily(days: Int)
    case categoriesDistribution
    case topSellingProducts(limit: Int)
    case sessionsHistory
    case sessionsSummary
    case sessionDetails(Int)
    case trends
    case topProducts
    case byLocation
    case warehouseOverview
    case ordersSummary
    case invoicesSummary
    case customersSummary
    case evolution
    case paymentMethods

    // Reports (2 routes)
    case report(type: String)
    case reportExport(type: String)

    // Main Account (4 routes)
    case mainAccount
    case mainAccountDeposit
    case mainAccountWithdraw
    case transferToPOS

    // Data Management (2 routes)
    case resetData
    case seedData

    var path: String { /* computed for each case */ }
    var method: HTTPMethod { /* .get / .post / .put / .delete */ }
}
```

### 6.3 Error Handling

```swift
enum AppError: LocalizedError {
    case network(URLError)
    case server(statusCode: Int, message: String?)
    case decoding(Error)
    case authentication(String)
    case validation([ValidationError])
    case notFound(String)
    case businessRuleViolation(String)
    case pinLockout(TimeInterval)
    case unknown(Error)

    var errorDescription: String? {
        switch self {
        case .network: return "Connexion perdue. Vérifiez votre réseau."
        case .server(_, let msg): return msg ?? "Erreur serveur. Réessayez."
        case .decoding: return "Données invalides reçues du serveur."
        case .authentication: return "Session expirée. Veuillez vous reconnecter."
        case .validation(let errors): return errors.map(\.message).joined(separator: "\n")
        case .notFound(let item): return "\(item) introuvable."
        case .businessRuleViolation(let msg): return msg
        case .pinLockout(let t): return "Trop de tentatives. Réessayez dans \(Int(t))s."
        case .unknown: return "Erreur inattendue. Réessayez."
        }
    }
}
```

### 6.4 Retry & Resilience

```swift
struct RetryPolicy {
    let maxRetries: Int = 2  // Read operations retry; writes do not
    let baseDelay: Duration = .seconds(1)
    let maxDelay: Duration = .seconds(5)
}
```

---

## 7. Offline Strategy

### 7.1 SwiftData Models

```swift
@Model final class CachedProduct {
    @Attribute(.unique) var id: Int
    var name: String; var sku: String; var price: Decimal
    var category: String; var stockQuantity: Int
    var lastUpdated: Date
}

@Model final class CachedCustomer {
    @Attribute(.unique) var id: Int
    var name: String; var clientCode: String
    var type: String; var lastUpdated: Date
}

@Model final class CachedSupplier {
    @Attribute(.unique) var id: Int
    var name: String; var lastUpdated: Date
}

@Model final class CachedKPI {
    @Attribute(.unique) var key: String
    var value: Decimal; var label: String; var lastUpdated: Date
}

@Model final class CachedWarehouse {
    @Attribute(.unique) var id: Int
    var name: String; var lastUpdated: Date
}

@Model final class CachedNotification {
    @Attribute(.unique) var id: Int
    var message: String; var isRead: Bool; var createdAt: Date
}
```

### 7.2 Cache Policy

| Data Type | Staleness | Strategy |
|-----------|-----------|----------|
| Dashboard KPIs | 10 min | Pull-to-refresh |
| Product list | 5 min | Background refresh on appear |
| Product detail | 2 min | Refresh on detail appear |
| Customers list | 10 min | Cache-then-network |
| Suppliers list | 1 hour | Cache-then-network |
| Warehouses list | 1 hour | Cache-then-network |
| Locations list | 1 hour | Cache-then-network |
| Stock quantity | 30 sec | Always fetch fresh for POS |
| Invoice history | 15 min | Background refresh |
| Notifications | 5 min | Poll on appear |
| Reports | Never cached | Always fetch fresh |
| POS transactions | Never cached | Real-time required |

### 7.3 Connectivity Handling

- **Read features**: Dashboard, product list, customers, suppliers, warehouses, locations, invoices, notifications — cache-then-network with staleness
- **Write operations**: All writes (create/edit/delete, POS, movements) require connection. Offline = disabled button + "Connexion requise" message
- **POS specifically**: Full-screen "Connexion requise pour la caisse" when offline. Stock validation must be real-time.

---

## 8. Quality & Testing

> **GStack operational layer**: `/health` tracks composite score, `/qa` runs browser-level testing, `/review` audits every PR, `/benchmark` catches regressions, `/investigate` enforces root-cause discipline.

### 8.1 Testing Pyramid

```
     /\
    /E2E\       ← 8 critical flows (POS checkout, scan-to-stock, CRUD each entity)
   /------\
  / Inte- \     ← Service layer with mock APIClient
 / gration \
/------------\
/ Unit Tests  \  ← ViewModels, Mappers, Formatters, Security
──────────────
```

### 8.2 Coverage Targets

- ViewModels: 90%+ (core business logic)
- Services: 80%+ (with mock API client)
- Mappers: 100% (pure transformations)
- Security: 100% (PIN hash/verify edge cases)
- Views: 0% (verified via previews + E2E)

### 8.3 Code Review Checklist

- [ ] No force unwraps outside tests
- [ ] All service calls wrapped in `do/catch`
- [ ] Every user-facing string in French (NSLocalizedString)
- [ ] No retain cycles (weak self in escaping closures)
- [ ] No magic numbers (use design tokens)
- [ ] SwiftUI views under 100 lines
- [ ] ViewModels under 300 lines
- [ ] Keychain items properly secured (kSecAttrAccessible)

### 8.4 Debugging

```swift
enum LogCategory {
    case networking, viewModel, scanning, persistence, auth, security
}

func log(_ message: String, category: LogCategory, level: OSLogType = .debug) {
    os_log("%{public}@", log: OSLog(subsystem: "com.bibliothequebadr.stockpro", category: category.rawValue), type: level, message)
}
```

---

## 9. Phased Roadmap

> **GStack gates embedded in every phase.**
> Cross-cutting: `/context-save` at end of every session → `/context-restore` at start of next.
> Every PR: `/review` pre-merge diff audit → `/ship`.

**Total: 7 phases / 18 sprints**

### Phase 0: Foundation (Sprint 1 — 2 weeks)
- **GStack**: `/plan-eng-review` architecture audit → `/cso` comprehensive 14-phase security audit → `/benchmark` baseline
- [ ] Xcode project setup, directory structure
- [ ] Design system tokens: colors, spacing, typography
- [ ] Core components: `StockButton`, `StockCard`, `StockTextField`, `StockBadge`, `StockFormRow`
- [ ] `APIClient` + `Endpoint` enum + `AppError`
- [ ] `DIContainer`, `Environment` injection
- [ ] `SecurityManager` + `PINManager` (Keychain, CryptoKit)
- [ ] PIN login screen (numeric keypad, 5-attempt lockout)
- [ ] `Reachability` monitoring + SwiftData setup
- [ ] User profile card (sidebar header placeholder)
- [ ] Tab-based navigation shell (sidebar for iPad later)

### Phase 1: Core Features — Products + Customers + Dashboard (Sprint 2-4 — 3 weeks)
- **GStack**: `/plan-eng-review` data flow audit → `/qa --quick` → `/review` → `/ship`
- [ ] **Dashboard — Performance des Ventes** (CA Jour, Ventes, Ticket Moyen, Marge)
- [ ] **Dashboard — Situation Financière** (Créances, Encaissement, Valeur Stock, Ruptures, Compte Principal)
- [ ] **Dashboard — Encaissements POS** (Total, Espèces, Carte)
- [ ] **Dashboard — 6 Charts** (Ventes, Catégories, Top 10, Factures, Mouvements, Marge)
- [ ] **Dashboard — 4 Data Tables** (À Commander, Créances, Ruptures, Périmés)
- [ ] Dashboard date/warehouse filters + refresh
- [ ] **Products list** — search, category filter, archive toggle
- [ ] **Products detail** — stock per location, tiers, movements, supplier
- [ ] **Products create/edit** — full form with Arabic name, extra prices
- [ ] **Products delete** — confirmation + soft archive
- [ ] **Customers list** — search by name/code/email
- [ ] **Customers CRUD** — create/edit/delete with type picker
- [ ] French localization (fr.lproj for all strings)
- [ ] Offline cache: products + customers + KPIs

### Phase 2: Scanner + POS 🔥 (Sprint 5-7 — 3 weeks)
- **GStack**: `/plan-ceo-review` HOLD SCOPE → `/plan-design-review` mockups → `/qa --standard` → `/investigate` → `/review` → `/ship`
- [ ] Barcode scanner (AVFoundation) — camera preview + haptics + torch
- [ ] Manual barcode entry text field
- [ ] Scan history list + clear button
- [ ] **POS session management** — open/close, opening cash
- [ ] **Scan-to-cart** — barcode → product with tiered price
- [ ] **Manual product add** — search + select for damaged barcodes
- [ ] **Best-sellers quick-add chips**
- [ ] **Customer selection** → pricing tier applied
- [ ] **Cart editing** — quantity (+/-), remove, clear
- [ ] **Per-item price override**
- [ ] **Cart summary** — sous-total, TVA, remises, total TTC
- [ ] **Payment** — Espèces, Carte, Mixte
- [ ] **Cash tendered** + change calculation
- [ ] **Quick-cash buttons** — "Montant exact", "Arrondi +10"
- [ ] **Credit sale** — checkbox (invoice pending)
- [ ] **Cash management** — Cash In/Out with reasons
- [ ] **Cash balance display** + cash movements list
- [ ] **Checkout** — transaction creation + invoice → receipt preview + AirPrint
- [ ] **Recent transactions** list per session
- [ ] Offline warning — full screen when POS required

### Phase 3: Admin CRUD — Suppliers + Warehouses + Locations (Sprint 8-9 — 2 weeks)
- **GStack**: `/qa --quick` → `/review` → `/ship`
- [ ] **Suppliers list** + **CRUD** (create/edit/delete)
- [ ] **Warehouses list** + **CRUD** (name, address, manager)
- [ ] **Locations list** + **CRUD** (type picker: Rayon/Étagère/Zone, capacity)
- [ ] **Stock movements history** — filterable by product/warehouse/type/date
- [ ] Offline cache: suppliers + warehouses + locations

### Phase 4: Orders + Invoices + Notifications (Sprint 10-12 — 3 weeks)
- **GStack**: `/plan-eng-review` → `/qa --standard` → `/review` → `/ship`
- [ ] **Reorder rules list** + **CRUD** (min/max, supplier, trigger)
- [ ] **Replenishment suggestions** — auto-calculated quantities
- [ ] **Purchase order list** — filter by warehouse/status
- [ ] **PO create** — product autocomplete, rupture tags, line items, running total
- [ ] **PO receive** — marks received, adds stock
- [ ] **PO pay** — debits main account
- [ ] **PO cancel** — reverses stock
- [ ] **PO convert to invoice** — customer picker + due date
- [ ] **Invoice list** — filter by date/status (5 statuses)
- [ ] **Invoice detail** — items, totals, payment method
- [ ] **Invoice items CRUD** — add/remove items, auto-recalculates
- [ ] **Invoice status** — mark paid, cancel (reverses stock)
- [ ] **Invoice PDF** — download/share
- [ ] **Notifications list** — last 50 with unread badge
- [ ] **Mark read** + **Mark all read**
- [ ] Push notification setup (APNs Phase 6)

### Phase 5: Reports + Sessions + Main Account + Settings (Sprint 13-15 — 3 weeks)
- **GStack**: `/qa --standard` → `/review` → `/ship`
- [ ] **Reports — 5 types**: Overview, Sales, Purchases, Stock, Financial
- [ ] Report period controls (7j/30j/90j/Année + manual)
- [ ] Report KPIs + charts + data tables
- [ ] **Export — PDF** (native PDFKit)
- [ ] **Export — CSV** (share sheet)
- [ ] **Export — Print** (UIPrintInteractionController)
- [ ] **Sessions history** — list with date filters
- [ ] Sessions summary KPIs (total sessions, CA, transactions)
- [ ] **Session detail** — transactions + cash movements
- [ ] **Sessions CSV export**
- [ ] **Main Account** — balance, total entries/out
- [ ] **Deposit** — amount + reason
- [ ] **Withdraw** — amount + reason (checks balance)
- [ ] **Transfer to POS**
- [ ] Main account transaction history
- [ ] **Settings screen** — profile card, about (version/build)
- [ ] **Change PIN** — old → new → confirm
- [ ] **Debug section** (long-press to reveal) — Reset data + Seed data
- [ ] **Logout** button → return to PIN login
- [ ] Push notifications (APNs) for low stock

### Phase 6: iPad + Release (Sprint 16-18 — 3 weeks)
- **GStack**: `/plan-design-review` iPad → `/qa --exhaustive` → `/health` (≥8.5) → `/cso` final → `/benchmark` final → `/ship` → `/retro` full-project
- [ ] iPad adaptive layout: portrait → landscape → split view
- [ ] Sidebar navigation for iPad (mirrors all 15+2 tabs)
- [ ] Dashboard: additional grid columns on iPad
- [ ] Split-view: product list ↔ detail, invoice list ↔ detail
- [ ] Keyboard shortcuts (iPad Magic Keyboard)
- [ ] Drag & drop: move products between locations
- [ ] Home Screen widget: low stock count
- [ ] Full VoiceOver pass with French labels
- [ ] WCAG AA audit using accessibility-compliance framework
- [ ] Dynamic Type scaling verification
- [ ] Performance profiling: Instruments Time Profiler, leaks, memory
- [ ] Memory optimization: image caching, lazy loading
- [ ] TestFlight beta: internal + external
- [ ] App Store assets: screenshots (French), description (French), icon
- [ ] CI/CD: GitHub Actions → TestFlight
- [ ] Bug bash: 3-day focused QA

---

## 10. Skill Orchestration Map

| Skill | Role | Applied In |
|-------|------|------------|
| **mobile-ios-design** | HIG compliance, navigation patterns, SwiftUI idioms | §2 Architecture, all feature specs |
| **architecture-patterns** | Clean Architecture layers, dependency inversion | §2 Architecture |
| **design-system-patterns** | Token system, component API, composition | §3 Design System |
| **visual-design-foundations** | Typography scale, 8pt grid, color theory | §3.1-3.2 Design System |
| **interaction-design** | Microinteractions, timing, haptics | §3.5, POS §4.9 |
| **responsive-design** | Adaptive layout, size classes, iPad | Phase 6 |
| **accessibility-compliance** | WCAG 2.2 AA, Dynamic Type, VoiceOver | §3.4, Phase 6 |
| **wcag-audit-patterns** | Audit methodology, violation classification | Phase 6 QA |
| **screen-reader-testing** | VoiceOver testing protocol | Phase 6 QA |
| **api-design-principles** | Resource-oriented endpoints, consistent error format | §6 Networking |
| **auth-implementation-patterns** | Local PIN, Keychain, biometric | §5 Security |
| **error-handling-patterns** | Result types, retry policy, user-facing errors | §6.3-6.4 |
| **debugging-strategies** | os_log, scientific debugging | §8.4 |
| **code-review-excellence** | Review checklist, Swift antipatterns | §8.3 |
| **workflow-orchestration-patterns** | Long-running inventory sync (future) | Future |
| **kpi-dashboard-design** | KPI hierarchy, chart selection | §4.1 Dashboard |
| **bmad-agent-architect** | System design, trade-off analysis | Entire architecture |
| **bmad-agent-ux-designer** | UX flow, component hierarchy, French UX | §3 + all features |
| **bmad-agent-dev** | Story implementation, code generation | Each sprint |
| **bmad-agent-pm** | PRD creation, roadmap sequencing | §1 Product Strategy |
| **bmad-agent-analyst** | Business rules, KPI definition | Dashboard, POS pricing |
| **bmad-agent-tech-writer** | Documentation, README | All project docs |
| **bmad-code-review** | Adversarial review, edge case hunting | Each PR |
| **bmad-distillator** | Document compression | Onboarding new agents |
| **bmad-party-mode** | Multi-agent discussion | Architecture decisions |
| **bmad-retrospective** | Sprint retrospectives | After each Phase |
| | **— GStack Operational Layer —** | |
| **plan-ceo-review** | CEO-mode: challenge premises, 4 scope modes | Before each Phase planning |
| **plan-eng-review** | Eng-manager: data flow, error map, blast radius | Before each Phase start |
| **plan-design-review** | Design: rate 0-10, generate visual mockups | Phase 2/6 |
| **cso** | Security: 14-phase audit, two modes | Phase 0 comprehensive + daily |
| **review** | Pre-landing: SQL safety, LLM boundaries | Every PR |
| **qa** | Browser test-fix-verify, 3 tiers | After each feature |
| **qa-only** | Report-only QA | Optional bug reporting |
| **benchmark** | Performance: Web Vitals, bundle size | Phase 0 baseline + compare |
| **investigate** | 4-phase bug investigation. Iron Law | Debugging |
| **health** | Composite 0-10 score | Phase 6 release gate |
| **ship** | PR creation, version bump, CHANGELOG | End of each Phase |
| **retro** | Commit patterns, praise/growth | After each Phase |
| **context-save** | Session state persistence | Every session boundary |
| **context-restore** | Cross-workspace handoff | Every session start |
| **autoplan** | CEO+Eng+Design+DX auto-gauntlet | Major milestones |

---

## 11. Quality & Operations Gates

*(Full reference from original plan — see prior version for complete text)*

### 11.1 Gate Summary

| Event | Gate | Action |
|-------|------|--------|
| Phase Planning | `plan-ceo-review` | Choose scope mode |
| Phase Start | `plan-eng-review` | Architecture audit |
| Design Phase | `plan-design-review` | Mockups + 0-10 rating |
| Feature Complete | `qa` (tiered) | Browse test loop |
| Bug Found | `investigate` | Root cause + fix |
| Pre-Merge | `review` | Diff audit |
| Release | `cso` | Security sweep |
| Continuous | `health` | Composite 0-10 |
| Phase End | `ship` | PR + version + CHANGELOG |
| Phase End | `retro` | Commit patterns + praise |
| Session | `context-save/restore` | State persistence |
| Milestone | `autoplan` | Full gauntlet |

### 11.2 QA Tiers

| Tier | Scope | When |
|------|-------|------|
| Quick | Critical/high from git diff | Daily dev |
| Standard | Diff + related modules | End of feature branch |
| Exhaustive | Full regression | Pre-release, Phase 6 |

### 11.3 Security Schedule

- **Comprehensive** (Phase 0, Phase 6, monthly): 14-phase — secrets, supply chain, CI/CD, LLM, skills, OWASP, STRIDE
- **Daily** (every sprint): 8/10 confidence gate, diff-only scope

### 11.4 Health Targets

| Component | Weight | Phase 0 | Phase 6 |
|-----------|--------|---------|---------|
| Typecheck | 22% | 7.0 | 9.0 |
| Lint | 18% | 7.0 | 9.0 |
| Tests | 28% | 5.0 | 8.5 |
| Dead Code | 13% | 6.0 | 8.0 |
| Shell | 9% | 8.0 | 9.0 |
| gbrain | 10% | 5.0 | 8.0 |
| **Composite** | **100%** | **6.3** | **8.6** |

---

## Appendix A: API Reference — All 97 Endpoints

### Products (7 routes)
| Method | Path | Feature | Phase |
|--------|------|---------|-------|
| GET | `/api/products` | List products (filter: warehouse, include_archived) | 1 |
| GET | `/api/products/<id>` | Product detail (stock, movements, tiers, stats) | 1 |
| POST | `/api/products` | Create product | 1 |
| PUT | `/api/products/<id>` | Update product (incl. extra_prices JSON) | 1 |
| DELETE | `/api/products/<id>` | Soft-delete product (cascade stock/invoices) | 1 |
| GET | `/api/products/for-sale` | Sale-able products (>0 qty, not deleted) | 2 |
| GET | `/api/categories` | Distinct product categories | 1 |

### Customers (5 routes)
| Method | Path | Feature | Phase |
|--------|------|---------|-------|
| GET | `/api/customers` | List (search by name/code/email) | 1 |
| POST | `/api/customers` | Create (auto client_code) | 1 |
| GET | `/api/customers/<id>` | Customer detail | 1 |
| PUT | `/api/customers/<id>` | Update | 1 |
| DELETE | `/api/customers/<id>` | Delete (blocked if invoices) | 1 |

### Suppliers (4 routes)
| Method | Path | Feature | Phase |
|--------|------|---------|-------|
| GET | `/api/suppliers` | List all | 3 |
| POST | `/api/suppliers` | Create | 3 |
| PUT | `/api/suppliers/<id>` | Update | 3 |
| DELETE | `/api/suppliers/<id>` | Delete | 3 |

### Warehouses (2+ routes)
| Method | Path | Feature | Phase |
|--------|------|---------|-------|
| GET | `/api/warehouses` | List | 3 |
| POST | `/api/warehouses` | Create | 3 |
| PUT | `/api/warehouses/<id>` | Update | 3 |
| DELETE | `/api/warehouses/<id>` | Delete | 3 |

### Locations (4 routes)
| Method | Path | Feature | Phase |
|--------|------|---------|-------|
| GET | `/api/locations` | List (filter: warehouse_id) | 3 |
| POST | `/api/locations` | Create (type, capacity) | 3 |
| PUT | `/api/locations/<id>` | Update | 3 |
| DELETE | `/api/locations/<id>` | Delete (blocked if stock exists) | 3 |

### Stock & Movements (6 routes)
| Method | Path | Feature | Phase |
|--------|------|---------|-------|
| GET | `/api/movements` | List movements (filter: product, warehouse) | 3 |
| POST | `/api/movements` | Create movement (in/out) | 2 |
| POST | `/api/stock/transfer` | Transfer within warehouse | 2 |
| POST | `/api/stock/inter-warehouse` | Transfer between warehouses | 2 |
| POST | `/api/stock/<product_id>` | Create stock movement for product | 2 |
| GET | `/api/movements/<product_id>` | Movements for specific product | 1 |

### POS (11 routes)
| Method | Path | Feature | Phase |
|--------|------|---------|-------|
| GET | `/api/pos/sessions` | Current open session | 2 |
| POST | `/api/pos/sessions` | Open session (opening_cash) | 2 |
| POST | `/api/pos/sessions/<id>/close` | Close session | 2 |
| POST | `/api/pos/transactions` | Create transaction (sale) | 2 |
| GET | `/api/pos/transactions/recent` | Recent transactions (filter: session, limit) | 2 |
| GET | `/api/pos/transaction-by-invoice/<num>` | Transaction by invoice | 2 |
| GET | `/api/pos/cash-movements` | Cash movements for open session | 2 |
| POST | `/api/pos/cash-movements` | Record cash movement (in/out) | 2 |
| GET | `/api/pos/customers` | Active customers for POS | 2 |
| GET | `/api/pos/best-sellers` | Best-selling products | 2 |
| GET | `/api/pos/tickets/<num>` | Ticket receipt HTML | 2 |

### Orders (5 routes)
| Method | Path | Feature | Phase |
|--------|------|---------|-------|
| GET | `/api/orders` | List (filter: warehouse, status) | 4 |
| POST | `/api/orders` | Create (items[], status=brouillon) | 4 |
| PUT | `/api/orders/<id>` | Update status (recu, paye, annule) | 4 |
| GET | `/api/orders/<id>/items` | Get order line items | 4 |
| DELETE | `/api/orders/<id>` | Permanently delete order | 4 |

### Reorder Rules (4 routes)
| Method | Path | Feature | Phase |
|--------|------|---------|-------|
| GET | `/api/reorder-rules` | List (filter: warehouse) | 4 |
| POST | `/api/reorder-rules` | Create rule | 4 |
| PUT | `/api/reorder-rules/<id>` | Update rule | 4 |
| DELETE | `/api/reorder-rules/<id>` | Delete rule | 4 |

### Replenishment (1 route)
| Method | Path | Feature | Phase |
|--------|------|---------|-------|
| GET | `/api/replenishment` | Replenishment suggestions | 4 |

### Invoices (9 routes)
| Method | Path | Feature | Phase |
|--------|------|---------|-------|
| GET | `/api/invoices` | List (filter: warehouse, status, date) | 4 |
| POST | `/api/invoices` | Create (with discount/tax, stock decrement) | 4 |
| GET | `/api/invoices/<id>` | Detail with items | 4 |
| PUT | `/api/invoices/<id>` | Update (pay, cancel, notes) | 4 |
| DELETE | `/api/invoices/<id>` | Delete (blocked if paid) | 4 |
| GET | `/api/invoices/<id>/items` | Get invoice items | 4 |
| POST | `/api/invoices/<id>/items` | Add item (decrements stock) | 4 |
| DELETE | `/api/invoices/<id>/items/<item_id>` | Remove item (restores stock) | 4 |
| GET | `/api/invoices/<id>/pdf` | Invoice PDF | 4 |

### Invoice Stats (2 routes)
| Method | Path | Feature | Phase |
|--------|------|---------|-------|
| GET | `/api/invoice-stats` | Invoice statistics (count, total, paid) | 4 |
| GET | `/api/receivables` | Receivables list (unpaid by customer) | 4 |

### Notifications (3 routes)
| Method | Path | Feature | Phase |
|--------|------|---------|-------|
| GET | `/api/notifications` | Last 50 + unread count | 4 |
| POST | `/api/notifications/<id>/read` | Mark single read | 4 |
| POST | `/api/notifications/mark-all-read` | Mark all read | 4 |

### KPIs (23 routes)
| Method | Path | Feature | Phase |
|--------|------|---------|-------|
| GET | `/api/kpis` | Main KPIs (products, value, low stock) | 1 |
| GET | `/api/kpis/dashboard` | Dashboard KPIs (trends, DIO, alerts) | 1 |
| GET | `/api/kpis/alertes` | Alerts (low, out, expiring, inactive) | 1 |
| GET | `/api/stats` | Quick stats (total, low, value, warehouses) | 1 |
| GET | `/api/kpis/sales` | Sales KPIs (CA, nb sales, ticket moyen, trend) | 1 |
| GET | `/api/kpis/margins` | Margin analysis by category | 1 |
| GET | `/api/kpis/receivables` | Receivables KPIs (creances, encaissement) | 1 |
| GET | `/api/kpis/invoices-status` | Invoice status breakdown | 1 |
| GET | `/api/kpis/sales-daily` | Daily sales history (CA + count) | 1 |
| GET | `/api/kpis/categories-distribution` | Sales by category | 1 |
| GET | `/api/kpis/top-selling-products` | Top selling products | 1 |
| GET | `/api/kpis/sessions-history` | POS sessions history | 5 |
| GET | `/api/kpis/sessions-summary` | Sessions summary stats | 5 |
| GET | `/api/kpis/sessions/<id>/details` | Session detail (transactions + cash) | 5 |
| GET | `/api/kpis/trends` | Stock movement trends per day | 1 |
| GET | `/api/kpis/top-products` | Top products by movement | 1 |
| GET | `/api/kpis/by-location` | Stock KPIs by location | 1 |
| GET | `/api/kpis/warehouse-overview` | Warehouse overview KPIs | 1 |
| GET | `/api/kpis/orders-summary` | Order summary (counts by status) | 4 |
| GET | `/api/kpis/invoices-summary` | Invoice summary (unpaid/sent/paid) | 4 |
| GET | `/api/kpis/customers-summary` | Customer summary (total, loyal) | 1 |
| GET | `/api/kpis/evolution` | Stock evolution per day | 1 |
| GET | `/api/kpis/payment-methods` | Payment method distribution | 1 |

### Reports (2 routes)
| Method | Path | Feature | Phase |
|--------|------|---------|-------|
| GET | `/api/reports?type=X` | Report data (overview, rotation, expiry, categories, low_stock, warehouses) | 5 |
| GET | `/api/reports/export?type=X` | Report CSV export (products, movements) | 5 |

### Main Account (4 routes)
| Method | Path | Feature | Phase |
|--------|------|---------|-------|
| GET | `/api/main-account` | Account info + transaction history | 5 |
| POST | `/api/main-account/deposit` | Deposit funds | 5 |
| POST | `/api/main-account/withdraw` | Withdraw funds | 5 |
| POST | `/api/main-account/transfer-to-pos` | Transfer to POS | 5 |

### Data Management (2 routes)
| Method | Path | Feature | Phase |
|--------|------|---------|-------|
| POST | `/api/reset-data` | Reset transactional data (debug) | 5 |
| POST | `/api/seed-data` | Seed demo data (debug) | 5 |

---

## Appendix B: Key Business Rules iOS Must Enforce

From `project-context.md` (38 rules) and legacy SPA analysis:

1. **No negative stock**: POS checkout fails server-side if insufficient stock; iOS shows clear error BEFORE confirming
2. **Pricing tiers**: Client displays server-returned price; never computes discount locally (tiers can change server-side)
3. **Movement types**: Only `in`, `out`, `transfer` — all other types rejected; iOS uses a typed enum
4. **French language**: Every user-facing string in French. English strings are a bug.
5. **Invoice requires items**: Empty cart cannot be checked out. iOS disables "Payer" when `cart.isEmpty`
6. **POS session management**: Session must be open before scanning; auto-close on idle > 1 hour
7. **Product uniqueness by SKU**: Barcode lookup returns exactly one result; multiple matches = error
8. **Customer delete blocked**: Cannot delete customer with existing invoices
9. **Location delete blocked**: Cannot delete location with existing stock
10. **Invoice cancel reverses stock**: Cancel = stock restored, payment reversed
11. **Order pay debits main account**: Marking order "paye" transfers funds from main account
12. **Order receive adds stock**: Marking order "recue" creates stock movements
13. **Invoice delete blocked if paid**: Paid invoices cannot be deleted (may be cancelled)
14. **Main account withdraw checks balance**: Withdrawal > balance returns server error
15. **POS session uniqueness**: Only one open session at a time; creating new session fails if one is open
16. **Zero backend changes**: All auth is local (Keychain). No `app.py` modifications.
17. **Data operations are debug-only**: Reset/seed operations require gesture-activated access (long-press)
18. **PIN lockout after 5 failures**: 30-second delay; stored in-app (not server)

---

*Full feature parity with the legacy 15-tab SPA, reimagined in native iOS with proper MVVM architecture, offline caching, and gstack operational gates — without modifying a single line of backend code.*
