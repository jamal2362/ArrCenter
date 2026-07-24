package com.jamal2367.arrcenter.web

import android.net.Uri
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jamal2367.arrcenter.model.ServiceType

/**
 * Observable state of the WebView belonging to one service.
 *
 * The WebView itself is kept alive while the app is running, so switching services keeps
 * scroll position, history and login session instead of reloading from scratch every time.
 */
@Stable
class ServiceWebController(val type: ServiceType) {

    var webView: WebView? = null
        private set

    var progress by mutableFloatStateOf(0f)
    var isLoading by mutableStateOf(false)
    var canGoBack by mutableStateOf(false)
    var loadError by mutableStateOf<String?>(null)

    /** The URL currently held by the WebView - used to avoid pointless reloads. */
    var loadedUrl: String? = null
        private set

    fun attach(view: WebView) {
        webView = view
    }

    fun load(url: String) {
        loadedUrl = url
        loadError = null
        webView?.loadUrl(url)
    }

    /** Loads [url] only when it differs from what is already displayed. */
    fun loadIfNeeded(url: String) {
        if (loadedUrl != url) load(url)
    }

    /** Forgets what is loaded so the next [loadIfNeeded] really navigates again. */
    fun forget() {
        loadedUrl = null
        loadError = null
    }

    fun reload() {
        loadError = null
        val view = webView ?: return
        val url = loadedUrl
        if (url != null && view.url == null) view.loadUrl(url) else view.reload()
    }

    fun goBack(): Boolean {
        val view = webView ?: return false
        if (!view.canGoBack()) return false
        view.goBack()
        return true
    }

    fun destroy() {
        webView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
            view.stopLoading()
            view.webChromeClient = null
            view.loadUrl("about:blank")
            view.removeAllViews()
            view.destroy()
        }
        webView = null
        loadedUrl = null
    }
}

/**
 * Owns one [ServiceWebController] per visited service.
 *
 * Created with `remember` inside the composition and torn down in a `DisposableEffect`, so
 * the WebViews never outlive the activity that created them - the old code kept a WebView
 * reference in composable state and never destroyed it.
 */
@Stable
class WebViewHost {

    private val controllers = linkedMapOf<ServiceType, ServiceWebController>()

    /** Set while a page is waiting for a file picker result. */
    var pendingFileCallback: ValueCallback<Array<Uri>>? = null

    /**
     * Opens the system file picker. Installed once by the root composable so a WebView that
     * survives several screens never calls into an unregistered launcher.
     */
    var requestFileChooser: ((mimeTypes: Array<String>, allowMultiple: Boolean) -> Unit)? = null

    fun controllerFor(type: ServiceType): ServiceWebController =
        controllers.getOrPut(type) { ServiceWebController(type) }

    fun deliverFileChooserResult(uris: Array<Uri>) {
        pendingFileCallback?.onReceiveValue(uris)
        pendingFileCallback = null
    }

    fun destroyAll() {
        pendingFileCallback?.onReceiveValue(emptyArray())
        pendingFileCallback = null
        controllers.values.forEach { it.destroy() }
        controllers.clear()
    }
}
