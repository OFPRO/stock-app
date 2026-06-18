const BASE = "/api"

async function fetchJson<T>(url: string): Promise<T> {
  const res = await fetch(BASE + url)
  if (!res.ok) {
    throw new Error(`API error ${res.status}: ${await res.text()}`)
  }
  return res.json()
}

export interface KpiSales {
  ca_jour: number
  nb_ventes_jour: number
  ticket_moyen: number
  ca_periode: number
  nb_ventes_periode: number
  ca_trend: number
  date_filter: { start: string | null; end: string | null; period: number }
}

export interface KpiMargins {
  marge_globale: number
  categories: { category: string; vente: number; achat: number; marge_pct: number }[]
}

export interface KpiReceivables {
  total_creances: number
  nb_impayees: number
  taux_encaissement: number
  clients: { id: number; name: string; client_code: string; montant: number; nb_factures: number; premiere_echeance: string }[]
}

export interface KpiInvoicesStatus {
  brouillon: number
  envoyee: number
  payee: number
  annulee: number
}

export interface KpiDashboard {
  total_products: number
  total_value: number
  avg_price: number
  low_stock: number
  out_of_stock: number
  products_to_order: { id: number; name: string; sku: string; quantity: number; min_quantity: number; needed: number }[]
  expiring_soon: number
  total_alerts: number
  in_movements: number
  out_movements: number
  today_movements: number
  rotation_rate: number
  dio: number
  in_trend: number
  out_trend: number
  period: number
}

export interface SalesDaily {
  date: string
  ca: number
  nb_ventes: number
}

export interface CategoryDistribution {
  category: string
  qty_vendue: number
  ca: number
}

export interface TopProduct {
  id: number
  name: string
  sku: string
  category: string
  qty_vendue: number
  ca: number
}

export interface KpiTrend {
  date: string
  entries: number
  exits: number
  net: number
}

export interface AlerteItem {
  id: number
  name: string
  sku: string
  quantity: number
  min_quantity: number
  price: number
  needed?: number
  lot_number?: string
  expiry_date?: string
  days_left?: number | null
}

export interface KpiAlertes {
  low_stock: AlerteItem[]
  out_of_stock: AlerteItem[]
  expiring: AlerteItem[]
  inactive: AlerteItem[]
}

export interface Warehouse {
  id: number
  name: string
  address: string
  manager: string
  is_default: number
}

export interface MainAccount {
  account: { id: number; current_balance: number }
  transactions: { id: number; type: string; amount: number; reason: string; created_at: string }[]
}

export function getSales(params: string): Promise<KpiSales> {
  return fetchJson(`/kpis/sales?${params}`)
}

export function getMargins(): Promise<KpiMargins> {
  return fetchJson("/kpis/margins")
}

export function getReceivables(): Promise<KpiReceivables> {
  return fetchJson("/kpis/receivables")
}

export function getInvoicesStatus(): Promise<KpiInvoicesStatus> {
  return fetchJson("/kpis/invoices-status")
}

export function getDashboard(params: string): Promise<KpiDashboard> {
  return fetchJson(`/kpis/dashboard?${params}`)
}

export function getSalesDaily(params: string): Promise<SalesDaily[]> {
  return fetchJson(`/kpis/sales-daily?${params}`)
}

export function getCategoriesDistribution(params: string): Promise<CategoryDistribution[]> {
  return fetchJson(`/kpis/categories-distribution?${params}`)
}

export function getTopSellingProducts(params: string): Promise<TopProduct[]> {
  return fetchJson(`/kpis/top-selling-products?${params}`)
}

export function getTrends(params: string): Promise<KpiTrend[]> {
  return fetchJson(`/kpis/trends?${params}`)
}

export function getAlertes(params: string): Promise<KpiAlertes> {
  return fetchJson(`/kpis/alertes?${params}`)
}

export function getMainAccount(): Promise<MainAccount> {
  return fetchJson("/main-account?limit=1")
}

