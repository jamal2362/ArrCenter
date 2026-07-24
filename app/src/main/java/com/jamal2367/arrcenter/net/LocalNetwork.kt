package com.jamal2367.arrcenter.net

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Runtime permission introduced by Local Network Protection.
 *
 * From Android 17 on, reaching a device on the local network requires this permission.
 * Without it both the reachability probe and the WebView fail, the latter reporting
 * `net::ERR_LOCAL_NETWORK_PERMISSION_MISSING` - exactly what a self-hosted setup on a LAN
 * address runs into.
 *
 * Referenced as a string rather than through `Manifest.permission` so the app still builds
 * against SDKs that do not define the constant yet.
 */
const val ACCESS_LOCAL_NETWORK: String = "android.permission.ACCESS_LOCAL_NETWORK"

/** `true` when this device knows the permission and it has not been granted yet. */
fun needsLocalNetworkPermission(context: Context): Boolean =
    isLocalNetworkPermissionKnown(context) &&
        ContextCompat.checkSelfPermission(context, ACCESS_LOCAL_NETWORK) !=
        PackageManager.PERMISSION_GRANTED

/**
 * Asks the platform whether the permission exists instead of comparing SDK levels.
 *
 * Requesting a permission the device does not define is silently denied, which would leave
 * the app looking broken with no dialog ever appearing.
 */
private fun isLocalNetworkPermissionKnown(context: Context): Boolean = try {
    context.packageManager.getPermissionInfo(ACCESS_LOCAL_NETWORK, 0)
    true
} catch (_: PackageManager.NameNotFoundException) {
    false
}
