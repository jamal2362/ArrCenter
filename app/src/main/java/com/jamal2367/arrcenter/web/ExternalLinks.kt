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
 * Hands a link to another app.
 *
 * YouTube links prefer the YouTube app; everything else goes to whatever can handle the
 * scheme. Links the WebView cannot render itself (`mailto:`, `magnet:`, `tel:`, …) used to
 * end up on an `ERR_UNKNOWN_URL_SCHEME` error page.
 *
 * @return `false` when no app on the device can open [uri].
 */
fun openExternally(context: Context, uri: Uri): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (uri.isYouTube()) intent.setPackage(YOUTUBE_PACKAGE)

    try {
        context.startActivity(intent)
        return true
    } catch (_: ActivityNotFoundException) {
        // The preferred app is not installed - retry without the package restriction.
    }

    if (intent.`package` == null) return false
    intent.setPackage(null)
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
