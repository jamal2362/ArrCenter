package com.jamal2367.arrcenter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jamal2367.arrcenter.R
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
        "jellyseerr" -> Color(0xFF1D2735)
        "radarr" -> Color(0xFF2A2A2A)
        "sonarr" -> Color(0xFF2A2A2A)
        "sabnzbd" -> Color(0xFF000000)
        else -> MaterialTheme.colorScheme.background
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
            composable("jellyseerr") { ServiceScreen(ServiceType.Jellyseerr, onShowSheet = { showSheet = true }) }
            composable("radarr") { ServiceScreen(ServiceType.Radarr, onShowSheet = { showSheet = true }) }
            composable("sonarr") { ServiceScreen(ServiceType.Sonarr, onShowSheet = { showSheet = true }) }
            composable("sabnzbd") { ServiceScreen(ServiceType.SABnzbd, onShowSheet = { showSheet = true }) }
            composable("settings") { SettingsScreen(onSaved = {}) }
        }

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showSheet = false
                    onShowSheetChange?.invoke(false)
                },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
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

@Composable
fun SheetItem(
    onClick: (String) -> Unit
) {
    val items = listOf(
        Triple("jellyseerr", R.drawable.ic_jellyseerr_24dp, "Jellyseerr"),
        Triple("radarr", R.drawable.ic_radarr_24dp, "Radarr"),
        Triple("sonarr", R.drawable.ic_sonarr_24dp, "Sonarr"),
        Triple("sabnzbd", R.drawable.ic_sabnzbd_24dp, "SABnzbd"),
        Triple("settings", null, "Settings")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.dropLast(1).chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowItems.forEach { (route, iconRes, _) ->
                    Surface(
                        onClick = { onClick(route) },
                        modifier = Modifier
                            .weight(1f)
                            .size(80.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (iconRes != null) {
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = route,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(36.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = route,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
                if (rowItems.size < 2) {
                    repeat(2 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }

        val (route, iconRes, _) = items.last()
        Surface(
            onClick = { onClick(route) },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (iconRes != null) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = route,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = route,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}


enum class ServiceType {
    Jellyseerr,
    Radarr,
    Sonarr,
    SABnzbd
}