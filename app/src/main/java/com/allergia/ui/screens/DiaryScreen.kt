package com.allergia.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.allergia.data.models.*
import com.allergia.ui.components.*
import com.allergia.ui.viewmodels.DiaryViewModel
import com.allergia.ui.viewmodels.PhotoAnalysisState
import com.allergia.utils.ImageUtils
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    onBack: () -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val selectedDate by viewModel.selectedDate.collectAsState()
    val foodItems by viewModel.foodItems.collectAsState()
    val medications by viewModel.medications.collectAsState()
    val skinCondition by viewModel.skinCondition.collectAsState()
    val symptoms by viewModel.symptoms.collectAsState()
    val photoState by viewModel.photoAnalysisState.collectAsState()

    val dateStr = selectedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru")))

    // URI для снимка камеры
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    // Показывать ли bottom-sheet с выбором источника
    var showPhotoSourceSheet by remember { mutableStateOf(false) }

    // Лаунчер камеры
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) cameraPhotoUri?.let { uri -> viewModel.analyzeFoodPhoto(uri) }
    }

    // Лаунчер галереи
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.analyzeFoodPhoto(it) }
    }

    // Запрос разрешения на камеру
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = ImageUtils.createTempPhotoFile(context)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraPhotoUri = uri
            cameraLauncher.launch(uri)
        }
    }

    fun launchCamera() {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                val file = ImageUtils.createTempPhotoFile(context)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                cameraPhotoUri = uri
                cameraLauncher.launch(uri)
            }
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // ── Dialogs overlay ──────────────────────────────────────────────────────

    when (val state = photoState) {
        is PhotoAnalysisState.Analyzing -> PhotoAnalyzingDialog()

        is PhotoAnalysisState.Results -> FoodRecognitionResultDialog(
            photoUri = cameraPhotoUri,
            detectedItems = state.editableItems,
            onConfirm = viewModel::confirmPhotoItems,
            onDismiss = viewModel::dismissPhotoAnalysis
        )

        is PhotoAnalysisState.NoFoodDetected -> {
            AlertDialog(
                onDismissRequest = viewModel::dismissPhotoAnalysis,
                title = { Text("Еда не найдена") },
                text = { Text("Gemma 3 не обнаружила продукты на фото. Попробуйте другой снимок с лучшим освещением.") },
                confirmButton = { TextButton(onClick = viewModel::dismissPhotoAnalysis) { Text("OK") } }
            )
        }

        is PhotoAnalysisState.Error -> {
            AlertDialog(
                onDismissRequest = viewModel::dismissPhotoAnalysis,
                title = { Text("Ошибка распознавания") },
                text = { Text(state.message) },
                confirmButton = { TextButton(onClick = viewModel::dismissPhotoAnalysis) { Text("OK") } }
            )
        }

        else -> {}
    }

    // Photo source chooser bottom sheet
    if (showPhotoSourceSheet) {
        ModalBottomSheet(onDismissRequest = { showPhotoSourceSheet = false }) {
            PhotoSourceSheet(
                onCamera = { launchCamera() },
                onGallery = { galleryLauncher.launch("image/*") },
                onDismiss = { showPhotoSourceSheet = false }
            )
        }
    }

    // ── Main content ─────────────────────────────────────────────────────────

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Дневник: $dateStr") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                },
                actions = {
                    // Кнопка фото — главная точка входа
                    IconButton(onClick = { showPhotoSourceSheet = true }) {
                        Icon(Icons.Default.CameraAlt, "Сфотографировать еду", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showPhotoSourceSheet = true },
                icon = { Icon(Icons.Default.CameraAlt, null) },
                text = { Text("Сфоткать еду") },
                containerColor = MaterialTheme.colorScheme.primaryContainer
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
            // AI photo tip banner
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Нажмите 📷 — Gemma 3 автоматически распознает блюда с фото",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            FoodSection(
                items = foodItems,
                onAdd = { name, amount, type -> viewModel.addFoodItem(name, amount, type) },
                onDelete = viewModel::deleteFoodItem,
                onPhotoClick = { showPhotoSourceSheet = true }
            )

            MedicationsSection(
                items = medications,
                onAdd = { name, dose, isAnti -> viewModel.addMedication(name, dose, isAnti) },
                onDelete = viewModel::deleteMedication
            )

            SkinConditionSection(
                condition = skinCondition,
                onSave = { overall, redness, itching, rash, swelling, dryness, areas, notes ->
                    viewModel.saveSkinCondition(overall, redness, itching, rash, swelling, dryness, areas, notes)
                }
            )

            SymptomsSection(
                symptoms = symptoms,
                onToggle = viewModel::toggleSymptom
            )

            Spacer(Modifier.height(80.dp)) // FAB clearance
        }
    }
}

