package com.jamal2367.arrcenter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jamal2367.arrcenter.helper.ServiceType
import com.jamal2367.arrcenter.helper.SheetItem
import com.jamal2367.arrcenter.ui.screens.ServiceScreen
import com.jamal2367.arrcenter.ui.screens.SettingsScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    showSheetExternally: Boolean = false,
    onShowSheetChange: ((Boolean) -> Unit)? = null
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val coroutineScope = rememberCoroutineScope()
    var showSheet by remember { mutableStateOf(showSheetExternally) }
    val sheetState = rememberModalBottomSheetState()

    val backgroundColor = when (currentRoute) {
        "jellyseerr" -> Color(0xFF111827)
        "radarr" -> Color(0xFF202020)
        "sonarr" -> Color(0xFF202020)
        "sabnzbd" -> Color(0xFF000000)
        "ugreen" -> Color(0xFF07011D)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    LaunchedEffect(showSheetExternally) {
        showSheet = showSheetExternally
    }

    Scaffold(
        containerColor = backgroundColor
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = "jellyseerr",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("jellyseerr") { ServiceScreen(ServiceType.Jellyseerr, backgroundColor = backgroundColor, onShowSheet = { showSheet = true }) }
            composable("radarr") { ServiceScreen(ServiceType.Radarr, backgroundColor = backgroundColor, onShowSheet = { showSheet = true }) }
            composable("sonarr") { ServiceScreen(ServiceType.Sonarr, backgroundColor = backgroundColor, onShowSheet = { showSheet = true }) }
            composable("sabnzbd") { ServiceScreen(ServiceType.SABnzbd, backgroundColor = backgroundColor, onShowSheet = { showSheet = true }) }
            composable("ugreen") { ServiceScreen(ServiceType.Ugreen, backgroundColor = backgroundColor, onShowSheet = { showSheet = true }) }
            composable("settings") { SettingsScreen(onSaved = {}) }
        }

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showSheet = false
                    onShowSheetChange?.invoke(false)
                },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SheetItem { route ->
                        navController.navigate(route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                        coroutineScope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion { showSheet = false }
                    }

                }
            }
        }
    }
}
