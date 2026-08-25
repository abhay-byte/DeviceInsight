package com.ivarna.deviceinsight.data.fps.source

import com.ivarna.deviceinsight.data.fps.model.FpsSnapshot

interface FpsDataSource {
    suspend fun readFps(): FpsSnapshot?
    val priority: Int
}
