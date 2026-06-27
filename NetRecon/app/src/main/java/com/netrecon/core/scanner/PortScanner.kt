package com.netrecon.core.scanner

import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.net.Socket
import java.net.InetAddress

data class PortResult(
    val port: Int,
    val isOpen: Boolean,
    val service: String,
    val banner: String = ""
)

data class HostResult(
    val ip: String,
    val isAlive: Boolean,
    val hostname: String = "",
    val responseTime: Long = 0
)

object PortScanner {

    private val commonServices = mapOf(
        21 to "FTP", 22 to "SSH", 23 to "Telnet", 25 to "SMTP",
        53 to "DNS", 80 to "HTTP", 110 to "POP3", 111 to "RPC",
        135 to "MSRPC", 139 to "NetBIOS", 143 to "IMAP", 443 to "HTTPS",
        445 to "SMB", 993 to "IMAPS", 995 to "POP3S", 1723 to "PPTP",
        3306 to "MySQL", 3389 to "RDP", 5432 to "PostgreSQL",
        5900 to "VNC", 6379 to "Redis", 8080 to "HTTP-Alt",
        8443 to "HTTPS-Alt", 27017 to "MongoDB"
    )

    suspend fun scanPort(host: String, port: Int, timeout: Int = 1500): PortResult {
        return withContext(Dispatchers.IO) {
            try {
                val socket = Socket()
                val startTime = System.currentTimeMillis()
                socket.connect(InetSocketAddress(host, port), timeout)
                val banner = grabBanner(socket)
                socket.close()
                PortResult(
                    port = port,
                    isOpen = true,
                    service = commonServices[port] ?: "Unknown",
                    banner = banner
                )
            } catch (e: Exception) {
                PortResult(port = port, isOpen = false, service = commonServices[port] ?: "Unknown")
            }
        }
    }

    suspend fun scanRange(
        host: String,
        startPort: Int,
        endPort: Int,
        timeout: Int = 1500,
        onProgress: (PortResult) -> Unit
    ): List<PortResult> = coroutineScope {
        val results = mutableListOf<PortResult>()
        val jobs = (startPort..endPort).map { port ->
            async(Dispatchers.IO) {
                val result = scanPort(host, port, timeout)
                if (result.isOpen) {
                    synchronized(results) { results.add(result) }
                    onProgress(result)
                }
                result
            }
        }
        jobs.awaitAll()
        results.sortedBy { it.port }
    }

    suspend fun scanCommonPorts(host: String, onProgress: (PortResult) -> Unit): List<PortResult> {
        return scanRange(host, 1, 1024, onProgress = onProgress)
    }

    suspend fun discoverHosts(
        subnet: String, // e.g. "192.168.1"
        onProgress: (HostResult) -> Unit
    ): List<HostResult> = coroutineScope {
        val results = mutableListOf<HostResult>()
        val jobs = (1..254).map { i ->
            async(Dispatchers.IO) {
                val ip = "$subnet.$i"
                val start = System.currentTimeMillis()
                val alive = InetAddress.getByName(ip).isReachable(1000)
                val elapsed = System.currentTimeMillis() - start
                val hostname = if (alive) {
                    try { InetAddress.getByName(ip).hostName } catch (e: Exception) { ip }
                } else ip
                val result = HostResult(ip, alive, hostname, elapsed)
                if (alive) {
                    synchronized(results) { results.add(result) }
                    onProgress(result)
                }
                result
            }
        }
        jobs.awaitAll()
        results.sortedBy { it.ip.split(".").last().toIntOrNull() ?: 0 }
    }

    private fun grabBanner(socket: Socket): String {
        return try {
            socket.soTimeout = 500
            val reader = socket.getInputStream().bufferedReader()
            reader.readLine()?.take(100) ?: ""
        } catch (e: Exception) { "" }
    }
}
