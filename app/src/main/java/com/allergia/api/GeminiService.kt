package com.allergia.api

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.allergia.data.models.*
import com.allergia.data.repository.DiaryRangeData
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")
private val API_KEY_PREF = stringPreferencesKey("openrouter_api_key")

@Singleton
class GeminiService @Inject constructor(
    private val api: OpenRouterApi,
    private val context: Context
) {
    private val gson = Gson()
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    private suspend fun authHeader(): String {
        val key = context.dataStore.data.map { it[API_KEY_PREF] ?: "" }.first()
        if (key.isBlank()) throw Exception("API ключ не настроен. Перейдите в Настройки и введите ключ OpenRouter.")
        return "Bearer $key"
    }

    // ─── System prompt ─────────────────────────────────────────────────────────

    private val allergySystemPrompt = """
Ты — специализированный ИИ-помощник по аллергологии и диетологии.
Твоя задача — анализировать дневник питания и симптомов пациента для выявления возможных аллергенов и паттернов аллергических реакций.

ПРИНЦИПЫ АНАЛИЗА:
1. Применяй причинно-следственный подход: ищи корреляции между едой/медикаментами и симптомами с задержкой 0–48 часов.
2. Учитывай перекрёстную реактивность аллергенов (например, берёза → яблоки, орехи).
3. Оценивай накопительный эффект: повторное воздействие аллергена может усиливать реакцию.
4. Разграничивай IgE-опосредованную аллергию и пищевую непереносимость.
5. Принимай во внимание антигистаминные препараты — они могут маскировать симптомы.
6. Всегда указывай ВЕРОЯТНОСТЬ в процентах для каждого подозреваемого триггера.
7. Рекомендуй элиминационные диеты как метод подтверждения.
8. Подчёркивай необходимость консультации аллерголога для официальной диагностики.

ФОРМАТ ОТВЕТА: Структурированный JSON + подробное текстовое объяснение на русском языке.
    """.trimIndent()

    // ─── Allergy analysis ──────────────────────────────────────────────────────

    suspend fun analyzeAllergyPatterns(data: DiaryRangeData): Result<AllergyAnalysisResponse> {
        return runCatching {
            val diaryContext = buildDiaryContext(data)

            val userPrompt = """
ДНЕВНИК АЛЛЕРГИКА за период ${data.periodStart.format(dateFormatter)} — ${data.periodEnd.format(dateFormatter)}:

$diaryContext

ЗАДАЧА: Проведи детальный анализ и ответь строго в следующем JSON-формате (без markdown, только чистый JSON):
{
  "summary": "Краткий вывод 2-3 предложения",
  "suspectedTriggers": [
    {
      "name": "Название продукта/медикамента",
      "type": "food|medication|environmental",
      "probability": 0.75,
      "confidenceLabel": "Высокая|Средняя|Низкая",
      "evidence": "Описание доказательств из дневника",
      "correlationDays": [1, 3, 5]
    }
  ],
  "patterns": [
    {
      "description": "Описание выявленного паттерна",
      "frequency": "ежедневно|несколько раз в неделю|редко"
    }
  ],
  "skinDynamics": "Анализ динамики состояния кожи",
  "medicationEffect": "Влияние принимаемых медикаментов на симптомы",
  "crossReactivity": ["возможные перекрёстные аллергены"],
  "eliminationDietSuggestion": "Рекомендация по элиминационной диете",
  "overallRisk": "high|medium|low",
  "recommendations": [
    "Конкретная рекомендация 1",
    "Конкретная рекомендация 2"
  ],
  "disclaimer": "Данный анализ носит информационный характер и не заменяет консультацию врача-аллерголога."
}
            """.trimIndent()

            val response = api.chatCompletion(
                authorization = authHeader(),
                request = OpenRouterRequest(
                    messages = listOf(
                        ChatMessage("system", allergySystemPrompt),
                        ChatMessage("user", userPrompt)
                    ),
                    temperature = 0.2
                )
            )

            if (response.error != null) throw Exception("API Error: ${response.error.message}")

            val content = response.choices.firstOrNull()?.message?.content
                ?: throw Exception("Empty response from API")

            val cleanJson = cleanJson(content)
            gson.fromJson(cleanJson, AllergyAnalysisResponse::class.java)
        }
    }

    // ─── Product allergenicity ─────────────────────────────────────────────────

    suspend fun assessProductAllergenicity(productName: String): Result<ProductAllergenicityResponse> {
        return runCatching {
            val prompt = """
Оцени аллергенность продукта питания: "$productName"

Ответь строго в JSON (без markdown):
{
  "productName": "$productName",
  "score": 0.85,
  "label": "Высокий|Средний|Низкий|Очень низкий",
  "allergens": ["глютен", "молоко", "яйца"],
  "histaminContent": "высокое|среднее|низкое|нет данных",
  "crossReactiveWith": ["другие продукты с перекрёстной реактивностью"],
  "commonReactions": ["крапивница", "отёк Квинке"],
  "safeAlternatives": ["безопасные аналоги для аллергиков"],
  "description": "Подробное описание аллергенности продукта (3-5 предложений)",
  "sources": "ВОЗ / EAACI / научные данные"
}

Шкала score: 0.0 = не аллерген, 1.0 = сильный аллерген.
Используй актуальные данные аллергологии (ВОЗ, EAACI, FDA).
            """.trimIndent()

            val response = api.chatCompletion(
                authorization = authHeader(),
                request = OpenRouterRequest(
                    messages = listOf(
                        ChatMessage("system", allergySystemPrompt),
                        ChatMessage("user", prompt)
                    ),
                    temperature = 0.1
                )
            )

            if (response.error != null) throw Exception("API Error: ${response.error.message}")

            val content = response.choices.firstOrNull()?.message?.content
                ?: throw Exception("Empty response")

            gson.fromJson(cleanJson(content), ProductAllergenicityResponse::class.java)
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private fun buildDiaryContext(data: DiaryRangeData): String {
        val sb = StringBuilder()

        // Group data by date
        val allDates = (data.foods.map { it.entryDate } +
                data.medications.map { it.entryDate } +
                data.skinConditions.map { it.entryDate } +
                data.symptoms.map { it.entryDate }).toSortedSet()

        allDates.forEach { date ->
            sb.appendLine("═══ ${date.format(dateFormatter)} ═══")

            val foods = data.foods.filter { it.entryDate == date }
            if (foods.isNotEmpty()) {
                sb.appendLine("🍽 Питание:")
                foods.groupBy { it.mealType }.forEach { (meal, items) ->
                    sb.append("  ${meal.displayName}: ")
                    sb.appendLine(items.joinToString(", ") { item ->
                        buildString {
                            append(item.name)
                            if (item.amount.isNotBlank()) append(" (${item.amount})")
                            if (item.allergenicityScore != null) append(" [аллергенность: ${(item.allergenicityScore * 100).toInt()}%]")
                        }
                    })
                }
            }

            val meds = data.medications.filter { it.entryDate == date }
            if (meds.isNotEmpty()) {
                sb.appendLine("💊 Медикаменты:")
                meds.forEach { med ->
                    sb.append("  • ${med.name}")
                    if (med.dose.isNotBlank()) sb.append(", ${med.dose}")
                    if (med.isAntihistamine) sb.append(" [антигистаминный]")
                    sb.appendLine()
                }
            }

            val skin = data.skinConditions.find { it.entryDate == date }
            if (skin != null) {
                sb.appendLine("🔬 Состояние кожи: тяжесть ${skin.overallSeverity}/3")
                if (skin.itching > 0) sb.appendLine("  • Зуд: ${skin.itching}/3")
                if (skin.redness > 0) sb.appendLine("  • Покраснение: ${skin.redness}/3")
                if (skin.rash > 0) sb.appendLine("  • Сыпь: ${skin.rash}/3")
                if (skin.swelling > 0) sb.appendLine("  • Отёк: ${skin.swelling}/3")
                if (skin.affectedAreas.isNotBlank()) sb.appendLine("  • Зоны: ${skin.affectedAreas}")
            }

            val symptoms = data.symptoms.filter { it.entryDate == date }
            if (symptoms.isNotEmpty()) {
                sb.appendLine("🤧 Симптомы:")
                symptoms.forEach { s ->
                    sb.appendLine("  • ${s.symptomType.displayName}: ${s.severity}/3")
                }
            }

            sb.appendLine()
        }

        return sb.toString()
    }

    private fun cleanJson(text: String): String {
        // Strip markdown code blocks if present
        return text
            .replace(Regex("```json\\s*"), "")
            .replace(Regex("```\\s*"), "")
            .trim()
    }
}

// ─── Response DTOs ─────────────────────────────────────────────────────────────

data class AllergyAnalysisResponse(
    val summary: String = "",
    val suspectedTriggers: List<TriggerSuspectDto> = emptyList(),
    val patterns: List<PatternDto> = emptyList(),
    val skinDynamics: String = "",
    val medicationEffect: String = "",
    val crossReactivity: List<String> = emptyList(),
    val eliminationDietSuggestion: String = "",
    val overallRisk: String = "low",
    val recommendations: List<String> = emptyList(),
    val disclaimer: String = ""
)

data class TriggerSuspectDto(
    val name: String = "",
    val type: String = "food",
    val probability: Float = 0f,
    val confidenceLabel: String = "Низкая",
    val evidence: String = "",
    val correlationDays: List<Int> = emptyList()
)

data class PatternDto(
    val description: String = "",
    val frequency: String = ""
)

data class ProductAllergenicityResponse(
    val productName: String = "",
    val score: Float = 0f,
    val label: String = "Низкий",
    val allergens: List<String> = emptyList(),
    val histaminContent: String = "нет данных",
    val crossReactiveWith: List<String> = emptyList(),
    val commonReactions: List<String> = emptyList(),
    val safeAlternatives: List<String> = emptyList(),
    val description: String = "",
    val sources: String = ""
)
