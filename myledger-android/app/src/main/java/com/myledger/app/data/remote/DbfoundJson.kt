package com.myledger.app.data.remote

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import retrofit2.HttpException

fun JsonArray.mapJsonObjects(): List<JsonObject> = (0 until size()).map { get(it).asJsonObject }

fun unwrapDbfound(json: JsonObject) {
    val ok = json.get("success")?.takeIf { it.isJsonPrimitive && !it.isJsonNull }?.asBoolean
    if (ok == false) {
        val msg = json.get("message")?.asStringOrNull() ?: json.get("msg")?.asStringOrNull() ?: "请求失败"
        throw ApiException(msg)
    }
}

fun mapDbfoundHttp(e: HttpException): ApiException {
    return try {
        val raw = e.response()?.errorBody()?.string().orEmpty()
        val json = com.google.gson.JsonParser.parseString(raw)
        if (json.isJsonObject) {
            val jo = json.asJsonObject
            val msg = jo.get("message")?.asStringOrNull() ?: jo.get("msg")?.asStringOrNull() ?: "HTTP ${e.code()}"
            ApiException(msg)
        } else {
            ApiException("接口异常 (${e.code()})")
        }
    } catch (_: Exception) {
        ApiException("网络请求错误 (${e.code()})")
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
    return if (t != null && t.isJsonPrimitive && t.asJsonPrimitive.isNumber && !t.isJsonNull) {
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
