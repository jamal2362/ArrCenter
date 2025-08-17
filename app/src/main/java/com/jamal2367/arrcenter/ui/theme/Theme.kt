package com.jamal2367.arrcenter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val colorScheme = dynamicDarkColorScheme(LocalContext.current)

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