export function getWarehouses(): Promise<Warehouse[]> {
  return fetchJson("/warehouses")
}

export function createWarehouse(data: { name: string; address?: string; manager?: string }): Promise<{ success: boolean; error?: string }> {
  return fetch(BASE + "/warehouses", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  }).then((r) => r.json())
}

export function updateWarehouse(id: number, data: { name?: string; address?: string; manager?: string }): Promise<{ success: boolean; error?: string }> {
  return fetch(BASE + "/warehouses/" + id, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  }).then((r) => r.json())
}

export function deleteWarehouse(id: number): Promise<{ success: boolean; error?: string }> {
  return fetch(BASE + "/warehouses/" + id, { method: "DELETE" }).then((r) => r.json())
}

export interface DashboardData {
  sales: KpiSales
  margins: KpiMargins
  receivables: KpiReceivables
  invoicesStatus: KpiInvoicesStatus
  dashboard: KpiDashboard
  salesDaily: SalesDaily[]
  categoriesDistribution: CategoryDistribution[]
  topSelling: TopProduct[]
  trends: KpiTrend[]
  alertes: KpiAlertes
  warehouses: Warehouse[]
  mainAccount: MainAccount
}

export async function fetchDashboardData(period: number, warehouseId: string): Promise<DashboardData> {
  const whParam = warehouseId && warehouseId !== "__all__" ? `&warehouse_id=${warehouseId}` : ""
  const params = `period=${period}${whParam}`
  const topParams = `period=${period}&limit=10`

  const [
    sales,
    margins,
    receivables,
    invoicesStatus,
    dashboard,
    salesDaily,
    categoriesDistribution,
    topSelling,
    trends,
    alertes,
    warehouses,
  ] = await Promise.all([
    getSales(params),
    getMargins(),
    getReceivables(),
    getInvoicesStatus(),
    getDashboard(params),
    getSalesDaily(params),
    getCategoriesDistribution(`period=${period}`),
    getTopSellingProducts(topParams),
    getTrends(params),
    getAlertes(whParam ? whParam.slice(1) : ""),
    getWarehouses(),
  ])

  const mainAccount = await getMainAccount()

  return {
    sales,
    margins,
    receivables,
    invoicesStatus,
    dashboard,
    salesDaily,
    categoriesDistribution,
    topSelling,
    trends,
    alertes,
    warehouses,
    mainAccount,
  }
}

export interface Product {
  id: number
  name: string
  name_ar?: string
  description: string
  sku: string
  barcode: string
  quantity: number
  min_quantity: number
  max_quantity: number
  price: number
  price_base: number
  price_loyal: number
  price_gros: number
  purchase_price_avg?: number
  margin_percent?: number
  discount_rate?: number
  discount_category?: string
  tax_category: string
  category: string
  category_ar?: string
  lot_number: string
  serial_number: string
  expiry_date: string | null
  warehouse_id: number
  location_id: number | null
  supplier_id: number | null
  supplier_name: string | null
  supplier_email?: string
  supplier_phone?: string
  warehouse_name: string | null
  location_name: string | null
  image_url?: string
  is_deleted: number
  created_at: string
  updated_at: string
}

export interface ProductDetail {
  product: Product
  purchase_stats: { total_qty: number; total_purchases: number }
  sales_stats: { total_qty: number; total_sales: number }
  stock_locations: { location_name: string; quantity: number }[]
  movements: { source: string; type: string; quantity: number; created_at: string; note: string }[]
}

export interface ProductFormData {
  name: string
  description?: string
  sku?: string
  barcode?: string
  quantity?: number
  min_quantity?: number
  max_quantity?: number
  price?: number
  price_base?: number
  price_loyal?: number
  price_gros?: number
  category?: string
  tax_category?: string
  warehouse_id?: number
  location_id?: number | null
  supplier_id?: number | null
  image_url?: string | null
}

