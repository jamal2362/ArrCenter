package com.jamal2367.arrcenter.web

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import androidx.core.net.toUri

private const val YOUTUBE_PACKAGE = "com.google.android.youtube"

fun Uri.isYouTube(): Boolean {
    val h = host?.lowercase().orEmpty()
    return h == "youtu.be" || h == "youtube.com" || h.endsWith(".youtube.com")
}

/**
 * Opens a link outside of the app's own WebView.
 *
 * Three steps, in this order:
 *  1. YouTube links prefer the YouTube app, which handles them far better than any browser.
 *  2. Web links go into an in-app browser (Custom Tab). The page is *not* rendered in a
 *     WebView of this app, and the user returns with the button in the top left of the tab.
 *  3. Everything else - `mailto:`, `magnet:`, `tel:`, `intent:`, … as well as web links on a
 *     device whose browser does not implement Custom Tabs - is handed to the app registered
 *     for the scheme, which for a web link is the system's default browser.
 *
 * @return `false` when no app on the device can open [uri].
 */
fun openExternalLink(
    context: Context,
    uri: Uri,
    appearance: CustomTabAppearance? = null,
): Boolean {
    if (uri.isYouTube() && startViewIntent(context, uri, YOUTUBE_PACKAGE)) return true
    if (openInCustomTab(context, uri, appearance)) return true
    return startViewIntent(context, uri, packageName = null)
}

/**
 * Fires a plain `ACTION_VIEW`, optionally restricted to one app.
 *
 * @return `false` when [packageName] is not installed, or nothing at all handles the scheme.
 */
private fun startViewIntent(context: Context, uri: Uri, packageName: String?): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, uri)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .setPackage(packageName)

    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

/**
 * Queues a download with the system DownloadManager, forwarding the WebView's cookies so
 * downloads from services behind a login work.
 *
 * @return `false` when the URL cannot be downloaded that way (`blob:`, `data:`, …).
 */
fun startDownload(
    context: Context,
    url: String,
    userAgent: String?,
    contentDisposition: String?,
    mimeType: String?,
): Boolean = try {
    val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
    val request = DownloadManager.Request(url.toUri()).apply {
        setMimeType(mimeType)
        setTitle(fileName)
        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        userAgent?.let { addRequestHeader("User-Agent", it) }
        CookieManager.getInstance().getCookie(url)?.let { addRequestHeader("Cookie", it) }
    }
    context.getSystemService(DownloadManager::class.java).enqueue(request)
    true
} catch (_: Exception) {
    false
}
