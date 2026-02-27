package com.jamal2367.arrcenter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
    val scrollState = rememberScrollState()

    var seerrPrimary by remember { mutableStateOf("") }
    var seerrSecondary by remember { mutableStateOf("") }
    var radarrPrimary by remember { mutableStateOf("") }
    var radarrSecondary by remember { mutableStateOf("") }
    var sonarrPrimary by remember { mutableStateOf("") }
    var sonarrSecondary by remember { mutableStateOf("") }
    var sabnzbdPrimary by remember { mutableStateOf("") }
    var sabnzbdSecondary by remember { mutableStateOf("") }
    var uvsPrimary by remember { mutableStateOf("") }
    var uvsSecondary by remember { mutableStateOf("") }

    LaunchedEffect(prefsFlow.value) {
        seerrPrimary = prefsFlow.value[SettingsKeys.SEERR_PRIMARY] ?: ""
        seerrSecondary = prefsFlow.value[SettingsKeys.SEERR_SECONDARY] ?: ""
        radarrPrimary = prefsFlow.value[SettingsKeys.RADARR_PRIMARY] ?: ""
        radarrSecondary = prefsFlow.value[SettingsKeys.RADARR_SECONDARY] ?: ""
        sonarrPrimary = prefsFlow.value[SettingsKeys.SONARR_PRIMARY] ?: ""
        sonarrSecondary = prefsFlow.value[SettingsKeys.SONARR_SECONDARY] ?: ""
        sabnzbdPrimary = prefsFlow.value[SettingsKeys.SABNZBD_PRIMARY] ?: ""
        sabnzbdSecondary = prefsFlow.value[SettingsKeys.SABNZBD_SECONDARY] ?: ""
        uvsPrimary = prefsFlow.value[SettingsKeys.UVS_PRIMARY] ?: ""
        uvsSecondary = prefsFlow.value[SettingsKeys.UVS_SECONDARY] ?: ""
    }

    Scaffold { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .verticalScroll(scrollState)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = inner.calculateTopPadding(),
                    bottom = inner.calculateBottomPadding()
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp)
            )

            SettingsSection(
                title = stringResource(R.string.seerr),
                primaryValue = seerrPrimary,
                onPrimaryChange = { seerrPrimary = it },
                secondaryValue = seerrSecondary,
                onSecondaryChange = { seerrSecondary = it }
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
                title = stringResource(R.string.uvs),
                primaryValue = uvsPrimary,
                onPrimaryChange = { uvsPrimary = it },
                secondaryValue = uvsSecondary,
                onSecondaryChange = { uvsSecondary = it }
            )

            Button(
                onClick = {
                    scope.launch {
                        context.dataStore.edit { e ->
                            e[SettingsKeys.SEERR_PRIMARY] = seerrPrimary
                            e[SettingsKeys.SEERR_SECONDARY] = seerrSecondary
                            e[SettingsKeys.RADARR_PRIMARY] = radarrPrimary
                            e[SettingsKeys.RADARR_SECONDARY] = radarrSecondary
                            e[SettingsKeys.SONARR_PRIMARY] = sonarrPrimary
                            e[SettingsKeys.SONARR_SECONDARY] = sonarrSecondary
                            e[SettingsKeys.SABNZBD_PRIMARY] = sabnzbdPrimary
                            e[SettingsKeys.SABNZBD_SECONDARY] = sabnzbdSecondary
                            e[SettingsKeys.UVS_PRIMARY] = uvsPrimary
                            e[SettingsKeys.UVS_SECONDARY] = uvsSecondary
                        }
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.snackbar_saved),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        onSaved()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .align(Alignment.CenterHorizontally),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = stringResource(R.string.save),
                    style = MaterialTheme.typography.titleMedium
                )
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
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = primaryValue,
                onValueChange = onPrimaryChange,
                label = { Text(stringResource(R.string.primary_url)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = secondaryValue,
                onValueChange = onSecondaryChange,
                label = { Text(stringResource(R.string.secondary_url)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface),
                singleLine = true
            )
        }
    }
}
