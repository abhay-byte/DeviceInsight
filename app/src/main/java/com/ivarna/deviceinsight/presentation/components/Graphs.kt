package com.ivarna.deviceinsight.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.core.tween
import com.ivarna.deviceinsight.domain.model.CpuDataPoint
import com.ivarna.deviceinsight.domain.model.CpuCoreDataPoint
import com.ivarna.deviceinsight.domain.model.PowerDataPoint
import com.ivarna.deviceinsight.domain.model.MemoryDataPoint
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import androidx.compose.ui.graphics.toArgb
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.common.shader.DynamicShader
import com.patrykandpatrick.vico.compose.common.shader.verticalGradient

import com.patrykandpatrick.vico.core.cartesian.data.AxisValueOverrider
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.HorizontalLayout
import com.patrykandpatrick.vico.compose.cartesian.fullWidth

@Composable
fun CpuUtilizationGraph(
    dataPoints: List<CpuDataPoint>,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer.build() }
    val primaryColor = MaterialTheme.colorScheme.primary
    
    androidx.compose.runtime.LaunchedEffect(dataPoints) {
        if (dataPoints.isNotEmpty()) {
            modelProducer.tryRunTransaction {
                lineSeries {
                    series(
                        x = dataPoints.indices.map { it.toDouble() },
                        y = dataPoints.map { it.utilization }
                    )
                }
            }
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = "CPU LOAD HISTORY (%)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (dataPoints.isNotEmpty()) {
                Text(
                    text = "${dataPoints.last().utilization.toInt()}%",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = primaryColor
                )
            }
        }
        if (dataPoints.isNotEmpty()) {
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(
                        axisValueOverrider = AxisValueOverrider.fixed(
                            minX = 0.0,
                            maxX = 60.0,
                            minY = 0.0,
                            maxY = 100.0
                        ),
                        lineProvider = LineCartesianLayer.LineProvider.series(
                            LineCartesianLayer.Line(
                                fill = LineCartesianLayer.LineFill.single(
                                    fill = com.patrykandpatrick.vico.core.common.Fill(primaryColor.toArgb())
                                ),
                                areaFill = LineCartesianLayer.AreaFill.single(
                                    fill = com.patrykandpatrick.vico.core.common.Fill(
                                        DynamicShader.verticalGradient(
                                            arrayOf(primaryColor.copy(alpha = 0.3f), Color.Transparent)
                                        )
                                    )
                                ),
                                pointProvider = null
                            )
                        )
                    ),
                    startAxis = rememberStartAxis(
                        valueFormatter = { value, _, _ -> "${value.toInt()}%" },
                        itemPlacer = VerticalAxis.ItemPlacer.step({ 25.0 }),
                        label = null,
                        line = null,
                        tick = null
                    ),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = { value, _, _ -> 
                            val secondsAgo = (60 - value).toInt()
                            when {
                                secondsAgo == 0 -> "Now"
                                secondsAgo == 60 -> "60s"
                                secondsAgo % 15 == 0 -> "${secondsAgo}s"
                                else -> ""
                            }
                        },
                        itemPlacer = remember { HorizontalAxis.ItemPlacer.default(spacing = 15) },
                        line = null,
                        tick = null
                    ),
                    horizontalLayout = HorizontalLayout.FullWidth()
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                scrollState = rememberVicoScrollState(scrollEnabled = false),
                zoomState = rememberVicoZoomState(initialZoom = Zoom.Content, zoomEnabled = false)
            )
        } else {
             Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = androidx.compose.ui.Alignment.Center
             ) {
                 Text("No Data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
             }
        }
    }
}

