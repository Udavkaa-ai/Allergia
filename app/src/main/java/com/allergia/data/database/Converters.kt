package com.allergia.data.database

import androidx.room.TypeConverter
import com.allergia.data.models.MealType
import com.allergia.data.models.SymptomType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class Converters {
    @TypeConverter fun fromLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }
    @TypeConverter fun toLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter fun fromLocalTime(value: String?): LocalTime? = value?.let { LocalTime.parse(it) }
    @TypeConverter fun toLocalTime(time: LocalTime?): String? = time?.toString()

    @TypeConverter fun fromLocalDateTime(value: String?): LocalDateTime? = value?.let { LocalDateTime.parse(it) }
    @TypeConverter fun toLocalDateTime(dt: LocalDateTime?): String? = dt?.toString()

    @TypeConverter fun fromMealType(value: String?): MealType? = value?.let { MealType.valueOf(it) }
    @TypeConverter fun toMealType(type: MealType?): String? = type?.name

    @TypeConverter fun fromSymptomType(value: String?): SymptomType? = value?.let { SymptomType.valueOf(it) }
    @TypeConverter fun toSymptomType(type: SymptomType?): String? = type?.name
}
