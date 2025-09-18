package com.jamal2367.arrcenter.helper

import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jamal2367.arrcenter.R
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun SheetItem(
    onClick: (String) -> Unit
) {
    val items = listOf(
        Triple("jellyseerr", R.drawable.ic_jellyseerr_24dp, "Jellyseerr"),
        Triple("radarr", R.drawable.ic_radarr_24dp, "Radarr"),
        Triple("sonarr", R.drawable.ic_sonarr_24dp, "Sonarr"),
        Triple("sabnzbd", R.drawable.ic_sabnzbd_24dp, "SABnzbd"),
        Triple("ugreen", R.drawable.ic_storage_24dp, "Ugreen"),
        Triple("settings", null, "Settings")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowItems.forEach { (route, iconRes, _) ->
                    Surface(
                        onClick = { onClick(route) },
                        modifier = Modifier
                            .weight(1f)
                            .size(80.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (iconRes != null) {
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = route,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(36.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = route,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
                if (rowItems.size < 2) {
                    repeat(2 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

fun isReachable(url: String?): Boolean {
    return try {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 500
        conn.readTimeout = 500
        conn.requestMethod = "GET"
        conn.connect()
        val code = conn.responseCode
        code in 200..399
    } catch (_: Exception) {
        false
    }
}

fun injectCSS(webView: WebView) {
    val cssInjection = """
        javascript:(function() {
            var style = document.createElement('style');
            style.innerHTML = `
                .padding-bottom-safe {
                  background-color: #111827;
                }
                
                div.top-0:nth-child(2) {
                  --tw-gradient-from: unset;
                }
                
                .searchbar {
                  background-color: #111827;
                }
                
                [class*="wallpaper-wrapper"] {
                  background-color: #07011D !important;
                  background-image: unset !important;
                }
                
                [class*="bg-top"] {
                  background: unset !important;
                }
                
                [class*="PageHeader-header-"] {
                  background-color: #202020 !important;
                }

                [class*="PageToolbar-toolbar-"] {
                  background-color: #202020 !important;
                }

                [class*="PageSidebar-sidebar-"] {
                  background-color: #202020 !important;
                }
                
                [class*="MovieDetails-contentContainer-"] {
                  padding: 20px !important;
                }
            `;
            document.head.appendChild(style);
        })();
    """.trimIndent()

    webView.evaluateJavascript(cssInjection, null)
}

fun isDesktopMode(): String {
    return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36"
}

fun isJS(): String {
    return "document.querySelector('meta[name=viewport]')?.setAttribute('content', 'width=1024');"
}

enum class ServiceType {
    Jellyseerr,
    Radarr,
    Sonarr,
    SABnzbd,
    Ugreen
}