@Composable
fun PowerConsumptionGraph(
    dataPoints: List<PowerDataPoint>,
    multiplier: Float = 1f,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer.build() }
    val secondaryColor = MaterialTheme.colorScheme.secondary
    
    androidx.compose.runtime.LaunchedEffect(dataPoints, multiplier) {
        if (dataPoints.isNotEmpty()) {
            modelProducer.tryRunTransaction {
                 lineSeries {
                    series(
                        x = dataPoints.indices.map { it.toDouble() },
                        y = dataPoints.map { kotlin.math.abs(it.powerWatts) * multiplier }
                    )
                }
            }
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = "POWER CONSUMPTION (W)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (dataPoints.isNotEmpty()) {
                val lastPower = dataPoints.last().powerWatts * multiplier
                val powerText = if (lastPower > 0) String.format("+%.2f", lastPower) 
                               else String.format("%.2f", lastPower)
                Text(
                    text = "${powerText} W",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = secondaryColor
                )
            }
        }
        if (dataPoints.isNotEmpty()) {
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(
                        axisValueOverrider = AxisValueOverrider.fixed(
                            minX = 0.0,
                            maxX = 60.0,
                            minY = 0.0
                        ),
                        lineProvider = LineCartesianLayer.LineProvider.series(
                             LineCartesianLayer.Line(
                                fill = LineCartesianLayer.LineFill.single(
                                    fill = com.patrykandpatrick.vico.core.common.Fill(secondaryColor.toArgb())
                                ),
                                areaFill = LineCartesianLayer.AreaFill.single(
                                    fill = com.patrykandpatrick.vico.core.common.Fill(
                                        DynamicShader.verticalGradient(
                                            arrayOf(secondaryColor.copy(alpha = 0.3f), Color.Transparent)
                                        )
                                    )
                                )
                            )
                        )
                    ),
                    startAxis = rememberStartAxis(
                        valueFormatter = { value, _, _ -> String.format("%.1f", value) },
                        line = null,
                        tick = null
                    ),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = { value, _, _ -> 
                            val secondsAgo = (60 - value).toInt()
                            when {
                                secondsAgo == 0 -> "Now"
                                secondsAgo == 60 -> "60s"
                                secondsAgo % 15 == 0 -> "${secondsAgo}s"
                                else -> ""
                            }
                        },
                        itemPlacer = remember { HorizontalAxis.ItemPlacer.default(spacing = 15) },
                        line = null,
                        tick = null
                    ),
                    horizontalLayout = HorizontalLayout.FullWidth()
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                scrollState = rememberVicoScrollState(scrollEnabled = false),
                zoomState = rememberVicoZoomState(initialZoom = Zoom.Content, zoomEnabled = false)
            )
        } else {
             Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = androidx.compose.ui.Alignment.Center
             ) {
                 Text("No Data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
             }
        }
    }
}

@Composable
fun RamUsageGraph(
    dataPoints: List<MemoryDataPoint>,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer.build() }
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    
    androidx.compose.runtime.LaunchedEffect(dataPoints) {
        if (dataPoints.isNotEmpty()) {
            modelProducer.tryRunTransaction {
                lineSeries {
                    series(
                        x = dataPoints.indices.map { it.toDouble() },
                        y = dataPoints.map { it.utilization }
                    )
                }
            }
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = "RAM LOAD HISTORY (%)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (dataPoints.isNotEmpty()) {
                Text(
                    text = String.format("%.1f%%", dataPoints.last().utilization),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = tertiaryColor
                )
            }
        }
        if (dataPoints.isNotEmpty()) {
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(
                        axisValueOverrider = AxisValueOverrider.fixed(
                            minX = 0.0,
                            maxX = 60.0,
                            minY = 0.0,
                            maxY = 100.0
                        ),
                        lineProvider = LineCartesianLayer.LineProvider.series(
                            LineCartesianLayer.Line(
                                fill = LineCartesianLayer.LineFill.single(
                                    fill = com.patrykandpatrick.vico.core.common.Fill(tertiaryColor.toArgb())
                                ),
                                areaFill = LineCartesianLayer.AreaFill.single(
                                    fill = com.patrykandpatrick.vico.core.common.Fill(
                                        DynamicShader.verticalGradient(
                                            arrayOf(tertiaryColor.copy(alpha = 0.3f), Color.Transparent)
                                        )
                                    )
                                )
                            )
                        )
                    ),
                    startAxis = rememberStartAxis(
                        valueFormatter = { value, _, _ -> String.format("%.0f%%", value) },
                        line = null,
                        tick = null
                    ),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = { value, _, _ -> 
                            val secondsAgo = (60 - value).toInt()
                            when {
                                secondsAgo == 0 -> "Now"
                                secondsAgo == 60 -> "60s"
                                secondsAgo % 15 == 0 -> "${secondsAgo}s"
                                else -> ""
                            }
                        },
                        itemPlacer = remember { HorizontalAxis.ItemPlacer.default(spacing = 15) },
                        line = null,
                        tick = null
                    ),
                    horizontalLayout = HorizontalLayout.FullWidth()
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                scrollState = rememberVicoScrollState(scrollEnabled = false),
                zoomState = rememberVicoZoomState(initialZoom = Zoom.Content, zoomEnabled = false)
            )
        } else {
             Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = androidx.compose.ui.Alignment.Center
             ) {
                 Text("No Data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
             }
        }
    }
}

