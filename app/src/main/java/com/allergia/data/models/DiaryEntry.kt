package com.allergia.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.allergia.data.database.Converters
import java.time.LocalDate

@Entity(tableName = "diary_entries")
@TypeConverters(Converters::class)
data class DiaryEntry(
    @PrimaryKey
    val date: LocalDate,
    val notes: String = ""
)
