package com.ivarna.deviceinsight.data.provider

import android.content.Context
import android.os.Environment
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class StorageProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getInternalStorageInfo(): Pair<Long, Long> {
        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong
        return Pair(totalBlocks * blockSize, availableBlocks * blockSize)
    }

    fun getExternalStorageInfo(): Pair<Long, Long> {
        return try {
            val dirs = context.getExternalFilesDirs(null)
            if (dirs.size > 1 && dirs[1] != null) {
                val stat = StatFs(dirs[1].path)
                val blockSize = stat.blockSizeLong
                val totalBlocks = stat.blockCountLong
                val availableBlocks = stat.availableBlocksLong
                Pair(totalBlocks * blockSize, availableBlocks * blockSize)
            } else {
                Pair(0L, 0L)
            }
        } catch (e: Exception) {
            Pair(0L, 0L)
        }
    }
}
