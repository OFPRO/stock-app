import { useEffect, useState, useCallback } from "react"
import { ShoppingCart, Plus, Minus, Trash2, Search, Banknote, CreditCard, CircleDollarSign, Printer } from "lucide-react"
import { getActiveSession, openSession, closeSession, createPosTransaction, getRecentTransactions, getCashMovements, createCashMovement, getProducts, getPosCustomers, getBestSellers, type PosSession, type PosTransaction, type PosCartItem, type CashMovement, type BestSeller, type Product, type PosCustomer } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter, DialogClose } from "@/components/ui/dialog"
import { Card, CardContent } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"

export function PosPage() {
  const [session, setSession] = useState<PosSession | null>(null)
  const [transactions, setTransactions] = useState<PosTransaction[]>([])
  const [loading, setLoading] = useState(true)
  const [cart, setCart] = useState<PosCartItem[]>([])
  const [searchQuery, setSearchQuery] = useState("")
  const [products, setProducts] = useState<Product[]>([])
  const [customers, setCustomers] = useState<PosCustomer[]>([])
  const [bestSellers, setBestSellers] = useState<BestSeller[]>([])
  const [selectedCustomerId, setSelectedCustomerId] = useState("0")
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
  const taxAmount = subtotal * 0.2
  const total = subtotal + taxAmount
  const change = paymentMethod === "cash" && tenderedAmount ? Math.max(0, parseFloat(tenderedAmount) - total) : 0

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
      if (existing) {
        return prev.map(item =>
          item.product_id === product.id
            ? { ...item, quantity: item.quantity + 1 }
            : item
        )
      }
      return [...prev, {
        product_id: product.id,
        product_name: product.name,
        product_sku: product.sku,
        quantity: 1,
        unit_price: product.price,
        base_price: product.price_base || product.price,
      }]
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
      })
      if (res.success) {
        setCart([])
        setTenderedAmount("")
        setSelectedCustomerId("0")
        const [txns, movs] = await Promise.all([
          getRecentTransactions(20, session.id),
          getCashMovements(),
        ])
        setTransactions(txns)
        setCashMovements(movs)
        if (res.document_number) {
          const url = res.document_type === "ticket"
            ? `/api/pos/tickets/${res.document_number}`
            : `/api/invoices/${res.document_number}/pdf`
          window.open(url, "_blank")
        }
      } else {
        alert(res.error || "Erreur lors de l'encaissement")
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
    !searchQuery || p.name.toLowerCase().includes(searchQuery.toLowerCase())
  )

  if (loading) return <div className="space-y-3">{Array.from({ length: 8 }).map((_, i) => <Skeleton key={i} className="h-10 w-full rounded-lg" />)}</div>

  if (!session) {
    return (
      <div className="space-y-6">
        <h1 className="text-2xl font-bold tracking-tight">Caisse</h1>
        <Card className="max-w-md mx-auto mt-12">
          <CardContent className="p-8 text-center space-y-4">
            <ShoppingCart className="size-12 mx-auto text-muted-foreground" />
            <h2 className="text-lg font-semibold">Aucune session ouverte</h2>
            <p className="text-sm text-muted-foreground">Ouvrez une session pour commencer à vendre.</p>
            <Button onClick={() => { setOpenSessionOpen(true); setOpeningCash("0") }}>
              Ouvrir une session
            </Button>
          </CardContent>
        </Card>

        <Dialog open={openSessionOpen} onOpenChange={setOpenSessionOpen}>
          <DialogContent className="sm:max-w-sm">
            <DialogHeader><DialogTitle>Ouvrir une session</DialogTitle><DialogDescription>Saisissez le montant d'ouverture de caisse.</DialogDescription></DialogHeader>
            <div className="space-y-3">
              <div className="space-y-1"><label className="text-xs font-medium">Fonds d'ouverture (DH)</label><Input type="number" step="0.01" value={openingCash} onChange={(e) => setOpeningCash(e.target.value)} /></div>
            </div>
            <DialogFooter>
              <DialogClose asChild><Button variant="outline">Annuler</Button></DialogClose>
              <Button onClick={handleOpenSession} disabled={saving}>Ouvrir</Button>
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
          <h1 className="text-2xl font-bold tracking-tight">Caisse</h1>
          <span className="inline-flex items-center rounded-full bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400 px-2.5 py-0.5 text-xs font-medium">
            {session.session_number}
          </span>
          <span className="text-xs text-muted-foreground">
            Ouvert à {session.opened_at.slice(11, 16)}
          </span>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={() => { setCashMovOpen(true); setCashMovAmount(""); setCashMovReason("") }}>
            <CircleDollarSign className="size-3.5 mr-1" />Mouv. caisse
          </Button>
          <Button variant="outline" size="sm" onClick={() => { setCloseSessionOpen(true); setClosingCash(String(expectedCash)) }}>
            Fermer la session
          </Button>
        </div>
      </div>

      <div className="grid gap-4 grid-cols-1 lg:grid-cols-5">
        <div className="lg:col-span-3 space-y-4">
          <Card>
            <CardContent className="p-4 space-y-3">
              <h2 className="text-sm font-semibold flex items-center gap-2"><ShoppingCart className="size-4" />Panier</h2>
              {cart.length === 0 ? (
                <p className="text-sm text-muted-foreground py-4 text-center">Panier vide. Ajoutez des produits depuis la liste.</p>
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
                      <span className="w-20 text-right text-sm tabular-nums">{(item.quantity * item.unit_price).toFixed(2)}</span>
                      <Button variant="ghost" size="icon-sm" onClick={() => removeFromCart(item.product_id)}><Trash2 className="size-3.5 text-destructive" /></Button>
                    </div>
                  ))}
                </div>
              )}
              <div className="border-t pt-2 space-y-1 text-sm">
                <div className="flex justify-between"><span className="text-muted-foreground">Sous-total</span><span className="tabular-nums">{subtotal.toFixed(2)} DH</span></div>
                <div className="flex justify-between"><span className="text-muted-foreground">TVA (20%)</span><span className="tabular-nums">{taxAmount.toFixed(2)} DH</span></div>
                <div className="flex justify-between font-bold text-base"><span>Total</span><span className="tabular-nums">{total.toFixed(2)} DH</span></div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-4 space-y-3">
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1"><label className="text-xs font-medium">Client</label><Select value={selectedCustomerId} onValueChange={setSelectedCustomerId}><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="0">Client comptoir</SelectItem>{customers.map(c => <SelectItem key={c.id} value={String(c.id)}>{c.name}</SelectItem>)}</SelectContent></Select></div>
                <div className="space-y-1"><label className="text-xs font-medium">Paiement</label><Select value={paymentMethod} onValueChange={setPaymentMethod}><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="cash"><div className="flex items-center gap-2"><Banknote className="size-3.5" />Espèces</div></SelectItem><SelectItem value="card"><div className="flex items-center gap-2"><CreditCard className="size-3.5" />Carte</div></SelectItem></SelectContent></Select></div>
              </div>
              {paymentMethod === "cash" && <div className="space-y-1"><label className="text-xs font-medium">Montant donné</label><Input type="number" step="0.01" placeholder="0.00" value={tenderedAmount} onChange={(e) => setTenderedAmount(e.target.value)} /></div>}
              {paymentMethod === "cash" && parseFloat(tenderedAmount) > 0 && change > 0 && (
                <div className="flex justify-between text-sm text-emerald-600 dark:text-emerald-400 font-medium"><span>Monnaie à rendre</span><span className="tabular-nums">{change.toFixed(2)} DH</span></div>
              )}
              <Button className="w-full" size="lg" disabled={cart.length === 0 || saving || (paymentMethod === "cash" && tenderedAmount !== "" && parseFloat(tenderedAmount) < total)} onClick={handleCompleteSale}>
                {saving ? "..." : paymentMethod === "cash" ? `Valider la vente${total > 0 ? ` (${total.toFixed(2)} DH)` : ""}` : "Valider la vente"}
              </Button>
            </CardContent>
          </Card>
        </div>

        <div className="lg:col-span-2 space-y-4">
          <Card>
            <CardContent className="p-4 space-y-3">
              <div className="relative">
                <Search className="absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                <Input className="pl-8" placeholder="Rechercher un produit..." value={searchQuery}
                  onFocus={() => loadProducts()}
                  onChange={(e) => setSearchQuery(e.target.value)} />
              </div>
              <div className="max-h-[320px] overflow-y-auto space-y-0.5">
                {filteredProducts.slice(0, 30).map(product => (
                  <div key={product.id} className="flex items-center gap-2 py-1.5 px-1 rounded hover:bg-muted/50 cursor-pointer" onClick={() => addToCart(product)}>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm truncate">{product.name}</p>
                      <p className="text-xs text-muted-foreground">{product.sku} — Stock: {product.quantity}</p>
                    </div>
                    <span className="text-sm tabular-nums">{product.price.toFixed(2)} DH</span>
                    <Button variant="ghost" size="icon-sm" className="shrink-0"><Plus className="size-3.5" /></Button>
                  </div>
                ))}
                {filteredProducts.length === 0 && <p className="text-sm text-muted-foreground py-4 text-center">Aucun produit trouvé</p>}
              </div>
            </CardContent>
          </Card>

          {bestSellers.length > 0 && <Card>
            <CardContent className="p-4">
              <h3 className="text-xs font-semibold text-muted-foreground uppercase mb-2">Meilleures ventes</h3>
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
              <h3 className="text-xs font-semibold text-muted-foreground uppercase mb-2">Résumé caisse</h3>
              <div className="space-y-1 text-sm">
                <div className="flex justify-between"><span className="text-muted-foreground">Fonds ouverture</span><span className="tabular-nums">{session.opening_cash.toFixed(2)} DH</span></div>
                <div className="flex justify-between"><span className="text-muted-foreground">Entrées</span><span className="tabular-nums text-green-600 dark:text-green-400">+{cashMovements.filter(m => m.type === "in").reduce((s, m) => s + m.amount, 0).toFixed(2)} DH</span></div>
                <div className="flex justify-between"><span className="text-muted-foreground">Sorties</span><span className="tabular-nums text-destructive">-{cashMovements.filter(m => m.type === "out").reduce((s, m) => s + m.amount, 0).toFixed(2)} DH</span></div>
                <div className="flex justify-between font-medium border-t pt-1"><span>Attendu</span><span className="tabular-nums">{expectedCash.toFixed(2)} DH</span></div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      <Card>
        <CardContent className="p-0">
          <div className="p-3 border-b"><h3 className="text-sm font-semibold">Transactions récentes</h3></div>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>N° Ticket</TableHead>
                <TableHead>Client</TableHead>
                <TableHead className="text-right">Total</TableHead>
                <TableHead>Paiement</TableHead>
                <TableHead>Date</TableHead>
                <TableHead className="w-12"></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {transactions.length === 0 ? (
                <TableRow><TableCell colSpan={6} className="text-center py-6 text-muted-foreground">Aucune transaction</TableCell></TableRow>
              ) : transactions.map(tx => (
                <TableRow key={tx.id}>
                  <TableCell className="font-mono text-xs text-muted-foreground">{tx.transaction_number}</TableCell>
                  <TableCell className="text-sm">{tx.customer_name ?? "Client comptoir"}</TableCell>
                  <TableCell className="text-right tabular-nums font-medium">{tx.total.toFixed(2)} DH</TableCell>
                  <TableCell>
                    <span className="inline-flex items-center gap-1 text-xs">
                      {tx.payment_method === "cash" ? <Banknote className="size-3" /> : <CreditCard className="size-3" />}
                      {tx.payment_method === "cash" ? "Espèces" : "Carte"}
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
          <DialogHeader><DialogTitle>Fermer la session</DialogTitle><DialogDescription>Comptez le fonds de caisse et saisissez le montant.</DialogDescription></DialogHeader>
          <div className="space-y-3">
            <div className="flex justify-between text-sm"><span className="text-muted-foreground">Attendu</span><span className="tabular-nums font-medium">{expectedCash.toFixed(2)} DH</span></div>
            <div className="space-y-1"><label className="text-xs font-medium">Fonds de caisse réel (DH)</label><Input type="number" step="0.01" value={closingCash} onChange={(e) => setClosingCash(e.target.value)} /></div>
          </div>
          <DialogFooter>
            <DialogClose asChild><Button variant="outline">Annuler</Button></DialogClose>
            <Button onClick={handleCloseSession} disabled={saving}>Fermer</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={cashMovOpen} onOpenChange={setCashMovOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader><DialogTitle>Mouvement de caisse</DialogTitle><DialogDescription>Ajouter ou retirer de l'argent de la caisse.</DialogDescription></DialogHeader>
          <div className="space-y-3">
            <div className="space-y-1"><label className="text-xs font-medium">Type</label><Select value={cashMovType} onValueChange={setCashMovType}><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="in"><div className="flex items-center gap-2">Entrée</div></SelectItem><SelectItem value="out"><div className="flex items-center gap-2">Sortie</div></SelectItem></SelectContent></Select></div>
            <div className="space-y-1"><label className="text-xs font-medium">Montant (DH)</label><Input type="number" step="0.01" value={cashMovAmount} onChange={(e) => setCashMovAmount(e.target.value)} /></div>
            <div className="space-y-1"><label className="text-xs font-medium">Motif</label><Input value={cashMovReason} onChange={(e) => setCashMovReason(e.target.value)} /></div>
          </div>
          <DialogFooter>
            <DialogClose asChild><Button variant="outline">Annuler</Button></DialogClose>
            <Button onClick={handleCashMovement} disabled={saving || !cashMovAmount}>{saving ? "..." : "Valider"}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}