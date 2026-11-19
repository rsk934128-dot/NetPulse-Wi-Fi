package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.PassphraseStrength
import com.example.ui.WifiAnalyzerUiState
import com.example.ui.theme.AmberCaution
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.EmeraldSignal

@Composable
fun SecurityScreen(
  state: WifiAnalyzerUiState,
  onPassphraseChange: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showPassphrase by rememberSaveable { mutableStateOf(false) }
  val summary = state.securitySummary
  val audit = state.passphraseAudit

  val strengthColor =
    when (audit.strength) {
      PassphraseStrength.EMPTY -> MaterialTheme.colorScheme.outline
      PassphraseStrength.VERY_WEAK,
      PassphraseStrength.WEAK -> CoralAlert
      PassphraseStrength.FAIR -> AmberCaution
      PassphraseStrength.STRONG -> CyanPulse
      PassphraseStrength.EXCELLENT -> EmeraldSignal
    }

  val animatedStrength by
    animateFloatAsState(
      targetValue = audit.scoreFraction,
      animationSpec = spring(stiffness = 260f),
      label = "passphrase_strength_bar",
    )

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item(key = "posture_summary") {
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
              imageVector = Icons.Outlined.Shield,
              contentDescription = null,
              tint = EmeraldSignal,
            )
            Text(
              text = stringResource(R.string.security_overview_title),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            PostureStatTile(
              label = "Strong (WPA2/3)",
              count = summary.secureCount,
              color = EmeraldSignal,
              modifier = Modifier.weight(1f),
            )
            PostureStatTile(
              label = "Legacy (WPA)",
              count = summary.moderateCount,
              color = AmberCaution,
              modifier = Modifier.weight(1f),
            )
            PostureStatTile(
              label = "At-Risk (Open/WEP)",
              count = summary.atRiskCount,
              color = CoralAlert,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }

    item(key = "passphrase_auditor") {
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
              imageVector = Icons.Outlined.VpnKey,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = stringResource(R.string.security_passphrase_title),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }

          Text(
            text = stringResource(R.string.security_passphrase_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          OutlinedTextField(
            value = state.candidatePassphrase,
            onValueChange = onPassphraseChange,
            modifier = Modifier.fillMaxWidth().testTag("auditor_passphrase_input"),
            label = { Text(stringResource(R.string.security_passphrase_input_label)) },
            placeholder = { Text(stringResource(R.string.security_passphrase_placeholder)) },
            singleLine = true,
            visualTransformation =
              if (showPassphrase) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
              IconButton(
                onClick = { showPassphrase = !showPassphrase },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("toggle_auditor_visibility"),
              ) {
                Icon(
                  imageVector =
                    if (showPassphrase) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                  contentDescription = if (showPassphrase) "Hide passphrase" else "Show passphrase",
                )
              }
            },
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = audit.strength.label,
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              color = strengthColor,
            )
            if (audit.strength != PassphraseStrength.EMPTY) {
              Text(
                text = "~${audit.entropyBits} bits entropy",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
          ) {
            if (animatedStrength > 0f) {
              Box(
                modifier =
                  Modifier.fillMaxWidth(animatedStrength)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(strengthColor)
              )
            }
          }

          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            audit.checks.forEach { check ->
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector =
                    if (check.passed) Icons.Outlined.CheckCircle
                    else Icons.Outlined.RadioButtonUnchecked,
                  contentDescription = null,
                  tint =
                    if (check.passed) EmeraldSignal
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = check.label,
                  style = MaterialTheme.typography.bodyMedium,
                  color =
                    if (check.passed) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          if (audit.warnings.isNotEmpty()) {
            Surface(
              shape = MaterialTheme.shapes.small,
              color = AmberCaution.copy(alpha = 0.1f),
              border = BorderStroke(1.dp, AmberCaution.copy(alpha = 0.4f)),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                audit.warnings.forEach { warning ->
                  Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.WarningAmber,
                      contentDescription = null,
                      tint = AmberCaution,
                      modifier = Modifier.size(16.dp),
                    )
                    Text(
                      text = warning,
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    item(key = "hardening_checklist") {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = stringResource(R.string.security_best_practices_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          HardeningTip(
            title = "Enable WPA3-Personal (or WPA2-AES)",
            body = "WPA3 uses Simultaneous Authentication of Equals (SAE) to protect against offline dictionary guessing.",
          )
          HardeningTip(
            title = "Disable WPS (Wi-Fi Protected Setup)",
            body = "8-digit WPS PINs are legacy and vulnerable; disable WPS in your router administration panel.",
          )
          HardeningTip(
            title = "Use a Dedicated Guest SSID",
            body = "Isolate IoT devices and visitors on a separate guest network with client isolation enabled.",
          )
        }
      }
    }
  }
}

@Composable
private fun PostureStatTile(
  label: String,
  count: Int,
  color: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = MaterialTheme.shapes.medium,
    color = color.copy(alpha = 0.1f),
    border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = count.toString(),
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        color = color,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun HardeningTip(title: String, body: String) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.primary,
    )
    Text(
      text = body,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
