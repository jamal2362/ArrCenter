package com.jamal2367.arrcenter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jamal2367.arrcenter.R
import com.jamal2367.arrcenter.ui.screens.ServiceScreen
import com.jamal2367.arrcenter.ui.screens.SettingsScreen

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    var fabExpanded by remember { mutableStateOf(false) }
    val currentRoute = navBackStackEntry?.destination?.route

    val backgroundColor = when (currentRoute) {
        "jellyseerr" -> Color(0xFF1D2735)
        "radarr" -> Color(0xFF2A2A2A)
        "sonarr" -> Color(0xFF2A2A2A)
        "sabnzbd" -> Color(0xFF000000)
        else -> MaterialTheme.colorScheme.background
    }

    Scaffold(
        containerColor = backgroundColor,
        floatingActionButton = {
            Box(
                contentAlignment = Alignment.BottomStart,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 32.dp, bottom = 48.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 64.dp)
                ) {
                    if (fabExpanded) {
                        FabItem("jellyseerr", R.drawable.ic_jellyseerr_24dp, R.string.jellyseerr) { route ->
                            navController.navigate(route) { popUpTo(0) }
                            fabExpanded = false
                        }
                        FabItem("radarr", R.drawable.ic_radarr_24dp, R.string.radarr) { route ->
                            navController.navigate(route) { popUpTo(0) }
                            fabExpanded = false
                        }
                        FabItem("settings", null, R.string.settings, Icons.Default.Settings) { route ->
                            navController.navigate(route) { popUpTo(0) }
                            fabExpanded = false
                        }
                        FabItem("sonarr", R.drawable.ic_sonarr_24dp, R.string.sonarr) { route ->
                            navController.navigate(route) { popUpTo(0) }
                            fabExpanded = false
                        }
                        FabItem("sabnzbd", R.drawable.ic_sabnzbd_24dp, R.string.sabnzbd) { route ->
                            navController.navigate(route) { popUpTo(0) }
                            fabExpanded = false
                        }
                    }
                }


                // Haupt-FAB bleibt unten rechts fixiert
                FloatingActionButton(
                    onClick = { fabExpanded = !fabExpanded },
                    shape = CircleShape
                ) {
                    Icon(
                        painter = painterResource(id = if (fabExpanded) R.drawable.ic_close_24dp else R.drawable.ic_menu_24dp),
                        contentDescription = null
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "jellyseerr",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("jellyseerr") { ServiceScreen(ServiceType.Jellyseerr) }
            composable("radarr") { ServiceScreen(ServiceType.Radarr) }
            composable("settings") { SettingsScreen(onSaved = {}) }
            composable("sonarr") { ServiceScreen(ServiceType.Sonarr) }
            composable("sabnzbd") { ServiceScreen(ServiceType.SABnzbd) }
        }
    }
}

@Composable
fun FabItem(
    route: String,
    iconRes: Int? = null,
    labelRes: Int,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: (String) -> Unit
) {
    ExtendedFloatingActionButton(
        onClick = { onClick(route) },
        modifier = Modifier
            .width(165.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp)
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null
                )
            } else if (iconVector != null) {
                Icon(iconVector, contentDescription = null)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(stringResource(id = labelRes))
        }
    }
}

enum class ServiceType {
    Jellyseerr,
    Radarr,
    Sonarr,
    SABnzbd
}
