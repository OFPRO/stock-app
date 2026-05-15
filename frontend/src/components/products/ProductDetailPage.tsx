import { useEffect, useState } from "react"
import { useParams, useNavigate } from "react-router-dom"
import { Package, Info, Boxes, DollarSign, Truck, History, MapPin, ShoppingCart, AlertTriangle, ArrowLeft } from "lucide-react"
import { getProduct, type ProductDetail } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Card, CardContent } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"

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

function PriceTier({ label, value }: { label: string; value: number }) {
  return (
    <div className="bg-muted/50 rounded-lg p-3 text-center">
      <p className="text-xs text-muted-foreground mb-1">{label}</p>
      <p className="text-sm font-bold tabular-nums">{value.toFixed(2)} DH</p>
    </div>
  )
}

export function ProductDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [data, setData] = useState<ProductDetail | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (id) {
      setLoading(true)
      getProduct(Number(id))
        .then(setData)
        .catch(() => setData(null))
        .finally(() => setLoading(false))
    }
  }, [id])

  const p = data?.product

  if (loading) {
    return (
      <div className="p-6 space-y-4">
        <div className="flex items-center gap-4 mb-6">
          <Skeleton className="h-10 w-10 rounded-full" />
          <Skeleton className="h-8 w-64" />
        </div>
        <Skeleton className="h-32 w-full" />
        <Skeleton className="h-64 w-full" />
      </div>
    )
  }

  if (!data || !p) {
    return (
      <div className="p-6 text-center">
        <p className="text-muted-foreground">Produit introuvable.</p>
        <Button variant="outline" className="mt-4" onClick={() => navigate("/products")}>
          <ArrowLeft className="size-4 mr-2" />
          Retour aux produits
        </Button>
      </div>
    )
  }

  return (
    <div className="p-6">
      <div className="flex items-center gap-4 mb-6">
        <Button variant="ghost" size="icon" onClick={() => navigate("/products")}>
          <ArrowLeft className="size-5" />
        </Button>
        <div className="flex-1">
          <h1 className="text-2xl font-bold flex items-center gap-2">
            <Package className="size-5" />
            {p.name}
          </h1>
          <p className="text-sm text-muted-foreground">
            SKU: <span className="font-mono">{p.sku}</span>
            {p.barcode ? <> &middot; Code-barres: {p.barcode}</> : null}
          </p>
        </div>
      </div>

      <div className="flex gap-4 mb-6">
        <Card>
          <CardContent className="flex items-center gap-3 p-4">
            <div className={`text-center ${p.quantity <= 0 ? "text-red-500" : p.quantity <= p.min_quantity ? "text-amber-500" : "text-emerald-500"}`}>
              <p className="text-2xl font-bold tabular-nums">{p.quantity}</p>
              <p className="text-xs text-muted-foreground">en stock</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center gap-3 p-4">
            <div className="text-center">
              <p className="text-lg font-bold tabular-nums text-primary">{p.price.toFixed(2)} DH</p>
              <p className="text-xs text-muted-foreground">Prix de vente</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center gap-3 p-4">
            <div className="text-center">
              <p className={`text-lg font-bold tabular-nums ${p.quantity <= 0 ? "text-red-500" : p.quantity <= p.min_quantity ? "text-amber-500" : "text-emerald-500"}`}>
                {p.quantity <= 0 ? "Rupture" : p.quantity <= p.min_quantity ? "Stock Faible" : "Normal"}
              </p>
              <p className="text-xs text-muted-foreground">Statut</p>
            </div>
          </CardContent>
        </Card>
      </div>

      <Tabs defaultValue="overview">
        <TabsList className="grid grid-cols-5 mb-4 w-fit">
          <TabsTrigger value="overview"><Info className="size-3.5" /><span className="ml-1">Aperçu</span></TabsTrigger>
          <TabsTrigger value="stock"><Boxes className="size-3.5" /><span className="ml-1">Stock</span></TabsTrigger>
          <TabsTrigger value="pricing"><DollarSign className="size-3.5" /><span className="ml-1">Prix</span></TabsTrigger>
          <TabsTrigger value="supplier"><Truck className="size-3.5" /><span className="ml-1">Fournisseur</span></TabsTrigger>
          <TabsTrigger value="movements"><History className="size-3.5" /><span className="ml-1">Historique</span></TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Card>
              <CardContent className="p-4">
                <h4 className="text-sm font-semibold mb-3 flex items-center gap-2"><Info className="size-3.5" />Informations Générales</h4>
                <DetailRow label="Nom" value={p.name} />
                <DetailRow label="Nom Arabe" value={p.name_ar} />
                <DetailRow label="Catégorie" value={p.category} />
                <DetailRow label="Barcode" value={p.barcode} />
                <DetailRow label="Entrepôt" value={p.warehouse_name} />
                <DetailRow label="Emplacement" value={p.location_name} />
              </CardContent>
            </Card>
            <Card>
              <CardContent className="p-4">
                <h4 className="text-sm font-semibold mb-3 flex items-center gap-2"><AlertTriangle className="size-3.5" />Seuils & Alertes</h4>
                <DetailRow label="Stock Actuel" value={p.quantity} highlight />
                <DetailRow label="Stock Minimum" value={p.min_quantity} />
                <DetailRow label="Stock Maximum" value={p.max_quantity} />
                <DetailRow label="Expiry" value={p.expiry_date} />
                <DetailRow label="Lot" value={p.lot_number} />
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="stock" className="space-y-4">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <StatCard icon={<Boxes className="size-4" />} label="Stock actuel" value={String(p.quantity)} color="text-primary" />
            <StatCard icon={<ShoppingCart className="size-4" />} label="Achats totaux" value={`${data.purchase_stats.total_qty} (${data.purchase_stats.total_purchases.toFixed(2)} DH)`} />
            <StatCard icon={<DollarSign className="size-4" />} label="Ventes totales" value={`${data.sales_stats.total_qty} (${data.sales_stats.total_sales.toFixed(2)} DH)`} />
            <StatCard icon={<MapPin className="size-4" />} label="Emplacements" value={String(data.stock_locations.length)} />
          </div>
          {data.stock_locations.length > 0 && (
            <Card>
              <CardContent className="p-4">
                <h4 className="text-sm font-semibold mb-2">Stock par Emplacement</h4>
                {data.stock_locations.map((loc, i) => (
                  <DetailRow key={i} label={loc.location_name} value={loc.quantity} />
                ))}
              </CardContent>
            </Card>
          )}
        </TabsContent>

        <TabsContent value="pricing" className="space-y-4">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <StatCard icon={<DollarSign className="size-4" />} label="Prix vente" value={`${p.price.toFixed(2)} DH`} color="text-primary" />
            <StatCard icon={<DollarSign className="size-4" />} label="Prix achat" value={`${(p.purchase_price_avg ?? 0).toFixed(2)} DH`} />
            <StatCard icon={<DollarSign className="size-4" />} label="Marge" value={`${p.margin_percent ?? 0}%`} color="text-emerald-600" />
            <StatCard icon={<DollarSign className="size-4" />} label="Remise" value={`${p.discount_rate ?? 0}%`} color="text-amber-600" />
          </div>
          <Card>
            <CardContent className="p-4">
              <h4 className="text-sm font-semibold mb-3">Tarifs par Catégorie Client</h4>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                <PriceTier label="Normal" value={p.price_base ?? p.price ?? 0} />
                <PriceTier label="Étudiant (-15%)" value={p.price_student ?? p.price ?? 0} />
                <PriceTier label="École (-20%)" value={p.price_school ?? p.price ?? 0} />
                <PriceTier label="Fidèle (-15%)" value={p.price_loyal ?? p.price ?? 0} />
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-4">
              <h4 className="text-sm font-semibold mb-3">Taxes & Remises</h4>
              <DetailRow label="TVA" value={`${p.tax_category ?? 0}%`} />
              <DetailRow label="Catégorie Remise" value={p.discount_category ?? "Aucune"} />
              <DetailRow label="Taux Remise" value={p.discount_rate ? `${p.discount_rate}%` : "0%"} />
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="supplier" className="space-y-4">
          {p.supplier_name ? (
            <Card>
              <CardContent className="p-4">
                <div className="flex items-center gap-3 mb-4">
                  <Truck className="size-8 text-muted-foreground" />
                  <div>
                    <h4 className="font-semibold">{p.supplier_name}</h4>
                    {p.supplier_email && <p className="text-xs text-muted-foreground">{p.supplier_email}</p>}
                  </div>
                </div>
                <DetailRow label="Email" value={p.supplier_email} />
                <DetailRow label="Téléphone" value={p.supplier_phone} />
              </CardContent>
            </Card>
          ) : (
            <p className="text-center py-8 text-muted-foreground">Aucun fournisseur associé à ce produit.</p>
          )}
        </TabsContent>

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
                        }`}>{m.type === "in" ? "Entrée" : "Sortie"}</span>
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
            <p className="text-center py-8 text-muted-foreground">Aucun mouvement pour ce produit.</p>
          )}
        </TabsContent>
      </Tabs>
    </div>
  )
}
