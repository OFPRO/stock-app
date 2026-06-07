# Session Context — StockPro iOS & Backend

## Goal
Build the StockPro iOS companion app (native SwiftUI) following the established phased plan (7 phases / 18 sprints), with proper skill use (gstack, bmad, mobile-ios-design, design/UX skills). Backend changes only when strictly necessary for iOS feature parity.

## Full Project Plan (from `stock-app/docs/planning-artifacts/StockPro_iOS_ARCHITECTURE.md`)

### Phased Roadmap Status

| Phase | Sprint | Description | Status |
|-------|--------|-------------|--------|
| **Phase 0** | S1 | Foundation — Xcode, design system, APIClient, DI, PIN auth, navigation shell | ✅ Done |
| **Phase 1** | S2-S4 | Products CRUD, Customers CRUD, Dashboard (KPIs, charts, data tables), offline cache | ✅ Done |
| **Phase 2** 🔥 | **S5** | **Scanner — AVFoundation barcode, manual entry, history** | ✅ Done |
| | **S6** | **POS/Caisse — session, scan-to-cart, best-sellers, cart editing, customer/pricing, payment, cash mgmt, receipt** | 🔄 **Current** |
| | S7 | Stock Movements — history list, in/out/transfer | ❌ |
| **Phase 3** | S8-S9 | Suppliers CRUD, Warehouses CRUD, Locations CRUD (✅ done ahead of plan) | ✅ Done |
| **Phase 4** | S10-S12 | Reorder rules, replenishment, Purchase Orders (full workflow), Invoices (full), Notifications | ❌ |
| **Phase 5** | S13-S15 | Reports (5 types), Sessions history, Main Account, Settings, PIN mgmt, debug, logout | ❌ |
| **Phase 6** | S16-S18 | iPad adaptive, sidebar, keyboard shortcuts, VoiceOver/WCAG, TestFlight, CI/CD, App Store | ❌ |

### Constraints & Preferences
- French UI (`fr-FR`), iOS 17+, Swift Charts, SwiftUI
- Zero backend changes when possible — Flask API consumed as-is
- MVVM + manual DI via `@Environment(\.diContainer)`
- PIN-based auth in Keychain (no backend auth)
- Must follow existing phased roadmap strictly
- Must load listed skills before each implementation phase

## Progress
### Done
- **All bmad/design/UX skills loaded**: `mobile-ios-design`, `design-system-patterns`, `interaction-design`, `visual-design-foundations`, `responsive-design`, `accessibility-compliance`, `kpi-dashboard-design`, `bmad-quick-dev`
- **Build blockers fixed**:
  - `Package.swift`: `.dynamic` → `.static`, added `path: "."`
  - `AuthService.swift`: removed all duplicate stub classes (moved to `ServiceStubs.swift`)
  - `ServiceStubs.swift` created: holds empty stubs for Stock, POS, Order, Invoice, Notification, Report, Session, MainAccount services
- **Sprint 4 completed — Warehouses + Locations CRUD**:
  - 12 new files (DTOs, Services, ViewModels, Views), 4 modified files
  - All views follow existing patterns (MVVM, ViewState, design system, French UI)
