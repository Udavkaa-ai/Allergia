package com.allergia.api

import android.content.Context
import com.allergia.data.models.*
import com.allergia.data.repository.DiaryRangeData
import com.allergia.utils.appDataStore
import com.allergia.utils.PreferenceKeys
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiService @Inject constructor(
    private val api: OpenRouterApi,
    private val context: Context
) {
    private val gson = Gson()
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    private suspend fun authHeader(): String {
        val key = context.appDataStore.data.map { it[PreferenceKeys.OPENROUTER_API_KEY] ?: "" }.first()
        if (key.isBlank()) throw Exception("API ключ не настроен. Перейдите в Настройки и введите ключ OpenRouter.")
        return "Bearer $key"
    }

    // ─── System prompt ─────────────────────────────────────────────────────────

    private val allergySystemPrompt = """
Ты — специализированный ИИ-помощник по аллергологии, дерматологии и токсикологии косметики.
Твоя задача — анализировать дневник пациента для выявления аллергенов и паттернов реакций.

ПРИНЦИПЫ АНАЛИЗА:
1. Причинно-следственный подход: ищи корреляции между едой/медикаментами/химией и симптомами с задержкой 0–72 часов.
2. Учитывай перекрёстную реактивность (берёза → яблоки, латекс → авокадо/банан/киви).
3. Оценивай накопительный эффект: повторное воздействие усиливает реакцию.
4. Разграничивай IgE-аллергию, контактный дерматит, пищевую непереносимость.
5. Антигистаминные препараты могут маскировать симптомы — учитывай это.
6. КОСМЕТИКА И ХИМИЯ: контактный дерматит может проявляться через 24–72ч после первого контакта.
   Особое внимание: SLS, MI/MCI, парабены, отдушки, формальдегид-доноры.
7. Учитывай комбинированное воздействие: нанесение нескольких средств одновременно.
8. Всегда указывай ВЕРОЯТНОСТЬ в % для каждого триггера (еда / лекарства / косметика / химия).
9. Рекомендуй patch-тест как метод подтверждения для контактных аллергенов.
10. Подчёркивай необходимость консультации аллерголога-дерматолога.
11. ВЕЙП: никотин, пропиленгликоль (PG), ароматизаторы жидкостей (диацетил, ванилин) могут вызывать раздражение дыхательных путей, контактный дерматит, бронхоспазм. VG (вегетарианский глицерин) обычно безопасен.

ФОРМАТ ОТВЕТА: Структурированный JSON на русском языке.
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
      "name": "Название продукта/медикамента/химии",
      "type": "food|medication|cosmetic|household_chemical|environmental",
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

    // ─── Medication side effects ───────────────────────────────────────────────

    suspend fun getMedicationSideEffects(name: String, dose: String): Result<MedicationSideEffectsResponse> {
        return runCatching {
            val doseInfo = if (dose.isNotBlank()) ", доза: $dose" else ""
            val prompt = """
Препарат: "$name"$doseInfo

Опиши побочные эффекты этого препарата с точки зрения аллергологии.
Ответь строго в JSON (без markdown):
{
  "medicationName": "$name",
  "commonSideEffects": ["частый побочный эффект 1", "частый побочный эффект 2"],
  "allergyRelated": ["аллергическая реакция 1", "аллергическая реакция 2"],
  "skinReactions": ["кожная реакция 1"],
  "allergenicPotential": "высокий|средний|низкий",
  "crossReactivity": ["перекрёстно-реактивный препарат 1"],
  "importantWarnings": ["важное предупреждение"],
  "recommendation": "Краткая рекомендация аллергологу (1-2 предложения)"
}

Используй данные из инструкции к препарату и актуальные клинические данные.
Если препарат неизвестен, укажи это в recommendation.
            """.trimIndent()

            val response = api.chatCompletion(
                authorization = authHeader(),
                request = OpenRouterRequest(
                    messages = listOf(
                        ChatMessage("system", allergySystemPrompt),
                        ChatMessage("user", prompt)
                    ),
                    temperature = 0.1,
                    maxTokens = 1024
                )
            )

            if (response.error != null) throw Exception("API Error: ${response.error.message}")

            val content = response.choices.firstOrNull()?.message?.content
                ?: throw Exception("Empty response")

            gson.fromJson(cleanJson(content), MedicationSideEffectsResponse::class.java)
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private fun buildDiaryContext(data: DiaryRangeData): String {
        val sb = StringBuilder()

        // Group data by date
        val allDates = (data.foods.map { it.entryDate } +
                data.medications.map { it.entryDate } +
                data.skinConditions.map { it.entryDate } +
                data.symptoms.map { it.entryDate } +
                data.householdProducts.map { it.entryDate } +
                data.vapeSessions.map { it.entryDate }).toSortedSet()

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

            // Household products & cosmetics — ключевые для контактного дерматита
            val products = data.householdProducts.filter { it.entryDate == date }
            if (products.isNotEmpty()) {
                sb.appendLine("🧴 Химия и косметика:")
                products.groupBy { it.category }.forEach { (cat, group) ->
                    sb.append("  ${cat.emoji} ${cat.displayName}: ")
                    sb.appendLine(group.joinToString(", ") { p ->
                        buildString {
                            append(p.name)
                            if (p.brand.isNotBlank()) append(" (${p.brand})")
                            if (p.allergenicityScore != null) append(" [аллергенность: ${(p.allergenicityScore * 100).toInt()}%]")
                            if (p.allergenicIngredients.isNotBlank()) append(" [⚠ ${p.allergenicIngredients}]")
                        }
                    })
                }
            }

            val vapes = data.vapeSessions.filter { it.entryDate == date }
            if (vapes.isNotEmpty()) {
                sb.appendLine("💨 Вейп:")
                vapes.forEach { v ->
                    sb.append("  •")
                    if (v.brand.isNotBlank()) sb.append(" ${v.brand}")
                    if (v.flavor.isNotBlank()) sb.append(", вкус: ${v.flavor}")
                    if (v.nicotineLevel.isNotBlank()) sb.append(", ник: ${v.nicotineLevel}")
                    if (v.pgVgRatio.isNotBlank()) sb.append(", PG/VG: ${v.pgVgRatio}")
                    sb.appendLine()
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

data class MedicationSideEffectsResponse(
    val medicationName: String = "",
    val commonSideEffects: List<String> = emptyList(),
    val allergyRelated: List<String> = emptyList(),
    val skinReactions: List<String> = emptyList(),
    val allergenicPotential: String = "неизвестен",
    val crossReactivity: List<String> = emptyList(),
    val importantWarnings: List<String> = emptyList(),
    val recommendation: String = ""
)
