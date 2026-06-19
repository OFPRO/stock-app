import { useEffect, useState } from "react"
import { useTranslation } from "react-i18next"
import { toast } from "sonner"
import {
  getPrinterSettings,
  updatePrinterSettings,
  checkPrinterStatus,
  testPrinter,
  discoverPrinters,
  type UsbPrinter,
  type PrinterSettings,
} from "@/lib/api"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Switch } from "@/components/ui/switch"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { useTheme } from "@/components/theme-provider"
import { Printer, Search, Settings, Monitor, Moon, Sun, CheckCircle2, AlertCircle } from "lucide-react"
import { ScrollArea } from "@/components/ui/scroll-area"
import { cn } from "@/lib/utils"

export function SettingsPage() {
  const { t } = useTranslation()
  const { theme, setTheme } = useTheme()

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-2">
        <Settings className="size-5" />
        <h1 className="text-2xl font-bold">{t("settings.title")}</h1>
      </div>

      <Tabs defaultValue="printer" className="w-full">
        <TabsList>
          <TabsTrigger value="general">
            <Monitor className="size-4" />
            <span className="ml-2">{t("settings.general")}</span>
          </TabsTrigger>
          <TabsTrigger value="printer">
            <Printer className="size-4" />
            <span className="ml-2">{t("settings.printer")}</span>
          </TabsTrigger>
        </TabsList>

        <TabsContent value="general" className="space-y-4 mt-4">
          <Card>
            <CardHeader>
              <CardTitle>{t("settings.theme")}</CardTitle>
              <CardDescription>{t("settings.theme_desc")}</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="flex gap-4">
                <Button
                  variant={theme === "light" ? "default" : "outline"}
                  onClick={() => setTheme("light")}
                  className="flex items-center gap-2"
                >
                  <Sun className="size-4" />
                  {t("settings.light")}
                </Button>
                <Button
                  variant={theme === "dark" ? "default" : "outline"}
                  onClick={() => setTheme("dark")}
                  className="flex items-center gap-2"
                >
                  <Moon className="size-4" />
                  {t("settings.dark")}
                </Button>
                <Button
                  variant={theme === "system" ? "default" : "outline"}
                  onClick={() => setTheme("system")}
                  className="flex items-center gap-2"
                >
                  <Monitor className="size-4" />
                  {t("settings.system")}
                </Button>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="printer" className="space-y-4 mt-4">
          <PrinterSettings />
        </TabsContent>
      </Tabs>
    </div>
  )
}

