package com.ivarna.deviceinsight.domain.model

data class NetworkDetailedInfo(
    // Telephony
    val phoneType: String,
    val networkOperatorName: String,
    val networkOperatorCode: String,
    val networkCountryIso: String,
    val simProviderName: String,
    val simProviderCode: String,
    val simCountryIso: String,
    val simState: String,
    val dataState: String,
    val dataActivity: String,
    val isRoaming: Boolean,
    val hasIccCard: Boolean,
    
    // Wi-Fi
    val wifiState: String,
    val ssid: String,
    val bssid: String,
    val isHiddenSsid: Boolean,
    val ipv4Address: String,
    val signalStrength: String,
    val linkSpeed: String,
    val frequency: String,
    val gateway: String,
    val dns1: String,
    val leaseDuration: String,
    val is5GHzSupported: Boolean,
    val isWifiAwareSupported: Boolean,
    val isWifiDirectSupported: Boolean
)
