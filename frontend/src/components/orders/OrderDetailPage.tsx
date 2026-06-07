import { useTranslation } from "react-i18next"
import { useEffect, useState, useCallback } from "react"
import { useParams, useNavigate } from "react-router-dom"
import { ArrowLeft, Package, ShoppingCart, Truck, CheckCircle, CreditCard, XCircle, ExternalLink } from "lucide-react"
import { getOrders, getOrderItems, updateOrderStatus, getCustomers, createInvoice, type PurchaseOrder, type OrderItem, type Customer } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Skeleton } from "@/components/ui/skeleton"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter, DialogClose } from "@/components/ui/dialog"
import { AlertDialog, AlertDialogTrigger, AlertDialogContent, AlertDialogHeader, AlertDialogTitle, AlertDialogDescription, AlertDialogFooter, AlertDialogCancel, AlertDialogAction } from "@/components/ui/alert-dialog"
import { NativeSelect } from "@/components/ui/native-select"
import { Input } from "@/components/ui/input"

const STATUS_COLORS: Record<string, string> = { brouillon: "bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300", recue: "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400", paye: "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400", annulee: "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400" }

export function OrderDetailPage() {
  const { t } = useTranslation()
  const STATUS_LABELS: Record<string, string> = { brouillon: t("orders.status.draft"), recue: t("orders.status.received"), paye: t("orders.status.paid"), annulee: t("orders.status.cancelled") }
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [order, setOrder] = useState<PurchaseOrder | null>(null)
  const [items, setItems] = useState<OrderItem[]>([])
  const [loading, setLoading] = useState(true)
  const [convOpen, setConvOpen] = useState(false)
  const [customers, setCustomers] = useState<Customer[]>([])
  const [convCustomer, setConvCustomer] = useState(0)
  const [convDueDate, setConvDueDate] = useState("")
  const [converting, setConverting] = useState(false)

  const load = useCallback(async () => {
    if (!id) return
    setLoading(true)
    try {
      const orders = await getOrders()
      const found = orders.find((o) => o.id === Number(id))
      setOrder(found ?? null)
      if (found) {
        const orderItems = await getOrderItems(found.id)
        setItems(orderItems)
      }
    } catch {
      setOrder(null)
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => { load() }, [load])

  const handleStatusChange = async (status: string) => {
    if (!order) return
    await updateOrderStatus(order.id, status)
    load()
  }

  const handleCancel = async () => {
    if (!order) return
    await updateOrderStatus(order.id, "annulee")
    load()
  }

  const openConvert = async () => {
    setCustomers(await getCustomers())
    const due = new Date()
    due.setDate(due.getDate() + 30)
    setConvDueDate(due.toISOString().slice(0, 10))
    setConvCustomer(0)
    setConvOpen(true)
  }

  const handleConvert = async () => {
    if (!order || !convCustomer) return
    setConverting(true)
    try {
      const res = await createInvoice({
        customer_id: convCustomer,
        items: items.map((i) => ({ product_id: i.product_id, quantity: i.quantity, unit_price: i.unit_price })),
      })
      if (res.success) {
        setConvOpen(false)
        load()
      }
    } finally {
      setConverting(false)
    }
  }

  if (loading) {
    return (
      <div className="p-6 space-y-4">
        <div className="flex items-center gap-4 mb-6">
          <Skeleton className="h-10 w-10 rounded-full" />
          <Skeleton className="h-8 w-64" />
        </div>
        <Skeleton className="h-32 w-full" />
        <Skeleton className="h-64 w-full" />
      </div>
    )
  }

  if (!order) {
    return (
      <div className="p-6 text-center">
        <p className="text-muted-foreground">{t("orders.not_found")}</p>
        <Button variant="outline" className="mt-4" onClick={() => navigate("/orders")}>
          <ArrowLeft className="size-4 mr-2" />
          {t("orders.back_to_list")}
        </Button>
      </div>
    )
  }

  const canReceive = order.status === "brouillon"
  const canPay = order.status === "recue"
  const canCancel = order.status !== "annulee"
  const canConvert = order.status === "paye"

  return (
    <div className="p-6">
      <div className="flex items-center gap-4 mb-6">
        <Button variant="ghost" size="icon" onClick={() => navigate("/orders")}>
          <ArrowLeft className="size-5" />
        </Button>
        <div className="flex-1 min-w-0">
          <h1 className="text-2xl font-bold flex items-center gap-2">
            <Package className="size-5 shrink-0" />
            <span className="truncate">{order.order_number}</span>
          </h1>
          <p className="text-sm text-muted-foreground truncate">{order.supplier_name}</p>
        </div>
        <span className={`inline-flex items-center rounded-full px-3 py-1 text-sm font-medium shrink-0 ${STATUS_COLORS[order.status] ?? ""}`}>
          {STATUS_LABELS[order.status] ?? order.status}
        </span>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
        <Card>
          <CardContent className="flex items-center gap-3 p-4">
            <CreditCard className="size-5 text-primary shrink-0" />
            <div>
              <p className="text-xs text-muted-foreground">{t("common.total")}</p>
              <p className="text-lg font-bold tabular-nums">{order.total.toFixed(2)} DH</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center gap-3 p-4">
            <Truck className="size-5 text-muted-foreground shrink-0" />
            <div>
              <p className="text-xs text-muted-foreground">{t("orders.supplier")}</p>
              <p className="text-sm font-medium truncate">{order.supplier_name}</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center gap-3 p-4">
            <ShoppingCart className="size-5 text-muted-foreground shrink-0" />
            <div>
              <p className="text-xs text-muted-foreground">{t("orders.items_count", { count: items.length })}</p>
              <p className="text-lg font-bold tabular-nums">{items.length}</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex items-center gap-3 p-4">
            <CheckCircle className="size-5 text-muted-foreground shrink-0" />
            <div>
              <p className="text-xs text-muted-foreground">{t("orders.created_on")}</p>
              <p className="text-sm font-medium">{order.created_at.slice(0, 10)}</p>
            </div>
          </CardContent>
        </Card>
      </div>

      <div className="flex flex-wrap gap-2 mb-6">
        {canReceive && <Button onClick={() => handleStatusChange("recue")}><CheckCircle className="size-4 mr-1" />{t("orders.action.receive")}</Button>}
        {canPay && <Button onClick={() => handleStatusChange("paye")}><CreditCard className="size-4 mr-1" />{t("orders.action.mark_paid")}</Button>}
        {canConvert && <Button variant="outline" onClick={openConvert}><ExternalLink className="size-4 mr-1" />{t("orders.action.convert_to_invoice")}</Button>}
        {canCancel && <AlertDialog>
          <AlertDialogTrigger asChild>
            <Button variant="destructive" size="sm"><XCircle className="size-4 mr-1" />{t("orders.action.cancel")}</Button>
          </AlertDialogTrigger>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>{t("orders.action.cancel")} la commande {order?.order_number} ?</AlertDialogTitle>
              <AlertDialogDescription>{t("orders.action.cancel_confirm")}</AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>{t("common.no")}</AlertDialogCancel>
              <AlertDialogAction onClick={handleCancel} className="bg-destructive text-destructive-foreground hover:bg-destructive/90">{t("common.yes")}</AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>}
      </div>

      <Card className="mb-6">
        <CardContent className="p-4">
          <h3 className="text-sm font-semibold mb-3 flex items-center gap-2"><Package className="size-3.5" />Informations</h3>
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 text-sm">
            <div><span className="text-muted-foreground">{t("common.status")}</span><p className="font-medium">{STATUS_LABELS[order.status] ?? order.status}</p></div>
            <div><span className="text-muted-foreground">{t("orders.supplier")}</span><p className="font-medium">{order.supplier_name}</p></div>
            <div><span className="text-muted-foreground">{t("orders.created_on")}</span><p className="font-medium">{order.created_at.slice(0, 10)}</p></div>
            {order.received_at && <div><span className="text-muted-foreground">{t("orders.received_on")}</span><p className="font-medium">{order.received_at.slice(0, 10)}</p></div>}
            {order.paid_at && <div><span className="text-muted-foreground">{t("orders.paid_on")}</span><p className="font-medium">{order.paid_at.slice(0, 10)}</p></div>}
          </div>
          {order.notes && <div className="mt-3 pt-3 border-t"><span className="text-xs text-muted-foreground">{t("common.notes")}</span><p className="text-sm mt-1">{order.notes}</p></div>}
        </CardContent>
      </Card>

      <Card>
        <CardContent className="p-0">
          <div className="p-4 border-b">
            <h3 className="text-sm font-semibold flex items-center gap-2"><ShoppingCart className="size-3.5" />{t("orders.items_count", { count: items.length })}</h3>
          </div>
          {items.length === 0 ? (
            <p className="text-center py-8 text-muted-foreground">{t("orders.empty_items")}</p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t("common.product")}</TableHead>
                  <TableHead className="text-right">{t("common.qty")}</TableHead>
                  <TableHead className="text-right">{t("orders.unit_price")}</TableHead>
                  <TableHead className="text-right">{t("common.total")}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {items.map((item) => (
                  <TableRow key={item.id}>
                    <TableCell className="font-medium">{item.product_name ?? `${t("common.product")} #${item.product_id}`}</TableCell>
                    <TableCell className="text-right tabular-nums">{item.quantity}</TableCell>
                    <TableCell className="text-right tabular-nums">{item.unit_price.toFixed(2)} DH</TableCell>
                    <TableCell className="text-right tabular-nums font-medium">{(item.quantity * item.unit_price).toFixed(2)} DH</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <Dialog open={convOpen} onOpenChange={setConvOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader><DialogTitle>{t("orders.convert.title")}</DialogTitle><DialogDescription>{t("orders.convert.description")}</DialogDescription></DialogHeader>
          <div className="space-y-3">
            <div className="space-y-1"><label className="text-xs font-medium">{t("orders.convert.customer")}</label><NativeSelect value={String(convCustomer)} onChange={(v) => setConvCustomer(parseInt(v))} placeholder="Sélectionner" options={customers.map((c) => ({ value: String(c.id), label: c.name }))} /></div>
            <div className="space-y-1"><label className="text-xs font-medium">{t("orders.convert.due_date")}</label><Input type="date" value={convDueDate} onChange={(e) => setConvDueDate(e.target.value)} /></div>
          </div>
          <DialogFooter>
            <DialogClose asChild><Button variant="outline">{t("common.cancel")}</Button></DialogClose>
            <Button onClick={handleConvert} disabled={converting || !convCustomer}>{converting ? "..." : t("orders.convert.submit")}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
