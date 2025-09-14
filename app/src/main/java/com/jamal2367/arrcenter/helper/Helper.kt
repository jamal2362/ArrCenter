package com.jamal2367.arrcenter.helper

import java.net.HttpURLConnection
import java.net.URL

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