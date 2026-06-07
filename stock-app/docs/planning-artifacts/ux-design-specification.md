# UX Design Specification — StockPro Android

**Author:** Omar
**Date:** 2026-06-06
**Version:** 1.0 (retrospective — documents built system)
**Status:** completed

---

## 1. Design System

### 1.1 Brand Identity

| Attribute | Value |
|-----------|-------|
| Company | Bibliotheque Badr — Marrakech, Morocco |
| Primary brand color | Navy Blue #1E3A6F |
| Accent color | Warm Amber #F5A623 |
| Industry | Retail / School supplies |
| UX language | ERP-grade, high-density, desktop-first on tablet |

### 1.2 Color Palette

**Light theme:**

| Token | Color | Usage |
|-------|-------|-------|
| Brand | #1E3A6F | Primary actions, top bars, selected state |
| BrandContainer | #D3E3FF | Container backgrounds |
| Accent | #F5A623 | Highlights, badges, call-to-action |
| Success | #34A853 | Stock OK, payments confirmed |
| Warning | #FBBC04 | Low stock alerts |
| Error | #EA4335 | Errors, critical stockouts, validation |
| Background | #F8F9FA | Screen backgrounds |
| Surface | #FFFFFF | Cards, dialogs |
| SurfaceVariant | #F1F3F5 | Subtle section backgrounds |
| Outline | #DADCE0 | Dividers, borders |

**Dark theme:** Navy darkens to #152B52, backgrounds shift to #121212/#1E1E1E, text to #E6E1E5.

### 1.3 Typography

| Style | Weight | Size | Line Height | Usage |
|-------|--------|------|-------------|-------|
| displayLarge | Bold | 28sp | 34sp | Screen titles, KPI hero numbers |
| headlineLarge | Bold | 22sp | 28sp | Section headers |
| headlineMedium | SemiBold | 18sp | 24sp | Card titles, list headers |
| titleLarge | Bold | 20sp | 26sp | Detail screen titles |
| titleMedium | SemiBold | 16sp | 22sp | Subsection headers, nav labels |
| bodyLarge | Normal | 15sp | 20sp | Primary content text |
| bodyMedium | Normal | 13sp | 18sp | Secondary content, descriptions |
| bodySmall | Normal | 12sp | 16sp | Captions, timestamps |
| labelLarge | Medium | 14sp | 20sp | Button labels |
| labelMedium | Medium | 12sp | 16sp | Chip labels, tabs |
| labelSmall | Medium | 10sp | 14sp | Badge counts, status labels |

System font (default Roboto/sans-serif). No custom font — relies on Android system font.

### 1.4 Component Library (core/ui/components/)

| Component | File | Behavior |
|-----------|------|----------|
| StockButton | StockButton.kt | Primary/secondary/outlined/text variants. Loading spinner state. Full-width option. |
| StockBadge | StockBadge.kt | Status badges (success/warning/error/info). Compact pill shape. |
| StockCard | StockCard.kt | Elevated card with optional header, content, and action footer. |
| StockErrorView | StockErrorView.kt | Full-screen error with icon, message, retry button. |
| StockFormRow | StockFormRow.kt | Label + input pair with error state for forms. |
| StockSkeleton | StockSkeleton.kt | Shimmer loading placeholder matching card/list shapes. |
| StockTag | StockTag.kt | Small colored tag for categories, types, filters. |
| StockTextField | StockTextField.kt | Outlined input with leading icon, error, counter options. |

---

## 2. Navigation Architecture

### 2.1 Bottom Navigation (Primary)

5-tab bottom navigation bar:

| Tab | Icon | Route | Screens |
|-----|------|-------|---------|
| Dashboard | Home | `dashboard` | Dashboard |
| Products | Package | `products` | Products list → Product detail |
| POS | ShoppingCart | `pos` | POS register, session |
| Warehouse | Building2 | `warehouses` | Warehouses → Locations → Movements |
| More | MoreHorizontal | `more` | Customers, Suppliers, Orders, Invoices, Notifications, Settings |

