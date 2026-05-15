import { cn } from "@/lib/utils"

interface NativeSelectProps {
  value: string
  onChange: (v: string) => void
  placeholder?: string
  options: { value: string; label: string }[]
  className?: string
}

export function NativeSelect({ value, onChange, placeholder, options, className }: NativeSelectProps) {
  return (
    <select
      value={value}
      onChange={(e) => onChange(e.target.value)}
      className={cn(
        "h-8 w-full min-w-0 rounded-lg border border-input bg-transparent px-2.5 py-1 text-base transition-colors outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 md:text-sm dark:bg-input/30",
        className
      )}
    >
      {placeholder && <option value="" disabled>{placeholder}</option>}
      {options.map((opt) => (
        <option key={opt.value} value={opt.value}>{opt.label}</option>
      ))}
    </select>
  )
}
