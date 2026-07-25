package com.jamal2367.arrcenter.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
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
import androidx.compose.ui.platform.LocalLayoutDirection
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun AppRoot(touches: TouchTracker, viewModel: AppViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    // Nothing is drawn until the stored theme is known, so the app never flashes the wrong
    // colour scheme on start.
    val loaded = settings ?: return

    AppTheme(themeMode = loaded.themeMode) {
        AppContent(
            viewModel = viewModel,
            settings = loaded,
            darkTheme = loaded.themeMode.isDark(),
            touches = touches,
        )
    }
}

@Composable
private fun AppContent(
    viewModel: AppViewModel,
    settings: AppSettings,
    darkTheme: Boolean,
    touches: TouchTracker,
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

    // The navigation is a transient overlay: any touch anywhere in the app brings it back,
    // and it gets out of the way again once the user has been idle for a moment.
    var barVisible by remember { mutableStateOf(false) }

    LaunchedEffect(touches) {
        // collectLatest cancels the pending countdown whenever a new touch arrives, so the
        // bar only disappears after BAR_IDLE_MS without any input at all.
        touches.events.collectLatest {
            barVisible = true
            delay(BAR_IDLE_MS)
            barVisible = false
        }
    }

    // For interactions that produce no touch event of their own - a hardware keyboard, a
    // remote - so the bar does not vanish while the user is working with it.
    val showBar: () -> Unit = { touches.onTouch() }

    val selectService: (ServiceType) -> Unit = { type ->
        showBar()
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

    // Back is a navigation key again: it leaves the settings, then walks the history of the
    // page that is on screen, and only when there is nothing left to go back to may it end
    // the app - and then not on the first press.
    val activity = LocalActivity.current
    val exitMessage = stringResource(R.string.snackbar_exit)
    var exitDeadline by remember { mutableLongStateOf(0L) }

    BackHandler {
        when {
            showSettings -> showSettings = false

            // Only while the page is really on screen: a service that failed to resolve
            // shows the error screen, and stepping through the history of the WebView
            // hidden behind it would look like back does nothing at all.
            state is ServiceState.Ready && controller.goBack() -> Unit

            else -> {
                // elapsedRealtime, not currentTimeMillis: it cannot jump when the clock is
                // adjusted, which would either close the app instantly or never.
                val now = SystemClock.elapsedRealtime()
                if (now <= exitDeadline) {
                    activity?.finish()
                } else {
                    // Nothing has to be cancelled when the second press does not come: the
                    // deadline simply passes and the next press arms the app again.
                    exitDeadline = now + EXIT_CONFIRM_WINDOW_MS
                    scope.launch {
                        // The queue is emptied first, otherwise a message left over from an
                        // earlier press would have to time out before this one is shown.
                        snackbarHostState.currentSnackbarData?.dismiss()
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

    val layoutDirection = LocalLayoutDirection.current

    Surface(color = MaterialTheme.colorScheme.surface) {
        Box(Modifier.fillMaxSize()) {

            Scaffold(
                // Paints the area behind the status bar in the colour of the service, so
                // removing the top app bar does not leave a mismatched strip.
                containerColor = currentService.chromeColor,
                bottomBar = {
                    AnimatedVisibility(
                        visible = barVisible,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    ) {
                        ServiceBottomBar(
                            current = currentService,
                            onSelect = selectService,
                            onReload = {
                                showBar()
                                controller.reload()
                            },
                            onOpenSettings = { showSettings = true },
                        )
                    }
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
                    // Everything but the bottom inset of the Scaffold: the navigation floats
                    // over the page instead of shortening it. It now comes and goes with
                    // every touch, and a WebView that changed height that often would reflow
                    // the page right under the user's finger.
                    modifier = Modifier.padding(
                        start = inner.calculateStartPadding(layoutDirection),
                        top = inner.calculateTopPadding(),
                        end = inner.calculateEndPadding(layoutDirection),
                    ),
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
                    // Toast rather than snackbar: the settings sheet covers the Scaffold's
                    // snackbar host, so the message would be invisible behind it.
                    onExport = { uri ->
                        viewModel.exportSettings(uri) { ok ->
                            toast(
                                context,
                                if (ok) R.string.export_success else R.string.export_failed,
                            )
                        }
                    },
                    onImport = { uri ->
                        viewModel.importSettings(uri) { ok ->
                            if (ok) {
                                // Same cleanup as after saving: the addresses changed, so the
                                // WebViews must navigate to the newly resolved URLs.
                                ServiceType.entries.forEach { host.controllerFor(it).forget() }
                                showSettings = false
                            }
                            toast(
                                context,
                                if (ok) R.string.import_success else R.string.import_failed,
                            )
                        }
                    },
                    onClose = {
                        viewModel.clearConnectionTests()
                        showSettings = false
                    },
                )
            }
        }
    }
}

/** How long the navigation stays on screen after the last user input. */
private const val BAR_IDLE_MS = 3_000L

/** How long the first back press stays armed before the app asks again. */
private const val EXIT_CONFIRM_WINDOW_MS = 2_000L

private fun toast(context: Context, @StringRes message: Int) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
