import { useEffect, useState } from "react"
import { useTranslation } from "react-i18next"
import { useNavigate } from "react-router-dom"
import { Package, Info, Boxes, DollarSign, Truck, History, MapPin, ShoppingCart, AlertTriangle, ExternalLink } from "lucide-react"
import { getProduct, type ProductDetail } from "@/lib/api"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogClose,
} from "@/components/ui/dialog"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Card, CardContent } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"

interface Props {
  productId: number | null
  open: boolean
  onOpenChange: (open: boolean) => void
}

function DetailRow({ label, value, highlight }: { label: string; value: string | number | null | undefined; highlight?: boolean }) {
  return (
    <div className="flex justify-between py-1.5 text-sm border-b border-border/50 last:border-0">
      <span className="text-muted-foreground">{label}</span>
      <span className={`font-medium tabular-nums ${highlight ? "text-primary" : ""}`}>{value ?? "-"}</span>
    </div>
  )
}

function StatCard({ icon, label, value, color }: { icon: React.ReactNode; label: string; value: string; color?: string }) {
  return (
    <Card>
      <CardContent className="flex items-center gap-3 p-3">
        <div className={`shrink-0 ${color ?? "text-muted-foreground"}`}>{icon}</div>
        <div>
          <p className="text-xs text-muted-foreground">{label}</p>
          <p className="text-sm font-bold tabular-nums">{value}</p>
        </div>
      </CardContent>
    </Card>
  )
}

