package com.allergia.api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenRouterApi {
    @POST("chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Header("HTTP-Referer") referer: String = "https://allergia.app",
        @Header("X-Title") title: String = "Allergia",
        @Body request: OpenRouterRequest
    ): OpenRouterResponse

    @POST("chat/completions")
    suspend fun visionCompletion(
        @Header("Authorization") authorization: String,
        @Header("HTTP-Referer") referer: String = "https://allergia.app",
        @Header("X-Title") title: String = "Allergia",
        @Body request: VisionRequest
    ): OpenRouterResponse
}
