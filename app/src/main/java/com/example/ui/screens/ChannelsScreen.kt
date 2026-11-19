package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.WifiBand
import com.example.domain.model.WifiNetworkInfo
import com.example.ui.WifiAnalyzerUiState
import com.example.ui.components.RssiHistoryLineChart
import com.example.ui.theme.AmberCaution
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.VioletSpectrum

@Composable
fun ChannelsScreen(
  state: WifiAnalyzerUiState,
  modifier: Modifier = Modifier,
) {
  val networks24 =
    state.allNetworks.filter { it.band == WifiBand.BAND_2_4_GHZ && it.channel in 1..14 }
  val networks5 =
    state.allNetworks.filter { it.band == WifiBand.BAND_5_GHZ && it.channel > 0 }
  val rec = state.channelRecommendation

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item(key = "rssi_live_chart") {
      RssiHistoryLineChart(samples = state.rssiHistory)
    }

    item(key = "channel_recommendations") {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.AutoGraph,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = stringResource(R.string.channels_recommendation_label),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            RecommendedChannelTile(
              bandLabel = "2.4 GHz (Non-Overlapping)",
              channelText = rec.best24GhzChannel?.let { "Ch $it" } ?: "--",
              accent = CyanPulse,
              modifier = Modifier.weight(1f),
            )
            RecommendedChannelTile(
              bandLabel = "5 GHz (Cleanest)",
              channelText = rec.best5GhzChannel?.let { "Ch $it" } ?: "--",
              accent = EmeraldSignal,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }

    item(key = "spectrum_24_ghz") {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = stringResource(R.string.channels_24_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )

          if (networks24.isEmpty()) {
            Text(
              text = stringResource(R.string.channels_no_data),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          } else {
            Spectrum24GhzCanvas(networks = networks24)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              listOf(1, 3, 6, 9, 11, 13).forEach { ch ->
                Text(
                  text = "Ch $ch",
                  style = MaterialTheme.typography.labelSmall,
                  color =
                    if (ch == rec.best24GhzChannel) EmeraldSignal
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              networks24.take(6).forEachIndexed { idx, net ->
                val color = spectrumPalette[idx % spectrumPalette.size]
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                  ) {
                    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(color))
                    Text(
                      text = net.ssid,
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                    )
                  }
                  Text(
                    text = "Ch ${net.channel} • ${net.rssiDbm} dBm",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }
    }

    item(key = "spectrum_5_ghz") {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = stringResource(R.string.channels_5_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          if (networks5.isEmpty()) {
            Text(
              text = stringResource(R.string.channels_no_data),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          } else {
            val active5Channels = rec.channel5Congestion.filter { it.directNetworkCount > 0 || it.channel == rec.best5GhzChannel }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              active5Channels.forEach { chInfo ->
                val fillFraction = (chInfo.directNetworkCount / 4f).coerceIn(0.06f, 1f)
                val barColor =
                  when {
                    chInfo.directNetworkCount == 0 -> EmeraldSignal
                    chInfo.directNetworkCount <= 2 -> CyanPulse
                    else -> AmberCaution
                  }
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Text(
                    text = "Ch ${chInfo.channel}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(0.22f),
                  )
                  Box(
                    modifier =
                      Modifier.weight(0.58f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                  ) {
                    if (chInfo.directNetworkCount > 0) {
                      Box(
                        modifier =
                          Modifier.fillMaxWidth(fillFraction)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(barColor)
                      )
                    }
                  }
                  Text(
                    text = "${chInfo.directNetworkCount} APs",
                    style = MaterialTheme.typography.labelSmall,
                    color = barColor,
                    modifier = Modifier.weight(0.20f),
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun RecommendedChannelTile(
  bandLabel: String,
  channelText: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = MaterialTheme.shapes.medium,
    color = accent.copy(alpha = 0.1f),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = bandLabel,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = channelText,
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        color = accent,
      )
    }
  }
}

private val spectrumPalette =
  listOf(CyanPulse, EmeraldSignal, VioletSpectrum, AmberCaution, CoralAlert)

@Composable
private fun Spectrum24GhzCanvas(networks: List<WifiNetworkInfo>) {
  val gridColor = MaterialTheme.colorScheme.outlineVariant
  Canvas(modifier = Modifier.fillMaxWidth().height(170.dp)) {
    val minDbm = -100f
    val maxDbm = -30f
    val bottomY = size.height - 6.dp.toPx()
    val topY = 8.dp.toPx()
    val chartHeight = bottomY - topY

    // Horizontal dBm reference lines
    listOf(-90f, -70f, -50f).forEach { dbm ->
      val ratio = ((dbm - minDbm) / (maxDbm - minDbm)).coerceIn(0f, 1f)
      val y = bottomY - (ratio * chartHeight)
      drawLine(
        color = gridColor,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = 1.dp.toPx(),
      )
    }

    // Draw parabolic channel lobes (-2 .. +2 channels) for each 2.4 GHz AP
    val minChannel = 0f
    val maxChannel = 14f
    networks.take(10).forEachIndexed { idx, net ->
      val color = spectrumPalette[idx % spectrumPalette.size]
      val centerCh = net.channel.toFloat().coerceIn(1f, 13f)
      val leftCh = (centerCh - 2f).coerceAtLeast(minChannel)
      val rightCh = (centerCh + 2f).coerceAtMost(maxChannel)

      val peakRatio = ((net.rssiDbm.toFloat() - minDbm) / (maxDbm - minDbm)).coerceIn(0.08f, 1f)
      val peakY = bottomY - (peakRatio * chartHeight)

      val leftX = (leftCh / maxChannel) * size.width
      val centerX = (centerCh / maxChannel) * size.width
      val rightX = (rightCh / maxChannel) * size.width

      val strokePath =
        Path().apply {
          moveTo(leftX, bottomY)
          quadraticTo(centerX, peakY - (bottomY - peakY) * 0.45f, rightX, bottomY)
        }
      val fillPath =
        Path().apply {
          addPath(strokePath)
          close()
        }

      drawPath(path = fillPath, color = color.copy(alpha = 0.14f))
      drawPath(
        path = strokePath,
        color = color,
        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round),
      )
    }
  }
}
