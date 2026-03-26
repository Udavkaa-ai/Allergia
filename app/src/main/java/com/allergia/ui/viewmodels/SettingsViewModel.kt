package com.allergia.ui.viewmodels

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        val API_KEY = stringPreferencesKey("openrouter_api_key")
        val USER_NAME = stringPreferencesKey("user_name")
    }

    val apiKey: StateFlow<String> = context.dataStore.data
        .map { prefs -> prefs[API_KEY] ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val userName: StateFlow<String> = context.dataStore.data
        .map { prefs -> prefs[USER_NAME] ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            context.dataStore.edit { it[API_KEY] = key.trim() }
        }
    }

    fun saveUserName(name: String) {
        viewModelScope.launch {
            context.dataStore.edit { it[USER_NAME] = name.trim() }
        }
    }
}
