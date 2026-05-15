import { Outlet } from "react-router-dom"
import { SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar"
import { AppSidebar } from "@/components/layout/AppSidebar"
import { Separator } from "@/components/ui/separator"
import { TooltipProvider } from "@/components/ui/tooltip"

export function DashboardLayout() {
  return (
    <TooltipProvider>
    <SidebarProvider>
      <AppSidebar />
      <main className="flex flex-1 flex-col min-h-svh">
        <header className="flex h-12 shrink-0 items-center gap-2 border-b px-4">
          <SidebarTrigger />
          <Separator orientation="vertical" className="h-6" />
          <span className="text-sm font-medium">StockPro — Gestion de Stock</span>
        </header>
        <div className="flex-1 p-4 md:p-6 overflow-auto">
          <Outlet />
        </div>
      </main>
    </SidebarProvider>
    </TooltipProvider>
  )
}
