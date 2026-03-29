package com.allergia.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allergia.data.models.ProfileItem
import com.allergia.data.models.ProfileItemType
import com.allergia.data.repository.DiaryRepository
import com.allergia.utils.BackupManager
import com.allergia.utils.PreferenceKeys
import com.allergia.utils.appDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DiaryRepository
) : ViewModel() {

    val apiKey: StateFlow<String> = context.appDataStore.data
        .map { prefs -> prefs[PreferenceKeys.OPENROUTER_API_KEY] ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val userName: StateFlow<String> = context.appDataStore.data
        .map { prefs -> prefs[PreferenceKeys.USER_NAME] ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val profileItems: StateFlow<List<ProfileItem>> = repository.getAllProfileItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _backupStatus = MutableStateFlow<BackupStatus>(BackupStatus.Idle)
    val backupStatus: StateFlow<BackupStatus> = _backupStatus.asStateFlow()

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            context.appDataStore.edit { it[PreferenceKeys.OPENROUTER_API_KEY] = key.trim() }
        }
    }

    fun saveUserName(name: String) {
        viewModelScope.launch {
            context.appDataStore.edit { it[PreferenceKeys.USER_NAME] = name.trim() }
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _backupStatus.value = BackupStatus.InProgress
            runCatching {
                val data = repository.getFullBackup()
                val json = BackupManager.toJson(data)
                BackupManager.writeToUri(context, uri, json)
                "Бекап сохранён: ${data.foodItems.size} блюд, " +
                    "${data.skinConditions.size} записей кожи, " +
                    "${data.medications.size} лекарств"
            }.onSuccess { msg ->
                _backupStatus.value = BackupStatus.Success(msg)
            }.onFailure { e ->
                _backupStatus.value = BackupStatus.Error("Ошибка экспорта: ${e.message}")
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _backupStatus.value = BackupStatus.InProgress
            runCatching {
                val json = BackupManager.readFromUri(context, uri)
                val data = BackupManager.fromJson(json)
                repository.restoreFromBackup(data)
                "Восстановлено: ${data.foodItems.size} блюд, " +
                    "${data.skinConditions.size} записей кожи, " +
                    "${data.medications.size} лекарств"
            }.onSuccess { msg ->
                _backupStatus.value = BackupStatus.Success(msg)
            }.onFailure { e ->
                _backupStatus.value = BackupStatus.Error("Ошибка импорта: ${e.message}")
            }
        }
    }

    fun clearBackupStatus() {
        _backupStatus.value = BackupStatus.Idle
    }

    fun addProfileItem(name: String, type: ProfileItemType, confirmedByTest: Boolean, notes: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertProfileItem(
                ProfileItem(name = name.trim(), type = type, confirmedByTest = confirmedByTest, notes = notes.trim())
            )
        }
    }

    fun deleteProfileItem(item: ProfileItem) {
        viewModelScope.launch { repository.deleteProfileItem(item) }
    }
}

sealed class BackupStatus {
    object Idle : BackupStatus()
    object InProgress : BackupStatus()
    data class Success(val message: String) : BackupStatus()
    data class Error(val message: String) : BackupStatus()
}
