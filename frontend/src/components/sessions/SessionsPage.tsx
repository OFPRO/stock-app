import { useEffect, useState, useCallback, useMemo } from "react"
import { History, Timer, DollarSign } from "lucide-react"
import { getSessionsHistory, type SessionSummary } from "@/lib/api"
import { type ColumnDef, type SortingState, flexRender, getCoreRowModel, getSortedRowModel, useReactTable } from "@tanstack/react-table"
import { Card, CardContent } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { useTranslation } from "react-i18next"

export function SessionsPage() {
  const { t } = useTranslation()
  const [sessions, setSessions] = useState<SessionSummary[]>([])
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    setLoading(true)
    try { setSessions(await getSessionsHistory()) }
    catch (err) { console.error(err) }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { load() }, [load])

  const columns = useMemo<ColumnDef<SessionSummary>[]>(() => [
    { accessorKey: "session_number", header: t("sessions.session"), cell: ({ getValue }) => <span className="font-mono text-xs">{getValue() as string}</span> },
    { accessorKey: "opened_at", header: t("sessions.opening"), cell: ({ getValue }) => <span className="text-xs text-muted-foreground">{(getValue() as string).slice(0, 16)}</span> },
    { accessorKey: "closed_at", header: t("sessions.closing"), cell: ({ getValue }) => { const v = getValue() as string | null; return v ? <span className="text-xs text-muted-foreground">{v.slice(0, 16)}</span> : <span className="text-xs text-muted-foreground">-</span> } },
    { accessorKey: "status", header: t("common.status"), cell: ({ getValue }) => { const v = getValue() as string; return <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${v === "open" ? "bg-emerald-100 text-emerald-700" : "bg-gray-100 text-gray-600"}`}>{t(v === "open" ? "sessions.status.open" : "sessions.status.closed")}</span> } },
    { accessorKey: "opening_cash", header: t("sessions.opening_cash"), cell: ({ getValue }) => <span className="tabular-nums">{(getValue() as number).toFixed(2)}</span> },
    { accessorKey: "total_sales", header: t("sessions.sales"), cell: ({ getValue }) => <span className="tabular-nums font-medium">{(getValue() as number).toFixed(2)} DH</span> },
    { accessorKey: "nb_transactions", header: t("sessions.transactions"), cell: ({ getValue }) => <span className="tabular-nums">{getValue() as number}</span> },
  ], [])

  const table = useReactTable({
    data: sessions, columns,
    state: { sorting: [{ id: "opened_at", desc: true }] as SortingState },
    onSortingChange: () => {},
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
  })

  const summary = { total: sessions.reduce((s, sess) => s + (sess.total_sales || 0), 0), count: sessions.filter(s => s.status === "closed").length, open: sessions.filter(s => s.status === "open").length }

  return (
    <div className="space-y-6">
      <div><h1 className="text-2xl font-bold tracking-tight">{t("sessions.title")}</h1><p className="text-sm text-muted-foreground">{t("sessions.count", { count: sessions.length })}</p></div>
      <div className="grid gap-4 sm:grid-cols-3">
        <Card><CardContent className="flex items-center gap-3 py-4"><History className="size-5 text-muted-foreground" /><div><p className="text-xs text-muted-foreground">{t("sessions.summary.closed")}</p><p className="text-xl font-bold">{summary.count}</p></div></CardContent></Card>
        <Card><CardContent className="flex items-center gap-3 py-4"><Timer className="size-5 text-muted-foreground" /><div><p className="text-xs text-muted-foreground">{t("sessions.summary.open")}</p><p className="text-xl font-bold">{summary.open}</p></div></CardContent></Card>
        <Card><CardContent className="flex items-center gap-3 py-4"><DollarSign className="size-5 text-muted-foreground" /><div><p className="text-xs text-muted-foreground">{t("sessions.summary.total_sales")}</p><p className="text-xl font-bold">{summary.total.toFixed(2)} DH</p></div></CardContent></Card>
      </div>
      {loading ? <div className="space-y-3">{Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-10 w-full rounded-lg" />)}</div>
      : <Card><CardContent className="p-0"><Table><TableHeader>{table.getHeaderGroups().map(hg => <TableRow key={hg.id}>{hg.headers.map(h => <TableHead key={h.id}>{flexRender(h.column.columnDef.header, h.getContext())}</TableHead>)}</TableRow>)}</TableHeader><TableBody>{table.getRowModel().rows.length === 0 ? <TableRow><TableCell colSpan={7} className="text-center py-8 text-muted-foreground">{t("sessions.empty")}</TableCell></TableRow> : table.getRowModel().rows.map(row => <TableRow key={row.id}>{row.getVisibleCells().map(cell => <TableCell key={cell.id}>{flexRender(cell.column.columnDef.cell, cell.getContext())}</TableCell>)}</TableRow>)}</TableBody></Table></CardContent></Card>}
    </div>
  )
}