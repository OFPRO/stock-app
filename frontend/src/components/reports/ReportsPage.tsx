import { useEffect, useState, useCallback } from "react"
import { Package, AlertTriangle, DollarSign, TrendingUp, BarChart3 } from "lucide-react"
import { getReportsSummary, getProducts, type ReportData, type Product } from "@/lib/api"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"

export function ReportsPage() {
  const [data, setData] = useState<ReportData | null>(null)
  const [products, setProducts] = useState<Product[]>([])
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    setLoading(true)
    try { const [r, p] = await Promise.all([getReportsSummary(), getProducts()]); setData(r); setProducts(p) }
    catch (err) { console.error(err) }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { load() }, [load])

  if (loading && !data) return <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">{Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-28 rounded-xl" />)}</div>
  if (!data) return <div className="flex items-center justify-center min-h-[400px] text-muted-foreground">Erreur de chargement</div>

  return (
    <div className="space-y-6">
      <div><h1 className="text-2xl font-bold tracking-tight">Rapports</h1><p className="text-sm text-muted-foreground">Synthèse et inventaire</p></div>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card><CardContent className="pt-6"><div className="flex items-center gap-3"><Package className="size-5 text-primary" /><div><p className="text-xs text-muted-foreground">Total Produits</p><p className="text-2xl font-bold">{data.total_products}</p></div></div></CardContent></Card>
        <Card><CardContent className="pt-6"><div className="flex items-center gap-3"><DollarSign className="size-5 text-primary" /><div><p className="text-xs text-muted-foreground">Valeur Stock</p><p className="text-2xl font-bold">{data.total_value.toFixed(2)} DH</p></div></div></CardContent></Card>
        <Card><CardContent className="pt-6"><div className="flex items-center gap-3"><AlertTriangle className="size-5 text-amber-500" /><div><p className="text-xs text-muted-foreground">Stock Bas</p><p className="text-2xl font-bold">{data.low_stock}</p></div></div></CardContent></Card>
        <Card><CardContent className="pt-6"><div className="flex items-center gap-3"><TrendingUp className="size-5 text-red-500" /><div><p className="text-xs text-muted-foreground">Ruptures</p><p className="text-2xl font-bold">{data.out_of_stock}</p></div></div></CardContent></Card>
      </div>
      <Card>
        <CardHeader><CardTitle className="text-sm flex items-center gap-2"><BarChart3 className="size-4" />Inventaire des Produits</CardTitle></CardHeader>
        <CardContent className="p-0">
          <Table><TableHeader><TableRow><TableHead>SKU</TableHead><TableHead>Nom</TableHead><TableHead>Catégorie</TableHead><TableHead>Stock</TableHead><TableHead>Min</TableHead><TableHead>Prix</TableHead><TableHead>Valeur</TableHead></TableRow></TableHeader>
          <TableBody>{products.map(p => <TableRow key={p.id}>
            <TableCell className="font-mono text-xs text-muted-foreground">{p.sku}</TableCell>
            <TableCell className="font-medium">{p.name}</TableCell>
            <TableCell className="text-xs text-muted-foreground">{p.category}</TableCell>
            <TableCell className={`tabular-nums ${p.quantity <= p.min_quantity ? "text-red-600 font-medium" : ""}`}>{p.quantity}</TableCell>
            <TableCell className="tabular-nums text-muted-foreground">{p.min_quantity}</TableCell>
            <TableCell className="tabular-nums">{p.price.toFixed(2)}</TableCell>
            <TableCell className="tabular-nums">{(p.quantity * p.price).toFixed(2)} DH</TableCell>
          </TableRow>)}</TableBody></Table>
        </CardContent>
      </Card>
    </div>
  )
}