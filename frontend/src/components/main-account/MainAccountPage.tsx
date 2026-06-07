import { useEffect, useState, useCallback } from "react"
import { Landmark, ArrowDown, ArrowUp, History } from "lucide-react"
import { getMainAccountFull, depositToMainAccount, withdrawFromMainAccount, type MainAccountData, type MainAccountTransaction } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter, DialogClose } from "@/components/ui/dialog"
import { useTranslation } from "react-i18next"

function TransactionRow({ tx }: { tx: MainAccountTransaction }) {
  const { t: translate } = useTranslation()
  const REASON_LABELS: Record<string, string> = {
    initial: translate("main_account.reason.initial"),
    session_close: translate("main_account.reason.session_close"),
    supplier_order: translate("main_account.reason.supplier_order"),
    refund: translate("main_account.reason.refund"),
    adjustment: translate("main_account.reason.adjustment"),
    deposit: translate("main_account.reason.deposit"),
    withdrawal: translate("main_account.reason.withdrawal"),
  }
  return (
    <div className="flex items-center gap-3 py-2.5 border-b last:border-0">
      <div className={`flex size-8 items-center justify-center rounded-full ${tx.type === "in" ? "bg-emerald-100 dark:bg-emerald-900/30" : "bg-red-100 dark:bg-red-900/30"}`}>
        {tx.type === "in" ? <ArrowDown className="size-4 text-emerald-600 dark:text-emerald-400" /> : <ArrowUp className="size-4 text-red-600 dark:text-red-400" />}
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium">{REASON_LABELS[tx.reason] ?? tx.reason}</p>
        <p className="text-xs text-muted-foreground">{tx.note || tx.created_at.slice(0, 16)}</p>
      </div>
      <span className={`text-sm font-medium tabular-nums ${tx.type === "in" ? "text-emerald-600 dark:text-emerald-400" : "text-red-600 dark:text-red-400"}`}>
        {tx.type === "in" ? "+" : "-"}{tx.amount.toFixed(2)} DH
      </span>
    </div>
  )
}

export function MainAccountPage() {
  const { t } = useTranslation()
  const [data, setData] = useState<MainAccountData | null>(null)
  const [loading, setLoading] = useState(true)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [dialogMode, setDialogMode] = useState<"deposit" | "withdraw">("deposit")
  const [amount, setAmount] = useState("")
  const [reason, setReason] = useState("")
  const [note, setNote] = useState("")
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try { setData(await getMainAccountFull()) }
    catch (err) { console.error(err) }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { load() }, [load])

  const handleAction = async () => {
    const amt = parseFloat(amount)
    if (!amt || amt <= 0 || !reason.trim()) return
    setSaving(true)
    try {
      const res = dialogMode === "deposit" ? await depositToMainAccount(amt, reason, note) : await withdrawFromMainAccount(amt, reason, note)
      if (res.success) { setDialogOpen(false); setAmount(""); setReason(""); setNote(""); load() }
    } finally { setSaving(false) }
  }

  if (loading && !data) return <div className="space-y-3">{Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-12 w-full rounded-lg" />)}</div>
  if (!data) return <div className="flex items-center justify-center min-h-[400px] text-muted-foreground">{t("main_account.load_error")}</div>

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div><h1 className="text-2xl font-bold tracking-tight">{t("main_account.title")}</h1><p className="text-sm text-muted-foreground">{data.account.name}</p></div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => { setDialogMode("withdraw"); setDialogOpen(true) }}><ArrowUp className="size-4" />{t("main_account.reason.withdrawal")}</Button>
          <Button onClick={() => { setDialogMode("deposit"); setDialogOpen(true) }}><ArrowDown className="size-4" />{t("main_account.reason.deposit")}</Button>
        </div>
      </div>
      <Card>
        <CardContent className="flex items-center gap-4 py-6">
          <div className="flex size-14 items-center justify-center rounded-full bg-primary/10"><Landmark className="size-7 text-primary" /></div>
          <div>
            <p className="text-sm text-muted-foreground">{t("main_account.current_balance")}</p>
            <p className="text-3xl font-bold tabular-nums">{data.account.current_balance.toFixed(2)} DH</p>
          </div>
        </CardContent>
      </Card>
      <div><h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-2"><History className="size-3.5 inline mr-1" />{t("main_account.transaction_history")}</h2>
        <Card><CardContent className="p-4">
          {data.transactions.length === 0 ? <p className="text-sm text-muted-foreground text-center py-4">{t("main_account.empty")}</p> : data.transactions.map(t => <TransactionRow key={t.id} tx={t} />)}
        </CardContent></Card>
      </div>
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader><DialogTitle>{dialogMode === "deposit" ? t("main_account.reason.deposit") : t("main_account.reason.withdrawal")}</DialogTitle><DialogDescription>{t("main_account.dialog.description")}</DialogDescription></DialogHeader>
          <div className="grid gap-3">
            <div className="space-y-1"><label className="text-xs font-medium">{t("main_account.form.amount")}</label><Input type="number" step="0.01" min="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} /></div>
            <div className="space-y-1"><label className="text-xs font-medium">{t("main_account.form.reason")}</label><Input value={reason} onChange={(e) => setReason(e.target.value)} /></div>
            <div className="space-y-1"><label className="text-xs font-medium">{t("common.notes")}</label><Input value={note} onChange={(e) => setNote(e.target.value)} /></div>
          </div>
          <DialogFooter><DialogClose asChild><Button variant="outline">{t("common.cancel")}</Button></DialogClose><Button onClick={handleAction} disabled={saving || !amount || !reason.trim()}>{saving ? "..." : t("common.save")}</Button></DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}