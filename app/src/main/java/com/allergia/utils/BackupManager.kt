package com.allergia.utils

import android.content.Context
import android.net.Uri
import com.allergia.data.models.*
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// Explicit deserializer for FoodItem so that fields absent in old backups
// (e.g. `ingredients` added in version 2) receive their default values
// instead of Gson's Java-null, which violates Room NOT NULL constraints.
private val foodItemDeserializer = JsonDeserializer { json, _, _ ->
    val o = json.asJsonObject
    FoodItem(
        id                 = o.get("id")?.asLong ?: 0L,
        entryDate          = LocalDate.parse(o.get("entryDate").asString),
        name               = o.get("name")?.asString ?: "",
        amount             = o.get("amount")?.asString ?: "",
        mealType           = o.get("mealType")?.asString
                                 ?.let { runCatching { MealType.valueOf(it) }.getOrNull() }
                                 ?: MealType.OTHER,
        allergenicityScore = o.get("allergenicityScore")?.takeIf { !it.isJsonNull }?.asFloat,
        allergenicityLabel = o.get("allergenicityLabel")?.asString ?: "",
        knownAllergens     = o.get("knownAllergens")?.asString ?: "",
        ingredients        = o.get("ingredients")?.asString ?: ""
    )
}

data class BackupData(
    val version: Int = 4,
    val exportedAt: String = LocalDateTime.now().toString(),
    val diaryEntries: List<DiaryEntry> = emptyList(),
    val foodItems: List<FoodItem> = emptyList(),
    val medications: List<Medication> = emptyList(),
    val skinConditions: List<SkinCondition> = emptyList(),
    val symptoms: List<AllergySymptom> = emptyList(),
    val householdProducts: List<HouseholdProduct> = emptyList(),
    val analysisResults: List<AnalysisResult> = emptyList(),
    val vapeSessions: List<VapeSession> = emptyList(),
    val profileItems: List<ProfileItem> = emptyList(),
    val allergyTestResults: List<AllergyTestResult> = emptyList()
)

object BackupManager {

    private val gson = GsonBuilder()
        .registerTypeAdapter(
            LocalDate::class.java,
            JsonSerializer<LocalDate> { src, _, _ -> JsonPrimitive(src.toString()) }
        )
        .registerTypeAdapter(
            LocalDate::class.java,
            JsonDeserializer { json, _, _ -> LocalDate.parse(json.asString) }
        )
        .registerTypeAdapter(
            LocalDateTime::class.java,
            JsonSerializer<LocalDateTime> { src, _, _ -> JsonPrimitive(src.toString()) }
        )
        .registerTypeAdapter(
            LocalDateTime::class.java,
            JsonDeserializer { json, _, _ -> LocalDateTime.parse(json.asString) }
        )
        .registerTypeAdapter(
            LocalTime::class.java,
            JsonSerializer<LocalTime> { src, _, _ -> JsonPrimitive(src.toString()) }
        )
        .registerTypeAdapter(
            LocalTime::class.java,
            JsonDeserializer { json, _, _ -> LocalTime.parse(json.asString) }
        )
        .registerTypeAdapter(FoodItem::class.java, foodItemDeserializer)
        .setPrettyPrinting()
        .create()

    fun toJson(data: BackupData): String = gson.toJson(data)

    fun fromJson(json: String): BackupData =
        gson.fromJson(json, BackupData::class.java)
            ?: throw IllegalArgumentException("Не удалось прочитать файл бекапа")

    fun writeToUri(context: Context, uri: Uri, content: String) {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(content.toByteArray(Charsets.UTF_8))
        } ?: throw IllegalStateException("Не удалось открыть файл для записи")
    }

    fun readFromUri(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        } ?: throw IllegalStateException("Не удалось открыть файл для чтения")
    }
}