export function getProducts(params?: string): Promise<Product[]> {
  return fetchJson(`/products${params ? `?${params}` : ""}`)
}

export function getProduct(id: number): Promise<ProductDetail> {
  return fetchJson(`/products/${id}`)
}

export function createProduct(data: ProductFormData): Promise<{ success: boolean; error?: string }> {
  return fetch(BASE + "/products", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  }).then((r) => r.json())
}

export function updateProduct(id: number, data: Partial<ProductFormData>): Promise<{ success: boolean; error?: string }> {
  return fetch(BASE + "/products/" + id, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  }).then((r) => r.json())
}

export function deleteProduct(id: number): Promise<{ success: boolean; message?: string; details?: string[]; error?: string }> {
  return fetch(BASE + "/products/" + id, { method: "DELETE" }).then((r) => r.json())
}

export interface Category {
  id: number
  name_ar: string
  name_fr: string
}

export function getCategories(): Promise<Category[]> {
  return fetchJson("/categories")
}

export function getProductsForSale(params?: string): Promise<unknown[]> {
  return fetchJson(`/products/for-sale${params ? `?${params}` : ""}`)
}

export function getProductByBarcode(barcode: string): Promise<unknown | null> {
  return getProductsForSale(`search=${encodeURIComponent(barcode)}`).then((products) => {
    const arr = products as unknown as { barcode?: string }[]
    return arr.find((p) => p.barcode === barcode) ?? null
  })
}

export interface StockMovement {
  id: number
  product_id: number
  product_name: string
  type: string
  quantity: number
  source_location_id: number | null
  dest_location_id: number | null
  source_location: string | null
  dest_location: string | null
  lot_number: string
  serial_number: string
  note: string
  created_at: string
}

export interface MovementFormData {
  product_id: number
  type: "in" | "out"
  quantity: number
  note?: string
  location_id?: number | null
  lot_number?: string
  serial_number?: string
}

export function getMovements(params?: string): Promise<StockMovement[]> {
  return fetchJson(`/movements${params ? `?${params}` : ""}`)
}

