package com.allergia.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.allergia.data.database.Converters
import java.time.LocalDate

@Entity(
    tableName = "food_items",
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
data class FoodItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entryDate: LocalDate,
    val name: String,
    val amount: String = "",
    val mealType: MealType = MealType.OTHER,
    // AI-assessed allergenicity score 0.0–1.0
    val allergenicityScore: Float? = null,
    val allergenicityLabel: String = "",
    val knownAllergens: String = "",   // comma-separated list
    val notes: String? = null
)

enum class MealType(val displayName: String) {
    BREAKFAST("Завтрак"),
    LUNCH("Обед"),
    DINNER("Ужин"),
    SNACK("Перекус"),
    DRINK("Напиток"),
    OTHER("Другое")
}
