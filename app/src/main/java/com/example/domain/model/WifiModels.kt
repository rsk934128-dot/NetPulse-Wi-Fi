package com.example.domain.model

enum class WifiBand(val label: String) {
  BAND_2_4_GHZ("2.4 GHz"),
  BAND_5_GHZ("5 GHz"),
  BAND_6_GHZ("6 GHz"),
  UNKNOWN("Unknown"),
}

enum class WifiSecurityType(val label: String, val riskLevel: SecurityRiskLevel) {
  WPA3("WPA3", SecurityRiskLevel.SECURE),
  OWE("OWE (Enhanced Open)", SecurityRiskLevel.SECURE),
  WPA2("WPA2", SecurityRiskLevel.SECURE),
  EAP("Enterprise (802.1X)", SecurityRiskLevel.SECURE),
  WPA("WPA (Legacy)", SecurityRiskLevel.MODERATE),
  WEP("WEP (Obsolete)", SecurityRiskLevel.HIGH_RISK),
  OPEN("Open (Unencrypted)", SecurityRiskLevel.HIGH_RISK),
}

enum class SecurityRiskLevel(val label: String) {
  SECURE("Strong"),
  MODERATE("Legacy"),
  HIGH_RISK("At-Risk"),
}

enum class SignalQuality(val label: String) {
  EXCELLENT("Excellent"),
  GOOD("Good"),
  FAIR("Fair"),
  WEAK("Weak"),
}

enum class ScanFilter {
  ALL,
  BAND_2_4,
  BAND_5_OR_6,
  STRONG_SIGNAL,
  AT_RISK,
}

enum class AppTab {
  SCANNER,
  CHANNELS,
  SECURITY,
}

data class WifiNetworkInfo(
  val ssid: String,
  val isHidden: Boolean,
  val bssid: String,
  val rssiDbm: Int,
  val signalPercent: Int,
  val signalQuality: SignalQuality,
  val frequencyMhz: Int,
  val channel: Int,
  val band: WifiBand,
  val securityType: WifiSecurityType,
  val capabilitiesRaw: String,
  val securityAdvisory: String,
  val isConnected: Boolean = false,
)

data class ActiveConnectionInfo(
  val ssid: String,
  val bssid: String?,
  val rssiDbm: Int,
  val signalPercent: Int,
  val linkSpeedMbps: Int?,
  val frequencyMhz: Int?,
  val band: WifiBand,
  val channel: Int?,
  val ipv4Address: String?,
)

data class RssiSample(
  val timestampMs: Long,
  val rssiDbm: Int,
  val ssid: String,
)

data class ChannelCongestionInfo(
  val channel: Int,
  val band: WifiBand,
  val directNetworkCount: Int,
  val interferenceScore: Int,
)

data class ChannelRecommendation(
  val best24GhzChannel: Int?,
  val best5GhzChannel: Int?,
  val channel24Congestion: List<ChannelCongestionInfo>,
  val channel5Congestion: List<ChannelCongestionInfo>,
)

enum class PassphraseStrength(val label: String) {
  EMPTY("Enter a passphrase"),
  VERY_WEAK("Very Weak"),
  WEAK("Weak"),
  FAIR("Fair"),
  STRONG("Strong"),
  EXCELLENT("Excellent"),
}

data class PassphraseCheckItem(
  val label: String,
  val passed: Boolean,
)

data class PassphraseAuditResult(
  val strength: PassphraseStrength,
  val entropyBits: Int,
  val scoreFraction: Float,
  val checks: List<PassphraseCheckItem>,
  val warnings: List<String>,
)

sealed interface ConnectionUiState {
  data object Idle : ConnectionUiState

  data class Connecting(val ssid: String) : ConnectionUiState

  data class Connected(val ssid: String) : ConnectionUiState

  data class Failed(val ssid: String, val reason: String) : ConnectionUiState
}
