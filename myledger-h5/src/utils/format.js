export function formatMoney(n) {
  const v = Number(n)
  if (Number.isNaN(v)) return '—'
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(v)
}

export function currentYearMonth(d = new Date()) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  return `${y}-${m}`
}

export function shiftYearMonth(ym, delta) {
  const [y, m] = ym.split('-').map(Number)
  const dt = new Date(y, m - 1 + delta, 1)
  return currentYearMonth(dt)
}

/** 表头用：`2026-04` → `26-04`（紧凑、等宽友好） */
export function formatYmCompact(ym) {
  const [y, m] = String(ym || '').split('-')
  if (!y || !m) return String(ym || '')
  return `${y.slice(2)}-${m}`
}

/** `2026-04` → `2026年4月` */
export function formatYearMonthLabel(ym) {
  const parts = String(ym || '').split('-')
  if (parts.length < 2) return ym || '—'
  const y = parts[0]
  const mo = Number(parts[1])
  if (!y || Number.isNaN(mo)) return ym || '—'
  return `${y}年${mo}月`
}

export function formatDateDisplay(s) {
  if (!s) return ''
  return String(s).slice(0, 10)
}

/** 用于统计表：2026-04-03 → 4月3日 */
export function formatMonthDayLabel(s) {
  const d = String(s || '').slice(0, 10)
  if (d.length < 10) return d || '—'
  const [, m, day] = d.split('-')
  return `${Number(m)}月${Number(day)}日`
}
