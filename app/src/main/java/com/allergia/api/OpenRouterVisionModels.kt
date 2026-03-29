package com.allergia.api

import com.google.gson.annotations.SerializedName

// Vision request — content is a list of parts (text + image)
data class VisionRequest(
    val model: String = Models.FOOD_PHOTO,
    val messages: List<VisionMessage>,
    val temperature: Double = 0.1,
    @SerializedName("max_tokens") val maxTokens: Int = 1024,
    val stream: Boolean = false
)

data class VisionMessage(
    val role: String,
    val content: List<VisionContentPart>
)

data class VisionContentPart(
    val type: String,                                           // "text" | "image_url"
    val text: String? = null,
    @SerializedName("image_url") val imageUrl: ImageUrlData? = null
)

data class ImageUrlData(
    val url: String                                            // "data:image/jpeg;base64,..."
)

// Parsed food recognition result
data class FoodRecognitionResult(
    val items: List<DetectedFoodItem>,
    val rawResponse: String,
    val savedPhotoPath: String? = null   // path to the locally saved 720p archive copy
)

data class DetectedFoodItem(
    val name: String,
    val estimatedAmount: String = "",
    val mealType: String = "OTHER",
    var isSelected: Boolean = true
)
