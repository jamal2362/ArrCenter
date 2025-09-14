package com.jamal2367.arrcenter.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.jamal2367.arrcenter.R
import com.jamal2367.arrcenter.ui.ServiceType
import com.jamal2367.arrcenter.data.SettingsKeys
import com.jamal2367.arrcenter.data.dataStore
import com.jamal2367.arrcenter.helper.isReachable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ServiceScreen(type: ServiceType, onShowSheet: (() -> Unit)? = null) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    var currentUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    suspend fun loadUrl() {
        isLoading = true
        error = false
        currentUrl = null

        val prefs = context.dataStore.data.first()

        val (primary, secondary) = when (type) {
            ServiceType.Jellyseerr -> prefs[SettingsKeys.JELLY_PRIMARY] to prefs[SettingsKeys.JELLY_SECONDARY]
            ServiceType.Radarr -> prefs[SettingsKeys.RADARR_PRIMARY] to prefs[SettingsKeys.RADARR_SECONDARY]
            ServiceType.Sonarr -> prefs[SettingsKeys.SONARR_PRIMARY] to prefs[SettingsKeys.SONARR_SECONDARY]
            ServiceType.SABnzbd -> prefs[SettingsKeys.SABNZBD_PRIMARY] to prefs[SettingsKeys.SABNZBD_SECONDARY]
        }

        val candidate = withContext(Dispatchers.IO) {
            when {
                isReachable(primary) -> primary
                isReachable(secondary) -> secondary
                else -> null
            }
        }

        currentUrl = candidate
        isLoading = false
        error = candidate == null
    }

    LaunchedEffect(type) {
        loadUrl()
    }

    DisposableEffect(activity) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webViewRef?.canGoBack() == true) {
                    webViewRef?.goBack()
                } else {
                    onShowSheet?.invoke()
                }
            }
        }
        activity.onBackPressedDispatcher.addCallback(callback)
        onDispose { callback.remove() }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color.DarkGray,
                    contentColor = Color.White
                )
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                coroutineScope.launch {
                    loadUrl()
                    isRefreshing = false
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                isLoading -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                error -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.no_connection))
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(stringResource(R.string.pull_down_to_refresh))
                }
                else -> currentUrl?.let { url ->
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            val swipeRefreshLayout = SwipeRefreshLayout(ctx)
                            val cookieManager = CookieManager.getInstance()

                            val webView = WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                settings.setSupportZoom(true)

                                cookieManager.setAcceptCookie(true)
                                cookieManager.setAcceptThirdPartyCookies(this, true)

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        swipeRefreshLayout.isRefreshing = false
                                        isRefreshing = false
                                    }
                                }

                                loadUrl(url)
                            }

                            webViewRef = webView

                            swipeRefreshLayout.setOnChildScrollUpCallback { _, _ ->
                                webView.scrollY > 0
                            }

                            swipeRefreshLayout.setOnRefreshListener {
                                webView.reload()
                            }

                            swipeRefreshLayout.addView(webView)
                            swipeRefreshLayout
                        }
                    )
                }
            }
        }
    }
}
