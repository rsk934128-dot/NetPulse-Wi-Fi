package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.ActiveConnectionInfo
import com.example.domain.model.SecurityRiskLevel
import com.example.domain.model.SignalQuality
import com.example.domain.model.WifiNetworkInfo
import com.example.domain.model.WifiSecurityType
import com.example.ui.theme.AmberCaution
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.EmeraldSignal

@Composable
fun signalColorForQuality(quality: SignalQuality): Color =
  when (quality) {
    SignalQuality.EXCELLENT -> EmeraldSignal
    SignalQuality.GOOD -> CyanPulse
    SignalQuality.FAIR -> AmberCaution
    SignalQuality.WEAK -> CoralAlert
  }

@Composable
fun securityColorForLevel(level: SecurityRiskLevel): Color =
  when (level) {
    SecurityRiskLevel.SECURE -> EmeraldSignal
    SecurityRiskLevel.MODERATE -> AmberCaution
    SecurityRiskLevel.HIGH_RISK -> CoralAlert
  }

@Composable
fun ActiveConnectionHeroCard(
  activeConnection: ActiveConnectionInfo?,
  onOpenWifiSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val accentColor =
    if (activeConnection != null) {
      when {
        activeConnection.signalPercent >= 75 -> EmeraldSignal
        activeConnection.signalPercent >= 50 -> CyanPulse
        activeConnection.signalPercent >= 30 -> AmberCaution
        else -> CoralAlert
      }
    } else {
      MaterialTheme.colorScheme.outline
    }

  val animatedPercent by
    animateFloatAsState(
      targetValue = (activeConnection?.signalPercent ?: 0) / 100f,
      animationSpec = spring(stiffness = 220f),
      label = "signal_gauge_progress",
    )

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surfaceContainer,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    tonalElevation = 2.dp,
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(
            Brush.radialGradient(
              colors =
                listOf(
                  accentColor.copy(alpha = 0.14f),
                  Color.Transparent,
                ),
              center = Offset(120f, 80f),
              radius = 540f,
            )
          )
          .padding(18.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Box(
          modifier = Modifier.size(76.dp),
          contentAlignment = Alignment.Center,
        ) {
          val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
          Canvas(modifier = Modifier.size(76.dp)) {
            val strokeWidth = 7.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
            drawArc(
              color = trackColor,
              startAngle = 140f,
              sweepAngle = 260f,
              useCenter = false,
              topLeft = topLeft,
              size = arcSize,
              style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            if (activeConnection != null) {
              drawArc(
                color = accentColor,
                startAngle = 140f,
                sweepAngle = 260f * animatedPercent,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
              )
            }
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = activeConnection?.let { "${it.rssiDbm}" } ?: "--",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "dBm",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Box(
              modifier =
                Modifier.size(8.dp)
                  .clip(CircleShape)
                  .background(
                    if (activeConnection != null) EmeraldSignal
                    else MaterialTheme.colorScheme.outline
                  )
            )
            Text(
              text = stringResource(R.string.active_connection_label),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }

          Text(
            text = activeConnection?.ssid ?: stringResource(R.string.active_not_connected),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )

          if (activeConnection != null) {
            val metaParts = buildList {
              add(activeConnection.band.label)
              activeConnection.channel?.takeIf { it > 0 }?.let { add("Ch $it") }
              activeConnection.linkSpeedMbps?.let { add("$it Mbps") }
            }
            Text(
              text = metaParts.joinToString(" • "),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            activeConnection.ipv4Address?.let { ip ->
              Text(
                text = "IP $ip",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          } else {
            Text(
              text = stringResource(R.string.active_not_connected_hint),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
              onClick = onOpenWifiSettings,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("open_wifi_settings_button"),
            ) {
              Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(text = stringResource(R.string.open_wifi_settings))
            }
          }
        }
      }
    }
  }
}

@Composable
fun SecurityBadge(
  securityType: WifiSecurityType,
  modifier: Modifier = Modifier,
) {
  val color = securityColorForLevel(securityType.riskLevel)
  val icon =
    when (securityType.riskLevel) {
      SecurityRiskLevel.SECURE -> Icons.Outlined.Shield
      SecurityRiskLevel.MODERATE -> Icons.Outlined.Lock
      SecurityRiskLevel.HIGH_RISK ->
        if (securityType == WifiSecurityType.OPEN) Icons.Outlined.LockOpen
        else Icons.Outlined.WarningAmber
    }

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(8.dp),
    color = color.copy(alpha = 0.14f),
    border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = securityType.label,
        tint = color,
        modifier = Modifier.size(13.dp),
      )
      Text(
        text = securityType.label,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = color,
      )
    }
  }
}

@Composable
fun WifiNetworkCard(
  network: WifiNetworkInfo,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val sigColor = signalColorForQuality(network.signalQuality)

  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .minimumInteractiveComponentSize()
        .clip(MaterialTheme.shapes.medium)
        .clickable(onClick = onClick)
        .testTag("network_card_${network.bssid}"),
    shape = MaterialTheme.shapes.medium,
    color =
      if (network.isConnected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
      else MaterialTheme.colorScheme.surfaceContainer,
    border =
      BorderStroke(
        width = 1.dp,
        color =
          if (network.isConnected) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
          else MaterialTheme.colorScheme.outlineVariant,
      ),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Box(
            modifier =
              Modifier.size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(sigColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Outlined.Router,
              contentDescription = network.ssid,
              tint = sigColor,
              modifier = Modifier.size(20.dp),
            )
          }

          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text =
                  if (network.isHidden) stringResource(R.string.hidden_ssid_label)
                  else network.ssid,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
              )
              if (network.isConnected) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = EmeraldSignal.copy(alpha = 0.18f),
                ) {
                  Text(
                    text = "LINKED",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldSignal,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  )
                }
              }
            }
            Text(
              text = network.bssid,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
            )
          }
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "${network.rssiDbm} dBm",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = sigColor,
          )
          Text(
            text = network.signalQuality.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Signal strength bar
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
      ) {
        Box(
          modifier =
            Modifier.fillMaxWidth(fraction = (network.signalPercent / 100f).coerceIn(0.05f, 1f))
              .height(5.dp)
              .clip(RoundedCornerShape(3.dp))
              .background(sigColor)
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          TelemetryTag(text = network.band.label)
          if (network.channel > 0) {
            TelemetryTag(text = "Ch ${network.channel}")
          }
          TelemetryTag(text = "${network.frequencyMhz} MHz")
        }
        SecurityBadge(securityType = network.securityType)
      }
    }
  }
}

@Composable
fun TelemetryTag(text: String, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(6.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
    )
  }
}