@Composable
fun CpuMultiCoreFrequencyGraph(
    coreHistory: List<List<CpuCoreDataPoint>>,
    maxFreq: Int,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer.build() }
    val coreColors = listOf(
        Color(0xFF00E676), // Neon Green
        Color(0xFF2979FF), // Neon Blue
        Color(0xFFD500F9), // Neon Purple
        Color(0xFFFF1744), // Neon Red
        Color(0xFFFFEA00), // Neon Yellow
        Color(0xFFFF9100), // Neon Orange
        Color(0xFF00B0FF), // Neon Light Blue
        Color(0xFF00E5FF)  // Neon Cyan
    )

    androidx.compose.runtime.LaunchedEffect(coreHistory) {
        if (coreHistory.isNotEmpty() && coreHistory.any { it.isNotEmpty() }) {
            modelProducer.runTransaction {
                lineSeries {
                    coreHistory.forEach { history ->
                        if (history.isNotEmpty()) {
                            series(
                                x = history.indices.map { it.toDouble() },
                                y = history.map { it.frequencyMHz }
                            )
                        }
                    }
                }
            }
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = "CPU CORES (MHz)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (coreHistory.isNotEmpty() && coreHistory.any { it.isNotEmpty() }) {
            
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(
                        axisValueOverrider = AxisValueOverrider.fixed(
                            minX = 0.0, 
                            maxX = 60.0, 
                            minY = 0.0, 
                            maxY = maxFreq.toDouble()
                        ),
                        lineProvider = LineCartesianLayer.LineProvider.series(
                            List(coreHistory.size) { index ->
                                val color = coreColors[index % coreColors.size]
                                LineCartesianLayer.Line(
                                    fill = LineCartesianLayer.LineFill.single(
                                        fill = com.patrykandpatrick.vico.core.common.Fill(color.toArgb())
                                    ),
                                    areaFill = null,
                                    pointProvider = null
                                )
                            }
                        )
                    ),
                    startAxis = rememberStartAxis(
                        valueFormatter = { value, _, _ -> "${value.toInt()}" },
                        itemPlacer = VerticalAxis.ItemPlacer.step({ (maxFreq / 4.0).toDouble().coerceAtLeast(100.0) }),
                        line = null,
                        tick = null
                    ),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = { value, _, _ -> 
                            val secondsAgo = (60 - value).toInt()
                            when {
                                secondsAgo == 0 -> "Now"
                                secondsAgo == 60 -> "60s"
                                secondsAgo % 15 == 0 -> "${secondsAgo}s"
                                else -> ""
                            }
                        },
                        itemPlacer = remember { HorizontalAxis.ItemPlacer.default(spacing = 15) },
                        line = null,
                        tick = null
                    ),
                    horizontalLayout = HorizontalLayout.FullWidth()
                ),
                modelProducer = modelProducer,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                scrollState = rememberVicoScrollState(scrollEnabled = false),
                zoomState = rememberVicoZoomState(initialZoom = Zoom.Content, zoomEnabled = false)
            )
        } else {
             Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = androidx.compose.ui.Alignment.Center
             ) {
                 androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(24.dp))
             }
        }
    }
}
