package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.RssiSample
import com.example.ui.theme.AmberCaution
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.EmeraldSignal
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import kotlin.math.roundToInt

/**
 * Real-time Wi-Fi RSSI (dBm) line chart powered by Vico Compose M3.
 */
@Composable
fun RssiHistoryLineChart(
  samples: List<RssiSample>,
  modifier: Modifier = Modifier,
) {
  val modelProducer = remember { CartesianChartModelProducer() }
  val rangeProvider = remember { CartesianLayerRangeProvider.fixed(minY = -100.0, maxY = -30.0) }

  LaunchedEffect(samples) {
    if (samples.isNotEmpty()) {
      val seriesValues =
        if (samples.size == 1) {
          val first = samples.first().rssiDbm
          listOf(first, first)
        } else {
          samples.map { it.rssiDbm }
        }
      modelProducer.runTransaction {
        lineSeries {
          series(seriesValues)
        }
      }
    }
  }

  Surface(
    modifier = modifier.fillMaxWidth().testTag("rssi_history_chart_card"),
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surfaceContainer,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.Timeline,
            contentDescription = null,
            tint = CyanPulse,
          )
          Column {
            Text(
              text = stringResource(R.string.rssi_history_title),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            samples.lastOrNull()?.let { latest ->
              Text(
                text = latest.ssid,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        samples.lastOrNull()?.let { latest ->
          Text(
            text = "${latest.rssiDbm} dBm",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = CyanPulse,
          )
        }
      }

      if (samples.isEmpty()) {
        Text(
          text = stringResource(R.string.rssi_history_empty),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        ProvideVicoTheme(rememberM3VicoTheme()) {
          CartesianChartHost(
            chart =
              rememberCartesianChart(
                rememberLineCartesianLayer(
                  lineProvider =
                    LineCartesianLayer.LineProvider.series(
                      LineCartesianLayer.rememberLine(
                        fill = LineCartesianLayer.LineFill.single(fill(CyanPulse)),
                        areaFill =
                          LineCartesianLayer.AreaFill.single(fill(CyanPulse.copy(alpha = 0.16f))),
                      )
                    ),
                  rangeProvider = rangeProvider,
                ),
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom(),
              ),
            modelProducer = modelProducer,
            scrollState = rememberVicoScrollState(scrollEnabled = false),
            modifier = Modifier.fillMaxWidth().height(180.dp).testTag("rssi_vico_line_chart"),
          )
        }

        val minRssi = samples.minOf { it.rssiDbm }
        val maxRssi = samples.maxOf { it.rssiDbm }
        val avgRssi = samples.map { it.rssiDbm }.average().roundToInt()

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          RssiStatChip(label = "Peak", value = "$maxRssi dBm", color = EmeraldSignal)
          RssiStatChip(label = "Avg", value = "$avgRssi dBm", color = CyanPulse)
          RssiStatChip(label = "Low", value = "$minRssi dBm", color = AmberCaution)
        }
      }
    }
  }
}

@Composable
private fun RssiStatChip(
  label: String,
  value: String,
  color: androidx.compose.ui.graphics.Color,
) {
  Surface(
    shape = MaterialTheme.shapes.small,
    color = color.copy(alpha = 0.1f),
    border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = color,
      )
    }
  }
}