function PrinterSettings() {
  const { t } = useTranslation()
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [testing, setTesting] = useState(false)
  const [scanning, setScanning] = useState(false)
  const [status, setStatus] = useState<{ status: string; error?: string } | null>(null)
  const [usbPrinters, setUsbPrinters] = useState<UsbPrinter[]>([])
  const [selectedUsb, setSelectedUsb] = useState<string | null>(null)
  const [config, setConfig] = useState<PrinterSettings>({
    connection_type: "network",
    host: "",
    port: 9100,
    usb_vendor_id: "",
    usb_product_id: "",
    printer_name: "",
    auto_print: true,
    paper_width: 80,
  })

  useEffect(() => {
    getPrinterSettings()
      .then((cfg) => {
        setConfig(cfg)
        if (cfg.usb_vendor_id) {
          setSelectedUsb(`${cfg.usb_vendor_id}:${cfg.usb_product_id}`)
        }
      })
      .catch(() => toast.error(t("settings.load_error")))
      .finally(() => setLoading(false))
    checkPrinterStatus()
      .then(setStatus)
      .catch(() => {})
  }, [])

  function refreshStatus() {
    checkPrinterStatus().then(setStatus).catch(() => {})
  }

  async function handleScan() {
    setScanning(true)
    setUsbPrinters([])
    try {
      const printers = await discoverPrinters()
      setUsbPrinters(printers)
      if (!printers.length) {
        toast.error(t("settings.no_usb_printers"))
      }
    } catch {
      toast.error(t("settings.scan_error"))
    } finally {
      setScanning(false)
    }
  }

  function handleSelectUsb(p: UsbPrinter) {
    const key = `${p.vendor_id}:${p.product_id}`
    setSelectedUsb(key)
    setConfig({
      ...config,
      usb_vendor_id: p.vendor_id,
      usb_product_id: p.product_id,
      instance_id: p.instance_id || "",
    })
  }

  async function handleSave() {
    setSaving(true)
    try {
      await updatePrinterSettings(config)
      toast.success(t("settings.save_success"))
      refreshStatus()
    } catch {
      toast.error(t("settings.save_error"))
    } finally {
      setSaving(false)
    }
  }

  async function handleTest() {
    setTesting(true)
    try {
      const res = await testPrinter()
      if (res.error) {
        toast.error(res.error)
      } else {
        toast.success(res.message)
      }
    } catch {
      toast.error(t("settings.test_error"))
    } finally {
      setTesting(false)
    }
  }

  if (loading) {
    return (
      <Card>
        <CardContent className="pt-6 space-y-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-10 w-full" />
          ))}
        </CardContent>
      </Card>
    )
  }

  const statusLabel =
    status?.status === "online"
      ? t("settings.printer_online")
      : status?.status === "offline"
        ? t("settings.printer_offline")
        : t("settings.printer_not_configured")

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>{t("settings.printer_config")}</CardTitle>
              <CardDescription>{t("settings.printer_config_desc")}</CardDescription>
            </div>
            <div className="flex items-center gap-2 text-sm">
              <span
                className={`size-2 rounded-full ${
                  status?.status === "online" ? "bg-green-500" : "bg-gray-400"
                }`}
              />
              <span className="text-muted-foreground">{statusLabel}</span>
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center justify-between">
            <Label htmlFor="auto-print">{t("settings.auto_print")}</Label>
            <Switch
              id="auto-print"
              checked={config.auto_print}
              onCheckedChange={(v) => setConfig({ ...config, auto_print: v })}
            />
          </div>

          <div className="space-y-2">
            <Label>{t("settings.connection_type")}</Label>
            <Select
              value={config.connection_type}
              onValueChange={(v) => setConfig({ ...config, connection_type: v })}
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="network">{t("settings.network")}</SelectItem>
                <SelectItem value="usb">{t("settings.usb")}</SelectItem>
                <SelectItem value="windows">{t("settings.windows")}</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {config.connection_type === "network" ? (
            <>
              <div className="space-y-2">
                <Label htmlFor="host">{t("settings.printer_host")}</Label>
                <Input
                  id="host"
                  value={config.host}
                  onChange={(e) => setConfig({ ...config, host: e.target.value })}
                  placeholder="192.168.1.100"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="port">{t("settings.printer_port")}</Label>
                <Input
                  id="port"
                  type="number"
                  value={config.port}
                  onChange={(e) => setConfig({ ...config, port: Number(e.target.value) })}
                />
              </div>
            </>
          ) : (
            <div className="space-y-3">
              {config.connection_type === "windows" && (
                <div className="space-y-2">
                  <Label htmlFor="printer-name">{t("settings.printer_name")}</Label>
                  <Input
                    id="printer-name"
                    value={config.printer_name}
                    onChange={(e) => setConfig({ ...config, printer_name: e.target.value, host: e.target.value })}
                    placeholder="XP-80"
                  />
                </div>
              )}
              <div className="flex items-center justify-between">
                <Label>{t("settings.usb_printers")}</Label>
                <Button variant="outline" size="sm" onClick={handleScan} disabled={scanning}>
                  <Search className="size-4 mr-1" />
                  {scanning ? t("settings.scanning") : t("settings.scan_usb")}
                </Button>
              </div>
              {usbPrinters.length > 0 && (
                <ScrollArea className="border rounded-md max-h-48">
                  <div className="p-1 space-y-0.5">
                    {usbPrinters.map((p, i) => {
                      const key = `${p.vendor_id}:${p.product_id}`
                      const isSelected = selectedUsb === key
                      return (
                        <button
                          key={i}
                          type="button"
                          onClick={() => handleSelectUsb(p)}
                          className={cn(
                            "w-full flex items-center gap-3 px-3 py-2.5 rounded-md text-left transition-colors",
                            isSelected ? "bg-primary/10" : "hover:bg-muted"
                          )}
                        >
                          <Printer className="size-5 shrink-0 text-primary" />
                          <div className="flex-1 min-w-0">
                            <div className="text-sm font-medium truncate">{p.name}</div>
                            <div className="text-xs text-muted-foreground">
                              VID: {p.vendor_id} &nbsp; PID: {p.product_id}
                              {p.manufacturer ? ` · ${p.manufacturer}` : ""}
                            </div>
                          </div>
                          {isSelected && <CheckCircle2 className="size-4 shrink-0 text-primary" />}
                        </button>
                      )
                    })}
                  </div>
                </ScrollArea>
              )}
              {!usbPrinters.length && !scanning && (
                <p className="text-sm text-muted-foreground">{t("settings.usb_printers_hint")}</p>
              )}
            </div>
          )}
        </CardContent>
        <CardFooter className="flex justify-between">
          <Button variant="outline" onClick={handleTest} disabled={testing}>
            {testing ? t("settings.testing") : t("settings.test_print")}
          </Button>
          <Button onClick={handleSave} disabled={saving}>
            {saving ? t("common.saving") : t("common.save")}
          </Button>
        </CardFooter>
      </Card>
    </>
  )
}
