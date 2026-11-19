package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.example.domain.analyzer.WifiTelemetryAnalyzer
import com.example.domain.model.ActiveConnectionInfo
import com.example.domain.model.ConnectionUiState
import com.example.domain.model.RssiSample
import com.example.domain.model.WifiNetworkInfo
import com.example.domain.model.WifiSecurityType
import java.net.Inet4Address
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WifiHardwareStatus(
  val hasScanPermission: Boolean = false,
  val isWifiEnabled: Boolean = true,
  val isLocationEnabled: Boolean = true,
  val scanThrottledNotice: Boolean = false,
  val lastScanTimestampMs: Long? = null,
)

class AndroidWifiRepository(context: Context) {

  private val appContext = context.applicationContext
  private val wifiManager =
    appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
  private val connectivityManager =
    appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
  private val locationManager =
    appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

  private val _networks = MutableStateFlow<List<WifiNetworkInfo>>(emptyList())
  val networks: StateFlow<List<WifiNetworkInfo>> = _networks.asStateFlow()

  private val _activeConnection = MutableStateFlow<ActiveConnectionInfo?>(null)
  val activeConnection: StateFlow<ActiveConnectionInfo?> = _activeConnection.asStateFlow()

  private val _rssiHistory = MutableStateFlow<List<RssiSample>>(emptyList())
  val rssiHistory: StateFlow<List<RssiSample>> = _rssiHistory.asStateFlow()

  private val _hardwareStatus = MutableStateFlow(WifiHardwareStatus())
  val hardwareStatus: StateFlow<WifiHardwareStatus> = _hardwareStatus.asStateFlow()

  private val _isScanning = MutableStateFlow(false)
  val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

  private val _connectionState = MutableStateFlow<ConnectionUiState>(ConnectionUiState.Idle)
  val connectionState: StateFlow<ConnectionUiState> = _connectionState.asStateFlow()

  private var isRegistered = false
  private var activeSpecifierCallback: ConnectivityManager.NetworkCallback? = null

