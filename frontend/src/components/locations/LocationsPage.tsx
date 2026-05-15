import { useEffect, useState, useCallback, useMemo } from "react"
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

const TYPE_LABELS: Record<string, string> = { rack: "Rayonnage", bin: "Bac", shelf: "Étagère", drawer: "Tiroir", cold: "Chambre froide", other: "Autre" }

function LocationForm({ data, onChange }: { data: LocationFormData & { type: string }; onChange: (d: LocationFormData & { type: string }) => void }) {
  return (
    <div className="grid gap-3">
      <div className="space-y-1"><label className="text-xs font-medium">Nom *</label><Input value={data.name} onChange={(e) => onChange({ ...data, name: e.target.value })} /></div>
      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-1"><label className="text-xs font-medium">Type</label><NativeSelect value={data.type} onChange={(v) => onChange({ ...data, type: v })} options={[{ value: "rack", label: "Rayonnage" }, { value: "bin", label: "Bac" }, { value: "shelf", label: "Étagère" }, { value: "drawer", label: "Tiroir" }, { value: "cold", label: "Chambre froide" }, { value: "other", label: "Autre" }]} /></div>
        <div className="space-y-1"><label className="text-xs font-medium">Capacité max</label><Input type="number" value={data.capacity ?? ""} onChange={(e) => onChange({ ...data, capacity: e.target.value ? parseInt(e.target.value) : null })} /></div>
      </div>
    </div>
  )
}

export function LocationsPage() {
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
    { accessorKey: "name", header: "Nom" },
    { accessorKey: "warehouse_name", header: "Entrepôt", cell: ({ getValue }) => <span>{(getValue() as string) ?? "—"}</span> },
    { accessorKey: "type", header: "Type", cell: ({ getValue }) => { const v = getValue() as string; return <span className="inline-flex items-center rounded-full bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300 px-2 py-0.5 text-xs font-medium">{TYPE_LABELS[v] ?? v}</span> } },
    { accessorKey: "capacity", header: "Capacité", cell: ({ getValue }) => { const v = getValue() as number | null; return <span className="tabular-nums">{v != null ? v : "—"}</span> } },
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
        <div><h1 className="text-2xl font-bold tracking-tight flex items-center gap-2"><MapPin className="size-5" />Zones de Stock</h1><p className="text-sm text-muted-foreground">{locations.length} zone{locations.length !== 1 ? "s" : ""}</p></div>
        <Button onClick={openCreate}><Plus className="size-4" />Nouvelle zone</Button>
      </div>
      <div className="flex flex-wrap items-center gap-3">
        <div className="relative flex-1 min-w-[200px] max-w-sm"><Search className="absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" /><Input className="pl-8" placeholder="Rechercher..." value={search} onChange={(e) => setSearch(e.target.value)} /></div>
        <Select value={warehouseFilter} onValueChange={setWarehouseFilter}><SelectTrigger className="w-44"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="__all__">Tous les entrepôts</SelectItem>{warehouses.map(w => <SelectItem key={w.id} value={String(w.id)}>{w.name}</SelectItem>)}</SelectContent></Select>
      </div>
      {loading ? <div className="space-y-3">{Array.from({ length: 8 }).map((_, i) => <Skeleton key={i} className="h-10 w-full rounded-lg" />)}</div>
      : <Card><CardContent className="p-0"><Table><TableHeader>{table.getHeaderGroups().map(hg => <TableRow key={hg.id}>{hg.headers.map(h => <TableHead key={h.id} onClick={h.column.getToggleSortingHandler()} className={h.column.getCanSort() ? "cursor-pointer select-none" : ""}>{flexRender(h.column.columnDef.header, h.getContext())}{{ asc: " ▲", desc: " ▼" }[h.column.getIsSorted() as string] ?? ""}</TableHead>)}</TableRow>)}</TableHeader><TableBody>{table.getRowModel().rows.length === 0 ? <TableRow><TableCell colSpan={5} className="text-center py-8 text-muted-foreground">Aucune zone de stock</TableCell></TableRow> : table.getRowModel().rows.map(row => <TableRow key={row.id}>{row.getVisibleCells().map(cell => <TableCell key={cell.id}>{flexRender(cell.column.columnDef.cell, cell.getContext())}</TableCell>)}</TableRow>)}</TableBody></Table></CardContent></Card>}

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader><DialogTitle>{editingId ? "Modifier la zone" : "Nouvelle zone de stock"}</DialogTitle><DialogDescription>{editingId ? "Modifier les informations de la zone." : "Ajouter une nouvelle zone de stock à un entrepôt."}</DialogDescription></DialogHeader>
          <div className="grid gap-3">
            <div className="space-y-1"><label className="text-xs font-medium">Entrepôt *</label><NativeSelect value={String(formData.warehouse_id)} onChange={(v) => setFormData({ ...formData, warehouse_id: parseInt(v) })} placeholder="Sélectionner" options={warehouses.map(w => ({ value: String(w.id), label: w.name }))} /></div>
            <LocationForm data={formData} onChange={setFormData} />
          </div>
          <DialogFooter><DialogClose asChild><Button variant="outline">Annuler</Button></DialogClose><Button onClick={handleSave} disabled={saving || !formData.name || !formData.warehouse_id}>{saving ? "..." : editingId ? "Enregistrer" : "Créer"}</Button></DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}