package com.jamal2367.arrcenter.data

import com.jamal2367.arrcenter.model.ServiceType
import org.json.JSONObject

/**
 * Backup format of the settings.
 *
 * Written with `org.json`, which ships with Android - a backup of a handful of URLs does not
 * justify pulling in a serialization library.
 *
 * The file contains addresses only. Sessions and logins live in the WebView's cookie store
 * and are deliberately not part of it.
 */
private const val FORMAT_VERSION = 1

private const val KEY_VERSION = "version"
private const val KEY_THEME = "themeMode"
private const val KEY_START = "startService"
private const val KEY_SERVICES = "services"
private const val KEY_PRIMARY = "primary"
private const val KEY_SECONDARY = "secondary"

/** Default file name offered by the export dialog. */
const val BACKUP_FILE_NAME = "arrcenter-settings.json"

fun AppSettings.toBackupJson(): String {
    val services = JSONObject()
    ServiceType.entries.forEach { type ->
        val endpoints = endpointsFor(type)
        services.put(
            type.id,
            JSONObject()
                .put(KEY_PRIMARY, endpoints.primary)
                .put(KEY_SECONDARY, endpoints.secondary),
        )
    }

    return JSONObject()
        .put(KEY_VERSION, FORMAT_VERSION)
        .put(KEY_THEME, themeMode.id)
        .put(KEY_START, startService.id)
        .put(KEY_SERVICES, services)
        .toString(2)
}

/**
 * Reads a backup file.
 *
 * @return `null` when the file is not valid JSON or carries no service block - importing a
 *         random file must not silently wipe the configuration.
 */
fun parseBackupJson(raw: String): AppSettings? = try {
    val root = JSONObject(raw)
    val services = root.optJSONObject(KEY_SERVICES)

    if (services == null) {
        null
    } else {
        AppSettings(
            endpoints = ServiceType.entries.associateWith { type ->
                val entry = services.optJSONObject(type.id)
                ServiceEndpoints(
                    primary = entry?.optString(KEY_PRIMARY).orEmpty(),
                    secondary = entry?.optString(KEY_SECONDARY).orEmpty(),
                )
            },
            themeMode = ThemeMode.fromId(root.optString(KEY_THEME)),
            startService = ServiceType.fromId(root.optString(KEY_START)) ?: ServiceType.Seerr,
        )
    }
} catch (_: Exception) {
    null
}
