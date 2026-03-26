package com.allergia.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.allergia.data.models.AllergenicIngredient
import com.allergia.data.models.LabelRecognitionResult
import com.allergia.data.models.ProductCategory

/** Диалог "Сканирую состав..." */
@Composable
fun LabelAnalyzingDialog() {
    Dialog(onDismissRequest = {}) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Text("Читаю состав...", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "Gemma 3 распознаёт ингредиенты с этикетки и выявляет аллергены",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/** Полноэкранный диалог с результатами анализа состава */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelResultDialog(
    result: LabelRecognitionResult,
    photoUri: Uri,
    onConfirm: (name: String, brand: String, category: ProductCategory) -> Unit,
    onDismiss: () -> Unit
) {
    var productName by remember { mutableStateOf(result.productName) }
    var brand by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ProductCategory.OTHER) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var showIngredients by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.95f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Header ─────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Science, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Состав распознан", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Gemma 3 4B · ${result.ingredients.size} ингредиентов", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                        }
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Color.White) }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Photo preview
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Этикетка продукта",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(130.dp)
                    )

                    // Allergenicity score
                    val scoreColor = when {
                        result.allergenicityScore >= 0.75f -> Color(0xFFE53935)
                        result.allergenicityScore >= 0.5f -> Color(0xFFFFA726)
                        result.allergenicityScore >= 0.25f -> Color(0xFFFFEE58)
                        else -> Color(0xFF66BB6A)
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = scoreColor.copy(alpha = 0.12f))) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Аллергенность состава", style = MaterialTheme.typography.labelMedium)
                                Text(result.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${(result.allergenicityScore * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = scoreColor)
                                Text(result.allergenicityLabel, style = MaterialTheme.typography.labelSmall, color = scoreColor)
                            }
                        }
                    }

                    // Allergenic ingredients highlight
                    if (result.allergenicIngredients.isNotEmpty()) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "⚠️ Потенциальные аллергены (${result.allergenicIngredients.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFB71C1C)
                                )
                                result.allergenicIngredients.forEach { allergen ->
                                    AllergenRow(allergen)
                                }
                            }
                        }
                    } else {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32))
                                Spacer(Modifier.width(8.dp))
                                Text("Известных аллергенов не обнаружено", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF2E7D32))
                            }
                        }
                    }

                    // Full ingredients list (collapsible)
                    if (result.ingredients.isNotEmpty()) {
                        OutlinedCard {
                            Column(Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Полный состав", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { showIngredients = !showIngredients }, modifier = Modifier.size(32.dp)) {
                                        Icon(if (showIngredients) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                                    }
                                }
                                AnimatedVisibility(visible = showIngredients) {
                                    Text(
                                        result.ingredients.joinToString(" • "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // Product info form
                    Text("Сохранить продукт в дневник", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                    OutlinedTextField(
                        value = productName,
                        onValueChange = { productName = it },
                        label = { Text("Название продукта *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Бренд / Производитель") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Category picker
                    ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                        OutlinedTextField(
                            value = "${selectedCategory.emoji} ${selectedCategory.displayName}",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Категория") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                            // Show grouped categories
                            listOf(
                                "Косметика и уход" to ProductCategory.COSMETICS,
                                "Бытовая химия" to ProductCategory.HOUSEHOLD,
                                "Гигиена полости рта" to ProductCategory.ORAL,
                                "Другое" to listOf(ProductCategory.OTHER)
                            ).forEach { (groupName, cats) ->
                                DropdownMenuItem(text = { Text(groupName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }, onClick = {}, enabled = false)
                                cats.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text("  ${cat.emoji} ${cat.displayName}") },
                                        onClick = { selectedCategory = cat; categoryExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Bottom actions ──────────────────────────────────────────
                HorizontalDivider()
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Отмена") }
                    Button(
                        onClick = { if (productName.isNotBlank()) onConfirm(productName, brand, selectedCategory) },
                        enabled = productName.isNotBlank(),
                        modifier = Modifier.weight(2f)
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}

@Composable
private fun AllergenRow(allergen: AllergenicIngredient) {
    val riskColor = when (allergen.riskLevel) {
        "high" -> Color(0xFFE53935)
        "medium" -> Color(0xFFFFA726)
        else -> Color(0xFFFFC107)
    }
    val riskLabel = when (allergen.riskLevel) {
        "high" -> "Высокий риск"
        "medium" -> "Средний риск"
        else -> "Низкий риск"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = riskColor.copy(alpha = 0.15f),
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Text(
                riskLabel,
                style = MaterialTheme.typography.labelSmall,
                color = riskColor,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(allergen.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                if (allergen.commonName.isNotBlank()) {
                    Spacer(Modifier.width(4.dp))
                    Text("(${allergen.commonName})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
            if (allergen.reason.isNotBlank()) {
                Text(allergen.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
            }
        }
    }
}

/** Bottom sheet выбора источника фото для этикетки */
@Composable
fun LabelPhotoSourceSheet(
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Сфотографировать состав", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Наведите камеру на список ингредиентов — Gemma 3 прочитает состав и выделит аллергены",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ElevatedButton(onClick = { onCamera(); onDismiss() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CameraAlt, null); Spacer(Modifier.width(6.dp)); Text("Камера")
                }
                ElevatedButton(onClick = { onGallery(); onDismiss() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.PhotoLibrary, null); Spacer(Modifier.width(6.dp)); Text("Галерея")
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Отмена") }
        }
    }
}
