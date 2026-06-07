import { useEffect } from "react"
import { Outlet } from "react-router-dom"
import { Toaster } from "sonner"
import { SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar"
import { AppSidebar } from "@/components/layout/AppSidebar"
import { Separator } from "@/components/ui/separator"
import { TooltipProvider } from "@/components/ui/tooltip"
import { LanguageSwitcher } from "@/i18n/LanguageSwitcher"
import { useTranslation } from "react-i18next"
import { getCurrentDir } from "@/i18n"

export function DashboardLayout() {
  const { t, i18n } = useTranslation()

  useEffect(() => {
    const dir = getCurrentDir()
    document.documentElement.dir = dir
    document.documentElement.lang = i18n.language
  }, [i18n.language])

  return (
    <TooltipProvider>
    <SidebarProvider>
      <AppSidebar />
      <main className="flex flex-1 flex-col min-h-svh">
        <header className="flex h-12 shrink-0 items-center gap-2 border-b px-4">
          <SidebarTrigger />
          <Separator orientation="vertical" className="h-6" />
          <LanguageSwitcher />
          <Separator orientation="vertical" className="h-6" />
          <span className="text-sm font-medium">{t("app.title")}</span>
        </header>
        <div className="flex-1 p-4 md:p-6 overflow-auto">
          <Outlet />
        </div>
      </main>
    </SidebarProvider>
      <Toaster position="top-right" richColors />
    </TooltipProvider>
  )
}
