import { useEffect, useState, useCallback, useMemo } from "react"
import { useTranslation } from "react-i18next"
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
  getSuppliers,
  createSupplier,
  updateSupplier,
  deleteSupplier,
  type Supplier,
  type SupplierFormData,
} from "@/lib/api"
import { Button } from "@/components/ui/button"
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

function SupplierForm({
  data,
  onChange,
}: {
  data: SupplierFormData
  onChange: (d: SupplierFormData) => void
}) {
  const { t } = useTranslation()
  return (
    <div className="grid gap-3">
      <div className="space-y-1">
        <label className="text-xs font-medium">{t("suppliers.form.name")}</label>
        <Input
          value={data.name}
          onChange={(e) => onChange({ ...data, name: e.target.value })}
          placeholder={t("suppliers.form.name_placeholder")}
        />
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-1">
          <label className="text-xs font-medium">{t("common.email")}</label>
          <Input
            type="email"
            value={data.email ?? ""}
            onChange={(e) => onChange({ ...data, email: e.target.value })}
            placeholder={t("suppliers.form.email_placeholder")}
          />
        </div>
        <div className="space-y-1">
          <label className="text-xs font-medium">{t("common.phone")}</label>
          <Input
            value={data.phone ?? ""}
            onChange={(e) => onChange({ ...data, phone: e.target.value })}
            placeholder={t("suppliers.form.phone_placeholder")}
          />
        </div>
      </div>
      <div className="space-y-1">
        <label className="text-xs font-medium">{t("common.address")}</label>
        <Input
          value={data.address ?? ""}
          onChange={(e) => onChange({ ...data, address: e.target.value })}
          placeholder={t("suppliers.form.address_placeholder")}
        />
      </div>
      <div className="space-y-1">
        <label className="text-xs font-medium">{t("suppliers.form.contact_person")}</label>
        <Input
          value={data.contact_person ?? ""}
          onChange={(e) => onChange({ ...data, contact_person: e.target.value })}
          placeholder={t("suppliers.form.contact_placeholder")}
        />
      </div>
    </div>
  )
}

export function SuppliersPage() {
  const { t } = useTranslation()
  const [suppliers, setSuppliers] = useState<Supplier[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")
  const [sorting, setSorting] = useState<SortingState>([{ id: "name", desc: false }])
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [formData, setFormData] = useState<SupplierFormData>({ name: "" })
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const data = await getSuppliers()
      setSuppliers(data)
    } catch (err) {
      console.error("Failed to load suppliers:", err)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  const filtered = useMemo(() => suppliers.filter((s) => {
    if (!search) return true
    const q = search.toLowerCase()
    return s.name.toLowerCase().includes(q) || s.email.toLowerCase().includes(q) || s.phone.toLowerCase().includes(q)
  }), [suppliers, search])

  const openCreate = () => {
    setEditingId(null)
    setFormData({ name: "" })
    setDialogOpen(true)
  }

  const openEdit = useCallback((s: Supplier) => {
    setEditingId(s.id)
    setFormData({
      name: s.name,
      email: s.email,
      phone: s.phone,
      address: s.address,
      contact_person: s.contact_person,
    })
    setDialogOpen(true)
  }, [])

  const handleSave = async () => {
    if (!formData.name.trim()) return
    setSaving(true)
    try {
      if (editingId) {
        const res = await updateSupplier(editingId, formData)
        if (res.success) { setDialogOpen(false); load() }
        else { console.error("Update failed:", res.error) }
      } else {
        const res = await createSupplier(formData)
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
      const res = await deleteSupplier(id)
      if (res.success) load()
      else console.error("Delete failed:", res.error)
    } catch (err) {
      console.error("Delete failed:", err)
    }
  }, [load])

  const columns = useMemo<ColumnDef<Supplier>[]>(() => [
    { accessorKey: "name", header: () => t("common.name"), cell: ({ getValue }) => <span className="font-medium">{getValue() as string}</span> },
    { accessorKey: "email", header: () => t("common.email") },
    { accessorKey: "phone", header: () => t("common.phone") },
    { accessorKey: "address", header: () => t("common.address"), cell: ({ getValue }) => { const v = getValue() as string; return v ? <span className="text-xs text-muted-foreground">{v}</span> : null } },
    { accessorKey: "contact_person", header: () => t("suppliers.contact"), cell: ({ getValue }) => { const v = getValue() as string; return v ? <span className="text-xs text-muted-foreground">{v}</span> : null } },
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
          <h1 className="text-2xl font-bold tracking-tight">{t("suppliers.title")}</h1>
          <p className="text-sm text-muted-foreground">{t("suppliers.count", { count: suppliers.length })}</p>
        </div>
        <Button onClick={openCreate}><Plus className="size-4" />{t("suppliers.new")}</Button>
      </div>

      <div className="relative flex-1 min-w-[200px] max-w-sm">
        <Search className="absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input className="pl-8" placeholder={t("common.search")} value={search} onChange={(e) => setSearch(e.target.value)} />
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
                  <TableRow><TableCell colSpan={columns.length} className="text-center py-8 text-muted-foreground">{t("suppliers.empty")}</TableCell></TableRow>
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
            <DialogTitle>{editingId ? t("suppliers.dialog.edit_title") : t("suppliers.new")}</DialogTitle>
            <DialogDescription>{t("suppliers.dialog.description")}</DialogDescription>
          </DialogHeader>
          <SupplierForm data={formData} onChange={setFormData} />
          <DialogFooter>
            <DialogClose asChild><Button variant="outline">{t("common.cancel")}</Button></DialogClose>
            <Button onClick={handleSave} disabled={saving || !formData.name.trim()}>
              {saving ? t("common.saving") : t("common.save")}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}