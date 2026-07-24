package com.jamal2367.arrcenter.ui.screens

import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.jamal2367.arrcenter.R
import com.jamal2367.arrcenter.model.ServiceType
import com.jamal2367.arrcenter.ui.ServiceState
import com.jamal2367.arrcenter.ui.components.StatusAction
import com.jamal2367.arrcenter.ui.components.StatusView
import com.jamal2367.arrcenter.web.ServiceWebController
import com.jamal2367.arrcenter.web.WebViewHost
import com.jamal2367.arrcenter.web.createServiceWebView

/**
 * Shows one service: either the loaded web app, or why it cannot be shown.
 *
 * The screen no longer owns any networking - it renders the [ServiceState] produced by the
 * view model, which means a service keeps its resolved address across configuration changes
 * instead of probing the network again on every recomposition.
 */
@Composable
fun ServiceScreen(
    type: ServiceType,
    state: ServiceState,
    host: WebViewHost,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    permissionMissing: Boolean = false,
    onGrantPermission: () -> Unit = {},
) {
    val label = stringResource(type.labelRes)
    val onChrome = Color.White.copy(alpha = 0.92f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(type.chromeColor),
    ) {
        when (state) {
            ServiceState.Idle, ServiceState.Resolving -> Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = onChrome)
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.connecting, label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = onChrome.copy(alpha = 0.75f),
                )
            }

            // No action button here: the settings are one tap away in the bottom bar, which
            // is visible on this screen anyway.
            ServiceState.NotConfigured -> StatusView(
                icon = Icons.Rounded.Settings,
                title = stringResource(R.string.not_configured, label),
                message = stringResource(R.string.not_configured_hint),
                contentColor = onChrome,
            )

            // A missing local network permission blocks every request, so naming it beats
            // showing a generic "no connection".
            is ServiceState.Unreachable -> if (permissionMissing) {
                StatusView(
                    icon = Icons.Rounded.Warning,
                    title = stringResource(R.string.local_network_blocked),
                    message = stringResource(R.string.local_network_blocked_hint),
                    contentColor = onChrome,
                    primaryAction = StatusAction(
                        label = stringResource(R.string.grant_permission),
                        onClick = onGrantPermission,
                    ),
                    secondaryAction = StatusAction(
                        label = stringResource(R.string.retry),
                        onClick = onRetry,
                    ),
                )
            } else {
                StatusView(
                    icon = Icons.Rounded.Warning,
                    title = stringResource(R.string.no_connection),
                    message = stringResource(R.string.no_connection_hint, label),
                    // The concrete network error - without it every cause looks identical.
                    detail = state.reason,
                    contentColor = onChrome,
                    primaryAction = StatusAction(
                        label = stringResource(R.string.retry),
                        onClick = onRetry,
                    ),
                )
            }

            is ServiceState.Ready -> ServiceWebContent(
                type = type,
                url = state.url,
                host = host,
                onRetry = onRetry,
                contentColor = onChrome,
            )
        }
    }
}

@Composable
private fun ServiceWebContent(
    type: ServiceType,
    url: String,
    host: WebViewHost,
    onRetry: () -> Unit,
    contentColor: Color,
) {
    val context = LocalContext.current
    val controller: ServiceWebController = host.controllerFor(type)

    // Stop timers and media while the app is in the background instead of letting the page
    // keep running - the previous version never paused its WebView.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        controller.webView?.onPause()
        controller.webView?.pauseTimers()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        controller.webView?.onResume()
        controller.webView?.resumeTimers()
    }

    Box(Modifier.fillMaxSize()) {
        // Each service needs its own node. AndroidView calls factory() once per node, so
        // without this key Compose reuses the node when switching between two services that
        // are both already resolved - factory() never runs again and the previously
        // attached WebView stays on screen until the app is restarted.
        key(type) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                factory = {
                    // Reuse the WebView of this service if it was created before, so
                    // switching keeps scroll position, history and the logged in session.
                    // Detaching first is required: disposing the previous node leaves the
                    // WebView parented to a holder that is no longer in the hierarchy.
                    controller.webView
                        ?.also { existing -> (existing.parent as? ViewGroup)?.removeView(existing) }
                        ?: createServiceWebView(context, type, controller, host)
                },
                // Deliberately no destroy here: the WebView outlives this composable and is
                // torn down by WebViewHost when the activity goes away.
                onRelease = { },
            )
        }

        LaunchedEffect(controller, url) {
            controller.loadIfNeeded(url)
        }

        AnimatedVisibility(
            visible = controller.isLoading && controller.progress < 1f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            val progressLabel = stringResource(R.string.loading_progress)
            LinearProgressIndicator(
                progress = { controller.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = progressLabel },
                color = type.brandColor,
                trackColor = Color.Transparent,
                drawStopIndicator = { },
            )
        }

        controller.loadError?.let { detail ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(type.chromeColor),
            ) {
                StatusView(
                    icon = Icons.Rounded.Warning,
                    title = stringResource(R.string.load_failed),
                    detail = detail.takeIf { it.isNotBlank() },
                    contentColor = contentColor,
                    primaryAction = StatusAction(
                        label = stringResource(R.string.retry),
                        onClick = onRetry,
                    ),
                )
            }
        }
    }
}
