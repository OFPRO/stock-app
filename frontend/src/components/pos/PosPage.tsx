import { useEffect, useState, useCallback, useRef } from "react"
import { useTranslation } from "react-i18next"
import { ShoppingCart, Plus, Minus, Trash2, Search, Banknote, CreditCard, CircleDollarSign, Printer } from "lucide-react"
import { getActiveSession, openSession, closeSession, createPosTransaction, getRecentTransactions, getCashMovements, createCashMovement, getProducts, getPosCustomers, getBestSellers, type PosSession, type PosTransaction, type PosCartItem, type CashMovement, type BestSeller, type Product, type PosCustomer } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Switch } from "@/components/ui/switch"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter, DialogClose } from "@/components/ui/dialog"
import { Card, CardContent } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"

type PricingTier = "normal" | "fidele" | "gros"

function getTierPrice(item: PosCartItem, tier: PricingTier): number {
  if (tier === "fidele") return item.price_loyal || item.base_price
  if (tier === "gros") return item.price_gros || item.base_price
  return item.base_price
}

export function PosPage() {
  const { t } = useTranslation()
  const [session, setSession] = useState<PosSession | null>(null)
  const [transactions, setTransactions] = useState<PosTransaction[]>([])
  const [loading, setLoading] = useState(true)
  const [cart, setCart] = useState<PosCartItem[]>([])
  const [searchQuery, setSearchQuery] = useState("")
  const searchRef = useRef<HTMLInputElement>(null)
  const addToCartRef = useRef(addToCart)
  addToCartRef.current = addToCart
  const [products, setProducts] = useState<Product[]>([])
  const [customers, setCustomers] = useState<PosCustomer[]>([])
  const [bestSellers, setBestSellers] = useState<BestSeller[]>([])
  const [selectedCustomerId, setSelectedCustomerId] = useState("0")
  const [pricingTier, setPricingTier] = useState<PricingTier>("normal")
  const [applyTax, setApplyTax] = useState(false)
  const [paymentMethod, setPaymentMethod] = useState("cash")
  const [tenderedAmount, setTenderedAmount] = useState("")
  const [saving, setSaving] = useState(false)
  const [openSessionOpen, setOpenSessionOpen] = useState(false)
  const [openingCash, setOpeningCash] = useState("0")
  const [closeSessionOpen, setCloseSessionOpen] = useState(false)
  const [closingCash, setClosingCash] = useState("0")
  const [cashMovOpen, setCashMovOpen] = useState(false)
  const [cashMovType, setCashMovType] = useState("in")
  const [cashMovAmount, setCashMovAmount] = useState("")
  const [cashMovReason, setCashMovReason] = useState("")
  const [cashMovements, setCashMovements] = useState<CashMovement[]>([])

  const subtotal = cart.reduce((sum, item) => sum + item.quantity * item.unit_price, 0)
  const rawSubtotal = cart.reduce((sum, item) => sum + item.quantity * item.base_price, 0)
  const discountAmount = rawSubtotal - subtotal
  const taxAmount = applyTax ? subtotal * 0.2 : 0
  const total = subtotal + taxAmount
  const change = paymentMethod === "cash" && tenderedAmount ? Math.max(0, parseFloat(tenderedAmount) - total) : 0

  const recalcPrices = useCallback((tier: PricingTier) => {
    setCart(prev => prev.map(item => ({
      ...item,
      unit_price: getTierPrice(item, tier),
    })))
  }, [])

  const handleTierChange = (tier: PricingTier) => {
    setPricingTier(tier)
    recalcPrices(tier)
  }

  const handleCustomerChange = (value: string) => {
    setSelectedCustomerId(value)
    if (value !== "0") {
      const customer = customers.find(c => String(c.id) === value)
      if (customer && (customer.type === "fidele" || customer.type === "gros")) {
        handleTierChange(customer.type as PricingTier)
      } else {
        handleTierChange("normal")
      }
    } else {
      handleTierChange("normal")
    }
  }

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const sessions = await getActiveSession()
      const s = sessions.length > 0 ? sessions[0] : null
      setSession(s)
      if (s) {
        const [txns, movs] = await Promise.all([
          getRecentTransactions(20, s.id),
          getCashMovements(),
        ])
        setTransactions(txns)
        setCashMovements(movs)
        loadProducts()
      }
    } catch (err) { console.error(err) }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { load() }, [load])

  const loadProducts = async () => {
    const results = await Promise.allSettled([getProducts(), getPosCustomers(), getBestSellers(10)])
    if (results[0].status === "fulfilled") setProducts(results[0].value)
    else console.error("Failed to load products", results[0].reason)
    if (results[1].status === "fulfilled") setCustomers(results[1].value)
    else console.error("Failed to load customers", results[1].reason)
    if (results[2].status === "fulfilled") setBestSellers(results[2].value)
    else console.error("Failed to load best sellers", results[2].reason)
  }

  const handleOpenSession = async () => {
    setSaving(true)
    try {
      const res = await openSession({ opening_cash: parseFloat(openingCash) || 0 })
      if (res.success && res.session) {
        setSession(res.session)
        setOpenSessionOpen(false)
        load()
      }
    } finally { setSaving(false) }
  }

  const handleCloseSession = async () => {
    setSaving(true)
    try {
      const res = await closeSession(session!.id, { closing_cash: parseFloat(closingCash) || 0 })
      if (res.success) {
        setSession(null)
        setCloseSessionOpen(false)
        setCart([])
        setTransactions([])
      }
    } finally { setSaving(false) }
  }

  const addToCart = (product: Product) => {
    setCart(prev => {
      const existing = prev.find(item => item.product_id === product.id)
      const base_price = product.price_base || product.price
      if (existing) {
        return prev.map(item =>
          item.product_id === product.id
            ? { ...item, quantity: item.quantity + 1 }
            : item
        )
      }
      const newItem: PosCartItem = {
        product_id: product.id,
        product_name: product.name,
        product_sku: product.sku,
        quantity: 1,
        unit_price: base_price,
        base_price,
        price_loyal: product.price_loyal || base_price,
        price_gros: product.price_gros || base_price,
      }
      if (pricingTier === "fidele") newItem.unit_price = newItem.price_loyal
      else if (pricingTier === "gros") newItem.unit_price = newItem.price_gros
      return [...prev, newItem]
    })
  }

  const updateCartQty = (productId: number, delta: number) => {
    setCart(prev => prev.map(item =>
      item.product_id === productId
        ? { ...item, quantity: Math.max(1, item.quantity + delta) }
        : item
    ))
  }

  const removeFromCart = (productId: number) => {
    setCart(prev => prev.filter(item => item.product_id !== productId))
  }

  const handleCompleteSale = async () => {
    if (cart.length === 0 || !session) return
    setSaving(true)
    try {
      const res = await createPosTransaction({
        session_id: session.id,
        customer_id: selectedCustomerId !== "0" ? parseInt(selectedCustomerId) : null,
        items: cart,
        payment_method: paymentMethod,
        tendered_amount: paymentMethod === "cash" ? (parseFloat(tenderedAmount) || total) : total,
        pricing_tier: pricingTier,
        apply_tax: applyTax,
      })
      if (res.success) {
        setCart([])
        setTenderedAmount("")
        setSelectedCustomerId("0")
        setPricingTier("normal")
        setApplyTax(true)
        const [txns, movs] = await Promise.all([
          getRecentTransactions(20, session.id),
          getCashMovements(),
        ])
        setTransactions(txns)
        setCashMovements(movs)
        if (res.document_number) {
          const url = res.document_type === "ticket"
            ? `/api/pos/tickets/${res.document_number}`
            : `/api/invoices/${res.document_id}/pdf`
          window.open(url, "_blank")
        }
      } else {
        alert(res.error || t("pos.sale_error"))
      }
    } finally { setSaving(false) }
  }

  const handleCashMovement = async () => {
    if (!session || !cashMovAmount) return
    setSaving(true)
    try {
      const res = await createCashMovement({
        type: cashMovType,
        amount: parseFloat(cashMovAmount),
        reason: cashMovReason || undefined,
      })
      if (res.success) {
        setCashMovOpen(false)
        setCashMovAmount("")
        setCashMovReason("")
        const movs = await getCashMovements()
        setCashMovements(movs)
      }
    } finally { setSaving(false) }
  }

  const filteredProducts = products.filter(p =>
    !searchQuery
    || p.name.toLowerCase().includes(searchQuery.toLowerCase())
    || (p.barcode && p.barcode.toLowerCase().includes(searchQuery.toLowerCase()))
    || (p.sku && p.sku.toLowerCase().includes(searchQuery.toLowerCase()))
  )

  useEffect(() => {
    if (session && cart.length === 0) searchRef.current?.focus()
  }, [session, cart.length])

  useEffect(() => {
    if (!searchQuery || products.length === 0) return
    const q = searchQuery.toLowerCase().trim()
    if (q.length < 3) return
    const timer = setTimeout(() => {
      const match = products.find(p => p.barcode && p.barcode.toLowerCase() === q)
      if (match) {
        addToCartRef.current(match)
        setSearchQuery("")
      }
    }, 200)
    return () => clearTimeout(timer)
  }, [searchQuery, products])

  if (loading) return <div className="space-y-3">{Array.from({ length: 8 }).map((_, i) => <Skeleton key={i} className="h-10 w-full rounded-lg" />)}</div>

  if (!session) {
    return (
      <div className="space-y-6">
        <h1 className="text-2xl font-bold tracking-tight">{t("pos.title")}</h1>
        <Card className="max-w-md mx-auto mt-12">
          <CardContent className="p-8 text-center space-y-4">
            <ShoppingCart className="size-12 mx-auto text-muted-foreground" />
            <h2 className="text-lg font-semibold">{t("pos.no_session")}</h2>
            <p className="text-sm text-muted-foreground">{t("pos.no_session_description")}</p>
            <Button onClick={() => { setOpenSessionOpen(true); setOpeningCash("0") }}>
              {t("pos.open_session")}
            </Button>
          </CardContent>
        </Card>

        <Dialog open={openSessionOpen} onOpenChange={setOpenSessionOpen}>
          <DialogContent className="sm:max-w-sm">
            <DialogHeader><DialogTitle>{t("pos.open_session")}</DialogTitle><DialogDescription>{t("pos.open_session_description")}</DialogDescription></DialogHeader>
            <div className="space-y-3">
              <div className="space-y-1"><label className="text-xs font-medium">{t("pos.opening_funds_label")}</label><Input type="number" step="0.01" value={openingCash} onChange={(e) => setOpeningCash(e.target.value)} /></div>
            </div>
            <DialogFooter>
              <DialogClose asChild><Button variant="outline">{t("common.cancel")}</Button></DialogClose>
              <Button onClick={handleOpenSession} disabled={saving}>{t("pos.open")}</Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>
    )
  }

  const expectedCash = session.opening_cash + cashMovements
    .filter(m => m.type === "in")
    .reduce((s, m) => s + m.amount, 0)
    - cashMovements.filter(m => m.type === "out")
    .reduce((s, m) => s + m.amount, 0)

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-3">
          <h1 className="text-2xl font-bold tracking-tight">{t("pos.title")}</h1>
          <span className="inline-flex items-center rounded-full bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400 px-2.5 py-0.5 text-xs font-medium">
            {session.session_number}
          </span>
          <span className="text-xs text-muted-foreground">
            {t("pos.opened_at", { time: session.opened_at.slice(11, 16) })}
          </span>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={() => { setCashMovOpen(true); setCashMovAmount(""); setCashMovReason("") }}>
            <CircleDollarSign className="size-3.5 mr-1" />{t("pos.cash_movement")}
          </Button>
          <Button variant="outline" size="sm" onClick={() => { setCloseSessionOpen(true); setClosingCash(String(expectedCash)) }}>
            {t("pos.close_session")}
          </Button>
        </div>
      </div>

      <div className="grid gap-4 grid-cols-1 lg:grid-cols-5">
        <div className="lg:col-span-3 space-y-4">
          <Card>
            <CardContent className="p-4 space-y-3">
              <h2 className="text-sm font-semibold flex items-center gap-2"><ShoppingCart className="size-4" />{t("pos.cart")}</h2>
              {cart.length === 0 ? (
                <p className="text-sm text-muted-foreground py-4 text-center">{t("pos.cart_empty")}</p>
              ) : (
                <div className="space-y-1">
                  {cart.map(item => (
                    <div key={item.product_id} className="flex items-center gap-2 py-1.5 border-b last:border-0">
                      <div className="flex-1 min-w-0">
                        <p className="text-sm truncate">{item.product_name}</p>
                        <p className="text-xs text-muted-foreground">{item.product_sku}</p>
                      </div>
                      <div className="flex items-center gap-1">
                        <Button variant="ghost" size="icon-sm" onClick={() => updateCartQty(item.product_id, -1)}><Minus className="size-3" /></Button>
                        <span className="w-6 text-center text-sm tabular-nums">{item.quantity}</span>
                        <Button variant="ghost" size="icon-sm" onClick={() => updateCartQty(item.product_id, 1)}><Plus className="size-3" /></Button>
                      </div>
                      <div className="text-right min-w-[4.5rem]">
                        <span className="text-sm tabular-nums font-medium">{(item.quantity * item.unit_price).toFixed(2)}</span>
                        {item.unit_price < item.base_price && (
                          <p className="text-[10px] text-muted-foreground line-through">{((item.quantity * item.base_price).toFixed(2))}</p>
                        )}
                      </div>
                      <Button variant="ghost" size="icon-sm" onClick={() => removeFromCart(item.product_id)}><Trash2 className="size-3.5 text-destructive" /></Button>
                    </div>
                  ))}
                </div>
              )}
              <div className="border-t pt-2 space-y-1 text-sm">
                <div className="flex justify-between"><span className="text-muted-foreground">{t("pos.subtotal_full_price")}</span><span className="tabular-nums">{rawSubtotal.toFixed(2)} DH</span></div>
                {discountAmount > 0 && (
                  <div className="flex justify-between text-amber-600 dark:text-amber-400"><span className="text-muted-foreground text-amber-600 dark:text-amber-400">{t("pos.discount")}</span><span className="tabular-nums">-{discountAmount.toFixed(2)} DH</span></div>
                )}
                <div className="flex justify-between font-medium"><span>{t("pos.net")}</span><span className="tabular-nums">{subtotal.toFixed(2)} DH</span></div>
                <div className="flex justify-between items-center">
                  <label className="flex items-center gap-2 cursor-pointer">
                    <Switch checked={applyTax} onCheckedChange={setApplyTax} />
                    <span className={applyTax ? "" : "text-muted-foreground"}>{t("pos.tva")}</span>
                  </label>
                  <span className="tabular-nums">{taxAmount.toFixed(2)} DH</span>
                </div>
                <div className="flex justify-between font-bold text-base"><span>{t("common.total")}</span><span className="tabular-nums">{total.toFixed(2)} DH</span></div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-4 space-y-3">
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1"><label className="text-xs font-medium">{t("common.client")}</label><Select value={selectedCustomerId} onValueChange={handleCustomerChange}><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="0">{t("pos.counter_customer")}</SelectItem>{customers.map(c => <SelectItem key={c.id} value={String(c.id)}>{c.name}</SelectItem>)}</SelectContent></Select></div>
                <div className="space-y-1"><label className="text-xs font-medium">{t("pos.pricing_category")}</label><Select value={pricingTier} onValueChange={handleTierChange}><SelectTrigger><SelectValue /></SelectTrigger><SelectContent>
                  <SelectItem value="normal">{t("pos.pricing_tier.normal")}</SelectItem>
                  <SelectItem value="fidele">{t("pos.pricing_tier.loyal")}</SelectItem>
                  <SelectItem value="gros">{t("pos.pricing_tier.bulk")}</SelectItem>
                </SelectContent></Select></div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1"><label className="text-xs font-medium">{t("pos.payment_method")}</label><Select value={paymentMethod} onValueChange={setPaymentMethod}><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="cash"><div className="flex items-center gap-2"><Banknote className="size-3.5" />{t("pos.payment.cash")}</div></SelectItem><SelectItem value="card"><div className="flex items-center gap-2"><CreditCard className="size-3.5" />{t("pos.payment.card")}</div></SelectItem></SelectContent></Select></div>
              </div>
              {paymentMethod === "cash" && <div className="space-y-1"><label className="text-xs font-medium">{t("pos.tendered_amount")}</label><Input type="number" step="0.01" placeholder="0.00" value={tenderedAmount} onChange={(e) => setTenderedAmount(e.target.value)} /></div>}
              {paymentMethod === "cash" && parseFloat(tenderedAmount) > 0 && change > 0 && (
                <div className="flex justify-between text-sm text-emerald-600 dark:text-emerald-400 font-medium"><span>{t("pos.change")}</span><span className="tabular-nums">{change.toFixed(2)} DH</span></div>
              )}
              <Button className="w-full" size="lg" disabled={cart.length === 0 || saving || (paymentMethod === "cash" && tenderedAmount !== "" && parseFloat(tenderedAmount) < total)} onClick={handleCompleteSale}>
                {saving ? "..." : paymentMethod === "cash" ? `${t("pos.complete_sale")}${total > 0 ? ` (${total.toFixed(2)} DH)` : ""}` : t("pos.complete_sale")}
              </Button>
            </CardContent>
          </Card>
        </div>

        <div className="lg:col-span-2 space-y-4">
          <Card>
            <CardContent className="p-4 space-y-3">
              <div className="relative">
                <Search className="absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                <Input ref={searchRef} className="pl-8" placeholder={t("pos.search_product")} value={searchQuery}
                  onFocus={() => loadProducts()}
                  onChange={(e) => setSearchQuery(e.target.value)} />
              </div>
              <div className="max-h-[320px] overflow-y-auto space-y-0.5">
                {filteredProducts.slice(0, 30).map(product => (
                  <div key={product.id} className="flex items-center gap-2 py-1.5 px-1 rounded hover:bg-muted/50 cursor-pointer" onClick={() => addToCart(product)}>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm truncate">{product.name}</p>
                      <p className="text-xs text-muted-foreground">{product.sku} — {t("pos.stock_prefix")} {product.quantity}</p>
                    </div>
                    <span className="text-sm tabular-nums">{product.price.toFixed(2)} DH</span>
                    <Button variant="ghost" size="icon-sm" className="shrink-0"><Plus className="size-3.5" /></Button>
                  </div>
                ))}
                {filteredProducts.length === 0 && <p className="text-sm text-muted-foreground py-4 text-center">{t("pos.no_product_found")}</p>}
              </div>
            </CardContent>
          </Card>

          {bestSellers.length > 0 && <Card>
            <CardContent className="p-4">
              <h3 className="text-xs font-semibold text-muted-foreground uppercase mb-2">{t("pos.best_sellers")}</h3>
              <div className="flex flex-wrap gap-2">
                {bestSellers.map(b => {
                  const found = products.find(pr => pr.id === b.id)
                  return (
                    <Button key={b.id} variant="outline" size="sm" className="text-xs h-7"
                      onClick={() => { if (found) addToCart(found) }}>
                      {b.name} {found ? "" : `— ${b.price.toFixed(2)} DH`}
                    </Button>
                  )
                })}
              </div>
            </CardContent>
          </Card>}

          <Card>
            <CardContent className="p-4">
              <h3 className="text-xs font-semibold text-muted-foreground uppercase mb-2">{t("pos.cash_summary")}</h3>
              <div className="space-y-1 text-sm">
                <div className="flex justify-between"><span className="text-muted-foreground">{t("pos.opening_funds")}</span><span className="tabular-nums">{session.opening_cash.toFixed(2)} DH</span></div>
                <div className="flex justify-between"><span className="text-muted-foreground">{t("pos.cash_in")}</span><span className="tabular-nums text-green-600 dark:text-green-400">+{cashMovements.filter(m => m.type === "in").reduce((s, m) => s + m.amount, 0).toFixed(2)} DH</span></div>
                <div className="flex justify-between"><span className="text-muted-foreground">{t("pos.cash_out")}</span><span className="tabular-nums text-destructive">-{cashMovements.filter(m => m.type === "out").reduce((s, m) => s + m.amount, 0).toFixed(2)} DH</span></div>
                <div className="flex justify-between font-medium border-t pt-1"><span>{t("pos.expected")}</span><span className="tabular-nums">{expectedCash.toFixed(2)} DH</span></div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      <Card>
        <CardContent className="p-0">
          <div className="p-3 border-b"><h3 className="text-sm font-semibold">{t("pos.recent_transactions")}</h3></div>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("pos.ticket_number")}</TableHead>
                <TableHead>{t("common.client")}</TableHead>
                <TableHead className="text-right">{t("common.total")}</TableHead>
                <TableHead>{t("pos.payment")}</TableHead>
                <TableHead>{t("common.date")}</TableHead>
                <TableHead className="w-12"></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {transactions.length === 0 ? (
                <TableRow><TableCell colSpan={6} className="text-center py-6 text-muted-foreground">{t("pos.no_transactions")}</TableCell></TableRow>
              ) : transactions.map(tx => (
                <TableRow key={tx.id}>
                  <TableCell className="font-mono text-xs text-muted-foreground">{tx.transaction_number}</TableCell>
                  <TableCell className="text-sm">{tx.customer_name ?? t("pos.counter_customer")}</TableCell>
                  <TableCell className="text-right tabular-nums font-medium">{tx.total.toFixed(2)} DH</TableCell>
                  <TableCell>
                    <span className="inline-flex items-center gap-1 text-xs">
                      {tx.payment_method === "cash" ? <Banknote className="size-3" /> : <CreditCard className="size-3" />}
                      {tx.payment_method === "cash" ? t("pos.payment.cash") : t("pos.payment.card")}
                    </span>
                  </TableCell>
                  <TableCell className="text-xs text-muted-foreground">{tx.created_at.slice(11, 19)}</TableCell>
                  <TableCell>
                    <Button variant="ghost" size="icon-sm" onClick={() => window.open('/api/pos/tickets/' + tx.transaction_number, '_blank')}>
                      <Printer className="size-3.5" />
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      <Dialog open={closeSessionOpen} onOpenChange={setCloseSessionOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader><DialogTitle>{t("pos.close_session_title")}</DialogTitle><DialogDescription>{t("pos.close_session_description")}</DialogDescription></DialogHeader>
          <div className="space-y-3">
            <div className="flex justify-between text-sm"><span className="text-muted-foreground">{t("pos.expected")}</span><span className="tabular-nums font-medium">{expectedCash.toFixed(2)} DH</span></div>
            <div className="space-y-1"><label className="text-xs font-medium">{t("pos.actual_cash")}</label><Input type="number" step="0.01" value={closingCash} onChange={(e) => setClosingCash(e.target.value)} /></div>
          </div>
          <DialogFooter>
            <DialogClose asChild><Button variant="outline">{t("common.cancel")}</Button></DialogClose>
            <Button onClick={handleCloseSession} disabled={saving}>{t("common.close")}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={cashMovOpen} onOpenChange={setCashMovOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader><DialogTitle>{t("pos.cash_movement_title")}</DialogTitle><DialogDescription>{t("pos.cash_movement_description")}</DialogDescription></DialogHeader>
          <div className="space-y-3">
            <div className="space-y-1"><label className="text-xs font-medium">{t("pos.cash_movement_type")}</label><Select value={cashMovType} onValueChange={setCashMovType}><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="in"><div className="flex items-center gap-2">{t("pos.cash_movement.in")}</div></SelectItem><SelectItem value="out"><div className="flex items-center gap-2">{t("pos.cash_movement.out")}</div></SelectItem></SelectContent></Select></div>
            <div className="space-y-1"><label className="text-xs font-medium">{t("pos.cash_movement_amount")}</label><Input type="number" step="0.01" value={cashMovAmount} onChange={(e) => setCashMovAmount(e.target.value)} /></div>
            <div className="space-y-1"><label className="text-xs font-medium">{t("pos.cash_movement_reason")}</label><Input value={cashMovReason} onChange={(e) => setCashMovReason(e.target.value)} /></div>
          </div>
          <DialogFooter>
            <DialogClose asChild><Button variant="outline">{t("common.cancel")}</Button></DialogClose>
            <Button onClick={handleCashMovement} disabled={saving || !cashMovAmount}>{saving ? "..." : t("common.validate")}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
