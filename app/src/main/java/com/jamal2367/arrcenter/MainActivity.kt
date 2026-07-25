package com.jamal2367.arrcenter

import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jamal2367.arrcenter.ui.AppRoot
import com.jamal2367.arrcenter.ui.TouchTracker

class MainActivity : ComponentActivity() {

    // Lives on the activity rather than in the composition: this is the only place that sees
    // every touch, including the ones the WebView swallows.
    private val touches = TouchTracker()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Required from Android 15 on, where edge to edge is enforced: without it the
        // content would sit under the status and navigation bars.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AppRoot(touches = touches)
        }
    }

    /**
     * Every touch inside the app - taps, scrolls, drags - is reported before it reaches the
     * view hierarchy, so the transient navigation can show itself and restart its countdown.
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        touches.onTouch()
        return super.dispatchTouchEvent(ev)
    }
}
