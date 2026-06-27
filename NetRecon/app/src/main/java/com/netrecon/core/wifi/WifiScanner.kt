package com.netrecon.core.wifi

import android.content.Context
import android.net.wifi.WifiManager
import android.net.wifi.ScanResult
import com.netrecon.core.root.RootChecker

data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequency: Int,
    val channel: Int,
    val security: String,
    val signalStrength: Int, // 0-4
    val band: String,
    val capabilities: String
)

data class WifiInfo(
    val ssid: String,
    val bssid: String,
    val ip: String,
    val gateway: String,
    val dns1: String,
    val dns2: String,
    val linkSpeed: Int,
    val frequency: Int,
    val rssi: Int
)

object WifiScanner {

    fun scanNetworks(context: Context): List<WifiNetwork> {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.startScan()
        return wifiManager.scanResults.map { result -> result.toWifiNetwork() }
    }

    fun getCurrentWifiInfo(context: Context): WifiInfo? {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo ?: return null
        val dhcpInfo = wifiManager.dhcpInfo

        return WifiInfo(
            ssid = info.ssid?.replace("\"", "") ?: "",
            bssid = info.bssid ?: "",
            ip = intToIp(dhcpInfo.ipAddress),
            gateway = intToIp(dhcpInfo.gateway),
            dns1 = intToIp(dhcpInfo.dns1),
            dns2 = intToIp(dhcpInfo.dns2),
            linkSpeed = info.linkSpeed,
            frequency = info.frequency,
            rssi = info.rssi
        )
    }

    // Root-only: get ARP table for connected clients
    fun getArpTable(): List<Pair<String, String>> {
        val output = RootChecker.executeRootCommand("cat /proc/net/arp")
        val lines = output.split("\n").drop(1)
        return lines.mapNotNull { line ->
            val parts = line.trim().split("\\s+".toRegex())
            if (parts.size >= 4 && parts[2] == "0x2") {
                Pair(parts[0], parts[3])
            } else null
        }
    }

    // Root-only: enable monitor mode
    fun enableMonitorMode(interface_: String = "wlan0"): Boolean {
        val result = RootChecker.executeRootCommand(
            "ip link set $interface_ down && iw $interface_ set monitor none && ip link set $interface_ up"
        )
        return !result.contains("Error")
    }

    private fun ScanResult.toWifiNetwork(): WifiNetwork {
        val channel = frequencyToChannel(frequency)
        val band = if (frequency > 5000) "5GHz" else "2.4GHz"
        val security = parseCapabilities(capabilities)
        val signal = WifiManager.calculateSignalLevel(level, 5)

        return WifiNetwork(
            ssid = SSID?.replace("\"", "") ?: "<Hidden>",
            bssid = BSSID ?: "",
            rssi = level,
            frequency = frequency,
            channel = channel,
            security = security,
            signalStrength = signal,
            band = band,
            capabilities = capabilities
        )
    }

    private fun parseCapabilities(cap: String): String {
        return when {
            cap.contains("WPA3") -> "WPA3"
            cap.contains("WPA2") -> "WPA2"
            cap.contains("WPA") -> "WPA"
            cap.contains("WEP") -> "WEP ⚠️"
            cap.contains("ESS") && !cap.contains("WPA") -> "Open ⚠️"
            else -> "Unknown"
        }
    }

    private fun frequencyToChannel(freq: Int): Int {
        return when {
            freq == 2484 -> 14
            freq < 2484 -> (freq - 2407) / 5
            freq in 5000..5885 -> (freq - 5000) / 5
            else -> 0
        }
    }

    private fun intToIp(ip: Int): String {
        return "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
    }
}
