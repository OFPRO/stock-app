import { useTranslation } from "react-i18next"
import { type LucideIcon } from "lucide-react"
import { cn } from "@/lib/utils"
import { Card, CardContent } from "@/components/ui/card"

interface KpiCardProps {
  label: string
  value: string | number
  sub?: string
  icon: LucideIcon
  trend?: { value: number; direction: "up" | "down" } | null
  color?: "default" | "primary" | "success" | "warning" | "danger"
  className?: string
}

const colorClasses = {
  default: "text-foreground",
  primary: "text-blue-600 dark:text-blue-400",
  success: "text-emerald-600 dark:text-emerald-400",
  warning: "text-amber-600 dark:text-amber-400",
  danger: "text-red-600 dark:text-red-400",
}

const iconBgClasses = {
  default: "bg-muted",
  primary: "bg-blue-100 dark:bg-blue-950",
  success: "bg-emerald-100 dark:bg-emerald-950",
  warning: "bg-amber-100 dark:bg-amber-950",
  danger: "bg-red-100 dark:bg-red-950",
}

export function KpiCard({
  label,
  value,
  sub,
  icon: Icon,
  trend,
  color = "default",
  className,
}: KpiCardProps) {
  const { t } = useTranslation()
  return (
    <Card className={cn("relative overflow-hidden", className)}>
      <CardContent className="p-4 md:p-5">
        <div className="flex items-start justify-between">
          <div className="space-y-1">
            <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
              {label}
            </p>
            <p className={cn("text-2xl font-bold tabular-nums", colorClasses[color])}>
              {value}
            </p>
            {sub && (
              <p className="text-xs text-muted-foreground">{sub}</p>
            )}
          </div>
          <div className={cn("rounded-lg p-2.5", iconBgClasses[color])}>
            <Icon className={cn("size-5", colorClasses[color])} />
          </div>
        </div>
        {trend && (
          <div className="mt-3 flex items-center gap-1 text-xs">
            <span
              className={cn(
                "font-medium",
                trend.direction === "up"
                  ? "text-emerald-600 dark:text-emerald-400"
                  : "text-red-600 dark:text-red-400"
              )}
            >
              {trend.direction === "up" ? "+" : ""}
              {trend.value.toFixed(1)}%
            </span>
            <span className="text-muted-foreground">{t("dashboard.kpi.vs_previous_period")}</span>
          </div>
        )}
      </CardContent>
    </Card>
  )
}
