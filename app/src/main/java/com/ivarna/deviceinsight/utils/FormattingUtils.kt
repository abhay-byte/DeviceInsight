package com.ivarna.deviceinsight.utils

import java.util.Locale
import kotlin.math.ceil

object FormattingUtils {
    fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1 -> String.format(Locale.ROOT, "%.2f GB", gb)
            mb >= 1 -> String.format(Locale.ROOT, "%.2f MB", mb)
            kb >= 1 -> String.format(Locale.ROOT, "%.2f KB", kb)
            else -> "$bytes B"
        }
    }

    fun formatMemorySize(bytes: Long): String {
        return "${bytes / (1024 * 1024)} MB"
    }

    fun formatInstalledRam(bytes: Long): String {
        val gb = ceil(bytes.toDouble() / (1024 * 1024 * 1024)).toInt()
        return "$gb GB"
    }
    
    fun String.capitalizeWords(): String {
        return this.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
    }
}
