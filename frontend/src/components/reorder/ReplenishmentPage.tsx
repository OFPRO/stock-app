import { useEffect, useState, useCallback, useMemo } from "react"
import { ShoppingCart, Search } from "lucide-react"
import {
  type ColumnDef,
  type SortingState,
  flexRender,
  getCoreRowModel,
  getSortedRowModel,
  useReactTable,
} from "@tanstack/react-table"
import {
  getReplenishment,
  getWarehouses,
  type ReplenishmentItem,
  type Warehouse,
} from "@/lib/api"
import { Input } from "@/components/ui/input"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  Card,
  CardContent,
} from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { useTranslation } from "react-i18next"

const TRIGGER_LABELS: Record<string, string> = { manual: "Manuel", auto: "Automatique" }

export function ReplenishmentPage() {
  const { t } = useTranslation()
  const [items, setItems] = useState<ReplenishmentItem[]>([])
  const [warehouses, setWarehouses] = useState<Warehouse[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")
  const [warehouseFilter, setWarehouseFilter] = useState("__all__")
  const [sorting, setSorting] = useState<SortingState>([{ id: "name", desc: false }])

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const wh = warehouseFilter !== "__all__" ? warehouseFilter : undefined
      const [r, w] = await Promise.all([
        getReplenishment(wh),
        getWarehouses(),
      ])
      setItems(r); setWarehouses(w)
    } catch (err) {
      console.error("Failed to load replenishment:", err)
    } finally {
      setLoading(false)
    }
  }, [warehouseFilter])

  useEffect(() => { load() }, [load])

  const needReorder = useMemo(() => items.filter((i) => i.suggested_qty > 0), [items])
  const totalSuggested = useMemo(() => needReorder.reduce((sum, i) => sum + i.suggested_qty, 0), [needReorder])

  const filtered = useMemo(() => {
    if (!search) return items
    const q = search.toLowerCase()
    return items.filter((i) =>
      i.name.toLowerCase().includes(q) ||
      i.sku.toLowerCase().includes(q) ||
      (i.supplier_name ?? "").toLowerCase().includes(q)
    )
  }, [items, search])

  const columns = useMemo<ColumnDef<ReplenishmentItem>[]>(() => [
    { accessorKey: "name", header: t("products.name"), cell: ({ getValue }) => <span className="font-medium">{getValue() as string}</span> },
    { accessorKey: "sku", header: "SKU", cell: ({ getValue }) => <span className="font-mono text-xs text-muted-foreground">{getValue() as string}</span> },
    {
      accessorKey: "current_qty",
      header: t("common.stock"),
      cell: ({ getValue, row }) => {
        const qty = getValue() as number
        const min = row.original.min_quantity
        const low = qty <= min
        return <span className={`tabular-nums ${low ? "text-amber-600 font-medium" : ""}`}>{qty}</span>
      },
    },
    { accessorKey: "min_quantity", header: "Min", cell: ({ getValue }) => <span className="tabular-nums">{getValue() as number}</span> },
    { accessorKey: "max_quantity", header: "Max", cell: ({ getValue }) => <span className="tabular-nums">{getValue() as number}</span> },
    {
      accessorKey: "suggested_qty",
      header: t("replenishment.to_order"),
      cell: ({ getValue }) => {
        const qty = getValue() as number
        if (qty === 0) return <span className="text-xs text-muted-foreground">—</span>
        return <span className="tabular-nums font-semibold text-emerald-600">{qty}</span>
      },
    },
    { accessorKey: "trigger_type", header: "Déclenchement", cell: ({ getValue }) => { const v = getValue() as string; return <span className="text-xs">{TRIGGER_LABELS[v] ?? v}</span> } },
    { accessorKey: "supplier_name", header: "Fournisseur", cell: ({ getValue }) => { const v = getValue() as string | null; return v ? <span className="text-xs text-muted-foreground">{v}</span> : <span className="text-xs text-muted-foreground">—</span> } },
  ], [])

  const table = useReactTable({
    data: filtered,
    columns,
    state: { sorting },
    onSortingChange: setSorting,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
  })

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">{t("replenishment.title")}</h1>
          <p className="text-sm text-muted-foreground">
            {t("replenishment.count", { count: items.length })}
            {needReorder.length > 0 && (
              <> — <span className="font-medium text-amber-600">{t("replenishment.to_replenish", { count: needReorder.length })}</span> ({t("replenishment.units", { count: totalSuggested })})</>
            )}
          </p>
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <div className="relative flex-1 min-w-[200px] max-w-sm">
          <Search className="absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input className="pl-8" placeholder={t("common.search")} value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
        <Select value={warehouseFilter} onValueChange={setWarehouseFilter}>
          <SelectTrigger className="w-40"><SelectValue /></SelectTrigger>
          <SelectContent>
            <SelectItem value="__all__">{t("common.all")}</SelectItem>
            {warehouses.map((w) => (
              <SelectItem key={w.id} value={String(w.id)}>{w.name}</SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {needReorder.length > 0 && (
        <Card className="border-amber-200 dark:border-amber-800">
          <CardContent className="p-4 flex items-center gap-3">
            <ShoppingCart className="size-5 text-amber-600 shrink-0" />
            <p className="text-sm">
              <span className="font-medium">{t("replenishment.alert", { count: needReorder.length })}</span> —
              <span className="font-semibold tabular-nums"> {t("replenishment.total_to_order", { units: totalSuggested })}</span>
            </p>
          </CardContent>
        </Card>
      )}

      {loading ? (
        <div className="space-y-3">{Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-10 w-full rounded-lg" />)}</div>
      ) : (
        <Card>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                {table.getHeaderGroups().map((hg) => (
                  <TableRow key={hg.id}>
                    {hg.headers.map((header) => (
                      <TableHead key={header.id} onClick={header.column.getToggleSortingHandler()} className={header.column.getCanSort() ? "cursor-pointer select-none" : ""}>
                        {flexRender(header.column.columnDef.header, header.getContext())}
                        {{ asc: " ▲", desc: " ▼" }[header.column.getIsSorted() as string] ?? ""}
                      </TableHead>
                    ))}
                  </TableRow>
                ))}
              </TableHeader>
              <TableBody>
                {table.getRowModel().rows.length === 0 ? (
                  <TableRow><TableCell colSpan={columns.length} className="text-center py-8 text-muted-foreground">{t("replenishment.empty")}</TableCell></TableRow>
                ) : (
                  table.getRowModel().rows.map((row) => (
                    <TableRow key={row.id}>
                      {row.getVisibleCells().map((cell) => (
                        <TableCell key={cell.id}>{flexRender(cell.column.columnDef.cell, cell.getContext())}</TableCell>
                      ))}
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
