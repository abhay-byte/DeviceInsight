package com.ivarna.deviceinsight.domain.model

data class UsbDeviceInfo(
    val productName: String,
    val manufacturerName: String,
    val serialNumber: String,
    val deviceId: String, // format: "VID-PID"
    val deviceClass: String,
    val deviceProtocol: String,
    val revision: String,
    val usbVersion: String,
    val speed: String // e.g. "480 Mbps"
)
