package com.jamal2367.arrcenter.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jamal2367.arrcenter.R
import com.jamal2367.arrcenter.ui.screens.ServiceScreen
import com.jamal2367.arrcenter.ui.screens.SettingsScreen

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    mapOf(
        "jellyseerr" to stringResource(R.string.jellyseerr),
        "radarr" to stringResource(R.string.radarr),
        "settings" to stringResource(R.string.settings),
        "sonarr" to stringResource(R.string.sonarr),
        "sabnzbd" to stringResource(R.string.sabnzbd)
    )
    var currentRoute by remember { mutableStateOf("jellyseerr") }

    Scaffold(
        bottomBar = {
            NavigationBar {
                fun navigate(route: String) {
                    currentRoute = route
                    navController.navigate(route) { popUpTo(0) }
                }

                NavigationBarItem(
                    icon = { Icon(painter = painterResource(id = R.drawable.ic_jellyseerr_24dp), contentDescription = null) },
                    label = { Text(stringResource(R.string.jellyseerr)) },
                    selected = currentRoute == "jellyseerr",
                    onClick = { navigate("jellyseerr") }
                )
                NavigationBarItem(
                    icon = { Icon(painter = painterResource(id = R.drawable.ic_radarr_24dp), contentDescription = null) },
                    label = { Text(stringResource(R.string.radarr)) },
                    selected = currentRoute == "radarr",
                    onClick = { navigate("radarr") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.settings)) },
                    selected = currentRoute == "settings",
                    onClick = { navigate("settings") }
                )
                NavigationBarItem(
                    icon = { Icon(painter = painterResource(id = R.drawable.ic_sonarr_24dp), contentDescription = null) },
                    label = { Text(stringResource(R.string.sonarr)) },
                    selected = currentRoute == "sonarr",
                    onClick = { navigate("sonarr") }
                )
                NavigationBarItem(
                    icon = { Icon(painter = painterResource(id = R.drawable.ic_sabnzbd_24dp), contentDescription = null) },
                    label = { Text(stringResource(R.string.sabnzbd)) },
                    selected = currentRoute == "sabnzbd",
                    onClick = { navigate("sabnzbd") }
                )
            }
        }
    )
    { innerPadding ->
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

enum class ServiceType() {
    Jellyseerr(),
    Radarr(),
    Sonarr(),
    SABnzbd()
}
