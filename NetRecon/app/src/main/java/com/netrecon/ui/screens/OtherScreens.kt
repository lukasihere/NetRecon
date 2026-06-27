package com.netrecon.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netrecon.core.dns.DnsEnumerator
import com.netrecon.core.dns.SubdomainResult
import com.netrecon.core.scanner.HostResult
import com.netrecon.core.scanner.PortScanner
import com.netrecon.core.ssl.SslInspector
import com.netrecon.core.root.RootChecker
import com.netrecon.ui.theme.*
import kotlinx.coroutines.*

// ─── HOST DISCOVERY ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostDiscoveryScreen(isRooted: Boolean, onBack: () -> Unit) {
    var subnet by remember { mutableStateOf("192.168.1") }
    var isScanning by remember { mutableStateOf(false) }
    var hosts by remember { mutableStateOf<List<HostResult>>(emptyList()) }
    var progress by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    ReconScreen(title = "HOST DISCOVERY", onBack = onBack) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            ReconCard {
                ReconTextField(value = subnet, onValueChange = { subnet = it }, label = "SUBNET (e.g. 192.168.1)")
                Spacer(Modifier.height(12.dp))
                ScanButton(isScanning = isScanning, label = "DISCOVER HOSTS") {
                    isScanning = true; hosts = emptyList(); progress = 0f
                    scope.launch {
                        PortScanner.discoverHosts(subnet) { host ->
                            hosts = hosts + host
                            progress = hosts.size / 254f
                        }
                        isScanning = false; progress = 1f
                    }
                }
            }
            if (isScanning) LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), color = NeonGreen, trackColor = SurfaceVariant)
            SectionHeader("LIVE HOSTS (${hosts.size})")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(hosts) { host ->
                    ReconResultCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(host.ip, fontFamily = FontFamily.Monospace, color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (host.hostname != host.ip) Text(host.hostname, fontFamily = FontFamily.Monospace, color = TextSecondary, fontSize = 11.sp)
                            }
                            Text("${host.responseTime}ms", fontFamily = FontFamily.Monospace, color = InfoCyan, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─── DNS ENUMERATION ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsEnumScreen(onBack: () -> Unit) {
    var domain by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    var subdomains by remember { mutableStateOf<List<SubdomainResult>>(emptyList()) }
    var aRecords by remember { mutableStateOf("") }
    var whoisData by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    ReconScreen(title = "DNS ENUMERATION", onBack = onBack) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            ReconCard {
                ReconTextField(value = domain, onValueChange = { domain = it }, label = "TARGET DOMAIN")
                Spacer(Modifier.height(12.dp))
                ScanButton(isScanning = isRunning, label = "ENUMERATE") {
                    isRunning = true; subdomains = emptyList(); aRecords = ""; whoisData = ""
                    scope.launch {
                        val aRecs = DnsEnumerator.dohLookup(domain, "A")
                        val mxRecs = DnsEnumerator.dohLookup(domain, "MX")
                        val nsRecs = DnsEnumerator.dohLookup(domain, "NS")
                        val txtRecs = DnsEnumerator.dohLookup(domain, "TXT")
                        aRecords = buildString {
                            appendLine("=== A Records ===")
                            aRecs.forEach { appendLine("${it.value} (TTL: ${it.ttl})") }
                            appendLine("\n=== MX Records ===")
                            mxRecs.forEach { appendLine(it.value) }
                            appendLine("\n=== NS Records ===")
                            nsRecs.forEach { appendLine(it.value) }
                            appendLine("\n=== TXT Records ===")
                            txtRecs.forEach { appendLine(it.value) }
                        }
                        DnsEnumerator.enumerateSubdomains(domain) { sub -> subdomains = subdomains + sub }
                        whoisData = DnsEnumerator.getWhois(domain)
                        isRunning = false
                    }
                }
            }
            if (isRunning) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), color = NeonGreen, trackColor = SurfaceVariant)

            TabRow(selectedTabIndex = activeTab, containerColor = SurfaceDark, contentColor = NeonGreen) {
                listOf("RECORDS", "SUBDOMAINS (${subdomains.size})", "WHOIS").forEachIndexed { i, title ->
                    Tab(selected = activeTab == i, onClick = { activeTab = i }) {
                        Text(title, fontFamily = FontFamily.Monospace, fontSize = 10.sp, modifier = Modifier.padding(vertical = 12.dp))
                    }
                }
            }

            LazyColumn(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                when (activeTab) {
                    0 -> item { Text(aRecords.ifEmpty { "No records yet" }, fontFamily = FontFamily.Monospace, color = TextPrimary, fontSize = 12.sp) }
                    1 -> items(subdomains) { sub ->
                        ReconResultCard {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(sub.subdomain, fontFamily = FontFamily.Monospace, color = InfoCyan, fontSize = 12.sp)
                                Text(sub.ip, fontFamily = FontFamily.Monospace, color = NeonGreen, fontSize = 12.sp)
                            }
                        }
                    }
                    2 -> item { Text(whoisData.ifEmpty { "No WHOIS data yet" }, fontFamily = FontFamily.Monospace, color = TextPrimary, fontSize = 11.sp) }
                }
            }
        }
    }
}