### 2.2 Route Map

```
BottomNav tabs:
  dashboard
  products
  pos
  warehouses
  more (sub-menu)

Detail routes (navigate from any list):
  products/{productId}
  customers/{customerId}
  suppliers/{supplierId}
  orders/{orderId}
  invoices/{invoiceId}
  warehouses/{warehouseId}/{warehouseName}
  movements
  notifications
  pin-setup
  pin-change
  settings
```

---

## 3. Screen Specifications

### 3.1 Dashboard

**Purpose:** At-a-glance business overview for daily operations.

**Layout (top to bottom):**
1. Top bar: "Tableau de Bord" title, notification bell icon
2. KPI row (horizontal scroll): 6 compact cards — CA today, sales count, low stock count, active alerts, pending orders, today's profit
3. Charts section:
   - Line chart: Daily sales trend (7/30 days)
   - Donut chart: Sales by category
   - Bar chart: Top selling products
4. Quick action buttons: New sale, New product, New order
5. Pull-to-refresh activates skeleton loading

**States:** Loading (skeleton grid), loaded, error (retry), empty (first-run prompt).

### 3.2 Products

**List screen:**
- Top bar: "Produits" with + FAB
- Search bar with debounced text input
- Filter chips: All / Active / Archived
- List items: Product photo thumb, name, SKU, stock qty, price, tier badge
- Swipe left to delete with confirmation dialog
- Tap to navigate to detail

**Detail screen:**
- Header: Image, name, SKU, category, status badge
- Pricing tiers section: Normal / Loyal (-15%) / Student (-15%) / School (-20%)
- Stock section: Current quantity, warehouse location, last movement
- Movement history: Chronological list of stock movements
- Edit button (top right) → ProductForm

**Form screen:**
- Fields: Name, SKU, Category (dropdown), Barcode, 4 price tier inputs, Min stock, Image URL
- Save / Cancel
- Validation: required fields, numeric prices
- Create mode vs Edit mode

### 3.3 POS / Caisse

**Purpose:** Complete point-of-sale for fast retail checkout.

**Layout:**
1. Session bar (top): Session status, cash drawer count, open/close controls
2. Search bar: Product search + barcode scan (auto-focus on tab open)
3. Cart panel (left/center): Line items with qty adjust, price tier indicator, remove
4. Customer selector: Optional assignment, applies tier discounts automatically
5. Payment section:
   - Cash: Enter amount received, display change due
   - Card: Mark as paid
   - Mixed: Split payment cash + card
   - Customer credit: Charge to customer account
6. Summary: Subtotal, discount, tax, total
7. Receipt generation on completion

**Scanner integration:**
- CameraX + ML Kit barcode scanner
- USB scanner support (auto-submit after scan)
- 6x optimized detection (150ms interval, 5 formats)
- Multi-article scan with dynamic bounding box
- Manual entry fallback
- Audio beep on successful scan

**Pricing tier logic:**
- Default: Normal price
- When customer selected: auto-apply loyalty/student/school discount
- Discount displayed per line item

### 3.4 Customers

**List screen:**
- Top bar: "Clients" with + FAB
- Search bar
- List: Name, type badge (School/Student/Company/Individual), total spent, loyalty status
- Tap → detail

**Detail screen:**
- Header: Name, phone, email, type badge, loyalty badge
- Stats: Total purchases, last purchase date, total spent
- Invoice history: Recent invoices with status
- Edit / Delete

**Form screen:**
- Fields: Name, Phone, Email, Type (dropdown)
- Loyalty badge assignment (auto-calculated from purchase history)

### 3.5 Suppliers

Same CRUD pattern as Customers. Fields: Name, Contact person, Phone, Email, Address.

### 3.6 Warehouses & Locations

