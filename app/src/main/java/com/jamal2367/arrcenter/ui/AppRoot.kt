package com.jamal2367.arrcenter.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jamal2367.arrcenter.R
import com.jamal2367.arrcenter.data.AppSettings
import com.jamal2367.arrcenter.model.ServiceType
import com.jamal2367.arrcenter.net.ACCESS_LOCAL_NETWORK
import com.jamal2367.arrcenter.net.needsLocalNetworkPermission
import com.jamal2367.arrcenter.ui.components.ServiceBottomBar
import com.jamal2367.arrcenter.ui.screens.ServiceScreen
import com.jamal2367.arrcenter.ui.screens.SettingsScreen
import com.jamal2367.arrcenter.ui.theme.AppTheme
import com.jamal2367.arrcenter.ui.theme.SystemBarIcons
import com.jamal2367.arrcenter.ui.theme.isDark
import com.jamal2367.arrcenter.web.WebViewHost
import kotlinx.coroutines.launch

@Composable
fun AppRoot(viewModel: AppViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    // Nothing is drawn until the stored theme is known, so the app never flashes the wrong
    // colour scheme on start.
    val loaded = settings ?: return

    AppTheme(themeMode = loaded.themeMode) {
        AppContent(
            viewModel = viewModel,
            settings = loaded,
            darkTheme = loaded.themeMode.isDark(),
        )
    }
}

