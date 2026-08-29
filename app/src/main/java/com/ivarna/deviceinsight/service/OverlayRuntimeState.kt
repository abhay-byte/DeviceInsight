package com.ivarna.deviceinsight.service

import java.util.concurrent.atomic.AtomicBoolean

/** Shared runtime state for the overlay and launcher-alias deferral logic. */
object OverlayRuntimeState {
    val isRunning = AtomicBoolean(false)
}
