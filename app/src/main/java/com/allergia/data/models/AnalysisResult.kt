package com.allergia.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.allergia.data.database.Converters
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "analysis_results")
@TypeConverters(Converters::class)
data class AnalysisResult(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val analyzedAt: LocalDateTime,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    // Full AI response text
    val fullAnalysis: String,
    // JSON-encoded list of TriggerSuspect
    val suspectedTriggers: String = "[]",
    val overallRisk: String = "",
    val recommendations: String = ""
)

data class TriggerSuspect(
    val name: String,
    val type: String,  // "food", "medication", "environmental"
    val probability: Float,  // 0.0–1.0
    val evidence: String,
    val confidenceLabel: String  // "Высокая", "Средняя", "Низкая"
)

data class ProductAllergenicityResult(
    val productName: String,
    val score: Float,   // 0.0–1.0
    val label: String,  // "Высокий", "Средний", "Низкий", "Очень низкий"
    val allergens: List<String>,
    val description: String,
    val sources: String
)
