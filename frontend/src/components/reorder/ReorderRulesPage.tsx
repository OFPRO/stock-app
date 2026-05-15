import { useEffect, useState, useCallback, useMemo } from "react"
import { Plus, Pencil, Trash2, Search } from "lucide-react"
import {
  type ColumnDef,
  type SortingState,
  flexRender,
  getCoreRowModel,
  getSortedRowModel,
  useReactTable,
} from "@tanstack/react-table"
import {
  getReorderRules,
  createReorderRule,
  updateReorderRule,
  deleteReorderRule,
  getProducts,
  getSuppliers,
  getWarehouses,
  type ReorderRule,
  type ReorderRuleFormData,
  type Product,
  type Supplier,
  type Warehouse,
} from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { NativeSelect } from "@/components/ui/native-select"
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

const TRIGGER_LABELS: Record<string, string> = { manual: "Manuel", auto: "Automatique" }

function ReorderRuleForm({
  data,
  onChange,
  products,
  suppliers,
  warehouses,
}: {
  data: ReorderRuleFormData
  onChange: (d: ReorderRuleFormData) => void
  products: Product[]
  suppliers: Supplier[]
  warehouses: Warehouse[]
}) {
  return (
    <div className="grid gap-3">
      <div className="space-y-1">
        <label className="text-xs font-medium">Produit *</label>
        <NativeSelect
          value={String(data.product_id)}
          onChange={(v) => onChange({ ...data, product_id: parseInt(v) })}
          placeholder="Sélectionner un produit"
          options={products.map((p) => ({ value: String(p.id), label: `${p.name} (${p.sku})` }))}
        />
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-1">
          <label className="text-xs font-medium">Entrepôt</label>
          <NativeSelect
            value={String(data.warehouse_id ?? 1)}
            onChange={(v) => onChange({ ...data, warehouse_id: parseInt(v) })}
            options={warehouses.map((w) => ({ value: String(w.id), label: w.name }))}
          />
        </div>
        <div className="space-y-1">
          <label className="text-xs font-medium">Type de déclenchement</label>
          <NativeSelect
            value={data.trigger_type ?? "manual"}
            onChange={(v) => onChange({ ...data, trigger_type: v })}
            options={[
              { value: "manual", label: "Manuel" },
              { value: "auto", label: "Automatique" },
            ]}
          />
        </div>
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-1">
          <label className="text-xs font-medium">Quantité minimale</label>
          <Input
            type="number"
            min={0}
            value={data.min_quantity ?? 5}
            onChange={(e) => onChange({ ...data, min_quantity: parseInt(e.target.value) || 0 })}
          />
        </div>
        <div className="space-y-1">
          <label className="text-xs font-medium">Quantité maximale</label>
          <Input
            type="number"
            min={0}
            value={data.max_quantity ?? 100}
            onChange={(e) => onChange({ ...data, max_quantity: parseInt(e.target.value) || 0 })}
          />
        </div>
      </div>
      <div className="space-y-1">
        <label className="text-xs font-medium">Fournisseur</label>
        <NativeSelect
          value={String(data.supplier_id ?? "")}
          onChange={(v) => onChange({ ...data, supplier_id: v ? parseInt(v) : null })}
          placeholder="Aucun"
          options={suppliers.map((s) => ({ value: String(s.id), label: s.name }))}
        />
      </div>
    </div>
  )
}

