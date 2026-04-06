package com.allergia.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.allergia.api.DetectedFoodItem
import com.allergia.api.FoodPhotoService
import com.allergia.api.FoodRecognitionResult
import com.allergia.api.GeminiService
import com.allergia.api.LabelPhotoService
import com.allergia.api.MedicationSideEffectsResponse
import com.allergia.api.ProductAllergenicityResponse
import com.allergia.api.VapeRecognitionResult
import com.allergia.utils.ImageUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import com.allergia.data.models.*
import com.allergia.data.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val repository: DiaryRepository,
    private val geminiService: GeminiService,
    private val foodPhotoService: FoodPhotoService,
    private val labelPhotoService: LabelPhotoService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val foodItems: StateFlow<List<FoodItem>> = _selectedDate
        .flatMapLatest { repository.getFoodItemsForDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val medications: StateFlow<List<Medication>> = _selectedDate
        .flatMapLatest { repository.getMedicationsForDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val skinCondition: StateFlow<SkinCondition?> = _selectedDate
        .flatMapLatest { repository.getSkinConditionForDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val symptoms: StateFlow<List<AllergySymptom>> = _selectedDate
        .flatMapLatest { repository.getSymptomsForDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEntries: StateFlow<List<DiaryEntry>> = repository.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _allergenicityState = MutableStateFlow<AllergenicityUiState>(AllergenicityUiState.Idle)
    val allergenicityState: StateFlow<AllergenicityUiState> = _allergenicityState.asStateFlow()

    // ─── Photo recognition state ─────────────────────────────────────────────

    val householdProducts: StateFlow<List<HouseholdProduct>> = _selectedDate
        .flatMapLatest { repository.getProductsForDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vapeSessions: StateFlow<List<VapeSession>> = _selectedDate
        .flatMapLatest { repository.getVapeSessionsForDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _photoAnalysisState = MutableStateFlow<PhotoAnalysisState>(PhotoAnalysisState.Idle)
    val photoAnalysisState: StateFlow<PhotoAnalysisState> = _photoAnalysisState.asStateFlow()

    private val _labelAnalysisState = MutableStateFlow<LabelAnalysisState>(LabelAnalysisState.Idle)
    val labelAnalysisState: StateFlow<LabelAnalysisState> = _labelAnalysisState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _sideEffectsState = MutableStateFlow<SideEffectsUiState>(SideEffectsUiState.Idle)
    val sideEffectsState: StateFlow<SideEffectsUiState> = _sideEffectsState.asStateFlow()

    private val _vapePhotoState = MutableStateFlow<VapePhotoState>(VapePhotoState.Idle)
    val vapePhotoState: StateFlow<VapePhotoState> = _vapePhotoState.asStateFlow()

    // Все продукты с оценками аллергенности — для экрана поиска/оценки
    val allProductsWithScores: StateFlow<List<HouseholdProduct>> = repository.getAllProductsWithScores()
        .map { products -> products.distinctBy { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        viewModelScope.launch { repository.getOrCreateEntry(date) }
    }

    // ─── Photo analysis ───────────────────────────────────────────────────────

    /**
     * Отправляет фото в Gemma 3 4B, разбирает ответ и показывает диалог подтверждения.
     */
    fun analyzeFoodPhoto(imageUri: Uri) {
        _photoAnalysisState.value = PhotoAnalysisState.Analyzing
        viewModelScope.launch {
            foodPhotoService.recognizeFoodFromPhoto(imageUri)
                .onSuccess { result ->
                    if (result.items.isEmpty()) {
                        _photoAnalysisState.value = PhotoAnalysisState.NoFoodDetected
                    } else {
                        _photoAnalysisState.value = PhotoAnalysisState.Results(
                            result = result,
                            editableItems = result.items.map { it.copy() }.toMutableList()
                        )
                    }
                }
                .onFailure { err ->
                    _photoAnalysisState.value = PhotoAnalysisState.Error(
                        err.message ?: "Ошибка распознавания"
                    )
                }
        }
    }

    /**
     * Пользователь подтвердил список — добавляем выбранные позиции в дневник
     * и запускаем оценку аллергенности фоном.
     */
    fun confirmPhotoItems(items: List<DetectedFoodItem>) {
        viewModelScope.launch {
            val date = _selectedDate.value
            repository.getOrCreateEntry(date)
            items.filter { it.isSelected }.forEach { detected ->
                val mealType = runCatching { MealType.valueOf(detected.mealType) }.getOrDefault(MealType.OTHER)
                val foodItem = FoodItem(
                    entryDate = date,
                    name = detected.name.trim(),
                    amount = detected.estimatedAmount,
                    mealType = mealType
                )
                val id = repository.insertFoodItem(foodItem)
                assessAllergenicity(foodItem.copy(id = id))
            }
            _toastMessage.emit("Добавлено ${items.count { it.isSelected }} продуктов")
            _photoAnalysisState.value = PhotoAnalysisState.Idle
        }
    }

    fun dismissPhotoAnalysis() {
        _photoAnalysisState.value = PhotoAnalysisState.Idle
    }

    // ─── Label photo analysis ────────────────────────────────────────────────

    fun analyzeLabelPhoto(imageUri: Uri) {
        _labelAnalysisState.value = LabelAnalysisState.Analyzing
        viewModelScope.launch {
            labelPhotoService.recognizeIngredients(imageUri)
                .onSuccess { result ->
                    _labelAnalysisState.value = LabelAnalysisState.Results(result, imageUri)
                }
                .onFailure { err ->
                    _labelAnalysisState.value = LabelAnalysisState.Error(err.message ?: "Ошибка распознавания состава")
                }
        }
    }

    /**
     * Пользователь подтвердил данные о продукте из анализа этикетки.
     */
    fun confirmHouseholdProduct(
        name: String,
        brand: String,
        category: ProductCategory,
        result: com.allergia.data.models.LabelRecognitionResult,
        labelPhotoPath: String? = null
    ) {
        viewModelScope.launch {
            val date = _selectedDate.value
            repository.getOrCreateEntry(date)
            repository.insertProduct(
                HouseholdProduct(
                    entryDate = date,
                    name = name.trim(),
                    brand = brand.trim(),
                    category = category,
                    ingredients = result.ingredients.joinToString(", "),
                    allergenicIngredients = result.allergenicIngredients
                        .filter { it.riskLevel == "high" || it.riskLevel == "medium" }
                        .joinToString(", ") { it.name },
                    allergenicityScore = result.allergenicityScore,
                    allergenicityLabel = result.allergenicityLabel,
                    labelPhotoPath = labelPhotoPath,
                    notes = result.summary
                )
            )
            _toastMessage.emit("Продукт добавлен в дневник")
            _labelAnalysisState.value = LabelAnalysisState.Idle
        }
    }

    /** Добавить продукт вручную без анализа фото */
    fun addHouseholdProductManual(name: String, brand: String, category: ProductCategory) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val date = _selectedDate.value
            repository.getOrCreateEntry(date)
            repository.insertProduct(HouseholdProduct(entryDate = date, name = name.trim(), brand = brand.trim(), category = category))
        }
    }

    fun deleteHouseholdProduct(product: HouseholdProduct) {
        viewModelScope.launch { repository.deleteProduct(product) }
    }

    fun toggleProductPersistence(product: HouseholdProduct) {
        viewModelScope.launch { repository.updateProduct(product.copy(isPersistent = !product.isPersistent)) }
    }

    fun addHouseholdProductManualPersistent(name: String, brand: String, category: ProductCategory, isPersistent: Boolean) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val date = _selectedDate.value
            repository.getOrCreateEntry(date)
            repository.insertProduct(HouseholdProduct(entryDate = date, name = name.trim(), brand = brand.trim(), category = category, isPersistent = isPersistent))
        }
    }

    fun dismissLabelAnalysis() { _labelAnalysisState.value = LabelAnalysisState.Idle }

    // ─── Food ────────────────────────────────────────────────────────────────

    fun addFoodItem(name: String, amount: String, mealType: MealType) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val date = _selectedDate.value
            repository.getOrCreateEntry(date)
            val item = FoodItem(
                entryDate = date,
                name = name.trim(),
                amount = amount.trim(),
                mealType = mealType
            )
            val id = repository.insertFoodItem(item)
            assessAllergenicity(item.copy(id = id))
        }
    }

    fun deleteFoodItem(item: FoodItem) {
        viewModelScope.launch { repository.deleteFoodItem(item) }
    }

    private fun assessAllergenicity(item: FoodItem) {
        viewModelScope.launch {
            geminiService.assessProductAllergenicity(item.name)
                .onSuccess { result ->
                    repository.updateFoodItem(
                        item.copy(
                            allergenicityScore = result.score,
                            allergenicityLabel = result.label,
                            knownAllergens = result.allergens.joinToString(", ")
                        )
                    )
                }
        }
    }

    fun checkProductAllergenicity(productName: String) {
        if (productName.isBlank()) return
        _allergenicityState.value = AllergenicityUiState.Loading
        viewModelScope.launch {
            geminiService.assessProductAllergenicity(productName)
                .onSuccess { _allergenicityState.value = AllergenicityUiState.Success(it) }
                .onFailure { _allergenicityState.value = AllergenicityUiState.Error(it.message ?: "Ошибка") }
        }
    }

    fun resetAllergenicityState() {
        _allergenicityState.value = AllergenicityUiState.Idle
    }

    // ─── Medication ───────────────────────────────────────────────────────────

    fun addMedication(name: String, dose: String, isAntihistamine: Boolean) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val date = _selectedDate.value
            repository.getOrCreateEntry(date)
            repository.insertMedication(
                Medication(entryDate = date, name = name.trim(), dose = dose.trim(), isAntihistamine = isAntihistamine)
            )
        }
    }

    fun deleteMedication(med: Medication) {
        viewModelScope.launch { repository.deleteMedication(med) }
    }

    fun checkMedicationSideEffects(name: String, dose: String) {
        _sideEffectsState.value = SideEffectsUiState.Loading(name)
        viewModelScope.launch {
            geminiService.getMedicationSideEffects(name, dose)
                .onSuccess { _sideEffectsState.value = SideEffectsUiState.Success(it) }
                .onFailure { _sideEffectsState.value = SideEffectsUiState.Error(it.message ?: "Ошибка") }
        }
    }

    fun dismissSideEffects() { _sideEffectsState.value = SideEffectsUiState.Idle }

    // ─── Vape ─────────────────────────────────────────────────────────────────

    fun copyYesterdayVape() {
        viewModelScope.launch {
            val today = _selectedDate.value
            // Look for any session on or before yesterday
            val prev = repository.getLatestVapeSessionBefore(today.minusDays(1)) ?: return@launch
            // Only copy if there's no session for today yet
            if (vapeSessions.value.isEmpty()) {
                repository.getOrCreateEntry(today)
                repository.insertVapeSession(prev.copy(id = 0, entryDate = today))
            }
        }
    }

    fun analyzeVapePhoto(imageUri: Uri) {
        _vapePhotoState.value = VapePhotoState.Analyzing
        viewModelScope.launch {
            try {
                val base64 = ImageUtils.uriToBase64(context, imageUri)
                geminiService.recognizeVapeFromPhoto(base64)
                    .onSuccess { result -> _vapePhotoState.value = VapePhotoState.Results(result) }
                    .onFailure { err -> _vapePhotoState.value = VapePhotoState.Error(err.message ?: "Ошибка распознавания") }
            } catch (e: Exception) {
                _vapePhotoState.value = VapePhotoState.Error(e.message ?: "Ошибка обработки фото")
            }
        }
    }

    fun confirmVapeFromPhoto(result: VapeRecognitionResult) {
        addVapeSession(result.brand, result.flavor, result.nicotineLevel, result.pgVgRatio, result.notes)
        _vapePhotoState.value = VapePhotoState.Idle
    }

    fun dismissVapePhoto() { _vapePhotoState.value = VapePhotoState.Idle }

    fun addVapeSession(brand: String, flavor: String, nicotineLevel: String, pgVgRatio: String, notes: String) {
        viewModelScope.launch {
            val date = _selectedDate.value
            repository.getOrCreateEntry(date)
            repository.insertVapeSession(
                VapeSession(
                    entryDate = date,
                    brand = brand.trim(),
                    flavor = flavor.trim(),
                    nicotineLevel = nicotineLevel.trim(),
                    pgVgRatio = pgVgRatio.trim(),
                    notes = notes.trim()
                )
            )
        }
    }

    fun deleteVapeSession(session: VapeSession) {
        viewModelScope.launch { repository.deleteVapeSession(session) }
    }

    // ─── Skin Condition ───────────────────────────────────────────────────────

    fun saveSkinCondition(
        overallSeverity: Int,
        redness: Int,
        itching: Int,
        rash: Int,
        swelling: Int,
        dryness: Int,
        affectedAreas: String,
        notes: String
    ) {
        viewModelScope.launch {
            val date = _selectedDate.value
            repository.getOrCreateEntry(date)
            val existing = repository.getSkinConditionForDate(date).firstOrNull()
            repository.saveSkinCondition(
                SkinCondition(
                    id = existing?.id ?: 0,
                    entryDate = date,
                    overallSeverity = overallSeverity,
                    redness = redness,
                    itching = itching,
                    rash = rash,
                    swelling = swelling,
                    dryness = dryness,
                    affectedAreas = affectedAreas,
                    notes = notes
                )
            )
            _toastMessage.emit("Состояние кожи сохранено")
        }
    }

    // ─── Symptoms ─────────────────────────────────────────────────────────────

    fun toggleSymptom(symptomType: SymptomType, severity: Int) {
        viewModelScope.launch {
            val date = _selectedDate.value
            repository.getOrCreateEntry(date)
            val existing = symptoms.value.find { it.symptomType == symptomType }
            if (existing != null) {
                if (severity == 0) repository.deleteSymptom(existing)
                else repository.updateSymptom(existing.copy(severity = severity))
            } else if (severity > 0) {
                repository.insertSymptom(AllergySymptom(entryDate = date, symptomType = symptomType, severity = severity))
            }
        }
    }
}

