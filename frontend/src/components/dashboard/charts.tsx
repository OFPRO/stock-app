import Chart from "react-apexcharts"
import type { SalesDaily, CategoryDistribution, TopProduct, KpiInvoicesStatus, KpiTrend, KpiMargins } from "@/lib/api"

const chartColors = {
  primary: "#2563eb",
  success: "#059669",
  warning: "#d97706",
  danger: "#dc2626",
  muted: "#94a3b8",
}

export function SalesDailyChart({ data }: { data: SalesDaily[] }) {
  return (
    <Chart
      options={{
        chart: { type: "area", toolbar: { show: false }, fontFamily: "inherit" },
        xaxis: { categories: data.map((d) => d.date.slice(5)), labels: { show: false } },
        yaxis: { labels: { formatter: (v: number) => v.toFixed(0) } },
        dataLabels: { enabled: false },
        stroke: { curve: "smooth", width: 2 },
        fill: { type: "gradient", gradient: { shadeIntensity: 0.1 } },
        colors: [chartColors.primary],
        grid: { borderColor: "var(--border)", strokeDashArray: 3 },
        tooltip: { y: { formatter: (v: number) => `${v.toFixed(2)} DH` } },
      }}
      series={[{ name: "CA", data: data.map((d) => d.ca) }]}
      type="area"
      height={220}
    />
  )
}

export function CategoriesChart({ data }: { data: CategoryDistribution[] }) {
  const colors = [chartColors.primary, chartColors.success, chartColors.warning, chartColors.danger, chartColors.muted]
  return (
    <Chart
      options={{
        chart: { type: "donut", fontFamily: "inherit" },
        labels: data.map((d) => d.category),
        dataLabels: { enabled: false },
        legend: { position: "bottom", fontSize: "11px" },
        colors,
        responsive: [{ breakpoint: 480, options: { chart: { height: 200 }, legend: { position: "bottom" } } }],
        plotOptions: { pie: { donut: { size: "60%" } } },
      }}
      series={data.map((d) => d.qty_vendue)}
      type="donut"
      height={220}
    />
  )
}

export function TopProductsChart({ data }: { data: TopProduct[] }) {
  const names = data.map((d) => d.name.length > 18 ? d.name.slice(0, 16) + ".." : d.name).reverse()
  const qties = data.map((d) => d.qty_vendue).reverse()
  return (
    <Chart
      options={{
        chart: { type: "bar", fontFamily: "inherit" },
        xaxis: { categories: names, labels: { style: { fontSize: "10px" } } },
        yaxis: { labels: { formatter: (v: number) => v.toFixed(0) } },
        plotOptions: { bar: { horizontal: true, borderRadius: 3 } },
        dataLabels: { enabled: false },
        colors: [chartColors.primary],
        grid: { borderColor: "var(--border)", strokeDashArray: 3 },
      }}
      series={[{ name: "Qté vendue", data: qties }]}
      type="bar"
      height={220}
    />
  )
}

export function InvoicesStatusChart({ data }: { data: KpiInvoicesStatus }) {
  const labels = ["Payée", "Envoyée", "Brouillon", "Annulée"]
  const values = [data.payee, data.envoyee, data.brouillon, data.annulee]
  const colors = [chartColors.success, chartColors.warning, chartColors.muted, chartColors.danger]
  return (
    <Chart
      options={{
        chart: { type: "donut", fontFamily: "inherit" },
        labels,
        colors,
        dataLabels: { enabled: false },
        legend: { position: "bottom", fontSize: "11px" },
        plotOptions: { pie: { donut: { size: "60%" } } },
      }}
      series={values}
      type="donut"
      height={220}
    />
  )
}

export function MovementsChart({ data }: { data: KpiTrend[] }) {
  const recent = data.slice(-14)
  const dates = recent.map((d) => d.date.slice(5))
  return (
    <Chart
      options={{
        chart: { type: "area", stacked: true, toolbar: { show: false }, fontFamily: "inherit" },
        xaxis: { categories: dates, labels: { show: false } },
        yaxis: { labels: { formatter: (v: number) => v.toFixed(0) } },
        dataLabels: { enabled: false },
        stroke: { curve: "smooth", width: 2 },
        fill: { type: "gradient", gradient: { shadeIntensity: 0.1 } },
        colors: [chartColors.success, chartColors.danger],
        grid: { borderColor: "var(--border)", strokeDashArray: 3 },
        legend: { position: "top", fontSize: "11px" },
      }}
      series={[
        { name: "Entrées", data: recent.map((d) => d.entries) },
        { name: "Sorties", data: recent.map((d) => d.exits) },
      ]}
      type="area"
      height={220}
    />
  )
}

export function MarginsChart({ data }: { data: KpiMargins }) {
  const colors = [chartColors.primary, chartColors.success, chartColors.warning, chartColors.danger, chartColors.muted]
  return (
    <Chart
      options={{
        chart: { type: "donut", fontFamily: "inherit" },
        labels: data.categories.map((c) => c.category),
        dataLabels: { enabled: false },
        legend: { position: "bottom", fontSize: "11px" },
        colors,
        plotOptions: { pie: { donut: { size: "60%" } } },
        tooltip: { y: { formatter: (v: number) => `${v.toFixed(1)}%` } },
      }}
      series={data.categories.map((c) => Math.abs(c.marge_pct))}
      type="donut"
      height={220}
    />
  )
}
