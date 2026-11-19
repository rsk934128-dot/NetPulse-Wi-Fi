package com.example.domain.analyzer

import com.example.domain.model.ChannelCongestionInfo
import com.example.domain.model.ChannelRecommendation
import com.example.domain.model.PassphraseAuditResult
import com.example.domain.model.PassphraseCheckItem
import com.example.domain.model.PassphraseStrength
import com.example.domain.model.SignalQuality
import com.example.domain.model.WifiBand
import com.example.domain.model.WifiNetworkInfo
import com.example.domain.model.WifiSecurityType
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.roundToInt

object WifiTelemetryAnalyzer {

  fun frequencyToBand(frequencyMhz: Int): WifiBand =
    when (frequencyMhz) {
      in 2400..2500 -> WifiBand.BAND_2_4_GHZ
      in 4900..5899 -> WifiBand.BAND_5_GHZ
      in 5925..7125 -> WifiBand.BAND_6_GHZ
      else -> WifiBand.UNKNOWN
    }

  fun frequencyToChannel(frequencyMhz: Int): Int =
    when (frequencyMhz) {
      2484 -> 14
      in 2412..2472 -> (frequencyMhz - 2412) / 5 + 1
      in 5000..5899 -> (frequencyMhz - 5000) / 5
      in 5955..7115 -> (frequencyMhz - 5950) / 5
      else -> 0
    }

  fun rssiToPercent(rssiDbm: Int): Int {
    val clamped = rssiDbm.coerceIn(-100, -45)
    return (((clamped + 100).toFloat() / 55f) * 100f).roundToInt().coerceIn(0, 100)
  }

  fun rssiToQuality(rssiDbm: Int): SignalQuality =
    when {
      rssiDbm >= -55 -> SignalQuality.EXCELLENT
      rssiDbm >= -67 -> SignalQuality.GOOD
      rssiDbm >= -78 -> SignalQuality.FAIR
      else -> SignalQuality.WEAK
    }

  fun parseSecurityType(capabilities: String): WifiSecurityType {
    val upper = capabilities.uppercase()
    return when {
      upper.contains("SAE") || upper.contains("WPA3") -> WifiSecurityType.WPA3
      upper.contains("OWE") -> WifiSecurityType.OWE
      upper.contains("EAP") -> WifiSecurityType.EAP
      upper.contains("WPA2") || upper.contains("RSN") -> WifiSecurityType.WPA2
      upper.contains("WPA") -> WifiSecurityType.WPA
      upper.contains("WEP") -> WifiSecurityType.WEP
      else -> WifiSecurityType.OPEN
    }
  }

  fun securityAdvisory(type: WifiSecurityType): String =
    when (type) {
      WifiSecurityType.WPA3 ->
        "Protected with WPA3 (SAE), providing modern forward secrecy and offline resistance."
      WifiSecurityType.OWE ->
        "Enhanced Open (OWE) encrypts wireless traffic even without a shared passphrase."
      WifiSecurityType.EAP ->
        "Enterprise 802.1X authentication isolates per-user credentials via RADIUS."
      WifiSecurityType.WPA2 ->
        "WPA2-PSK (AES) is widely compatible; prefer a 12+ character random passphrase."
      WifiSecurityType.WPA ->
        "Legacy WPA/TKIP uses deprecated cipher suites. Upgrade the router to WPA2/WPA3."
      WifiSecurityType.WEP ->
        "WEP encryption is broken and trivially recoverable. Do not transmit sensitive data."
      WifiSecurityType.OPEN ->
        "Unencrypted network: nearby listeners can inspect plain traffic. Use a VPN if required."
    }

  fun analyzeChannels(networks: List<WifiNetworkInfo>): ChannelRecommendation {
    val networks24 = networks.filter { it.band == WifiBand.BAND_2_4_GHZ && it.channel in 1..14 }
    val channel24Stats =
      (1..13).map { ch ->
        val directCount = networks24.count { it.channel == ch }
        val interference =
          networks24.sumOf { net ->
            val distance = abs(net.channel - ch)
            when {
              distance == 0 -> 10 + (net.signalPercent / 10)
              distance <= 2 -> 6 + (net.signalPercent / 15)
              distance == 3 -> 2
              else -> 0
            }
          }
        ChannelCongestionInfo(
          channel = ch,
          band = WifiBand.BAND_2_4_GHZ,
          directNetworkCount = directCount,
          interferenceScore = interference,
        )
      }

    val best24 =
      listOf(1, 6, 11)
        .mapNotNull { candidate -> channel24Stats.find { it.channel == candidate } }
        .minByOrNull { it.interferenceScore }
        ?.channel

    val networks5 = networks.filter { it.band == WifiBand.BAND_5_GHZ && it.channel > 0 }
    val candidate5Channels = listOf(36, 40, 44, 48, 149, 153, 157, 161, 165)
    val discovered5Channels = networks5.map { it.channel }.distinct()
    val all5Channels = (candidate5Channels + discovered5Channels).distinct().sorted()

    val channel5Stats =
      all5Channels.map { ch ->
        val directCount = networks5.count { it.channel == ch }
        val score =
          networks5
            .filter { it.channel == ch }
            .sumOf { 10 + (it.signalPercent / 10) }
        ChannelCongestionInfo(
          channel = ch,
          band = WifiBand.BAND_5_GHZ,
          directNetworkCount = directCount,
          interferenceScore = score,
        )
      }

    val best5 =
      channel5Stats
        .filter { it.channel in candidate5Channels }
        .minByOrNull { it.interferenceScore }
        ?.channel

    return ChannelRecommendation(
      best24GhzChannel = best24,
      best5GhzChannel = best5,
      channel24Congestion = channel24Stats,
      channel5Congestion = channel5Stats,
    )
  }

