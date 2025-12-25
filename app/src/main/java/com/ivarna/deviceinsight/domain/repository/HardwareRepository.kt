package com.ivarna.deviceinsight.domain.repository

import com.ivarna.deviceinsight.domain.model.HardwareInfo

interface HardwareRepository {
    fun getHardwareInfo(): HardwareInfo
}
