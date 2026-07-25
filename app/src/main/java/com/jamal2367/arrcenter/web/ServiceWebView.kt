package com.jamal2367.arrcenter.web

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.jamal2367.arrcenter.R
import com.jamal2367.arrcenter.model.ServiceType

/**
 * Builds the WebView for one service.
 *
 * Compared to the previous inline setup this adds: load progress, real error reporting for
 * the main frame, `window.open` support (needed for the Plex sign-in popup used by Seerr),
 * downloads through the system DownloadManager, handling of non-http links and
 * flicker-free style injection.
 *
 * The WebView only ever shows the service itself. Every link that leads somewhere else is
 * opened in an in-app browser (Custom Tab) instead - see [belongsToCurrentService].
 *
 * Everything that outlives a single composition is routed through [host] rather than
 * captured in a lambda, so the WebView can be reused across service switches without ever
 * holding on to a stale activity result launcher.
 */
@SuppressLint("SetJavaScriptEnabled")
fun createServiceWebView(
    context: Context,
    type: ServiceType,
    controller: ServiceWebController,
    host: WebViewHost,
): WebView = WebView(context).apply {
    val appContext = context.applicationContext

    layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
    )
    setBackgroundColor(type.chromeColor.toArgb())
    isVerticalScrollBarEnabled = true
    isHorizontalScrollBarEnabled = false
    overScrollMode = WebView.OVER_SCROLL_NEVER

    with(settings) {
        javaScriptEnabled = true
        domStorageEnabled = true
        builtInZoomControls = true
        displayZoomControls = false
        setSupportZoom(true)
        // The services are remote web apps; they have no business reading local files.
        allowFileAccess = false
        allowContentAccess = false
        setSupportMultipleWindows(true)
        javaScriptCanOpenWindowsAutomatically = true

        if (type.requestsDesktopSite) {
            useWideViewPort = true
            loadWithOverviewMode = true
            userAgentString = DESKTOP_USER_AGENT
        }
    }

    CookieManager.getInstance().let { cookies ->
        cookies.setAcceptCookie(true)
        cookies.setAcceptThirdPartyCookies(this, true)
    }

    val script = injectionScriptFor(type)
    val documentStartSupported = script != null &&
        WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT) &&
        runCatching {
            WebViewCompat.addDocumentStartJavaScript(this, script, setOf("*"))
        }.isSuccess

    webViewClient = object : WebViewClient() {

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest,
        ): Boolean {
            val uri = request.url
            // The activity context keeps a Custom Tab inside this app's task, so its close
            // button returns here. Only used while the WebView is on screen, which is the
            // only moment a link can be tapped.
            val linkContext = view?.context ?: appContext

            // mailto:, magnet:, tel:, intent:, … cannot be rendered by a WebView.
            if (uri.scheme != null && !uri.isWebLink()) {
                handOff(linkContext, uri, host)
                return true
            }

            // Never hijack iframes and never break out of a redirect chain: a service behind
            // a reverse proxy or single sign-on bounces the main frame through other hosts,
            // and those steps have to stay in the WebView to keep the session.
            if (!request.isForMainFrame || request.isRedirect) return false

            if (belongsToCurrentService(view, controller, uri)) return false

            handOff(linkContext, uri, host)
            return true
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            controller.isLoading = true
            controller.progress = 0f
            controller.loadError = null
        }

        override fun onPageCommitVisible(view: WebView?, url: String?) {
            super.onPageCommitVisible(view, url)
            if (!documentStartSupported) injectFallback(view, script)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            controller.isLoading = false
            controller.progress = 1f
            if (!documentStartSupported) injectFallback(view, script)
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?,
        ) {
            super.onReceivedError(view, request, error)
            if (request?.isForMainFrame != true) return
            controller.isLoading = false
            controller.loadError = error?.description?.toString().orEmpty()
        }

        override fun onReceivedSslError(
            view: WebView?,
            handler: SslErrorHandler?,
            error: SslError?,
        ) {
            // Never silently trust a bad certificate - surface it instead.
            handler?.cancel()
            controller.isLoading = false
            controller.loadError = "SSL: ${sslErrorText(error)}"
        }
    }

    webChromeClient = object : WebChromeClient() {

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            controller.progress = newProgress / 100f
            if (newProgress >= 100) controller.isLoading = false
        }

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?,
        ): Boolean {
            val request = host.requestFileChooser ?: return false

            // Release a picker that is somehow still pending, otherwise the page hangs.
            host.pendingFileCallback?.onReceiveValue(emptyArray())
            host.pendingFileCallback = filePathCallback

            val allowMultiple =
                fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE
            request(mimeTypesOf(fileChooserParams), allowMultiple)
            return true
        }

        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?,
        ): Boolean {
            val target = view ?: return false
            val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false

            // A throwaway WebView that only reports where the popup wanted to go. The URL is
            // then opened in the real WebView so the session and OAuth redirect are kept.
            //
            // Popups stay in the WebView on purpose: the only ones the services open are
            // sign-in windows (Plex for Seerr), and those have to share the cookie jar of
            // the page that opened them. A Custom Tab cannot, so it would break the login.
            val proxy = WebView(target.context)
            proxy.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    proxyView: WebView?,
                    request: WebResourceRequest,
                ): Boolean {
                    target.loadUrl(request.url.toString())
                    proxyView?.post { proxyView.destroy() }
                    return true
                }
            }

            transport.webView = proxy
            resultMsg.sendToTarget()
            return true
        }
    }

    setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
        val started = startDownload(appContext, url, userAgent, contentDisposition, mimeType)
        val message = if (started) R.string.download_started else R.string.download_failed
        Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
    }
}.also { controller.attach(it) }

