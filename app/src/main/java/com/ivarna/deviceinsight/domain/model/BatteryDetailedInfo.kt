package com.ivarna.deviceinsight.domain.model

data class BatteryDetailedInfo(
    val powerSource: String,
    val chargeCounter: String,
    val currentNow: String,
    val chargingCycles: Int,
    val remainingChargeTime: String,
    val capacity: String
)
