import { useEffect, useState, useCallback, useMemo } from "react"
import { useTranslation } from "react-i18next"
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
  type Category,
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
  const { t } = useTranslation()
  return (
    <div className="grid gap-3">
      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-1">
          <label className="text-xs font-medium">{t("products.form.name")}</label>
          <Input
            value={data.name}
            onChange={(e) => onChange({ ...data, name: e.target.value })}
            placeholder={t("products.form.name_placeholder")}
          />
        </div>
        <div className="space-y-1">
          <label className="text-xs font-medium">{t("products.sku")}</label>
          <Input
            value={data.sku ?? ""}
            onChange={(e) => onChange({ ...data, sku: e.target.value })}
            placeholder={t("products.form.sku_placeholder")}
          />
        </div>
      </div>
      <div className="space-y-1">
        <label className="text-xs font-medium">{t("products.form.description")}</label>
        <Input
          value={data.description ?? ""}
          onChange={(e) => onChange({ ...data, description: e.target.value })}
          placeholder={t("products.form.description_placeholder")}
        />
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-1">
          <label className="text-xs font-medium">{t("products.category")}</label>
          <NativeSelect
            value={data.category ?? ""}
            onChange={(v) => onChange({ ...data, category: v || undefined })}
            placeholder={t("common.select")}
            options={categories.map((c) => ({ value: c.name_fr, label: c.name_ar + ' / ' + c.name_fr }))}
          />
        </div>
        <div className="space-y-1">
          <label className="text-xs font-medium">{t("products.form.tax")}</label>
          <NativeSelect
            value={data.tax_category ?? "20"}
            onChange={(v) => onChange({ ...data, tax_category: v })}
            options={[
              { value: "20", label: t("products.tax.20") },
              { value: "14", label: t("products.tax.14") },
              { value: "10", label: t("products.tax.10") },
              { value: "7", label: t("products.tax.7") },
            ]}
          />
        </div>
      </div>
      <div className="grid grid-cols-3 gap-3">
        <div className="space-y-1">
          <label className="text-xs font-medium">{t("products.price")}</label>
          <Input
            type="number"
            step="0.01"
            value={data.price ?? 0}
            onChange={(e) => onChange({ ...data, price: parseFloat(e.target.value) || 0 })}
          />
        </div>
        <div className="space-y-1">
          <label className="text-xs font-medium">{t("products.form.min_qty")}</label>
          <Input
            type="number"
            value={data.min_quantity ?? 5}
            onChange={(e) => onChange({ ...data, min_quantity: parseInt(e.target.value) || 0 })}
          />
        </div>
        <div className="space-y-1">
          <label className="text-xs font-medium">{t("products.form.max_qty")}</label>
          <Input
            type="number"
            value={data.max_quantity ?? 100}
            onChange={(e) => onChange({ ...data, max_quantity: parseInt(e.target.value) || 0 })}
          />
        </div>
      </div>
      <div className="grid grid-cols-3 gap-3">
        <div className="space-y-1">
          <label className="text-xs font-medium">{t("products.form.price_normal")}</label>
          <Input type="number" step="0.01" value={data.price_base ?? data.price ?? 0}
            onChange={(e) => onChange({ ...data, price_base: parseFloat(e.target.value) || 0 })} />
        </div>
        <div className="space-y-1">
          <label className="text-xs font-medium">{t("products.form.price_loyal")}</label>
          <Input type="number" step="0.01" value={data.price_loyal ?? 0}
            onChange={(e) => onChange({ ...data, price_loyal: parseFloat(e.target.value) || 0 })} />
        </div>
        <div className="space-y-1">
          <label className="text-xs font-medium">{t("products.form.price_bulk")}</label>
          <Input type="number" step="0.01" value={data.price_gros ?? 0}
            onChange={(e) => onChange({ ...data, price_gros: parseFloat(e.target.value) || 0 })} />
        </div>
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-1">
          <label className="text-xs font-medium">{t("products.warehouse")}</label>
          <NativeSelect
            value={String(data.warehouse_id ?? 1)}
            onChange={(v) => onChange({ ...data, warehouse_id: parseInt(v) })}
            options={warehouses.map((w) => ({ value: String(w.id), label: w.name }))}
          />
        </div>
        <div className="space-y-1">
          <label className="text-xs font-medium">{t("products.barcode")}</label>
          <Input
            value={data.barcode ?? ""}
            onChange={(e) => onChange({ ...data, barcode: e.target.value })}
            placeholder={t("common.optional")}
          />
        </div>
      </div>
    </div>
  )
}

