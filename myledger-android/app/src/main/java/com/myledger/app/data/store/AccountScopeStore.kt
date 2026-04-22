package com.myledger.app.data.store

import android.content.Context

/** 与 H5 `accountFilterScope.js` 一致：按资金账户筛选（null = 全部） */
class AccountScopeStore(context: Context) {
    private val prefs = context.getSharedPreferences("myledger_prefs", Context.MODE_PRIVATE)

    fun getScopeAccountId(): Long? {
        if (!prefs.contains(KEY)) return null
        val v = prefs.getLong(KEY, -1L)
        return if (v <= 0L) null else v
    }

    fun setScopeAccountId(id: Long?) {
        prefs.edit().apply {
            if (id == null || id <= 0L) remove(KEY) else putLong(KEY, id)
        }.apply()
    }

    companion object {
        private const val KEY = "myledger_scope_account_id"
    }
}