export function ProductDetailDialog({ productId, open, onOpenChange }: Props) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [data, setData] = useState<ProductDetail | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (open && productId) {
      setLoading(true)
      getProduct(productId)
        .then(setData)
        .catch(() => setData(null))
        .finally(() => setLoading(false))
    }
  }, [open, productId])

  const p = data?.product

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-3xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Package className="size-4" />
            {loading ? t("common.loading") : p?.name ?? t("product_detail.dialog.title")}
          </DialogTitle>
          <DialogDescription>
            {p ? <>SKU: <span className="font-mono">{p.sku}</span>{p.barcode ? <> &middot; {t("product_detail.barcode_prefix")} {p.barcode}</> : null}</> : ""}
          </DialogDescription>
        </DialogHeader>

        {loading ? (
          <div className="space-y-4 p-4">
            <Skeleton className="h-8 w-48" />
            <Skeleton className="h-32 w-full" />
            <Skeleton className="h-32 w-full" />
          </div>
        ) : !data ? (
          <p className="text-center py-8 text-muted-foreground">{t("product_detail.dialog.load_error")}</p>
        ) : (
          <Tabs defaultValue="overview" className="mt-2">
            <TabsList className="grid grid-cols-5 mb-4">
              <TabsTrigger value="overview"><Info className="size-3.5" /><span className="hidden sm:inline ml-1">{t("product_detail.tab.overview")}</span></TabsTrigger>
              <TabsTrigger value="stock"><Boxes className="size-3.5" /><span className="hidden sm:inline ml-1">{t("product_detail.tab.stock")}</span></TabsTrigger>
              <TabsTrigger value="pricing"><DollarSign className="size-3.5" /><span className="hidden sm:inline ml-1">{t("product_detail.tab.pricing")}</span></TabsTrigger>
              <TabsTrigger value="supplier"><Truck className="size-3.5" /><span className="hidden sm:inline ml-1">{t("product_detail.tab.supplier")}</span></TabsTrigger>
              <TabsTrigger value="movements"><History className="size-3.5" /><span className="hidden sm:inline ml-1">{t("product_detail.tab.movements")}</span></TabsTrigger>
            </TabsList>

            {/* Overview Tab */}
            <TabsContent value="overview" className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <Card>
                  <CardContent className="p-4">
                    <h4 className="text-sm font-semibold mb-3 flex items-center gap-2"><Info className="size-3.5" />{t("product_detail.general_info")}</h4>
                    <DetailRow label={t("common.name")} value={p?.name} />
                    <DetailRow label={t("product_detail.arabic_name")} value={p?.name_ar} />
                    <DetailRow label={t("products.category")} value={p?.category_ar ? p.category_ar + ' / ' + p.category : p?.category} />
                    <DetailRow label={t("products.barcode")} value={p?.barcode} />
                    <DetailRow label={t("products.warehouse")} value={p?.warehouse_name} />
                    <DetailRow label={t("product_detail.locations")} value={p?.location_name} />
                  </CardContent>
                </Card>
                <Card>
                  <CardContent className="p-4">
                    <h4 className="text-sm font-semibold mb-3 flex items-center gap-2"><AlertTriangle className="size-3.5" />{t("product_detail.thresholds_alerts")}</h4>
                    <DetailRow label={t("product_detail.current_stock")} value={p?.quantity} highlight />
                    <DetailRow label={t("product_detail.min_stock")} value={p?.min_quantity} />
                    <DetailRow label={t("product_detail.max_stock")} value={p?.max_quantity} />
                    <DetailRow label={t("product_detail.expiry")} value={p?.expiry_date} />
                    <DetailRow label={t("product_detail.lot")} value={p?.lot_number} />
                  </CardContent>
                </Card>
              </div>
            </TabsContent>

            {/* Stock Tab */}
            <TabsContent value="stock" className="space-y-4">
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                <StatCard icon={<Boxes className="size-4" />} label={t("product_detail.current_stock")} value={String(p?.quantity ?? 0)} color="text-primary" />
                <StatCard icon={<ShoppingCart className="size-4" />} label={t("product_detail.total_purchases")} value={`${data.purchase_stats.total_qty} (${data.purchase_stats.total_purchases.toFixed(2)} DH)`} />
                <StatCard icon={<DollarSign className="size-4" />} label={t("product_detail.total_sales")} value={`${data.sales_stats.total_qty} (${data.sales_stats.total_sales.toFixed(2)} DH)`} />
                <StatCard icon={<MapPin className="size-4" />} label={t("product_detail.locations")} value={String(data.stock_locations.length)} />
              </div>
              {data.stock_locations.length > 0 && (
                <Card>
                  <CardContent className="p-4">
                    <h4 className="text-sm font-semibold mb-2">{t("product_detail.stock_by_location")}</h4>
                    {data.stock_locations.map((loc, i) => (
                      <DetailRow key={i} label={loc.location_name} value={loc.quantity} />
                    ))}
                  </CardContent>
                </Card>
              )}
            </TabsContent>

            {/* Pricing Tab */}
            <TabsContent value="pricing" className="space-y-4">
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                <StatCard icon={<DollarSign className="size-4" />} label={t("product_detail.sale_price")} value={`${p?.price.toFixed(2)} DH`} color="text-primary" />
                <StatCard icon={<DollarSign className="size-4" />} label={t("product_detail.purchase_price")} value={`${(p?.purchase_price_avg ?? 0).toFixed(2)} DH`} />
                <StatCard icon={<DollarSign className="size-4" />} label={t("product_detail.margin")} value={`${p?.margin_percent ?? 0}%`} color="text-emerald-600" />
                <StatCard icon={<DollarSign className="size-4" />} label={t("product_detail.discount")} value={`${p?.discount_rate ?? 0}%`} color="text-amber-600" />
              </div>
              <Card>
                <CardContent className="p-4">
                  <h4 className="text-sm font-semibold mb-3">{t("product_detail.pricing_by_customer")}</h4>
                  <div className="grid grid-cols-3 gap-3">
                    <PriceTier label={t("pos.pricing_tier.normal")} value={p?.price_base ?? p?.price ?? 0} />
                    <PriceTier label={t("pos.pricing_tier.loyal")} value={p?.price_loyal ?? p?.price ?? 0} />
                    <PriceTier label={t("pos.pricing_tier.bulk")} value={p?.price_gros ?? p?.price ?? 0} />
                  </div>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-4">
                  <h4 className="text-sm font-semibold mb-3">{t("product_detail.taxes_discounts")}</h4>
                  <DetailRow label={t("product_detail.tva")} value={`${p?.tax_category ?? 0}%`} />
                  <DetailRow label={t("product_detail.discount_category")} value={p?.discount_category ?? t("common.none")} />
                  <DetailRow label={t("product_detail.discount_rate")} value={p?.discount_rate ? `${p.discount_rate}%` : "0%"} />
                </CardContent>
              </Card>
            </TabsContent>

            {/* Supplier Tab */}
            <TabsContent value="supplier" className="space-y-4">
              {p?.supplier_name ? (
                <Card>
                  <CardContent className="p-4">
                    <div className="flex items-center gap-3 mb-4">
                      <Truck className="size-8 text-muted-foreground" />
                      <div>
                        <h4 className="font-semibold">{p.supplier_name}</h4>
                        {p.supplier_email && <p className="text-xs text-muted-foreground">{p.supplier_email}</p>}
                      </div>
                    </div>
                    <DetailRow label={t("common.email")} value={p.supplier_email} />
                    <DetailRow label={t("common.phone")} value={p.supplier_phone} />
                  </CardContent>
                </Card>
              ) : (
                <p className="text-center py-8 text-muted-foreground">{t("product_detail.no_supplier")}</p>
              )}
            </TabsContent>

            {/* Movements Tab */}
            <TabsContent value="movements" className="space-y-4">
              {data.movements.length > 0 ? (
                <Card>
                  <CardContent className="p-0">
                    <div className="divide-y">
                      {data.movements.map((m, i) => (
                        <div key={i} className="flex items-center justify-between px-4 py-2.5 text-sm">
                          <div className="flex items-center gap-2">
                            <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${
                              m.type === "in" ? "bg-emerald-100 text-emerald-700" : "bg-red-100 text-red-700"
                            }`}>{m.type === "in" ? t("movements.in") : t("movements.out")}</span>
                            <span className="text-muted-foreground text-xs">{m.note}</span>
                          </div>
                          <div className="flex items-center gap-3">
                            <span className="tabular-nums font-medium">{Math.abs(m.quantity)}</span>
                            <span className="text-xs text-muted-foreground">{m.created_at.slice(0, 10)}</span>
                          </div>
                        </div>
                      ))}
                    </div>
                  </CardContent>
                </Card>
              ) : (
                <p className="text-center py-8 text-muted-foreground">{t("product_detail.no_movements")}</p>
              )}
            </TabsContent>
          </Tabs>
        )}

        <div className="flex justify-between pt-2 border-t">
          <Button variant="link" size="sm" onClick={() => navigate(`/product/${productId}`)}>
            <ExternalLink className="size-3.5 mr-1" />
            {t("product_detail.more_details")}
          </Button>
          <DialogClose asChild>
            <Button variant="outline">{t("common.close")}</Button>
          </DialogClose>
        </div>
      </DialogContent>
    </Dialog>
  )
}

function PriceTier({ label, value }: { label: string; value: number }) {
  return (
    <div className="bg-muted/50 rounded-lg p-3 text-center">
      <p className="text-xs text-muted-foreground mb-1">{label}</p>
      <p className="text-sm font-bold tabular-nums">{value.toFixed(2)} DH</p>
    </div>
  )
}
