package com.jamal2367.arrcenter.web

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.jamal2367.arrcenter.R

/**
 * Colours the in-app browser is asked to use.
 *
 * Filled from the Compose theme by the root composable, so a Custom Tab shows up in the
 * app's own (dynamic) colours and in the same light/dark appearance instead of whatever the
 * browser happens to default to.
 *
 * @param darkTheme whether the app currently renders dark - the Custom Tab is pinned to the
 *        same appearance, which also tells us how to tint the close button.
 */
data class CustomTabAppearance(
    val darkTheme: Boolean,
    val toolbarColor: Int,
    val iconColor: Int,
    val navigationBarColor: Int,
)

/** `true` for links an in-app browser is able to show. */
fun Uri.isWebLink(): Boolean = when (scheme?.lowercase()) {
    "http", "https" -> true
    else -> false
}

/**
 * Opens [uri] in an in-app browser (Custom Tab).
 *
 * The tab is launched from the hosting activity whenever one can be found, so it becomes
 * part of this app's task: the close button in the top left of the tab brings the user
 * straight back to ArrCenter instead of dropping them on the home screen.
 *
 * @return `false` when [uri] is not a web link or no installed browser implements Custom
 *         Tabs - the caller then falls back to the system default browser.
 */
fun openInCustomTab(context: Context, uri: Uri, appearance: CustomTabAppearance?): Boolean {
    if (!uri.isWebLink()) return false

    // Picks the user's default browser when it supports Custom Tabs, and any other capable
    // browser otherwise. Null means nothing on the device can do it.
    val browser = CustomTabsClient.getPackageName(context, null) ?: return false

    val activity = context.findActivity()
    val customTabsIntent = buildCustomTabsIntent(context, appearance)
    customTabsIntent.intent.setPackage(browser)
    // Only needed when the link was handed over from a non-activity context; a tab started
    // in its own task cannot be part of the app's back stack.
    if (activity == null) customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    return runCatching { customTabsIntent.launchUrl(activity ?: context, uri) }.isSuccess
}

private fun buildCustomTabsIntent(
    context: Context,
    appearance: CustomTabAppearance?,
): CustomTabsIntent {
    val builder = CustomTabsIntent.Builder(CustomTabsConnector.session)
        .setShowTitle(true)
        // Toolbar hiding on scroll stays off deliberately: the way back into the app must be
        // on screen at all times, not only after scrolling up again.
        .setUrlBarHidingEnabled(false)
        .setShareState(CustomTabsIntent.SHARE_STATE_ON)
        .setCloseButtonPosition(CustomTabsIntent.CLOSE_BUTTON_POSITION_START)

    if (appearance != null) {
        builder.setColorScheme(
            if (appearance.darkTheme) {
                CustomTabsIntent.COLOR_SCHEME_DARK
            } else {
                CustomTabsIntent.COLOR_SCHEME_LIGHT
            },
        )
        builder.setDefaultColorSchemeParams(
            CustomTabColorSchemeParams.Builder()
                .setToolbarColor(appearance.toolbarColor)
                .setSecondaryToolbarColor(appearance.toolbarColor)
                .setNavigationBarColor(appearance.navigationBarColor)
                .setNavigationBarDividerColor(appearance.navigationBarColor)
                .build(),
        )
    }

    // A back arrow rather than the default X: the tab is a detour inside the app, not a
    // window that gets dismissed. Only replaced when the tint is known - an untinted icon
    // could end up invisible on the browser's own toolbar colour.
    appearance?.iconColor
        ?.let { closeButtonIcon(context, it) }
        ?.let(builder::setCloseButtonIcon)

    return builder.build()
}

private fun closeButtonIcon(context: Context, tint: Int): Bitmap? {
    val icon = ContextCompat.getDrawable(context, R.drawable.ic_arrow_back_24dp) ?: return null
    return runCatching { icon.mutate().apply { setTint(tint) }.toBitmap() }.getOrNull()
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Keeps a connection to the Custom Tabs provider of the device.
 *
 * Recommended by the platform documentation: the browser gets to warm up its process while
 * the app is in the foreground, so the first external link opens without the delay of a cold
 * browser start. Everything here degrades gracefully - if no provider exists, or binding
 * fails, [session] simply stays `null` and the tab is launched without one.
 */
object CustomTabsConnector {

    private var connection: CustomTabsServiceConnection? = null

    var session: CustomTabsSession? = null
        private set

    /** Binds to the browser. Called from `onStart`; a second call while bound does nothing. */
    fun bind(context: Context) {
        if (connection != null) return
        val browser = CustomTabsClient.getPackageName(context, null) ?: return

        val serviceConnection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(
                name: ComponentName,
                client: CustomTabsClient,
            ) {
                runCatching { client.warmup(0L) }
                session = runCatching { client.newSession(null) }.getOrNull()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                session = null
            }
        }

        connection = serviceConnection
        val bound = runCatching {
            CustomTabsClient.bindCustomTabsService(context, browser, serviceConnection)
        }.getOrDefault(false)

        if (!bound) connection = null
    }

    /** Releases the browser again. Must be called with the same context that bound it. */
    fun unbind(context: Context) {
        val active = connection ?: return
        connection = null
        session = null
        runCatching { context.unbindService(active) }
    }
}
