import { useTranslation } from "react-i18next"
import { useEffect, useRef, useState } from "react"
import { type Product } from "@/lib/api"
import { Input } from "@/components/ui/input"

interface ProductSelectProps {
  products: Product[]
  onSelect: (product: Product, defaultPrice: number) => void
  placeholder?: string
  loading?: boolean
}

export function ProductSelect({ products, onSelect, placeholder, loading }: ProductSelectProps) {
  const { t } = useTranslation()
  const [search, setSearch] = useState("")
  const [isOpen, setIsOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setIsOpen(false)
      }
    }
    document.addEventListener("mousedown", handleClick)
    return () => document.removeEventListener("mousedown", handleClick)
  }, [])

  const outOfStock = products.filter((p) => p.quantity <= p.min_quantity || p.quantity < 0)
  const inStock = products.filter((p) => p.quantity > p.min_quantity && p.quantity >= 0)

  const filter = search.toLowerCase().trim()

  const filteredOutOfStock = filter
    ? outOfStock.filter(
        (p) =>
          p.name.toLowerCase().indexOf(filter) !== -1 ||
          (p.sku && p.sku.toLowerCase().indexOf(filter) !== -1)
      ).slice(0, 5)
    : []

  const filteredInStock = filter
    ? inStock.filter(
        (p) =>
          p.name.toLowerCase().indexOf(filter) !== -1 ||
          (p.sku && p.sku.toLowerCase().indexOf(filter) !== -1)
      ).slice(0, 5)
    : []

  const allResults = filteredOutOfStock.concat(filteredInStock).slice(0, 5)

  const handleSelect = (product: Product) => {
    const defaultPrice = product.purchase_price_avg || product.price || 0
    onSelect(product, defaultPrice)
    setSearch("")
    setIsOpen(false)
  }

  return (
    <div ref={ref} className="relative">
      <Input
        value={search}
        onChange={(e) => {
          setSearch(e.target.value)
          if (e.target.value.trim().length > 0) setIsOpen(true)
        }}
        onFocus={() => {
          if (search.trim().length > 0) setIsOpen(true)
        }}
        placeholder={placeholder ?? t("products.select.search_placeholder")}
        className="w-full"
      />
      {isOpen && (
        <div className="absolute left-0 right-0 z-50 mt-1 max-h-64 overflow-y-auto rounded-lg border bg-popover text-popover-foreground shadow-md">
          {loading ? (
            <div className="px-3 py-2 text-xs text-muted-foreground italic">
              {t("products.select.loading")}
            </div>
          ) : products.length === 0 ? (
            <div className="px-3 py-2 text-xs text-muted-foreground italic">
              {t("products.select.no_products")}
            </div>
          ) : allResults.length === 0 ? (
            <div className="px-3 py-2 text-xs text-muted-foreground italic">
              {t("products.select.no_results")}
            </div>
          ) : (
            allResults.map((p) => {
              const stockClass =
                p.quantity <= 0
                  ? "text-red-600"
                  : p.quantity <= p.min_quantity
                  ? "text-amber-600"
                  : "text-emerald-600"
              const stockLabel =
                p.quantity <= 0
                  ? "Rupture"
                  : p.quantity <= p.min_quantity
                  ? t("products.select.remaining_prefix") + " " + p.quantity
                  : t("products.select.stock_prefix") + " " + p.quantity
              return (
                <div
                  key={p.id}
                  className="flex items-center justify-between gap-2 px-3 py-2 text-sm cursor-pointer hover:bg-accent"
                  onClick={() => handleSelect(p)}
                >
                  <div className="flex flex-col min-w-0">
                    <span className="font-medium truncate">{p.name}</span>
                    <span className="text-xs text-muted-foreground">{p.sku || "-"}</span>
                  </div>
                  <span className={`text-xs font-medium whitespace-nowrap ${stockClass}`}>
                    {stockLabel}
                  </span>
                </div>
              )
            })
          )}
        </div>
      )}
    </div>
  )
}
