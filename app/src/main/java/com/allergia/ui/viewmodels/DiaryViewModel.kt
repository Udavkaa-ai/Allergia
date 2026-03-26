package com.allergia.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allergia.api.DetectedFoodItem
import com.allergia.api.FoodPhotoService
import com.allergia.api.FoodRecognitionResult
import com.allergia.api.GeminiService
import com.allergia.api.ProductAllergenicityResponse
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
    private val foodPhotoService: FoodPhotoService
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

    private val _photoAnalysisState = MutableStateFlow<PhotoAnalysisState>(PhotoAnalysisState.Idle)
    val photoAnalysisState: StateFlow<PhotoAnalysisState> = _photoAnalysisState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

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

    // ─── Food ────────────────────────────────────────────────────────────────

    fun addFoodItem(name: String, amount: String, mealType: MealType) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val date = _selectedDate.value
            repository.getOrCreateEntry(date)
            val item = FoodItem(entryDate = date, name = name.trim(), amount = amount.trim(), mealType = mealType)
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
