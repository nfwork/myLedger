package com.myledger.app.data.store

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenStore(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "myledger_tokens",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS, null)?.takeIf { it.isNotBlank() }
        set(v) {
            prefs.edit().apply {
                if (v.isNullOrBlank()) remove(KEY_ACCESS) else putString(KEY_ACCESS, v)
            }.apply()
        }

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH, null)?.takeIf { it.isNotBlank() }
        set(v) {
            prefs.edit().apply {
                if (v.isNullOrBlank()) remove(KEY_REFRESH) else putString(KEY_REFRESH, v)
            }.apply()
        }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun applyAuthPayload(
        access: String?,
        refresh: String?,
    ) {
        if (!access.isNullOrBlank()) accessToken = access
        if (!refresh.isNullOrBlank()) refreshToken = refresh
    }

    companion object {
        private const val KEY_ACCESS = "myledger.access_token"
        private const val KEY_REFRESH = "myledger.refresh_token"
    }
}
