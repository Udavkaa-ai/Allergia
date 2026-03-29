package com.allergia.api

import android.content.Context
import android.net.Uri
import com.allergia.data.models.MealType
import com.allergia.utils.ImageUtils
import com.allergia.utils.PreferenceKeys
import com.allergia.utils.appDataStore
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodPhotoService @Inject constructor(
    private val api: OpenRouterApi,
    private val context: Context
) {
    private val gson = Gson()

    private suspend fun authHeader(): String {
        val key = context.appDataStore.data.map { it[PreferenceKeys.OPENROUTER_API_KEY] ?: "" }.first().trim()
        if (key.isBlank()) throw Exception("API ключ не настроен. Перейдите в Настройки.")
        return "Bearer $key"
    }

    /**
     * Анализирует фото еды:
     * 1. Сжимает и сохраняет снимок в 720p в приватное хранилище (архив до 7 дней).
     * 2. Отправляет изображение в Vision API.
     * 3. Возвращает распознанные продукты + путь к сохранённому файлу.
     */
    suspend fun recognizeFoodFromPhoto(imageUri: Uri): Result<FoodRecognitionResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                // 1. Сохранить сжатое фото в архив
                val savedFile = ImageUtils.compressAndSave(context, imageUri)
                // Прореживать архив (оставлять только 7 дней)
                ImageUtils.pruneOldPhotos(context, keepDays = 7)

                // 2. Конвертировать в base64 из сохранённого файла
                val base64 = ImageUtils.fileToBase64(savedFile)
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
                    model = Models.FOOD_PHOTO,
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

                if (response.error != null) {
                    val msg = response.error.message
                    throw Exception(
                        if (msg.contains("429") || response.error.code == "429")
                            "Превышен лимит запросов к модели. Подождите минуту и попробуйте снова."
                        else "Ошибка API: $msg"
                    )
                }

                val content = response.choices.firstOrNull()?.message?.content
                    ?: throw Exception("Пустой ответ от модели")

                parseFoodResponse(content, savedFile.absolutePath)
            }
        }

    private fun parseFoodResponse(raw: String, savedPhotoPath: String? = null): FoodRecognitionResult {
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
                rawResponse = raw,
                savedPhotoPath = savedPhotoPath
            )
        }.getOrElse {
            // Fallback: если JSON кривой — парсим текст эвристически
            FoodRecognitionResult(
                items = extractItemsFallback(raw),
                rawResponse = raw,
                savedPhotoPath = savedPhotoPath
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
