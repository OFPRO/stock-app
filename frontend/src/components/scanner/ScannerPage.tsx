import { useEffect, useRef, useState } from "react"
import { useNavigate } from "react-router-dom"
import { useTranslation } from "react-i18next"
import { ScanBarcode, Package, X, Eye } from "lucide-react"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { toast } from "sonner"
import { getProductByBarcode } from "@/lib/api"

interface ScannedProduct {
  id: number
  name: string
  sku: string
  barcode?: string
  price: number
  sale_price?: number
  quantity: number
  category?: string
}

export function ScannerPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [scanning, setScanning] = useState(false)
  const [scannedCode, setScannedCode] = useState<string | null>(null)
  const [product, setProduct] = useState<ScannedProduct | null>(null)
  const [loading, setLoading] = useState(false)
  const scannerRef = useRef<HTMLDivElement>(null)
  const scannerInstance = useRef<{ stop: () => Promise<void> } | null>(null)

  const startScanning = async () => {
    setScannedCode(null)
    setProduct(null)
    try {
      await import("html5-qrcode")
      const Html5Qrcode = (window as unknown as Record<string, unknown>).Html5Qrcode as new (id: string) => { start: (config: Record<string, unknown>, cb: (text: string) => void) => Promise<void>; stop: () => Promise<void> }
      if (!Html5Qrcode) throw new Error("html5-qrcode not loaded")

      const scanner = new Html5Qrcode("scanner-element")
      scannerInstance.current = scanner
      setScanning(true)
      await scanner.start(
        { fps: 10, qrbox: { width: 250, height: 150 } },
        (decodedText: string) => {
          setScannedCode(decodedText)
          scanner.stop().catch(() => {})
          scannerInstance.current = null
          setScanning(false)
          lookupProduct(decodedText)
        }
      )
    } catch (err) {
      toast.error(t("scanner.camera_error"))
      setScanning(false)
    }
  }

  const stopScanning = async () => {
    if (scannerInstance.current) {
      await scannerInstance.current.stop().catch(() => {})
      scannerInstance.current = null
    }
    setScanning(false)
  }

  const lookupProduct = async (barcode: string) => {
    setLoading(true)
    try {
      const result = await getProductByBarcode(barcode)
      if (result) {
        setProduct(result as ScannedProduct)
        toast.success(t("scanner.found", { name: (result as ScannedProduct).name }))
      } else {
        setProduct(null)
        toast.error(t("scanner.not_found", { code: barcode }))
      }
    } catch {
      toast.error(t("scanner.lookup_error"))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    return () => { stopScanning() }
  }, [])

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">{t("scanner.title")}</h1>
          <p className="text-sm text-muted-foreground">{t("scanner.subtitle")}</p>
        </div>
        <Button onClick={scanning ? stopScanning : startScanning} variant={scanning ? "destructive" : "default"}>
          {scanning ? <X className="size-4" /> : <ScanBarcode className="size-4" />}
          {scanning ? t("scanner.stop") : t("scanner.start")}
        </Button>
      </div>

      {scannedCode && (
        <Card>
          <CardContent className="flex items-center gap-3 p-4">
            <Package className="size-5 text-primary" />
            <div className="flex-1">
              <p className="text-sm font-medium">{t("scanner.scanned")}</p>
              <code className="text-xs text-muted-foreground">{scannedCode}</code>
            </div>
            {loading ? (
              <Badge variant="secondary">{t("scanner.searching")}</Badge>
            ) : product ? (
              <Badge variant="success">{t("scanner.found_short")}</Badge>
            ) : (
              <Badge variant="destructive">{t("scanner.not_found_short")}</Badge>
            )}
          </CardContent>
        </Card>
      )}

      {product && (
        <Card>
          <CardContent className="p-4 space-y-3">
            <div className="flex items-start justify-between">
              <div>
                <h3 className="font-semibold text-lg">{product.name}</h3>
                <p className="text-xs text-muted-foreground">
                  SKU: {product.sku}
                  {product.barcode ? ` | Code: ${product.barcode}` : ""}
                </p>
              </div>
              <Badge>{product.category || t("scanner.uncategorized")}</Badge>
            </div>
            <div className="grid grid-cols-2 gap-2 text-sm">
              <div>
                <span className="text-muted-foreground">{t("scanner.stock")}:</span>{" "}
                <span className={product.quantity <= 0 ? "text-destructive font-medium" : ""}>
                  {product.quantity}
                </span>
              </div>
              <div>
                <span className="text-muted-foreground">{t("scanner.price")}:</span>{" "}
                <span className="font-medium">{product.sale_price ?? product.price} DH</span>
              </div>
            </div>
            <Button
              variant="outline"
              size="sm"
              className="w-full"
              onClick={() => navigate(`/products/${product.id}`)}
            >
              <Eye className="size-4 mr-2" />
              {t("scanner.view_product")}
            </Button>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardContent className="flex flex-col items-center justify-center py-8 text-muted-foreground gap-4">
          {scanning ? (
            <div id="scanner-element" ref={scannerRef} className="w-full max-w-md aspect-video rounded-lg overflow-hidden bg-black" />
          ) : (
            <>
              <ScanBarcode className="size-16 opacity-20" />
              <p className="text-sm">
                {scannedCode
                  ? product
                    ? t("scanner.scan_again")
                    : t("scanner.not_found_hint")
                  : t("scanner.placeholder")}
              </p>
              <p className="text-xs">{t("scanner.help_text")}</p>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
