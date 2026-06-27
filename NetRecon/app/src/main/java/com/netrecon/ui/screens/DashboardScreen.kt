package com.netrecon.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netrecon.ui.Screen
import com.netrecon.ui.theme.*

data class ToolItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
    val color: Color,
    val rootOnly: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(isRooted: Boolean, onNavigate: (String) -> Unit) {
    val tools = listOf(
        ToolItem("PORT SCAN", "TCP/UDP port scanner", Icons.Default.NetworkCheck, Screen.PortScanner.route, NeonGreen),
        ToolItem("HOST DISCO", "Subnet host discovery", Icons.Default.DeviceHub, Screen.HostDiscovery.route, NeonGreen),
        ToolItem("DNS ENUM", "Subdomain & record enum", Icons.Default.Dns, Screen.DnsEnum.route, InfoCyan),
        ToolItem("SSL CHECK", "Certificate inspector", Icons.Default.Lock, Screen.SslInspector.route, InfoCyan),
        ToolItem("WIFI SCAN", "Network scanner", Icons.Default.Wifi, Screen.WifiScanner.route, WarningAmber, rootOnly = false),
        ToolItem("TRACEROUTE", "Hop path analysis", Icons.Default.Route, Screen.Traceroute.route, WarningAmber),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "NETRECON",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            color = NeonGreen,
                            fontSize = 20.sp
                        )
                        Text(
                            if (isRooted) "[ ROOT MODE ACTIVE ]" else "[ STANDARD MODE ]",
                            fontFamily = FontFamily.Monospace,
                            color = if (isRooted) NeonGreen else InfoCyan,
                            fontSize = 10.sp
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .background(
                                if (isRooted) NeonGreen.copy(alpha = 0.1f) else InfoCyan.copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            if (isRooted) "ROOT" else "STD",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (isRooted) NeonGreen else InfoCyan,
                            fontSize = 11.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "SELECT TOOL",
                fontFamily = FontFamily.Monospace,
                color = TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tools) { tool ->
                    val isAvailable = !tool.rootOnly || isRooted
                    ToolCard(
                        tool = tool,
                        isAvailable = isAvailable,
                        onClick = { if (isAvailable) onNavigate(tool.route) }
                    )
                }
            }
        }
    }
}

@Composable
fun ToolCard(tool: ToolItem, isAvailable: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isAvailable) tool.color.copy(alpha = 0.4f) else Color(0xFF333333))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                tool.icon,
                contentDescription = null,
                tint = if (isAvailable) tool.color else TextSecondary,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    tool.title,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isAvailable) tool.color else TextSecondary,
                    fontSize = 13.sp
                )
                Text(
                    tool.subtitle,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary,
                    fontSize = 10.sp
                )
                if (tool.rootOnly) {
                    Text(
                        "ROOT ONLY",
                        fontFamily = FontFamily.Monospace,
                        color = if (isAvailable) NeonGreen else ErrorRed,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
