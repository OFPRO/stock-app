# Backend API Inventory — StockPro Flask

**Generated:** 2026-06-06
**Purpose:** Map Flask API endpoints to Android feature needs for remaining epics.

---

## Legend

| Status | Meaning |
|--------|---------|
| ✅ Ready | Endpoint exists, Android screen built, API connected |
| ⚠️ Code exists | Endpoint exists AND Android screen exists but API integration may be partial |
| ❌ Missing | Endpoint or Android screen not built |

---

## Orders (Epic 9)

| Endpoint | Method | Status | Android Feature |
|----------|--------|--------|----------------|
| `/api/orders` | GET | ✅ Ready | OrdersScreen — list all orders |
| `/api/orders` | POST | ✅ Ready | OrderFormScreen — create order |
| `/api/orders/<id>` | PUT | ✅ Ready | OrderFormScreen — edit order |
| `/api/orders/<id>` | DELETE | ✅ Ready | OrderDetailScreen — delete |
| `/api/orders/<id>/items` | GET | ⚠️ | OrderDetailScreen — line items |

## Invoices (Epic 10)

| Endpoint | Method | Status | Android Feature |
|----------|--------|--------|----------------|
| `/api/invoices` | GET | ✅ Ready | InvoicesScreen — list |
| `/api/invoices` | POST | ✅ Ready | InvoiceFormScreen — create |
| `/api/invoices/<id>` | GET | ✅ Ready | InvoiceDetailScreen |
| `/api/invoices/<id>` | PUT | ✅ Ready | InvoiceFormScreen — edit |
| `/api/invoices/<id>` | DELETE | ✅ Ready | Detail screen |
| `/api/invoices/<id>/items` | GET | ⚠️ | InvoiceDetailScreen — line items |
| `/api/invoices/<id>/items` | POST | ⚠️ | — |
| `/api/invoices/<id>/items/<item_id>` | DELETE | ⚠️ | — |
| `/api/invoices/<id>/pay-credit` | POST | ⚠️ | — |
| `/api/invoices/<id>/pdf` | GET | ⚠️ | PDF download |
| `/api/invoice-stats` | GET | ⚠️ | Dashboard/stats |

## Notifications (Epic 11)

| Endpoint | Method | Status | Android Feature |
|----------|--------|--------|----------------|
| `/api/notifications` | GET | ✅ | NotificationsScreen — list |
| `/api/notifications/<id>/read` | POST | ✅ | Mark as read |
| `/api/notifications/mark-all-read` | POST | ✅ | Mark all read |

## Reordering Rules (Epic 12)

| Endpoint | Method | Status | Android Feature |
|----------|--------|--------|----------------|
| `/api/reorder-rules` | GET | ✅ | Not started |
| `/api/reorder-rules` | POST | ✅ | Not started |
| `/api/reorder-rules/<id>` | PUT | ✅ | Not started |
| `/api/reorder-rules/<id>` | DELETE | ✅ | Not started |

---

## Summary

| Epic | Backend | Android Screen | API Bindings |
|------|---------|---------------|--------------|
| 9 — Orders | ✅ All endpoints | ⚠️ Built | Needs API client wiring |
| 10 — Invoices | ✅ All endpoints | ⚠️ Built | Needs API client wiring |
| 11 — Notifications | ✅ All endpoints | ⚠️ Built | Needs API client wiring |
| 12 — Reordering | ✅ All endpoints | ❌ Not built | — |
| 14 — Settings | N/A (local) | ⚠️ Built | No API needed |

The backend covers all needed endpoints. The gap is in the Android API client layer:
- Verify `ApiService.kt` has Retrofit methods for all order/invoice/notification endpoints
- Verify repository implementations exist and connect to the API
- Epic 12 (Reordering) needs both Android screens and API client methods
