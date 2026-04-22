package com.myledger.app.domain

import com.google.gson.JsonObject

data class MatrixCategory(val id: Long, val name: String, val byMonth: Map<String, Double>, val total: Double)

data class MatrixRow(val ym: String, val monthLabel: String, val amounts: List<Double>, val rowTotal: Double)

data class BuiltMatrix(
    val rows: List<MatrixRow>,
    val cols: List<MatrixCategory>,
    val colTotals: List<Double>,
    val grand: Double,
)

fun catAmount(row: JsonObject): Double {
    val v = row.get("total_amount") ?: row.get("totalAmount")
    return when {
        v == null || v.isJsonNull -> 0.0
        v.isJsonPrimitive && v.asJsonPrimitive.isNumber -> v.asDouble
        else -> v.asString.toDoubleOrNull() ?: 0.0
    }
}

fun buildMatrix(flatRows: List<JsonObject>, monthsList: List<String>): BuiltMatrix {
    data class CatAcc(var id: Long, var name: String, val byMonth: MutableMap<String, Double> = mutableMapOf())

    val catMap = LinkedHashMap<Long, CatAcc>()
    for (r in flatRows) {
        val id = r.get("category_id")?.takeIf { !it.isJsonNull }?.asLong
            ?: r.get("categoryId")?.takeIf { !it.isJsonNull }?.asLong
            ?: continue
        val name = r.get("category_name")?.asString ?: r.get("categoryName")?.asString ?: "—"
        val ym = r.get("bill_month")?.asString ?: r.get("billMonth")?.asString ?: ""
        val amt = catAmount(r)
        val acc = catMap.getOrPut(id) { CatAcc(id, name) }
        acc.byMonth[ym] = (acc.byMonth[ym] ?: 0.0) + amt
    }

    val cats = catMap.values.map { c ->
        val total = monthsList.sumOf { m -> c.byMonth[m] ?: 0.0 }
        MatrixCategory(c.id, c.name, c.byMonth.toMap(), total)
    }.sortedByDescending { it.total }

    val rows = monthsList.map { ym ->
        val amounts = cats.map { c -> c.byMonth[ym] ?: 0.0 }
        val rowTotal = amounts.sum()
        MatrixRow(ym, monthLabelFromYm(ym), amounts, rowTotal)
    }

    val colTotals = cats.indices.map { j -> rows.sumOf { it.amounts[j] } }
    val grand = colTotals.sum()

    return BuiltMatrix(rows, cats, colTotals, grand)
}
