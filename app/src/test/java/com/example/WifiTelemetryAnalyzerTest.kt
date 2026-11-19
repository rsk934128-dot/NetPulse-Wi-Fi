package com.example

import com.example.domain.analyzer.WifiTelemetryAnalyzer
import com.example.domain.model.PassphraseStrength
import com.example.domain.model.SecurityRiskLevel
import com.example.domain.model.SignalQuality
import com.example.domain.model.WifiBand
import com.example.domain.model.WifiNetworkInfo
import com.example.domain.model.WifiSecurityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiTelemetryAnalyzerTest {

  @Test
  fun `frequency maps accurately to band and channel`() {
    assertEquals(WifiBand.BAND_2_4_GHZ, WifiTelemetryAnalyzer.frequencyToBand(2437))
    assertEquals(6, WifiTelemetryAnalyzer.frequencyToChannel(2437))

    assertEquals(WifiBand.BAND_5_GHZ, WifiTelemetryAnalyzer.frequencyToBand(5180))
    assertEquals(36, WifiTelemetryAnalyzer.frequencyToChannel(5180))

    assertEquals(WifiBand.BAND_6_GHZ, WifiTelemetryAnalyzer.frequencyToBand(6115))
    assertEquals(33, WifiTelemetryAnalyzer.frequencyToChannel(6115))
  }

  @Test
  fun `capabilities parse security protocols and risk levels`() {
    assertEquals(WifiSecurityType.WPA3, WifiTelemetryAnalyzer.parseSecurityType("[WPA2-SAE-CCMP][ESS]"))
    assertEquals(WifiSecurityType.WPA2, WifiTelemetryAnalyzer.parseSecurityType("[WPA2-PSK-CCMP][ESS]"))
    assertEquals(WifiSecurityType.WEP, WifiTelemetryAnalyzer.parseSecurityType("[WEP][ESS]"))
    assertEquals(WifiSecurityType.OPEN, WifiTelemetryAnalyzer.parseSecurityType("[ESS]"))

    assertEquals(SecurityRiskLevel.SECURE, WifiSecurityType.WPA3.riskLevel)
    assertEquals(SecurityRiskLevel.HIGH_RISK, WifiSecurityType.OPEN.riskLevel)
  }

  @Test
  fun `channel analyzer recommends least congested 2_4 GHz non-overlapping channel`() {
    val crowdedOn1And6 =
      listOf(
        sampleNetwork("Home1", 2412, 1, -50),
        sampleNetwork("Neighbor1", 2417, 2, -55),
        sampleNetwork("Home6", 2437, 6, -52),
      )
    val rec = WifiTelemetryAnalyzer.analyzeChannels(crowdedOn1And6)
    assertEquals(11, rec.best24GhzChannel)
  }

  @Test
  fun `passphrase auditor flags weak patterns and rewards strong random phrases`() {
    val weak = WifiTelemetryAnalyzer.auditPassphrase("12345678")
    assertTrue(
      weak.strength == PassphraseStrength.VERY_WEAK || weak.strength == PassphraseStrength.WEAK
    )
    assertTrue(weak.warnings.isNotEmpty())

    val strong = WifiTelemetryAnalyzer.auditPassphrase("River-Lantern#94!Pine")
    assertTrue(
      strong.strength == PassphraseStrength.STRONG ||
        strong.strength == PassphraseStrength.EXCELLENT
    )
    assertTrue(strong.entropyBits >= 75)
  }

  private fun sampleNetwork(ssid: String, freq: Int, channel: Int, rssi: Int) =
    WifiNetworkInfo(
      ssid = ssid,
      isHidden = false,
      bssid = "00:11:22:33:44:${channel.toString().padStart(2, '0')}",
      rssiDbm = rssi,
      signalPercent = WifiTelemetryAnalyzer.rssiToPercent(rssi),
      signalQuality = SignalQuality.GOOD,
      frequencyMhz = freq,
      channel = channel,
      band = WifiBand.BAND_2_4_GHZ,
      securityType = WifiSecurityType.WPA2,
      capabilitiesRaw = "[WPA2-PSK-CCMP]",
      securityAdvisory = "",
    )
}
