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
import com.allergia.ui.viewmodels.LabelAnalysisState
import com.allergia.ui.viewmodels.PhotoAnalysisState
import com.allergia.ui.viewmodels.SideEffectsUiState
import com.allergia.ui.viewmodels.VapePhotoState
import com.allergia.utils.ImageUtils
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    onBack: () -> Unit,
    onNavigateToArchive: () -> Unit = {},
    initialDate: java.time.LocalDate = java.time.LocalDate.now(),
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Set initial date from navigation argument
    LaunchedEffect(initialDate) {
        viewModel.selectDate(initialDate)
    }

    val selectedDate by viewModel.selectedDate.collectAsState()
    val foodItems by viewModel.foodItems.collectAsState()
    val medications by viewModel.medications.collectAsState()
    val skinCondition by viewModel.skinCondition.collectAsState()
    val symptoms by viewModel.symptoms.collectAsState()
    val photoState by viewModel.photoAnalysisState.collectAsState()
    val labelState by viewModel.labelAnalysisState.collectAsState()
    val householdProducts by viewModel.householdProducts.collectAsState()
    val vapeSessions by viewModel.vapeSessions.collectAsState()
    val sideEffectsState by viewModel.sideEffectsState.collectAsState()
    val vapePhotoState by viewModel.vapePhotoState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val dateStr = selectedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru")))

    // URI для снимка камеры (еда)
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    // URI для снимка этикетки (химия/косметика)
    var labelPhotoUri by remember { mutableStateOf<Uri?>(null) }
    // URI для снимка вейпа
    var vapePhotoUri by remember { mutableStateOf<Uri?>(null) }

    var showPhotoSourceSheet by remember { mutableStateOf(false) }
    var showLabelPhotoSheet by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showVapePhotoSheet by remember { mutableStateOf(false) }

    // Лаунчер камеры (еда)
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraPhotoUri?.let { viewModel.analyzeFoodPhoto(it) }
    }
    // Лаунчер галереи (еда)
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.analyzeFoodPhoto(it) }
    }

    // Лаунчер камеры (этикетка)
    val labelCameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) labelPhotoUri?.let { viewModel.analyzeLabelPhoto(it) }
    }
    // Лаунчер галереи (этикетка)
    val labelGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { labelPhotoUri = it; viewModel.analyzeLabelPhoto(it) }
    }

    // Лаунчер камеры (вейп)
    val vapeCameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) vapePhotoUri?.let { viewModel.analyzeVapePhoto(it) }
    }
    // Лаунчер галереи (вейп)
    val vapeGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vapePhotoUri = it; viewModel.analyzeVapePhoto(it) }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // re-triggered per action below; just a guard
    }

    fun launchFoodCamera() {
        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            val file = ImageUtils.createTempPhotoFile(context)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraPhotoUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun launchLabelCamera() {
        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            val file = ImageUtils.createTempPhotoFile(context)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            labelPhotoUri = uri
            labelCameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun launchVapeCamera() {
        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            val file = ImageUtils.createTempPhotoFile(context)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            vapePhotoUri = uri
            vapeCameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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

    // Label analysis dialogs
    when (val ls = labelState) {
        is LabelAnalysisState.Analyzing -> LabelAnalyzingDialog()
        is LabelAnalysisState.Results -> LabelResultDialog(
            result = ls.result,
            photoUri = ls.photoUri,
            onConfirm = { name, brand, category ->
                viewModel.confirmHouseholdProduct(name, brand, category, ls.result, ls.photoUri.toString())
            },
            onDismiss = viewModel::dismissLabelAnalysis
        )
        is LabelAnalysisState.Error -> AlertDialog(
            onDismissRequest = viewModel::dismissLabelAnalysis,
            title = { Text("Ошибка распознавания") },
            text = { Text(ls.message) },
            confirmButton = { TextButton(onClick = viewModel::dismissLabelAnalysis) { Text("OK") } }
        )
        else -> {}
    }

    // Side effects dialog
    when (val se = sideEffectsState) {
        is SideEffectsUiState.Loading -> {
            AlertDialog(
                onDismissRequest = viewModel::dismissSideEffects,
                title = { Text("Побочные эффекты") },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Запрашиваю данные о «${se.medicationName}»…")
                    }
                },
                confirmButton = { TextButton(onClick = viewModel::dismissSideEffects) { Text("Закрыть") } }
            )
        }
        is SideEffectsUiState.Success -> {
            MedicationSideEffectsDialog(result = se.result, onDismiss = viewModel::dismissSideEffects)
        }
        is SideEffectsUiState.Error -> {
            AlertDialog(
                onDismissRequest = viewModel::dismissSideEffects,
                title = { Text("Ошибка") },
                text = { Text(se.message) },
                confirmButton = { TextButton(onClick = viewModel::dismissSideEffects) { Text("OK") } }
            )
        }
        else -> {}
    }

    // Vape photo state dialogs
    when (val vs = vapePhotoState) {
        is VapePhotoState.Analyzing -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Распознавание вейпа") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Анализирую фото...")
                }
            },
            confirmButton = {}
        )
        is VapePhotoState.Results -> VapePhotoResultDialog(
            result = vs.result,
            onConfirm = { viewModel.confirmVapeFromPhoto(vs.result) },
            onDismiss = viewModel::dismissVapePhoto
        )
        is VapePhotoState.Error -> AlertDialog(
            onDismissRequest = viewModel::dismissVapePhoto,
            title = { Text("Ошибка распознавания") },
            text = { Text(vs.message) },
            confirmButton = { TextButton(onClick = viewModel::dismissVapePhoto) { Text("OK") } }
        )
        else -> {}
    }

    // Category chooser — Food vs Chemistry
    if (showCategorySheet) {
        ModalBottomSheet(onDismissRequest = { showCategorySheet = false }) {
            Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
                Text("Что сфотографировать?", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ElevatedButton(
                        onClick = { showCategorySheet = false; showPhotoSourceSheet = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🍽", style = MaterialTheme.typography.headlineMedium)
                            Text("Питание", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    ElevatedButton(
                        onClick = { showCategorySheet = false; showLabelPhotoSheet = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🧴", style = MaterialTheme.typography.headlineMedium)
                            Text("Химия / Косметика", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // Food photo source sheet
    if (showPhotoSourceSheet) {
        ModalBottomSheet(onDismissRequest = { showPhotoSourceSheet = false }) {
            PhotoSourceSheet(
                onCamera = { launchFoodCamera() },
                onGallery = { galleryLauncher.launch("image/*") },
                onDismiss = { showPhotoSourceSheet = false }
            )
        }
    }

    // Label photo source sheet
    if (showLabelPhotoSheet) {
        ModalBottomSheet(onDismissRequest = { showLabelPhotoSheet = false }) {
            LabelPhotoSourceSheet(
                onCamera = { launchLabelCamera() },
                onGallery = { labelGalleryLauncher.launch("image/*") },
                onDismiss = { showLabelPhotoSheet = false }
            )
        }
    }

    // Vape photo source sheet
    if (showVapePhotoSheet) {
        ModalBottomSheet(onDismissRequest = { showVapePhotoSheet = false }) {
            Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
                Text("Сфотографируйте вейп или жидкость", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ElevatedButton(
                        onClick = { showVapePhotoSheet = false; launchVapeCamera() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📷", style = MaterialTheme.typography.headlineMedium)
                            Text("Камера", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    ElevatedButton(
                        onClick = { showVapePhotoSheet = false; vapeGalleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🖼", style = MaterialTheme.typography.headlineMedium)
                            Text("Галерея", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
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
                actions = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCategorySheet = true },
                icon = { Icon(Icons.Default.CameraAlt, null) },
                text = { Text("Добавить фото") },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FoodSection(
                items = foodItems,
                onAdd = { name, amount, type -> viewModel.addFoodItem(name, amount, type) },
                onDelete = viewModel::deleteFoodItem,
                onPhotoClick = { showPhotoSourceSheet = true },
                onArchiveClick = onNavigateToArchive
            )

            MedicationsSection(
                items = medications,
                onAdd = { name, dose, isAnti -> viewModel.addMedication(name, dose, isAnti) },
                onDelete = viewModel::deleteMedication,
                onSideEffects = { name, dose -> viewModel.checkMedicationSideEffects(name, dose) }
            )

            HouseholdProductsSection(
                items = householdProducts,
                onAddManual = { name, brand, cat, persist -> viewModel.addHouseholdProductManualPersistent(name, brand, cat, persist) },
                onDelete = viewModel::deleteHouseholdProduct,
                onTogglePersistence = viewModel::toggleProductPersistence,
                onScanLabel = { showLabelPhotoSheet = true }
            )

            VapeSection(
                sessions = vapeSessions,
                onAdd = { brand, flavor, nic, pgvg, notes -> viewModel.addVapeSession(brand, flavor, nic, pgvg, notes) },
                onDelete = viewModel::deleteVapeSession,
                onCopyYesterday = viewModel::copyYesterdayVape,
                onScanPhoto = { showVapePhotoSheet = true }
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
    onPhotoClick: () -> Unit,
    onArchiveClick: () -> Unit = {}
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
                // Photo archive
                FilledTonalIconButton(onClick = onArchiveClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.PhotoLibrary, "Архив фото", modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(4.dp))
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
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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

// ─── Household Products Section ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HouseholdProductsSection(
    items: List<HouseholdProduct>,
    onAddManual: (String, String, ProductCategory, Boolean) -> Unit,
    onDelete: (HouseholdProduct) -> Unit,
    onTogglePersistence: (HouseholdProduct) -> Unit,
    onScanLabel: () -> Unit
) {
    var showManualDialog by remember { mutableStateOf(false) }
    var sortByAllergenicity by remember { mutableStateOf(false) }

    val displayItems = if (sortByAllergenicity) {
        items.sortedBy { it.allergenicityScore ?: 0f }
    } else {
        items
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "🧴 Химия и косметика",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                // Sort toggle
                FilledTonalIconButton(
                    onClick = { sortByAllergenicity = !sortByAllergenicity },
                    modifier = Modifier.size(36.dp),
                    colors = if (sortByAllergenicity)
                        IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    else
                        IconButtonDefaults.filledTonalIconButtonColors()
                ) {
                    Icon(Icons.Default.Sort, "Сортировка по аллергенности", modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(4.dp))
                FilledTonalIconButton(onClick = onScanLabel, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.DocumentScanner, "Сканировать состав", modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(4.dp))
                FilledTonalIconButton(onClick = { showManualDialog = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Add, "Добавить вручную", modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            // Count persistent vs one-time
            val persistentCount = items.count { it.isPersistent }
            val hint = buildString {
                if (persistentCount > 0) append("📌 $persistentCount постоянных · ")
                if (sortByAllergenicity) append("↑ Сортировка: от наименее аллергенного")
                else if (persistentCount == 0) append("Добавьте средства, которые используете ежедневно")
            }.ifBlank { "Добавьте средства, которые используете ежедневно" }
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Spacer(Modifier.height(8.dp))

            if (items.isEmpty()) {
                EmptyHint("📷 Сканируйте этикетку или добавьте вручную")
            } else if (sortByAllergenicity) {
                // Flat sorted list (no category grouping when sorted)
                displayItems.forEach { product ->
                    HouseholdProductRow(
                        product = product,
                        onDelete = { onDelete(product) },
                        onTogglePersistence = { onTogglePersistence(product) }
                    )
                }
            } else {
                displayItems.groupBy { it.category }.forEach { (cat, group) ->
                    Text(
                        "${cat.emoji} ${cat.displayName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    group.forEach { product ->
                        HouseholdProductRow(
                            product = product,
                            onDelete = { onDelete(product) },
                            onTogglePersistence = { onTogglePersistence(product) }
                        )
                    }
                }
            }
        }
    }

    if (showManualDialog) {
        AddHouseholdProductDialog(
            onDismiss = { showManualDialog = false },
            onConfirm = { name, brand, cat, persist -> onAddManual(name, brand, cat, persist); showManualDialog = false }
        )
    }
}

@Composable
private fun HouseholdProductRow(
    product: HouseholdProduct,
    onDelete: () -> Unit,
    onTogglePersistence: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(product.name, style = MaterialTheme.typography.bodyMedium)
                if (product.brand.isNotBlank()) {
                    Spacer(Modifier.width(4.dp))
                    Text(product.brand, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
            if (product.allergenicIngredients.isNotBlank()) {
                Text("⚠ ${product.allergenicIngredients}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE53935))
            }
        }
        product.allergenicityScore?.let { score ->
            AllergenicityBadge(score, Modifier.padding(end = 4.dp))
        }
        // Pin / unpin toggle
        IconButton(onClick = onTogglePersistence, modifier = Modifier.size(32.dp)) {
            Icon(
                if (product.isPersistent) Icons.Default.PushPin else Icons.Default.PushPin,
                contentDescription = if (product.isPersistent) "Убрать из постоянных" else "Сделать постоянным",
                modifier = Modifier.size(16.dp),
                tint = if (product.isPersistent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddHouseholdProductDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, ProductCategory, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ProductCategory.OTHER) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var isPersistent by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить продукт") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Бренд") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                    OutlinedTextField(
                        value = "${category.emoji} ${category.displayName}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Категория") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        ProductCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${cat.emoji} ${cat.displayName}") },
                                onClick = { category = cat; categoryExpanded = false }
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = isPersistent, onCheckedChange = { isPersistent = it })
                    Column {
                        Text("📌 Показывать каждый день", style = MaterialTheme.typography.bodyMedium)
                        Text("Не нужно вносить повторно", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name, brand, category, isPersistent) }, enabled = name.isNotBlank()) { Text("Добавить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

// ─── Medications Section ──────────────────────────────────────────────────────

@Composable
private fun MedicationsSection(
    items: List<Medication>,
    onAdd: (String, String, Boolean) -> Unit,
    onDelete: (Medication) -> Unit,
    onSideEffects: (String, String) -> Unit
) {
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
                    IconButton(onClick = { onSideEffects(med.name, med.dose) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Info, "Побочные эффекты", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
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
                    SeverityRow("Шелушение", dryness, { dryness = it })
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

// ─── Vape Section ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VapeSection(
    sessions: List<VapeSession>,
    onAdd: (String, String, String, String, String) -> Unit,
    onDelete: (VapeSession) -> Unit,
    onCopyYesterday: () -> Unit = {},
    onScanPhoto: () -> Unit = {}
) {
    var showDialog by remember { mutableStateOf(false) }
    var prefillResult by remember { mutableStateOf<com.allergia.api.VapeRecognitionResult?>(null) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("💨 Вейп", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (sessions.isEmpty()) {
                    TextButton(
                        onClick = onCopyYesterday,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("как вчера", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.width(2.dp))
                }
                FilledTonalIconButton(onClick = onScanPhoto, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.CameraAlt, "Сканировать фото", modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(4.dp))
                FilledTonalIconButton(onClick = { showDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Add, "Добавить", modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            if (sessions.isEmpty()) {
                EmptyHint("Добавьте информацию о вейпе")
            } else {
                sessions.forEach { s ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            val title = listOfNotNull(
                                s.brand.takeIf { it.isNotBlank() },
                                s.flavor.takeIf { it.isNotBlank() }
                            ).joinToString(" — ").ifBlank { "Вейп" }
                            Text(title, style = MaterialTheme.typography.bodyMedium)
                            val details = listOfNotNull(
                                s.nicotineLevel.takeIf { it.isNotBlank() }?.let { "Ник: $it" },
                                s.pgVgRatio.takeIf { it.isNotBlank() }?.let { "PG/VG: $it" }
                            ).joinToString(" · ")
                            if (details.isNotBlank()) Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            if (s.notes.isNotBlank()) Text(s.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                        IconButton(onClick = { onDelete(s) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        val pre = prefillResult
        var brand by remember(pre) { mutableStateOf(pre?.brand ?: "") }
        var flavor by remember(pre) { mutableStateOf(pre?.flavor ?: "") }
        var nicotineLevel by remember(pre) { mutableStateOf(pre?.nicotineLevel ?: "") }
        var pgVgRatio by remember(pre) { mutableStateOf(pre?.pgVgRatio ?: "") }
        var notes by remember(pre) { mutableStateOf(pre?.notes ?: "") }
        AlertDialog(
            onDismissRequest = { showDialog = false; prefillResult = null },
            title = { Text("Добавить запись о вейпе") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Бренд/Устройство") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = flavor, onValueChange = { flavor = it }, label = { Text("Вкус жидкости") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = nicotineLevel, onValueChange = { nicotineLevel = it }, label = { Text("Никотин (например, 6 мг)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = pgVgRatio, onValueChange = { pgVgRatio = it }, label = { Text("PG/VG соотношение") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Заметки") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                }
            },
            confirmButton = {
                TextButton(onClick = { onAdd(brand, flavor, nicotineLevel, pgVgRatio, notes); showDialog = false; prefillResult = null }) { Text("Добавить") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false; prefillResult = null }) { Text("Отмена") } }
        )
    }
}

// ─── Vape Photo Result Dialog ───────────────────────────────────��──────────────

@Composable
private fun VapePhotoResultDialog(
    result: com.allergia.api.VapeRecognitionResult,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Распознан вейп") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (result.brand.isNotBlank()) Text("Бренд: ${result.brand}", style = MaterialTheme.typography.bodyMedium)
                if (result.flavor.isNotBlank()) Text("Вкус: ${result.flavor}", style = MaterialTheme.typography.bodyMedium)
                if (result.nicotineLevel.isNotBlank()) Text("Никотин: ${result.nicotineLevel}", style = MaterialTheme.typography.bodySmall)
                if (result.pgVgRatio.isNotBlank()) Text("PG/VG: ${result.pgVgRatio}", style = MaterialTheme.typography.bodySmall)
                if (result.notes.isNotBlank()) Text(result.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Добавить в дневник") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

// ─── Medication Side Effects Dialog ───────────────────────────────────────────

@Composable
private fun MedicationSideEffectsDialog(
    result: com.allergia.api.MedicationSideEffectsResponse,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Побочные эффекты: ${result.medicationName}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (result.allergyRelated.isNotEmpty()) {
                    Text("Аллергические реакции", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    result.allergyRelated.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }
                if (result.skinReactions.isNotEmpty()) {
                    Text("Кожные реакции", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    result.skinReactions.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }
                if (result.commonSideEffects.isNotEmpty()) {
                    Text("Частые побочные эффекты", style = MaterialTheme.typography.labelMedium)
                    result.commonSideEffects.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }
                if (result.crossReactivity.isNotEmpty()) {
                    Text("Перекрёстная реактивность", style = MaterialTheme.typography.labelMedium)
                    result.crossReactivity.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }
                if (result.importantWarnings.isNotEmpty()) {
                    result.importantWarnings.forEach {
                        Text("⚠ $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                if (result.allergenicPotential.isNotBlank()) {
                    Text("Аллергенный потенциал: ${result.allergenicPotential}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                if (result.recommendation.isNotBlank()) {
                    HorizontalDivider()
                    Text(result.recommendation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } }
    )
}
