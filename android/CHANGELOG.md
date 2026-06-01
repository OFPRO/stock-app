# Changelog

## [1.1.0] - 2026-06-02

### Added
- Dashboard KPIs with revenue, profit, margin charts
- POS module with session management, barcode scanning, checkout
- Products CRUD with stock status badges
- Customers CRUD with detail view
- Suppliers CRUD
- Warehouses & locations CRUD with movement tracking
- Stock movements (entry, exit, transfer, inter-warehouse)
- PIN lock screen with lockout mechanism
- Notifications with mark-read
- Reports module
- Dark/light mode support

### Fixed
- CRITICAL: POS customer_id not passed to pricing API (tiered pricing broken)
- CRITICAL: Delete customer navigates before API responds
- CRITICAL: POS payment dialog allows double-submit
- CRITICAL: BarcodeScanner flash toggle not wired (disabled)
- HIGH: Dashboard formatPercent multiplies by 100 (already in %)
- HIGH: PIN manager SHA-256 without salt, two edit() calls merged
- HIGH: Network logging set to HEADERS (was BODY in production)
- HIGH: StockButton Primary/Danger text invisible (contrast fix)
- HIGH: StockSkeleton dark mode broken (hardcoded Color.LightGray)
- HIGH: Small touch targets in POS cart (24dp → 44dp)
- MEDIUM: POSSession openSession/closeSession silent returns
- MEDIUM: Products delete error not shown to user
- MEDIUM: CreateMovement missing location_id/source-destination validation

### Security
- Network logging reduced to HEADERS level
- PIN attempts now use single SharedPreferences edit()
- Added PBKDF2 recommendation comments in PinManager

### Build
- Gradle configuration cache enabled
- Room FK indices added
- All deprecated API usages cleaned (menuAnchor, TrendingUp, statusBarColor)
- lint + test passing with zero errors
