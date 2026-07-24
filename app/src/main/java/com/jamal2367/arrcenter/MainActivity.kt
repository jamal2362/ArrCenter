package com.jamal2367.arrcenter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jamal2367.arrcenter.ui.AppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Required from Android 15 on, where edge to edge is enforced: without it the
        // content would sit under the status and navigation bars.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AppRoot()
        }
    }
}
