package com.jamal2367.arrcenter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jamal2367.arrcenter.R
import com.jamal2367.arrcenter.model.ServiceType

/** One slot in the navigation, in the order it is shown. */
private sealed interface BarEntry {
    data class Service(val type: ServiceType) : BarEntry
    data object Reload : BarEntry
    data object Settings : BarEntry
}

private val BarEntries: List<BarEntry> = listOf(
    BarEntry.Service(ServiceType.Seerr),
    BarEntry.Service(ServiceType.Radarr),
    BarEntry.Service(ServiceType.Sonarr),
    BarEntry.Reload,
    BarEntry.Service(ServiceType.SABnzbd),
    BarEntry.Service(ServiceType.Uvs),
    BarEntry.Settings,
)

private val BarHeight = 56.dp
private val IndicatorWidth = 48.dp
private val IndicatorHeight = 32.dp

// The bar sits on the service background, which is always dark, so the content colours are
// derived from white rather than from the Material colour scheme.
//
// Every icon uses the same full white: dimming the inactive ones made them composite to a
// grey that read as "disabled", and a separate shade for the actions made the settings icon
// look like it belonged to something else. The selected item is marked by the pill alone.
private val BarContent = Color.White
private val SelectedIndicator = Color.White.copy(alpha = 0.18f)

/**
 * Bottom navigation - the five services plus reload and settings.
 *
 * Every entry is always visible and shares the width equally, on any screen size and in any
 * orientation. Reload and settings live here instead of in a top app bar, so the web content
 * gets the full height of the screen.
 *
 * The bar takes the background colour of the service that is currently open so it merges
 * with the web content instead of sitting on a differently coloured strip. Icons only - the
 * label is exposed to accessibility services rather than drawn.
 */
@Composable
fun ServiceBottomBar(
    current: ServiceType,
    onSelect: (ServiceType) -> Unit,
    onReload: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val groupLabel = stringResource(R.string.navigation)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = groupLabel },
        color = current.chromeColor,
        // contentColorFor() cannot resolve a colour outside the scheme and would hand down
        // Color.Unspecified, which leaves icons untinted.
        contentColor = BarContent,
        // No tonal elevation: it would tint the explicit colour we just set.
        tonalElevation = 0.dp,
    ) {
        Box(Modifier.windowInsetsPadding(NavigationBarDefaults.windowInsets)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BarHeight)
                    .selectableGroup(),
            ) {
                BarEntries.forEach { entry ->
                    when (entry) {
                        is BarEntry.Service -> BarItem(
                            label = stringResource(entry.type.labelRes),
                            selected = entry.type == current,
                            role = Role.Tab,
                            onClick = { onSelect(entry.type) },
                        ) {
                            Icon(painterResource(entry.type.iconRes), contentDescription = null)
                        }

                        BarEntry.Reload -> BarItem(
                            label = stringResource(R.string.reload_short),
                            selected = false,
                            role = Role.Button,
                            onClick = onReload,
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                        }

                        BarEntry.Settings -> BarItem(
                            label = stringResource(R.string.settings),
                            selected = false,
                            role = Role.Button,
                            onClick = onOpenSettings,
                        ) {
                            Icon(Icons.Rounded.Settings, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.BarItem(
    label: String,
    selected: Boolean,
    role: Role,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .weight(1f)
            .height(BarHeight)
            .selectable(
                selected = selected,
                role = role,
                onClick = onClick,
                interactionSource = interactionSource,
                // The touch target spans the whole slot, but its ripple must not: a
                // rectangle covering the full slot is what made taps look boxy.
                indication = null,
            )
            // Nothing is written next to the icon, so the name has to reach TalkBack here.
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = IndicatorWidth, height = IndicatorHeight)
                .clip(CircleShape)
                .background(if (selected) SelectedIndicator else Color.Transparent)
                // Drawn inside the clipped pill, so the feedback follows its rounded shape.
                .indication(interactionSource, ripple()),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides BarContent) { icon() }
        }
    }
}
