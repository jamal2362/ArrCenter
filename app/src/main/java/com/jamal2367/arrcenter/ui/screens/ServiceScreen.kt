package com.jamal2367.arrcenter.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
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
import androidx.compose.runtime.*
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
import com.jamal2367.arrcenter.model.ServiceType
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
    var isError by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

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
        isError = false
        currentUrl = null

        val prefs = context.dataStore.data.first()

        val (primary, secondary) = when (type) {
            ServiceType.Jellyseerr -> prefs[SettingsKeys.JELLY_PRIMARY] to prefs[SettingsKeys.JELLY_SECONDARY]
            ServiceType.Radarr -> prefs[SettingsKeys.RADARR_PRIMARY] to prefs[SettingsKeys.RADARR_SECONDARY]
            ServiceType.Sonarr -> prefs[SettingsKeys.SONARR_PRIMARY] to prefs[SettingsKeys.SONARR_SECONDARY]
            ServiceType.SABnzbd -> prefs[SettingsKeys.SABNZBD_PRIMARY] to prefs[SettingsKeys.SABNZBD_SECONDARY]
            ServiceType.Uvs -> prefs[SettingsKeys.UVS_PRIMARY] to prefs[SettingsKeys.UVS_SECONDARY]
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
        isError = candidate == null
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
            isError -> Column(
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
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.no_connection),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(36.dp))
                Button(
                    onClick = { coroutineScope.launch {
                        loadUrl()
                    } },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer)
                ) {
                    Text(text = stringResource(R.string.retry))
                }
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
                            settings.allowFileAccess = true
                            settings.allowContentAccess = true
                            settings.setSupportZoom(true)

                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            if (type == ServiceType.SABnzbd) {
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                settings.userAgentString = isDesktopMode()
                            }

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest): Boolean {
                                    val clickedUrl = request.url.toString()

                                    if (clickedUrl.contains("youtube.com") || clickedUrl.contains("youtu.be")) {
                                        val intent = Intent(Intent.ACTION_VIEW, clickedUrl.toUri())
                                        intent.setPackage("com.google.android.youtube")
                                        if (intent.resolveActivity(ctx.packageManager) == null) intent.setPackage(null)
                                        ctx.startActivity(intent)
                                        return true
                                    }
                                    return false
                                }


                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    swipeRefreshLayout.isRefreshing = false

                                    view?.let {
                                        injectCSS(it)
                                    }

                                    if (type == ServiceType.SABnzbd || type == ServiceType.Uvs) {
                                        view?.evaluateJavascript(isJS(), null)
                                    }
                                }

                                override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                                    super.onReceivedError(view, request, error)
                                    swipeRefreshLayout.isRefreshing = false
                                }
                            }

                            webChromeClient = WebChromeClientWithCallback { mimeTypes ->
                                fileChooserLauncher.launch(mimeTypes)
                            }

                            loadUrl(url)
                        }

                        webViewRef = webView

                        swipeRefreshLayout.setOnRefreshListener {
                            webView.reload()
                            swipeRefreshLayout.isRefreshing = true
                        }

                        swipeRefreshLayout.isEnabled = !(context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && (type == ServiceType.Radarr || type == ServiceType.Sonarr))

                        swipeRefreshLayout.addView(webView)
                        swipeRefreshLayout
                    }
                )
            }
        }
    }
}
