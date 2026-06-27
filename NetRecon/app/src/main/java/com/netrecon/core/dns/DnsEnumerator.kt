package com.netrecon.core.dns

import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.InetAddress

data class DnsRecord(
    val type: String,
    val name: String,
    val value: String,
    val ttl: Int = 0
)

data class SubdomainResult(
    val subdomain: String,
    val ip: String,
    val isAlive: Boolean
)

object DnsEnumerator {

    private val client = OkHttpClient()

    private val commonSubdomains = listOf(
        "www", "mail", "ftp", "admin", "api", "dev", "staging", "test",
        "vpn", "ssh", "remote", "portal", "blog", "shop", "cdn", "static",
        "mx", "ns1", "ns2", "smtp", "pop", "imap", "webmail", "dashboard",
        "app", "m", "mobile", "beta", "alpha", "secure", "login", "auth",
        "git", "gitlab", "jenkins", "jira", "confluence", "wiki", "docs",
        "monitor", "status", "help", "support", "backup", "db", "database"
    )

    suspend fun lookupA(domain: String): List<DnsRecord> = withContext(Dispatchers.IO) {
        try {
            InetAddress.getAllByName(domain).map { addr ->
                DnsRecord("A", domain, addr.hostAddress ?: "")
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun reverseLookup(ip: String): String = withContext(Dispatchers.IO) {
        try {
            InetAddress.getByName(ip).hostName
        } catch (e: Exception) { "N/A" }
    }

    suspend fun dohLookup(domain: String, type: String = "A"): List<DnsRecord> =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://cloudflare-dns.com/dns-query?name=$domain&type=$type"
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/dns-json")
                    .build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(body)
                val answers = json.optJSONArray("Answer") ?: return@withContext emptyList()
                (0 until answers.length()).map { i ->
                    val answer = answers.getJSONObject(i)
                    DnsRecord(
                        type = dnsTypeToString(answer.optInt("type", 1)),
                        name = answer.optString("name"),
                        value = answer.optString("data"),
                        ttl = answer.optInt("TTL", 0)
                    )
                }
            } catch (e: Exception) { emptyList() }
        }

    suspend fun enumerateSubdomains(
        domain: String,
        onFound: (SubdomainResult) -> Unit
    ): List<SubdomainResult> = coroutineScope {
        val results = mutableListOf<SubdomainResult>()
        val jobs = commonSubdomains.map { sub ->
            async(Dispatchers.IO) {
                val full = "$sub.$domain"
                try {
                    val addr = InetAddress.getByName(full)
                    val result = SubdomainResult(full, addr.hostAddress ?: "", true)
                    synchronized(results) { results.add(result) }
                    onFound(result)
                    result
                } catch (e: Exception) {
                    SubdomainResult(full, "", false)
                }
            }
        }
        jobs.awaitAll()
        results
    }

    suspend fun getWhois(domain: String): String = withContext(Dispatchers.IO) {
        try {
            val url = "https://who-dat.as93.net/$domain"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            response.body?.string() ?: "No data"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private fun dnsTypeToString(type: Int): String = when (type) {
        1 -> "A"; 2 -> "NS"; 5 -> "CNAME"; 6 -> "SOA"
        15 -> "MX"; 16 -> "TXT"; 28 -> "AAAA"; 33 -> "SRV"
        else -> "TYPE$type"
    }
}
