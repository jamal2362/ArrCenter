package com.jamal2367.arrcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.jamal2367.arrcenter.data.SettingsKeys
import com.jamal2367.arrcenter.data.dataStore
import kotlinx.coroutines.launch
import com.jamal2367.arrcenter.R

@Composable
fun SettingsScreen(onSaved: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsFlow = context.dataStore.data.collectAsState(initial = emptyPreferences())
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    var jellyPrimary by remember { mutableStateOf("") }
    var jellySecondary by remember { mutableStateOf("") }
    var radarrPrimary by remember { mutableStateOf("") }
    var radarrSecondary by remember { mutableStateOf("") }
    var sonarrPrimary by remember { mutableStateOf("") }
    var sonarrSecondary by remember { mutableStateOf("") }
    var sabnzbdPrimary by remember { mutableStateOf("") }
    var sabnzbdSecondary by remember { mutableStateOf("") }
    var ugreenPrimary by remember { mutableStateOf("") }
    var ugreenSecondary by remember { mutableStateOf("") }

    LaunchedEffect(prefsFlow.value) {
        jellyPrimary = prefsFlow.value[SettingsKeys.JELLY_PRIMARY] ?: ""
        jellySecondary = prefsFlow.value[SettingsKeys.JELLY_SECONDARY] ?: ""
        radarrPrimary = prefsFlow.value[SettingsKeys.RADARR_PRIMARY] ?: ""
        radarrSecondary = prefsFlow.value[SettingsKeys.RADARR_SECONDARY] ?: ""
        sonarrPrimary = prefsFlow.value[SettingsKeys.SONARR_PRIMARY] ?: ""
        sonarrSecondary = prefsFlow.value[SettingsKeys.SONARR_SECONDARY] ?: ""
        sabnzbdPrimary = prefsFlow.value[SettingsKeys.SABNZBD_PRIMARY] ?: ""
        sabnzbdSecondary = prefsFlow.value[SettingsKeys.SABNZBD_SECONDARY] ?: ""
        ugreenPrimary = prefsFlow.value[SettingsKeys.UGREEN_PRIMARY] ?: ""
        ugreenSecondary = prefsFlow.value[SettingsKeys.UGREEN_SECONDARY] ?: ""
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )}
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .verticalScroll(scrollState)
                .padding(
                    bottom = inner.calculateBottomPadding(),
                    start = 12.dp,
                    end = 12.dp
                )
        ) {
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp)
            )

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

            SettingsSection(
                title = stringResource(R.string.ugreen),
                primaryValue = ugreenPrimary,
                onPrimaryChange = { ugreenPrimary = it },
                secondaryValue = ugreenSecondary,
                onSecondaryChange = { ugreenSecondary = it }
            )

            Spacer(Modifier.height(8.dp))

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
                            e[SettingsKeys.UGREEN_PRIMARY] = ugreenPrimary
                            e[SettingsKeys.UGREEN_SECONDARY] = ugreenSecondary
                        }
                        snackbarHostState.showSnackbar(context.getString(R.string.snackbar_saved))
                        onSaved()
                    }
                },
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = primaryValue,
                onValueChange = onPrimaryChange,
                label = { Text(stringResource(R.string.primary_url), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = secondaryValue,
                onValueChange = onSecondaryChange,
                label = { Text(stringResource(R.string.secondary_url), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

