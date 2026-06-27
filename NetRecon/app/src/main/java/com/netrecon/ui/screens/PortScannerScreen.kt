package com.netrecon.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netrecon.core.scanner.PortResult
import com.netrecon.core.scanner.PortScanner
import com.netrecon.ui.theme.*
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortScannerScreen(isRooted: Boolean, onBack: () -> Unit) {
    var target by remember { mutableStateOf("") }
    var startPort by remember { mutableStateOf("1") }
    var endPort by remember { mutableStateOf("1024") }
    var isScanning by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<PortResult>>(emptyList()) }
    var progress by remember { mutableStateOf(0f) }
    var statusText by remember { mutableStateOf("Ready") }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PORT SCANNER", fontFamily = FontFamily.Monospace, color = NeonGreen, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = NeonGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            // Input section
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ReconTextField(value = target, onValueChange = { target = it }, label = "TARGET HOST / IP")
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReconTextField(
                            value = startPort, onValueChange = { startPort = it },
                            label = "START PORT", modifier = Modifier.weight(1f),
                            keyboardType = KeyboardType.Number
                        )
                        ReconTextField(
                            value = endPort, onValueChange = { endPort = it },
                            label = "END PORT", modifier = Modifier.weight(1f),
                            keyboardType = KeyboardType.Number
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    // Quick presets
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple("TOP 100", "1", "100"),
                            Triple("COMMON", "1", "1024"),
                            Triple("FULL", "1", "65535")
                        ).forEach { (label, start, end) ->
                            OutlinedButton(
                                onClick = { startPort = start; endPort = end },
                                modifier = Modifier.weight(1f).height(32.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                                border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f))
                            ) {
                                Text(label, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (!isScanning && target.isNotBlank()) {
                                isScanning = true
                                results = emptyList()
                                scope.launch {
                                    val start = startPort.toIntOrNull() ?: 1
                                    val end = endPort.toIntOrNull() ?: 1024
                                    val total = end - start + 1
                                    var scanned = 0
                                    PortScanner.scanRange(
                                        host = target,
                                        startPort = start,
                                        endPort = end
                                    ) { result ->
                                        results = results + result
                                        scanned++
                                        progress = scanned.toFloat() / total
                                        statusText = "Scanning port ${result.port}..."
                                    }
                                    statusText = "Scan complete — ${results.size} open ports"
                                    isScanning = false
                                    progress = 1f
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = BackgroundDark),
                        shape = RoundedCornerShape(4.dp),
                        enabled = !isScanning && target.isNotBlank()
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BackgroundDark, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (isScanning) "SCANNING..." else "[ SCAN ]", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Progress
            if (isScanning || progress > 0f) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    color = NeonGreen,
                    trackColor = SurfaceVariant
                )
                Text(statusText, fontFamily = FontFamily.Monospace, color = NeonGreen, fontSize = 10.sp, modifier = Modifier.padding(bottom = 8.dp))
            }

            // Results
            Text(
                "OPEN PORTS (${results.size})",
                fontFamily = FontFamily.Monospace,
                color = TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(results) { result ->
                    PortResultRow(result)
                }
            }
        }
    }
}

@Composable
fun PortResultRow(result: PortResult) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${result.port}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen,
                    fontSize = 16.sp,
                    modifier = Modifier.width(56.dp)
                )
                Column {
                    Text(result.service, fontFamily = FontFamily.Monospace, color = TextPrimary, fontSize = 13.sp)
                    if (result.banner.isNotEmpty()) {
                        Text(result.banner, fontFamily = FontFamily.Monospace, color = TextSecondary, fontSize = 10.sp)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .background(NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("OPEN", fontFamily = FontFamily.Monospace, color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReconTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonGreen,
            unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
            focusedLabelColor = NeonGreen,
            unfocusedLabelColor = TextSecondary,
            cursorColor = NeonGreen,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(4.dp)
    )
}
