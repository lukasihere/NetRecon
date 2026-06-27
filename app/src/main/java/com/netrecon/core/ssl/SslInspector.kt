package com.netrecon.core.ssl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date

data class SslInfo(
    val host: String,
    val port: Int,
    val subject: String,
    val issuer: String,
    val validFrom: String,
    val validTo: String,
    val isExpired: Boolean,
    val daysUntilExpiry: Long,
    val serialNumber: String,
    val signatureAlgorithm: String,
    val subjectAltNames: List<String>,
    val supportedProtocols: List<String>,
    val weakCiphers: List<String>,
    val grade: String
)

object SslInspector {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    suspend fun inspect(host: String, port: Int = 443): Result<SslInfo> = withContext(Dispatchers.IO) {
        try {
            val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
            val socket = factory.createSocket(host, port) as SSLSocket
            socket.soTimeout = 5000
            socket.startHandshake()

            val session = socket.session
            val certs = session.peerCertificates
            val cert = certs[0] as X509Certificate

            val now = Date()
            val expiry = cert.notAfter
            val daysLeft = (expiry.time - now.time) / (1000 * 60 * 60 * 24)

            val altNames = cert.subjectAlternativeNames?.mapNotNull { it[1] as? String } ?: emptyList()

            val supportedProtocols = socket.enabledProtocols.toList()
            val weakCiphers = findWeakCiphers(socket.enabledCipherSuites.toList())

            socket.close()

            val grade = calculateGrade(daysLeft, weakCiphers, supportedProtocols)

            Result.success(
                SslInfo(
                    host = host,
                    port = port,
                    subject = cert.subjectDN.name,
                    issuer = cert.issuerDN.name,
                    validFrom = dateFormat.format(cert.notBefore),
                    validTo = dateFormat.format(cert.notAfter),
                    isExpired = now.after(expiry),
                    daysUntilExpiry = daysLeft,
                    serialNumber = cert.serialNumber.toString(16).uppercase(),
                    signatureAlgorithm = cert.sigAlgName,
                    subjectAltNames = altNames,
                    supportedProtocols = supportedProtocols,
                    weakCiphers = weakCiphers,
                    grade = grade
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun findWeakCiphers(ciphers: List<String>): List<String> {
        val weakPatterns = listOf("RC4", "DES", "MD5", "NULL", "EXPORT", "anon", "3DES")
        return ciphers.filter { cipher -> weakPatterns.any { cipher.contains(it, ignoreCase = true) } }
    }

    private fun calculateGrade(daysLeft: Long, weakCiphers: List<String>, protocols: List<String>): String {
        var score = 100
        if (daysLeft < 0) score -= 50
        else if (daysLeft < 30) score -= 20
        else if (daysLeft < 90) score -= 10
        score -= weakCiphers.size * 10
        if (protocols.contains("TLSv1") || protocols.contains("SSLv3")) score -= 15
        return when {
            score >= 90 -> "A+"
            score >= 80 -> "A"
            score >= 70 -> "B"
            score >= 60 -> "C"
            score >= 50 -> "D"
            else -> "F"
        }
    }
}
