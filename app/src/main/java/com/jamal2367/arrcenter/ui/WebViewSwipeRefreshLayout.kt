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
    private var initialY = 0f
    private var hasDeterminedDirection = false
    private var originalEnabledState = true

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
        val wv = webView ?: return false
        // Only check native WebView scroll position (synchronous and reliable)
        // Don't use canChildScrollUpValue here as it's async and can cause race conditions
        // The parent SwipeRefreshLayout calls this frequently during touch handling
        return wv.scrollY > 0
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        originalEnabledState = enabled
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialY = ev.y
                hasDeterminedDirection = false
                updateScrollPosition()
            }
            MotionEvent.ACTION_MOVE -> {
                // First check: Use synchronous native scroll position as fast path
                val wv = webView
                val isAtTop = wv != null && wv.scrollY == 0 && !canChildScrollUpValue
                
                if (!hasDeterminedDirection && isAtTop) {
                    val deltaY = ev.y - initialY
                    // If user is scrolling down (finger moving down, positive delta)
                    // disable swipe refresh temporarily
                    if (deltaY > 10) {
                        // This is a downward scroll, not a pull-to-refresh
                        hasDeterminedDirection = true
                        super.setEnabled(false)
                        return false
                    } else if (deltaY < -10) {
                        // This is an upward pull (pull-to-refresh gesture)
                        hasDeterminedDirection = true
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Re-enable on touch end to allow next gesture
                super.setEnabled(originalEnabledState)
                hasDeterminedDirection = false
            }
        }
        return super.onInterceptTouchEvent(ev)
    }
}
