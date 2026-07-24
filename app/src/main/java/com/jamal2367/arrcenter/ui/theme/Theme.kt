package com.jamal2367.arrcenter.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.jamal2367.arrcenter.data.ThemeMode

/**
 * Material 3 theme driven by the stored [ThemeMode] instead of the system setting alone,
 * so the app can be forced to light or dark independently of the device.
 *
 * `minSdk` is 31, so dynamic colour is always available and no static fallback palette is
 * needed.
 */
@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val darkTheme = themeMode.isDark()
    val context = LocalContext.current
    val colorScheme = if (darkTheme) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

/**
 * Switches the status and navigation bar icons between light and dark.
 *
 * This is not derived from the theme alone: the service screens paint a dark background
 * behind both system bars, so their icons must stay light even when the app itself runs in
 * the light theme. Only the settings screen follows the theme.
 *
 * @param lightIcons `true` for light (white) icons on a dark background.
 */
@Composable
fun SystemBarIcons(lightIcons: Boolean) {
    val view = LocalView.current
    if (LocalInspectionMode.current) return

    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !lightIcons
            isAppearanceLightNavigationBars = !lightIcons
        }
    }
}

/** Whether [ThemeMode] resolves to a dark appearance right now. */
@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}
