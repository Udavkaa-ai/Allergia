package com.allergia.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.allergia.data.models.*

@Database(
    entities = [
        DiaryEntry::class,
        FoodItem::class,
        Medication::class,
        SkinCondition::class,
        AllergySymptom::class,
        AnalysisResult::class,
        HouseholdProduct::class,
        VapeSession::class,
        ProfileItem::class,
        AllergyTestResult::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao
}
