package com.myledger.app.data.remote

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

fun JsonArray.mapJsonObjects(): List<JsonObject> = (0 until size()).map { get(it).asJsonObject }

fun unwrapDbfound(json: JsonObject) {
    val ok = json.get("success")?.takeIf { it.isJsonPrimitive }?.asBoolean
    if (ok == false) {
        val msg = json.get("message")?.asString ?: json.get("msg")?.asString ?: "请求失败"
        throw ApiException(msg)
    }
}

fun dbfoundDatas(json: JsonObject): JsonArray {
    unwrapDbfound(json)
    val datas = json.get("datas")
    if (datas != null && datas.isJsonArray) return datas.asJsonArray
    val data = json.get("data")
    if (data != null && data.isJsonObject) {
        val inner = data.asJsonObject.get("datas")
        if (inner != null && inner.isJsonArray) return inner.asJsonArray
    }
    return JsonArray()
}

fun dbfoundTotalCounts(json: JsonObject): Int {
    val t = json.get("total_counts") ?: json.get("totalCounts")
    return if (t != null && t.isJsonPrimitive && t.asJsonPrimitive.isNumber) {
        t.asInt
    } else {
        -1
    }
}

fun JsonObject.optString(key: String): String? =
    this.get(key)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asString

fun JsonObject.optLong(key: String): Long? {
    val e = this.get(key) ?: return null
    if (e.isJsonNull) return null
    return when {
        e.isJsonPrimitive && e.asJsonPrimitive.isNumber -> e.asLong
        e.isJsonPrimitive -> e.asString.toLongOrNull()
        else -> null
    }
}

fun JsonObject.optDouble(key: String): Double? {
    val e = this.get(key) ?: return null
    if (e.isJsonNull) return null
    return when {
        e.isJsonPrimitive && e.asJsonPrimitive.isNumber -> e.asDouble
        e.isJsonPrimitive -> e.asString.toDoubleOrNull()
        else -> null
    }
}

fun JsonObject.optInt(key: String): Int? {
    val e = this.get(key) ?: return null
    if (e.isJsonNull) return null
    return when {
        e.isJsonPrimitive && e.asJsonPrimitive.isNumber -> e.asInt
        e.isJsonPrimitive -> e.asString.toIntOrNull()
        else -> null
    }
}

fun JsonElement?.asStringOrNull(): String? =
    this?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asString
