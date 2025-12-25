package com.ivarna.deviceinsight.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ivarna.deviceinsight.domain.model.CpuDataPoint
import com.ivarna.deviceinsight.domain.model.PowerDataPoint
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import androidx.compose.ui.graphics.toArgb

@Composable
fun CpuUtilizationGraph(
    dataPoints: List<CpuDataPoint>,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer.build() }
    
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
        Text(
            text = "CPU Load History",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (dataPoints.isNotEmpty()) {
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(
                        lineProvider = LineCartesianLayer.LineProvider.series(
                            LineCartesianLayer.Line(
                                fill = LineCartesianLayer.LineFill.single(
                                    fill = com.patrykandpatrick.vico.core.common.Fill(MaterialTheme.colorScheme.primary.toArgb())
                                )
                            )
                        )
                    ),
                    startAxis = rememberStartAxis(
                        valueFormatter = { value, _, _ -> "${value.toInt()}%" },
                        itemPlacer = com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis.ItemPlacer.step({ 25.0 })
                    ),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = { _, _, _ -> "" } // Hide X labels for cleanliness or format time
                    ),
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .height(150.dp)
                    .fillMaxWidth()
                    .padding(end = 16.dp), // Padding for Y-axis labels
                zoomState = com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState(
                    initialZoom = com.patrykandpatrick.vico.core.cartesian.Zoom.Content,
                    minZoom = com.patrykandpatrick.vico.core.cartesian.Zoom.Content
                ),
                scrollState = com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState(
                    scrollEnabled = true
                )
            )
        } else {
             androidx.compose.foundation.layout.Box(
                modifier = Modifier.height(150.dp).fillMaxWidth(),
                contentAlignment = androidx.compose.ui.Alignment.Center
             ) {
                 Text("No Data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
             }
        }
        Text(
             text = if (dataPoints.isNotEmpty()) "Current: ${dataPoints.last().utilization.toInt()}%" else "",
             style = MaterialTheme.typography.bodySmall,
             color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PowerConsumptionGraph(
    dataPoints: List<PowerDataPoint>,
    multiplier: Float = 1f,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer.build() }
    
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
         Text(
            text = "Power Consumption (Watts)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (dataPoints.isNotEmpty()) {
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(
                        lineProvider = LineCartesianLayer.LineProvider.series(
                             LineCartesianLayer.Line(
                                fill = LineCartesianLayer.LineFill.single(
                                    fill = com.patrykandpatrick.vico.core.common.Fill(MaterialTheme.colorScheme.secondary.toArgb())
                                )
                            )
                        )
                    ),
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = { _, _, _ -> "" }
                    ),
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .height(150.dp)
                    .fillMaxWidth()
                    .padding(end = 16.dp),
                scrollState = com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState(
                    scrollEnabled = true
                )
            )
        } else {
             androidx.compose.foundation.layout.Box(
                modifier = Modifier.height(150.dp).fillMaxWidth(),
                contentAlignment = androidx.compose.ui.Alignment.Center
             ) {
                 Text("No Data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
             }
        }
    }
}
