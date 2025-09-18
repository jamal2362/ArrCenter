package com.jamal2367.arrcenter.helper

import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient

class WebChromeClientWithCallback(
    private val launchFileChooser: (Array<String>) -> Unit
) : WebChromeClient() {

    var filePathCallback: ValueCallback<Array<Uri>>? = null

    override fun onShowFileChooser(
        webView: android.webkit.WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        this.filePathCallback?.onReceiveValue(null)
        this.filePathCallback = filePathCallback
        launchFileChooser(arrayOf("*/*"))
        return true
    }
}
