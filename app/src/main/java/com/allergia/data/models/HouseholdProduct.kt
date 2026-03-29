package com.allergia.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.allergia.data.database.Converters
import java.time.LocalDate

@Entity(
    tableName = "household_products",
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
data class HouseholdProduct(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entryDate: LocalDate,
    val name: String,
    val brand: String = "",
    val category: ProductCategory = ProductCategory.OTHER,
    // Full ingredients text (INCI or plain), obtained from label photo or manual entry
    val ingredients: String = "",
    // Comma-separated list of flagged allergenic ingredients
    val allergenicIngredients: String = "",
    // 0.0–1.0 allergenicity score from AI
    val allergenicityScore: Float? = null,
    val allergenicityLabel: String = "",
    // Path to compressed label photo stored locally
    val labelPhotoPath: String? = null,
    val notes: String = "",
    /** If true, this product is shown on every diary date until manually removed */
    val isPersistent: Boolean = true
)

enum class ProductCategory(val displayName: String, val emoji: String) {
    DISH_SOAP("Гель для посуды", "🍽"),
    SHAMPOO("Шампунь", "💆"),
    SHOWER_GEL("Гель для душа", "🚿"),
    HAIR_CONDITIONER("Кондиционер для волос", "💇"),
    FACE_CREAM("Крем для лица", "🧴"),
    BODY_CREAM("Крем для тела", "🧴"),
    HAND_CREAM("Крем для рук", "🤲"),
    TOOTHPASTE("Зубная паста", "🦷"),
    DEODORANT("Дезодорант", "✨"),
    PERFUME("Парфюм / Духи", "🌸"),
    LAUNDRY("Средство для стирки", "👕"),
    CLEANING("Чистящее средство", "🧹"),
    SUNSCREEN("Солнцезащитный крем", "☀️"),
    MAKEUP("Декоративная косметика", "💄"),
    BABY_CARE("Детская косметика", "👶"),
    OTHER("Другое", "🧪");

    companion object {
        // Groups for display
        val COSMETICS = listOf(SHAMPOO, SHOWER_GEL, HAIR_CONDITIONER, FACE_CREAM, BODY_CREAM, HAND_CREAM, DEODORANT, PERFUME, SUNSCREEN, MAKEUP, BABY_CARE)
        val HOUSEHOLD = listOf(DISH_SOAP, LAUNDRY, CLEANING)
        val ORAL = listOf(TOOTHPASTE)
    }
}

// Parsed from AI label recognition
data class LabelRecognitionResult(
    val productName: String = "",
    val ingredients: List<String> = emptyList(),
    val allergenicIngredients: List<AllergenicIngredient> = emptyList(),
    val allergenicityScore: Float = 0f,
    val allergenicityLabel: String = "Низкий",
    val summary: String = "",
    val rawIngredientText: String = ""
)

data class AllergenicIngredient(
    val name: String,
    val commonName: String = "",
    val riskLevel: String = "low",       // "high" | "medium" | "low"
    val reason: String = ""
)