// ─── UI States ────────────────────────────────────────────────────────────────

sealed class AllergenicityUiState {
    object Idle : AllergenicityUiState()
    object Loading : AllergenicityUiState()
    data class Success(val result: ProductAllergenicityResponse) : AllergenicityUiState()
    data class Error(val message: String) : AllergenicityUiState()
}

sealed class LabelAnalysisState {
    object Idle : LabelAnalysisState()
    object Analyzing : LabelAnalysisState()
    data class Results(
        val result: com.allergia.data.models.LabelRecognitionResult,
        val photoUri: android.net.Uri
    ) : LabelAnalysisState()
    data class Error(val message: String) : LabelAnalysisState()
}

sealed class PhotoAnalysisState {
    object Idle : PhotoAnalysisState()
    object Analyzing : PhotoAnalysisState()
    object NoFoodDetected : PhotoAnalysisState()
    data class Results(
        val result: FoodRecognitionResult,
        val editableItems: MutableList<DetectedFoodItem>
    ) : PhotoAnalysisState()
    data class Error(val message: String) : PhotoAnalysisState()
}

sealed class SideEffectsUiState {
    object Idle : SideEffectsUiState()
    data class Loading(val medicationName: String) : SideEffectsUiState()
    data class Success(val result: MedicationSideEffectsResponse) : SideEffectsUiState()
    data class Error(val message: String) : SideEffectsUiState()
}

sealed class VapePhotoState {
    object Idle : VapePhotoState()
    object Analyzing : VapePhotoState()
    data class Results(val result: VapeRecognitionResult) : VapePhotoState()
    data class Error(val message: String) : VapePhotoState()
}
