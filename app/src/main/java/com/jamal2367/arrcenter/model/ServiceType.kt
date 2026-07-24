package com.jamal2367.arrcenter.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.jamal2367.arrcenter.R

/**
 * Single source of truth for every service the app can show.
 *
 * Everything that used to be spread over `when (currentRoute)` blocks, hard coded colour
 * tables and duplicated DataStore keys lives here, so adding a service means adding one
 * entry to this enum.
 *
 * @param id stable identifier - also the prefix of the DataStore keys, so the values
 *           written by earlier versions of the app keep working.
 * @param chromeColor background painted behind the web content. It matches the background
 *           of the actual web app so there is no white flash while a page loads.
 * @param brandColor accent used for the service in the navigation and in the settings.
 * @param requestsDesktopSite whether the service is only usable with a desktop user agent.
 */
enum class ServiceType(
    val id: String,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
    val brandColor: Color,
    val chromeColor: Color,
    val requestsDesktopSite: Boolean = false,
) {
    Seerr(
        id = "seerr",
        labelRes = R.string.seerr,
        iconRes = R.drawable.ic_seerr_24dp,
        brandColor = Color(0xFF6366F1),
        chromeColor = Color(0xFF111827),
    ),
    Radarr(
        id = "radarr",
        labelRes = R.string.radarr,
        iconRes = R.drawable.ic_radarr_24dp,
        brandColor = Color(0xFFFFC230),
        chromeColor = Color(0xFF202020),
    ),
    Sonarr(
        id = "sonarr",
        labelRes = R.string.sonarr,
        iconRes = R.drawable.ic_sonarr_24dp,
        brandColor = Color(0xFF35C5F4),
        chromeColor = Color(0xFF202020),
    ),
    SABnzbd(
        id = "sabnzbd",
        labelRes = R.string.sabnzbd,
        iconRes = R.drawable.ic_sabnzbd_24dp,
        brandColor = Color(0xFFFFCC33),
        chromeColor = Color(0xFF000000),
        requestsDesktopSite = true,
    ),
    Uvs(
        id = "uvs",
        labelRes = R.string.uvs,
        iconRes = R.drawable.ic_uvs_24dp,
        brandColor = Color(0xFF4EA1D3),
        chromeColor = Color(0xFF0A0C12),
    );

    companion object {
        fun fromId(id: String?): ServiceType? = entries.firstOrNull { it.id == id }
    }
}