@Composable
private fun AppContent(
    viewModel: AppViewModel,
    settings: AppSettings,
    darkTheme: Boolean,
) {
    val serviceStates by viewModel.serviceStates.collectAsStateWithLifecycle()
    val connectionTests by viewModel.connectionTests.collectAsStateWithLifecycle()

    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val currentService = ServiceType.fromId(selectedId) ?: settings.startService
    var showSettings by rememberSaveable { mutableStateOf(false) }

    // One host for the whole app: the WebViews live here and are destroyed exactly once,
    // when the composition goes away together with the activity.
    val host = remember { WebViewHost() }
    DisposableEffect(host) {
        onDispose { host.destroyAll() }
    }

    val singleFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        host.deliverFileChooserResult(listOfNotNull(uri).toTypedArray())
    }
    val multiFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris: List<Uri> ->
        host.deliverFileChooserResult(uris.toTypedArray())
    }

    // Registered here rather than captured inside a WebView callback: the launchers belong
    // to this composition, the WebViews outlive it.
    DisposableEffect(host, singleFileLauncher, multiFileLauncher) {
        host.requestFileChooser = { mimeTypes, allowMultiple ->
            if (allowMultiple) {
                multiFileLauncher.launch(mimeTypes)
            } else {
                singleFileLauncher.launch(mimeTypes)
            }
        }
        onDispose { host.requestFileChooser = null }
    }

    val controller = host.controllerFor(currentService)
    val state = serviceStates[currentService] ?: ServiceState.Idle

    val currentEndpoints = settings.endpointsFor(currentService)

    LaunchedEffect(currentService, currentEndpoints) {
        viewModel.resolve(currentService, currentEndpoints)
    }

    // Local Network Protection: without this permission nothing on the LAN is reachable,
    // so ask for it before the first connection attempt and retry once it is granted.
    val context = LocalContext.current
    var permissionMissing by remember { mutableStateOf(needsLocalNetworkPermission(context)) }

    val localNetworkPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionMissing = !granted
        if (granted) {
            controller.forget()
            viewModel.resolve(currentService, currentEndpoints, force = true)
        }
    }

    LaunchedEffect(Unit) {
        if (permissionMissing) localNetworkPermission.launch(ACCESS_LOCAL_NETWORK)
    }

    // Picks up a permission granted in the system settings while the app was in background.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val missing = needsLocalNetworkPermission(context)
        if (permissionMissing && !missing) {
            controller.forget()
            viewModel.resolve(currentService, currentEndpoints, force = true)
        }
        permissionMissing = missing
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedMessage = stringResource(R.string.snackbar_saved)

    val retry: () -> Unit = {
        controller.forget()
        viewModel.resolve(currentService, currentEndpoints, force = true)
    }

    val selectService: (ServiceType) -> Unit = { type ->
        if (type != currentService) {
            // Dropping the remembered URL makes the target service load its page again
            // instead of showing whatever was left on screen from the last visit. The
            // WebView itself stays alive, so cookies and the login session survive.
            host.controllerFor(type).forget()
            selectedId = type.id

            // A service that failed earlier keeps that result cached and would greet the
            // user with the error screen again. Probe once more on arrival - the same thing
            // the retry button does - so a server that came back up just works.
            if (viewModel.stateOf(type) is ServiceState.Unreachable) {
                viewModel.resolve(type, settings.endpointsFor(type), force = true)
            }
        }
    }

    val exitMessage = stringResource(R.string.snackbar_exit)
    var exitArmedAt by remember { mutableLongStateOf(0L) }

    // Always enabled: the last back press must not reach the system directly, otherwise the
    // app would close without the confirmation step.
    BackHandler {
        when {
            showSettings -> showSettings = false
            controller.goBack() -> Unit
            currentService != settings.startService -> selectService(settings.startService)

            else -> {
                // elapsedRealtime, not currentTimeMillis: it cannot jump when the clock is
                // adjusted, which would either close the app instantly or never.
                val now = SystemClock.elapsedRealtime()
                if (now - exitArmedAt <= EXIT_CONFIRM_WINDOW_MS) {
                    context.findActivity()?.finish()
                } else {
                    exitArmedAt = now
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = exitMessage,
                            duration = SnackbarDuration.Short,
                        )
                    }
                }
            }
        }
    }

    // The service screens are always dark behind the system bars; the settings screen
    // follows the app theme.
    SystemBarIcons(lightIcons = !showSettings || darkTheme)

    Surface(color = MaterialTheme.colorScheme.surface) {
        Box(Modifier.fillMaxSize()) {

            Scaffold(
                // Paints the area behind the status bar in the colour of the service, so
                // removing the top app bar does not leave a mismatched strip.
                containerColor = currentService.chromeColor,
                bottomBar = {
                    ServiceBottomBar(
                        current = currentService,
                        onSelect = selectService,
                        onReload = { controller.reload() },
                        onOpenSettings = { showSettings = true },
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { inner ->
                ServiceScreen(
                    type = currentService,
                    state = state,
                    host = host,
                    onRetry = retry,
                    permissionMissing = permissionMissing,
                    onGrantPermission = { openAppSettings(context) },
                    modifier = Modifier.padding(inner),
                )
            }

            AnimatedVisibility(
                visible = showSettings,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            ) {
                SettingsScreen(
                    settings = settings,
                    connectionTests = connectionTests,
                    onSave = { endpoints ->
                        viewModel.saveEndpoints(endpoints) {
                            scope.launch { snackbarHostState.showSnackbar(savedMessage) }
                        }
                        // Addresses may have changed, so drop what the WebViews currently
                        // show and let them navigate to the newly resolved URL.
                        ServiceType.entries.forEach { host.controllerFor(it).forget() }
                        showSettings = false
                    },
                    onTest = viewModel::testConnection,
                    onThemeModeChange = viewModel::setThemeMode,
                    onStartServiceChange = viewModel::setStartService,
                    onClose = {
                        viewModel.clearConnectionTests()
                        showSettings = false
                    },
                )
            }
        }
    }
}

/** How long the first back press stays armed before the app asks again. */
private const val EXIT_CONFIRM_WINDOW_MS = 2_000L

/**
 * Walks up the context chain to the hosting activity.
 *
 * `LocalContext` is not guaranteed to be the activity - it can be a wrapper, for instance
 * once a different configuration is applied to a subtree - so casting it directly would be
 * a crash waiting to happen.
 */
private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/**
 * Opens this app's page in the system settings.
 *
 * Needed because Android stops showing the permission dialog after the user declined twice;
 * from then on the switch can only be flipped in the settings.
 */
private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
