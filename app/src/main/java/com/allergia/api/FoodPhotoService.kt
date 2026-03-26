package com.allergia.api

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.allergia.data.models.MealType
import com.allergia.utils.ImageUtils
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.photoDataStore by preferencesDataStore(name = "settings")
private val PHOTO_API_KEY_PREF = stringPreferencesKey("openrouter_api_key")

@Singleton
class FoodPhotoService @Inject constructor(
    private val api: OpenRouterApi,
    private val context: Context
) {
    private val gson = Gson()

    companion object {
        private const val VISION_MODEL = "google/gemma-3-4b-it:free"
    }

    private suspend fun authHeader(): String {
        val key = context.photoDataStore.data.map { it[PHOTO_API_KEY_PREF] ?: "" }.first()
        if (key.isBlank()) throw Exception("API ключ не настроен. Перейдите в Настройки.")
        return "Bearer $key"
    }

    /**
     * Анализирует фото еды и возвращает список распознанных блюд/продуктов.
     * [imageUri] — URI камеры или галереи.
     * [savedFile] — если изображение уже сжато и сохранено, передаётся путь.
     */
    suspend fun recognizeFoodFromPhoto(imageUri: Uri): Result<FoodRecognitionResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Сжать и закодировать в base64
                val base64 = ImageUtils.uriToBase64(context, imageUri)
                val dataUrl = "data:image/jpeg;base64,$base64"

                val systemPrompt = """
Ты — специалист по питанию и распознаванию еды на фотографиях.
Анализируй фото и определяй все блюда, продукты и напитки.
Отвечай ТОЛЬКО на русском языке в строгом JSON-формате без markdown.
                """.trimIndent()

                val userPrompt = """
На этом фото завтрак/обед/ужин/перекус.
Определи все продукты, блюда и напитки которые ты видишь.

Ответь строго в JSON (без ```json и без пояснений за пределами JSON):
{
  "mealGuess": "завтрак|обед|ужин|перекус|напиток",
  "items": [
    {
      "name": "Название продукта на русском",
      "estimatedAmount": "примерное количество, например: 1 порция, 200г, 1 стакан",
      "mealType": "BREAKFAST|LUNCH|DINNER|SNACK|DRINK|OTHER",
      "confidence": 0.95
    }
  ],
  "description": "Краткое описание блюда/приёма пищи одним предложением"
}

Правила:
- Указывай каждый отдельный продукт как отдельный элемент
- mealType выбирай по контексту (если явно чай — DRINK, утренняя еда — BREAKFAST и т.д.)
- confidence: насколько уверен в распознавании 0.0–1.0
- Если на фото не еда, верни пустой массив items
                """.trimIndent()

                val request = VisionRequest(
                    model = VISION_MODEL,
                    messages = listOf(
                        VisionMessage(
                            role = "user",
                            content = listOf(
                                VisionContentPart(type = "text", text = systemPrompt),
                                VisionContentPart(
                                    type = "image_url",
                                    imageUrl = ImageUrlData(url = dataUrl)
                                ),
                                VisionContentPart(type = "text", text = userPrompt)
                            )
                        )
                    ),
                    temperature = 0.1,
                    maxTokens = 1024
                )

                val response = api.visionCompletion(
                    authorization = authHeader(),
                    request = request
                )

                if (response.error != null) throw Exception("API Error: ${response.error.message}")

                val content = response.choices.firstOrNull()?.message?.content
                    ?: throw Exception("Пустой ответ от модели")

                parseFoodResponse(content)
            }
        }

    private fun parseFoodResponse(raw: String): FoodRecognitionResult {
        val cleaned = raw
            .replace(Regex("```json\\s*"), "")
            .replace(Regex("```\\s*"), "")
            .trim()

        return runCatching {
            val dto = gson.fromJson(cleaned, FoodRecognitionDto::class.java)
            FoodRecognitionResult(
                items = dto.items.map { item ->
                    DetectedFoodItem(
                        name = item.name,
                        estimatedAmount = item.estimatedAmount,
                        mealType = item.mealType,
                        isSelected = (item.confidence ?: 1f) >= 0.5f
                    )
                },
                rawResponse = raw
            )
        }.getOrElse {
            // Fallback: если JSON кривой — парсим текст эвристически
            FoodRecognitionResult(
                items = extractItemsFallback(raw),
                rawResponse = raw
            )
        }
    }

    /** Резервный парсинг: ищем строки вида «- продукт» или «• продукт» */
    private fun extractItemsFallback(text: String): List<DetectedFoodItem> {
        val lines = text.lines()
        return lines
            .filter { it.trimStart().startsWith("-") || it.trimStart().startsWith("•") }
            .map { line ->
                val name = line.trimStart('-', '•', ' ').trim()
                DetectedFoodItem(name = name, estimatedAmount = "", mealType = "OTHER")
            }
            .filter { it.name.isNotBlank() }
            .take(20)
    }
}

// DTO for parsing Gemma response
private data class FoodRecognitionDto(
    val mealGuess: String = "",
    val items: List<FoodItemDto> = emptyList(),
    val description: String = ""
)

private data class FoodItemDto(
    val name: String = "",
    val estimatedAmount: String = "",
    val mealType: String = "OTHER",
    val confidence: Float? = null
)
