import { useEffect, useState, useCallback, useMemo } from "react"
import { Plus, Search } from "lucide-react"
import {
  type ColumnDef,
  type SortingState,
  flexRender,
  getCoreRowModel,
  getSortedRowModel,
  useReactTable,
} from "@tanstack/react-table"
import {
  getMovements,
  createMovement,
  getProducts,
  type StockMovement,
  type MovementFormData,
  type Product,
} from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
  DialogClose,
} from "@/components/ui/dialog"
import {
  Card,
  CardContent,
} from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { NativeSelect } from "@/components/ui/native-select"
import { useTranslation } from "react-i18next"

const TYPE_COLORS: Record<string, string> = {
  in: "text-emerald-600 dark:text-emerald-400",
  out: "text-red-600 dark:text-red-400",
  sale: "text-red-600 dark:text-red-400",
  transfer: "text-blue-600 dark:text-blue-400",
  inter_warehouse: "text-blue-600 dark:text-blue-400",
  destruction: "text-red-600 dark:text-red-400",
  retour: "text-amber-600 dark:text-amber-400",
  other: "text-muted-foreground",
}

function TypeBadge({ type }: { type: string }) {
  const { t } = useTranslation()
  const TYPE_LABELS: Record<string, string> = {
    in: t("movements.in"),
    out: t("movements.out"),
    sale: t("movements.pos_sale"),
    transfer: t("movements.transfer"),
    inter_warehouse: t("movements.inter_warehouse"),
    destruction: t("movements.destruction"),
    retour: t("movements.return"),
    other: t("movements.other"),
  }
  const label = TYPE_LABELS[type] ?? type
  const color = TYPE_COLORS[type] ?? "text-muted-foreground"
  return (
    <span className={`inline-flex items-center rounded-full bg-muted px-2 py-0.5 text-xs font-medium ${color}`}>
      {label}
    </span>
  )
}