// ─── SSL INSPECTOR ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SslInspectorScreen(onBack: () -> Unit) {
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("443") }
    var isChecking by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<com.netrecon.core.ssl.SslInfo?>(null) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    ReconScreen(title = "SSL INSPECTOR", onBack = onBack) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            ReconCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReconTextField(value = host, onValueChange = { host = it }, label = "HOST", modifier = Modifier.weight(3f))
                    ReconTextField(value = port, onValueChange = { port = it }, label = "PORT", modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                ScanButton(isScanning = isChecking, label = "INSPECT CERT") {
                    isChecking = true; result = null; error = ""
                    scope.launch {
                        SslInspector.inspect(host, port.toIntOrNull() ?: 443)
                            .onSuccess { result = it }
                            .onFailure { error = it.message ?: "Unknown error" }
                        isChecking = false
                    }
                }
            }

            if (error.isNotEmpty()) {
                Text("ERROR: $error", fontFamily = FontFamily.Monospace, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }

            result?.let { ssl ->
                Spacer(Modifier.height(12.dp))
                // Grade badge
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(gradeColor(ssl.grade).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(2.dp, gradeColor(ssl.grade), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(ssl.grade, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, color = gradeColor(ssl.grade), fontSize = 28.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        listOf(
                            "Subject" to ssl.subject.take(60),
                            "Issuer" to ssl.issuer.take(60),
                            "Valid From" to ssl.validFrom,
                            "Valid To" to ssl.validTo,
                            "Days Left" to "${ssl.daysUntilExpiry} days",
                            "Serial" to ssl.serialNumber,
                            "Algorithm" to ssl.signatureAlgorithm,
                            "SANs" to ssl.subjectAltNames.take(5).joinToString(", "),
                            "Weak Ciphers" to if (ssl.weakCiphers.isEmpty()) "None ✓" else ssl.weakCiphers.size.toString()
                        ).forEach { (label, value) ->
                            SslRow(label, value)
                        }
                    }
                }
            }
        }
    }
}

fun gradeColor(grade: String): Color = when (grade) {
    "A+", "A" -> NeonGreen
    "B" -> InfoCyan
    "C", "D" -> WarningAmber
    else -> ErrorRed
}

@Composable
fun SslRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontFamily = FontFamily.Monospace, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(value, fontFamily = FontFamily.Monospace, color = TextPrimary, fontSize = 11.sp, modifier = Modifier.weight(2f))
    }
    Divider(color = SurfaceVariant, thickness = 0.5.dp)
}

// ─── WIFI SCANNER ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiScannerScreen(isRooted: Boolean, onBack: () -> Unit) {
    ReconScreen(title = "WIFI SCANNER", onBack = onBack) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                "WiFi scanning requires location permission.\nGranted via Android Settings > App Permissions.",
                fontFamily = FontFamily.Monospace,
                color = WarningAmber,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            if (isRooted) {
                ReconCard {
                    Text("ROOT FEATURES AVAILABLE", fontFamily = FontFamily.Monospace, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("• ARP table — connected clients\n• Monitor mode toggle\n• Raw packet capture", fontFamily = FontFamily.Monospace, color = TextSecondary, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "WiFi scanning is context-dependent.\nInitialize from within the app on device.",
                fontFamily = FontFamily.Monospace,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

// ─── TRACEROUTE ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TracerouteScreen(isRooted: Boolean, onBack: () -> Unit) {
    var target by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    var hops by remember { mutableStateOf<List<String>>(emptyList()) }
    val scope = rememberCoroutineScope()

    ReconScreen(title = "TRACEROUTE", onBack = onBack) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            ReconCard {
                ReconTextField(value = target, onValueChange = { target = it }, label = "TARGET HOST / IP")
                Spacer(Modifier.height(12.dp))
                ScanButton(isScanning = isRunning, label = "TRACE ROUTE") {
                    isRunning = true; hops = emptyList()
                    scope.launch(Dispatchers.IO) {
                        try {
                            val cmd = if (isRooted) "su -c 'traceroute $target'" else "traceroute $target"
                            val process = Runtime.getRuntime().exec(cmd)
                            process.inputStream.bufferedReader().forEachLine { line ->
                                if (line.isNotBlank()) hops = hops + line
                            }
                        } catch (e: Exception) {
                            hops = listOf("Error: ${e.message}")
                        }
                        isRunning = false
                    }
                }
            }
            if (isRunning) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), color = NeonGreen, trackColor = SurfaceVariant)
            SectionHeader("HOPS (${hops.size})")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(hops.withIndex().toList()) { (index, hop) ->
                    ReconResultCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}".padStart(3), fontFamily = FontFamily.Monospace, color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(12.dp))
                            Text(hop, fontFamily = FontFamily.Monospace, color = TextPrimary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─── SHARED COMPONENTS ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReconScreen(title: String, onBack: () -> Unit, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontFamily = FontFamily.Monospace, color = NeonGreen, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = NeonGreen) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = BackgroundDark,
        content = content
    )
}

@Composable
fun ReconCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun ReconResultCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(12.dp)) { content() }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(text, fontFamily = FontFamily.Monospace, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
fun ScanButton(isScanning: Boolean, label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(44.dp),
        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = BackgroundDark),
        shape = RoundedCornerShape(4.dp),
        enabled = !isScanning
    ) {
        if (isScanning) { CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BackgroundDark, strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
        Text(if (isScanning) "RUNNING..." else "[ $label ]", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}
