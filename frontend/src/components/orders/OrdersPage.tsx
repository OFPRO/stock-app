import { useTranslation } from "react-i18next"
import { useEffect, useState, useCallback, useMemo } from "react"
import { Plus, Search, Eye } from "lucide-react"
import { type ColumnDef, type SortingState, flexRender, getCoreRowModel, getSortedRowModel, useReactTable } from "@tanstack/react-table"
import { getOrders, getOrderItems, updateOrderStatus, type PurchaseOrder, type OrderItem } from "@/lib/api"
import { OrderCreateDialog } from "./OrderCreateDialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter, DialogClose } from "@/components/ui/dialog"
import { Card, CardContent } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"

const STATUS_COLORS: Record<string, string> = { brouillon: "bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300", recue: "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400", paye: "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400", annulee: "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400" }

const STATUS_ACTIONS: Record<string, string[]> = { brouillon: ["recue", "annulee"], recue: ["paye", "annulee"], paye: ["annulee"] }

export function OrdersPage() {
  const { t } = useTranslation()
  const STATUS_LABELS: Record<string, string> = { brouillon: t("orders.status.draft"), recue: t("orders.status.received"), paye: t("orders.status.paid"), annulee: t("orders.status.cancelled") }
  const [orders, setOrders] = useState<PurchaseOrder[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")
  const [statusFilter, setStatusFilter] = useState("all")
  const [sorting, setSorting] = useState<SortingState>([{ id: "created_at", desc: true }])
  const [createOpen, setCreateOpen] = useState(false)
  const [detailOpen, setDetailOpen] = useState(false)
  const [detailItems, setDetailItems] = useState<OrderItem[]>([])
  const [detailOrder, setDetailOrder] = useState<PurchaseOrder | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    try { setOrders(await getOrders()) }
    catch (err) { console.error(err) }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { load() }, [load])

  const filtered = useMemo(() => orders.filter(o => {
    if (statusFilter !== "all" && o.status !== statusFilter) return false
    if (search) { const q = search.toLowerCase(); return o.order_number.toLowerCase().includes(q) || o.supplier_name.toLowerCase().includes(q) }
    return true
  }), [orders, statusFilter, search])

  const openCreate = () => setCreateOpen(true)

  const openDetail = useCallback(async (order: PurchaseOrder) => {
    setDetailOrder(order)
    try { setDetailItems(await getOrderItems(order.id)) }
    catch { setDetailItems([]) }
    setDetailOpen(true)
  }, [])

  const handleStatusChange = useCallback(async (orderId: number, status: string) => {
    const res = await updateOrderStatus(orderId, status)
    if (res.success) load()
  }, [load])

  const columns = useMemo<ColumnDef<PurchaseOrder>[]>(() => [
    { accessorKey: "order_number", header: t("orders.number"), cell: ({ getValue }) => <span className="font-mono text-xs text-muted-foreground">{getValue() as string}</span> },
    { accessorKey: "supplier_name", header: t("orders.supplier") },
    { accessorKey: "total", header: t("common.total"), cell: ({ getValue }) => <span className="tabular-nums">{(getValue() as number).toFixed(2)} DH</span> },
    { accessorKey: "status", header: t("common.status"), cell: ({ getValue }) => { const v = getValue() as string; return <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_COLORS[v] ?? ""}`}>{STATUS_LABELS[v] ?? v}</span> } },
    { accessorKey: "created_at", header: t("common.date"), cell: ({ getValue }) => <span className="text-xs text-muted-foreground">{(getValue() as string).slice(0, 10)}</span> },
    { id: "actions", header: "", cell: ({ row }) => {
      const o = row.original
      const nextStatuses = STATUS_ACTIONS[o.status] ?? []
      return <div className="flex justify-end gap-1">
        <Button variant="ghost" size="icon-sm" onClick={() => openDetail(o)}><Eye className="size-3.5" /></Button>
        {nextStatuses.map(s => <Button key={s} variant="outline" size="sm" className="text-xs h-7" onClick={() => handleStatusChange(o.id, s)}>{STATUS_LABELS[s] ?? s}</Button>)}
      </div>
    }},
  ], [openDetail, handleStatusChange, t])

  const table = useReactTable({ data: filtered, columns, state: { sorting }, onSortingChange: setSorting, getCoreRowModel: getCoreRowModel(), getSortedRowModel: getSortedRowModel() })

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div><h1 className="text-2xl font-bold tracking-tight">{t("orders.title")}</h1><p className="text-sm text-muted-foreground">{t("orders.count", { count: orders.length })}</p></div>
        <Button onClick={openCreate}><Plus className="size-4" />{t("orders.new")}</Button>
      </div>
      <div className="flex flex-wrap items-center gap-3">
        <div className="relative flex-1 min-w-[200px] max-w-sm"><Search className="absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" /><Input className="pl-8" placeholder={t("common.search")} value={search} onChange={(e) => setSearch(e.target.value)} /></div>
        <Select value={statusFilter} onValueChange={setStatusFilter}><SelectTrigger className="w-36"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="all">Tous</SelectItem><SelectItem value="brouillon">{t("orders.status.draft")}</SelectItem><SelectItem value="recue">{t("orders.status.received")}</SelectItem><SelectItem value="paye">{t("orders.status.paid")}</SelectItem><SelectItem value="annulee">{t("orders.status.cancelled")}</SelectItem></SelectContent></Select>
      </div>
      {loading ? <div className="space-y-3">{Array.from({ length: 8 }).map((_, i) => <Skeleton key={i} className="h-10 w-full rounded-lg" />)}</div>
      : <Card><CardContent className="p-0"><Table><TableHeader>{table.getHeaderGroups().map(hg => <TableRow key={hg.id}>{hg.headers.map(h => <TableHead key={h.id} onClick={h.column.getToggleSortingHandler()} className={h.column.getCanSort() ? "cursor-pointer select-none" : ""}>{flexRender(h.column.columnDef.header, h.getContext())}{{ asc: " ▲", desc: " ▼" }[h.column.getIsSorted() as string] ?? ""}</TableHead>)}</TableRow>)}</TableHeader><TableBody>{table.getRowModel().rows.length === 0 ? <TableRow><TableCell colSpan={6} className="text-center py-8 text-muted-foreground">{t("orders.empty")}</TableCell></TableRow> : table.getRowModel().rows.map(row => <TableRow key={row.id}>{row.getVisibleCells().map(cell => <TableCell key={cell.id}>{flexRender(cell.column.columnDef.cell, cell.getContext())}</TableCell>)}</TableRow>)}</TableBody></Table></CardContent></Card>}

      <OrderCreateDialog open={createOpen} onOpenChange={setCreateOpen} onSuccess={load} />

      <Dialog open={detailOpen} onOpenChange={setDetailOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader><DialogTitle>{t("orders.number")} {detailOrder?.order_number}</DialogTitle><DialogDescription>{t("orders.supplier")}: {detailOrder?.supplier_name}</DialogDescription></DialogHeader>
          <div className="space-y-2">
            <div className="flex justify-between text-sm"><span className="text-muted-foreground">{t("common.total")}</span><span className="font-medium tabular-nums">{detailOrder?.total.toFixed(2)} DH</span></div>
            <div className="flex justify-between text-sm"><span className="text-muted-foreground">{t("common.status")}</span><span>{detailOrder ? (STATUS_LABELS[detailOrder.status] ?? detailOrder.status) : ""}</span></div>
            <div className="flex justify-between text-sm"><span className="text-muted-foreground">{t("common.date")}</span><span>{detailOrder?.created_at.slice(0, 10)}</span></div>
            {detailItems.length > 0 && <div className="pt-2 border-t"><p className="text-xs font-medium mb-2">Articles</p>{detailItems.map(item => <div key={item.id} className="flex justify-between text-sm py-1"><span>{item.product_name ?? `Produit #${item.product_id}`}</span><span className="tabular-nums">{item.quantity} × {item.unit_price.toFixed(2)} DH</span></div>)}</div>}
          </div>
          <DialogFooter><DialogClose asChild><Button>{t("common.close")}</Button></DialogClose></DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}