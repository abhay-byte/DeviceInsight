package com.ivarna.deviceinsight.ui.caliper.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.GlanceId

data class WidgetRenderState(
    val kind: WidgetKind,
    val appWidgetId: Int,
    val exactSize: DpSize,
    val tier: Tier,
    val medium: com.ivarna.deviceinsight.ui.caliper.Medium,
    val config: BenchConfig,
    val snapshot: BenchSnapshot,
    val calibrating: Boolean,
    val snapshotSource: WidgetSnapshotSource = WidgetSnapshotSource.ON_DEMAND
)

fun buildWidgetRenderState(
    kind: WidgetKind,
    appWidgetId: Int,
    exactSize: DpSize,
    medium: com.ivarna.deviceinsight.ui.caliper.Medium,
    config: BenchConfig,
    snapshot: BenchSnapshot,
    calibrating: Boolean = false,
    snapshotSource: WidgetSnapshotSource = WidgetSnapshotSource.ON_DEMAND
): WidgetRenderState = WidgetRenderState(
    kind = kind,
    appWidgetId = appWidgetId,
    exactSize = exactSize,
    tier = Tier.of(exactSize.width.value.toInt(), exactSize.height.value.toInt()),
    medium = medium,
    config = config,
    snapshot = snapshot,
    calibrating = calibrating,
    snapshotSource = snapshotSource
)

object WidgetSizeResolver {
    private const val FALLBACK_WIDTH_DP = 280
    private const val FALLBACK_HEIGHT_DP = 140

    fun fromOptions(options: android.os.Bundle?): DpSize {
        val width = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            ?.takeIf { it > 0 } ?: FALLBACK_WIDTH_DP
        val height = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            ?.takeIf { it > 0 } ?: FALLBACK_HEIGHT_DP
        return fromDimensions(width, height)
    }

    fun fromDimensions(widthDp: Int, heightDp: Int): DpSize =
        DpSize(widthDp.coerceAtLeast(1).dp, heightDp.coerceAtLeast(1).dp)

    suspend fun resolve(context: Context, appWidgetId: Int): DpSize {
        val manager = AppWidgetManager.getInstance(context)
        val options = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            manager.getAppWidgetOptions(appWidgetId)
        } else null
        val optionSize = fromOptions(options)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            return optionSize
        }
        return runCatching {
            val glanceId: GlanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
            val sizes = GlanceAppWidgetManager(context).getAppWidgetSizes(glanceId)
            sizes.minByOrNull { size ->
                kotlin.math.abs(size.width.value - optionSize.width.value) +
                    kotlin.math.abs(size.height.value - optionSize.height.value)
            } ?: optionSize
        }.getOrElse { optionSize }
    }
}
