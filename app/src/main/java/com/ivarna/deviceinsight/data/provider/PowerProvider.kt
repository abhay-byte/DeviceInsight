package com.ivarna.deviceinsight.data.provider

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.math.abs

class PowerProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getPowerConsumption(): Float {
        val manager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val currentNow = manager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val voltageMv = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        
        if (voltageMv == 0) return 0f
        
        val voltageV = voltageMv / 1000f
        val currentA = currentNow / 1_000_000f
        
        val power = voltageV * currentA
        
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return if (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL) {
            abs(power)
        } else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) {
            -abs(power)
        } else {
            power
        }
    }
}
