import { useEffect, useState, useCallback, useMemo } from "react"
import { Plus, Search, Eye, Trash2, FileText, Printer } from "lucide-react"
import { type ColumnDef, type SortingState, flexRender, getCoreRowModel, getSortedRowModel, useReactTable } from "@tanstack/react-table"
import { getInvoices, getInvoice, createInvoice, updateInvoice, deleteInvoice, removeInvoiceItem, getCustomers, getProducts, getInvoiceStats, type Invoice, type InvoiceDetail, type Customer, type Product } from "@/lib/api"
import { NativeSelect } from "@/components/ui/native-select"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter, DialogClose } from "@/components/ui/dialog"
import { Card, CardContent } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"

const STATUS_LABELS: Record<string, string> = { brouillon: "Brouillon", envoyee: "Envoyée", payee: "Payée", annulee: "Annulée" }
const STATUS_COLORS: Record<string, string> = { brouillon: "bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300", envoyee: "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400", payee: "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400", annulee: "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400" }
const STATUS_ACTIONS: Record<string, string[]> = { brouillon: ["envoyee", "annulee"], envoyee: ["payee", "annulee"] }

export function InvoicesPage() {
  const [invoices, setInvoices] = useState<Invoice[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")
  const [statusFilter, setStatusFilter] = useState("all")
  const [sorting, setSorting] = useState<SortingState>([{ id: "created_at", desc: true }])
  const [createOpen, setCreateOpen] = useState(false)
  const [detailOpen, setDetailOpen] = useState(false)
  const [detailData, setDetailData] = useState<InvoiceDetail | null>(null)
  const [customers, setCustomers] = useState<Customer[]>([])
  const [products, setProducts] = useState<Product[]>([])
  const [stats, setStats] = useState({ total_invoices: 0, total_amount: 0, paid_amount: 0, pending_amount: 0 })
  const [formData, setFormData] = useState({ customer_id: 0, notes: "" })
  const [formItems, setFormItems] = useState<{ product_id: number; quantity: number; unit_price: number }[]>([{ product_id: 0, quantity: 1, unit_price: 0 }])
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [inv, s] = await Promise.all([getInvoices(), getInvoiceStats()])
      setInvoices(inv)
      setStats(s)
    } catch (err) { console.error(err) }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { load() }, [load])

  const filtered = useMemo(() => invoices.filter(inv => {
    if (statusFilter !== "all" && inv.status !== statusFilter) return false
    if (search) {
      const q = search.toLowerCase()
      return inv.invoice_number.toLowerCase().includes(q) || (inv.customer_name ?? "").toLowerCase().includes(q)
    }
    return true
  }), [invoices, statusFilter, search])

  const openCreate = async () => {
    const [c, p] = await Promise.all([getCustomers(), getProducts()])
    setCustomers(c); setProducts(p)
    setFormData({ customer_id: 0, notes: "" })
    setFormItems([{ product_id: 0, quantity: 1, unit_price: 0 }])
    setCreateOpen(true)
  }

  const openDetail = useCallback(async (inv: Invoice) => {
    try {
      const d = await getInvoice(inv.id)
      setDetailData(d)
    } catch { setDetailData(null) }
    setDetailOpen(true)
  }, [])

  const handleCreate = async () => {
    if (formItems.every(i => !i.product_id)) return
    setSaving(true)
    try {
      const items = formItems.filter(i => i.product_id && i.quantity > 0)
      const customerPayload = formData.customer_id ? { customer_id: formData.customer_id } : {}
      const res = await createInvoice({ ...customerPayload, notes: formData.notes, items })
      if (res.success) { setCreateOpen(false); load() }
    } finally { setSaving(false) }
  }

  const handleStatusChange = useCallback(async (id: number, status: string) => {
    const res = await updateInvoice(id, { status })
    if (res.success) {
      if (detailOpen && detailData) {
        setDetailData({ ...detailData, status })
      }
      load()
    }
  }, [detailOpen, detailData, load])

  const handleDelete = useCallback(async (id: number) => {
    if (!confirm("Supprimer cette facture ?")) return
    const res = await deleteInvoice(id)
    if (res.success) load()
  }, [load])

  const handleItemRemove = async (itemId: number) => {
    if (!detailData || !confirm("Retirer cet article ?")) return
    const res = await removeInvoiceItem(detailData.id, itemId)
    if (res.success) {
      const updated = await getInvoice(detailData.id)
      setDetailData(updated)
      load()
    }
  }

  const columns = useMemo<ColumnDef<Invoice>[]>(() => [
    { accessorKey: "invoice_number", header: "N° Facture", cell: ({ getValue }) => <span className="font-mono text-xs text-muted-foreground">{getValue() as string}</span> },
    { accessorKey: "customer_name", header: "Client", cell: ({ getValue }) => <span>{(getValue() as string) ?? "—"}</span> },
    { accessorKey: "total", header: "Total", cell: ({ getValue }) => <span className="tabular-nums">{(getValue() as number).toFixed(2)} DH</span> },
    { accessorKey: "status", header: "Statut", cell: ({ getValue }) => { const v = getValue() as string; return <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_COLORS[v] ?? ""}`}>{STATUS_LABELS[v] ?? v}</span> } },
    { accessorKey: "created_at", header: "Date", cell: ({ getValue }) => <span className="text-xs text-muted-foreground">{(getValue() as string).slice(0, 10)}</span> },
    { id: "actions", header: "", cell: ({ row }) => {
      const inv = row.original
      const nextStatuses = STATUS_ACTIONS[inv.status] ?? []
      return <div className="flex justify-end gap-1">
        <Button variant="ghost" size="icon-sm" onClick={() => openDetail(inv)}><Eye className="size-3.5" /></Button>
        <Button variant="ghost" size="icon-sm" onClick={() => window.open('/api/invoices/' + inv.id + '/pdf', '_blank')}><Printer className="size-3.5" /></Button>
        {nextStatuses.map(s => <Button key={s} variant="outline" size="sm" className="text-xs h-7" onClick={() => handleStatusChange(inv.id, s)}>{STATUS_LABELS[s] ?? s}</Button>)}
        {inv.status !== "payee" && inv.status !== "annulee" && <Button variant="ghost" size="icon-sm" onClick={() => handleDelete(inv.id)}><Trash2 className="size-3.5 text-destructive" /></Button>}
      </div>
    }},
  ], [openDetail, handleStatusChange, handleDelete])

  const table = useReactTable({ data: filtered, columns, state: { sorting }, onSortingChange: setSorting, getCoreRowModel: getCoreRowModel(), getSortedRowModel: getSortedRowModel() })

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div><h1 className="text-2xl font-bold tracking-tight">Factures</h1><p className="text-sm text-muted-foreground">{stats.total_invoices} facture{stats.total_invoices !== 1 ? "s" : ""}</p></div>
        <Button onClick={openCreate}><Plus className="size-4" />Nouvelle facture</Button>
      </div>

      <div className="grid gap-3 grid-cols-1 sm:grid-cols-3">
        <Card><CardContent className="p-4"><div className="text-xs text-muted-foreground">Total facturé</div><div className="text-xl font-bold tabular-nums mt-1">{stats.total_amount.toFixed(2)} DH</div></CardContent></Card>
        <Card><CardContent className="p-4"><div className="text-xs text-muted-foreground">Payé</div><div className="text-xl font-bold text-green-600 dark:text-green-400 tabular-nums mt-1">{stats.paid_amount.toFixed(2)} DH</div></CardContent></Card>
        <Card><CardContent className="p-4"><div className="text-xs text-muted-foreground">En attente</div><div className="text-xl font-bold text-amber-600 dark:text-amber-400 tabular-nums mt-1">{stats.pending_amount.toFixed(2)} DH</div></CardContent></Card>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <div className="relative flex-1 min-w-[200px] max-w-sm"><Search className="absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" /><Input className="pl-8" placeholder="Rechercher..." value={search} onChange={(e) => setSearch(e.target.value)} /></div>
        <Select value={statusFilter} onValueChange={setStatusFilter}><SelectTrigger className="w-36"><SelectValue /></SelectTrigger><SelectContent><SelectItem value="all">Tous</SelectItem><SelectItem value="brouillon">Brouillon</SelectItem><SelectItem value="envoyee">Envoyée</SelectItem><SelectItem value="payee">Payée</SelectItem><SelectItem value="annulee">Annulée</SelectItem></SelectContent></Select>
      </div>

      {loading ? <div className="space-y-3">{Array.from({ length: 8 }).map((_, i) => <Skeleton key={i} className="h-10 w-full rounded-lg" />)}</div>
      : <Card><CardContent className="p-0"><Table><TableHeader>{table.getHeaderGroups().map(hg => <TableRow key={hg.id}>{hg.headers.map(h => <TableHead key={h.id} onClick={h.column.getToggleSortingHandler()} className={h.column.getCanSort() ? "cursor-pointer select-none" : ""}>{flexRender(h.column.columnDef.header, h.getContext())}{{ asc: " ▲", desc: " ▼" }[h.column.getIsSorted() as string] ?? ""}</TableHead>)}</TableRow>)}</TableHeader><TableBody>{table.getRowModel().rows.length === 0 ? <TableRow><TableCell colSpan={6} className="text-center py-8 text-muted-foreground">Aucune facture</TableCell></TableRow> : table.getRowModel().rows.map(row => <TableRow key={row.id}>{row.getVisibleCells().map(cell => <TableCell key={cell.id}>{flexRender(cell.column.columnDef.cell, cell.getContext())}</TableCell>)}</TableRow>)}</TableBody></Table></CardContent></Card>}

      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader><DialogTitle>Nouvelle facture</DialogTitle><DialogDescription>Créer une facture client.</DialogDescription></DialogHeader>
          <div className="grid gap-3">
            <div className="grid grid-cols-1 gap-3">
              <div className="space-y-1"><label className="text-xs font-medium">Client</label><NativeSelect value={String(formData.customer_id)} onChange={(v) => setFormData({ ...formData, customer_id: parseInt(v) })} placeholder="Sélectionner (optionnel)" options={[{ value: "0", label: "Client comptoir" }, ...customers.map(c => ({ value: String(c.id), label: `${c.name} (${c.client_code})` }))]} /></div>
            </div>
            <div className="space-y-1"><label className="text-xs font-medium">Notes</label><Input value={formData.notes} onChange={(e) => setFormData({ ...formData, notes: e.target.value })} /></div>
            <div><label className="text-xs font-medium mb-1 block">Articles</label>
              {formItems.map((item, i) => <div key={i} className="flex gap-2 mb-2 items-end">
                <div className="flex-1"><NativeSelect value={String(item.product_id)} onChange={(v) => { const items = [...formItems]; items[i] = { ...items[i], product_id: parseInt(v) }; setFormItems(items) }} placeholder="Produit" options={products.map(p => ({ value: String(p.id), label: p.name }))} /></div>
                <div className="w-20"><Input type="number" min={1} value={item.quantity} onChange={(e) => { const items = [...formItems]; items[i] = { ...items[i], quantity: parseInt(e.target.value) || 0 }; setFormItems(items) }} /></div>
                <div className="w-24"><Input type="number" step="0.01" value={item.unit_price} placeholder="Prix" onChange={(e) => { const items = [...formItems]; items[i] = { ...items[i], unit_price: parseFloat(e.target.value) || 0 }; setFormItems(items) }} /></div>
                {formItems.length > 1 && <Button variant="ghost" size="icon-sm" onClick={() => setFormItems(formItems.filter((_, j) => j !== i))}>×</Button>}
              </div>)}
              <Button variant="outline" size="sm" onClick={() => setFormItems([...formItems, { product_id: 0, quantity: 1, unit_price: 0 }])}>+ Ajouter un article</Button>
            </div>
          </div>
          <DialogFooter><DialogClose asChild><Button variant="outline">Annuler</Button></DialogClose><Button onClick={handleCreate} disabled={saving || formItems.every(i => !i.product_id)}>{saving ? "..." : "Créer"}</Button></DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={detailOpen} onOpenChange={setDetailOpen}>
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2"><FileText className="size-4" />{detailData?.invoice_number}</DialogTitle>
            <DialogDescription>
              {detailData?.customer_name ? `Client: ${detailData.customer_name}` : "Client comptoir"}
              {detailData?.status ? ` — ${STATUS_LABELS[detailData.status] ?? detailData.status}` : ""}
            </DialogDescription>
          </DialogHeader>
          {detailData && <div className="space-y-4">
            <div className="grid grid-cols-2 gap-3 text-sm">
              <div><span className="text-muted-foreground">Date</span><p>{detailData.created_at.slice(0, 10)}</p></div>
              <div><span className="text-muted-foreground">Entrepôt</span><p>{detailData.warehouse_name}</p></div>
              {detailData.customer_address && <div><span className="text-muted-foreground">Adresse</span><p className="text-xs">{detailData.customer_address}</p></div>}
              {detailData.due_date && <div><span className="text-muted-foreground">Échéance</span><p>{detailData.due_date}</p></div>}
            </div>
            {detailData.items.length > 0 && <div><p className="text-xs font-medium mb-2">Articles ({detailData.items.length})</p>
              <Table><TableHeader><TableRow><TableHead>Produit</TableHead><TableHead className="text-right">Qté</TableHead><TableHead className="text-right">P.U.</TableHead><TableHead className="text-right">Total</TableHead>{detailData.status !== "payee" && detailData.status !== "annulee" && <TableHead className="w-10"></TableHead>}</TableRow></TableHeader>
              <TableBody>{detailData.items.map(item => <TableRow key={item.id}>
                <TableCell><span className="text-sm">{item.product_name}</span><span className="text-xs text-muted-foreground ml-2">({item.product_sku})</span></TableCell>
                <TableCell className="text-right tabular-nums">{item.quantity}</TableCell>
                <TableCell className="text-right tabular-nums">{item.unit_price.toFixed(2)} DH</TableCell>
                <TableCell className="text-right tabular-nums font-medium">{item.line_total.toFixed(2)} DH</TableCell>
                {detailData.status !== "payee" && detailData.status !== "annulee" && <TableCell><Button variant="ghost" size="icon-sm" onClick={() => handleItemRemove(item.id)}><Trash2 className="size-3.5 text-destructive" /></Button></TableCell>}
              </TableRow>)}</TableBody></Table>
              <div className="flex justify-end mt-3 space-y-1 text-sm">
                <div className="w-48 space-y-1">
                  <div className="flex justify-between"><span className="text-muted-foreground">Sous-total</span><span className="tabular-nums">{detailData.subtotal.toFixed(2)} DH</span></div>
                  {detailData.discount_total > 0 && <div className="flex justify-between"><span className="text-muted-foreground">Remise</span><span className="tabular-nums text-destructive">-{detailData.discount_total.toFixed(2)} DH</span></div>}
                  <div className="flex justify-between"><span className="text-muted-foreground">TVA</span><span className="tabular-nums">{detailData.tax_amount.toFixed(2)} DH</span></div>
                  <div className="flex justify-between font-bold border-t pt-1"><span>Total</span><span className="tabular-nums">{detailData.total.toFixed(2)} DH</span></div>
                </div>
              </div>
            </div>}
            <div className="flex gap-2 pt-2 border-t">
              {detailData.status === "brouillon" && <>
                <Button onClick={() => handleStatusChange(detailData.id, "envoyee")}>Marquer envoyée</Button>
                <Button variant="destructive" onClick={() => handleStatusChange(detailData.id, "annulee")}>Annuler</Button>
              </>}
              {detailData.status === "envoyee" && <>
                <Button onClick={() => handleStatusChange(detailData.id, "payee")}>Marquer payée</Button>
                <Button variant="destructive" onClick={() => handleStatusChange(detailData.id, "annulee")}>Annuler</Button>
              </>}
            </div>
          </div>}
          <DialogFooter><DialogClose asChild><Button>Fermer</Button></DialogClose></DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}