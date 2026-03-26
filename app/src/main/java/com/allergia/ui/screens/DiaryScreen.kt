package com.allergia.ui.screens

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
import com.allergia.data.models.*
import com.allergia.ui.components.AllergenicityBadge
import com.allergia.ui.components.SeverityRow
import com.allergia.ui.viewmodels.DiaryViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    onBack: () -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val foodItems by viewModel.foodItems.collectAsState()
    val medications by viewModel.medications.collectAsState()
    val skinCondition by viewModel.skinCondition.collectAsState()
    val symptoms by viewModel.symptoms.collectAsState()

    val dateStr = selectedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru")))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Дневник: $dateStr") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                },
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
            // Food Section
            FoodSection(
                items = foodItems,
                onAdd = { name, amount, type -> viewModel.addFoodItem(name, amount, type) },
                onDelete = viewModel::deleteFoodItem
            )

            // Medications Section
            MedicationsSection(
                items = medications,
                onAdd = { name, dose, isAnti -> viewModel.addMedication(name, dose, isAnti) },
                onDelete = viewModel::deleteMedication
            )

            // Skin Condition Section
            SkinConditionSection(
                condition = skinCondition,
                onSave = { overall, redness, itching, rash, swelling, dryness, areas, notes ->
                    viewModel.saveSkinCondition(overall, redness, itching, rash, swelling, dryness, areas, notes)
                }
            )

            // Symptoms Section
            SymptomsSection(
                symptoms = symptoms,
                onToggle = viewModel::toggleSymptom
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─── Food Section ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodSection(
    items: List<FoodItem>,
    onAdd: (String, String, MealType) -> Unit,
    onDelete: (FoodItem) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    SectionCard(
        title = "🍽 Питание и напитки",
        onAdd = { showDialog = true }
    ) {
        if (items.isEmpty()) {
            EmptyHint("Добавьте продукты, которые вы ели")
        } else {
            val grouped = items.groupBy { it.mealType }
            grouped.forEach { (mealType, group) ->
                Text(
                    mealType.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                group.forEach { food ->
                    FoodItemRow(food = food, onDelete = { onDelete(food) })
                }
            }
        }
    }

    if (showDialog) {
        AddFoodDialog(
            onDismiss = { showDialog = false },
            onConfirm = { name, amount, type ->
                onAdd(name, amount, type)
                showDialog = false
            }
        )
    }
}

@Composable
private fun FoodItemRow(food: FoodItem, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(food.name, style = MaterialTheme.typography.bodyMedium)
            if (food.amount.isNotBlank()) {
                Text(food.amount, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            if (food.knownAllergens.isNotBlank()) {
                Text("⚠ ${food.knownAllergens}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFA726))
            }
        }
        food.allergenicityScore?.let {
            AllergenicityBadge(it, Modifier.padding(end = 4.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFoodDialog(onDismiss: () -> Unit, onConfirm: (String, String, MealType) -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var mealType by remember { mutableStateOf(MealType.OTHER) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить продукт") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название продукта *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Количество (например: 200г)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = mealType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Приём пищи") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        MealType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = { mealType = type; expanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name, amount, mealType) }, enabled = name.isNotBlank()) {
                Text("Добавить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

// ─── Medications Section ──────────────────────────────────────────────────────

@Composable
private fun MedicationsSection(
    items: List<Medication>,
    onAdd: (String, String, Boolean) -> Unit,
    onDelete: (Medication) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    SectionCard(title = "💊 Медикаменты", onAdd = { showDialog = true }) {
        if (items.isEmpty()) {
            EmptyHint("Добавьте принятые лекарства")
        } else {
            items.forEach { med ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(med.name, style = MaterialTheme.typography.bodyMedium)
                        if (med.dose.isNotBlank()) {
                            Text(med.dose, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        if (med.isAntihistamine) {
                            Text("Антигистаминный", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2196F3))
                        }
                    }
                    IconButton(onClick = { onDelete(med) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    if (showDialog) {
        var name by remember { mutableStateOf("") }
        var dose by remember { mutableStateOf("") }
        var isAnti by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Добавить медикамент") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = dose, onValueChange = { dose = it }, label = { Text("Доза (например: 10 мг)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isAnti, onCheckedChange = { isAnti = it })
                        Text("Антигистаминный препарат")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { if (name.isNotBlank()) { onAdd(name, dose, isAnti); showDialog = false } }, enabled = name.isNotBlank()) { Text("Добавить") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Отмена") } }
        )
    }
}

// ─── Skin Condition Section ───────────────────────────────────────────────────

@Composable
private fun SkinConditionSection(
    condition: SkinCondition?,
    onSave: (Int, Int, Int, Int, Int, Int, String, String) -> Unit
) {
    var overall by remember(condition) { mutableIntStateOf(condition?.overallSeverity ?: 0) }
    var redness by remember(condition) { mutableIntStateOf(condition?.redness ?: 0) }
    var itching by remember(condition) { mutableIntStateOf(condition?.itching ?: 0) }
    var rash by remember(condition) { mutableIntStateOf(condition?.rash ?: 0) }
    var swelling by remember(condition) { mutableIntStateOf(condition?.swelling ?: 0) }
    var dryness by remember(condition) { mutableIntStateOf(condition?.dryness ?: 0) }
    var areas by remember(condition) { mutableStateOf(condition?.affectedAreas ?: "") }
    var notes by remember(condition) { mutableStateOf(condition?.notes ?: "") }
    var expanded by remember { mutableStateOf(condition != null && condition.overallSeverity > 0) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔬 Состояние кожи", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                }
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                SeverityRow("Общее", overall, { overall = it })
                SeverityRow("Покраснение", redness, { redness = it })
                SeverityRow("Зуд", itching, { itching = it })
                SeverityRow("Сыпь", rash, { rash = it })
                SeverityRow("Отёк", swelling, { swelling = it })
                SeverityRow("Сухость", dryness, { dryness = it })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = areas,
                    onValueChange = { areas = it },
                    label = { Text("Поражённые области") },
                    placeholder = { Text("лицо, шея, руки...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Заметки") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onSave(overall, redness, itching, rash, swelling, dryness, areas, notes) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Сохранить")
                }
            }
        }
    }
}

// ─── Symptoms Section ─────────────────────────────────────────────────────────

@Composable
private fun SymptomsSection(
    symptoms: List<AllergySymptom>,
    onToggle: (SymptomType, Int) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("🤧 Симптомы аллергии", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                }
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                SymptomType.entries.forEach { type ->
                    val current = symptoms.find { it.symptomType == type }?.severity ?: 0
                    SeverityRow(
                        label = "${type.emoji} ${type.displayName}",
                        value = current,
                        onValueChange = { onToggle(type, it) }
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun SectionCard(
    title: String,
    onAdd: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                FilledTonalIconButton(onClick = onAdd, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Add, "Добавить", modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 8.dp))
}
