package com.myledger.app.data.remote

import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

interface SpringAuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body body: JsonObject): JsonObject

    @POST("api/auth/refresh")
    suspend fun refresh(@Body body: JsonObject): JsonObject

    @POST("api/auth/logout")
    suspend fun logout(@Body body: JsonObject): JsonObject

    @GET("api/auth/me")
    suspend fun me(): JsonObject
}

interface DbfoundApi {
    @POST
    suspend fun post(@Url path: String, @Body body: JsonObject): JsonObject
}
