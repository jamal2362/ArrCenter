package com.jamal2367.arrcenter.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.jamal2367.arrcenter.R
import com.jamal2367.arrcenter.data.SettingsKeys
import com.jamal2367.arrcenter.data.dataStore
import com.jamal2367.arrcenter.helper.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ServiceScreen(type: ServiceType, backgroundColor: Color, onShowSheet: (() -> Unit)? = null) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val coroutineScope = rememberCoroutineScope()

    var currentUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            val callback = (webViewRef?.webChromeClient as? WebChromeClientWithCallback)?.filePathCallback
            callback?.onReceiveValue(uri?.let { arrayOf(it) } ?: emptyArray())
            (webViewRef?.webChromeClient as? WebChromeClientWithCallback)?.filePathCallback = null
        }
    )

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
                        .background(backgroundColor)
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                error -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor)
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.no_connection),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.refresh),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                loadUrl()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimaryContainer)
                    ) {
                        Text(
                            text = stringResource(R.string.retry)
                        )
                    }
                } else -> currentUrl?.let { url ->
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
                                settings.allowFileAccess = true
                                settings.allowContentAccess = true
                                settings.setSupportZoom(true)

                                cookieManager.setAcceptCookie(true)
                                cookieManager.setAcceptThirdPartyCookies(this, true)

                                if (type == ServiceType.SABnzbd || type == ServiceType.Ugreen) {
                                    settings.useWideViewPort = true
                                    settings.loadWithOverviewMode = true
                                    settings.userAgentString = isDesktopMode()
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest): Boolean {
                                        val clickedUrl = request.url.toString()

                                        if (clickedUrl.contains("youtube.com") || clickedUrl.contains("youtu.be")) {
                                            val intent =
                                                Intent(Intent.ACTION_VIEW, clickedUrl.toUri())
                                            intent.setPackage("com.google.android.youtube")

                                            if (intent.resolveActivity(ctx.packageManager) == null) {
                                                intent.setPackage(null)
                                            }

                                            ctx.startActivity(intent)
                                            return true
                                        }

                                        return false
                                    }


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

                                webChromeClient = WebChromeClientWithCallback { mimeTypes ->
                                    fileChooserLauncher.launch(mimeTypes)
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
