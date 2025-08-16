package com.jamal2367.arrcenter.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.jamal2367.arrcenter.R
import com.jamal2367.arrcenter.ui.ServiceType
import com.jamal2367.arrcenter.data.SettingsKeys
import com.jamal2367.arrcenter.data.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ServiceScreen(type: ServiceType) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var currentUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    suspend fun loadUrl() {
        isLoading = true
        error = false
        currentUrl = null

        val prefs = context.dataStore.data.first()
        val (primary, secondary) = when (type) {
            ServiceType.Jellyseerr -> prefs[SettingsKeys.JELLY_PRIMARY] to prefs[SettingsKeys.JELLY_SECONDARY]
            ServiceType.Radarr -> prefs[SettingsKeys.RADARR_PRIMARY] to prefs[SettingsKeys.RADARR_SECONDARY]
            ServiceType.Sonarr -> prefs[SettingsKeys.SONARR_PRIMARY] to prefs[SettingsKeys.SONARR_SECONDARY]
        }

        val p = primary ?: defaultPrimary()
        val s = secondary ?: defaultSecondary()

        val candidate = withContext(Dispatchers.IO) {
            when {
                isReachable(p) -> p
                isReachable(s) -> s
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

    var isRefreshing by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            webViewRef?.reload()
        },
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            error -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { Text(stringResource(R.string.no_connection), color = MaterialTheme.colorScheme.onSurface) }

            else -> currentUrl?.let { url ->
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val swipeRefreshLayout = SwipeRefreshLayout(ctx)

                        val webView = WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true

                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    swipeRefreshLayout.isRefreshing = false
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

private fun defaultPrimary(): String? = null

private fun defaultSecondary(): String? = null

private fun isReachable(url: String?): Boolean {
    return try {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 250
        conn.readTimeout = 250
        conn.requestMethod = "GET"
        conn.connect()
        val code = conn.responseCode
        code in 200..399
    } catch (_: Exception) {
        false
    }
}

