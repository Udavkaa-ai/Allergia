package com.allergia.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allergia.api.AllergyTestAnalysisResult
import com.allergia.api.GeminiService
import com.allergia.data.models.AllergyTestResult
import com.allergia.data.repository.DiaryRepository
import com.allergia.utils.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AllergyTestViewModel @Inject constructor(
    private val repository: DiaryRepository,
    private val geminiService: GeminiService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val testResults: StateFlow<List<AllergyTestResult>> = repository.getAllAllergyTestResults()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _analyzeState = MutableStateFlow<AllergyTestUiState>(AllergyTestUiState.Idle)
    val analyzeState: StateFlow<AllergyTestUiState> = _analyzeState.asStateFlow()

    fun analyzePhoto(imageUri: Uri) {
        _analyzeState.value = AllergyTestUiState.Analyzing
        viewModelScope.launch {
            try {
                val base64 = ImageUtils.uriToBase64(context, imageUri)
                geminiService.analyzeAllergyTestPhoto(base64)
                    .onSuccess { result -> _analyzeState.value = AllergyTestUiState.Results(result) }
                    .onFailure { err -> _analyzeState.value = AllergyTestUiState.Error(err.message ?: "Ошибка анализа фото") }
            } catch (e: Exception) {
                _analyzeState.value = AllergyTestUiState.Error(e.message ?: "Ошибка обработки фото")
            }
        }
    }

    fun saveResult(
        result: AllergyTestAnalysisResult,
        testDate: LocalDate,
        photoPath: String?,
        notes: String
    ) {
        viewModelScope.launch {
            repository.insertAllergyTestResult(
                AllergyTestResult(
                    testDate = testDate,
                    photoPath = photoPath,
                    testType = result.testType,
                    labName = result.labName,
                    positiveAllergens = result.positiveAllergens.joinToString(", "),
                    borderlineAllergens = result.borderlineAllergens.joinToString(", "),
                    negativeAllergens = result.negativeAllergens.joinToString(", "),
                    summary = result.summary,
                    recommendation = result.recommendation,
                    notes = notes
                )
            )
            _analyzeState.value = AllergyTestUiState.Saved
        }
    }

    fun saveManual(
        testType: String,
        testDate: LocalDate,
        positiveAllergens: String,
        borderlineAllergens: String,
        labName: String,
        notes: String
    ) {
        viewModelScope.launch {
            repository.insertAllergyTestResult(
                AllergyTestResult(
                    testDate = testDate,
                    testType = testType,
                    labName = labName,
                    positiveAllergens = positiveAllergens,
                    borderlineAllergens = borderlineAllergens,
                    notes = notes
                )
            )
            _analyzeState.value = AllergyTestUiState.Saved
        }
    }

    fun deleteResult(result: AllergyTestResult) {
        viewModelScope.launch { repository.deleteAllergyTestResult(result) }
    }

    fun reset() { _analyzeState.value = AllergyTestUiState.Idle }
}

sealed class AllergyTestUiState {
    object Idle : AllergyTestUiState()
    object Analyzing : AllergyTestUiState()
    object Saved : AllergyTestUiState()
    data class Results(val result: AllergyTestAnalysisResult) : AllergyTestUiState()
    data class Error(val message: String) : AllergyTestUiState()
}
