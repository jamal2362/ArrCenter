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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.jamal2367.arrcenter.R
import com.jamal2367.arrcenter.data.SettingsKeys
import com.jamal2367.arrcenter.data.dataStore
import com.jamal2367.arrcenter.helper.ServiceType
import com.jamal2367.arrcenter.helper.injectCSS
import com.jamal2367.arrcenter.helper.isDesktopMode
import com.jamal2367.arrcenter.helper.isJS
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
    val coroutineScope = rememberCoroutineScope()
    var currentUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

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
            ServiceType.Ugreen -> prefs[SettingsKeys.UGREEN_PRIMARY] to prefs[SettingsKeys.UGREEN_SECONDARY]
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

    Scaffold { innerPadding ->
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

                                if (type == ServiceType.SABnzbd || type == ServiceType.Ugreen) {
                                    settings.useWideViewPort = true
                                    settings.loadWithOverviewMode = true
                                    settings.userAgentString = isDesktopMode()
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        swipeRefreshLayout.isRefreshing = false
                                        isRefreshing = false

                                        view?.let {
                                            injectCSS(it)
                                        }

                                        if (type == ServiceType.SABnzbd || type == ServiceType.Ugreen) {
                                            view?.evaluateJavascript(
                                                isJS(),
                                                null
                                            )
                                        }
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
