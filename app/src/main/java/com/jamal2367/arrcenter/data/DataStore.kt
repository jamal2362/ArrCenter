package com.jamal2367.arrcenter.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore by preferencesDataStore("settings")

object SettingsKeys {
    val JELLY_PRIMARY: Preferences.Key<String> = stringPreferencesKey("jelly_primary")
    val JELLY_SECONDARY: Preferences.Key<String> = stringPreferencesKey("jelly_secondary")
    val RADARR_PRIMARY: Preferences.Key<String> = stringPreferencesKey("radarr_primary")
    val RADARR_SECONDARY: Preferences.Key<String> = stringPreferencesKey("radarr_secondary")
    val SONARR_PRIMARY: Preferences.Key<String> = stringPreferencesKey("sonarr_primary")
    val SONARR_SECONDARY: Preferences.Key<String> = stringPreferencesKey("sonarr_secondary")
    val SABNZBD_PRIMARY: Preferences.Key<String> = stringPreferencesKey("sabnzbd_primary")
    val SABNZBD_SECONDARY: Preferences.Key<String> = stringPreferencesKey("sabnzbd_secondary")
}
