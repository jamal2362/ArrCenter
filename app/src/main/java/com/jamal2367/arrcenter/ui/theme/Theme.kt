package com.jamal2367.arrcenter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = dynamicDarkColorScheme(context)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