// ─── Food Section ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodSection(
    items: List<FoodItem>,
    onAdd: (String, String, MealType) -> Unit,
    onDelete: (FoodItem) -> Unit,
    onPhotoClick: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "🍽 Питание и напитки",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                // Camera shortcut
                FilledTonalIconButton(onClick = onPhotoClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.CameraAlt, "Фото еды", modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(4.dp))
                // Manual add
                FilledTonalIconButton(onClick = { showDialog = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Add, "Добавить вручную", modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            if (items.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📷 Сфотографируйте еду или добавьте вручную", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            } else {
                val grouped = items.groupBy { it.mealType }
                grouped.forEach { (mealType, group) ->
                    Text(
                        mealType.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    group.forEach { food -> FoodItemRow(food = food, onDelete = { onDelete(food) }) }
                }
            }
        }
    }

    if (showDialog) {
        AddFoodDialog(
            onDismiss = { showDialog = false },
            onConfirm = { name, amount, type -> onAdd(name, amount, type); showDialog = false }
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
        food.allergenicityScore?.let { AllergenicityBadge(it, Modifier.padding(end = 4.dp)) }
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
        title = { Text("Добавить продукт вручную") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Количество (200г, 1 стакан)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(value = mealType.displayName, onValueChange = {}, readOnly = true, label = { Text("Приём пищи") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        MealType.entries.forEach { type ->
                            DropdownMenuItem(text = { Text(type.displayName) }, onClick = { mealType = type; expanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onConfirm(name, amount, mealType) }, enabled = name.isNotBlank()) { Text("Добавить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

// ─── Medications Section ──────────────────────────────────────────────────────

@Composable
private fun MedicationsSection(items: List<Medication>, onAdd: (String, String, Boolean) -> Unit, onDelete: (Medication) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    SectionCard(title = "💊 Медикаменты", onAdd = { showDialog = true }) {
        if (items.isEmpty()) {
            EmptyHint("Добавьте принятые лекарства")
        } else {
            items.forEach { med ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(med.name, style = MaterialTheme.typography.bodyMedium)
                        if (med.dose.isNotBlank()) Text(med.dose, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        if (med.isAntihistamine) Text("Антигистаминный", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2196F3))
                    }
                    IconButton(onClick = { onDelete(med) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) }
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
                    OutlinedTextField(value = dose, onValueChange = { dose = it }, label = { Text("Доза") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isAnti, onCheckedChange = { isAnti = it })
                        Text("Антигистаминный препарат")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { if (name.isNotBlank()) { onAdd(name, dose, isAnti); showDialog = false } }, enabled = name.isNotBlank()) { Text("Добавить") } },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Отмена") } }
        )
    }
}

// ─── Skin Condition Section ───────────────────────────────────────────────────

@Composable
private fun SkinConditionSection(condition: SkinCondition?, onSave: (Int, Int, Int, Int, Int, Int, String, String) -> Unit) {
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
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("🔬 Состояние кожи", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = { expanded = !expanded }) { Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null) }
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    SeverityRow("Общее", overall, { overall = it })
                    SeverityRow("Покраснение", redness, { redness = it })
                    SeverityRow("Зуд", itching, { itching = it })
                    SeverityRow("Сыпь", rash, { rash = it })
                    SeverityRow("Отёк", swelling, { swelling = it })
                    SeverityRow("Сухость", dryness, { dryness = it })
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = areas, onValueChange = { areas = it }, label = { Text("Поражённые области") }, placeholder = { Text("лицо, шея, руки...") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Заметки") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { onSave(overall, redness, itching, rash, swelling, dryness, areas, notes) }, modifier = Modifier.align(Alignment.End)) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Сохранить")
                    }
                }
            }
        }
    }
}

// ─── Symptoms Section ─────────────────────────────────────────────────────────

@Composable
private fun SymptomsSection(symptoms: List<AllergySymptom>, onToggle: (SymptomType, Int) -> Unit) {
    var expanded by remember { mutableStateOf(true) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("🤧 Симптомы аллергии", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = { expanded = !expanded }) { Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null) }
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    SymptomType.entries.forEach { type ->
                        val current = symptoms.find { it.symptomType == type }?.severity ?: 0
                        SeverityRow(label = "${type.emoji} ${type.displayName}", value = current, onValueChange = { onToggle(type, it) })
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun SectionCard(title: String, onAdd: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                FilledTonalIconButton(onClick = onAdd, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Add, "Добавить", modifier = Modifier.size(18.dp)) }
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
