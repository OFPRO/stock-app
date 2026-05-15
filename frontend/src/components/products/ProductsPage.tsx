import { useEffect, useState, useCallback, useMemo } from "react"
import { Plus, Pencil, Trash2, Search, Package, Eye } from "lucide-react"
import {
  type ColumnDef,
  type SortingState,
  flexRender,
  getCoreRowModel,
  getSortedRowModel,
  useReactTable,
} from "@tanstack/react-table"
import {
  getProducts,
  getProduct,
  createProduct,
  updateProduct,
  deleteProduct,
  getCategories,
  getWarehouses,
  type Product,
  type ProductFormData,
  type Warehouse,
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
import { ProductDetailDialog } from "@/components/products/ProductDetailDialog"

function ProductForm({
  data,
  onChange,
  categories,
  warehouses,
}: {
  data: ProductFormData
  onChange: (d: ProductFormData) => void
  categories: string[]
  warehouses: Warehouse[]
}) {
  return (
    <div className="grid gap-3">
      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-1">
          <label className="text-xs font-medium">Nom *</label>
          <Input
            value={data.name}
            onChange={(e) => onChange({ ...data, name: e.target.value })}
            placeholder="Nom du produit"
          />
        </div>
        <div className="space-y-1">
          <label className="text-xs font-medium">SKU</label>
          <Input
            value={data.sku ?? ""}
            onChange={(e) => onChange({ ...data, sku: e.target.value })}
            placeholder="Auto-généré si vide"
          />
        </div>
      </div>
      <div className="space-y-1">
        <label className="text-xs font-medium">Description</label>
        <Input
          value={data.description ?? ""}
          onChange={(e) => onChange({ ...data, description: e.target.value })}
          placeholder="Description optionnelle"
        />
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-1">
          <label className="text-xs font-medium">Catégorie</label>
          <NativeSelect
            value={data.category ?? ""}
            onChange={(v) => onChange({ ...data, category: v || undefined })}
            placeholder="Sélectionner"
            options={categories.map((c) => ({ value: c, label: c }))}
          />
        </div>
        <div className="space-y-1">
          <label className="text-xs font-medium">Taxe</label>
          <NativeSelect
            value={data.tax_category ?? "20"}
            onChange={(v) => onChange({ ...data, tax_category: v })}
            options={[
              { value: "20", label: "20%" },
              { value: "14", label: "14%" },
              { value: "10", label: "10%" },
              { value: "7", label: "7%" },
            ]}
          />
        </div>
      </div>
      <div className="grid grid-cols-3 gap-3">
        <div className="space-y-1">
          <label className="text-xs font-medium">Prix</label>
          <Input
            type="number"
            step="0.01"
            value={data.price ?? 0}
            onChange={(e) => onChange({ ...data, price: parseFloat(e.target.value) || 0 })}
          />
        </div>
        <div className="space-y-1">
          <label className="text-xs font-medium">Qté Min</label>
          <Input
            type="number"
            value={data.min_quantity ?? 5}
            onChange={(e) => onChange({ ...data, min_quantity: parseInt(e.target.value) || 0 })}
          />
        </div>
        <div className="space-y-1">
          <label className="text-xs font-medium">Qté Max</label>
          <Input
            type="number"
            value={data.max_quantity ?? 100}
            onChange={(e) => onChange({ ...data, max_quantity: parseInt(e.target.value) || 0 })}
          />
        </div>
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
          <label className="text-xs font-medium">Code-barres</label>
          <Input
            value={data.barcode ?? ""}
            onChange={(e) => onChange({ ...data, barcode: e.target.value })}
            placeholder="Optionnel"
          />
        </div>
      </div>
    </div>
  )
}

function StockBadge({ quantity, minQuantity }: { quantity: number; minQuantity: number }) {
  if (quantity <= 0) {
    return <span className="inline-flex items-center rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700 dark:bg-red-900/30 dark:text-red-400">Rupture</span>
  }
  if (quantity <= minQuantity) {
    return <span className="inline-flex items-center rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700 dark:bg-amber-900/30 dark:text-amber-400">Stock bas</span>
  }
  return <span className="inline-flex items-center rounded-full bg-emerald-100 px-2 py-0.5 text-xs font-medium text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400">OK</span>
}

export function ProductsPage() {
  const [products, setProducts] = useState<Product[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")
  const [categoryFilter, setCategoryFilter] = useState("__all__")
  const [categories, setCategories] = useState<string[]>([])
  const [warehouses, setWarehouses] = useState<Warehouse[]>([])
  const [sorting, setSorting] = useState<SortingState>([{ id: "name", desc: false }])
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [formData, setFormData] = useState<ProductFormData>({ name: "" })
  const [saving, setSaving] = useState(false)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [detailProductId, setDetailProductId] = useState<number | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [productsData, categoriesData, warehousesData] = await Promise.all([
        getProducts(),
        getCategories(),
        getWarehouses(),
      ])
      setProducts(productsData)
      setCategories(categoriesData)
      setWarehouses(warehousesData)
    } catch (err) {
      console.error("Failed to load products:", err)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  const filtered = useMemo(() => products.filter((p) => {
    if (categoryFilter !== "__all__" && p.category !== categoryFilter) return false
    if (search) {
      const q = search.toLowerCase()
      if (!p.name.toLowerCase().includes(q) && !p.sku.toLowerCase().includes(q) && !(p.barcode || "").toLowerCase().includes(q)) {
        return false
      }
    }
    return true
  }), [products, categoryFilter, search])

  const openCreate = () => {
    setEditingId(null)
    setFormData({ name: "", warehouse_id: 1, min_quantity: 5, max_quantity: 100, price: 0, tax_category: "20" })
    setDialogOpen(true)
  }

  const openEdit = useCallback(async (id: number) => {
    setSaving(true)
    try {
      const detail = await getProduct(id)
      const p = detail.product
      setEditingId(id)
      setFormData({
        name: p.name,
        description: p.description,
        sku: p.sku,
        barcode: p.barcode,
        quantity: p.quantity,
        min_quantity: p.min_quantity,
        max_quantity: p.max_quantity,
        price: p.price,
        price_base: p.price_base,
        category: p.category,
        tax_category: p.tax_category,
        warehouse_id: p.warehouse_id,
        location_id: p.location_id,
        supplier_id: p.supplier_id,
      })
      setDialogOpen(true)
    } catch (err) {
      console.error("Failed to load product:", err)
    } finally {
      setSaving(false)
    }
  }, [])

  const handleSave = async () => {
    if (!formData.name.trim()) return
    setSaving(true)
    try {
      if (editingId) {
        const res = await updateProduct(editingId, formData)
        if (res.success) {
          setDialogOpen(false)
          load()
        } else {
          console.error("Update failed:", res.error)
        }
      } else {
        const res = await createProduct(formData)
        if (res.success) {
          setDialogOpen(false)
          load()
        } else {
          console.error("Create failed:", res.error)
        }
      }
    } catch (err) {
      console.error("Save failed:", err)
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = useCallback(async (id: number) => {
    setDeletingId(id)
    try {
      const res = await deleteProduct(id)
      if (res.success) {
        load()
      } else {
        console.error("Delete failed:", res.error)
      }
    } catch (err) {
      console.error("Delete failed:", err)
    } finally {
      setDeletingId(null)
    }
  }, [load])

  const openDetail = useCallback((id: number) => {
    setDetailProductId(id)
  }, [])

  const columns = useMemo<ColumnDef<Product>[]>(() => [
    {
      accessorKey: "name",
      header: "Nom",
      cell: ({ row }) => (
        <div className="flex items-center gap-2">
          <Package className="size-3.5 shrink-0 text-muted-foreground" />
          <span className="font-medium">{row.original.name}</span>
        </div>
      ),
    },
    {
      accessorKey: "sku",
      header: "SKU",
      cell: ({ getValue }) => (
        <span className="text-muted-foreground font-mono text-xs">{getValue() as string}</span>
      ),
    },
    {
      accessorKey: "category",
      header: "Catégorie",
      cell: ({ getValue }) => {
        const v = getValue() as string
        return v ? <span className="text-xs">{v}</span> : null
      },
    },
    {
      accessorKey: "quantity",
      header: "Stock",
      cell: ({ row }) => (
        <div className="flex items-center gap-2">
          <span className="tabular-nums">{row.original.quantity}</span>
          <StockBadge quantity={row.original.quantity} minQuantity={row.original.min_quantity} />
        </div>
      ),
    },
    {
      accessorKey: "price",
      header: "Prix",
      cell: ({ getValue }) => (
        <span className="tabular-nums">{(getValue() as number).toFixed(2)} DH</span>
      ),
    },
    {
      accessorKey: "warehouse_name",
      header: "Entrepôt",
      cell: ({ getValue }) => {
        const v = getValue() as string | null
        return v ? <span className="text-xs text-muted-foreground">{v}</span> : null
      },
    },
    {
      accessorKey: "supplier_name",
      header: "Fournisseur",
      cell: ({ getValue }) => {
        const v = getValue() as string | null
        return v ? <span className="text-xs text-muted-foreground">{v}</span> : null
      },
    },
    {
      id: "actions",
      header: "",
      cell: ({ row }) => (
        <div className="flex items-center justify-end gap-1">
          <Button variant="ghost" size="icon-sm" onClick={() => openDetail(row.original.id)}>
            <Eye className="size-3.5" />
          </Button>
          <Button variant="ghost" size="icon-sm" onClick={() => openEdit(row.original.id)}>
            <Pencil className="size-3.5" />
          </Button>
          <Button
            variant="ghost"
            size="icon-sm"
            onClick={() => handleDelete(row.original.id)}
            disabled={deletingId === row.original.id}
          >
            <Trash2 className="size-3.5 text-destructive" />
          </Button>
        </div>
      ),
    },
  ], [openEdit, handleDelete, openDetail])

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
          <h1 className="text-2xl font-bold tracking-tight">Produits</h1>
          <p className="text-sm text-muted-foreground">
            {products.length} produit{products.length !== 1 ? "s" : ""} enregistré{products.length !== 1 ? "s" : ""}
          </p>
        </div>
        <Button onClick={openCreate}>
          <Plus className="size-4" />
          Nouveau produit
        </Button>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <div className="relative flex-1 min-w-[200px] max-w-sm">
          <Search className="absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            className="pl-8"
            placeholder="Rechercher par nom, SKU ou code-barres..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <Select value={categoryFilter} onValueChange={setCategoryFilter}>
          <SelectTrigger className="w-40">
            <SelectValue placeholder="Catégorie" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="__all__">Toutes</SelectItem>
            {categories.map((c) => (
              <SelectItem key={c} value={c}>{c}</SelectItem>
            ))}
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
                      Aucun produit trouvé
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
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>{editingId ? "Modifier le produit" : "Nouveau produit"}</DialogTitle>
            <DialogDescription>
              {editingId ? "Modifiez les champs ci-dessous." : "Remplissez les informations du nouveau produit."}
            </DialogDescription>
          </DialogHeader>
          <ProductForm
            data={formData}
            onChange={setFormData}
            categories={categories}
            warehouses={warehouses}
          />
          <DialogFooter>
            <DialogClose asChild>
              <Button variant="outline">Annuler</Button>
            </DialogClose>
            <Button onClick={handleSave} disabled={saving || !formData.name.trim()}>
              {saving ? "Enregistrement..." : "Enregistrer"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ProductDetailDialog
        productId={detailProductId}
        open={detailProductId !== null}
        onOpenChange={(open) => { if (!open) setDetailProductId(null) }}
      />
    </div>
  )
}