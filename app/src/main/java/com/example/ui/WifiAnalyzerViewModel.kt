package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AndroidWifiRepository
import com.example.data.WifiHardwareStatus
import com.example.domain.analyzer.WifiTelemetryAnalyzer
import com.example.domain.model.ActiveConnectionInfo
import com.example.domain.model.AppTab
import com.example.domain.model.ChannelRecommendation
import com.example.domain.model.ConnectionUiState
import com.example.domain.model.PassphraseAuditResult
import com.example.domain.model.RssiSample
import com.example.domain.model.ScanFilter
import com.example.domain.model.SecurityRiskLevel
import com.example.domain.model.WifiBand
import com.example.domain.model.WifiNetworkInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SecurityPostureSummary(
  val totalNetworks: Int = 0,
  val secureCount: Int = 0,
  val moderateCount: Int = 0,
  val atRiskCount: Int = 0,
)

data class WifiAnalyzerUiState(
  val currentTab: AppTab = AppTab.SCANNER,
  val selectedFilter: ScanFilter = ScanFilter.ALL,
  val searchQuery: String = "",
  val hardwareStatus: WifiHardwareStatus = WifiHardwareStatus(),
  val isScanning: Boolean = false,
  val activeConnection: ActiveConnectionInfo? = null,
  val rssiHistory: List<RssiSample> = emptyList(),
  val allNetworks: List<WifiNetworkInfo> = emptyList(),
  val filteredNetworks: List<WifiNetworkInfo> = emptyList(),
  val channelRecommendation: ChannelRecommendation =
    WifiTelemetryAnalyzer.analyzeChannels(emptyList()),
  val securitySummary: SecurityPostureSummary = SecurityPostureSummary(),
  val selectedNetwork: WifiNetworkInfo? = null,
  val connectionState: ConnectionUiState = ConnectionUiState.Idle,
  val candidatePassphrase: String = "",
  val passphraseAudit: PassphraseAuditResult = WifiTelemetryAnalyzer.auditPassphrase(""),
)

class WifiAnalyzerViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = AndroidWifiRepository(application)

  private val currentTab = MutableStateFlow(AppTab.SCANNER)
  private val selectedFilter = MutableStateFlow(ScanFilter.ALL)
  private val searchQuery = MutableStateFlow("")
  private val selectedNetworkBssid = MutableStateFlow<String?>(null)
  private val candidatePassphrase = MutableStateFlow("")

  private var rssiPollingJob: Job? = null

  private val controlsFlow =
    combine(currentTab, selectedFilter, searchQuery, selectedNetworkBssid, candidatePassphrase) {
      tab,
      filter,
      query,
      selectedBssid,
      passphrase ->
      ControlInputs(tab, filter, query, selectedBssid, passphrase)
    }

  private val repoTelemetryFlow =
    combine(
      repository.hardwareStatus,
      repository.isScanning,
      repository.activeConnection,
      repository.rssiHistory,
      repository.networks,
    ) { hw, scanning, active, history, networks ->
      RepoTelemetry(hw, scanning, active, history, networks)
    }

  private val repoFlow =
    combine(
      repoTelemetryFlow,
      repository.connectionState,
    ) { telemetry, connState ->
      RepoSnapshot(
        hardwareStatus = telemetry.hardwareStatus,
        isScanning = telemetry.isScanning,
        activeConnection = telemetry.activeConnection,
        rssiHistory = telemetry.rssiHistory,
        networks = telemetry.networks,
        connectionState = connState,
      )
    }

  val uiState: StateFlow<WifiAnalyzerUiState> =
    combine(controlsFlow, repoFlow) { controls, repo ->
      val filtered =
        repo.networks.filter { net ->
          val matchesQuery =
            controls.query.isBlank() ||
              net.ssid.contains(controls.query, ignoreCase = true) ||
              net.bssid.contains(controls.query, ignoreCase = true)
          val matchesFilter =
            when (controls.filter) {
              ScanFilter.ALL -> true
              ScanFilter.BAND_2_4 -> net.band == WifiBand.BAND_2_4_GHZ
              ScanFilter.BAND_5_OR_6 ->
                net.band == WifiBand.BAND_5_GHZ || net.band == WifiBand.BAND_6_GHZ
              ScanFilter.STRONG_SIGNAL -> net.rssiDbm >= -67
              ScanFilter.AT_RISK -> net.securityType.riskLevel != SecurityRiskLevel.SECURE
            }
          matchesQuery && matchesFilter
        }

      val summary =
        SecurityPostureSummary(
          totalNetworks = repo.networks.size,
          secureCount =
            repo.networks.count { it.securityType.riskLevel == SecurityRiskLevel.SECURE },
          moderateCount =
            repo.networks.count { it.securityType.riskLevel == SecurityRiskLevel.MODERATE },
          atRiskCount =
            repo.networks.count { it.securityType.riskLevel == SecurityRiskLevel.HIGH_RISK },
        )

      WifiAnalyzerUiState(
        currentTab = controls.tab,
        selectedFilter = controls.filter,
        searchQuery = controls.query,
        hardwareStatus = repo.hardwareStatus,
        isScanning = repo.isScanning,
        activeConnection = repo.activeConnection,
        rssiHistory = repo.rssiHistory,
        allNetworks = repo.networks,
        filteredNetworks = filtered,
        channelRecommendation = WifiTelemetryAnalyzer.analyzeChannels(repo.networks),
        securitySummary = summary,
        selectedNetwork = repo.networks.find { it.bssid == controls.selectedBssid },
        connectionState = repo.connectionState,
        candidatePassphrase = controls.passphrase,
        passphraseAudit = WifiTelemetryAnalyzer.auditPassphrase(controls.passphrase),
      )
    }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WifiAnalyzerUiState(),
      )

  fun startObserving() {
    repository.startObserving()
    if (rssiPollingJob?.isActive != true) {
      rssiPollingJob =
        viewModelScope.launch {
          while (isActive) {
            delay(2_000L)
            repository.refreshCurrentActiveConnection()
          }
        }
    }
  }

  fun stopObserving() {
    rssiPollingJob?.cancel()
    rssiPollingJob = null
    repository.stopObserving()
  }

  fun refreshPermissionsAndScan() {
    repository.refreshHardwareStatus()
    repository.triggerScan()
  }

  fun selectTab(tab: AppTab) {
    currentTab.value = tab
  }

  fun selectFilter(filter: ScanFilter) {
    selectedFilter.value = filter
  }

  fun updateSearchQuery(query: String) {
    searchQuery.value = query
  }

  fun openNetworkDetail(network: WifiNetworkInfo) {
    repository.resetConnectionState()
    selectedNetworkBssid.value = network.bssid
  }

  fun closeNetworkDetail() {
    selectedNetworkBssid.value = null
    repository.resetConnectionState()
  }

  fun connectToSelectedNetwork(passphrase: String?) {
    val selected = uiState.value.selectedNetwork ?: return
    repository.connectSingleNetwork(selected, passphrase)
  }

  fun releaseRequestedNetwork() {
    repository.releaseRequestedNetwork()
  }

  fun updateCandidatePassphrase(value: String) {
    candidatePassphrase.value = value
  }

  override fun onCleared() {
    super.onCleared()
    repository.stopObserving()
  }

  private data class ControlInputs(
    val tab: AppTab,
    val filter: ScanFilter,
    val query: String,
    val selectedBssid: String?,
    val passphrase: String,
  )

  private data class RepoTelemetry(
    val hardwareStatus: WifiHardwareStatus,
    val isScanning: Boolean,
    val activeConnection: ActiveConnectionInfo?,
    val rssiHistory: List<RssiSample>,
    val networks: List<WifiNetworkInfo>,
  )

  private data class RepoSnapshot(
    val hardwareStatus: WifiHardwareStatus,
    val isScanning: Boolean,
    val activeConnection: ActiveConnectionInfo?,
    val rssiHistory: List<RssiSample>,
    val networks: List<WifiNetworkInfo>,
    val connectionState: ConnectionUiState,
  )
}