  fun auditPassphrase(passphrase: String): PassphraseAuditResult {
    if (passphrase.isEmpty()) {
      return PassphraseAuditResult(
        strength = PassphraseStrength.EMPTY,
        entropyBits = 0,
        scoreFraction = 0f,
        checks =
          listOf(
            PassphraseCheckItem("At least 12 characters (WPA2/WPA3 best practice)", false),
            PassphraseCheckItem("Contains uppercase & lowercase letters", false),
            PassphraseCheckItem("Contains numbers & special symbols", false),
            PassphraseCheckItem("Avoids predictable sequences or repeats", false),
          ),
        warnings = emptyList(),
      )
    }

    val hasLower = passphrase.any { it.isLowerCase() }
    val hasUpper = passphrase.any { it.isUpperCase() }
    val hasDigit = passphrase.any { it.isDigit() }
    val hasSymbol = passphrase.any { !it.isLetterOrDigit() }

    val poolSize =
      (if (hasLower) 26 else 0) +
        (if (hasUpper) 26 else 0) +
        (if (hasDigit) 10 else 0) +
        (if (hasSymbol) 32 else 0)

    val rawEntropy =
      if (poolSize > 0) (passphrase.length * log2(poolSize.toDouble())).roundToInt() else 0

    val warnings = mutableListOf<String>()
    if (passphrase.length < 8) {
      warnings.add("WPA2/WPA3 passphrases must be at least 8 characters long.")
    } else if (passphrase.length < 12) {
      warnings.add("Passphrases under 12 characters are easier to guess offline.")
    }

    val lower = passphrase.lowercase()
    val commonPatterns = listOf("password", "admin", "qwerty", "wifi", "123456", "000000", "112233")
    if (commonPatterns.any { lower.contains(it) }) {
      warnings.add("Contains a commonly guessed word or keyboard/digit pattern.")
    }

    val hasRepeatedRun = Regex("(.)\\1{2,}").containsMatchIn(passphrase)
    if (hasRepeatedRun) {
      warnings.add("Avoid 3 or more identical characters in a row.")
    }

    val hasSequentialDigits = hasSequentialRun(passphrase)
    if (hasSequentialDigits) {
      warnings.add("Avoid sequential ascending or descending runs like 1234 or abcd.")
    }

    val penalty = warnings.size * 14
    val effectiveEntropy = (rawEntropy - penalty).coerceAtLeast(0)

    val strength =
      when {
        passphrase.length < 8 || effectiveEntropy < 32 -> PassphraseStrength.VERY_WEAK
        effectiveEntropy < 48 -> PassphraseStrength.WEAK
        effectiveEntropy < 64 -> PassphraseStrength.FAIR
        effectiveEntropy < 84 -> PassphraseStrength.STRONG
        else -> PassphraseStrength.EXCELLENT
      }

    val checks =
      listOf(
        PassphraseCheckItem(
          "At least 12 characters (WPA2/WPA3 best practice)",
          passphrase.length >= 12,
        ),
        PassphraseCheckItem("Contains uppercase & lowercase letters", hasUpper && hasLower),
        PassphraseCheckItem("Contains numbers & special symbols", hasDigit && hasSymbol),
        PassphraseCheckItem(
          "Avoids predictable sequences or repeats",
          warnings.none { !it.contains("characters") },
        ),
      )

    val fraction = (effectiveEntropy.toFloat() / 96f).coerceIn(0.06f, 1f)

    return PassphraseAuditResult(
      strength = strength,
      entropyBits = effectiveEntropy,
      scoreFraction = fraction,
      checks = checks,
      warnings = warnings,
    )
  }

  private fun hasSequentialRun(input: String): Boolean {
    if (input.length < 4) return false
    var ascCount = 1
    var descCount = 1
    for (i in 1 until input.length) {
      val diff = input[i].code - input[i - 1].code
      if (diff == 1) {
        ascCount++
        descCount = 1
      } else if (diff == -1) {
        descCount++
        ascCount = 1
      } else {
        ascCount = 1
        descCount = 1
      }
      if (ascCount >= 4 || descCount >= 4) return true
    }
    return false
  }
}
