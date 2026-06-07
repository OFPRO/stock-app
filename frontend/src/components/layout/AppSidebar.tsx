import { useTranslation } from "react-i18next"
import {
  LayoutDashboard,
  Package,
  Warehouse,
  MapPin,
  ArrowLeftRight,
  Truck,
  ClipboardList,
  Users,
  FileText,
  BarChart3,
  History,
  QrCode,
  ShoppingCart,
  Landmark,
  Bell,
  RefreshCw,
  ClipboardCheck,
} from "lucide-react"
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from "@/components/ui/sidebar"
import { cn } from "@/lib/utils"
import { Link, useLocation } from "react-router-dom"

const navItems = [
  { key: "nav.dashboard", icon: LayoutDashboard, href: "/" },
  { key: "nav.products", icon: Package, href: "/products" },
  { key: "nav.warehouses", icon: Warehouse, href: "/warehouses" },
  { key: "nav.storageZones", icon: MapPin, href: "/locations" },
  { key: "nav.movements", icon: ArrowLeftRight, href: "/movements" },
  { key: "nav.suppliers", icon: Truck, href: "/suppliers" },
  { key: "nav.orders", icon: ClipboardList, href: "/orders" },
  { key: "nav.reorderRules", icon: RefreshCw, href: "/reorder-rules" },
  { key: "nav.replenishment", icon: ClipboardCheck, href: "/replenishment" },
  { key: "nav.customers", icon: Users, href: "/customers" },
  { key: "nav.invoices", icon: FileText, href: "/invoices" },
  { key: "nav.reports", icon: BarChart3, href: "/reports" },
  { key: "nav.sessionHistory", icon: History, href: "/sessions" },
  { key: "nav.scanner", icon: QrCode, href: "/scanner" },
  { key: "nav.pos", icon: ShoppingCart, href: "/pos" },
  { key: "nav.mainAccount", icon: Landmark, href: "/main-account" },
  { key: "nav.notifications", icon: Bell, href: "/notifications" },
]

export function AppSidebar() {
  const { t } = useTranslation()
  const location = useLocation()

  return (
    <Sidebar collapsible="icon">
      <SidebarHeader className="border-b px-4 py-3">
        <Link to="/" className="flex items-center gap-2 font-semibold text-lg">
          <Package className="size-5" />
          <span className="truncate">StockPro</span>
        </Link>
      </SidebarHeader>
      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupContent>
            <SidebarMenu>
              {navItems.map((item) => {
                const Icon = item.icon
                const isActive = location.pathname === item.href
                const label = t(item.key)
                return (
                  <SidebarMenuItem key={item.href}>
                    <SidebarMenuButton asChild isActive={isActive} tooltip={label}>
                      <Link
                        to={item.href}
                        className={cn(
                          "flex items-center gap-3",
                          isActive && "font-semibold"
                        )}
                      >
                        <Icon className="size-4 shrink-0" />
                        <span>{label}</span>
                      </Link>
                    </SidebarMenuButton>
                  </SidebarMenuItem>
                )
              })}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>
      <SidebarFooter className="border-t px-4 py-3 text-xs text-muted-foreground">
        <span>{t("footer.company")}</span>
      </SidebarFooter>
    </Sidebar>
  )
}
