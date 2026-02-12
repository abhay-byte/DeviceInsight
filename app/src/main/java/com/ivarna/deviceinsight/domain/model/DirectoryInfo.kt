package com.ivarna.deviceinsight.domain.model

data class DirectoryInfo(
    val data: String,
    val root: String,
    val javaHome: String,
    val downloadCache: String,
    val externalStorage: ExternalStorageInfo,
    val mountPoints: List<MountPoint>
)

data class ExternalStorageInfo(
    val primary: String,
    val externalFiles: String,
    val alarms: String,
    val dcim: String,
    val documents: String,
    val downloads: String,
    val movies: String,
    val music: String,
    val notifications: String,
    val pictures: String,
    val podcasts: String,
    val ringtones: String
)

data class MountPoint(
    val path: String,
    val device: String,
    val fileSystem: String,
    val isReadOnly: Boolean
)
