package com.allergia.api

import com.google.gson.annotations.SerializedName

// Request
data class OpenRouterRequest(
    val model: String = "google/gemini-2.5-flash-preview",
    val messages: List<ChatMessage>,
    val temperature: Double = 0.3,
    @SerializedName("max_tokens") val maxTokens: Int = 4096,
    val stream: Boolean = false
)

data class ChatMessage(
    val role: String,  // "system", "user", "assistant"
    val content: String
)

// Response
data class OpenRouterResponse(
    val id: String = "",
    val choices: List<Choice> = emptyList(),
    val usage: Usage? = null,
    val error: ApiError? = null
)

data class Choice(
    val index: Int = 0,
    val message: ChatMessage? = null,
    @SerializedName("finish_reason") val finishReason: String = ""
)

data class Usage(
    @SerializedName("prompt_tokens") val promptTokens: Int = 0,
    @SerializedName("completion_tokens") val completionTokens: Int = 0,
    @SerializedName("total_tokens") val totalTokens: Int = 0
)

data class ApiError(
    val message: String = "",
    val type: String = "",
    val code: String = ""
)
