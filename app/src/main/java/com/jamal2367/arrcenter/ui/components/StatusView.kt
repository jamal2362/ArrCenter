package com.jamal2367.arrcenter.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Centred "something is not right" panel: icon, headline, explanation and up to two actions.
 *
 * Replaces the previous copy-pasted error column and works on any background because the
 * caller passes the text colour that fits the surface behind it.
 */
@Composable
fun StatusView(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    detail: String? = null,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    primaryAction: StatusAction? = null,
    secondaryAction: StatusAction? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor.copy(alpha = 0.9f),
            modifier = Modifier.size(72.dp),
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = contentColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 420.dp),
        )

        if (message != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 420.dp),
            )
        }

        if (!detail.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 420.dp),
            )
        }

        if (primaryAction != null) {
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = primaryAction.onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 320.dp),
            ) {
                Text(primaryAction.label)
            }
        }

        if (secondaryAction != null) {
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = secondaryAction.onClick) {
                Text(secondaryAction.label, color = contentColor)
            }
        }
    }
}

data class StatusAction(val label: String, val onClick: () -> Unit)
