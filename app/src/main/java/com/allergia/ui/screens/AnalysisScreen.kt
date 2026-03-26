package com.allergia.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.allergia.api.AllergyAnalysisResponse
import com.allergia.data.models.AnalysisResult
import com.allergia.ui.viewmodels.AnalysisUiState
import com.allergia.ui.viewmodels.AnalysisViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    onBack: () -> Unit,
    viewModel: AnalysisViewModel = hiltViewModel()
) {
    val state by viewModel.analysisState.collectAsState()
    val periodDays by viewModel.selectedPeriodDays.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadLatestAnalysis() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI-анализ аллергии") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Period selector
            PeriodSelector(
                selectedDays = periodDays,
                onSelect = viewModel::setPeriod
            )

            // Analyse button
            Button(
                onClick = viewModel::startAnalysis,
                enabled = state !is AnalysisUiState.Loading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Psychology, null)
                Spacer(Modifier.width(8.dp))
                Text("Запустить AI-анализ (Gemini 2.5)")
            }

            AnimatedContent(targetState = state, transitionSpec = { fadeIn() togetherWith fadeOut() }) { s ->
                when (s) {
                    is AnalysisUiState.Idle -> {}
                    is AnalysisUiState.Loading -> LoadingCard(s.message)
                    is AnalysisUiState.Success -> AnalysisResultCard(s.result, s.response)
                    is AnalysisUiState.LoadedPrevious -> PreviousAnalysisCard(s.result)
                    is AnalysisUiState.Error -> ErrorCard(s.message) { viewModel.resetState() }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PeriodSelector(selectedDays: Int, onSelect: (Int) -> Unit) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Период анализа", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7 to "7 дней", 14 to "14 дней", 30 to "30 дней").forEach { (days, label) ->
                    FilterChip(
                        selected = selectedDays == days,
                        onClick = { onSelect(days) },
                        label = { Text(label) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingCard(message: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(32.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(message, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Gemini 2.5 анализирует ваш дневник...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun AnalysisResultCard(result: AnalysisResult, response: AllergyAnalysisResponse) {
    val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale("ru"))

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Header with risk
        val (riskColor, riskLabel) = when (response.overallRisk) {
            "high" -> Color(0xFFE53935) to "Высокий риск"
            "medium" -> Color(0xFFFFA726) to "Средний риск"
            else -> Color(0xFF66BB6A) to "Низкий риск"
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = riskColor.copy(alpha = 0.1f))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(riskColor)
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(riskLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = riskColor)
                    Text(result.analyzedAt.format(dateFormatter), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Summary
        Card {
            Column(Modifier.padding(16.dp)) {
                Text("Резюме", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(response.summary, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Suspected triggers
        if (response.suspectedTriggers.isNotEmpty()) {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Подозреваемые триггеры", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    response.suspectedTriggers.sortedByDescending { it.probability }.forEach { trigger ->
                        TriggerRow(
                            name = trigger.name,
                            type = trigger.type,
                            probability = trigger.probability,
                            evidence = trigger.evidence,
                            confidenceLabel = trigger.confidenceLabel
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }

        // Patterns
        if (response.patterns.isNotEmpty()) {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Выявленные паттерны", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    response.patterns.forEach { pattern ->
                        Row(Modifier.padding(vertical = 4.dp)) {
                            Text("•", modifier = Modifier.padding(end = 8.dp))
                            Column {
                                Text(pattern.description, style = MaterialTheme.typography.bodyMedium)
                                Text(pattern.frequency, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        // Cross-reactivity
        if (response.crossReactivity.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Перекрёстная реактивность", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    response.crossReactivity.forEach { item ->
                        Text("• $item", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // Elimination diet
        if (response.eliminationDietSuggestion.isNotBlank()) {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("🥗 Элиминационная диета", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(response.eliminationDietSuggestion, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Recommendations
        if (response.recommendations.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Рекомендации", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    response.recommendations.forEachIndexed { i, rec ->
                        Text("${i + 1}. $rec", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }

        // Disclaimer
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Text(
                "⚠️ ${response.disclaimer}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun TriggerRow(name: String, type: String, probability: Float, evidence: String, confidenceLabel: String) {
    val pct = (probability * 100).toInt()
    val barColor = when {
        pct >= 75 -> Color(0xFFE53935)
        pct >= 50 -> Color(0xFFFFA726)
        else -> Color(0xFFFFEE58)
    }
    val typeEmoji = when (type) { "food" -> "🍽"; "medication" -> "💊"; else -> "🌿" }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$typeEmoji $name", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text("$pct%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = barColor)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { probability },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = barColor,
            trackColor = barColor.copy(alpha = 0.2f)
        )
        Spacer(Modifier.height(4.dp))
        Row {
            Surface(color = barColor.copy(alpha = 0.15f), shape = MaterialTheme.shapes.extraSmall) {
                Text(confidenceLabel, style = MaterialTheme.typography.labelSmall, color = barColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(evidence, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
private fun PreviousAnalysisCard(result: AnalysisResult) {
    val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale("ru"))
    Card {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Последний анализ: ${result.analyzedAt.format(dateFormatter)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Период: ${result.periodStart} — ${result.periodEnd}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(8.dp))
            Text(result.fullAnalysis, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ErrorCard(message: String, onDismiss: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text("Ошибка", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
