package com.admin.vpnshare.ui.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.admin.vpnshare.proxy.ProxyManager
import com.admin.vpnshare.proxy.ProxyService
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.admin.vpnshare.ui.screens.*
import com.admin.vpnshare.ui.theme.*
import com.admin.vpnshare.ui.viewmodel.ProxyViewModel
import kotlinx.coroutines.launch

enum class ScreenRoute(val route: String, val title: String, val icon: ImageVector) {
    HOME("home", "HOME", Icons.Default.Home),
    STATS("stats", "STATS", Icons.Default.BarChart),
    CLIENTS("clients", "CLIENTS", Icons.Default.Devices),
    LOGS("logs", "LOGS", Icons.Default.Terminal),
    CONFIG("config", "CONFIG", Icons.Default.Settings),
    QR("qr", "QR CODE", Icons.Default.QrCode)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavGraph(viewModel: ProxyViewModel) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("vpn_share_prefs", Context.MODE_PRIVATE) }

    var currentScreen by remember { mutableStateOf("splash") } // splash, onboarding, main
    var selectedRoute by remember { mutableStateOf("home") }

    var showDiagnosticsDialog by remember { mutableStateOf(false) }
    var showFaqDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val httpPort by viewModel.httpPort.collectAsState()
    val socksPort by viewModel.socksPort.collectAsState()

    if (showDiagnosticsDialog) {
        DiagnosticsDialog(
            httpPort = httpPort,
            socksPort = socksPort,
            onDismiss = { showDiagnosticsDialog = false }
        )
    }

    if (showFaqDialog) {
        FaqDialog(
            onDismiss = { showFaqDialog = false }
        )
    }

    when (currentScreen) {
        "splash" -> {
            SplashScreen(
                onFinish = {
                    val isFirstLaunch = sharedPrefs.getBoolean("is_first_launch", true)
                    if (isFirstLaunch) {
                        currentScreen = "onboarding"
                    } else {
                        currentScreen = "main"
                    }
                }
            )
        }
        "onboarding" -> {
            OnboardingScreen(
                onComplete = {
                    sharedPrefs.edit().putBoolean("is_first_launch", false).apply()
                    currentScreen = "main"
                }
            )
        }
        "main" -> {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    DrawerMenu(
                        selectedRoute = selectedRoute,
                        onNavigate = { selectedRoute = it },
                        onOpenDiagnostics = { showDiagnosticsDialog = true },
                        onOpenFaqs = { showFaqDialog = true },
                        onCloseDrawer = {
                            coroutineScope.launch { drawerState.close() }
                        },
                        onExitApp = {
                            try {
                                ProxyManager.stopAll(context)
                                val stopIntent = Intent(context, ProxyService::class.java).apply {
                                    action = ProxyService.ACTION_STOP
                                }
                                context.startService(stopIntent)
                                context.stopService(Intent(context, ProxyService::class.java))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            val activity = context as? Activity
                            activity?.finishAffinity()
                        }
                    )
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BgColor)
                ) {
                    // Full-screen content flowing behind the transparent navigation bar
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        when (selectedRoute) {
                            "home" -> HomeScreen(
                                viewModel = viewModel,
                                onOpenDrawer = {
                                    coroutineScope.launch { drawerState.open() }
                                }
                            )
                            "stats" -> StatsScreen(viewModel = viewModel)
                            "clients" -> ClientsScreen(viewModel = viewModel)
                            "logs" -> LogsScreen(viewModel = viewModel)
                            "config" -> ConfigScreen(viewModel = viewModel)
                            "qr" -> QrShareScreen(viewModel = viewModel)
                            else -> HomeScreen(
                                viewModel = viewModel,
                                onOpenDrawer = {
                                    coroutineScope.launch { drawerState.open() }
                                }
                            )
                        }
                    }

                    // Floating Glassmorphism Bottom Navigation Bar (Telegram-style edge-to-edge)
                    FloatingBottomNavigationBar(
                        selectedRoute = selectedRoute,
                        onNavigate = { selectedRoute = it },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}
