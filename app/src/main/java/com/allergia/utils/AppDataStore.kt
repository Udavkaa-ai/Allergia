package com.allergia.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/**
 * Single application-wide DataStore instance.
 * Import this extension in all classes that need to read preferences.
 * Having a single delegate prevents multiple DataStore instances pointing
 * to the same file, which is explicitly discouraged by Jetpack docs.
 */
val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Centralised preference key definitions — change key names here only. */
object PreferenceKeys {
    val OPENROUTER_API_KEY = stringPreferencesKey("openrouter_api_key")
    val USER_NAME          = stringPreferencesKey("user_name")
}
