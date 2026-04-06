package com.allergia.data.repository

import com.allergia.data.database.DiaryDao
import com.allergia.data.models.*
import com.allergia.utils.BackupData
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiaryRepository @Inject constructor(
    private val dao: DiaryDao
) {
    // Entries
    fun getAllEntries(): Flow<List<DiaryEntry>> = dao.getAllEntries()

    suspend fun getOrCreateEntry(date: LocalDate): DiaryEntry {
        return dao.getEntry(date) ?: DiaryEntry(date = date).also { dao.upsertEntry(it) }
    }

    suspend fun updateEntryNotes(date: LocalDate, notes: String) {
        dao.upsertEntry(DiaryEntry(date = date, notes = notes))
    }

    // Food
    fun getFoodItemsForDate(date: LocalDate): Flow<List<FoodItem>> = dao.getFoodItemsForDate(date)
    suspend fun insertFoodItem(item: FoodItem): Long = dao.insertFoodItem(item)
    suspend fun updateFoodItem(item: FoodItem) = dao.updateFoodItem(item)
    suspend fun deleteFoodItem(item: FoodItem) = dao.deleteFoodItem(item)
    suspend fun getFoodItemsInRange(from: LocalDate, to: LocalDate) = dao.getFoodItemsInRange(from, to)

    // Medications
    fun getMedicationsForDate(date: LocalDate): Flow<List<Medication>> = dao.getMedicationsForDate(date)
    suspend fun insertMedication(med: Medication): Long = dao.insertMedication(med)
    suspend fun updateMedication(med: Medication) = dao.updateMedication(med)
    suspend fun deleteMedication(med: Medication) = dao.deleteMedication(med)
    suspend fun getMedicationsInRange(from: LocalDate, to: LocalDate) = dao.getMedicationsInRange(from, to)

    // Skin Condition
    fun getSkinConditionForDate(date: LocalDate): Flow<SkinCondition?> = dao.getSkinConditionForDate(date)
    suspend fun saveSkinCondition(condition: SkinCondition): Long = dao.insertSkinCondition(condition)
    suspend fun getSkinConditionsInRange(from: LocalDate, to: LocalDate) = dao.getSkinConditionsInRange(from, to)

    // Symptoms
    fun getSymptomsForDate(date: LocalDate): Flow<List<AllergySymptom>> = dao.getSymptomsForDate(date)
    suspend fun insertSymptom(symptom: AllergySymptom): Long = dao.insertSymptom(symptom)
    suspend fun updateSymptom(symptom: AllergySymptom) = dao.updateSymptom(symptom)
    suspend fun deleteSymptom(symptom: AllergySymptom) = dao.deleteSymptom(symptom)
    suspend fun getSymptomsInRange(from: LocalDate, to: LocalDate) = dao.getSymptomsInRange(from, to)

    // Household Products
    fun getProductsForDate(date: LocalDate): Flow<List<HouseholdProduct>> = dao.getProductsForDate(date)
    suspend fun insertProduct(product: HouseholdProduct): Long = dao.insertHouseholdProduct(product)
    suspend fun updateProduct(product: HouseholdProduct) = dao.updateHouseholdProduct(product)
    suspend fun deleteProduct(product: HouseholdProduct) = dao.deleteHouseholdProduct(product)
    suspend fun getProductsInRange(from: LocalDate, to: LocalDate) = dao.getProductsInRange(from, to)

    // Analysis
    fun getAllAnalyses(): Flow<List<AnalysisResult>> = dao.getAllAnalyses()
    suspend fun saveAnalysis(result: AnalysisResult): Long = dao.insertAnalysis(result)
    suspend fun getLatestAnalysis(): AnalysisResult? = dao.getLatestAnalysis()

    // Allergy Profile
    fun getAllProfileItems(): Flow<List<ProfileItem>> = dao.getAllProfileItems()
    suspend fun insertProfileItem(item: ProfileItem): Long = dao.insertProfileItem(item)
    suspend fun deleteProfileItem(item: ProfileItem) = dao.deleteProfileItem(item)

    // Vape Sessions
    fun getVapeSessionsForDate(date: LocalDate): Flow<List<VapeSession>> = dao.getVapeSessionsForDate(date)
    suspend fun insertVapeSession(session: VapeSession): Long = dao.insertVapeSession(session)
    suspend fun deleteVapeSession(session: VapeSession) = dao.deleteVapeSession(session)
    suspend fun getLatestVapeSessionBefore(date: LocalDate): VapeSession? = dao.getLatestVapeSessionBefore(date)

    // ── Backup / Restore ──────────────────────────────────────────────────────

    suspend fun getFullBackup(): BackupData = BackupData(
        diaryEntries       = dao.getAllEntriesList(),
        foodItems          = dao.getAllFoodItemsList(),
        medications        = dao.getAllMedicationsList(),
        skinConditions     = dao.getAllSkinConditionsList(),
        symptoms           = dao.getAllSymptomsList(),
        householdProducts  = dao.getAllHouseholdProductsList(),
        analysisResults    = dao.getAllAnalysisResultsList(),
        vapeSessions       = dao.getAllVapeSessionsList(),
        profileItems       = dao.getAllProfileItemsList(),
        allergyTestResults = dao.getAllAllergyTestResultsList()
    )

    suspend fun restoreFromBackup(data: BackupData) {
        dao.clearAllergyTestResults()
        dao.clearAnalysisResults()
        dao.clearHouseholdProducts()
        dao.clearSymptoms()
        dao.clearSkinConditions()
        dao.clearMedications()
        dao.clearFoodItems()
        dao.clearVapeSessions()
        dao.clearProfileItems()
        dao.clearDiaryEntries()

        dao.insertAllEntries(data.diaryEntries)
        @Suppress("SENSELESS_COMPARISON")
        val sanitizedFoodItems = data.foodItems.map { item ->
            if (item.ingredients == null) item.copy(ingredients = "") else item
        }
        dao.insertAllFoodItems(sanitizedFoodItems)
        dao.insertAllMedications(data.medications)
        dao.insertAllSkinConditions(data.skinConditions)
        dao.insertAllSymptoms(data.symptoms)
        dao.insertAllHouseholdProducts(data.householdProducts)
        dao.insertAllAnalysisResults(data.analysisResults)
        dao.insertAllVapeSessions(data.vapeSessions)
        data.profileItems.forEach { dao.insertProfileItem(it) }
        dao.insertAllAllergyTestResults(data.allergyTestResults)
    }

    // Allergy Test Results
    fun getAllAllergyTestResults(): Flow<List<AllergyTestResult>> = dao.getAllAllergyTestResults()
    suspend fun insertAllergyTestResult(result: AllergyTestResult): Long = dao.insertAllergyTestResult(result)
    suspend fun deleteAllergyTestResult(result: AllergyTestResult) = dao.deleteAllergyTestResult(result)

    // Sorted products for search screen
    fun getAllProductsWithScores(): Flow<List<HouseholdProduct>> = dao.getAllProductsWithScores()

    // Data for AI context (now includes household products + allergy tests)
    suspend fun getDataForRange(from: LocalDate, to: LocalDate): DiaryRangeData {
        return DiaryRangeData(
            foods = dao.getFoodItemsInRange(from, to),
            medications = dao.getMedicationsInRange(from, to),
            skinConditions = dao.getSkinConditionsInRange(from, to),
            symptoms = dao.getSymptomsInRange(from, to),
            householdProducts = dao.getProductsInRange(from, to),
            vapeSessions = dao.getVapeSessionsInRange(from, to),
            profileItems = dao.getAllProfileItemsList(),
            allergyTestResults = dao.getAllAllergyTestResultsList(),
            periodStart = from,
            periodEnd = to
        )
    }
}

data class DiaryRangeData(
    val foods: List<FoodItem>,
    val medications: List<Medication>,
    val skinConditions: List<SkinCondition>,
    val symptoms: List<AllergySymptom>,
    val householdProducts: List<HouseholdProduct>,
    val vapeSessions: List<VapeSession> = emptyList(),
    val profileItems: List<ProfileItem> = emptyList(),
    val allergyTestResults: List<AllergyTestResult> = emptyList(),
    val periodStart: LocalDate,
    val periodEnd: LocalDate
)
