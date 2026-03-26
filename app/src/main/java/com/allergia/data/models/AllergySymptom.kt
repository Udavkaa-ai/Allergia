package com.allergia.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.allergia.data.database.Converters
import java.time.LocalDate

@Entity(
    tableName = "allergy_symptoms",
    foreignKeys = [
        ForeignKey(
            entity = DiaryEntry::class,
            parentColumns = ["date"],
            childColumns = ["entryDate"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("entryDate")]
)
@TypeConverters(Converters::class)
data class AllergySymptom(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entryDate: LocalDate,
    val symptomType: SymptomType,
    val severity: Int = 0,  // 0–3
    val notes: String = ""
)

enum class SymptomType(val displayName: String, val emoji: String) {
    SNEEZING("Чихание", "🤧"),
    RUNNY_NOSE("Насморк", "👃"),
    NASAL_CONGESTION("Заложенность носа", "😤"),
    ITCHY_EYES("Зуд глаз", "👁️"),
    WATERY_EYES("Слезотечение", "😢"),
    THROAT_IRRITATION("Раздражение горла", "🫁"),
    COUGH("Кашель", "😮‍💨"),
    SHORTNESS_OF_BREATH("Одышка", "💨"),
    WHEEZING("Хрипы", "🫀"),
    NAUSEA("Тошнота", "🤢"),
    STOMACH_PAIN("Боль в животе", "🤕"),
    HEADACHE("Головная боль", "🤯"),
    FATIGUE("Усталость", "😴"),
    HIVES("Крапивница", "🔴"),
    ANAPHYLAXIS_SIGNS("Признаки анафилаксии", "⚠️"),
    OTHER("Другое", "❓")
}
