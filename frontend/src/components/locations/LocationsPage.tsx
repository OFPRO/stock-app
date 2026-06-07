import { useEffect, useState, useCallback, useMemo } from "react"
import { useTranslation } from "react-i18next"
import { Plus, Pencil, Trash2, Search, MapPin } from "lucide-react"
import { type ColumnDef, type SortingState, flexRender, getCoreRowModel, getSortedRowModel, useReactTable } from "@tanstack/react-table"
import { getLocations, createLocation, updateLocation, deleteLocation, getWarehouses, type Location as StockLocation, type LocationFormData, type Warehouse } from "@/lib/api"
import { NativeSelect } from "@/components/ui/native-select"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter, DialogClose } from "@/components/ui/dialog"
import { Card, CardContent } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"

function LocationForm({ data, onChange }: { data: LocationFormData & { type: string }; onChange: (d: LocationFormData & { type: string }) => void }) {
  const { t } = useTranslation()
  return (
    <div className="grid gap-3">
      <div className="space-y-1"><label className="text-xs font-medium">{t("common.name")} *</label><Input value={data.name} onChange={(e) => onChange({ ...data, name: e.target.value })} /></div>
      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-1"><label className="text-xs font-medium">{t("locations.form.type")}</label><NativeSelect value={data.type} onChange={(v) => onChange({ ...data, type: v })} options={[{ value: "rack", label: t("locations.type.rack") }, { value: "bin", label: t("locations.type.bin") }, { value: "shelf", label: t("locations.type.shelf") }, { value: "drawer", label: t("locations.type.drawer") }, { value: "cold", label: t("locations.type.cold") }, { value: "other", label: t("locations.type.other") }]} /></div>
        <div className="space-y-1"><label className="text-xs font-medium">{t("locations.form.capacity")}</label><Input type="number" value={data.capacity ?? ""} onChange={(e) => onChange({ ...data, capacity: e.target.value ? parseInt(e.target.value) : null })} /></div>
      </div>
    </div>
  )
}

