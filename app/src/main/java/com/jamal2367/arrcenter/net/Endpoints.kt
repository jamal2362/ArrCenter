package com.jamal2367.arrcenter.net

import com.jamal2367.arrcenter.data.ServiceEndpoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/** Outcome of probing a single address. */
sealed interface ProbeResult {
    data object Ok : ProbeResult

    /** [reason] is the underlying exception, kept so the UI can say *why* it failed. */
    data class Failed(val reason: String) : ProbeResult
}

/** Result of picking a working address for a service. */
sealed interface EndpointResult {
    /** [url] answered and can be handed to the WebView. */
    data class Reachable(val url: String, val usedFallback: Boolean) : EndpointResult

    /** Neither address is filled in. */
    data object NotConfigured : EndpointResult

    /**
     * Addresses exist but nothing answered.
     *
     * @param reason why the probe failed, shown to the user.
     */
    data class Unreachable(val reason: String?) : EndpointResult
}

/**
 * Turns user input into something [URL] can open.
 *
 * Users type `192.168.1.10:7878`, not `http://192.168.1.10:7878`. The previous version fed
 * such input straight into [URL], which threw and made the service look permanently offline.
 *
 * @return the normalised URL, or `null` if the input is blank or cannot be a URL at all.
 */
fun normalizeUrl(raw: String?): String? {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isEmpty()) return null

    val candidate = if (trimmed.matches(SCHEME_PREFIX)) trimmed else "http://$trimmed"

    return try {
        val uri = URI(candidate)
        if (uri.host.isNullOrBlank()) null else candidate
    } catch (_: Exception) {
        null
    }
}

/** `true` when the text is either empty or a usable address - used for inline validation. */
fun isAcceptableUrlInput(raw: String): Boolean =
    raw.isBlank() || normalizeUrl(raw) != null

/**
 * Probes a single address.
 *
 * Any HTTP status code counts as reachable: Radarr, Sonarr and Seerr answer `401`/`403`
 * behind a login, and the previous `code in 200..399` check reported those servers as
 * offline even though they were perfectly fine.
 */
suspend fun probeEndpoint(url: String, timeoutMs: Int = DEFAULT_TIMEOUT_MS): ProbeResult =
    withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                requestMethod = "GET"
                instanceFollowRedirects = true
                useCaches = false
                // Ask for a single byte - we only care that something answers, not about the body.
                setRequestProperty("Range", "bytes=0-0")
                setRequestProperty("Accept-Encoding", "identity")
            }
            if (connection.responseCode > 0) ProbeResult.Ok else ProbeResult.Failed("no response")
        } catch (e: Exception) {
            ProbeResult.Failed(describe(e))
        } finally {
            runCatching { connection?.disconnect() }
        }
    }

/** Convenience wrapper for the connection test in the settings. */
suspend fun isReachable(url: String, timeoutMs: Int = DEFAULT_TIMEOUT_MS): Boolean =
    probeEndpoint(url, timeoutMs) is ProbeResult.Ok

/**
 * Picks the address to load for a service.
 *
 * Probing happens strictly one after another. Running both concurrently inside a
 * `coroutineScope` looks faster but is not: the scope only returns once *every* child has
 * finished, so a working primary address still had to wait out the full timeout of an
 * unreachable fallback before the page was allowed to load.
 */
suspend fun resolveEndpoint(
    endpoints: ServiceEndpoints,
    timeoutMs: Int = DEFAULT_TIMEOUT_MS,
): EndpointResult {
    val primary = normalizeUrl(endpoints.primary)
    val secondary = normalizeUrl(endpoints.secondary)

    if (primary == null && secondary == null) return EndpointResult.NotConfigured

    var failure: String? = null

    if (primary != null) {
        // A tighter budget while a fallback exists: a primary that is actually up answers in
        // a fraction of this, and every second spent waiting here delays the fallback.
        val budget = if (secondary != null) minOf(timeoutMs, PRIMARY_TIMEOUT_MS) else timeoutMs
        when (val result = probeEndpoint(primary, budget)) {
            is ProbeResult.Ok -> return EndpointResult.Reachable(primary, usedFallback = false)
            is ProbeResult.Failed -> failure = result.reason
        }
    }

    if (secondary != null) {
        when (val result = probeEndpoint(secondary, timeoutMs)) {
            is ProbeResult.Ok -> return EndpointResult.Reachable(secondary, usedFallback = true)
            is ProbeResult.Failed -> if (failure == null) failure = result.reason
        }
    }

    return EndpointResult.Unreachable(reason = failure)
}

/** Short, human readable form of a network exception, e.g. `UnknownHostException: …`. */
private fun describe(e: Exception): String {
    val name = e.javaClass.simpleName
    val message = e.message?.takeIf { it.isNotBlank() }
    return if (message != null) "$name: $message" else name
}

/** Budget for the last candidate, which may well be a remote address over mobile data. */
const val DEFAULT_TIMEOUT_MS: Int = 5_000

/**
 * Budget for the primary address while a fallback is configured. A server on the local
 * network replies within a few dozen milliseconds, so this only has to be long enough to
 * cover a brief hiccup - anything more just delays the switch to the fallback.
 */
private const val PRIMARY_TIMEOUT_MS: Int = 1_000

private val SCHEME_PREFIX = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://.*", RegexOption.DOT_MATCHES_ALL)
