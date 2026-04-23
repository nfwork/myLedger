package com.myledger.app.data.repo

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.myledger.app.data.remote.ApiClient
import com.myledger.app.data.remote.dbfoundDatas
import com.myledger.app.data.remote.dbfoundTotalCounts
import com.myledger.app.data.remote.mapDbfoundHttp
import com.myledger.app.data.remote.unwrapDbfound
import retrofit2.HttpException

class LedgerRepository(private val client: ApiClient) {

    private suspend fun post(path: String, body: Map<String, Any?> = emptyMap()): JsonObject {
        val jo = JsonObject()
        body.forEach { (k, v) ->
            if (v == null) return@forEach
            when (v) {
                is Number -> jo.addProperty(k, v)
                is Boolean -> jo.addProperty(k, v)
                is String -> jo.addProperty(k, v)
                else -> jo.addProperty(k, v.toString())
            }
        }
        return try {
            client.dbfound.post(path, jo)
        } catch (e: HttpException) {
            throw mapDbfoundHttp(e)
        }
    }

    suspend fun register(username: String, password: String, nickname: String?) {
        val m = mutableMapOf<String, Any?>(
            "username" to username,
            "password" to password,
        )
        if (!nickname.isNullOrBlank()) m["nickname"] = nickname
        val data = post("/user/user.execute!register", m)
        unwrapDbfound(data)
    }

    suspend fun updateProfile(nickname: String) {
        val data = post("/user/user.execute!updateProfile", mapOf("nickname" to nickname))
        unwrapDbfound(data)
    }

    suspend fun changePassword(oldPassword: String, newPassword: String) {
        val data = post(
            "/user/user.execute!changePassword",
            mapOf("old_password" to oldPassword, "new_password" to newPassword),
        )
        unwrapDbfound(data)
    }

    suspend fun listAccounts(): JsonArray {
        val data = post("/ledger_settings/account.query!listByUser", emptyMap())
        unwrapDbfound(data)
        return dbfoundDatas(data)
    }

    suspend fun addAccount(name: String, sortOrder: Int) {
        unwrapDbfound(post("/ledger_settings/account.execute!add", mapOf("name" to name, "sort_order" to sortOrder)))
    }

    suspend fun updateAccount(id: Long, name: String, sortOrder: Int?) {
        val m = mutableMapOf<String, Any?>("id" to id, "name" to name)
        if (sortOrder != null) m["sort_order"] = sortOrder
        unwrapDbfound(post("/ledger_settings/account.execute!update", m))
    }

    suspend fun setDefaultAccount(id: Long) {
        unwrapDbfound(post("/ledger_settings/account.execute!setDefault", mapOf("id" to id)))
    }

    suspend fun deleteAccount(id: Long) {
        unwrapDbfound(post("/ledger_settings/account.execute!delete", mapOf("id" to id)))
    }

    suspend fun listCategories(type: String): JsonArray {
        val data = post("/ledger_settings/category.query!list", mapOf("type" to type))
        unwrapDbfound(data)
        return dbfoundDatas(data)
    }

    suspend fun addCategory(name: String, type: String, sortOrder: Int) {
        unwrapDbfound(
            post(
                "/ledger_settings/category.execute!add",
                mapOf("name" to name, "type" to type, "sort_order" to sortOrder),
            ),
        )
    }

    suspend fun updateCategory(id: Long, name: String, sortOrder: Int?) {
        val m = mutableMapOf<String, Any?>("id" to id, "name" to name)
        if (sortOrder != null) m["sort_order"] = sortOrder
        unwrapDbfound(post("/ledger_settings/category.execute!update", m))
    }

    suspend fun deleteCategory(id: Long) {
        unwrapDbfound(post("/ledger_settings/category.execute!delete", mapOf("id" to id)))
    }

    suspend fun monthTotals(yearMonth: String, accountId: Long?): JsonObject {
        val m = mutableMapOf<String, Any?>("year_month" to yearMonth)
        if (accountId != null) m["account_id"] = accountId
        val data = post("/report/summary.query!monthTotals", m)
        unwrapDbfound(data)
        val arr = dbfoundDatas(data)
        return if (arr.size() > 0) arr[0].asJsonObject else JsonObject()
    }

    suspend fun categoryTotals(yearMonth: String, entryType: String, accountId: Long?): JsonArray {
        val m = mutableMapOf<String, Any?>(
            "year_month" to yearMonth,
            "entry_type" to entryType,
        )
        if (accountId != null) m["account_id"] = accountId
        val data = post("/report/summary.query!byCategory", m)
        unwrapDbfound(data)
        return dbfoundDatas(data)
    }

    suspend fun matrixByMonthCategory(
        from: String,
        to: String,
        entryType: String,
        accountId: Long?,
    ): JsonArray {
        val m = mutableMapOf<String, Any?>(
            "year_month_from" to from,
            "year_month_to" to to,
            "entry_type" to entryType,
        )
        if (accountId != null) m["account_id"] = accountId
        val data = post("/report/summary.query!matrixByMonthCategory", m)
        unwrapDbfound(data)
        return dbfoundDatas(data)
    }

    suspend fun entryList(body: Map<String, Any?>): JsonArray {
        val data = post("/bookkeeping/entry.query!list", body)
        unwrapDbfound(data)
        return dbfoundDatas(data)
    }

    suspend fun entryListPage(body: Map<String, Any?>): Pair<JsonArray, Int> {
        val data = post("/bookkeeping/entry.query!list", body)
        unwrapDbfound(data)
        val rows = dbfoundDatas(data)
        val t = dbfoundTotalCounts(data)
        val total = if (t >= 0) t else rows.size()
        return rows to total
    }

    suspend fun entryById(id: Long): JsonObject? {
        val data = post("/bookkeeping/entry.query!getById", mapOf("id" to id))
        unwrapDbfound(data)
        val arr = dbfoundDatas(data)
        return if (arr.size() > 0) arr[0].asJsonObject else null
    }

    suspend fun addEntry(body: Map<String, Any?>) {
        unwrapDbfound(post("/bookkeeping/entry.execute!add", body))
    }

    suspend fun updateEntry(body: Map<String, Any?>) {
        unwrapDbfound(post("/bookkeeping/entry.execute!update", body))
    }

    suspend fun deleteEntry(id: Long) {
        unwrapDbfound(post("/bookkeeping/entry.execute!delete", mapOf("id" to id)))
    }
}
