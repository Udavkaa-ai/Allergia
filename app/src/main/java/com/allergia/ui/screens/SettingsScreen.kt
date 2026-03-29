package com.allergia.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.allergia.data.models.ProfileItem
import com.allergia.data.models.ProfileItemType
import com.allergia.ui.viewmodels.BackupStatus
import com.allergia.ui.viewmodels.SettingsViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSection(
    items: List<ProfileItem>,
    onAdd: (name: String, type: ProfileItemType, confirmed: Boolean, notes: String) -> Unit,
    onDelete: (ProfileItem) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Аллергологический профиль", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${items.size} записей · используется в AI-анализе",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, "Добавить")
                }
            }

            val safeItems = items.filter { it.type == ProfileItemType.CONFIRMED_SAFE }
            val allergenItems = items.filter { it.type == ProfileItemType.KNOWN_ALLERGEN }

            if (items.isEmpty()) {
                Text(
                    "Добавьте данные анализов: на что точно нет аллергии и известные аллергены. AI будет учитывать это при анализе.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }

            if (safeItems.isNotEmpty()) {
                Text(
                    "✅ Точно нет аллергии",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                safeItems.forEach { item ->
                    ProfileItemRow(item = item, onDelete = { onDelete(item) })
                }
            }

            if (allergenItems.isNotEmpty()) {
                Text(
                    "⚠️ Известные аллергены",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
                allergenItems.forEach { item ->
                    ProfileItemRow(item = item, onDelete = { onDelete(item) })
                }
            }
        }
    }

    if (showAddDialog) {
        AddProfileItemDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type, confirmed, notes ->
                onAdd(name, type, confirmed, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ProfileItemRow(item: ProfileItem, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyMedium)
            if (item.notes.isNotBlank()) {
                Text(
                    item.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            if (item.confirmedByTest) {
                Text(
                    "подтверждено анализами",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddProfileItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: ProfileItemType, confirmed: Boolean, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ProfileItemType.CONFIRMED_SAFE) }
    var confirmedByTest by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить запись в профиль") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название (пыль, лактоза, глютен...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Тип:", style = MaterialTheme.typography.labelMedium)
                ProfileItemType.entries.forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type }
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("${type.emoji} ${type.displayName}", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = confirmedByTest,
                        onCheckedChange = { confirmedByTest = it }
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Подтверждено анализами", style = MaterialTheme.typography.bodyMedium)
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Примечание (необязательно)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), selectedType, confirmedByTest, notes.trim()) },
                enabled = name.isNotBlank()
            ) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val apiKey by viewModel.apiKey.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val backupStatus by viewModel.backupStatus.collectAsState()
    val profileItems by viewModel.profileItems.collectAsState()

    var apiKeyInput by remember(apiKey) { mutableStateOf(apiKey) }
    var userNameInput by remember(userName) { mutableStateOf(userName) }
    var showApiKey by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // Export: user picks where to save the file
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportBackup(it) }
    }

    // Import: user picks an existing backup file
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            showRestoreConfirm = true
        }
    }

    // Confirm restore dialog (replaces all data)
    if (showRestoreConfirm && pendingRestoreUri != null) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false; pendingRestoreUri = null },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Восстановить данные?") },
            text = { Text("Все текущие данные дневника будут ЗАМЕНЕНЫ данными из файла бекапа. Это действие необратимо.") },
            confirmButton = {
                Button(
                    onClick = {
                        pendingRestoreUri?.let { viewModel.importBackup(it) }
                        showRestoreConfirm = false
                        pendingRestoreUri = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Восстановить") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false; pendingRestoreUri = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
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
            // ── Profile ──────────────────────────────────────────────────────
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Профиль", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = userNameInput,
                        onValueChange = { userNameInput = it },
                        label = { Text("Ваше имя") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── API Key ──────────────────────────────────────────────────────
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("OpenRouter API (Gemini 2.5)", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Получите API ключ на openrouter.ai. Ключ хранится только на вашем устройстве.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it; saved = false },
                        label = { Text("API Key") },
                        singleLine = true,
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        leadingIcon = { Icon(Icons.Default.Key, null) },
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (apiKeyInput.isNotBlank()) {
                        Text(
                            "Модель: google/gemini-2.5-flash-preview",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Save settings button
            Button(
                onClick = {
                    viewModel.saveApiKey(apiKeyInput)
                    viewModel.saveUserName(userNameInput)
                    saved = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Сохранить настройки")
            }

            if (saved) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(Modifier.padding(12.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Настройки сохранены", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // ── Backup / Restore ─────────────────────────────────────────────
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Бекап и восстановление", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Экспорт сохраняет весь дневник в JSON-файл. Импорт полностью заменяет текущие данные.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Export button
                        OutlinedButton(
                            onClick = {
                                viewModel.clearBackupStatus()
                                val fileName = "allergia_backup_${LocalDate.now()}.json"
                                exportLauncher.launch(fileName)
                            },
                            modifier = Modifier.weight(1f),
                            enabled = backupStatus !is BackupStatus.InProgress
                        ) {
                            Icon(Icons.Default.Upload, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Экспорт")
                        }

                        // Import button
                        Button(
                            onClick = {
                                viewModel.clearBackupStatus()
                                importLauncher.launch(arrayOf("application/json", "text/*"))
                            },
                            modifier = Modifier.weight(1f),
                            enabled = backupStatus !is BackupStatus.InProgress
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Импорт")
                        }
                    }

                    // Status
                    when (val s = backupStatus) {
                        is BackupStatus.InProgress -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Обработка...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        is BackupStatus.Success -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(s.message, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        is BackupStatus.Error -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(s.message, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error)
                            }
                        }
                        else -> {}
                    }
                }
            }

            // ── Allergy Profile ──────────────────────────────────────────────
            ProfileSection(
                items = profileItems,
                onAdd = { name, type, confirmed, notes ->
                    viewModel.addProfileItem(name, type, confirmed, notes)
                },
                onDelete = viewModel::deleteProfileItem
            )

            // ── About ────────────────────────────────────────────────────────
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("О приложении", style = MaterialTheme.typography.titleSmall)
                    Text("Allergia v1.0", style = MaterialTheme.typography.bodySmall)
                    Text("AI: Gemini 2.5 Flash (OpenRouter)", style = MaterialTheme.typography.bodySmall)
                    Text("База данных: Room (локальная)", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Приложение не является медицинским устройством. Все AI-рекомендации носят информационный характер.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