export function LocationsPage() {
  const { t } = useTranslation()
  const [locations, setLocations] = useState<StockLocation[]>([])
  const [warehouses, setWarehouses] = useState<Warehouse[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")
  const [warehouseFilter, setWarehouseFilter] = useState("__all__")
  const [sorting, setSorting] = useState<SortingState>([{ id: "name", desc: false }])
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [formData, setFormData] = useState<LocationFormData & { type: string }>({ warehouse_id: 0, name: "", type: "rack", capacity: null })
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [loc, wh] = await Promise.all([getLocations(), getWarehouses()])
      setLocations(loc)
      setWarehouses(wh)
    } catch (err) { console.error(err) }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { load() }, [load])

  const filtered = useMemo(() => locations.filter(l => {
    if (warehouseFilter !== "__all__" && l.warehouse_id !== parseInt(warehouseFilter)) return false
    if (search) { const q = search.toLowerCase(); return l.name.toLowerCase().includes(q) || (l.warehouse_name ?? "").toLowerCase().includes(q) }
    return true
  }), [locations, warehouseFilter, search])

  const openCreate = () => {
    const firstWh = warehouses[0]
    setEditingId(null)
    setFormData({ warehouse_id: firstWh?.id ?? 0, name: "", type: "rack", capacity: null })
    setDialogOpen(true)
  }

  const openEdit = useCallback((loc: StockLocation) => {
    setEditingId(loc.id)
    setFormData({ warehouse_id: loc.warehouse_id, name: loc.name, type: loc.type, capacity: loc.capacity })
    setDialogOpen(true)
  }, [])

  const handleSave = async () => {
    if (!formData.name || !formData.warehouse_id) return
    setSaving(true)
    try {
      if (editingId) {
        await updateLocation(editingId, formData)
      } else {
        await createLocation(formData)
      }
      setDialogOpen(false)
      load()
    } finally { setSaving(false) }
  }

  const handleDelete = useCallback(async (id: number) => {
    if (!confirm("Supprimer cette zone de stock ?")) return
    const res = await deleteLocation(id)
    if (res.success) load()
  }, [load])

  const columns = useMemo<ColumnDef<StockLocation>[]>(() => [
    { accessorKey: "name", header: () => t("common.name") },
    { accessorKey: "warehouse_name", header: () => t("locations.form.warehouse"), cell: ({ getValue }) => <span>{(getValue() as string) ?? "—"}</span> },
    { accessorKey: "type", header: () => t("locations.type"), cell: ({ getValue }) => { const v = getValue() as string; return <span className="inline-flex items-center rounded-full bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300 px-2 py-0.5 text-xs font-medium">{({ rack: t("locations.type.rack"), bin: t("locations.type.bin"), shelf: t("locations.type.shelf"), drawer: t("locations.type.drawer"), cold: t("locations.type.cold"), other: t("locations.type.other") })[v] ?? v}</span> } },
    { accessorKey: "capacity", header: () => t("locations.capacity"), cell: ({ getValue }) => { const v = getValue() as number | null; return <span className="tabular-nums">{v != null ? v : "—"}</span> } },
    { id: "actions", header: "", cell: ({ row }) => {
      const loc = row.original
      return <div className="flex justify-end gap-1">
        <Button variant="ghost" size="icon-sm" onClick={() => openEdit(loc)}><Pencil className="size-3.5" /></Button>
        <Button variant="ghost" size="icon-sm" onClick={() => handleDelete(loc.id)}><Trash2 className="size-3.5 text-destructive" /></Button>
      </div>
    }},
  ], [openEdit, handleDelete])

  const table = useReactTable({ data: filtered, columns, state: { sorting }, onSortingChange: setSorting, getCoreRowModel: getCoreRowModel(), getSortedRowModel: getSortedRowModel() })

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div><h1 className="text-2xl font-bold tracking-tight flex items-center gap-2"><MapPin className="size-5" />{t("locations.title")}</h1><p className="text-sm text-muted-foreground">{t("locations.count", { count: locations.length })}</p></div>
        <Button onClick={openCreate}><Plus className="size-4" />{t("locations.new")}</Button>
      </div>
      <div className="flex flex-wrap items-center gap-3">
        <div className="relative flex-1 min-w-[200px] max-w-sm"><Search className="absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" /><Input className="pl-8" placeholder={t("common.search")} value={search} onChange={(e) => setSearch(e.target.value)} /></div>
        <Select value={warehouseFilter} onValueChange={setWarehouseFilter}><SelectTrigger className="w-44"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="__all__">{t("common.all")}</SelectItem>{warehouses.map(w => <SelectItem key={w.id} value={String(w.id)}>{w.name}</SelectItem>)}</SelectContent></Select>
      </div>
      {loading ? <div className="space-y-3">{Array.from({ length: 8 }).map((_, i) => <Skeleton key={i} className="h-10 w-full rounded-lg" />)}</div>
      : <Card><CardContent className="p-0"><Table><TableHeader>{table.getHeaderGroups().map(hg => <TableRow key={hg.id}>{hg.headers.map(h => <TableHead key={h.id} onClick={h.column.getToggleSortingHandler()} className={h.column.getCanSort() ? "cursor-pointer select-none" : ""}>{flexRender(h.column.columnDef.header, h.getContext())}{{ asc: " ▲", desc: " ▼" }[h.column.getIsSorted() as string] ?? ""}</TableHead>)}</TableRow>)}</TableHeader><TableBody>{table.getRowModel().rows.length === 0 ? <TableRow><TableCell colSpan={5} className="text-center py-8 text-muted-foreground">{t("locations.empty")}</TableCell></TableRow> : table.getRowModel().rows.map(row => <TableRow key={row.id}>{row.getVisibleCells().map(cell => <TableCell key={cell.id}>{flexRender(cell.column.columnDef.cell, cell.getContext())}</TableCell>)}</TableRow>)}</TableBody></Table></CardContent></Card>}

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader><DialogTitle>{editingId ? t("locations.dialog.title") : t("locations.new")}</DialogTitle><DialogDescription>{t("locations.dialog.description")}</DialogDescription></DialogHeader>
          <div className="grid gap-3">
            <div className="space-y-1"><label className="text-xs font-medium">{t("locations.form.warehouse")}</label><NativeSelect value={String(formData.warehouse_id)} onChange={(v) => setFormData({ ...formData, warehouse_id: parseInt(v) })} placeholder={t("common.select")} options={warehouses.map(w => ({ value: String(w.id), label: w.name }))} /></div>
            <LocationForm data={formData} onChange={setFormData} />
          </div>
          <DialogFooter><DialogClose asChild><Button variant="outline">{t("common.cancel")}</Button></DialogClose><Button onClick={handleSave} disabled={saving || !formData.name || !formData.warehouse_id}>{saving ? t("common.saving") : editingId ? t("common.save") : t("common.create")}</Button></DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}