package com.allergia.ui.viewmodels

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allergia.utils.PreferenceKeys
import com.allergia.utils.appDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    val apiKey: StateFlow<String> = context.appDataStore.data
        .map { prefs -> prefs[PreferenceKeys.OPENROUTER_API_KEY] ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val userName: StateFlow<String> = context.appDataStore.data
        .map { prefs -> prefs[PreferenceKeys.USER_NAME] ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

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
}
