package com.jamal2367.arrcenter.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.jamal2367.arrcenter.R
import com.jamal2367.arrcenter.data.AppSettings
import com.jamal2367.arrcenter.data.BACKUP_FILE_NAME
import com.jamal2367.arrcenter.data.ServiceEndpoints
import com.jamal2367.arrcenter.data.ThemeMode
import com.jamal2367.arrcenter.model.ServiceType
import com.jamal2367.arrcenter.net.isAcceptableUrlInput
import com.jamal2367.arrcenter.ui.ConnectionTest

/**
 * Settings for every service plus appearance.
 *
 * The screen has a real top bar with a back action - previously it could only be entered,
 * never left, because the back handler lived in the service screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    connectionTests: Map<ServiceType, ConnectionTest>,
    onSave: (Map<ServiceType, ServiceEndpoints>) -> Unit,
    onTest: (ServiceType, ServiceEndpoints) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onStartServiceChange: (ServiceType) -> Unit,
    onExport: (Uri) -> Unit,
    onImport: (Uri) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? -> uri?.let(onExport) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? -> uri?.let(onImport) }

    // Local draft of the text fields. Seeded once per settings revision so a background
    // DataStore emission cannot overwrite what the user is currently typing.
    val drafts = remember { mutableStateMapOf<ServiceType, ServiceEndpoints>() }
    var seeded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!seeded) {
            ServiceType.entries.forEach { drafts[it] = settings.endpointsFor(it) }
            seeded = true
        }
    }

    val hasInvalidInput = drafts.values.any {
        !isAcceptableUrlInput(it.primary) || !isAcceptableUrlInput(it.secondary)
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings)) },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
        ) { inner ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = inner.calculateTopPadding() + 8.dp,
                    bottom = inner.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(ServiceType.entries, key = { it.id }) { type ->
                    ServiceSettingsCard(
                        type = type,
                        endpoints = drafts[type] ?: ServiceEndpoints(),
                        test = connectionTests[type],
                        onChange = { drafts[type] = it },
                        onTest = { onTest(type, drafts[type] ?: ServiceEndpoints()) },
                    )
                }

                item {
                    BackupCard(
                        onExport = { exportLauncher.launch(BACKUP_FILE_NAME) },
                        // Not filtered by MIME type: content providers routinely report .json
                        // as octet-stream, which would hide the user's own backup file.
                        onImport = { importLauncher.launch(arrayOf("*/*")) },
                    )
                }

                item {
                    AppearanceCard(
                        themeMode = settings.themeMode,
                        startService = settings.startService,
                        onThemeModeChange = onThemeModeChange,
                        onStartServiceChange = onStartServiceChange,
                    )
                }

                item {
                    Button(
                        onClick = { onSave(drafts.toMap()) },
                        enabled = !hasInvalidInput,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceSettingsCard(
    type: ServiceType,
    endpoints: ServiceEndpoints,
    test: ConnectionTest?,
    onChange: (ServiceEndpoints) -> Unit,
    onTest: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(type.iconRes),
                        contentDescription = null,
                        tint = type.brandColor,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(Modifier.size(12.dp))
                Text(
                    text = stringResource(type.labelRes),
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            Spacer(Modifier.height(16.dp))

            UrlField(
                value = endpoints.primary,
                onValueChange = { onChange(endpoints.copy(primary = it)) },
                label = stringResource(R.string.primary_url),
                imeAction = ImeAction.Next,
            )

            Spacer(Modifier.height(12.dp))

            UrlField(
                value = endpoints.secondary,
                onValueChange = { onChange(endpoints.copy(secondary = it)) },
                label = stringResource(R.string.secondary_url),
                imeAction = ImeAction.Done,
            )

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onTest, enabled = test !is ConnectionTest.Running) {
                    Text(stringResource(R.string.test_connection))
                }
                Spacer(Modifier.size(8.dp))
                ConnectionTestResult(test)
            }
        }
    }
}

@Composable
private fun UrlField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    imeAction: ImeAction,
) {
    val isValid = isAcceptableUrlInput(value)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(stringResource(R.string.url_hint)) },
        supportingText = if (isValid) null else {
            { Text(stringResource(R.string.url_invalid)) }
        },
        isError = !isValid,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = imeAction,
            autoCorrectEnabled = false,
            capitalization = KeyboardCapitalization.None,
        ),
    )
}

@Composable
private fun ConnectionTestResult(test: ConnectionTest?) {
    AnimatedVisibility(visible = test != null) {
        when (test) {
            ConnectionTest.Running -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.test_running),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            is ConnectionTest.Ok -> TestBadge(
                text = stringResource(
                    if (test.usedFallback) R.string.test_secondary_ok else R.string.test_primary_ok
                ),
                positive = true,
            )

            is ConnectionTest.Failed -> TestBadge(
                text = stringResource(R.string.test_failed),
                detail = test.reason,
                positive = false,
            )

            ConnectionTest.Empty -> TestBadge(
                text = stringResource(R.string.test_empty),
                positive = false,
            )

            null -> Unit
        }
    }
}

@Composable
private fun TestBadge(text: String, positive: Boolean, detail: String? = null) {
    val color = if (positive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (positive) Icons.Rounded.Check else Icons.Rounded.Close,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.size(6.dp))
            Text(text = text, style = MaterialTheme.typography.bodySmall, color = color)
        }
        if (!detail.isNullOrBlank()) {
            // The raw exception: this is what turns "not reachable" into something the user
            // can actually act on.
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BackupCard(
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.backup),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.backup_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.export_settings))
                }
                OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.import_settings))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AppearanceCard(
    themeMode: ThemeMode,
    startService: ServiceType,
    onThemeModeChange: (ThemeMode) -> Unit,
    onStartServiceChange: (ServiceType) -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.appearance),
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(Modifier.height(12.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = mode == themeMode,
                        onClick = { onThemeModeChange(mode) },
                        label = {
                            Text(
                                stringResource(
                                    when (mode) {
                                        ThemeMode.System -> R.string.theme_system
                                        ThemeMode.Light -> R.string.theme_light
                                        ThemeMode.Dark -> R.string.theme_dark
                                    }
                                )
                            )
                        },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.start_service),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.start_service_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ServiceType.entries.forEach { type ->
                    FilterChip(
                        selected = type == startService,
                        onClick = { onStartServiceChange(type) },
                        label = { Text(stringResource(type.labelRes)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(type.iconRes),
                                contentDescription = null,
                                tint = type.brandColor,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
            }
        }
    }
}
