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

    @Query("SELECT * FROM household_products WHERE entryDate = :date OR isPersistent = 1 ORDER BY category, id")
    fun getProductsForDate(date: LocalDate): Flow<List<HouseholdProduct>>

    @Query("SELECT DISTINCT * FROM household_products WHERE (entryDate BETWEEN :from AND :to) OR isPersistent = 1 ORDER BY entryDate, category")
    suspend fun getProductsInRange(from: LocalDate, to: LocalDate): List<HouseholdProduct>

    @Query("SELECT * FROM household_products WHERE name LIKE '%' || :query || '%' ORDER BY entryDate DESC LIMIT 20")
    suspend fun searchProducts(query: String): List<HouseholdProduct>

    @Query("SELECT * FROM household_products WHERE allergenicityScore IS NOT NULL ORDER BY allergenicityScore ASC")
    fun getAllProductsWithScores(): Flow<List<HouseholdProduct>>

    // AnalysisResult
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(result: AnalysisResult): Long

    @Query("SELECT * FROM analysis_results ORDER BY analyzedAt DESC")
    fun getAllAnalyses(): Flow<List<AnalysisResult>>

    @Query("SELECT * FROM analysis_results ORDER BY analyzedAt DESC LIMIT 1")
    suspend fun getLatestAnalysis(): AnalysisResult?

    // VapeSession
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVapeSession(session: com.allergia.data.models.VapeSession): Long

    @Delete
    suspend fun deleteVapeSession(session: com.allergia.data.models.VapeSession)

    @Query("SELECT * FROM vape_sessions WHERE entryDate = :date ORDER BY id")
    fun getVapeSessionsForDate(date: LocalDate): Flow<List<com.allergia.data.models.VapeSession>>

    @Query("SELECT * FROM vape_sessions WHERE entryDate <= :date ORDER BY entryDate DESC, id DESC LIMIT 1")
    suspend fun getLatestVapeSessionBefore(date: LocalDate): com.allergia.data.models.VapeSession?

    @Query("SELECT * FROM vape_sessions WHERE entryDate BETWEEN :from AND :to ORDER BY entryDate, id")
    suspend fun getVapeSessionsInRange(from: LocalDate, to: LocalDate): List<com.allergia.data.models.VapeSession>

    // ProfileItem
    @Query("SELECT * FROM profile_items ORDER BY type, name")
    fun getAllProfileItems(): Flow<List<com.allergia.data.models.ProfileItem>>

    @Query("SELECT * FROM profile_items ORDER BY type, name")
    suspend fun getAllProfileItemsList(): List<com.allergia.data.models.ProfileItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfileItem(item: com.allergia.data.models.ProfileItem): Long

    @Delete
    suspend fun deleteProfileItem(item: com.allergia.data.models.ProfileItem)

    @Query("DELETE FROM profile_items")
    suspend fun clearProfileItems()

    // ── Backup: full-table reads ──────────────────────────────────────────────

    @Query("SELECT * FROM diary_entries ORDER BY date")
    suspend fun getAllEntriesList(): List<DiaryEntry>

    @Query("SELECT * FROM food_items ORDER BY entryDate, id")
    suspend fun getAllFoodItemsList(): List<FoodItem>

    @Query("SELECT * FROM medications ORDER BY entryDate, id")
    suspend fun getAllMedicationsList(): List<Medication>

    @Query("SELECT * FROM skin_conditions ORDER BY entryDate")
    suspend fun getAllSkinConditionsList(): List<SkinCondition>

    @Query("SELECT * FROM allergy_symptoms ORDER BY entryDate, symptomType")
    suspend fun getAllSymptomsList(): List<AllergySymptom>

    @Query("SELECT * FROM household_products ORDER BY entryDate, id")
    suspend fun getAllHouseholdProductsList(): List<HouseholdProduct>

    @Query("SELECT * FROM analysis_results ORDER BY analyzedAt")
    suspend fun getAllAnalysisResultsList(): List<AnalysisResult>

    @Query("SELECT * FROM vape_sessions ORDER BY entryDate, id")
    suspend fun getAllVapeSessionsList(): List<com.allergia.data.models.VapeSession>

    // ── Backup: batch inserts for restore ────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllEntries(entries: List<DiaryEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFoodItems(items: List<FoodItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMedications(meds: List<Medication>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSkinConditions(conditions: List<SkinCondition>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSymptoms(symptoms: List<AllergySymptom>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllHouseholdProducts(products: List<HouseholdProduct>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAnalysisResults(results: List<AnalysisResult>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllVapeSessions(sessions: List<com.allergia.data.models.VapeSession>)

    // ── Backup: table clears for full restore ────────────────────────────────

    @Query("DELETE FROM food_items")         suspend fun clearFoodItems()
    @Query("DELETE FROM medications")        suspend fun clearMedications()
    @Query("DELETE FROM skin_conditions")    suspend fun clearSkinConditions()
    @Query("DELETE FROM allergy_symptoms")   suspend fun clearSymptoms()
    @Query("DELETE FROM household_products") suspend fun clearHouseholdProducts()
    @Query("DELETE FROM analysis_results")   suspend fun clearAnalysisResults()
    @Query("DELETE FROM vape_sessions")      suspend fun clearVapeSessions()
    @Query("DELETE FROM diary_entries")      suspend fun clearDiaryEntries()

    // AllergyTestResult
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllergyTestResult(result: AllergyTestResult): Long

    @Delete
    suspend fun deleteAllergyTestResult(result: AllergyTestResult)

    @Query("SELECT * FROM allergy_test_results ORDER BY testDate DESC")
    fun getAllAllergyTestResults(): Flow<List<AllergyTestResult>>

    @Query("SELECT * FROM allergy_test_results ORDER BY testDate DESC")
    suspend fun getAllAllergyTestResultsList(): List<AllergyTestResult>

    @Query("DELETE FROM allergy_test_results")
    suspend fun clearAllergyTestResults()
}
