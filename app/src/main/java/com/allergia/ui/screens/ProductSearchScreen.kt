package com.allergia.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.allergia.api.ProductAllergenicityResponse
import com.allergia.ui.components.AllergenicityBadge
import com.allergia.ui.viewmodels.AllergenicityUiState
import com.allergia.ui.viewmodels.DiaryViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductSearchScreen(
    onBack: () -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val state by viewModel.allergenicityState.collectAsState()
    var query by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Оценка аллергенности") },
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
            Text(
                "Введите название продукта, и Gemini 2.5 оценит его аллергенность с указанием конкретных аллергенов, возможных реакций и безопасных альтернатив.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            // Search bar
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Название продукта") },
                placeholder = { Text("Например: арахис, молоко, яйца...") },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = {
                            viewModel.checkProductAllergenicity(query)
                        }) {
                            Icon(Icons.Default.Search, "Проверить")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { viewModel.checkProductAllergenicity(query) },
                enabled = query.isNotBlank() && state !is AllergenicityUiState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Science, null)
                Spacer(Modifier.width(8.dp))
                Text("Оценить аллергенность")
            }

            AnimatedContent(targetState = state, transitionSpec = { fadeIn() togetherWith fadeOut() }) { s ->
                when (s) {
                    is AllergenicityUiState.Idle -> CommonAllergensList()
                    is AllergenicityUiState.Loading -> LoadingAllergenCard()
                    is AllergenicityUiState.Success -> AllergenicityResultCard(s.result)
                    is AllergenicityUiState.Error -> {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text(s.message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommonAllergensList() {
    val big8 = listOf(
        "🥜 Арахис" to 0.95f,
        "🌾 Глютен (пшеница)" to 0.85f,
        "🥛 Молоко" to 0.80f,
        "🥚 Яйца" to 0.78f,
        "🐟 Рыба" to 0.75f,
        "🦐 Ракообразные" to 0.82f,
        "🌳 Древесные орехи" to 0.88f,
        "🫘 Соя" to 0.70f
    )

    Card {
        Column(Modifier.padding(16.dp)) {
            Text("Топ-8 аллергенов (ВОЗ)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            big8.forEach { (name, score) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    AllergenicityBadge(score)
                }
            }
        }
    }
}

@Composable
private fun LoadingAllergenCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(32.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator()
            Text("Gemini 2.5 анализирует продукт...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AllergenicityResultCard(result: ProductAllergenicityResponse) {
    val scoreColor = when {
        result.score >= 0.75f -> Color(0xFFE53935)
        result.score >= 0.5f -> Color(0xFFFFA726)
        result.score >= 0.25f -> Color(0xFFFFEE58)
        else -> Color(0xFF66BB6A)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Score header
        Card(colors = CardDefaults.cardColors(containerColor = scoreColor.copy(alpha = 0.12f))) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(result.productName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Гистамин: ${result.histaminContent}", style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${(result.score * 100).toInt()}%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = scoreColor)
                    Text(result.label, style = MaterialTheme.typography.labelMedium, color = scoreColor)
                }
            }
        }

        // Description
        Card {
            Column(Modifier.padding(16.dp)) {
                Text("Описание", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(result.description, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text("Источник: ${result.sources}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }

        // Allergens
        if (result.allergens.isNotEmpty()) {
            InfoChipsCard("⚠️ Содержит аллергены", result.allergens, Color(0xFFE53935))
        }

        // Cross-reactive
        if (result.crossReactiveWith.isNotEmpty()) {
            InfoChipsCard("🔗 Перекрёстная реактивность", result.crossReactiveWith, Color(0xFFFFA726))
        }

        // Common reactions
        if (result.commonReactions.isNotEmpty()) {
            InfoChipsCard("🤧 Типичные реакции", result.commonReactions, Color(0xFF9C27B0))
        }

        // Safe alternatives
        if (result.safeAlternatives.isNotEmpty()) {
            InfoChipsCard("✅ Безопасные альтернативы", result.safeAlternatives, Color(0xFF4CAF50))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoChipsCard(title: String, items: List<String>, color: Color) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items.forEach { item ->
                    Surface(color = color.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small) {
                        Text(item, style = MaterialTheme.typography.bodySmall, color = color, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
        }
    }
}
