package com.example.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WifiFind
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.ConnectionUiState
import com.example.domain.model.ScanFilter
import com.example.domain.model.WifiNetworkInfo
import com.example.domain.model.WifiSecurityType
import com.example.ui.WifiAnalyzerUiState
import com.example.ui.components.ActiveConnectionHeroCard
import com.example.ui.components.SecurityBadge
import com.example.ui.components.WifiNetworkCard
import com.example.ui.components.securityColorForLevel
import com.example.ui.theme.AmberCaution
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.EmeraldSignal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
  state: WifiAnalyzerUiState,
  onRequestPermission: () -> Unit,
  onOpenWifiSettings: () -> Unit,
  onOpenLocationSettings: () -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onSelectFilter: (ScanFilter) -> Unit,
  onSelectNetwork: (WifiNetworkInfo) -> Unit,
  onDismissNetworkDetail: () -> Unit,
  onConnectSingleNetwork: (String?) -> Unit,
  onReleaseRequestedNetwork: () -> Unit,
  onTriggerScan: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item(key = "active_hero") {
      ActiveConnectionHeroCard(
        activeConnection = state.activeConnection,
        onOpenWifiSettings = onOpenWifiSettings,
      )
    }

    if (!state.hardwareStatus.hasScanPermission) {
      item(key = "permission_card") {
        ActionNoticeCard(
          icon = Icons.Outlined.Lock,
          accentColor = MaterialTheme.colorScheme.primary,
          title = stringResource(R.string.permission_title),
          description = stringResource(R.string.permission_rationale),
          buttonLabel = stringResource(R.string.permission_grant_button),
          testTag = "grant_permission_button",
          onClick = onRequestPermission,
        )
      }
    } else if (!state.hardwareStatus.isWifiEnabled) {
      item(key = "wifi_off_card") {
        ActionNoticeCard(
          icon = Icons.Outlined.WifiOff,
          accentColor = CoralAlert,
          title = stringResource(R.string.wifi_disabled_title),
          description = stringResource(R.string.wifi_disabled_desc),
          buttonLabel = stringResource(R.string.open_wifi_settings),
          testTag = "wifi_off_settings_button",
          onClick = onOpenWifiSettings,
        )
      }
    } else if (!state.hardwareStatus.isLocationEnabled) {
      item(key = "location_off_card") {
        ActionNoticeCard(
          icon = Icons.Outlined.LocationOff,
          accentColor = AmberCaution,
          title = stringResource(R.string.location_disabled_title),
          description = stringResource(R.string.location_disabled_desc),
          buttonLabel = stringResource(R.string.open_location_settings),
          testTag = "location_off_settings_button",
          onClick = onOpenLocationSettings,
        )
      }
    }

    item(key = "throttle_notice") {
      AnimatedVisibility(visible = state.hardwareStatus.scanThrottledNotice) {
        Surface(
          shape = MaterialTheme.shapes.small,
          color = AmberCaution.copy(alpha = 0.12f),
          border = BorderStroke(1.dp, AmberCaution.copy(alpha = 0.4f)),
        ) {
          Text(
            text = stringResource(R.string.scan_throttled_notice),
            style = MaterialTheme.typography.bodyMedium,
            color = AmberCaution,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
          )
        }
      }
    }

    item(key = "search_and_filters") {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = state.searchQuery,
          onValueChange = onSearchQueryChange,
          modifier = Modifier.fillMaxWidth().testTag("search_ssid_input"),
          placeholder = { Text("Filter by SSID or BSSID…") },
          leadingIcon = {
            Icon(
              imageVector = Icons.Outlined.Search,
              contentDescription = "Search networks",
            )
          },
          trailingIcon = {
            if (state.searchQuery.isNotEmpty()) {
              IconButton(
                onClick = { onSearchQueryChange("") },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("clear_search_button"),
              ) {
                Icon(imageVector = Icons.Outlined.Clear, contentDescription = "Clear search")
              }
            }
          },
          singleLine = true,
          shape = MaterialTheme.shapes.medium,
        )

        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ScanFilterChip(
            label = stringResource(R.string.filter_all) + " (${state.allNetworks.size})",
            selected = state.selectedFilter == ScanFilter.ALL,
            tag = "filter_all",
            onClick = { onSelectFilter(ScanFilter.ALL) },
          )
          ScanFilterChip(
            label = stringResource(R.string.filter_2_4_ghz),
            selected = state.selectedFilter == ScanFilter.BAND_2_4,
            tag = "filter_24",
            onClick = { onSelectFilter(ScanFilter.BAND_2_4) },
          )
          ScanFilterChip(
            label = stringResource(R.string.filter_5_ghz),
            selected = state.selectedFilter == ScanFilter.BAND_5_OR_6,
            tag = "filter_5",
            onClick = { onSelectFilter(ScanFilter.BAND_5_OR_6) },
          )
          ScanFilterChip(
            label = stringResource(R.string.filter_strong),
            selected = state.selectedFilter == ScanFilter.STRONG_SIGNAL,
            tag = "filter_strong",
            onClick = { onSelectFilter(ScanFilter.STRONG_SIGNAL) },
          )
          ScanFilterChip(
            label = stringResource(R.string.filter_at_risk),
            selected = state.selectedFilter == ScanFilter.AT_RISK,
            tag = "filter_at_risk",
            onClick = { onSelectFilter(ScanFilter.AT_RISK) },
          )
        }
      }
    }

    if (state.filteredNetworks.isEmpty()) {
      item(key = "empty_state") {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = MaterialTheme.shapes.large,
          color = MaterialTheme.colorScheme.surfaceContainer,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.WifiFind,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(44.dp),
            )
            Text(
              text = stringResource(R.string.empty_scan_title),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
              textAlign = TextAlign.Center,
            )
            Text(
              text = stringResource(R.string.empty_scan_desc),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
            if (state.hardwareStatus.hasScanPermission && state.hardwareStatus.isWifiEnabled) {
              Spacer(modifier = Modifier.height(4.dp))
              Button(
                onClick = onTriggerScan,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("empty_scan_button"),
              ) {
                Text(text = stringResource(R.string.action_scan_now))
              }
            }
          }
        }
      }
    } else {
      items(items = state.filteredNetworks, key = { "${it.bssid}_${it.frequencyMhz}" }) { network ->
        WifiNetworkCard(network = network, onClick = { onSelectNetwork(network) })
      }
    }
  }

  val selected = state.selectedNetwork
  if (selected != null) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = onDismissNetworkDetail,
      sheetState = sheetState,
      containerColor = MaterialTheme.colorScheme.surface,
    ) {
      NetworkDetailSheetContent(
        network = selected,
        connectionState = state.connectionState,
        onConnect = onConnectSingleNetwork,
        onReleaseNetwork = onReleaseRequestedNetwork,
        onOpenWifiSettings = onOpenWifiSettings,
      )
    }
  }
}

