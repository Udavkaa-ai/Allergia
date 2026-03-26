package com.allergia.ui.components

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.allergia.api.DetectedFoodItem
import com.allergia.data.models.MealType

/**
 * Диалог показывается пока Gemma 3 4B анализирует фото.
 */
@Composable
fun PhotoAnalyzingDialog() {
    Dialog(onDismissRequest = {}) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Text(
                    "Gemma 3 распознаёт блюда...",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Анализируем содержимое фото",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Диалог с результатами распознавания — пользователь выбирает / редактирует позиции перед добавлением.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodRecognitionResultDialog(
    photoUri: Uri?,
    detectedItems: List<DetectedFoodItem>,
    onConfirm: (List<DetectedFoodItem>) -> Unit,
    onDismiss: () -> Unit
) {
    // Локальная копия для редактирования
    val items = remember(detectedItems) {
        mutableStateListOf(*detectedItems.map { it.copy() }.toTypedArray())
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhotoCamera, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Распознано продуктов: ${items.size}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Gemma 3 4B · Снимите галочку чтобы исключить",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                    }
                }

                // Photo preview (small)
                if (photoUri != null) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Фото еды",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                }

                // Item list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(items) { index, item ->
                        DetectedItemRow(
                            item = item,
                            onToggle = { selected ->
                                items[index] = items[index].copy(isSelected = selected)
                            },
                            onNameChange = { newName ->
                                items[index] = items[index].copy(name = newName)
                            },
                            onAmountChange = { newAmount ->
                                items[index] = items[index].copy(estimatedAmount = newAmount)
                            },
                            onMealTypeChange = { newType ->
                                items[index] = items[index].copy(mealType = newType)
                            }
                        )
                    }
                }

                // Bottom actions
                HorizontalDivider()
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val selectedCount = items.count { it.isSelected }
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Отмена")
                    }
                    Button(
                        onClick = { onConfirm(items) },
                        enabled = selectedCount > 0,
                        modifier = Modifier.weight(2f)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Добавить ($selectedCount)")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetectedItemRow(
    item: DetectedFoodItem,
    onToggle: (Boolean) -> Unit,
    onNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onMealTypeChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var mealTypeExpanded by remember { mutableStateOf(false) }

    val alpha = if (item.isSelected) 1f else 0.4f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (item.isSelected) 1.5.dp else 0.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.medium
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = item.isSelected,
                    onCheckedChange = onToggle
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { if (item.isSelected) expanded = !expanded }
                ) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.estimatedAmount.isNotBlank()) {
                            Text(
                                item.estimatedAmount,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f * alpha)
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        val mealType = runCatching { MealType.valueOf(item.mealType) }.getOrDefault(MealType.OTHER)
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = alpha),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                mealType.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (item.isSelected) {
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.Edit,
                            null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Expanded edit fields
            AnimatedVisibility(visible = expanded && item.isSelected) {
                Column(
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = item.name,
                        onValueChange = onNameChange,
                        label = { Text("Название") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = item.estimatedAmount,
                        onValueChange = onAmountChange,
                        label = { Text("Количество") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ExposedDropdownMenuBox(
                        expanded = mealTypeExpanded,
                        onExpandedChange = { mealTypeExpanded = it }
                    ) {
                        val currentType = runCatching { MealType.valueOf(item.mealType) }.getOrDefault(MealType.OTHER)
                        OutlinedTextField(
                            value = currentType.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Приём пищи") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(mealTypeExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = mealTypeExpanded,
                            onDismissRequest = { mealTypeExpanded = false }
                        ) {
                            MealType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.displayName) },
                                    onClick = {
                                        onMealTypeChange(type.name)
                                        mealTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bottom sheet / кнопка для выбора источника фото (камера или галерея).
 */
@Composable
fun PhotoSourceSheet(
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Добавить фото еды",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Gemma 3 4B автоматически распознает блюда и продукты",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ElevatedButton(
                    onClick = { onCamera(); onDismiss() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Камера")
                }
                ElevatedButton(
                    onClick = { onGallery(); onDismiss() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoLibrary, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Галерея")
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Отмена")
            }
        }
    }
}
