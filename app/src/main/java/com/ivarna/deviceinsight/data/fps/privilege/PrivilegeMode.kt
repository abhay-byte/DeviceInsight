package com.ivarna.deviceinsight.data.fps.privilege

enum class PrivilegeMode {
    AUTO,
    ROOT,
    SHIZUKU,
    STANDARD;

    companion object {
        fun fromString(value: String?): PrivilegeMode =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: AUTO
    }
}