@Composable
private fun ScanFilterChip(
  label: String,
  selected: Boolean,
  tag: String,
  onClick: () -> Unit,
) {
  FilterChip(
    selected = selected,
    onClick = onClick,
    modifier = Modifier.minimumInteractiveComponentSize().testTag(tag),
    label = { Text(text = label, style = MaterialTheme.typography.labelLarge) },
    colors =
      FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.primary,
      ),
  )
}

@Composable
private fun ActionNoticeCard(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  accentColor: androidx.compose.ui.graphics.Color,
  title: String,
  description: String,
  buttonLabel: String,
  testTag: String,
  onClick: () -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.medium,
    color = accentColor.copy(alpha = 0.08f),
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(imageVector = icon, contentDescription = null, tint = accentColor)
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
      Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Button(
        onClick = onClick,
        modifier = Modifier.minimumInteractiveComponentSize().testTag(testTag),
      ) {
        Text(text = buttonLabel)
      }
    }
  }
}

@Composable
private fun NetworkDetailSheetContent(
  network: WifiNetworkInfo,
  connectionState: ConnectionUiState,
  onConnect: (String?) -> Unit,
  onReleaseNetwork: () -> Unit,
  onOpenWifiSettings: () -> Unit,
) {
  var passphrase by rememberSaveable(network.bssid) { mutableStateOf("") }
  var passwordVisible by rememberSaveable(network.bssid) { mutableStateOf(false) }
  val riskColor = securityColorForLevel(network.securityType.riskLevel)

  Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text =
            if (network.isHidden) stringResource(R.string.hidden_ssid_label) else network.ssid,
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = network.bssid,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      SecurityBadge(securityType = network.securityType)
    }

    Surface(
      shape = MaterialTheme.shapes.small,
      color = riskColor.copy(alpha = 0.1f),
      border = BorderStroke(1.dp, riskColor.copy(alpha = 0.35f)),
    ) {
      Text(
        text = network.securityAdvisory,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth().padding(12.dp),
      )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      DetailRow(label = stringResource(R.string.detail_band), value = network.band.label)
      DetailRow(
        label = stringResource(R.string.detail_channel),
        value = if (network.channel > 0) "Channel ${network.channel}" else "N/A",
      )
      DetailRow(
        label = stringResource(R.string.detail_frequency),
        value = "${network.frequencyMhz} MHz",
      )
      DetailRow(
        label = stringResource(R.string.detail_signal),
        value = "${network.rssiDbm} dBm (${network.signalPercent}% • ${network.signalQuality.label})",
      )
      if (network.capabilitiesRaw.isNotBlank()) {
        DetailRow(
          label = stringResource(R.string.detail_security),
          value = network.capabilitiesRaw,
        )
      }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

    Text(
      text = stringResource(R.string.connect_section_title),
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurface,
    )

    val supportsAppSpecifier =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
        !network.isHidden &&
        network.securityType != WifiSecurityType.WEP &&
        network.securityType != WifiSecurityType.EAP

    if (!supportsAppSpecifier) {
      Text(
        text = stringResource(R.string.connect_legacy_notice),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      OutlinedButton(
        onClick = onOpenWifiSettings,
        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("detail_wifi_settings_button"),
      ) {
        Text(text = stringResource(R.string.open_wifi_settings))
      }
    } else {
      val needsPassphrase =
        network.securityType != WifiSecurityType.OPEN &&
          network.securityType != WifiSecurityType.OWE

      if (needsPassphrase) {
        OutlinedTextField(
          value = passphrase,
          onValueChange = { passphrase = it },
          modifier = Modifier.fillMaxWidth().testTag("connect_passphrase_input"),
          label = { Text(stringResource(R.string.connect_password_label)) },
          placeholder = { Text(stringResource(R.string.connect_password_hint)) },
          singleLine = true,
          visualTransformation =
            if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          trailingIcon = {
            IconButton(
              onClick = { passwordVisible = !passwordVisible },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("toggle_connect_password_visibility"),
            ) {
              Icon(
                imageVector =
                  if (passwordVisible) Icons.Outlined.VisibilityOff
                  else Icons.Outlined.Visibility,
                contentDescription = if (passwordVisible) "Hide passphrase" else "Show passphrase",
              )
            }
          },
        )
      }

      when (connectionState) {
        is ConnectionUiState.Connecting -> {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Text(
              text = stringResource(R.string.connect_status_connecting),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
        is ConnectionUiState.Connected -> {
          Text(
            text = stringResource(R.string.connect_status_connected, connectionState.ssid),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = EmeraldSignal,
          )
        }
        is ConnectionUiState.Failed -> {
          Text(
            text = connectionState.reason,
            style = MaterialTheme.typography.bodyMedium,
            color = CoralAlert,
          )
        }
        ConnectionUiState.Idle -> Unit
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Button(
          onClick = { onConnect(if (needsPassphrase) passphrase else null) },
          enabled =
            connectionState !is ConnectionUiState.Connecting &&
              (!needsPassphrase || passphrase.isNotEmpty()),
          modifier =
            Modifier.weight(1f).minimumInteractiveComponentSize().testTag("connect_single_network_button"),
        ) {
          Text(
            text =
              if (needsPassphrase) stringResource(R.string.connect_button)
              else stringResource(R.string.connect_open_button)
          )
        }
        if (connectionState is ConnectionUiState.Connected ||
          connectionState is ConnectionUiState.Connecting
        ) {
          OutlinedButton(
            onClick = onReleaseNetwork,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("release_network_button"),
          ) {
            Text(text = stringResource(R.string.disconnect_button))
          }
        }
      }
    }
  }
}

@Composable
private fun DetailRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.width(16.dp))
    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
      Text(
        text = value,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.End,
      )
    }
  }
}
