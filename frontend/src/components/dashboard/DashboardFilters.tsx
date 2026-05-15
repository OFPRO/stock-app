import { RefreshCw } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import type { Warehouse } from "@/lib/api"

interface DashboardFiltersProps {
  period: string
  onPeriodChange: (val: string) => void
  warehouseId: string
  onWarehouseChange: (val: string) => void
  warehouses: Warehouse[]
  onRefresh: () => void
  loading: boolean
}

export function DashboardFilters({
  period,
  onPeriodChange,
  warehouseId,
  onWarehouseChange,
  warehouses,
  onRefresh,
  loading,
}: DashboardFiltersProps) {
  return (
    <div className="flex flex-wrap items-center gap-3">
      <Select value={period} onValueChange={onPeriodChange}>
        <SelectTrigger className="w-28">
          <SelectValue placeholder="Période" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="7">7 jours</SelectItem>
          <SelectItem value="30">30 jours</SelectItem>
          <SelectItem value="90">90 jours</SelectItem>
        </SelectContent>
      </Select>
      <Select value={warehouseId} onValueChange={onWarehouseChange}>
        <SelectTrigger className="w-44">
          <SelectValue placeholder="Tous les entrepôts" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="__all__">Tous les entrepôts</SelectItem>
          {warehouses.map((w) => (
            <SelectItem key={w.id} value={String(w.id)}>
              {w.name}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      <Button variant="outline" size="icon" onClick={onRefresh} disabled={loading}>
        <RefreshCw className={loading ? "size-4 animate-spin" : "size-4"} />
      </Button>
    </div>
  )
}
