package com.ivarna.deviceinsight.ui.caliper.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/** Launcher fires this after the user accepts the pin sheet — log only; the sheet's
 *  delay(1200) + ON_RESUME observer stays the primary instrument-list refresh. */
class PinSuccessReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i("PinSuccessReceiver", "widget pinned")
    }
}
