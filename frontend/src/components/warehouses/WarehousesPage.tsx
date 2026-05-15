import { useEffect, useState, useCallback, useMemo } from "react"
import { Plus, Pencil, Trash2, Search, Warehouse } from "lucide-react"
import { type ColumnDef, type SortingState, flexRender, getCoreRowModel, getSortedRowModel, useReactTable } from "@tanstack/react-table"
import { getWarehouses, type Warehouse as WarehouseType } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter, DialogClose } from "@/components/ui/dialog"
import { Card, CardContent } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"

export function WarehousesPage() {
  const [warehouses, setWarehouses] = useState<WarehouseType[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")
  const [sorting, setSorting] = useState<SortingState>([{ id: "is_default", desc: true }])
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editData, setEditData] = useState<{ id?: number; name: string; address: string; manager: string }>({ name: "", address: "", manager: "" })
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try { setWarehouses(await getWarehouses()) }
    catch (err) { console.error(err) }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { load() }, [load])

  const filtered = useMemo(() => warehouses.filter(w => !search || w.name.toLowerCase().includes(search.toLowerCase())), [warehouses, search])

  const openCreate = () => { setEditData({ name: "", address: "", manager: "" }); setDialogOpen(true) }
  const openEdit = useCallback((w: WarehouseType) => { setEditData({ id: w.id, name: w.name, address: w.address, manager: w.manager }); setDialogOpen(true) }, [])

  const handleSave = async () => {
    if (!editData.name.trim()) return
    setSaving(true)
    try {
      const res = editData.id
        ? await (await fetch("/api/warehouses/" + editData.id, { method: "PUT", headers: { "Content-Type": "application/json" }, body: JSON.stringify(editData) })).json()
        : await (await fetch("/api/warehouses", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(editData) })).json()
      if (res.success) { setDialogOpen(false); load() }
    } finally { setSaving(false) }
  }

  const handleDelete = useCallback(async (id: number) => {
    try {
      const res = await (await fetch("/api/warehouses/" + id, { method: "DELETE" })).json()
      if (res.success) load()
    } catch (err) { console.error(err) }
  }, [load])

  const columns = useMemo<ColumnDef<WarehouseType>[]>(() => [
    { accessorKey: "name", header: "Nom", cell: ({ row }) => <div className="flex items-center gap-2"><Warehouse className="size-3.5 text-muted-foreground" /><span className="font-medium">{row.original.name}</span></div> },
    { accessorKey: "address", header: "Adresse", cell: ({ getValue }) => { const v = getValue() as string; return v ? <span className="text-xs text-muted-foreground">{v}</span> : null } },
    { accessorKey: "manager", header: "Responsable", cell: ({ getValue }) => { const v = getValue() as string; return v ? <span className="text-xs text-muted-foreground">{v}</span> : null } },
    { accessorKey: "is_default", header: "Défaut", cell: ({ getValue }) => getValue() ? <span className="text-xs font-medium text-emerald-600">Oui</span> : null },
    { id: "actions", header: "", cell: ({ row }) => <div className="flex justify-end gap-1"><Button variant="ghost" size="icon-sm" onClick={() => openEdit(row.original)}><Pencil className="size-3.5" /></Button><Button variant="ghost" size="icon-sm" onClick={() => handleDelete(row.original.id)}><Trash2 className="size-3.5 text-destructive" /></Button></div> },
  ], [openEdit, handleDelete])

  const table = useReactTable({ data: filtered, columns, state: { sorting }, onSortingChange: setSorting, getCoreRowModel: getCoreRowModel(), getSortedRowModel: getSortedRowModel() })

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div><h1 className="text-2xl font-bold tracking-tight">Entrepôts</h1><p className="text-sm text-muted-foreground">{warehouses.length} entrepôt{warehouses.length !== 1 ? "s" : ""}</p></div>
        <Button onClick={openCreate}><Plus className="size-4" />Nouvel entrepôt</Button>
      </div>
      <div className="relative max-w-sm"><Search className="absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" /><Input className="pl-8" placeholder="Rechercher..." value={search} onChange={(e) => setSearch(e.target.value)} /></div>
      {loading ? <div className="space-y-3">{Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-10 w-full rounded-lg" />)}</div>
      : <Card><CardContent className="p-0"><Table><TableHeader>{table.getHeaderGroups().map(hg => <TableRow key={hg.id}>{hg.headers.map(h => <TableHead key={h.id} onClick={h.column.getToggleSortingHandler()} className={h.column.getCanSort() ? "cursor-pointer select-none" : ""}>{flexRender(h.column.columnDef.header, h.getContext())}{{ asc: " ▲", desc: " ▼" }[h.column.getIsSorted() as string] ?? ""}</TableHead>)}</TableRow>)}</TableHeader><TableBody>{table.getRowModel().rows.length === 0 ? <TableRow><TableCell colSpan={columns.length} className="text-center py-8 text-muted-foreground">Aucun entrepôt</TableCell></TableRow> : table.getRowModel().rows.map(row => <TableRow key={row.id}>{row.getVisibleCells().map(cell => <TableCell key={cell.id}>{flexRender(cell.column.columnDef.cell, cell.getContext())}</TableCell>)}</TableRow>)}</TableBody></Table></CardContent></Card>}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader><DialogTitle>{editData.id ? "Modifier" : "Nouvel"} entrepôt</DialogTitle><DialogDescription>Remplissez les informations.</DialogDescription></DialogHeader>
          <div className="grid gap-3">
            <div className="space-y-1"><label className="text-xs font-medium">Nom *</label><Input value={editData.name} onChange={(e) => setEditData({ ...editData, name: e.target.value })} /></div>
            <div className="space-y-1"><label className="text-xs font-medium">Adresse</label><Input value={editData.address} onChange={(e) => setEditData({ ...editData, address: e.target.value })} /></div>
            <div className="space-y-1"><label className="text-xs font-medium">Responsable</label><Input value={editData.manager} onChange={(e) => setEditData({ ...editData, manager: e.target.value })} /></div>
          </div>
          <DialogFooter><DialogClose asChild><Button variant="outline">Annuler</Button></DialogClose><Button onClick={handleSave} disabled={saving || !editData.name.trim()}>{saving ? "..." : "Enregistrer"}</Button></DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}