export function createMovement(productId: number, data: MovementFormData): Promise<{ success: boolean; error?: string }> {
  return fetch(BASE + `/stock/${productId}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  }).then((r) => r.json())
}

export interface Supplier {
  id: number
  name: string
  email: string
  phone: string
  address: string
  contact_person: string
  created_at: string
}

export interface SupplierFormData {
  name: string
  email?: string
  phone?: string
  address?: string
  contact_person?: string
}

export function getSuppliers(): Promise<Supplier[]> {
  return fetchJson("/suppliers")
}

export function createSupplier(data: SupplierFormData): Promise<{ success: boolean; error?: string }> {
  return fetch(BASE + "/suppliers", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  }).then((r) => r.json())
}

export function updateSupplier(id: number, data: SupplierFormData): Promise<{ success: boolean; error?: string }> {
  return fetch(BASE + "/suppliers/" + id, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  }).then((r) => r.json())
}

export function deleteSupplier(id: number): Promise<{ success: boolean; error?: string }> {
  return fetch(BASE + "/suppliers/" + id, { method: "DELETE" }).then((r) => r.json())
}

export interface Customer {
  id: number
  name: string
  type: string
  email: string
  phone: string
  address: string
  client_code: string
  discount_rate: number
  is_loyal: number
  is_active: number
  ice: string | null
  notes: string
  created_at: string
}

export interface CustomerFormData {
  name: string
  type?: string
  email?: string
  phone?: string
  address?: string
  discount_rate?: number
  is_loyal?: number
  notes?: string
}

export function getCustomers(search?: string): Promise<Customer[]> {
  return fetchJson(`/customers${search ? `?search=${encodeURIComponent(search)}` : ""}`)
}

export function getCustomer(id: number): Promise<Customer> {
  return fetchJson(`/customers/${id}`)
}

export function createCustomer(data: CustomerFormData): Promise<{ success: boolean; error?: string }> {
  return fetch(BASE + "/customers", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  }).then((r) => r.json())
}

export function updateCustomer(id: number, data: CustomerFormData): Promise<{ success: boolean; error?: string }> {
  return fetch(BASE + "/customers/" + id, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  }).then((r) => r.json())
}

export function deleteCustomer(id: number): Promise<{ success: boolean; error?: string }> {
  return fetch(BASE + "/customers/" + id, { method: "DELETE" }).then((r) => r.json())
}

export interface PurchaseOrder {
  id: number
  order_number: string
  supplier_id: number
  supplier_name: string
  warehouse_id: number
  warehouse_name: string
  total: number
  status: string
  notes: string
  created_at: string
  sent_at: string | null
  received_at: string | null
  paid_at: string | null
}

export interface OrderItem {
  id: number
  product_id: number
  product_name?: string
  quantity: number
  unit_price: number
}

export function getOrders(status?: string, warehouseId?: string): Promise<PurchaseOrder[]> {
  const params = new URLSearchParams()
  if (status && status !== "all") params.set("status", status)
  if (warehouseId) params.set("warehouse_id", warehouseId)
  const qs = params.toString()
  return fetchJson(`/orders${qs ? `?${qs}` : ""}`)
}

export function getOrderItems(orderId: number): Promise<OrderItem[]> {
  return fetchJson(`/orders/${orderId}/items`)
}

export function createOrder(data: { supplier_id: number; warehouse_id?: number; notes?: string; items: { product_id: number; quantity: number; unit_price: number }[] }): Promise<{ success: boolean; order_id?: number; error?: string }> {
  return fetch(BASE + "/orders", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  }).then((r) => r.json())
}

export function updateOrderStatus(orderId: number, status: string): Promise<{ success: boolean; error?: string }> {
  return fetch(BASE + "/orders/" + orderId, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ status }),
  }).then((r) => r.json())
}

export function updateOrder(orderId: number, data: { supplier_id?: number; notes?: string; items?: { product_id: number; quantity: number; unit_price: number }[] }): Promise<{ success: boolean; error?: string }> {
  return fetch(BASE + "/orders/" + orderId, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  }).then((r) => r.json())
}

export function deleteOrder(orderId: number): Promise<{ success: boolean; error?: string }> {
  return fetch(BASE + "/orders/" + orderId, { method: "DELETE" }).then((r) => r.json())
}

export interface Notification {
  id: number
  product_id: number | null
  product_name: string | null
  type: string
  message: string
  warehouse_id: number | null
  is_read: number
  created_at: string
}

export interface NotificationsResponse {
  notifications: Notification[]
  unread_count: number
}

export function getNotifications(warehouseId?: string): Promise<NotificationsResponse> {
  return fetchJson(`/notifications${warehouseId ? `?warehouse_id=${warehouseId}` : ""}`)
}

export function markNotificationRead(id: number): Promise<{ success: boolean }> {
  return fetch(BASE + `/notifications/${id}/read`, { method: "POST" }).then((r) => r.json())
}

export function markAllNotificationsRead(): Promise<{ success: boolean }> {
  return fetch(BASE + "/notifications/mark-all-read", { method: "POST" }).then((r) => r.json())
}

export interface MainAccountTransaction {
  id: number
  type: string
  amount: number
  reason: string
  reference_id: number | null
  note: string
  created_at: string
}

export interface MainAccountData {
  account: { id: number; name: string; current_balance: number; initial_balance: number; created_at: string }
  transactions: MainAccountTransaction[]
}

export function getMainAccountFull(limit?: number): Promise<MainAccountData> {
  return fetchJson(`/main-account${limit ? `?limit=${limit}` : ""}`)
}

export function depositToMainAccount(amount: number, reason: string, note?: string): Promise<{ success: boolean; error?: string }> {
  return fetch(BASE + "/main-account/deposit", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ amount, reason, note }),
  }).then((r) => r.json())
}

export function withdrawFromMainAccount(amount: number, reason: string, note?: string): Promise<{ success: boolean; error?: string }> {
  return fetch(BASE + "/main-account/withdraw", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ amount, reason, note }),
  }).then((r) => r.json())
}

export interface SessionSummary {
  id: number
  session_number: string
  opened_at: string
  closed_at: string | null
  status: string
  warehouse_id: number
  opening_cash: number
  closing_cash: number | null
  expected_cash: number
  total_sales: number
  nb_transactions: number
  total_cash_in: number
  total_cash_out: number
}

export function getSessionsHistory(): Promise<SessionSummary[]> {
  return fetchJson("/kpis/sessions-history")
}

export interface ReportData {
  total_products: number
  total_value: number
  low_stock: number
  out_of_stock: number
  expiring_soon: number
  top_products?: { id: number; name: string; qty: number; value: number }[]
}

export function getReportsSummary(): Promise<ReportData> {
  return fetchJson("/reports")
}

// Invoices
export interface Invoice {
  id: number
  invoice_number: string
  customer_id: number | null
  warehouse_id: number
  status: string
  subtotal: number
  discount_total: number
  tax_amount: number
  total: number
  notes: string
  due_date: string | null
  paid_at: string | null
  created_at: string
  updated_at: string
  customer_name: string | null
  client_code: string | null
  warehouse_name: string
}

export interface InvoiceItem {
  id: number
  invoice_id: number
  product_id: number
  product_name: string
  product_sku: string
  quantity: number
  unit_price: number
  discount_percent: number
  tax_rate: number
  line_total: number
}

export interface InvoiceDetail extends Invoice {
  customer_type: string | null
  customer_address: string | null
  customer_phone: string | null
  customer_email: string | null
  warehouse_address: string | null
  items: InvoiceItem[]
}

export interface InvoiceFormData {
  warehouse_id?: number
  customer_id?: number | null
  notes?: string
  items: { product_id: number; quantity?: number; unit_price?: number; discount_percent?: number; tax_rate?: number }[]
}

export function getInvoices(status?: string, warehouseId?: string): Promise<Invoice[]> {
  const params = new URLSearchParams()
  if (status && status !== "all") params.set("status", status)
  if (warehouseId) params.set("warehouse_id", warehouseId)
  const qs = params.toString()
  return fetchJson(`/invoices${qs ? `?${qs}` : ""}`)
}

export function getInvoice(id: number): Promise<InvoiceDetail> {
  return fetchJson(`/invoices/${id}`)
}

export function createInvoice(data: InvoiceFormData): Promise<{ success: boolean; invoice_id?: number; invoice_number?: string; error?: string }> {
  return fetch(BASE + "/invoices", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  }).then((r) => r.json())
}

export function updateInvoice(id: number, data: { status?: string; notes?: string }): Promise<{ success: boolean; error?: string }> {
  return fetch(BASE + "/invoices/" + id, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  }).then((r) => r.json())
}

export function deleteInvoice(id: number): Promise<{ success: boolean; error?: string }> {
  return fetch(BASE + "/invoices/" + id, { method: "DELETE" }).then((r) => r.json())
}

export function getInvoiceItems(invoiceId: number): Promise<InvoiceItem[]> {
  return fetchJson(`/invoices/${invoiceId}/items`)
}

export function addInvoiceItem(invoiceId: number, data: { product_id: number; quantity?: number; unit_price?: number; discount_percent?: number; tax_rate?: number }): Promise<{ success: boolean; error?: string }> {
  return fetch(BASE + `/invoices/${invoiceId}/items`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  }).then((r) => r.json())
}

export function removeInvoiceItem(invoiceId: number, itemId: number): Promise<{ success: boolean; error?: string }> {
  return fetch(BASE + `/invoices/${invoiceId}/items/${itemId}`, { method: "DELETE" }).then((r) => r.json())
}

export function getInvoiceStats(): Promise<{ total_invoices: number; total_amount: number; paid_amount: number; pending_amount: number }> {
  return fetchJson("/invoice-stats")
}

// POS / Caisse
export interface PosSession {
  id: number
  session_number: string
  warehouse_id: number
  user_name: string
  opening_cash: number
  closing_cash: number
  expected_cash: number
  status: string
  opened_at: string
  closed_at: string | null
}

export interface PosTransaction {
  id: number
  transaction_number: string
  session_id: number
  customer_id: number | null
  payment_method: string
  subtotal: number
  discount_total: number
  tax_amount: number
  total: number
  tendered_amount: number
  change_given: number
  status: string
  created_at: string
  customer_name: string | null
}

export interface PosCartItem {
  product_id: number
  product_name: string
  product_sku: string
  quantity: number
  unit_price: number
  base_price: number
  price_loyal: number
  price_gros: number
  discount_percent?: number
}

export interface CashMovement {
  id: number
  session_id: number
  type: string
  amount: number
  reason: string
  note: string
  created_at: string
}

export interface BestSeller {
  id: number
  name: string
  sku: string
  price: number
  price_base: number
  total_sold: number
}

export interface PosCustomer {
  id: number
  name: string
  client_code: string
  discount_rate: number
  type: string
}

export function getActiveSession(): Promise<PosSession[]> {
  return fetchJson("/pos/sessions")
}

export function openSession(data: { warehouse_id?: number; opening_cash?: number }): Promise<{ success: boolean; session?: PosSession; session_number?: string; error?: string }> {
  return fetch(BASE + "/pos/sessions", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  }).then((r) => r.json())
}

export function closeSession(sessionId: number, data: { closing_cash?: number; deposit_to_main?: boolean }): Promise<{ success: boolean; expected_cash?: number; deposited?: boolean; error?: string }> {
  return fetch(BASE + `/pos/sessions/${sessionId}/close`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  }).then((r) => r.json())
}

export function createPosTransaction(data: {
  session_id: number
  customer_id?: number | null
  items: PosCartItem[]
  payment_method?: string
  tendered_amount?: number
  pricing_tier?: string
  apply_tax?: boolean
  notes?: string
}): Promise<{ success: boolean; document_number?: string; document_type?: string; document_id?: number; total?: number; change_amount?: number; customer_name?: string; error?: string }> {
  return fetch(BASE + "/pos/transactions", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  }).then(async (r) => {
    const text = await r.text()
    try {
      return JSON.parse(text)
    } catch {
      return { success: false, error: `Erreur serveur (${r.status})` }
    }
  })
}

export function getRecentTransactions(limit?: number, sessionId?: number): Promise<PosTransaction[]> {
  const params = new URLSearchParams()
  if (limit) params.set("limit", String(limit))
  if (sessionId) params.set("session_id", String(sessionId))
  const qs = params.toString()
  return fetchJson(`/pos/transactions/recent${qs ? `?${qs}` : ""}`)
}

export function getCashMovements(): Promise<CashMovement[]> {
  return fetchJson("/pos/cash-movements")
}

export function createCashMovement(data: { type: string; amount?: number; reason?: string; note?: string }): Promise<{ success: boolean; movement?: CashMovement; error?: string }> {
  return fetch(BASE + "/pos/cash-movements", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  }).then((r) => r.json())
}

export function getPosCustomers(): Promise<PosCustomer[]> {
  return fetchJson("/pos/customers")
}

export function getBestSellers(limit?: number): Promise<BestSeller[]> {
  const qs = limit ? `?limit=${limit}` : ""
  return fetchJson(`/pos/best-sellers${qs}`)
}

// Locations
export interface Location {
  id: number
  warehouse_id: number
  name: string
  type: string
  capacity: number | null
  created_at: string
  warehouse_name?: string
}

export interface LocationFormData {
  warehouse_id: number
  name: string
  type?: string
  capacity?: number | null
}

export function getLocations(warehouseId?: string): Promise<Location[]> {
  return fetchJson(`/locations${warehouseId ? `?warehouse_id=${warehouseId}` : ""}`)
}

export function createLocation(data: LocationFormData): Promise<{ success: boolean; error?: string }> {
  return fetch(BASE + "/locations", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  }).then((r) => r.json())
}

export function updateLocation(id: number, data: Partial<LocationFormData>): Promise<{ success: boolean; error?: string }> {
  return fetch(BASE + "/locations/" + id, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  }).then((r) => r.json())
}

export function deleteLocation(id: number): Promise<{ success: boolean; error?: string }> {
  return fetch(BASE + "/locations/" + id, { method: "DELETE" }).then((r) => r.json())
}

export function transferToPos(data: { amount: number; note?: string }): Promise<{ success: boolean; account?: { id: number; name: string; initial_balance: number; current_balance: number; created_at: string }; error?: string }> {
  return fetch(BASE + "/main-account/transfer-to-pos", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  }).then((r) => r.json())
}

// Reorder Rules
export interface ReorderRule {
  id: number
  product_id: number
  warehouse_id: number
  min_quantity: number
  max_quantity: number
  trigger_type: string
  supplier_id: number | null
  created_at: string
  product_name: string
  current_qty: number
  supplier_name: string | null
}

export interface ReorderRuleFormData {
  product_id: number
  warehouse_id?: number
  min_quantity?: number
  max_quantity?: number
  trigger_type?: string
  supplier_id?: number | null
}

export function getReorderRules(warehouseId?: string): Promise<ReorderRule[]> {
  return fetchJson(`/reorder-rules${warehouseId ? `?warehouse_id=${warehouseId}` : ""}`)
}

export function createReorderRule(data: ReorderRuleFormData): Promise<{ success: boolean; error?: string }> {
  return fetch(BASE + "/reorder-rules", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  }).then((r) => r.json())
}

export function updateReorderRule(id: number, data: Partial<ReorderRuleFormData>): Promise<{ success: boolean; error?: string }> {
  return fetch(BASE + "/reorder-rules/" + id, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  }).then((r) => r.json())
}

export function deleteReorderRule(id: number): Promise<{ success: boolean; error?: string }> {
  return fetch(BASE + "/reorder-rules/" + id, { method: "DELETE" }).then((r) => r.json())
}

// Replenishment
export interface ReplenishmentItem {
  rule_id: number
  product_id: number
  name: string
  sku: string
  current_qty: number
  min_quantity: number
  max_quantity: number
  trigger_type: string
  supplier_id: number | null
  rule_warehouse_id: number
  supplier_name: string | null
  suggested_qty: number
}

export function getReplenishment(warehouseId?: string): Promise<ReplenishmentItem[]> {
  return fetchJson(`/replenishment${warehouseId ? `?warehouse_id=${warehouseId}` : ""}`)
}

// Printer Settings
export interface PrinterSettings {
  connection_type: string
  host: string
  port: number
  usb_vendor_id: string
  usb_product_id: string
  printer_name: string
  auto_print: boolean
  paper_width: number
}

export function getPrinterSettings(): Promise<PrinterSettings> {
  return fetchJson("/settings/printer")
}

export function updatePrinterSettings(data: Partial<PrinterSettings>): Promise<{ message: string }> {
  return fetch(BASE + "/settings/printer", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  }).then((r) => r.json())
}

export function checkPrinterStatus(): Promise<{ status: string; error?: string }> {
  return fetchJson("/settings/printer/status")
}

export function testPrinter(): Promise<{ message: string; details?: object; error?: string }> {
  return fetch(BASE + "/settings/printer/test", { method: "POST" }).then((r) => r.json())
}

export interface UsbPrinter {
  name: string
  vendor_id: string
  product_id: string
  manufacturer: string
  description: string
  bus: string
  address: string
}

export function discoverPrinters(): Promise<UsbPrinter[]> {
  return fetchJson("/settings/printer/discover")
}


