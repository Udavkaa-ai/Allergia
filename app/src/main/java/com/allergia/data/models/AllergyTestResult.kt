package com.allergia.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.allergia.data.database.Converters
import java.time.LocalDate

@Entity(tableName = "allergy_test_results")
@TypeConverters(Converters::class)
data class AllergyTestResult(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val testDate: LocalDate,
    val photoPath: String? = null,
    val testType: String = "",             // "кожный прик-тест", "IgE тест", "патч-тест"
    val labName: String = "",
    val positiveAllergens: String = "",    // через запятую
    val borderlineAllergens: String = "",  // через запятую
    val negativeAllergens: String = "",    // через запятую
    val summary: String = "",
    val recommendation: String = "",
    val notes: String = ""
)
