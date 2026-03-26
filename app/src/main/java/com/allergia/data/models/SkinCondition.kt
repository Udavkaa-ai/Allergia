package com.allergia.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.allergia.data.database.Converters
import java.time.LocalDate

@Entity(
    tableName = "skin_conditions",
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
data class SkinCondition(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entryDate: LocalDate,
    // Severity 0 = none, 1 = mild, 2 = moderate, 3 = severe
    val overallSeverity: Int = 0,
    val redness: Int = 0,
    val itching: Int = 0,
    val rash: Int = 0,
    val swelling: Int = 0,
    val dryness: Int = 0,
    val affectedAreas: String = "",  // comma-separated body areas
    val photoPath: String? = null,
    val notes: String = ""
)
