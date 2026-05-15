import { useEffect, useState } from "react"
import { Plus, Trash2, AlertTriangle } from "lucide-react"
import { createOrder, getProducts, getSuppliers, getWarehouses, type Product, type Supplier, type Warehouse } from "@/lib/api"
import { ProductSelect } from "./ProductSelect"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { NativeSelect } from "@/components/ui/native-select"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter, DialogClose } from "@/components/ui/dialog"

interface OrderItemRow {
  product_id: number
  product_name: string
  quantity: number
  unit_price: number
}

interface OrderCreateDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSuccess: () => void
}

export function OrderCreateDialog({ open, onOpenChange, onSuccess }: OrderCreateDialogProps) {
  const [products, setProducts] = useState<Product[]>([])
  const [suppliers, setSuppliers] = useState<Supplier[]>([])
  const [warehouses, setWarehouses] = useState<Warehouse[]>([])
  const [supplierId, setSupplierId] = useState(0)
  const [warehouseId, setWarehouseId] = useState(1)
  const [notes, setNotes] = useState("")
  const [items, setItems] = useState<OrderItemRow[]>([])
  const [saving, setSaving] = useState(false)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (open) {
      setLoading(true)
      Promise.all([getProducts(), getSuppliers(), getWarehouses()]).then(([p, s, w]) => {
        setProducts(p)
        setSuppliers(s)
        setWarehouses(w)
        setSupplierId(0)
        setWarehouseId(1)
        setNotes("")
        setItems([{ product_id: 0, product_name: "", quantity: 1, unit_price: 0 }])
      }).finally(() => setLoading(false))
    }
  }, [open])

  const outOfStock = products.filter((p) => p.quantity <= p.min_quantity || p.quantity < 0).slice(0, 5)

  const addItem = () => {
    setItems([...items, { product_id: 0, product_name: "", quantity: 1, unit_price: 0 }])
  }

  const removeItem = (index: number) => {
    if (items.length > 1) {
      setItems(items.filter((_, i) => i !== index))
    }
  }

  const updateItem = (index: number, field: keyof OrderItemRow, value: number | string) => {
    const updated = [...items]
    ;(updated[index] as any)[field] = value
    setItems(updated)
  }

  const addRuptureProduct = (product: Product) => {
    const defaultPrice = product.purchase_price_avg || product.price || 0
    setItems([
      ...items,
      {
        product_id: product.id,
        product_name: product.name,
        quantity: 1,
        unit_price: defaultPrice,
      },
    ])
  }

  const handleProductSelect = (index: number, product: Product, defaultPrice: number) => {
    const updated = [...items]
    updated[index].product_id = product.id
    updated[index].product_name = product.name
    updated[index].unit_price = defaultPrice
    setItems(updated)
  }

  const total = items.reduce((sum, item) => sum + item.quantity * item.unit_price, 0)

  const handleSave = async () => {
    if (!supplierId || items.length === 0) return
    setSaving(true)
    try {
      const validItems = items
        .filter((i) => i.product_id && i.quantity > 0)
        .map((i) => ({ product_id: i.product_id, quantity: i.quantity, unit_price: i.unit_price }))
      const res = await createOrder({
        supplier_id: supplierId,
        warehouse_id: warehouseId,
        notes,
        items: validItems,
      })
      if (res.success) {
        onOpenChange(false)
        onSuccess()
      }
    } finally {
      setSaving(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>Nouvelle commande</DialogTitle>
          <DialogDescription>Créer un bon de commande fournisseur.</DialogDescription>
        </DialogHeader>

        <div className="grid gap-4">
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1">
              <label className="text-xs font-medium">Fournisseur *</label>
              <NativeSelect
                value={String(supplierId)}
                onChange={(v) => setSupplierId(parseInt(v))}
                placeholder="Sélectionner"
                options={suppliers.map((s) => ({ value: String(s.id), label: s.name }))}
              />
            </div>
            <div className="space-y-1">
              <label className="text-xs font-medium">Entrepôt</label>
              <NativeSelect
                value={String(warehouseId)}
                onChange={(v) => setWarehouseId(parseInt(v))}
                options={warehouses.map((w) => ({ value: String(w.id), label: w.name }))}
              />
            </div>
          </div>

          {outOfStock.length > 0 && (
            <div className="flex flex-wrap items-center gap-1.5 rounded-lg border border-red-200 bg-red-50 dark:border-red-900 dark:bg-red-950/20 px-3 py-2">
              <AlertTriangle className="size-3.5 shrink-0 text-red-600" />
              <span className="text-xs font-semibold text-red-600 mr-1">RUPTURE:</span>
              {outOfStock.map((p) => (
                <button
                  key={p.id}
                  type="button"
                  onClick={() => addRuptureProduct(p)}
                  className="inline-flex items-center gap-1 rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700 hover:bg-red-200 dark:bg-red-900/30 dark:text-red-400 dark:hover:bg-red-900/50 transition-colors"
                >
                  {p.name}
                  <span className="tabular-nums opacity-70">{p.quantity || 0}</span>
                </button>
              ))}
            </div>
          )}

          <div>
            <div className="flex items-center justify-between mb-2">
              <label className="text-xs font-medium">Articles</label>
              <Button variant="outline" size="sm" onClick={addItem}>
                <Plus className="size-3" /> Ajouter
              </Button>
            </div>

            <div className="rounded-lg border">
              <div className="grid grid-cols-[1fr_70px_90px_80px_32px] gap-2 px-3 py-1.5 text-xs font-medium text-muted-foreground bg-muted/50 border-b">
                <span>Produit</span>
                <span className="text-right">Qté</span>
                <span className="text-right">Prix</span>
                <span className="text-right">Total</span>
                <span />
              </div>

              <div className="divide-y">
                {items.map((item, i) => (
                  <div key={i} className="grid grid-cols-[1fr_70px_90px_80px_32px] gap-2 px-3 py-1.5 items-center">
                    <div className="min-w-0">
                      {item.product_name && item.product_id ? (
                        <div className="flex items-center gap-2">
                          <span className="text-sm truncate">{item.product_name}</span>
                          <button
                            type="button"
                            onClick={() => {
                              const updated = [...items]
                              updated[i] = { product_id: 0, product_name: "", quantity: 1, unit_price: 0 }
                              setItems(updated)
                            }}
                            className="text-xs text-muted-foreground hover:text-foreground shrink-0"
                          >
                            ×
                          </button>
                        </div>
                      ) : (
                        <ProductSelect
                          products={products}
                          onSelect={(product, defaultPrice) => handleProductSelect(i, product, defaultPrice)}
                          loading={loading}
                        />
                      )}
                    </div>
                    <Input
                      type="number"
                      min={1}
                      value={item.quantity}
                      onChange={(e) => updateItem(i, "quantity", parseInt(e.target.value) || 0)}
                      className="h-8 text-right"
                    />
                    <Input
                      type="number"
                      step="0.01"
                      min={0}
                      value={item.unit_price}
                      onChange={(e) => updateItem(i, "unit_price", parseFloat(e.target.value) || 0)}
                      className="h-8 text-right"
                    />
                    <span className="text-right text-sm font-medium tabular-nums">
                      {(item.quantity * item.unit_price).toFixed(2)}
                    </span>
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      onClick={() => removeItem(i)}
                      disabled={items.length <= 1}
                    >
                      <Trash2 className="size-3.5 text-destructive" />
                    </Button>
                  </div>
                ))}
              </div>

              <div className="flex justify-end px-3 py-2 border-t bg-muted/30">
                <span className="text-sm font-semibold">
                  Total: <span className="text-primary tabular-nums">{total.toFixed(2)} DH</span>
                </span>
              </div>
            </div>
          </div>

          <div className="space-y-1">
            <label className="text-xs font-medium">Notes</label>
            <Input
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Notes complémentaires..."
            />
          </div>
        </div>

        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline">Annuler</Button>
          </DialogClose>
          <Button
            onClick={handleSave}
            disabled={saving || !supplierId}
          >
            {saving ? "Enregistrement..." : "Enregistrer"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
