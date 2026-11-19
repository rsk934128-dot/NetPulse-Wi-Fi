package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.AppTab
import com.example.ui.WifiAnalyzerViewModel
import com.example.ui.screens.ChannelsScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SecurityScreen
import com.example.ui.theme.NetPulseTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      NetPulseTheme {
        NetPulseApp()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetPulseApp(viewModel: WifiAnalyzerViewModel = viewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  val permissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
      viewModel.refreshPermissionsAndScan()
    }

  val requestRequiredPermissions: () -> Unit = {
    val perms =
      buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
      }.toTypedArray()
    permissionLauncher.launch(perms)
  }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_START -> viewModel.startObserving()
        Lifecycle.Event.ON_RESUME -> viewModel.refreshPermissionsAndScan()
        Lifecycle.Event.ON_STOP -> viewModel.stopObserving()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  LaunchedEffect(Unit) {
    if (!uiState.hardwareStatus.hasScanPermission) {
      requestRequiredPermissions()
    }
  }

  BackHandler(enabled = uiState.currentTab != AppTab.SCANNER) {
    viewModel.selectTab(AppTab.SCANNER)
  }

  val openWifiSettings: () -> Unit = {
    context.startActivity(
      Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
  }

  val openLocationSettings: () -> Unit = {
    context.startActivity(
      Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavRail = maxWidth >= 600.dp

    Row(modifier = Modifier.fillMaxSize()) {
      if (useNavRail) {
        NavigationRail(
          containerColor = MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxHeight(),
        ) {
          Spacer(modifier = Modifier.weight(1f))
          AppTab.entries.forEach { tab ->
            val selected = uiState.currentTab == tab
            NavigationRailItem(
              selected = selected,
              onClick = { viewModel.selectTab(tab) },
              icon = { TabIcon(tab = tab, selected = selected) },
              label = { Text(tabLabel(tab)) },
              modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}"),
            )
          }
          Spacer(modifier = Modifier.weight(1f))
        }
      }

      Scaffold(
        modifier = Modifier.weight(1f).fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
          TopAppBar(
            colors =
              TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
              ),
            title = {
              Column {
                Text(
                  text = stringResource(R.string.app_name),
                  style = MaterialTheme.typography.titleLarge,
                )
                Text(
                  text = stringResource(R.string.header_subtitle),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            },
            actions = {
              FilledTonalButton(
                onClick = {
                  if (!uiState.hardwareStatus.hasScanPermission) {
                    requestRequiredPermissions()
                  } else {
                    viewModel.refreshPermissionsAndScan()
                  }
                },
                modifier =
                  Modifier.padding(end = 12.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("top_scan_button"),
              ) {
                if (uiState.isScanning) {
                  CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(stringResource(R.string.action_scanning))
                } else {
                  Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = stringResource(R.string.action_scan_now),
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(stringResource(R.string.action_scan_now))
                }
              }
            },
          )
        },
        bottomBar = {
          if (!useNavRail) {
            NavigationBar(
              containerColor = MaterialTheme.colorScheme.surface,
              windowInsets = WindowInsets.navigationBars,
            ) {
              AppTab.entries.forEach { tab ->
                val selected = uiState.currentTab == tab
                NavigationBarItem(
                  selected = selected,
                  onClick = { viewModel.selectTab(tab) },
                  icon = { TabIcon(tab = tab, selected = selected) },
                  label = { Text(tabLabel(tab)) },
                  modifier = Modifier.testTag("nav_bar_${tab.name.lowercase()}"),
                )
              }
            }
          }
        },
      ) { innerPadding ->
        Box(
          modifier = Modifier.fillMaxSize().padding(innerPadding),
          contentAlignment = Alignment.TopCenter,
        ) {
          Box(modifier = Modifier.fillMaxWidth().widthIn(max = 840.dp)) {
            when (uiState.currentTab) {
              AppTab.SCANNER ->
                ScannerScreen(
                  state = uiState,
                  onRequestPermission = requestRequiredPermissions,
                  onOpenWifiSettings = openWifiSettings,
                  onOpenLocationSettings = openLocationSettings,
                  onSearchQueryChange = viewModel::updateSearchQuery,
                  onSelectFilter = viewModel::selectFilter,
                  onSelectNetwork = viewModel::openNetworkDetail,
                  onDismissNetworkDetail = viewModel::closeNetworkDetail,
                  onConnectSingleNetwork = viewModel::connectToSelectedNetwork,
                  onReleaseRequestedNetwork = viewModel::releaseRequestedNetwork,
                  onTriggerScan = viewModel::refreshPermissionsAndScan,
                )
              AppTab.CHANNELS -> ChannelsScreen(state = uiState)
              AppTab.SECURITY ->
                SecurityScreen(
                  state = uiState,
                  onPassphraseChange = viewModel::updateCandidatePassphrase,
                )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TabIcon(tab: AppTab, selected: Boolean) {
  val icon =
    when (tab) {
      AppTab.SCANNER -> if (selected) Icons.Filled.Radar else Icons.Outlined.Radar
      AppTab.CHANNELS -> if (selected) Icons.Filled.BarChart else Icons.Outlined.BarChart
      AppTab.SECURITY -> if (selected) Icons.Filled.Shield else Icons.Outlined.Shield
    }
  Icon(imageVector = icon, contentDescription = tabLabel(tab))
}

@Composable
private fun tabLabel(tab: AppTab): String =
  when (tab) {
    AppTab.SCANNER -> stringResource(R.string.nav_scanner)
    AppTab.CHANNELS -> stringResource(R.string.nav_channels)
    AppTab.SECURITY -> stringResource(R.string.nav_security)
  }
