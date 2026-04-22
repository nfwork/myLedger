package com.myledger.app.data.remote

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.myledger.app.BuildConfig
import com.myledger.app.data.store.TokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient private constructor(
    val gson: Gson,
    val spring: SpringAuthApi,
    val dbfound: DbfoundApi,
    private val tokenStore: TokenStore,
    val refreshOnlySpring: SpringAuthApi,
) {

    suspend fun refreshTokensOrThrow() {
        val rt = tokenStore.refreshToken ?: throw ApiException("no refresh token")
        val body = JsonObject().apply { addProperty("refresh_token", rt) }
        try {
            val res = refreshOnlySpring.refresh(body)
            val ok = res.get("success")?.asBoolean
            val data = res.getAsJsonObject("data")
            if (ok != true || data == null) {
                throw ApiException(res.get("message")?.asString ?: "刷新失败")
            }
            applyAuthData(data)
        } catch (e: HttpException) {
            if (e.code() == 401) tokenStore.clear()
            throw ApiException(e.message ?: "刷新失败")
        }
    }

    fun applyAuthData(data: JsonObject) {
        val at = data.get("access_token")?.asString
        val rt = data.get("refresh_token")?.asString
        tokenStore.applyAuthPayload(at, rt)
    }

    companion object {
        fun create(context: Context, tokenStore: TokenStore): ApiClient {
            val gson: Gson = GsonBuilder().serializeNulls().create()
            val baseUrl = BuildConfig.API_BASE

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val refreshClient = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            val refreshRetrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(refreshClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
            val refreshApi = refreshRetrofit.create(SpringAuthApi::class.java)

            val authInterceptor = Interceptor { chain ->
                val original = chain.request()
                val path = original.url.encodedPath
                if (path.endsWith("/api/auth/refresh")) {
                    return@Interceptor chain.proceed(original)
                }
                val authed = tokenStore.accessToken?.let { at ->
                    original.newBuilder().header("Authorization", "Bearer $at").build()
                } ?: original
                var response = chain.proceed(authed)
                if (response.code != 401) return@Interceptor response
                if (path.endsWith("/api/auth/login") || path.endsWith("/api/auth/refresh")) {
                    return@Interceptor response
                }
                if (tokenStore.refreshToken.isNullOrBlank()) {
                    return@Interceptor response
                }
                if (original.header("X-Auth-Retry") != null) {
                    return@Interceptor response
                }
                response.close()
                val refreshed = runCatching {
                    runBlocking { tryRefresh(refreshApi, tokenStore) }
                }.getOrElse { e ->
                    if (e is HttpException && e.code() == 401) tokenStore.clear()
                    false
                }
                if (refreshed && !tokenStore.accessToken.isNullOrBlank()) {
                    val retry = original.newBuilder()
                        .header("Authorization", "Bearer ${tokenStore.accessToken}")
                        .header("X-Auth-Retry", "1")
                        .build()
                    return@Interceptor chain.proceed(retry)
                }
                chain.proceed(authed)
            }

            val mainClient = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .build()

            val mainRetrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(mainClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

            return ApiClient(
                gson = gson,
                spring = mainRetrofit.create(SpringAuthApi::class.java),
                dbfound = mainRetrofit.create(DbfoundApi::class.java),
                tokenStore = tokenStore,
                refreshOnlySpring = refreshApi,
            )
        }

        private suspend fun tryRefresh(refreshApi: SpringAuthApi, tokenStore: TokenStore): Boolean {
            val rt = tokenStore.refreshToken ?: return false
            return try {
                val body = JsonObject().apply { addProperty("refresh_token", rt) }
                val res = refreshApi.refresh(body)
                if (res.get("success")?.asBoolean == true && res.get("data")?.isJsonObject == true) {
                    val data = res.getAsJsonObject("data")
                    val at = data.get("access_token")?.asString
                    val nrt = data.get("refresh_token")?.asString
                    tokenStore.applyAuthPayload(at, nrt)
                    true
                } else {
                    false
                }
            } catch (e: HttpException) {
                if (e.code() == 401) tokenStore.clear()
                false
            }
        }
    }
}