export function MovementsPage() {
  const { t } = useTranslation()
  const [movements, setMovements] = useState<StockMovement[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")
  const [typeFilter, setTypeFilter] = useState("__all__")
  const [sorting, setSorting] = useState<SortingState>([{ id: "created_at", desc: true }])
  const [dialogOpen, setDialogOpen] = useState(false)
  const [formData, setFormData] = useState<MovementFormData>({
    product_id: 0,
    type: "in",
    quantity: 1,
    note: "",
  })
  const [products, setProducts] = useState<Product[]>([])
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const data = await getMovements()
      setMovements(data)
    } catch (err) {
      console.error("Failed to load movements:", err)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  const openCreate = async () => {
    setFormData({ product_id: 0, type: "in", quantity: 1, note: "" })
    try {
      const data = await getProducts()
      setProducts(data)
    } catch {
      setProducts([])
    }
    setDialogOpen(true)
  }

  const handleSave = async () => {
    if (!formData.product_id || formData.quantity <= 0) return
    setSaving(true)
    try {
      const res = await createMovement(formData.product_id, formData)
      if (res.success) {
        setDialogOpen(false)
        load()
      } else {
        console.error("Create movement failed:", res.error)
      }
    } catch (err) {
      console.error("Save failed:", err)
    } finally {
      setSaving(false)
    }
  }

  const filterMatch = (type: string, filter: string): boolean => {
    if (filter === "in") return ["in", "retour"].includes(type)
    if (filter === "out") return ["out", "sale", "destruction"].includes(type)
    if (filter === "transfer") return ["transfer", "inter_warehouse"].includes(type)
    if (filter === "other") return type === "other"
    return true
  }

  const filtered = useMemo(() => movements.filter((m) => {
    if (typeFilter !== "__all__" && !filterMatch(m.type, typeFilter)) return false
    if (search) {
      const q = search.toLowerCase()
      if (!m.product_name.toLowerCase().includes(q) && !(m.note || "").toLowerCase().includes(q)) {
        return false
      }
    }
    return true
  }), [movements, typeFilter, search])

  const columns = useMemo<ColumnDef<StockMovement>[]>(() => [
    {
      accessorKey: "created_at",
      header: t("common.date"),
      cell: ({ getValue }) => {
        const d = getValue() as string
        return <span className="text-xs tabular-nums text-muted-foreground">{d.slice(0, 16)}</span>
      },
    },
    {
      accessorKey: "product_name",
      header: t("common.product"),
      cell: ({ getValue }) => (
        <span className="font-medium">{getValue() as string}</span>
      ),
    },
    {
      accessorKey: "type",
      header: t("movements.type"),
      cell: ({ getValue }) => <TypeBadge type={getValue() as string} />,
    },
    {
      accessorKey: "quantity",
      header: t("common.qty"),
      cell: ({ row }) => {
        const qty = row.original.quantity
        const isOut = ["out", "sale", "destruction"].includes(row.original.type)
        return (
          <span className={`tabular-nums font-medium ${isOut ? "text-red-600 dark:text-red-400" : "text-emerald-600 dark:text-emerald-400"}`}>
            {isOut ? "-" : "+"}{qty}
          </span>
        )
      },
    },
    {
      accessorKey: "source_location",
      header: t("movements.from"),
      cell: ({ getValue }) => {
        const v = getValue() as string | null
        return v ? <span className="text-xs text-muted-foreground">{v}</span> : <span className="text-xs text-muted-foreground">-</span>
      },
    },
    {
      accessorKey: "dest_location",
      header: t("movements.to"),
      cell: ({ getValue }) => {
        const v = getValue() as string | null
        return v ? <span className="text-xs text-muted-foreground">{v}</span> : <span className="text-xs text-muted-foreground">-</span>
      },
    },
    {
      accessorKey: "note",
      header: t("common.notes"),
      cell: ({ getValue }) => {
        const v = getValue() as string
        return v ? <span className="text-xs text-muted-foreground max-w-[200px] truncate block">{v}</span> : null
      },
    },
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
          <h1 className="text-2xl font-bold tracking-tight">{t("movements.title")}</h1>
          <p className="text-sm text-muted-foreground">
            {t("movements.count", { count: movements.length })}
          </p>
        </div>
        <Button onClick={openCreate}>
          <Plus className="size-4" />
          {t("movements.new")}
        </Button>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <div className="relative flex-1 min-w-[200px] max-w-sm">
          <Search className="absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            className="pl-8"
            placeholder={t("movements.search_placeholder")}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <Select value={typeFilter} onValueChange={setTypeFilter}>
          <SelectTrigger className="w-36">
            <SelectValue placeholder={t("movements.type")} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="__all__">{t("common.all")}</SelectItem>
            <SelectItem value="in">{t("movements.in")}</SelectItem>
            <SelectItem value="out">{t("movements.out")}</SelectItem>
            <SelectItem value="transfer">{t("movements.transfers")}</SelectItem>
            <SelectItem value="other">{t("movements.others")}</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {loading ? (
        <div className="space-y-3">
          {Array.from({ length: 8 }).map((_, i) => (
            <Skeleton key={i} className="h-10 w-full rounded-lg" />
          ))}
        </div>
      ) : (
        <Card>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                {table.getHeaderGroups().map((hg) => (
                  <TableRow key={hg.id}>
                    {hg.headers.map((header) => (
                      <TableHead
                        key={header.id}
                        onClick={header.column.getToggleSortingHandler()}
                        className={header.column.getCanSort() ? "cursor-pointer select-none" : ""}
                      >
                        {flexRender(header.column.columnDef.header, header.getContext())}
                        {{ asc: " ▲", desc: " ▼" }[header.column.getIsSorted() as string] ?? ""}
                      </TableHead>
                    ))}
                  </TableRow>
                ))}
              </TableHeader>
              <TableBody>
                {table.getRowModel().rows.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={columns.length} className="text-center py-8 text-muted-foreground">
                      {t("movements.empty")}
                    </TableCell>
                  </TableRow>
                ) : (
                  table.getRowModel().rows.map((row) => (
                    <TableRow key={row.id}>
                      {row.getVisibleCells().map((cell) => (
                        <TableCell key={cell.id}>
                          {flexRender(cell.column.columnDef.cell, cell.getContext())}
                        </TableCell>
                      ))}
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{t("movements.new")}</DialogTitle>
            <DialogDescription>
              {t("movements.dialog.description")}
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-3">
            <div className="space-y-1">
              <label className="text-xs font-medium">{t("movements.form.product")}</label>
              <NativeSelect
                value={String(formData.product_id)}
                onChange={(v) => setFormData({ ...formData, product_id: parseInt(v) })}
                placeholder={t("movements.form.select_product")}
                options={products.map((p) => ({ value: String(p.id), label: `${p.name} (${p.sku}) — Stock: ${p.quantity}` }))}
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="text-xs font-medium">{t("movements.form.type")}</label>
                <NativeSelect
                  value={formData.type}
                  onChange={(v) => setFormData({ ...formData, type: v as "in" | "out" })}
                  options={[{ value: "in", label: t("movements.in") }, { value: "out", label: t("movements.out") }]}
                />
              </div>
              <div className="space-y-1">
                <label className="text-xs font-medium">{t("movements.form.quantity")}</label>
                <Input
                  type="number"
                  min={1}
                  value={formData.quantity}
                  onChange={(e) => setFormData({ ...formData, quantity: parseInt(e.target.value) || 0 })}
                />
              </div>
            </div>
            <div className="space-y-1">
              <label className="text-xs font-medium">{t("common.notes")}</label>
              <Input
                value={formData.note ?? ""}
                onChange={(e) => setFormData({ ...formData, note: e.target.value })}
                placeholder="Optionnelle"
              />
            </div>
          </div>
          <DialogFooter>
            <DialogClose asChild>
              <Button variant="outline">{t("common.cancel")}</Button>
            </DialogClose>
            <Button onClick={handleSave} disabled={saving || !formData.product_id || formData.quantity <= 0}>
              {saving ? t("common.saving") : t("common.save")}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}