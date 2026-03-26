package com.allergia.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.allergia.data.database.Converters
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "medications",
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
data class Medication(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entryDate: LocalDate,
    val name: String,
    val dose: String = "",
    val takenAt: LocalTime? = null,
    val isAntihistamine: Boolean = false,
    val notes: String = ""
)
