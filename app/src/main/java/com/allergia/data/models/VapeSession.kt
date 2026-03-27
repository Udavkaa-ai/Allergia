package com.allergia.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.allergia.data.database.Converters
import java.time.LocalDate

@Entity(
    tableName = "vape_sessions",
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
data class VapeSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entryDate: LocalDate,
    val brand: String = "",
    val flavor: String = "",
    val nicotineLevel: String = "",   // e.g. "6 мг", "0 мг"
    val pgVgRatio: String = "",       // e.g. "70/30"
    val notes: String = ""
)
