package com.allergia.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allergia.api.AllergyAnalysisResponse
import com.allergia.api.GeminiService
import com.allergia.data.models.AnalysisResult
import com.allergia.data.models.TriggerSuspect
import com.allergia.data.repository.DiaryRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val repository: DiaryRepository,
    private val geminiService: GeminiService
) : ViewModel() {

    private val gson = Gson()

    val analyses: StateFlow<List<AnalysisResult>> = repository.getAllAnalyses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _analysisState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val analysisState: StateFlow<AnalysisUiState> = _analysisState.asStateFlow()

    private val _selectedPeriodDays = MutableStateFlow(14)
    val selectedPeriodDays: StateFlow<Int> = _selectedPeriodDays.asStateFlow()

    fun setPeriod(days: Int) { _selectedPeriodDays.value = days }

    fun startAnalysis() {
        _analysisState.value = AnalysisUiState.Loading("Сбор данных дневника...")
        viewModelScope.launch {
            val to = LocalDate.now()
            val from = to.minusDays(_selectedPeriodDays.value.toLong())

            val data = repository.getDataForRange(from, to)

            if (data.foods.isEmpty() && data.symptoms.isEmpty()) {
                _analysisState.value = AnalysisUiState.Error("Недостаточно данных за выбранный период. Заполните дневник питания и симптомов.")
                return@launch
            }

            _analysisState.value = AnalysisUiState.Loading("Анализ данных с Gemini 2.5...")

            geminiService.analyzeAllergyPatterns(data)
                .onSuccess { response ->
                    val triggers = response.suspectedTriggers.map { dto ->
                        TriggerSuspect(
                            name = dto.name,
                            type = dto.type,
                            probability = dto.probability,
                            evidence = dto.evidence,
                            confidenceLabel = dto.confidenceLabel
                        )
                    }

                    val analysisResult = AnalysisResult(
                        analyzedAt = LocalDateTime.now(),
                        periodStart = from,
                        periodEnd = to,
                        fullAnalysis = buildFullAnalysisText(response),
                        suspectedTriggers = gson.toJson(triggers),
                        overallRisk = response.overallRisk,
                        recommendations = response.recommendations.joinToString("\n")
                    )

                    repository.saveAnalysis(analysisResult)
                    _analysisState.value = AnalysisUiState.Success(analysisResult, response)
                }
                .onFailure { error ->
                    _analysisState.value = AnalysisUiState.Error(
                        "Ошибка анализа: ${error.message ?: "Неизвестная ошибка"}\n\nПроверьте API ключ в настройках."
                    )
                }
        }
    }

    fun loadLatestAnalysis() {
        viewModelScope.launch {
            repository.getLatestAnalysis()?.let { result ->
                _analysisState.value = AnalysisUiState.LoadedPrevious(result)
            }
        }
    }

    fun resetState() { _analysisState.value = AnalysisUiState.Idle }

    private fun buildFullAnalysisText(response: AllergyAnalysisResponse): String {
        return buildString {
            appendLine("РЕЗЮМЕ")
            appendLine(response.summary)
            appendLine()
            appendLine("ДИНАМИКА КОЖИ")
            appendLine(response.skinDynamics)
            appendLine()
            appendLine("ВЛИЯНИЕ МЕДИКАМЕНТОВ")
            appendLine(response.medicationEffect)
            appendLine()
            appendLine("ПЕРЕКРЁСТНАЯ РЕАКТИВНОСТЬ")
            response.crossReactivity.forEach { appendLine("• $it") }
            appendLine()
            appendLine("РЕКОМЕНДАЦИИ ПО ДИЕТЕ")
            appendLine(response.eliminationDietSuggestion)
            appendLine()
            appendLine("РЕКОМЕНДАЦИИ")
            response.recommendations.forEachIndexed { i, rec -> appendLine("${i + 1}. $rec") }
            appendLine()
            appendLine("⚠️ ${response.disclaimer}")
        }
    }
}

sealed class AnalysisUiState {
    object Idle : AnalysisUiState()
    data class Loading(val message: String) : AnalysisUiState()
    data class Success(val result: AnalysisResult, val response: AllergyAnalysisResponse) : AnalysisUiState()
    data class LoadedPrevious(val result: AnalysisResult) : AnalysisUiState()
    data class Error(val message: String) : AnalysisUiState()
}
