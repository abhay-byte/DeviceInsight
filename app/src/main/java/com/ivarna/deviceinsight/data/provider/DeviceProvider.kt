package com.ivarna.deviceinsight.data.provider

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import com.ivarna.deviceinsight.domain.model.AndroidDetailedInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@javax.inject.Singleton
class DeviceProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securityProvider: SecurityProvider
) {
    @Volatile private var cachedPlatform: String? = null
    @Volatile private var cachedDeviceFeatures: List<String>? = null
    @Volatile private var cachedDeviceModelName: String? = null
    @Volatile private var cachedBluetoothVersion: String? = null
    @Volatile private var cachedDeviceType: String? = null
    @Volatile private var cachedSerial: String? = null
    @Volatile private var cachedKernelVersion: String? = null

    fun getAndroidDetailedInfo(): AndroidDetailedInfo {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "Unknown"
        val baseband = Build.getRadioVersion() ?: "Unknown"
        
        // Java VM Info
        val javaRuntime = System.getProperty("java.runtime.version") ?: "Android Runtime 0.9"
        val javaVm = System.getProperty("java.vm.version") ?: "ART 2.1.0"
        val heapSize = "${Runtime.getRuntime().maxMemory() / (1024 * 1024)} MB"
        
        // Kernel Info
        val kernelArch = System.getProperty("os.arch") ?: "aarch64"
        val kernelVersion = getKernelVersion()

        // Locale & TimeZone
        val locale = Locale.getDefault()
        val language = "${locale.displayLanguage} (${locale.displayCountry})"
        val tz = TimeZone.getDefault()
        val timeZoneStr = "${tz.displayName} (${getFormattedOffset(tz.rawOffset)})"

        // ICU Info
        val icuVersion = "76.1"
        val icuUnicode = "16.0"
        
        return AndroidDetailedInfo(
            androidVersion = "${Build.VERSION.RELEASE} (${getAndroidCodename()})",
            apiLevel = Build.VERSION.SDK_INT,
            securityPatch = if (Build.VERSION.SDK_INT >= 23) Build.VERSION.SECURITY_PATCH else "Unknown",
            isRooted = securityProvider.isRooted(),
            androidId = androidId,
            baseband = baseband,
            buildId = Build.DISPLAY,
            codename = Build.VERSION.CODENAME,
            fingerprint = Build.FINGERPRINT,
            id = Build.ID,
            incremental = Build.VERSION.INCREMENTAL,
            javaRuntimeVersion = javaRuntime,
            javaVmVersion = javaVm,
            javaVmHeapSize = heapSize,
            kernelArchitecture = kernelArch,
            kernelVersion = kernelVersion,
            tags = Build.TAGS,
            type = Build.TYPE,
            gmsVersion = getPackageVersion("com.google.android.gms"),
            hmsVersion = getPackageVersion("com.huawei.hwid"),
            openSslVersion = "OpenSSL 1.1.1 (compatible; BoringSSL)",
            zLibVersion = "1.3.1",
            icuCldrVersion = "46.0",
            icuLibraryVersion = icuVersion,
            icuUnicodeVersion = icuUnicode,
            androidLanguage = language,
            configuredTimeZone = timeZoneStr,
            upTime = getFormattedUptime()
        )
    }

    private fun getAndroidCodename(): String {
        return when (Build.VERSION.SDK_INT) {
            36 -> "Baklava"
            35 -> "Vanilla Ice Cream"
            34 -> "Upside Down Cake"
            33 -> "Tiramisu"
            else -> "REL"
        }
    }

    private fun getPackageVersion(packageName: String): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName ?: "< Not Present >"
        } catch (e: Exception) {
            "< Not Present >"
        }
    }

    private fun getFormattedOffset(offset: Int): String {
        val totalMinutes = offset / (60 * 1000)
        val hours = totalMinutes / 60
        val minutes = Math.abs(totalMinutes % 60)
        return String.format("UTC%+03d:%02d", hours, minutes)
    }

    private fun getFormattedUptime(): String {
        val millis = SystemClock.elapsedRealtime()
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis / (1000 * 60)) % 60
        val seconds = (millis / 1000) % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun getDeviceModelName(): String {
        cachedDeviceModelName?.let { return it }
        val name = try {
            val n = android.provider.Settings.Global.getString(context.contentResolver, "device_name")
            if (!n.isNullOrEmpty()) n else "${Build.MANUFACTURER} ${Build.MODEL}"
        } catch (e: Exception) {
            "${Build.MANUFACTURER} ${Build.MODEL}"
        }
        cachedDeviceModelName = name
        return name
    }

    fun getPlatform(): String {
        cachedPlatform?.let { return it }
        val plat = try {
            val process = Runtime.getRuntime().exec("getprop ro.board.platform")
            process.inputStream.bufferedReader().use { it.readLine() } ?: Build.HARDWARE
        } catch (e: Exception) {
            Build.HARDWARE
        }
        cachedPlatform = plat
        return plat
    }

    fun getBluetoothVersion(): String {
        cachedBluetoothVersion?.let { return it }
        val version = try {
            if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    "5.3+"
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    "5.2"
                } else {
                    "5.0"
                }
            } else {
                "4.2"
            }
            if (getPlatform().contains("mt6897")) "5.4" else "5.0+"
        } catch (e: Exception) {
            "Unknown"
        }
        cachedBluetoothVersion = version
        return version
    }

    fun getDeviceFeatures(): List<String> {
        cachedDeviceFeatures?.let { return it }
        val features = try {
            context.packageManager.systemAvailableFeatures
                .mapNotNull { it.name }
                .sorted()
        } catch (e: Exception) {
            emptyList()
        }
        cachedDeviceFeatures = features
        return features
    }

    fun getDeviceType(): String {
        cachedDeviceType?.let { return it }
        val count = context.resources.configuration.screenLayout and android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK
        val type = when (count) {
            android.content.res.Configuration.SCREENLAYOUT_SIZE_SMALL -> "Small Phone"
            android.content.res.Configuration.SCREENLAYOUT_SIZE_NORMAL -> "Phone"
            android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE -> "Phablet"
            android.content.res.Configuration.SCREENLAYOUT_SIZE_XLARGE -> "Tablet"
            else -> "Unknown"
        }
        cachedDeviceType = type
        return type
    }

    @android.annotation.SuppressLint("MissingPermission", "HardwareIds")
    fun getSerial(): String {
        cachedSerial?.let { return it }
        val serial = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Build.getSerial()
            } else {
                Build.SERIAL
            }
        } catch (e: Exception) {
            "Unknown"
        }
        cachedSerial = serial
        return serial
    }

    fun getKernelVersion(): String {
        cachedKernelVersion?.let { return it }
        val kernel = try {
            System.getProperty("os.version") ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
        cachedKernelVersion = kernel
        return kernel
    }

    fun getUpTime(): String {
        val uptimeMillis = SystemClock.elapsedRealtime()
        val uptimeSeconds = TimeUnit.MILLISECONDS.toSeconds(uptimeMillis)
        val days = TimeUnit.SECONDS.toDays(uptimeSeconds)
        val hours = TimeUnit.SECONDS.toHours(uptimeSeconds) - TimeUnit.DAYS.toHours(days)
        val minutes = TimeUnit.SECONDS.toMinutes(uptimeSeconds) - TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(uptimeSeconds))
        return "${days}d ${hours}h ${minutes}m"
    }
}
