package com.ivarna.deviceinsight.data.provider

import android.content.Context
import android.os.Environment
import com.ivarna.deviceinsight.domain.model.DirectoryInfo
import com.ivarna.deviceinsight.domain.model.ExternalStorageInfo
import com.ivarna.deviceinsight.domain.model.MountPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DirectoryProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun getDirectoryInfo(): DirectoryInfo {
        return DirectoryInfo(
            data = Environment.getDataDirectory().absolutePath,
            root = Environment.getRootDirectory().absolutePath,
            javaHome = System.getProperty("java.home") ?: "/apex/com.android.art",
            downloadCache = Environment.getDownloadCacheDirectory().absolutePath,
            externalStorage = getExternalStorageInfo(),
            mountPoints = getMountPoints()
        )
    }

    private fun getExternalStorageInfo(): ExternalStorageInfo {
        return ExternalStorageInfo(
            primary = Environment.getExternalStorageDirectory().absolutePath,
            externalFiles = context.getExternalFilesDir(null)?.absolutePath ?: "Unknown",
            alarms = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS).absolutePath,
            dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath,
            documents = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath,
            downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath,
            movies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).absolutePath,
            music = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath,
            notifications = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS).absolutePath,
            pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath,
            podcasts = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS).absolutePath,
            ringtones = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES).absolutePath
        )
    }

    private fun getMountPoints(): List<MountPoint> {
        val mounts = mutableListOf<MountPoint>()
        try {
            File("/proc/mounts").forEachLine { line ->
                val parts = line.split(Regex("\\s+"))
                if (parts.size >= 4) {
                    val device = parts[0]
                    val path = parts[1]
                    val fileSystem = parts[2]
                    val options = parts[3].split(",")
                    val isReadOnly = options.contains("ro")
                    
                    mounts.add(
                        MountPoint(
                            path = path,
                            device = device,
                            fileSystem = fileSystem,
                            isReadOnly = isReadOnly
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mounts
    }
}
