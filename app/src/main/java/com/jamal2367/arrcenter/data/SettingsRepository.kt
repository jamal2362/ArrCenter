package com.jamal2367.arrcenter.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jamal2367.arrcenter.model.ServiceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore("settings")

/**
 * Typed access to the persisted settings.
 *
 * The preference keys are derived from [ServiceType.id] (`seerr_primary`, `radarr_primary`, …)
 * which is exactly what previous versions of the app wrote, so upgrading keeps every
 * configured URL.
 */
class SettingsRepository(context: Context) {

    private val store = context.applicationContext.dataStore

    val settings: Flow<AppSettings> = store.data
        .catch { cause ->
            // A corrupted preferences file must not take the whole app down.
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }
        .map { prefs ->
            AppSettings(
                endpoints = ServiceType.entries.associateWith { type ->
                    ServiceEndpoints(
                        primary = prefs[primaryKey(type)].orEmpty(),
                        secondary = prefs[secondaryKey(type)].orEmpty(),
                    )
                },
                themeMode = ThemeMode.fromId(prefs[THEME_MODE]),
                startService = ServiceType.fromId(prefs[START_SERVICE]) ?: ServiceType.Seerr,
            )
        }

    suspend fun saveEndpoints(endpoints: Map<ServiceType, ServiceEndpoints>) {
        store.edit { prefs ->
            endpoints.forEach { (type, value) ->
                prefs[primaryKey(type)] = value.primary.trim()
                prefs[secondaryKey(type)] = value.secondary.trim()
            }
        }
    }

    suspend fun saveThemeMode(mode: ThemeMode) {
        store.edit { it[THEME_MODE] = mode.id }
    }

    suspend fun saveStartService(type: ServiceType) {
        store.edit { it[START_SERVICE] = type.id }
    }

    private companion object {
        val THEME_MODE: Preferences.Key<String> = stringPreferencesKey("theme_mode")
        val START_SERVICE: Preferences.Key<String> = stringPreferencesKey("start_service")

        fun primaryKey(type: ServiceType): Preferences.Key<String> =
            stringPreferencesKey("${type.id}_primary")

        fun secondaryKey(type: ServiceType): Preferences.Key<String> =
            stringPreferencesKey("${type.id}_secondary")
    }
}