function StockBadge({ quantity, minQuantity }: { quantity: number; minQuantity: number }) {
  const { t } = useTranslation()
  if (quantity <= 0) {
    return <span className="inline-flex items-center rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700 dark:bg-red-900/30 dark:text-red-400">{t("products.stock_badge.out_of_stock")}</span>
  }
  if (quantity <= minQuantity) {
    return <span className="inline-flex items-center rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700 dark:bg-amber-900/30 dark:text-amber-400">{t("products.stock_badge.low_stock")}</span>
  }
  return <span className="inline-flex items-center rounded-full bg-emerald-100 px-2 py-0.5 text-xs font-medium text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400">{t("products.stock_badge.ok")}</span>
}

export function ProductsPage() {
  const { t } = useTranslation()
  const [products, setProducts] = useState<Product[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")
  const [categoryFilter, setCategoryFilter] = useState("__all__")
  const [categories, setCategories] = useState<Category[]>([])
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
        price_loyal: p.price_loyal,
        price_gros: p.price_gros,
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
      header: t("products.name"),
      cell: ({ row }) => (
        <div className="flex items-center gap-2">
          <Package className="size-3.5 shrink-0 text-muted-foreground" />
          <span className="font-medium">{row.original.name}</span>
        </div>
      ),
    },
    {
      accessorKey: "sku",
      header: t("products.sku"),
      cell: ({ getValue }) => (
        <span className="text-muted-foreground font-mono text-xs">{getValue() as string}</span>
      ),
    },
    {
      accessorKey: "category",
      header: t("products.category"),
      cell: ({ row }) => {
        const cat = row.original.category
        const catAr = row.original.category_ar
        return cat ? <span className="text-xs">{catAr ? catAr + ' / ' : ''}{cat}</span> : null
      },
    },
    {
      accessorKey: "quantity",
      header: t("products.stock"),
      cell: ({ row }) => (
        <div className="flex items-center gap-2">
          <span className="tabular-nums">{row.original.quantity}</span>
          <StockBadge quantity={row.original.quantity} minQuantity={row.original.min_quantity} />
        </div>
      ),
    },
    {
      accessorKey: "price",
      header: t("products.price"),
      cell: ({ getValue }) => (
        <span className="tabular-nums">{(getValue() as number).toFixed(2)} DH</span>
      ),
    },
    {
      accessorKey: "warehouse_name",
      header: t("products.warehouse"),
      cell: ({ getValue }) => {
        const v = getValue() as string | null
        return v ? <span className="text-xs text-muted-foreground">{v}</span> : null
      },
    },
    {
      accessorKey: "supplier_name",
      header: t("products.supplier"),
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
          <h1 className="text-2xl font-bold tracking-tight">{t("products.title")}</h1>
          <p className="text-sm text-muted-foreground">
            {products.length} {t("products.count", { count: products.length })} {t("products.registered", { count: products.length })}
          </p>
        </div>
        <Button onClick={openCreate}>
          <Plus className="size-4" />
          {t("products.new")}
        </Button>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <div className="relative flex-1 min-w-[200px] max-w-sm">
          <Search className="absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            className="pl-8"
            placeholder={t("products.search_placeholder")}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <Select value={categoryFilter} onValueChange={setCategoryFilter}>
          <SelectTrigger className="w-40">
            <SelectValue placeholder={t("products.filter_category")} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="__all__">{t("products.all_categories")}</SelectItem>
            {categories.map((c) => (
              <SelectItem key={c.id} value={c.name_fr}>{c.name_ar} / {c.name_fr}</SelectItem>
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
                      {t("products.empty.no_results")}
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
            <DialogTitle>{editingId ? t("products.dialog.edit_title") : t("products.dialog.create_title")}</DialogTitle>
            <DialogDescription>
              {editingId ? t("products.dialog.edit_description") : t("products.dialog.create_description")}
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
              <Button variant="outline">{t("common.cancel")}</Button>
            </DialogClose>
            <Button onClick={handleSave} disabled={saving || !formData.name.trim()}>
              {saving ? t("common.saving") : t("common.save")}
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