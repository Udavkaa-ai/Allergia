package com.allergia.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores personal allergy profile data confirmed by medical tests or doctor.
 * Used to give the AI precise exclusion/inclusion lists for analysis.
 */
@Entity(tableName = "profile_items")
data class ProfileItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Display name, e.g. "Пыль", "Лактоза", "Арахис" */
    val name: String,
    val type: ProfileItemType = ProfileItemType.CONFIRMED_SAFE,
    /** Was this confirmed by an allergy test? */
    val confirmedByTest: Boolean = true,
    val notes: String = ""
)

enum class ProfileItemType(val displayName: String, val emoji: String) {
    CONFIRMED_SAFE("Точно нет аллергии", "✅"),
    KNOWN_ALLERGEN("Известный аллерген", "⚠️")
}
