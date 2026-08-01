package com.jamal2367.arrcenter.web

import com.jamal2367.arrcenter.model.ServiceType

/**
 * The cosmetic patches applied to each web app.
 *
 * These used to be one big blob applied to every service, which meant Radarr got SABnzbd's
 * Bootstrap rules and vice versa. They are now scoped per service and injected as a single
 * script that runs at document start, so the page no longer flashes unstyled first.
 */
const val DESKTOP_USER_AGENT: String =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36"

/** The full script injected for [type], or `null` when the service needs no patching. */
fun injectionScriptFor(type: ServiceType): String? {
    val css = cssFor(type)
    val body = extraScriptFor(type)
    if (css == null && body == null) return null

    return buildString {
        append("(function(){\n")
        if (css != null) {
            append("var css = ").append(css.toJsStringLiteral()).append(";\n")
            append(
                """
                function applyStyle() {
                  if (document.getElementById('$STYLE_ELEMENT_ID')) return;
                  var root = document.head || document.documentElement;
                  if (!root) return;
                  var el = document.createElement('style');
                  el.id = '$STYLE_ELEMENT_ID';
                  el.textContent = css;
                  root.appendChild(el);
                }
                applyStyle();
                document.addEventListener('DOMContentLoaded', applyStyle, { once: true });

                """.trimIndent()
            )
            append('\n')
        }
        if (body != null) {
            append(
                """
                function onReady(fn) {
                  if (document.readyState === 'loading') {
                    document.addEventListener('DOMContentLoaded', fn, { once: true });
                  } else {
                    fn();
                  }
                }

                """.trimIndent()
            )
            append('\n')
            append(body)
            append('\n')
        }
        append("})();")
    }
}

private fun cssFor(type: ServiceType): String? = when (type) {
    ServiceType.Seerr -> """
        .bg-gray-800\/90 { background-color: #111827 !important; }
        div.top-0:nth-child(2) { --tw-gradient-from: unset; }
        .searchbar { background-color: #111827; }
    """.trimIndent()

    ServiceType.Radarr, ServiceType.Sonarr -> """
        [class*="PageHeader-header-"] { background-color: #202020 !important; }
        [class*="PageToolbar-toolbar-"] { background-color: #202020 !important; }
        [class*="PageSidebar-sidebar-"] { background-color: #202020 !important; }
        [class*="MovieDetails-contentContainer-"] { padding: 20px !important; }
        [class*="SeriesDetails-contentContainer-"] { padding: 20px !important; }
    """.trimIndent()

    ServiceType.SABnzbd -> """
        .container-full-width .container { width: 98%; }
        .navbar-toggle { margin-right: 0px; }
        .navbar-logo { margin-left: 0px; }
        .navbar-nav .open .dropdown-menu { float: right; }
    """.trimIndent()

    ServiceType.Uvs -> null
}

private fun extraScriptFor(type: ServiceType): String? = when (type) {
    // SABnzbd's Bootstrap navbar starts collapsed on narrow screens. Keep it expanded but
    // let a tap outside close it again.
    ServiceType.SABnzbd -> """
        onReady(function () {
          var collapse = document.querySelector('.collapse');
          if (!collapse) return;
          collapse.classList.add('show');
          document.addEventListener('click', function (e) {
            if (!collapse.contains(e.target)) collapse.classList.remove('show');
          }, true);
        });
    """.trimIndent()

    else -> null
}

/** Escapes a Kotlin string so it can be embedded as a JavaScript string literal. */
private fun String.toJsStringLiteral(): String = buildString {
    append('"')
    this@toJsStringLiteral.forEach { c ->
        when (c) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            '<' -> append("\\u003C") // avoid breaking out of an inline <script> context
            else -> append(c)
        }
    }
    append('"')
}

private const val STYLE_ELEMENT_ID = "arrcenter-style"
