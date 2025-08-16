package com.jamal2367.arrcenter

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.jamal2367.arrcenter.ui.AppRoot
import com.jamal2367.arrcenter.ui.theme.AppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        setContent {
            AppTheme {
                DoubleBackToExit {
                    AppRoot()
                }
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun DoubleBackToExit(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var backPressedOnce by remember { mutableStateOf(false) }
    val handler = remember { android.os.Handler(context.mainLooper) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    BackHandler {
        if (backPressedOnce) {
            (context as? ComponentActivity)?.finishAffinity()
        } else {
            backPressedOnce = true
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.snackbar_exit))
            }
            handler.postDelayed({ backPressedOnce = false }, 2000)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                androidx.compose.material3.Snackbar(
                    snackbarData = data,
                    containerColor = Color.DarkGray,
                    contentColor = Color.White
                )
            }
        }
    ) {
        content()
    }
}
