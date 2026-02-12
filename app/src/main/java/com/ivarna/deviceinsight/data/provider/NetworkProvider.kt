package com.ivarna.deviceinsight.data.provider

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.DhcpInfo
import android.net.wifi.WifiManager
import android.telephony.TelephonyManager
import com.ivarna.deviceinsight.domain.model.NetworkDetailedInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections
import javax.inject.Inject
import android.text.format.Formatter

class NetworkProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getNetworkDetailedInfo(): NetworkDetailedInfo {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Telephony info
        val phoneType = when (telephonyManager.phoneType) {
            TelephonyManager.PHONE_TYPE_GSM -> "GSM"
            TelephonyManager.PHONE_TYPE_CDMA -> "CDMA"
            TelephonyManager.PHONE_TYPE_SIP -> "SIP"
            else -> "None"
        }

        val simState = when (telephonyManager.simState) {
            TelephonyManager.SIM_STATE_READY -> "Ready"
            TelephonyManager.SIM_STATE_ABSENT -> "Absent"
            TelephonyManager.SIM_STATE_NETWORK_LOCKED -> "Locked"
            TelephonyManager.SIM_STATE_PIN_REQUIRED -> "PIN Required"
            TelephonyManager.SIM_STATE_PUK_REQUIRED -> "PUK Required"
            else -> "Unknown"
        }

        val dataState = when (telephonyManager.dataState) {
            TelephonyManager.DATA_CONNECTED -> "Connected"
            TelephonyManager.DATA_CONNECTING -> "Connecting"
            TelephonyManager.DATA_DISCONNECTED -> "Disconnected"
            TelephonyManager.DATA_SUSPENDED -> "Suspended"
            else -> "Unknown"
        }

        val dataActivity = when (telephonyManager.dataActivity) {
            TelephonyManager.DATA_ACTIVITY_IN -> "In"
            TelephonyManager.DATA_ACTIVITY_OUT -> "Out"
            TelephonyManager.DATA_ACTIVITY_INOUT -> "In/Out"
            TelephonyManager.DATA_ACTIVITY_NONE -> "None"
            else -> "Unknown"
        }

        // Wi-Fi Info
        val wifiInfo = wifiManager.connectionInfo
        val dhcpInfo = wifiManager.dhcpInfo
        
        val wifiState = when (wifiManager.wifiState) {
            WifiManager.WIFI_STATE_ENABLED -> "Enabled"
            WifiManager.WIFI_STATE_DISABLED -> "Disabled"
            WifiManager.WIFI_STATE_ENABLING -> "Enabling"
            WifiManager.WIFI_STATE_DISABLING -> "Disabling"
            else -> "Unknown"
        }

        return NetworkDetailedInfo(
            phoneType = phoneType,
            networkOperatorName = telephonyManager.networkOperatorName ?: "Unknown",
            networkOperatorCode = telephonyManager.networkOperator ?: "Unknown",
            networkCountryIso = telephonyManager.networkCountryIso?.uppercase() ?: "Unknown",
            simProviderName = telephonyManager.simOperatorName ?: "Unknown",
            simProviderCode = telephonyManager.simOperator ?: "Unknown",
            simCountryIso = telephonyManager.simCountryIso?.uppercase() ?: "Unknown",
            simState = simState,
            dataState = dataState,
            dataActivity = dataActivity,
            isRoaming = telephonyManager.isNetworkRoaming,
            hasIccCard = telephonyManager.hasIccCard(),
            
            wifiState = wifiState,
            ssid = wifiInfo?.ssid?.removeSurrounding("\"") ?: "<unknown ssid>",
            bssid = wifiInfo?.bssid ?: "02:00:00:00:00:00",
            isHiddenSsid = wifiInfo?.hiddenSSID ?: false,
            ipv4Address = getIpAddress(),
            signalStrength = wifiInfo?.let { "${it.rssi} dBm (${getSignalLabel(it.rssi)})" } ?: "Unknown",
            linkSpeed = wifiInfo?.let { "${it.linkSpeed} Mbps" } ?: "Unknown",
            frequency = wifiInfo?.let { "${it.frequency} MHz" } ?: "Unknown",
            gateway = dhcpInfo?.let { formatIpAddress(it.gateway) } ?: "Unknown",
            dns1 = dhcpInfo?.let { formatIpAddress(it.dns1) } ?: "Unknown",
            leaseDuration = dhcpInfo?.let { "${it.leaseDuration / 3600} hours" } ?: "Unknown",
            is5GHzSupported = wifiManager.is5GHzBandSupported,
            isWifiAwareSupported = context.packageManager.hasSystemFeature("android.hardware.wifi.aware"),
            isWifiDirectSupported = context.packageManager.hasSystemFeature("android.hardware.wifi.direct")
        )
    }

    private fun getSignalLabel(rssi: Int): String {
        return when {
            rssi >= -50 -> "Excellent"
            rssi >= -60 -> "Good"
            rssi >= -70 -> "Fair"
            else -> "Poor"
        }
    }

    private fun formatIpAddress(ip: Int): String {
        return Formatter.formatIpAddress(ip)
    }

    fun getNetworkOperatorName(): String {
        val manager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return manager.networkOperatorName ?: "Unknown"
    }
    
    fun getNetworkType(): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return "No Network"
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return "Unknown"
        
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Other"
        }
    }

    fun getIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: ""
                    }
                }
            }
        } catch (ex: Exception) { }
        return "Unknown"
    }
}
