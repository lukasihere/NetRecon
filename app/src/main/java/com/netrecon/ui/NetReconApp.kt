package com.netrecon.ui

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.netrecon.ui.screens.*

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Dashboard : Screen("dashboard")
    object PortScanner : Screen("port_scanner")
    object HostDiscovery : Screen("host_discovery")
    object DnsEnum : Screen("dns_enum")
    object SslInspector : Screen("ssl_inspector")
    object WifiScanner : Screen("wifi_scanner")
    object Traceroute : Screen("traceroute")
}

@Composable
fun NetReconApp() {
    val navController = rememberNavController()
    var isRooted by remember { mutableStateOf(false) }

    NavHost(navController = navController, startDestination = Screen.Onboarding.route) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onContinue = { rootMode ->
                    isRooted = rootMode
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                isRooted = isRooted,
                onNavigate = { navController.navigate(it) }
            )
        }
        composable(Screen.PortScanner.route) {
            PortScannerScreen(isRooted = isRooted, onBack = { navController.popBackStack() })
        }
        composable(Screen.HostDiscovery.route) {
            HostDiscoveryScreen(isRooted = isRooted, onBack = { navController.popBackStack() })
        }
        composable(Screen.DnsEnum.route) {
            DnsEnumScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.SslInspector.route) {
            SslInspectorScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.WifiScanner.route) {
            WifiScannerScreen(isRooted = isRooted, onBack = { navController.popBackStack() })
        }
        composable(Screen.Traceroute.route) {
            TracerouteScreen(isRooted = isRooted, onBack = { navController.popBackStack() })
        }
    }
}