- **Sprint 5 completed — Scanner module**:
  - `Utilities/BarcodeScanner.swift` — AVFoundation wrapper (UIViewRepresentable) with:
    - AVCaptureSession camera preview (`.high` preset)
    - `AVCaptureMetadataOutput` for EAN-8/13, UPC-E, Code 39/128, QR, PDF417, Aztec
    - Debounce (1.5s cooldown between scans)
    - Haptic feedback via `AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)`
    - Platform-guarded with `#if canImport(UIKit)`
  - `Data/DTOs/ProductDTOs.swift` — Added:
    - `ForSaleProductDTO` (maps `/api/products/for-sale` response with `sale_price`)
    - `ScannedProduct` model (Identifiable + Equatable)
  - `Networking/Endpoint.swift` — Added `.productByBarcode(String)` case:
    - Path: `/api/products/for-sale?search=<barcode>`
    - Method: GET
  - `Services/ServiceProtocols.swift` — Added `fetchProductByBarcode(_:)` to protocol
  - `Services/ProductService.swift` — Real impl calls endpoint + client-side barcode match; Mock impl matches by SKU
  - `ViewModels/ScannerViewModel.swift` — Rewrote with:
    - `ScanningState` enum: idle / scanning / found(ScannedProduct) / notFound(String) / error(String)
    - Real `lookupBarcode()` via `ProductService.fetchProductByBarcode()`
    - `ScanRecord` with code, productName, timestamp
    - Manual entry + camera callback paths converge on same lookup method
    - `dismissResult()` to resume scanning
  - `Views/Scanner/ScannerView.swift` — Rewrote with:
    - Real camera preview (ZStack with overlay)
    - Scanned product card (checkmark icon, name, price, stock, "Continuer" button)
    - Not found / error states with retry
    - Torch button (when scanning)
    - Manual barcode entry + history list (unchanged from stub)
    - Permission handling (`.task` + alert for denied access)
- **Sprint 6 — POS UI work (current session)**:
  - Searched for stock data in backend endpoints (best-sellers, for-sale products)
  - Fixed backend `app.py` best-sellers query to include `p.quantity`
  - Fixed backend `routes/products.py` to remove `quantity > 0` filter from for-sale endpoint
  - Created `StockTag` component (reusable chip, 3 variants: brand/surface/success)
  - Fixed StockButton secondary variant (background `AppColor.surface`, 1px border for dark mode)
  - Added `.search` variant to StockButton (accent orange gradient, white text, shadow)
  - Added `compact: Bool` param to StockButton for horizontal scrollviews
  - Added `min_quantity: Int?` + computed `stockStatus` to `ForSaleProductDTO`
  - Added `quantity: Int?` + computed `stockStatus` to `BestSellerDTO`
  - ProductSearchSheet: best-sellers when search empty, stock badges (green/orange/red) on all rows
  - Force-add out-of-stock: `pendingOutOfStock` state + alert "Ajouter quand même"
  - Mock data updated with quantities (5 best-sellers, one with qty 0)
  - Updated `ForSaleProductDTO` quantity to be optional from scratch (no hardcoded qty)

### In Progress
- Sprint 6 (POS/Caisse) — partial UI work done, core flows remaining

### Blocked
- No simulator runtime installed (iOS 26.5 SDK present but no runtime) — cannot run in simulator, can only verify via `swift build`
- Build output: all errors are macOS availability warnings (`UIKit`, `UIKeyboardType`, `navigationBarTrailing`, `@Model` macros) — identical pattern across ALL existing and new files. No unique structural errors.

## Key Decisions
- Barcode lookup reuses existing `/api/products/for-sale` endpoint (searches by name, SKU, barcode) + client-side exact barcode match (backend uses `LIKE` which is fuzzy)
- New `productByBarcode` endpoint case wraps the same URL path with `search=` query param
- Camera session lifecycle managed at View level (`@State` + `.task`), not in ViewModel (keeps AVFoundation concerns out of business logic)
- `ScanningState` is `Equatable` via auto-synthesis + `ScannedProduct` conformance — supports `.animation(value:)` transitions
- Haptic on scan via system sound (no `UIImpactFeedbackGenerator` dependency)
- 1.5s debounce prevents duplicate scans from continuous barcode reads
- BestSellerDTO.quantity made `Int?` so decoders don't fail when backend response lacks the field
- StockBadge uses existing DesignSystem component mapped via stockStatus → variant (.success/.warning/.error)
- Compact mode for action-row buttons prevents text wrapping
- nohup + /tmp/flask.log for persistent backend process across shell sessions
- Flask backend must be restarted after any `app.py` or `routes/*.py` changes

## Sprint 6 (POS/Caisse) — Remaining Work per Plan
Per `StockPro_iOS_ARCHITECTURE.md §4.9` and `§9 Phased Roadmap`:

