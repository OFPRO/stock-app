import { BrowserRouter, Routes, Route } from "react-router-dom"
import { DashboardLayout } from "@/components/layout/DashboardLayout"
import { DashboardPage } from "@/components/dashboard/DashboardPage"
import { ProductsPage } from "@/components/products/ProductsPage"
import { ProductDetailPage } from "@/components/products/ProductDetailPage"
import { MovementsPage } from "@/components/movements/MovementsPage"
import { SuppliersPage } from "@/components/suppliers/SuppliersPage"
import { WarehousesPage } from "@/components/warehouses/WarehousesPage"
import { LocationsPage } from "@/components/locations/LocationsPage"
import { CustomersPage } from "@/components/customers/CustomersPage"
import { OrdersPage } from "@/components/orders/OrdersPage"
import { OrderDetailPage } from "@/components/orders/OrderDetailPage"
import { ReorderRulesPage } from "@/components/reorder/ReorderRulesPage"
import { ReplenishmentPage } from "@/components/reorder/ReplenishmentPage"
import { NotificationsPage } from "@/components/notifications/NotificationsPage"
import { MainAccountPage } from "@/components/main-account/MainAccountPage"
import { SessionsPage } from "@/components/sessions/SessionsPage"
import { ReportsPage } from "@/components/reports/ReportsPage"
import { InvoicesPage } from "@/components/invoices/InvoicesPage"
import { PosPage } from "@/components/pos/PosPage"
import { ScannerPage } from "@/components/scanner/ScannerPage"

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<DashboardLayout />}>
          <Route index element={<DashboardPage />} />
          <Route path="products" element={<ProductsPage />} />
          <Route path="product/:id" element={<ProductDetailPage />} />
          <Route path="movements" element={<MovementsPage />} />
          <Route path="suppliers" element={<SuppliersPage />} />
          <Route path="warehouses" element={<WarehousesPage />} />
          <Route path="locations" element={<LocationsPage />} />
          <Route path="customers" element={<CustomersPage />} />
          <Route path="orders" element={<OrdersPage />} />
          <Route path="orders/:id" element={<OrderDetailPage />} />
          <Route path="reorder-rules" element={<ReorderRulesPage />} />
          <Route path="replenishment" element={<ReplenishmentPage />} />
          <Route path="invoices" element={<InvoicesPage />} />
          <Route path="pos" element={<PosPage />} />
          <Route path="notifications" element={<NotificationsPage />} />
          <Route path="main-account" element={<MainAccountPage />} />
          <Route path="sessions" element={<SessionsPage />} />
          <Route path="reports" element={<ReportsPage />} />
          <Route path="scanner" element={<ScannerPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

export default App