import { useEffect, useState, useCallback } from "react"
import { useTranslation } from "react-i18next"
import { Plus, Pencil, Trash2, Search, Star } from "lucide-react"
import { getCustomers, createCustomer, updateCustomer, deleteCustomer, type Customer, type CustomerFormData } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter, DialogClose } from "@/components/ui/dialog"
import { Card, CardContent } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"

function CustomerForm({ data, onChange }: { data: CustomerFormData; onChange: (d: CustomerFormData) => void }) {
  const { t } = useTranslation()
  return (
    <div className="grid gap-3">
      <div className="space-y-1"><label className="text-xs font-medium">{t("customers.form.name")}</label><Input value={data.name} onChange={(e) => onChange({ ...data, name: e.target.value })} /></div>
      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-1"><label className="text-xs font-medium">{t("customers.form.type")}</label><Select value={data.type ?? "normal"} onValueChange={(v) => onChange({ ...data, type: v })}><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="normal">{t("customers.type.normal")}</SelectItem><SelectItem value="fidele">{t("customers.type.loyal")}</SelectItem><SelectItem value="gros">{t("customers.type.bulk")}</SelectItem></SelectContent></Select></div>
        <div className="space-y-1"><label className="text-xs font-medium">{t("customers.loyal")}</label><Select value={String(data.is_loyal ?? 0)} onValueChange={(v) => onChange({ ...data, is_loyal: parseInt(v) })}><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="1">{t("common.yes")}</SelectItem><SelectItem value="0">{t("common.no")}</SelectItem></SelectContent></Select></div>
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-1"><label className="text-xs font-medium">{t("common.email")}</label><Input type="email" value={data.email ?? ""} onChange={(e) => onChange({ ...data, email: e.target.value })} /></div>
        <div className="space-y-1"><label className="text-xs font-medium">{t("common.phone")}</label><Input value={data.phone ?? ""} onChange={(e) => onChange({ ...data, phone: e.target.value })} /></div>
      </div>
      <div className="space-y-1"><label className="text-xs font-medium">{t("common.address")}</label><Input value={data.address ?? ""} onChange={(e) => onChange({ ...data, address: e.target.value })} /></div>
      <div className="space-y-1"><label className="text-xs font-medium">{t("common.notes")}</label><Input value={data.notes ?? ""} onChange={(e) => onChange({ ...data, notes: e.target.value })} /></div>
    </div>
  )
}

export function CustomersPage() {
  const { t } = useTranslation()
  const [customers, setCustomers] = useState<Customer[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [formData, setFormData] = useState<CustomerFormData>({ name: "" })
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try { setCustomers(await getCustomers()) }
    catch (err) { console.error(err) }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { load() }, [load])

  const filtered = customers.filter(c => !search || c.name.toLowerCase().includes(search.toLowerCase()) || c.client_code.toLowerCase().includes(search.toLowerCase()) || c.email.toLowerCase().includes(search.toLowerCase()))

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div><h1 className="text-2xl font-bold tracking-tight">{t("customers.title")}</h1><p className="text-sm text-muted-foreground">{t("customers.count", { count: customers.length })}</p></div>
        <Button onClick={() => { setEditingId(null); setFormData({ name: "" }); setDialogOpen(true) }}><Plus className="size-4" />{t("customers.new")}</Button>
      </div>
      <div className="relative max-w-sm"><Search className="absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" /><Input className="pl-8" placeholder={t("common.search")} value={search} onChange={(e) => setSearch(e.target.value)} /></div>
      {loading ? <div className="space-y-3">{Array.from({ length: 8 }).map((_, i) => <Skeleton key={i} className="h-10 w-full rounded-lg" />)}</div>
      : <Card><CardContent className="p-0"><Table>
        <TableHeader><TableRow><TableHead>{t("customers.code")}</TableHead><TableHead>{t("common.name")}</TableHead><TableHead>{t("customers.type")}</TableHead><TableHead>{t("common.email")}</TableHead><TableHead>{t("common.phone")}</TableHead><TableHead>{t("customers.loyal")}</TableHead><TableHead /></TableRow></TableHeader>
        <TableBody>{filtered.length === 0 ? <TableRow><TableCell colSpan={7} className="text-center py-8 text-muted-foreground">{t("customers.empty")}</TableCell></TableRow> : filtered.map(c => <TableRow key={c.id}>
          <TableCell className="font-mono text-xs text-muted-foreground">{c.client_code}</TableCell>
          <TableCell className="font-medium">{c.name}</TableCell>
          <TableCell><span className="text-xs">{({ normal: t("customers.type.normal"), fidele: t("customers.type.loyal"), gros: t("customers.type.bulk") })[c.type] ?? c.type}</span></TableCell>
          <TableCell className="text-xs text-muted-foreground">{c.email}</TableCell>
          <TableCell className="text-xs text-muted-foreground">{c.phone}</TableCell>
          <TableCell>{c.is_loyal ? <Star className="size-3.5 text-amber-500 fill-amber-500" /> : null}</TableCell>
          <TableCell><div className="flex justify-end gap-1">
            <Button variant="ghost" size="icon-sm" onClick={() => { setEditingId(c.id); setFormData({ name: c.name, type: c.type, email: c.email, phone: c.phone, address: c.address, is_loyal: c.is_loyal, notes: c.notes }); setDialogOpen(true) }}><Pencil className="size-3.5" /></Button>
            <Button variant="ghost" size="icon-sm" onClick={async () => { const res = await deleteCustomer(c.id); if (res.success) load() }}><Trash2 className="size-3.5 text-destructive" /></Button>
          </div></TableCell>
        </TableRow>)}</TableBody>
      </Table></CardContent></Card>}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader><DialogTitle>{editingId ? t("customers.dialog.title") : t("customers.new")}</DialogTitle><DialogDescription>{t("customers.dialog.description")}</DialogDescription></DialogHeader>
          <CustomerForm data={formData} onChange={setFormData} />
          <DialogFooter><DialogClose asChild><Button variant="outline">{t("common.cancel")}</Button></DialogClose>
            <Button onClick={async () => {
              if (!formData.name.trim()) return; setSaving(true)
              try {
                const res = editingId ? await updateCustomer(editingId, formData) : await createCustomer(formData)
                if (res.success) { setDialogOpen(false); load() }
              } finally { setSaving(false) }
            }} disabled={saving || !formData.name.trim()}>{saving ? t("common.saving") : t("common.save")}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}