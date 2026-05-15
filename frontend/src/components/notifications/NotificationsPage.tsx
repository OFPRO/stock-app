import { useEffect, useState, useCallback } from "react"
import { Bell, CheckCheck, Check } from "lucide-react"
import { getNotifications, markNotificationRead, markAllNotificationsRead, type Notification } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"

const TYPE_ICONS: Record<string, string> = { low_stock: "🔴", out_of_stock: "⛔", expiring: "⚠️", order: "📦", system: "ℹ️" }

export function NotificationsPage() {
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    setLoading(true)
    try { const res = await getNotifications(); setNotifications(res.notifications) }
    catch (err) { console.error(err) }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { load() }, [load])

  const handleMarkRead = async (id: number) => {
    await markNotificationRead(id)
    setNotifications(prev => prev.map(n => n.id === id ? { ...n, is_read: 1 } : n))
  }

  const handleMarkAllRead = async () => {
    await markAllNotificationsRead()
    setNotifications(prev => prev.map(n => ({ ...n, is_read: 1 })))
  }

  const unread = notifications.filter(n => !n.is_read).length

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div><h1 className="text-2xl font-bold tracking-tight">Notifications</h1><p className="text-sm text-muted-foreground">{unread} non lue{unread !== 1 ? "s" : ""}</p></div>
        {unread > 0 && <Button variant="outline" onClick={handleMarkAllRead}><CheckCheck className="size-4" />Tout marquer comme lu</Button>}
      </div>
      {loading ? <div className="space-y-2">{Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} className="h-16 w-full rounded-lg" />)}</div>
      : <div className="space-y-1">
        {notifications.length === 0 ? <Card><CardContent className="flex items-center justify-center py-12 text-muted-foreground"><Bell className="size-8 mb-2 opacity-30" /><p>Aucune notification</p></CardContent></Card>
        : notifications.map(n => (
          <Card key={n.id} className={n.is_read ? "opacity-60" : ""}>
            <CardContent className="flex items-start gap-3 py-3">
              <span className="text-lg">{TYPE_ICONS[n.type] ?? "ℹ️"}</span>
              <div className="flex-1 min-w-0">
                <p className="text-sm">{n.message}</p>
                {n.product_name && <p className="text-xs text-muted-foreground mt-0.5">{n.product_name}</p>}
                <p className="text-xs text-muted-foreground mt-0.5">{n.created_at.slice(0, 16)}</p>
              </div>
              {!n.is_read && <Button variant="ghost" size="icon-sm" onClick={() => handleMarkRead(n.id)}><Check className="size-3.5" /></Button>}
            </CardContent>
          </Card>
        ))}
      </div>}
    </div>
  )
}