package com.myledger.app.domain

import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

private val cny: NumberFormat = NumberFormat.getCurrencyInstance(Locale.CHINA).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}

fun formatMoney(n: Double?): String {
    if (n == null || n.isNaN()) return "—"
    return cny.format(n)
}

fun currentYearMonth(): String {
    val c = Calendar.getInstance()
    val y = c.get(Calendar.YEAR)
    val m = c.get(Calendar.MONTH) + 1
    return String.format(Locale.US, "%04d-%02d", y, m)
}

fun shiftYearMonth(ym: String, delta: Int): String {
    val p = ym.split("-")
    if (p.size < 2) return ym
    val y = p[0].toIntOrNull() ?: return ym
    val mo = p[1].toIntOrNull() ?: return ym
    val c = Calendar.getInstance()
    c.set(Calendar.YEAR, y)
    c.set(Calendar.MONTH, mo - 1 + delta)
    c.set(Calendar.DAY_OF_MONTH, 1)
    return String.format(Locale.US, "%04d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1)
}

fun formatDateDisplay(s: String?): String {
    if (s.isNullOrBlank()) return ""
    return s.take(10)
}

fun formatYearMonthLabel(ym: String): String {
    val p = ym.split("-")
    if (p.size < 2) return ym
    val y = p[0]
    val mo = p[1].toIntOrNull() ?: return ym
    return "${y}年${mo}月"
}

fun monthLabelFromYm(ym: String): String {
    val mo = ym.split("-").getOrNull(1)?.toIntOrNull()
    return if (mo != null) "${mo}月" else ym
}

fun monthsOfCalendarYear(year: Int): List<String> {
    val y = String.format(Locale.US, "%04d", year)
    return (1..12).map { m -> String.format(Locale.US, "%s-%02d", y, m) }
}

fun visibleMonthsForStats(year: Int): List<String> {
    val full = monthsOfCalendarYear(year)
    val c = Calendar.getInstance()
    val cy = c.get(Calendar.YEAR)
    val cm = c.get(Calendar.MONTH) + 1
    return when {
        year < cy -> full
        year > cy -> full
        else -> full.take(cm)
    }
}
