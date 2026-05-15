import { Camera } from "lucide-react"
import { Card, CardContent } from "@/components/ui/card"

export function ScannerPage() {
  return (
    <div className="space-y-6">
      <div><h1 className="text-2xl font-bold tracking-tight">Scanner</h1><p className="text-sm text-muted-foreground">Scanner de codes-barres</p></div>
      <Card>
        <CardContent className="flex flex-col items-center justify-center py-16 text-muted-foreground gap-4">
          <Camera className="size-16 opacity-20" />
          <p className="text-sm">Le scanner sera disponible ici</p>
          <p className="text-xs">Utilisez un scanner USB ou l&apos;appareil photo du téléphone</p>
        </CardContent>
      </Card>
    </div>
  )
}