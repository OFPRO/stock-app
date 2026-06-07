import { RefreshCw } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { useTranslation } from "react-i18next"
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
  const { t } = useTranslation()
  return (
    <div className="flex flex-wrap items-center gap-3">
      <Select value={period} onValueChange={onPeriodChange}>
        <SelectTrigger className="w-28">
          <SelectValue placeholder={t("dashboard.filters.period")} />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="7">{t("dashboard.filters.7_days")}</SelectItem>
          <SelectItem value="30">{t("dashboard.filters.30_days")}</SelectItem>
          <SelectItem value="90">{t("dashboard.filters.90_days")}</SelectItem>
        </SelectContent>
      </Select>
      <Select value={warehouseId} onValueChange={onWarehouseChange}>
        <SelectTrigger className="w-44">
          <SelectValue placeholder={t("dashboard.filters.all_warehouses")} />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="__all__">{t("dashboard.filters.all_warehouses")}</SelectItem>
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
