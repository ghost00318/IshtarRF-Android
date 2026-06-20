package com.ishtarrf.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ishtarrf.domain.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ishtar_settings")

/** Persists user preferences (theme + favorite frequencies) via DataStore. */
class SettingsRepository(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme")
    private val favoritesKey = stringPreferencesKey("fav_freqs")

    val theme: Flow<AppTheme> = context.dataStore.data.map { AppTheme.fromKey(it[themeKey]) }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { it[themeKey] = theme.key }
    }

    val favorites: Flow<List<Double>> = context.dataStore.data.map { prefs ->
        prefs[favoritesKey]?.let { decode(it) } ?: DEFAULT_FAVORITES
    }

    suspend fun setFavorites(list: List<Double>) {
        context.dataStore.edit { it[favoritesKey] = list.joinToString(",") }
    }

    private fun decode(raw: String): List<Double> =
        raw.split(",").mapNotNull { it.trim().toDoubleOrNull() }.distinct().sorted()

    companion object {
        val DEFAULT_FAVORITES = listOf(300.0, 315.0, 390.0, 433.92, 868.0, 915.0)
    }
}
