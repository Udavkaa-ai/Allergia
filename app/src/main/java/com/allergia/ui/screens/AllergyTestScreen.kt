package com.allergia.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.allergia.api.AllergyTestAnalysisResult
import com.allergia.data.models.AllergyTestResult
import com.allergia.ui.viewmodels.AllergyTestUiState
import com.allergia.ui.viewmodels.AllergyTestViewModel
import com.allergia.utils.ImageUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllergyTestScreen(
    onBack: () -> Unit,
    viewModel: AllergyTestViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val analyzeState by viewModel.analyzeState.collectAsState()
    val testResults by viewModel.testResults.collectAsState()
    val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))

    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var savedPhotoPath by remember { mutableStateOf<String?>(null) }
    var showManualDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var pendingResult by remember { mutableStateOf<AllergyTestAnalysisResult?>(null) }
    var saveNotes by remember { mutableStateOf("") }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraUri?.let { viewModel.analyzePhoto(it) }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.analyzePhoto(it) }
    }

    fun launchCamera() {
        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            val file = ImageUtils.createTempPhotoFile(context)
            savedPhotoPath = file.absolutePath
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Handle "Saved" state — reset after save
    LaunchedEffect(analyzeState) {
        if (analyzeState is AllergyTestUiState.Saved) {
            viewModel.reset()
            showSaveDialog = false
            saveNotes = ""
        }
    }

    // Results → show save dialog
    if (analyzeState is AllergyTestUiState.Results && !showSaveDialog) {
        pendingResult = (analyzeState as AllergyTestUiState.Results).result
        showSaveDialog = true
    }

    // Save dialog
    if (showSaveDialog && pendingResult != null) {
        val result = pendingResult!!
        AlertDialog(
            onDismissRequest = { showSaveDialog = false; viewModel.reset() },
            title = { Text("Результат теста") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Тип: ${result.testType.ifBlank { "Тест на аллергены" }}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    if (result.labName.isNotBlank())
                        Text("Лаборатория: ${result.labName}", style = MaterialTheme.typography.bodySmall)
                    if (result.positiveAllergens.isNotEmpty()) {
                        Text("✅ Положительные:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                        result.positiveAllergens.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    }
                    if (result.borderlineAllergens.isNotEmpty()) {
                        Text("⚠️ Пограничные:", style = MaterialTheme.typography.labelMedium, color = Color(0xFFFFA726))
                        result.borderlineAllergens.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    }
                    if (result.negativeAllergens.isNotEmpty()) {
                        Text("✅ Отрицательные:", style = MaterialTheme.typography.labelMedium, color = Color(0xFF4CAF50))
                        Text(result.negativeAllergens.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                    }
                    if (result.summary.isNotBlank()) {
                        HorizontalDivider()
                        Text(result.summary, style = MaterialTheme.typography.bodySmall)
                    }
                    if (result.recommendation.isNotBlank()) {
                        Text(result.recommendation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    HorizontalDivider()
                    OutlinedTextField(
                        value = saveNotes,
                        onValueChange = { saveNotes = it },
                        label = { Text("Заметки (необязательно)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveResult(result, LocalDate.now(), savedPhotoPath, saveNotes)
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false; viewModel.reset() }) { Text("Отмена") }
            }
        )
    }

    // Manual add dialog
    if (showManualDialog) {
        ManualTestDialog(
            onDismiss = { showManualDialog = false },
            onSave = { testType, date, positive, borderline, lab, notes ->
                viewModel.saveManual(testType, date, positive, borderline, lab, notes)
                showManualDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Тесты на аллергены") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад") } },
                actions = {
                    IconButton(onClick = { showManualDialog = true }) {
                        Icon(Icons.Default.Add, "Добавить вручную")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
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
            // Description card
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text("🔬 Анализ тестов на аллергены", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Сфотографируйте бланк результатов теста. ИИ распознает аллергены и их результаты. Данные учитываются при AI-анализе дневника.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { launchCamera() },
                    modifier = Modifier.weight(1f),
                    enabled = analyzeState !is AllergyTestUiState.Analyzing
                ) {
                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Камера")
                }
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    enabled = analyzeState !is AllergyTestUiState.Analyzing
                ) {
                    Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Галерея")
                }
            }

            // Loading state
            if (analyzeState is AllergyTestUiState.Analyzing) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text("Gemini анализирует тест...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Error state
            if (analyzeState is AllergyTestUiState.Error) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                        Text(
                            (analyzeState as AllergyTestUiState.Error).message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = viewModel::reset) { Text("OK") }
                    }
                }
            }

            // Saved test results list
            if (testResults.isNotEmpty()) {
                Text(
                    "Сохранённые тесты",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                testResults.forEach { result ->
                    AllergyTestResultCard(
                        result = result,
                        dateFormatter = dateFormatter,
                        onDelete = { viewModel.deleteResult(result) }
                    )
                }
            } else if (analyzeState is AllergyTestUiState.Idle) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📋", style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("Тестов пока нет", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Сфотографируйте результат теста или добавьте вручную",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AllergyTestResultCard(
    result: AllergyTestResult,
    dateFormatter: DateTimeFormatter,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        result.testType.ifBlank { "Тест на аллергены" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        result.testDate.format(dateFormatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (result.labName.isNotBlank())
                        Text(result.labName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "Удалить", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
            if (result.positiveAllergens.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("✅ Положительные: ${result.positiveAllergens}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            if (result.borderlineAllergens.isNotBlank()) {
                Text("⚠️ Пограничные: ${result.borderlineAllergens}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFA726))
            }
            if (result.summary.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(result.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            if (result.notes.isNotBlank()) {
                Text("📝 ${result.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualTestDialog(
    onDismiss: () -> Unit,
    onSave: (String, LocalDate, String, String, String, String) -> Unit
) {
    var testType by remember { mutableStateOf("") }
    var positiveAllergens by remember { mutableStateOf("") }
    var borderlineAllergens by remember { mutableStateOf("") }
    var labName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить тест вручную") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = testType,
                    onValueChange = { testType = it },
                    label = { Text("Тип теста") },
                    placeholder = { Text("кожный прик-тест, IgE, патч-тест") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = positiveAllergens,
                    onValueChange = { positiveAllergens = it },
                    label = { Text("Положительные аллергены") },
                    placeholder = { Text("через запятую: пшеница, молоко") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = borderlineAllergens,
                    onValueChange = { borderlineAllergens = it },
                    label = { Text("Пограничные (необязательно)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = labName,
                    onValueChange = { labName = it },
                    label = { Text("Лаборатория (необязательно)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Заметки") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(testType, LocalDate.now(), positiveAllergens, borderlineAllergens, labName, notes) },
                enabled = testType.isNotBlank() || positiveAllergens.isNotBlank()
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
