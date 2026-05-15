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
  { label: "Dashboard", icon: LayoutDashboard, href: "/" },
  { label: "Produits", icon: Package, href: "/products" },
  { label: "Entrepôts", icon: Warehouse, href: "/warehouses" },
  { label: "Zones de Stock", icon: MapPin, href: "/locations" },
  { label: "Mouvements", icon: ArrowLeftRight, href: "/movements" },
  { label: "Fournisseurs", icon: Truck, href: "/suppliers" },
  { label: "Commandes", icon: ClipboardList, href: "/orders" },
  { label: "Règles Réap.", icon: RefreshCw, href: "/reorder-rules" },
  { label: "Réappro.", icon: ClipboardCheck, href: "/replenishment" },
  { label: "Clients", icon: Users, href: "/customers" },
  { label: "Factures", icon: FileText, href: "/invoices" },
  { label: "Rapports", icon: BarChart3, href: "/reports" },
  { label: "Hist. Sessions", icon: History, href: "/sessions" },
  { label: "Scanner", icon: QrCode, href: "/scanner" },
  { label: "Caisse", icon: ShoppingCart, href: "/pos" },
  { label: "Compte Principal", icon: Landmark, href: "/main-account" },
  { label: "Notifications", icon: Bell, href: "/notifications" },
]

export function AppSidebar() {
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
                return (
                  <SidebarMenuItem key={item.href}>
                    <SidebarMenuButton asChild isActive={isActive} tooltip={item.label}>
                      <Link
                        to={item.href}
                        className={cn(
                          "flex items-center gap-3",
                          isActive && "font-semibold"
                        )}
                      >
                        <Icon className="size-4 shrink-0" />
                        <span>{item.label}</span>
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
        <span>Bibliothèque Badr — Marrakech</span>
      </SidebarFooter>
    </Sidebar>
  )
}
