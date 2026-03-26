package com.allergia.data.database

import androidx.room.*
import com.allergia.data.models.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DiaryDao {

    // DiaryEntry
    @Upsert
    suspend fun upsertEntry(entry: DiaryEntry)

    @Query("SELECT * FROM diary_entries ORDER BY date DESC")
    fun getAllEntries(): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE date = :date LIMIT 1")
    suspend fun getEntry(date: LocalDate): DiaryEntry?

    @Query("SELECT * FROM diary_entries WHERE date BETWEEN :from AND :to ORDER BY date")
    suspend fun getEntriesInRange(from: LocalDate, to: LocalDate): List<DiaryEntry>

    // FoodItem
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItem(item: FoodItem): Long

    @Update
    suspend fun updateFoodItem(item: FoodItem)

    @Delete
    suspend fun deleteFoodItem(item: FoodItem)

    @Query("SELECT * FROM food_items WHERE entryDate = :date ORDER BY mealType, id")
    fun getFoodItemsForDate(date: LocalDate): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE entryDate BETWEEN :from AND :to ORDER BY entryDate, mealType")
    suspend fun getFoodItemsInRange(from: LocalDate, to: LocalDate): List<FoodItem>

    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%' ORDER BY entryDate DESC LIMIT 20")
    suspend fun searchFoodItems(query: String): List<FoodItem>

    // Medication
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: Medication): Long

    @Update
    suspend fun updateMedication(medication: Medication)

    @Delete
    suspend fun deleteMedication(medication: Medication)

    @Query("SELECT * FROM medications WHERE entryDate = :date ORDER BY takenAt, id")
    fun getMedicationsForDate(date: LocalDate): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE entryDate BETWEEN :from AND :to ORDER BY entryDate")
    suspend fun getMedicationsInRange(from: LocalDate, to: LocalDate): List<Medication>

    // SkinCondition
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkinCondition(condition: SkinCondition): Long

    @Update
    suspend fun updateSkinCondition(condition: SkinCondition)

    @Delete
    suspend fun deleteSkinCondition(condition: SkinCondition)

    @Query("SELECT * FROM skin_conditions WHERE entryDate = :date LIMIT 1")
    fun getSkinConditionForDate(date: LocalDate): Flow<SkinCondition?>

    @Query("SELECT * FROM skin_conditions WHERE entryDate BETWEEN :from AND :to ORDER BY entryDate")
    suspend fun getSkinConditionsInRange(from: LocalDate, to: LocalDate): List<SkinCondition>

    // AllergySymptom
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptom(symptom: AllergySymptom): Long

    @Update
    suspend fun updateSymptom(symptom: AllergySymptom)

    @Delete
    suspend fun deleteSymptom(symptom: AllergySymptom)

    @Query("SELECT * FROM allergy_symptoms WHERE entryDate = :date ORDER BY symptomType")
    fun getSymptomsForDate(date: LocalDate): Flow<List<AllergySymptom>>

    @Query("SELECT * FROM allergy_symptoms WHERE entryDate BETWEEN :from AND :to ORDER BY entryDate, symptomType")
    suspend fun getSymptomsInRange(from: LocalDate, to: LocalDate): List<AllergySymptom>

    // HouseholdProduct
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHouseholdProduct(product: HouseholdProduct): Long

    @Update
    suspend fun updateHouseholdProduct(product: HouseholdProduct)

    @Delete
    suspend fun deleteHouseholdProduct(product: HouseholdProduct)

    @Query("SELECT * FROM household_products WHERE entryDate = :date ORDER BY category, id")
    fun getProductsForDate(date: LocalDate): Flow<List<HouseholdProduct>>

    @Query("SELECT * FROM household_products WHERE entryDate BETWEEN :from AND :to ORDER BY entryDate, category")
    suspend fun getProductsInRange(from: LocalDate, to: LocalDate): List<HouseholdProduct>

    @Query("SELECT * FROM household_products WHERE name LIKE '%' || :query || '%' ORDER BY entryDate DESC LIMIT 20")
    suspend fun searchProducts(query: String): List<HouseholdProduct>

    // AnalysisResult
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(result: AnalysisResult): Long

    @Query("SELECT * FROM analysis_results ORDER BY analyzedAt DESC")
    fun getAllAnalyses(): Flow<List<AnalysisResult>>

    @Query("SELECT * FROM analysis_results ORDER BY analyzedAt DESC LIMIT 1")
    suspend fun getLatestAnalysis(): AnalysisResult?
}
