package com.ivarna.deviceinsight.data.provider

import android.net.TrafficStats
import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkTrafficProvider @Inject constructor() {
    private var lastRxBytes = TrafficStats.getTotalRxBytes()
    private var lastTxBytes = TrafficStats.getTotalTxBytes()
    private var lastNetworkCheckTime = SystemClock.elapsedRealtime()

    data class TrafficInfo(
        val rxBps: Long,
        val txBps: Long,
        val totalBps: Long
    )

    fun getTrafficSpeed(): TrafficInfo {
        val currentRx = TrafficStats.getTotalRxBytes()
        val currentTx = TrafficStats.getTotalTxBytes()
        val currentTime = SystemClock.elapsedRealtime()

        val timeDiff = currentTime - lastNetworkCheckTime
        
        val rxBps: Long
        val txBps: Long
        val totalBps: Long
        
        if (timeDiff > 0) {
            val rxDiff = currentRx - lastRxBytes
            val txDiff = currentTx - lastTxBytes
            rxBps = (rxDiff * 1000) / timeDiff
            txBps = (txDiff * 1000) / timeDiff
            totalBps = ((rxDiff + txDiff) * 1000) / timeDiff
        } else {
            rxBps = 0
            txBps = 0
            totalBps = 0
        }

        // Update state
        lastRxBytes = currentRx
        lastTxBytes = currentTx
        lastNetworkCheckTime = currentTime

        return TrafficInfo(rxBps, txBps, totalBps)
    }
}
