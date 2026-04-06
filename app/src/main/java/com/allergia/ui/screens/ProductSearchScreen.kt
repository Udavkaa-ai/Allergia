package com.allergia.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.allergia.api.ProductAllergenicityResponse
import com.allergia.data.models.HouseholdProduct
import com.allergia.ui.components.AllergenicityBadge
import com.allergia.ui.components.LabelAnalyzingDialog
import com.allergia.ui.components.LabelResultDialog
import com.allergia.ui.viewmodels.AllergenicityUiState
import com.allergia.ui.viewmodels.DiaryViewModel
import com.allergia.ui.viewmodels.LabelAnalysisState
import com.allergia.utils.ImageUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductSearchScreen(
    onBack: () -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.allergenicityState.collectAsState()
    val labelState by viewModel.labelAnalysisState.collectAsState()
    val allProducts by viewModel.allProductsWithScores.collectAsState()
    var query by remember { mutableStateOf("") }
    var labelPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showSortedList by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) labelPhotoUri?.let { viewModel.analyzeLabelPhoto(it) }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { labelPhotoUri = it; viewModel.analyzeLabelPhoto(it) }
    }

    var showPhotoSheet by remember { mutableStateOf(false) }

    fun launchCamera() {
        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            val file = ImageUtils.createTempPhotoFile(context)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            labelPhotoUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
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

    // Photo source sheet
    if (showPhotoSheet) {
        ModalBottomSheet(onDismissRequest = { showPhotoSheet = false }) {
            Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
                Text("Сканировать этикетку продукта", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ElevatedButton(
                        onClick = { showPhotoSheet = false; launchCamera() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📷", style = MaterialTheme.typography.headlineMedium)
                            Text("Камера", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    ElevatedButton(
                        onClick = { showPhotoSheet = false; galleryLauncher.launch("image/*") },
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Оценка аллергенности") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Назад") } },
                actions = {
                    IconButton(onClick = { showPhotoSheet = true }) {
                        Icon(Icons.Default.DocumentScanner, "Сканировать этикетку")
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
            Text(
                "Введите название продукта или отсканируйте этикетку 📷 — Gemini 2.5 оценит аллергенность.",
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
                        IconButton(onClick = { viewModel.checkProductAllergenicity(query) }) {
                            Icon(Icons.Default.Search, "Проверить")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.checkProductAllergenicity(query) },
                    enabled = query.isNotBlank() && state !is AllergenicityUiState.Loading,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Science, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Оценить")
                }
                OutlinedButton(
                    onClick = { showPhotoSheet = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("По фото")
                }
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

            // Scanned products list section
            if (allProducts.isNotEmpty()) {
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "📋 Проверенные продукты",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showSortedList = !showSortedList }) {
                        Text(if (showSortedList) "Скрыть" else "Показать (${allProducts.size})")
                    }
                }

                if (showSortedList) {
                    Text(
                        "Отсортированы от наименее аллергенного",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(4.dp))
                    allProducts.forEach { product ->
                        ScannedProductRow(
                            product = product,
                            onAddToDiary = {
                                viewModel.addHouseholdProductManualPersistent(
                                    product.name, product.brand, product.category, false
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannedProductRow(
    product: HouseholdProduct,
    onAddToDiary: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                if (product.brand.isNotBlank())
                    Text(product.brand, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                if (product.allergenicIngredients.isNotBlank())
                    Text("⚠ ${product.allergenicIngredients}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE53935))
            }
            product.allergenicityScore?.let { AllergenicityBadge(it, Modifier.padding(horizontal = 4.dp)) }
            IconButton(onClick = onAddToDiary, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, "Добавить в дневник", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
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

        if (result.allergens.isNotEmpty()) InfoChipsCard("⚠️ Содержит аллергены", result.allergens, Color(0xFFE53935))
        if (result.crossReactiveWith.isNotEmpty()) InfoChipsCard("🔗 Перекрёстная реактивность", result.crossReactiveWith, Color(0xFFFFA726))
        if (result.commonReactions.isNotEmpty()) InfoChipsCard("🤧 Типичные реакции", result.commonReactions, Color(0xFF9C27B0))
        if (result.safeAlternatives.isNotEmpty()) InfoChipsCard("✅ Безопасные альтернативы", result.safeAlternatives, Color(0xFF4CAF50))
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
