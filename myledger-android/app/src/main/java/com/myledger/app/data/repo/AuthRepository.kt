package com.myledger.app.data.repo

import com.google.gson.JsonObject
import com.myledger.app.data.remote.ApiClient
import com.myledger.app.data.remote.ApiException
import com.myledger.app.data.store.TokenStore
import retrofit2.HttpException

class AuthRepository(
    private val client: ApiClient,
    private val tokenStore: TokenStore,
) {
    suspend fun login(username: String, password: String): JsonObject {
        val body = JsonObject().apply {
            addProperty("username", username)
            addProperty("password", password)
        }
        try {
            val res = client.spring.login(body)
            val ok = res.get("success")?.asBoolean
            val data = res.getAsJsonObject("data")
            if (ok != true || data == null) {
                throw ApiException(res.get("message")?.asString ?: "暂时无法登录")
            }
            client.applyAuthData(data)
            return data
        } catch (e: HttpException) {
            throw mapLoginHttp(e)
        }
    }

    suspend fun bootstrapUser(): JsonObject? {
        val access = tokenStore.accessToken
        val refresh = tokenStore.refreshToken
        if (access.isNullOrBlank() && refresh.isNullOrBlank()) return null
        return try {
            if (!access.isNullOrBlank()) {
                val me = fetchMe()
                if (me != null) return me
            }
            if (!refresh.isNullOrBlank()) {
                client.refreshTokensOrThrow()
                fetchMe()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchMe(): JsonObject? {
        return try {
            val res = client.spring.me()
            if (res.get("success")?.asBoolean == true && res.get("data")?.isJsonObject == true) {
                res.getAsJsonObject("data")
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun logout() {
        try {
            val rt = tokenStore.refreshToken
            val body = if (rt.isNullOrBlank()) JsonObject() else JsonObject().apply {
                addProperty("refresh_token", rt)
            }
            client.spring.logout(body)
        } finally {
            tokenStore.clear()
        }
    }

    private fun mapLoginHttp(e: HttpException): ApiException {
        if (e.code() == 401) {
            return ApiException("账号或密码不对，请核对后再试；新用户可先注册")
        }
        val raw = e.response()?.errorBody()?.string().orEmpty()
        if (raw.length in 1..160) return ApiException(raw)
        return ApiException("暂时无法登录，请稍后再试")
    }
}
