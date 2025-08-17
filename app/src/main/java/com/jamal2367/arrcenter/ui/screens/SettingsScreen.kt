package com.jamal2367.arrcenter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.jamal2367.arrcenter.data.SettingsKeys
import com.jamal2367.arrcenter.data.dataStore
import kotlinx.coroutines.launch
import com.jamal2367.arrcenter.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onSaved: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsFlow = context.dataStore.data.collectAsState(initial = emptyPreferences())

    var jellyPrimary by remember { mutableStateOf("") }
    var jellySecondary by remember { mutableStateOf("") }
    var radarrPrimary by remember { mutableStateOf("") }
    var radarrSecondary by remember { mutableStateOf("") }
    var sonarrPrimary by remember { mutableStateOf("") }
    var sonarrSecondary by remember { mutableStateOf("") }
    var sabnzbdPrimary by remember { mutableStateOf("") }
    var sabnzbdSecondary by remember { mutableStateOf("") }

    LaunchedEffect(prefsFlow.value) {
        jellyPrimary = prefsFlow.value[SettingsKeys.JELLY_PRIMARY] ?: ""
        jellySecondary = prefsFlow.value[SettingsKeys.JELLY_SECONDARY] ?: ""
        radarrPrimary = prefsFlow.value[SettingsKeys.RADARR_PRIMARY] ?: ""
        radarrSecondary = prefsFlow.value[SettingsKeys.RADARR_SECONDARY] ?: ""
        sonarrPrimary = prefsFlow.value[SettingsKeys.SONARR_PRIMARY] ?: ""
        sonarrSecondary = prefsFlow.value[SettingsKeys.SONARR_SECONDARY] ?: ""
        sabnzbdPrimary = prefsFlow.value[SettingsKeys.SABNZBD_PRIMARY] ?: ""
        sabnzbdSecondary = prefsFlow.value[SettingsKeys.SABNZBD_SECONDARY] ?: ""
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    bottom = inner.calculateBottomPadding(),
                    start = 12.dp,
                    end = 12.dp
                )
        ) {
            SettingsSection(
                title = stringResource(R.string.jellyseerr),
                primaryValue = jellyPrimary,
                onPrimaryChange = { jellyPrimary = it },
                secondaryValue = jellySecondary,
                onSecondaryChange = { jellySecondary = it }
            )

            SettingsSection(
                title = stringResource(R.string.radarr),
                primaryValue = radarrPrimary,
                onPrimaryChange = { radarrPrimary = it },
                secondaryValue = radarrSecondary,
                onSecondaryChange = { radarrSecondary = it }
            )

            SettingsSection(
                title = stringResource(R.string.sonarr),
                primaryValue = sonarrPrimary,
                onPrimaryChange = { sonarrPrimary = it },
                secondaryValue = sonarrSecondary,
                onSecondaryChange = { sonarrSecondary = it }
            )

            SettingsSection(
                title = stringResource(R.string.sabnzbd),
                primaryValue = sabnzbdPrimary,
                onPrimaryChange = { sabnzbdPrimary = it },
                secondaryValue = sabnzbdSecondary,
                onSecondaryChange = { sabnzbdSecondary = it }
            )

            Button(
                onClick = {
                    scope.launch {
                        context.dataStore.edit { e ->
                            e[SettingsKeys.JELLY_PRIMARY] = jellyPrimary
                            e[SettingsKeys.JELLY_SECONDARY] = jellySecondary
                            e[SettingsKeys.RADARR_PRIMARY] = radarrPrimary
                            e[SettingsKeys.RADARR_SECONDARY] = radarrSecondary
                            e[SettingsKeys.SONARR_PRIMARY] = sonarrPrimary
                            e[SettingsKeys.SONARR_SECONDARY] = sonarrSecondary
                            e[SettingsKeys.SABNZBD_PRIMARY] = sabnzbdPrimary
                            e[SettingsKeys.SABNZBD_SECONDARY] = sabnzbdSecondary
                        }
                        snackbarHostState.showSnackbar(context.getString(R.string.snackbar_saved))
                        onSaved()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(stringResource(R.string.save), fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    primaryValue: String,
    onPrimaryChange: (String) -> Unit,
    secondaryValue: String,
    onSecondaryChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = primaryValue,
                onValueChange = onPrimaryChange,
                label = { Text(stringResource(R.string.primary_url)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = secondaryValue,
                onValueChange = onSecondaryChange,
                label = { Text(stringResource(R.string.secondary_url)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
