import { useEffect, useState, useCallback } from "react"
import {
  DollarSign,
  Receipt,
  Calculator,
  TrendingUp,
  AlertTriangle,
  CheckCircle,
  Package,
  XCircle,
  Landmark,
  RefreshCw,
} from "lucide-react"
import { fetchDashboardData, type DashboardData } from "@/lib/api"
import { KpiCard } from "@/components/dashboard/KpiCard"
import { DashboardFilters } from "@/components/dashboard/DashboardFilters"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Skeleton } from "@/components/ui/skeleton"
import {
  SalesDailyChart,
  CategoriesChart,
  TopProductsChart,
  InvoicesStatusChart,
  MovementsChart,
  MarginsChart,
} from "@/components/dashboard/charts"

export function DashboardPage() {
  const [data, setData] = useState<DashboardData | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [period, setPeriod] = useState("30")
  const [warehouseId, setWarehouseId] = useState("__all__")

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const result = await fetchDashboardData(Number(period), warehouseId)
      setData(result)
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err)
      setError(msg)
      console.error("Failed to load dashboard:", msg)
    } finally {
      setLoading(false)
    }
  }, [period, warehouseId])

  useEffect(() => {
    load()
  }, [load])

  if (loading && !data) {
    return <DashboardSkeleton />
  }

  if (!data) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] gap-4 text-muted-foreground">
        <p>Erreur lors du chargement du tableau de bord</p>
        {error && (
          <p className="text-xs max-w-md text-center text-destructive bg-destructive/10 px-4 py-2 rounded-md">
            {error}
          </p>
        )}
        <button
          onClick={load}
          disabled={loading}
          className="inline-flex items-center gap-2 text-sm font-medium text-primary hover:underline disabled:opacity-50"
        >
          <RefreshCw className={`size-4 ${loading ? "animate-spin" : ""}`} />
          Réessayer
        </button>
      </div>
    )
  }

  const { sales, margins, receivables, invoicesStatus, dashboard, salesDaily, categoriesDistribution, topSelling, trends, alertes, warehouses, mainAccount } = data

  const totalSalesAmount = salesDaily.reduce((sum, d) => sum + d.ca, 0)

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Dashboard Commercial</h1>
          <p className="text-sm text-muted-foreground">
            Vue d&apos;ensemble de votre activité
          </p>
        </div>
        <DashboardFilters
          period={period}
          onPeriodChange={setPeriod}
          warehouseId={warehouseId}
          onWarehouseChange={setWarehouseId}
          warehouses={warehouses}
          onRefresh={load}
          loading={loading}
        />
      </div>

      <section>
        <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-3">
          Performance des Ventes
        </h2>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <KpiCard
            label="CA Aujourd'hui"
            value={`${sales.ca_jour.toFixed(2)} DH`}
            icon={DollarSign}
            color="primary"
            trend={{ value: sales.ca_trend, direction: sales.ca_trend >= 0 ? "up" : "down" }}
          />
          <KpiCard
            label="Ventes Aujourd'hui"
            value={sales.nb_ventes_jour}
            icon={Receipt}
            color="success"
          />
          <KpiCard
            label="Ticket Moyen"
            value={`${sales.ticket_moyen.toFixed(2)} DH`}
            icon={Calculator}
            color="primary"
          />
          <KpiCard
            label="Marge Brute"
            value={`${margins.marge_globale.toFixed(1)}%`}
            icon={TrendingUp}
            color="success"
          />
        </div>
      </section>

      <section>
        <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-3">
          Situation Financière
        </h2>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
          <KpiCard
            label="Créances"
            value={`${receivables.total_creances.toFixed(2)} DH`}
            icon={AlertTriangle}
            color="warning"
          />
          <KpiCard
            label="Taux d'Encaissement"
            value={`${receivables.taux_encaissement.toFixed(1)}%`}
            icon={CheckCircle}
            color="success"
          />
          <KpiCard
            label="Valeur Stock"
            value={`${dashboard.total_value.toFixed(2)} DH`}
            icon={Package}
            color="primary"
          />
          <KpiCard
            label="Ruptures"
            value={dashboard.out_of_stock}
            icon={XCircle}
            color="danger"
          />
          <KpiCard
            label="Compte Principal"
            value={`${mainAccount.account.current_balance.toFixed(2)} DH`}
            icon={Landmark}
            color="primary"
          />
        </div>
      </section>

      <div className="grid gap-4 lg:grid-cols-3">
        <ChartCard title="Ventes Journalières" description={`Total: ${totalSalesAmount.toFixed(2)} DH`}>
          <SalesDailyChart data={salesDaily} />
        </ChartCard>
        <ChartCard title="Ventes par Catégorie" description="Répartition des quantités vendues">
          <CategoriesChart data={categoriesDistribution} />
        </ChartCard>
        <ChartCard title="Top 10 Produits" description="Les plus vendus">
          <TopProductsChart data={topSelling} />
        </ChartCard>
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <ChartCard title="État des Factures" description="Par statut">
          <InvoicesStatusChart data={invoicesStatus} />
        </ChartCard>
        <ChartCard title="Mouvements Stock" description="Entrées / Sorties">
          <MovementsChart data={trends} />
        </ChartCard>
        <ChartCard title="Répartition Marge" description="Par catégorie">
          <MarginsChart data={margins} />
        </ChartCard>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-sm font-semibold flex items-center gap-2">
              <Package className="size-4" />
              À Commander
            </CardTitle>
          </CardHeader>
          <CardContent>
            <MiniTable
              columns={["Produit", "Stock", "Min", "À cmd"]}
              rows={dashboard.products_to_order.slice(0, 8).map((p) => [
                p.name,
                <span key={`s-${p.id}`} className={p.quantity <= 0 ? "text-red-600 font-medium" : ""}>{p.quantity}</span>,
                p.min_quantity,
                p.needed,
              ])}
              emptyText="Aucun produit à commander"
            />
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-sm font-semibold flex items-center gap-2">
              <AlertTriangle className="size-4" />
              Créances Clients
            </CardTitle>
          </CardHeader>
          <CardContent>
            <MiniTable
              columns={["Client", "Montant", "Échéance"]}
              rows={receivables.clients.slice(0, 8).map((c) => [
                c.name,
                `${c.montant.toFixed(2)} DH`,
                c.premiere_echeance?.slice(0, 10) ?? "-",
              ])}
              emptyText="Aucune créance"
            />
          </CardContent>
        </Card>
      </div>

      {alertes.out_of_stock.length > 0 || alertes.expiring.length > 0 ? (
        <div className="grid gap-4 lg:grid-cols-2">
          {alertes.out_of_stock.length > 0 && (
            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="text-sm font-semibold flex items-center gap-2">
                  <XCircle className="size-4 text-red-600" />
                  Ruptures de Stock
                </CardTitle>
              </CardHeader>
              <CardContent>
                <MiniTable
                  columns={["Produit", "Stock", "Min"]}
                  rows={alertes.out_of_stock.slice(0, 8).map((p) => [
                    p.name,
                    <span key={`r-${p.id}`} className="text-red-600 font-medium">0</span>,
                    p.min_quantity,
                  ])}
                  emptyText="Aucune rupture"
                />
              </CardContent>
            </Card>
          )}
          {alertes.expiring.length > 0 && (
            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="text-sm font-semibold flex items-center gap-2">
                  <Package className="size-4" />
                  Périmés
                </CardTitle>
              </CardHeader>
              <CardContent>
                <MiniTable
                  columns={["Produit", "DLC", "Jours"]}
                  rows={alertes.expiring.slice(0, 8).map((p) => [
                    p.name,
                    p.expiry_date ?? "-",
                    <span key={`e-${p.id}`} className={p.days_left != null && p.days_left <= 7 ? "text-red-600 font-medium" : ""}>
                      {p.days_left != null ? `${p.days_left} j` : "-"}
                    </span>,
                  ])}
                  emptyText="Aucun produit périmé"
                />
              </CardContent>
            </Card>
          )}
        </div>
      ) : null}
    </div>
  )
}