  private val wifiScanReceiver =
    object : BroadcastReceiver() {
      override fun onReceive(ctx: Context?, intent: Intent?) {
        val updated =
          intent?.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false) ?: false
        _isScanning.value = false
        readLatestScanResults(wasThrottled = !updated)
      }
    }

  private val activeNetworkCallback =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
        override fun onCapabilitiesChanged(
          network: Network,
          networkCapabilities: NetworkCapabilities,
        ) {
          val linkProps = connectivityManager.getLinkProperties(network)
          val wifiInfo = networkCapabilities.transportInfo as? WifiInfo
          updateActiveConnectionFromWifiInfo(wifiInfo, linkProps)
        }

        override fun onLost(network: Network) {
          _activeConnection.value = null
          _rssiHistory.value = emptyList()
          refreshConnectedFlagInList(null)
        }
      }
    } else {
      object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(
          network: Network,
          networkCapabilities: NetworkCapabilities,
        ) {
          val linkProps = connectivityManager.getLinkProperties(network)
          @Suppress("DEPRECATION")
          val wifiInfo = wifiManager.connectionInfo
          updateActiveConnectionFromWifiInfo(wifiInfo, linkProps)
        }

        override fun onLost(network: Network) {
          _activeConnection.value = null
          _rssiHistory.value = emptyList()
          refreshConnectedFlagInList(null)
        }
      }
    }

  fun startObserving() {
    refreshHardwareStatus()
    if (isRegistered) return
    val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
    ContextCompat.registerReceiver(
      appContext,
      wifiScanReceiver,
      filter,
      ContextCompat.RECEIVER_EXPORTED,
    )
    val wifiRequest =
      NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .build()
    try {
      connectivityManager.registerNetworkCallback(wifiRequest, activeNetworkCallback)
    } catch (_: SecurityException) {
      // Fallback safely if state permission unavailable
    }
    isRegistered = true
    readLatestScanResults(wasThrottled = false)
    refreshCurrentActiveConnection()
  }

  fun stopObserving() {
    if (!isRegistered) return
    try {
      appContext.unregisterReceiver(wifiScanReceiver)
    } catch (_: IllegalArgumentException) {
    }
    try {
      connectivityManager.unregisterNetworkCallback(activeNetworkCallback)
    } catch (_: IllegalArgumentException) {
    }
    releaseRequestedNetwork()
    isRegistered = false
  }

  fun refreshHardwareStatus() {
    val hasFine =
      ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    val isWifiOn =
      try {
        wifiManager.isWifiEnabled
      } catch (_: SecurityException) {
        false
      }
    val isLocationOn = LocationManagerCompat.isLocationEnabled(locationManager)
    _hardwareStatus.value =
      _hardwareStatus.value.copy(
        hasScanPermission = hasFine,
        isWifiEnabled = isWifiOn,
        isLocationEnabled = isLocationOn,
      )
  }

  fun triggerScan() {
    refreshHardwareStatus()
    val status = _hardwareStatus.value
    if (!status.hasScanPermission || !status.isWifiEnabled || !status.isLocationEnabled) {
      _isScanning.value = false
      return
    }
    _isScanning.value = true
    @Suppress("DEPRECATION")
    val initiated =
      try {
        wifiManager.startScan()
      } catch (_: SecurityException) {
        false
      }
    if (!initiated) {
      _isScanning.value = false
      readLatestScanResults(wasThrottled = true)
    }
  }

  @SuppressLint("MissingPermission")
  fun readLatestScanResults(wasThrottled: Boolean) {
    refreshHardwareStatus()
    if (!_hardwareStatus.value.hasScanPermission) {
      _networks.value = emptyList()
      return
    }
    val rawResults: List<ScanResult> =
      try {
        wifiManager.scanResults ?: emptyList()
      } catch (_: SecurityException) {
        emptyList()
      }

    val activeBssid = _activeConnection.value?.bssid
    val activeSsid = _activeConnection.value?.ssid

    val mapped =
      rawResults
        .map { result ->
          val rawSsid =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              result.wifiSsid?.toString()?.removeSurrounding("\"") ?: ""
            } else {
              @Suppress("DEPRECATION")
              result.SSID.orEmpty()
            }
          val isHidden = rawSsid.isBlank()
          val displaySsid = if (isHidden) "Hidden Network" else rawSsid
          val security = WifiTelemetryAnalyzer.parseSecurityType(result.capabilities.orEmpty())
          val band = WifiTelemetryAnalyzer.frequencyToBand(result.frequency)
          val channel = WifiTelemetryAnalyzer.frequencyToChannel(result.frequency)
          val isCurrent =
            (!activeBssid.isNullOrBlank() && activeBssid.equals(result.BSSID, ignoreCase = true)) ||
              (!isHidden && !activeSsid.isNullOrBlank() && activeSsid == rawSsid)

          WifiNetworkInfo(
            ssid = displaySsid,
            isHidden = isHidden,
            bssid = result.BSSID.orEmpty(),
            rssiDbm = result.level,
            signalPercent = WifiTelemetryAnalyzer.rssiToPercent(result.level),
            signalQuality = WifiTelemetryAnalyzer.rssiToQuality(result.level),
            frequencyMhz = result.frequency,
            channel = channel,
            band = band,
            securityType = security,
            capabilitiesRaw = result.capabilities.orEmpty(),
            securityAdvisory = WifiTelemetryAnalyzer.securityAdvisory(security),
            isConnected = isCurrent,
          )
        }
        .sortedByDescending { it.rssiDbm }

    _networks.value = mapped
    _hardwareStatus.value =
      _hardwareStatus.value.copy(
        scanThrottledNotice = wasThrottled,
        lastScanTimestampMs = System.currentTimeMillis(),
      )
  }

  fun refreshCurrentActiveConnection() {
    val activeNetwork = connectivityManager.activeNetwork ?: return
    val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return
    if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
      _activeConnection.value = null
      return
    }
    val linkProps = connectivityManager.getLinkProperties(activeNetwork)
    val wifiInfo =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        caps.transportInfo as? WifiInfo
      } else {
        @Suppress("DEPRECATION")
        wifiManager.connectionInfo
      }
    updateActiveConnectionFromWifiInfo(wifiInfo, linkProps)
  }

  private fun updateActiveConnectionFromWifiInfo(
    wifiInfo: WifiInfo?,
    linkProperties: LinkProperties?,
  ) {
    if (wifiInfo == null) {
      return
    }
    val rawSsid = wifiInfo.ssid?.removeSurrounding("\"").orEmpty()
    val cleanSsid =
      if (rawSsid.isBlank() || rawSsid == "<unknown ssid>") "Connected Wi-Fi" else rawSsid
    val rssi = wifiInfo.rssi.takeIf { it > -127 } ?: -70
    val freq = wifiInfo.frequency.takeIf { it > 0 }
    val band = freq?.let { WifiTelemetryAnalyzer.frequencyToBand(it) } ?: com.example.domain.model.WifiBand.UNKNOWN
    val channel = freq?.let { WifiTelemetryAnalyzer.frequencyToChannel(it) }
    val ipv4 =
      linkProperties?.linkAddresses
        ?.mapNotNull { it.address as? Inet4Address }
        ?.firstOrNull()
        ?.hostAddress

    val info =
      ActiveConnectionInfo(
        ssid = cleanSsid,
        bssid = wifiInfo.bssid?.takeUnless { it == "02:00:00:00:00:00" },
        rssiDbm = rssi,
        signalPercent = WifiTelemetryAnalyzer.rssiToPercent(rssi),
        linkSpeedMbps = wifiInfo.linkSpeed.takeIf { it > 0 },
        frequencyMhz = freq,
        band = band,
        channel = channel,
        ipv4Address = ipv4,
      )
    _activeConnection.value = info
    appendRssiSample(RssiSample(timestampMs = System.currentTimeMillis(), rssiDbm = rssi, ssid = cleanSsid))
    refreshConnectedFlagInList(info)
  }

  private fun appendRssiSample(sample: RssiSample) {
    val current = _rssiHistory.value
    val base = if (current.isNotEmpty() && current.last().ssid != sample.ssid) emptyList() else current
    _rssiHistory.value = (base + sample).takeLast(30)
  }

  private fun refreshConnectedFlagInList(active: ActiveConnectionInfo?) {
    if (_networks.value.isEmpty()) return
    _networks.value =
      _networks.value.map { item ->
        val matched =
          active != null &&
            ((!active.bssid.isNullOrBlank() && active.bssid.equals(item.bssid, ignoreCase = true)) ||
              (!item.isHidden && active.ssid == item.ssid))
        item.copy(isConnected = matched)
      }
  }

  /**
   * Requests a single, explicit user-authorized connection to [network] using Android 10+
   * [WifiNetworkSpecifier] and [ConnectivityManager.requestNetwork].
   */
  fun connectSingleNetwork(network: WifiNetworkInfo, passphrase: String?) {
    if (network.isHidden) {
      _connectionState.value =
        ConnectionUiState.Failed(network.ssid, "Hidden SSIDs should be configured in system Wi-Fi settings.")
      return
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
      _connectionState.value =
        ConnectionUiState.Failed(network.ssid, "Use system Wi-Fi settings on Android 9 and below.")
      return
    }

    if (
      network.securityType != WifiSecurityType.OPEN &&
        network.securityType != WifiSecurityType.OWE &&
        passphrase.isNullOrEmpty()
    ) {
      _connectionState.value =
        ConnectionUiState.Failed(network.ssid, "Enter the network passphrase to connect.")
      return
    }

    releaseRequestedNetwork()
    _connectionState.value = ConnectionUiState.Connecting(network.ssid)

    val specifierBuilder = WifiNetworkSpecifier.Builder().setSsid(network.ssid)
    when (network.securityType) {
      WifiSecurityType.WPA3 -> specifierBuilder.setWpa3Passphrase(passphrase.orEmpty())
      WifiSecurityType.WPA2,
      WifiSecurityType.WPA -> specifierBuilder.setWpa2Passphrase(passphrase.orEmpty())
      WifiSecurityType.OPEN,
      WifiSecurityType.OWE -> Unit
      WifiSecurityType.WEP,
      WifiSecurityType.EAP -> {
        _connectionState.value =
          ConnectionUiState.Failed(
            network.ssid,
            "${network.securityType.label} networks must be configured via system Wi-Fi settings.",
          )
        return
      }
    }

    val request =
      NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .setNetworkSpecifier(specifierBuilder.build())
        .build()

    val callback =
      object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(net: Network) {
          _connectionState.value = ConnectionUiState.Connected(network.ssid)
          refreshCurrentActiveConnection()
        }

        override fun onUnavailable() {
          _connectionState.value =
            ConnectionUiState.Failed(network.ssid, "Connection unavailable or cancelled.")
        }

        override fun onLost(net: Network) {
          if (_connectionState.value is ConnectionUiState.Connected) {
            _connectionState.value = ConnectionUiState.Idle
          }
        }
      }

    activeSpecifierCallback = callback
    try {
      connectivityManager.requestNetwork(request, callback)
    } catch (e: Exception) {
      _connectionState.value =
        ConnectionUiState.Failed(network.ssid, e.localizedMessage ?: "Unable to request network.")
    }
  }

  fun releaseRequestedNetwork() {
    activeSpecifierCallback?.let { cb ->
      try {
        connectivityManager.unregisterNetworkCallback(cb)
      } catch (_: IllegalArgumentException) {
      }
    }
    activeSpecifierCallback = null
    if (_connectionState.value !is ConnectionUiState.Idle) {
      _connectionState.value = ConnectionUiState.Idle
    }
  }

  fun resetConnectionState() {
    _connectionState.value = ConnectionUiState.Idle
  }
}