export function ReorderRulesPage() {
  const [rules, setRules] = useState<ReorderRule[]>([])
  const [products, setProducts] = useState<Product[]>([])
  const [suppliers, setSuppliers] = useState<Supplier[]>([])
  const [warehouses, setWarehouses] = useState<Warehouse[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")
  const [warehouseFilter, setWarehouseFilter] = useState("__all__")
  const [sorting, setSorting] = useState<SortingState>([{ id: "product_name", desc: false }])
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [formData, setFormData] = useState<ReorderRuleFormData>({ product_id: 0 })
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const wh = warehouseFilter !== "__all__" ? warehouseFilter : undefined
      const [r, p, s, w] = await Promise.all([
        getReorderRules(wh),
        getProducts(),
        getSuppliers(),
        getWarehouses(),
      ])
      setRules(r); setProducts(p); setSuppliers(s); setWarehouses(w)
    } catch (err) {
      console.error("Failed to load reorder rules:", err)
    } finally {
      setLoading(false)
    }
  }, [warehouseFilter])

  useEffect(() => { load() }, [load])

  const filtered = useMemo(() => {
    if (!search) return rules
    const q = search.toLowerCase()
    return rules.filter((r) =>
      r.product_name.toLowerCase().includes(q) ||
      (r.supplier_name ?? "").toLowerCase().includes(q)
    )
  }, [rules, search])

  const openCreate = () => {
    setEditingId(null)
    setFormData({ product_id: 0, warehouse_id: 1, min_quantity: 5, max_quantity: 100, trigger_type: "manual", supplier_id: null })
    setDialogOpen(true)
  }

  const openEdit = useCallback((rule: ReorderRule) => {
    setEditingId(rule.id)
    setFormData({
      product_id: rule.product_id,
      warehouse_id: rule.warehouse_id,
      min_quantity: rule.min_quantity,
      max_quantity: rule.max_quantity,
      trigger_type: rule.trigger_type,
      supplier_id: rule.supplier_id,
    })
    setDialogOpen(true)
  }, [])

  const handleSave = async () => {
    if (!formData.product_id) return
    setSaving(true)
    try {
      if (editingId) {
        const res = await updateReorderRule(editingId, formData)
        if (res.success) { setDialogOpen(false); load() }
        else { console.error("Update failed:", res.error) }
      } else {
        const res = await createReorderRule(formData)
        if (res.success) { setDialogOpen(false); load() }
        else { console.error("Create failed:", res.error) }
      }
    } catch (err) {
      console.error("Save failed:", err)
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = useCallback(async (id: number) => {
    try {
      const res = await deleteReorderRule(id)
      if (res.success) load()
      else console.error("Delete failed:", res.error)
    } catch (err) {
      console.error("Delete failed:", err)
    }
  }, [load])

  const columns = useMemo<ColumnDef<ReorderRule>[]>(() => [
    { accessorKey: "product_name", header: "Produit", cell: ({ getValue }) => <span className="font-medium">{getValue() as string}</span> },
    {
      accessorKey: "current_qty",
      header: "Stock",
      cell: ({ getValue, row }) => {
        const qty = getValue() as number
        const min = row.original.min_quantity
        const low = qty <= min
        return <span className={`tabular-nums ${low ? "text-amber-600 font-medium" : ""}`}>{qty}</span>
      },
    },
    { accessorKey: "min_quantity", header: "Min", cell: ({ getValue }) => <span className="tabular-nums">{getValue() as number}</span> },
    { accessorKey: "max_quantity", header: "Max", cell: ({ getValue }) => <span className="tabular-nums">{getValue() as number}</span> },
    { accessorKey: "trigger_type", header: "Déclenchement", cell: ({ getValue }) => { const v = getValue() as string; return <span className="text-xs">{TRIGGER_LABELS[v] ?? v}</span> } },
    { accessorKey: "supplier_name", header: "Fournisseur", cell: ({ getValue }) => { const v = getValue() as string | null; return v ? <span className="text-xs text-muted-foreground">{v}</span> : <span className="text-xs text-muted-foreground">—</span> } },
    {
      id: "actions",
      header: "",
      cell: ({ row }) => (
        <div className="flex items-center justify-end gap-1">
          <Button variant="ghost" size="icon-sm" onClick={() => openEdit(row.original)}>
            <Pencil className="size-3.5" />
          </Button>
          <Button variant="ghost" size="icon-sm" onClick={() => handleDelete(row.original.id)}>
            <Trash2 className="size-3.5 text-destructive" />
          </Button>
        </div>
      ),
    },
  ], [openEdit, handleDelete])

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
          <h1 className="text-2xl font-bold tracking-tight">Règles de Réapprovisionnement</h1>
          <p className="text-sm text-muted-foreground">{rules.length} règle{rules.length !== 1 ? "s" : ""}</p>
        </div>
        <Button onClick={openCreate}><Plus className="size-4" />Nouvelle règle</Button>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <div className="relative flex-1 min-w-[200px] max-w-sm">
          <Search className="absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input className="pl-8" placeholder="Rechercher..." value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
        <Select value={warehouseFilter} onValueChange={setWarehouseFilter}>
          <SelectTrigger className="w-40"><SelectValue /></SelectTrigger>
          <SelectContent>
            <SelectItem value="__all__">Tous les entrepôts</SelectItem>
            {warehouses.map((w) => (
              <SelectItem key={w.id} value={String(w.id)}>{w.name}</SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

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
                  <TableRow><TableCell colSpan={columns.length} className="text-center py-8 text-muted-foreground">Aucune règle trouvée</TableCell></TableRow>
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

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{editingId ? "Modifier la règle" : "Nouvelle règle"}</DialogTitle>
            <DialogDescription>Définissez les paramètres de réapprovisionnement.</DialogDescription>
          </DialogHeader>
          <ReorderRuleForm data={formData} onChange={setFormData} products={products} suppliers={suppliers} warehouses={warehouses} />
          <DialogFooter>
            <DialogClose asChild><Button variant="outline">Annuler</Button></DialogClose>
            <Button onClick={handleSave} disabled={saving || !formData.product_id}>
              {saving ? "Enregistrement..." : "Enregistrer"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