private fun handOff(context: Context, uri: Uri, host: WebViewHost) {
    if (!openExternalLink(context, uri, host.customTabAppearance)) {
        Toast.makeText(context, R.string.no_app_for_link, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Whether [uri] is part of what this WebView is meant to show.
 *
 * The host is compared against the page currently on screen *and* against the resolved
 * service address. The first covers a login flow that sent the WebView to an identity
 * provider - once there, its own links are not external either. The second brings the user
 * back to the service after such a detour, even though the service is no longer the page in
 * the WebView.
 *
 * Ports are deliberately ignored: a service that redirects to another port of the same host
 * is still the same service.
 */
private fun belongsToCurrentService(
    view: WebView?,
    controller: ServiceWebController,
    uri: Uri,
): Boolean {
    // A URL without a host is nothing a browser could open either - leave it to the WebView.
    val target = uri.host?.lowercase() ?: return true
    return target == hostOf(view?.url) || target == hostOf(controller.loadedUrl)
}

private fun hostOf(url: String?): String? =
    url?.takeIf { it.isNotBlank() }?.toUri()?.host?.lowercase()

/**
 * `accept` attributes may contain file extensions (`.nzb`) which the document picker cannot
 * use - only real MIME types are forwarded.
 */
private fun mimeTypesOf(params: WebChromeClient.FileChooserParams?): Array<String> =
    params?.acceptTypes
        ?.filter { it.contains('/') }
        ?.takeIf { it.isNotEmpty() }
        ?.toTypedArray()
        ?: arrayOf("*/*")

private fun injectFallback(view: WebView?, script: String?) {
    if (view == null || script == null) return
    view.evaluateJavascript(script, null)
}

private fun sslErrorText(error: SslError?): String = when (error?.primaryError) {
    SslError.SSL_DATE_INVALID -> "certificate date invalid"
    SslError.SSL_EXPIRED -> "certificate expired"
    SslError.SSL_IDMISMATCH -> "hostname mismatch"
    SslError.SSL_INVALID -> "invalid certificate"
    SslError.SSL_NOTYETVALID -> "certificate not yet valid"
    SslError.SSL_UNTRUSTED -> "certificate not trusted"
    else -> "unknown error"
}