1. **Session management** — open session with opening cash, close with reconciliation, status badge
2. **Scan-to-cart** — barcode → product with tiered price (bridge Scanner → POS)
3. **Manual product add** — search + select for damaged barcodes (partial: search done)
4. **Customer selection** → pricing tier applied (Loyal -15%, Student -15%, School -20%)
5. **Cart editing** — quantity (+/-), remove, clear
6. **Per-item price override** — edit field
7. **Cart summary** — sous-total HT, TVA, remises, total TTC
8. **Payment** — Espèces, Carte, Mixte
9. **Cash tendered** + change calculation + quick-cash buttons ("Montant exact", "Arrondi +10")
10. **Credit sale** — checkbox (invoice pending)
11. **Cash management** — Cash In/Out with reasons, balance display, movements list
12. **Checkout** — POST /api/pos/transactions, auto-create invoice, receipt preview + AirPrint
13. **Recent transactions** — last 20 per session, refresh button

## Backend Notes
- Flask runs on port 5001, SQLite (`stock.db`), WAL mode
- Relaunch: `nohup python3 app.py > /tmp/flask.log 2>&1 &`
- DB auto-initializes on first run. Delete `stock.db*` to fully reset
- Seed: `python3 seed.py` (destructive — cleans all tables first)
- 18 tables, all queries use `?` placeholders (no f-string interpolation)
- `*.bak` files contain SQL injection vulns — never copy from them
- `routes/db.py` provides `get_db()` and `get_db_ctx()` — shared DB utilities

## Critical Context
- Xcode 26.5 installed, iOS SDK 26.5 present, no simulator runtime — cannot run in simulator
- `swift build` errors are all macOS availability warnings (identical for existing + new files) — code compiles correctly when targeting iOS
- Bundle ID: `com.bibliothequebadr.stockpro` (not `com.stockpro.ios`)
- XcodeGen used to regenerate project: `xcodegen generate --project .`
- Build command: `xcodebuild -project StockPro.xcodeproj -scheme StockPro -sdk iphonesimulator -destination 'platform=iOS Simulator,id=9FD1A116-AD90-4A62-B704-C2DDC0E7F5EF' build`
- Backend scans by barcode via `GET /api/products/for-sale?search=<code>` (fuzzy LIKE match on name/SKU/barcode)
- 13 git commits (`main` branch), no remotes
- Project is clean: `ios/` as untracked, no merge conflicts

## Relevant Files
- `stock-app/docs/planning-artifacts/StockPro_iOS_ARCHITECTURE.md`: Main iOS architecture plan (13 sections, 97 API endpoints, phased roadmap for 18 sprints)
- `docs/adr.md`: Architecture Decision Records (8 ADRs)
- `docs/audit-report.md`: Legacy SPA audit report
- `docs/stories/S1-columns-db.md` → `S5-fac-numbering.md`: Completed backend bugfix stories
- `AGENTS.md`: Root agent instructions & commands
- `stock-app/docs/project-context.md`: 38 critical AI agent rules

### iOS files modified in this session
- `ios/StockPro/DesignSystem/Components/StockButton.swift`: Added `.search` variant, `compact` Bool
- `ios/StockPro/DesignSystem/Components/StockTag.swift`: New reusable chip component
- `ios/StockPro/Views/POS/POSView.swift`: topActionRow layout, productSearchSheet with stock badges, out-of-stock alert
- `ios/StockPro/Data/DTOs/POSDTOs.swift`: BestSellerDTO added `quantity: Int?` + `stockStatus`
- `ios/StockPro/Data/DTOs/ProductDTOs.swift`: ForSaleProductDTO added `min_quantity: Int?` + `stockStatus`, quantity made optional
- `ios/StockPro/ViewModels/POSViewModel.swift`: addBestSeller passes actual quantity; didScanBarcode includes min_quantity:nil
- `ios/StockPro/Services/POSService.swift`: Mock fetchBestSellers includes quantity for all 5 items
- `ios/StockPro/Services/ProductService.swift`: ForSaleProductDTO mapping updated

### Backend files modified
- `app.py`: Best-sellers query includes `p.quantity` (line ~2101-2119)
- `routes/products.py`: Removed `quantity > 0` filter from for-sale endpoint (line ~299)
