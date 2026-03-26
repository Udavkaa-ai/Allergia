package com.allergia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.allergia.ui.theme.GreenPrimary
import com.allergia.ui.viewmodels.DiaryViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDiary: () -> Unit,
    onNavigateToAnalysis: () -> Unit,
    onNavigateToSearch: () -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val foodItems by viewModel.foodItems.collectAsState()
    val medications by viewModel.medications.collectAsState()
    val skinCondition by viewModel.skinCondition.collectAsState()
    val symptoms by viewModel.symptoms.collectAsState()

    val todayFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("ru"))
    val today = LocalDate.now()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Allergia", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Дневник аллергика",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, "Поиск аллергенов")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Calendar strip
            CalendarStrip(
                selectedDate = selectedDate,
                onDateSelected = viewModel::selectDate
            )

            Spacer(Modifier.height(8.dp))

            // Selected date header
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedDate.format(todayFormatter).replaceFirstChar { it.uppercaseChar() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (selectedDate == today) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = GreenPrimary,
                        shape = CircleShape
                    ) {
                        Text(
                            "Сегодня",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Summary cards — 2 rows of 3
            val cardMod = Modifier.weight(1f)
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryCard(icon = "🍽", label = "Продукты", count = foodItems.size, modifier = cardMod)
                SummaryCard(icon = "💊", label = "Медикам.", count = medications.size, modifier = cardMod)
                SummaryCard(icon = "🧴", label = "Химия", count = 0, modifier = cardMod)
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryCard(icon = "🔬", label = "Кожа", count = skinCondition?.overallSeverity ?: 0, suffix = "/3", modifier = cardMod)
                SummaryCard(icon = "🤧", label = "Симптомы", count = symptoms.size, modifier = cardMod)
                SummaryCard(icon = "📅", label = "Записей", count = allEntries.size, modifier = cardMod)
            }

            Spacer(Modifier.height(12.dp))

            // Quick actions
            QuickActionButton(
                icon = Icons.Default.Edit,
                title = "Заполнить дневник",
                subtitle = "Еда, медикаменты, кожа, симптомы",
                onClick = onNavigateToDiary
            )

            Spacer(Modifier.height(8.dp))

            QuickActionButton(
                icon = Icons.Default.Psychology,
                title = "AI-анализ зависимостей",
                subtitle = "Gemini 2.5 найдёт триггеры аллергии",
                onClick = onNavigateToAnalysis,
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )

            Spacer(Modifier.height(8.dp))

            QuickActionButton(
                icon = Icons.Default.Search,
                title = "Оценка аллергенности",
                subtitle = "Проверить любой продукт питания",
                onClick = onNavigateToSearch,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )

            Spacer(Modifier.height(16.dp))

            // Today's food preview
            if (foodItems.isNotEmpty()) {
                Text(
                    "Питание за день",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(4.dp))
                foodItems.take(5).forEach { food ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("• ${food.name}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        food.allergenicityScore?.let { score ->
                            val color = when {
                                score >= 0.75f -> Color(0xFFE53935)
                                score >= 0.5f -> Color(0xFFFFA726)
                                else -> Color(0xFF66BB6A)
                            }
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        }
                    }
                }
                if (foodItems.size > 5) {
                    Text(
                        "...ещё ${foodItems.size - 5}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 20.dp).clickable { onNavigateToDiary() }
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CalendarStrip(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val dates = (-15..0).map { today.plusDays(it.toLong()) }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        items(dates) { date ->
            val isSelected = date == selectedDate
            val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("ru"))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        if (isSelected) Color.White.copy(alpha = 0.3f)
                        else Color.Transparent
                    )
                    .clickable { onDateSelected(date) }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    dayOfWeek,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    "${date.dayOfMonth}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    icon: String,
    label: String,
    count: Int,
    suffix: String = "",
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp).fillMaxWidth()
        ) {
            Text(icon, style = MaterialTheme.typography.titleMedium)
            Text("$count$suffix", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
    }
}
