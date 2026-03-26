package com.allergia.api

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.allergia.data.models.AllergenicIngredient
import com.allergia.data.models.LabelRecognitionResult
import com.allergia.utils.ImageUtils
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.labelDataStore by preferencesDataStore(name = "settings")
private val LABEL_API_KEY_PREF = stringPreferencesKey("openrouter_api_key")

@Singleton
class LabelPhotoService @Inject constructor(
    private val api: OpenRouterApi,
    private val context: Context
) {
    private val gson = Gson()

    companion object {
        private val VISION_MODEL = Models.LABEL_SCAN

        // Список ключевых аллергенных ингредиентов косметики для дообогащения результата
        val KNOWN_ALLERGENS = mapOf(
            "sodium lauryl sulfate" to "SLS — раздражает кожу и слизистые",
            "sodium laureth sulfate" to "SLES — потенциальный раздражитель",
            "methylisothiazolinone" to "MI — сильный контактный аллерген",
            "methylchloroisothiazolinone" to "MCI — сильный контактный аллерген",
            "parabens" to "Парабены — консерванты, возможные эндокринные дизрапторы",
            "methylparaben" to "Парабен — консервант",
            "propylparaben" to "Парабен — консервант",
            "butylparaben" to "Парабен — консервант",
            "ethylparaben" to "Парабен — консервант",
            "fragrance" to "Отдушка/Парфюм — частая причина контактного дерматита",
            "parfum" to "Парфюм — частая причина контактного дерматита",
            "formaldehyde" to "Формальдегид — консервант, канцероген",
            "dmdm hydantoin" to "Выделяет формальдегид",
            "imidazolidinyl urea" to "Выделяет формальдегид",
            "diazolidinyl urea" to "Выделяет формальдегид",
            "quaternium-15" to "Выделяет формальдегид",
            "propylene glycol" to "Может вызывать раздражение у чувствительной кожи",
            "lanolin" to "Ланолин — аллерген животного происхождения",
            "beeswax" to "Пчелиный воск — аллерген для части людей",
            "nickel" to "Никель — металлический аллерген",
            "chromium" to "Хром — металлический аллерген",
            "cocamidopropyl betaine" to "Может вызывать контактный дерматит",
            "benzalkonium chloride" to "Консервант, раздражитель кожи",
            "chlorhexidine" to "Антисептик, редкий но серьёзный аллерген",
            "triclosan" to "Антибактериальное, эндокринный дизраптор",
            "salicylic acid" to "Может раздражать кожу при высоких концентрациях",
            "retinol" to "Может вызывать раздражение и фотосенсибилизацию",
            "alpha arbutin" to "Депигментирующее, осторожно при чувствительной коже",
            "linalool" to "Терпеновый аромат — аллерген ЕС",
            "limonene" to "Цитрусовый аромат — аллерген ЕС",
            "eugenol" to "Аромат гвоздики — аллерген ЕС",
            "cinnamal" to "Коричный альдегид — аллерген ЕС",
            "geraniol" to "Цветочный аромат — аллерген ЕС",
            "citronellol" to "Аромат розы — аллерген ЕС",
            "amyl cinnamal" to "Аромат — аллерген ЕС",
            "benzyl alcohol" to "Консервант / аромат — аллерген ЕС",
            "benzyl salicylate" to "UV-фильтр / аромат — аллерген ЕС",
            "hexyl cinnamal" to "Аромат жасмина — аллерген ЕС"
        )
    }

    private suspend fun authHeader(): String {
        val key = context.labelDataStore.data.map { it[LABEL_API_KEY_PREF] ?: "" }.first()
        if (key.isBlank()) throw Exception("API ключ не настроен. Перейдите в Настройки.")
        return "Bearer $key"
    }

    /**
     * Анализирует фото состава продукта (этикетка, упаковка).
     * Возвращает список ингредиентов с выделенными аллергенами.
     */
    suspend fun recognizeIngredients(imageUri: Uri): Result<LabelRecognitionResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val base64 = ImageUtils.uriToBase64(context, imageUri)
                val dataUrl = "data:image/jpeg;base64,$base64"

                val userPrompt = """
На этом фото — этикетка или упаковка бытовой химии или косметики.

Твои задачи:
1. Прочитай ВСЕ ингредиенты (INCI-список или состав) с фото максимально точно
2. Выдели потенциально аллергенные компоненты
3. Оцени общий уровень аллергенности продукта

Ответь СТРОГО в JSON (без ```json, без пояснений вне JSON):
{
  "productName": "Название продукта если видно на упаковке",
  "ingredients": ["Ingredient 1", "Ingredient 2", "...все ингредиенты..."],
  "allergenicIngredients": [
    {
      "name": "Methylisothiazolinone",
      "commonName": "MI/консервант",
      "riskLevel": "high",
      "reason": "Сильный контактный аллерген, запрещён в несмываемых продуктах ЕС"
    }
  ],
  "allergenicityScore": 0.65,
  "allergenicityLabel": "Высокий|Средний|Низкий|Очень низкий",
  "summary": "Краткое описание состава: чем обусловлен уровень аллергенности",
  "rawIngredientText": "полный текст состава как на этикетке"
}

Шкала allergenicityScore: 0.0 = безопасен, 1.0 = сильный аллерген.
riskLevel: "high" | "medium" | "low"
Если текст на фото нечёткий — укажи что смог прочитать и отметь "(нечётко)" у спорных позиций.
                """.trimIndent()

                val request = VisionRequest(
                    model = VISION_MODEL,
                    messages = listOf(
                        VisionMessage(
                            role = "user",
                            content = listOf(
                                VisionContentPart(
                                    type = "image_url",
                                    imageUrl = ImageUrlData(url = dataUrl)
                                ),
                                VisionContentPart(type = "text", text = userPrompt)
                            )
                        )
                    ),
                    temperature = 0.05,   // максимально детерминированный — нужна точность чтения
                    maxTokens = 2048
                )

                val response = api.visionCompletion(
                    authorization = authHeader(),
                    request = request
                )

                if (response.error != null) throw Exception("API Error: ${response.error.message}")

                val content = response.choices.firstOrNull()?.message?.content
                    ?: throw Exception("Пустой ответ модели")

                parseLabelResponse(content)
            }
        }

    private fun parseLabelResponse(raw: String): LabelRecognitionResult {
        val cleaned = raw
            .replace(Regex("```json\\s*"), "")
            .replace(Regex("```\\s*"), "")
            .trim()

        return runCatching {
            val dto = gson.fromJson(cleaned, LabelResponseDto::class.java)

            // Дополнительная проверка: ищем известные аллергены в списке ингредиентов
            val extraAllergens = findKnownAllergens(dto.ingredients)
                .filter { extra -> dto.allergenicIngredients.none { it.name.equals(extra.name, ignoreCase = true) } }

            LabelRecognitionResult(
                productName = dto.productName,
                ingredients = dto.ingredients,
                allergenicIngredients = dto.allergenicIngredients.map {
                    AllergenicIngredient(it.name, it.commonName, it.riskLevel, it.reason)
                } + extraAllergens,
                allergenicityScore = dto.allergenicityScore.coerceIn(0f, 1f),
                allergenicityLabel = dto.allergenicityLabel,
                summary = dto.summary,
                rawIngredientText = dto.rawIngredientText.ifBlank { dto.ingredients.joinToString(", ") }
            )
        }.getOrElse {
            // Fallback: если JSON не распознан, вернуть сырой текст как один ингредиент-блок
            LabelRecognitionResult(
                productName = "",
                ingredients = listOf(raw.take(500)),
                allergenicIngredients = emptyList(),
                allergenicityScore = 0f,
                allergenicityLabel = "Не определено",
                summary = "Не удалось распознать состав. Попробуйте сфотографировать этикетку чётче.",
                rawIngredientText = raw
            )
        }
    }

    /** Поиск известных аллергенов в сыром списке ингредиентов */
    private fun findKnownAllergens(ingredients: List<String>): List<AllergenicIngredient> {
        val lowerIngredients = ingredients.map { it.lowercase() }
        return KNOWN_ALLERGENS
            .filter { (key, _) -> lowerIngredients.any { it.contains(key) } }
            .map { (key, description) ->
                val riskLevel = when {
                    key.contains("isothiazolinone") || key.contains("formaldehyde") ||
                    key.contains("dmdm") || key.contains("quaternium") -> "high"
                    key.contains("paraben") || key.contains("fragrance") ||
                    key.contains("parfum") || key.contains("sls") || key.contains("sles") -> "medium"
                    else -> "low"
                }
                AllergenicIngredient(
                    name = key.replaceFirstChar { it.uppercaseChar() },
                    commonName = "",
                    riskLevel = riskLevel,
                    reason = description
                )
            }
    }
}

// DTO for Gson parsing
private data class LabelResponseDto(
    val productName: String = "",
    val ingredients: List<String> = emptyList(),
    val allergenicIngredients: List<AllergenicIngredientDto> = emptyList(),
    val allergenicityScore: Float = 0f,
    val allergenicityLabel: String = "Низкий",
    val summary: String = "",
    val rawIngredientText: String = ""
)

private data class AllergenicIngredientDto(
    val name: String = "",
    val commonName: String = "",
    val riskLevel: String = "low",
    val reason: String = ""
)