function ChartCard({ title, description, children }: { title: string; description: string; children: React.ReactNode }) {
  return (
    <Card>
      <CardHeader className="pb-3">
        <CardTitle className="text-sm font-semibold">{title}</CardTitle>
        <CardDescription className="text-xs">{description}</CardDescription>
      </CardHeader>
      <CardContent>{children}</CardContent>
    </Card>
  )
}

function MiniTable({
  columns,
  rows,
  emptyText,
}: {
  columns: string[]
  rows: (string | number | React.ReactNode)[][]
  emptyText: string
}) {
  if (rows.length === 0) {
    return <p className="text-sm text-muted-foreground py-4 text-center">{emptyText}</p>
  }
  return (
    <Table>
      <TableHeader>
        <TableRow>
          {columns.map((col) => (
            <TableHead key={col} className="text-xs">{col}</TableHead>
          ))}
        </TableRow>
      </TableHeader>
      <TableBody>
        {rows.map((cells, i) => (
          <TableRow key={i}>
            {cells.map((cell, j) => (
              <TableCell key={j} className="text-sm">{cell}</TableCell>
            ))}
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}

function DashboardSkeleton() {
  return (
    <div className="space-y-6">
      <div className="flex justify-between">
        <div className="space-y-2">
          <Skeleton className="h-8 w-64" />
          <Skeleton className="h-4 w-40" />
        </div>
        <div className="flex gap-3">
          <Skeleton className="h-9 w-28" />
          <Skeleton className="h-9 w-44" />
          <Skeleton className="h-9 w-9" />
        </div>
      </div>
      {[4, 5].map((count) => (
        <div key={count} className="space-y-3">
          <Skeleton className="h-4 w-48" />
          <div className="grid gap-4" style={{ gridTemplateColumns: `repeat(${Math.min(count, 4)}, 1fr)` }}>
            {Array.from({ length: count }).map((_, i) => (
              <Skeleton key={i} className="h-32 rounded-xl" />
            ))}
          </div>
        </div>
      ))}
      {[3, 3].map((_, ri) => (
        <div key={`chart-${ri}`} className="grid gap-4 lg:grid-cols-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-64 rounded-xl" />
          ))}
        </div>
      ))}
      <div className="grid gap-4 lg:grid-cols-2">
        {[1, 2].map((i) => (
          <Skeleton key={i} className="h-48 rounded-xl" />
        ))}
      </div>
    </div>
  )
}
