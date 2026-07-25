package com.jamal2367.arrcenter.ui

import androidx.compose.runtime.Stable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Reports that the user touched the screen - no matter where the touch landed.
 *
 * The services fill the screen with a WebView, and a WebView consumes its touches: a pointer
 * input modifier around it would never see them. The activity therefore feeds every motion
 * event it dispatches into this tracker, and the UI observes [events] instead of trying to
 * catch the touches itself.
 */
@Stable
class TouchTracker {

    // Conflated on purpose: a single gesture produces a stream of move events, and for an
    // idle timeout only the most recent one matters. tryEmit never fails with a replay slot,
    // so reporting a touch stays cheap enough for the dispatch path.
    private val signal = MutableSharedFlow<Unit>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Emits on every touch. Collect with `collectLatest` to restart an idle timeout. */
    val events: Flow<Unit> = signal.asSharedFlow()

    init {
        // Opening the app counts as an interaction, so anything driven by this tracker
        // starts out visible instead of waiting for the first touch.
        signal.tryEmit(Unit)
    }

    fun onTouch() {
        signal.tryEmit(Unit)
    }
}