**Warehouse list:**
- List with name, location count, total stock value
- + FAB to create
- Tap → Locations for that warehouse

**Location form:**
- Fields: Name, Warehouse (pre-selected if coming from warehouse), Description

### 3.7 Stock Movements

**Movement list:**
- Filterable list by type (In/Out/Transfer), warehouse, product, date range
- Each item: Product name, type icon, quantity, source/destination, timestamp

**Create movement:**
- Type selector: Incoming / Outgoing / Transfer
- Product selector (searchable)
- Quantity input
- Source warehouse/location (outgoing/transfer)
- Destination warehouse/location (incoming/transfer)

### 3.8 Auth (PIN)

**PinLockScreen:**
- 6-digit PIN input with masked dots
- Custom numeric keypad
- SHA-256 hash verification
- 5 attempts max → 30s lockout

**PinSetup:**
- First-time setup on app launch if no PIN exists
- Enter + confirm PIN

**PinChange:**
- Old PIN → New PIN → Confirm

---

## 4. User Journeys

### 4.1 Daily Opening — POS

1. Open app → Dashboard shows daily KPIs
2. Check low stock alerts on dashboard
3. Tap "New Sale" quick action → POS opens
4. Open session (if not already open) → cash count
5. Search product or scan barcode → item added to cart
6. Select customer (optional) → tier discount auto-applied
7. Complete payment: cash/card/mixed → change calculated
8. Receipt printed/shown
9. Repeat 5-8 for each customer
10. End session → cash count, close drawer

### 4.2 Stock Management

1. Tap More → Warehouses
2. View warehouse list → tap one → view locations
3. Tap Movements → filter by type Outgoing
4. Review low stock items
5. Create new purchase order from supplier
6. When stock arrives: Create movement → Incoming → select product, qty, location

### 4.3 Product Management

1. Tap Products tab → search or browse
2. Tap product → see detail with pricing tiers, stock, movements
3. Edit pricing if needed → save
4. Archive discontinued products via swipe

---

## 5. Layout Guidelines

| Principle | Rule |
|-----------|------|
| Density | High-density data first. Minimize whitespace. |
| Scrolling | Vertical scroll. Horizontal scroll only for KPI row. |
| Sticky headers | Table headers, section headers stay fixed on scroll. |
| Navigation | Bottom nav primary, modal sheets for forms, push navigation for details. |
| Empty states | Every list shows illustration + message + action on empty. |
| Loading | Skeleton shimmer (not spinners) for content loading. |
| Error | StockErrorView with retry for API failures. Snackbar for ephemeral errors. |
| Dark mode | Full dark theme support, follows system setting. |

---

## 6. Accessibility

- Minimum touch target 48dp
- All icons paired with text labels in bottom nav
- Color not sole indicator (icons + text + patterns)
- Content descriptions on all interactive elements (Compose built-in)
- Scrim on dialogs and modal sheets
- Pull-to-refresh on all list screens

---

## 7. Remaining Screens (Next Epics)

| Feature | Screens Needed | Status |
|---------|---------------|--------|
| Orders | List, Detail, Form | Code done, needs UX review |
| Invoices | List, Detail, Form | Code done, needs UX review |
| Notifications | List | Code done, needs UX review |
| Reordering | Rules list, Suggestions | Not started |
| Settings | About, Change PIN, Debug | Code done, needs UX review |

---

## 8. Design Decisions Log

| Decision | Rationale |
|----------|-----------|
| Material 3 over custom | Faster development, consistent across Android, built-in dark mode |
| 5-tab bottom nav | Covers the 4 primary workflows + overflow. Keeps navigation flat. |
| No Shape.kt | Default Material 3 shapes used. Extract if custom shapes needed for cards/badges. |
| Skeleton over spinner | Perceived performance: skeleton shows structure, spinner shows waiting. |
| Dense list rows | ERP data density: show more items per screen without scrolling. |
