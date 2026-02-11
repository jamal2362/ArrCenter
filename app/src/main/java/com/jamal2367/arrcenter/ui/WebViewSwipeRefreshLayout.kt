package com.jamal2367.arrcenter.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.webkit.WebView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class WebViewSwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwipeRefreshLayout(context, attrs) {

    var webView: WebView? = null
    private var canChildScrollUpValue = false

    @SuppressLint("SetJavaScriptEnabled")
    fun updateScrollPosition() {
        webView?.evaluateJavascript(
            """
            (function() {
                var scrollTop = document.scrollingElement ? document.scrollingElement.scrollTop : 0;
                if (scrollTop > 0) return true;
                if (window.scrollY > 0 || window.pageYOffset > 0) return true;
                var elements = document.querySelectorAll('*');
                for (var i = 0; i < elements.length; i++) {
                    var el = elements[i];
                    if (el.scrollTop > 0 && el.scrollHeight > el.clientHeight) {
                        return true;
                    }
                }
                return false;
            })();
            """.trimIndent()
        ) { result ->
            canChildScrollUpValue = result == "true"
        }
    }

    override fun canChildScrollUp(): Boolean {
        val wv = webView
        if (wv != null && wv.scrollY > 0) return true
        return canChildScrollUpValue
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            updateScrollPosition()
        }
        return super.onInterceptTouchEvent(ev)
    }
}
