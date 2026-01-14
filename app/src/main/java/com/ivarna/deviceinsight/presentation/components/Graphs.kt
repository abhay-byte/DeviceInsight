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
import androidx.compose.ui.text.font.FontWeight
import com.ivarna.deviceinsight.domain.model.CpuDataPoint
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
import com.patrykandpatrick.vico.core.common.shader.DynamicShader
import com.patrykandpatrick.vico.compose.common.shader.verticalGradient

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
                        x = dataPoints.map { it.xValue },
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
                text = "CPU Load History",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
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
                        itemPlacer = VerticalAxis.ItemPlacer.step({ 50.0 }),
                        label = null,
                        line = null,
                        tick = null
                    ),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = { _, _, _ -> "" },
                        line = null,
                        tick = null
                    ),
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                zoomState = com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState(
                    initialZoom = com.patrykandpatrick.vico.core.cartesian.Zoom.Content,
                    minZoom = com.patrykandpatrick.vico.core.cartesian.Zoom.Content
                ),
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
                        x = dataPoints.map { it.xValue },
                        y = dataPoints.map { it.powerWatts * multiplier }
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
                text = "Power (Watts)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (dataPoints.isNotEmpty()) {
                Text(
                    text = String.format("%.2f W", dataPoints.last().powerWatts * multiplier),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = secondaryColor
                )
            }
        }
        if (dataPoints.isNotEmpty()) {
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(
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
                        valueFormatter = { _, _, _ -> "" },
                        line = null,
                        tick = null
                    ),
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
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
                        x = dataPoints.map { it.xValue },
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
                text = "RAM Usage (%)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (dataPoints.isNotEmpty()) {
                Text(
                    text = String.format("%.1f%%", dataPoints.last().utilization * 100),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = tertiaryColor
                )
            }
        }
        if (dataPoints.isNotEmpty()) {
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(
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
                        valueFormatter = { value, _, _ -> String.format("%.0f%%", value * 100) },
                        line = null,
                        tick = null
                    ),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = { _, _, _ -> "" },
                        line = null,
                        tick = null
                    ),
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
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
