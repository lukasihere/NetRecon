package com.netrecon.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netrecon.core.root.RootChecker
import com.netrecon.ui.theme.*
import kotlinx.coroutines.*

@Composable
fun OnboardingScreen(onContinue: (Boolean) -> Unit) {
    var step by remember { mutableStateOf(0) }
    var rootDetected by remember { mutableStateOf(false) }
    var rootConfirmed by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        delay(500)
        isChecking = true
        withContext(Dispatchers.IO) {
            rootDetected = RootChecker.isDeviceRooted()
            rootConfirmed = if (rootDetected) RootChecker.canExecuteRoot() else false
        }
        isChecking = false
        step = 1
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo / Title
            Text(
                text = "NET",
                fontSize = 52.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = NeonGreen
            )
            Text(
                text = "RECON",
                fontSize = 52.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )
            Text(
                text = "Professional Network Analysis",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            AnimatedVisibility(visible = step == 0 || isChecking) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = NeonGreen, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "[ SCANNING DEVICE... ]",
                        fontFamily = FontFamily.Monospace,
                        color = NeonGreen,
                        fontSize = 12.sp
                    )
                }
            }

            AnimatedVisibility(visible = step == 1) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Root status card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (rootDetected) NeonGreen else TextSecondary)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (rootDetected) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (rootDetected) NeonGreen else ErrorRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "ROOT STATUS",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (rootDetected) {
                                    if (rootConfirmed) "✓ Root access confirmed — Full capabilities available"
                                    else "⚠ Root detected but SU access denied"
                                } else "✗ No root detected — Standard mode only",
                                fontFamily = FontFamily.Monospace,
                                color = if (rootDetected && rootConfirmed) NeonGreen else if (rootDetected) WarningAmber else TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Text(
                        "SELECT OPERATING MODE",
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Mode selection
                    ModeCard(
                        title = "ROOT MODE",
                        description = "Full packet capture, ARP scanning,\nraw sockets, monitor mode",
                        features = listOf("Port scan (all)", "WiFi monitor", "ARP table", "Raw sockets", "Packet sniff"),
                        isSelected = selectedMode == true,
                        isAvailable = rootConfirmed,
                        color = NeonGreen,
                        onClick = { if (rootConfirmed) selectedMode = true }
                    )

                    Spacer(Modifier.height(12.dp))

                    ModeCard(
                        title = "STANDARD MODE",
                        description = "DNS lookup, SSL inspection,\nTCP connect scan, WiFi info",
                        features = listOf("TCP scan", "DNS enum", "SSL check", "WHOIS", "Traceroute"),
                        isSelected = selectedMode == false,
                        isAvailable = true,
                        color = InfoCyan,
                        onClick = { selectedMode = false }
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = { selectedMode?.let { onContinue(it) } },
                        enabled = selectedMode != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            contentColor = BackgroundDark,
                            disabledContainerColor = SurfaceVariant
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "[ INITIALIZE ]",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModeCard(
    title: String,
    description: String,
    features: List<String>,
    isSelected: Boolean,
    isAvailable: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isAvailable, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.1f) else SurfaceDark
        ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) color else if (isAvailable) color.copy(alpha = 0.3f) else Color(0xFF333333)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isAvailable) color else TextSecondary,
                    fontSize = 14.sp
                )
                if (!isAvailable) {
                    Text(
                        "UNAVAILABLE",
                        fontFamily = FontFamily.Monospace,
                        color = ErrorRed,
                        fontSize = 9.sp
                    )
                } else if (isSelected) {
                    Icon(Icons.Default.RadioButtonChecked, null, tint = color, modifier = Modifier.size(16.dp))
                } else {
                    Icon(Icons.Default.RadioButtonUnchecked, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                description,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                features.forEach { feature ->
                    Text(
                        feature,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = if (isAvailable) color else TextSecondary,
                        modifier = Modifier
                            .background(
                                if (isAvailable) color.copy(alpha = 0.1f) else SurfaceVariant,
                                RoundedCornerShape(2.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
