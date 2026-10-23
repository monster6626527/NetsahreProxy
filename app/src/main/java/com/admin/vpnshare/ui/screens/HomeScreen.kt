package com.admin.vpnshare.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.admin.vpnshare.LanguageManager
import com.admin.vpnshare.ThemeManager
import com.admin.vpnshare.proxy.UsbStatus
import com.admin.vpnshare.ui.theme.*
import com.admin.vpnshare.ui.viewmodel.ProxyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ProxyViewModel,
    onOpenDrawer: () -> Unit
) {
    val context = LocalContext.current
    val isHttpRunning by viewModel.isHttpRunning.collectAsState()
    val isSocksRunning by viewModel.isSocksRunning.collectAsState()
    val isStarting by viewModel.isStarting.collectAsState()
    val httpPort by viewModel.httpPort.collectAsState()
    val socksPort by viewModel.socksPort.collectAsState()
    val downloadSpeed by viewModel.downloadSpeed.collectAsState()
    val uploadSpeed by viewModel.uploadSpeed.collectAsState()
    val activeConnections by viewModel.activeConnections.collectAsState()
    val networkInterfaces by viewModel.networkInterfaces.collectAsState()
    val historyDown by viewModel.speedHistoryDown.collectAsState()
    val historyUp by viewModel.speedHistoryUp.collectAsState()
    val usbStatus by viewModel.usbStatus.collectAsState()
    val latencyMs by viewModel.latencyMs.collectAsState()
    val signalBars by viewModel.signalBars.collectAsState()
    val portError by viewModel.portError.collectAsState()

    val isRunning = isHttpRunning || isSocksRunning

    val activeIp = remember(networkInterfaces) {
        viewModel.getBestIP(networkInterfaces)
    }

    val activeIfaceName = remember(networkInterfaces) {
        networkInterfaces.firstOrNull { !it.isLoopback && it.ipAddress != "127.0.0.1" }?.name ?: "wlan0"
    }

    var httpPortText by remember(httpPort) { mutableStateOf(httpPort.toString()) }
    var socksPortText by remember(socksPort) { mutableStateOf(socksPort.toString()) }
    var usbExpanded by remember { mutableStateOf(false) }
    var themeChangeBanner by remember { mutableStateOf<String?>(null) }
    var themeChangeKey by remember { mutableStateOf(0) }

    LaunchedEffect(themeChangeKey) {
        if (themeChangeKey > 0) {
            kotlinx.coroutines.delay(2200)
            themeChangeBanner = null
        }
    }

    val scrollState = rememberScrollState()

    // Listen for data milestones
    LaunchedEffect(Unit) {
        viewModel.dataMilestoneEvent.collect { alertMessage ->
            Toast.makeText(context, alertMessage, Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        // SCROLLABLE CONTENT (Flows smoothly behind floating top bar and floating bottom bar)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Space reserved for the top floating glass bar so cards don't overlap initial view
            Spacer(modifier = Modifier.height(68.dp))

            // Port Error Banner
            AnimatedVisibility(
                visible = portError != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                portError?.let { err ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = NeonRed.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = NeonRed)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = err,
                                    fontSize = 12.sp,
                                    fontFamily = InterFont,
                                    color = TextPrimary
                                )
                            }
                            IconButton(onClick = { viewModel.clearPortError() }) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            val sharingMode by viewModel.sharingMode.collectAsState()
            val shizukuSessionState by viewModel.shizukuSessionState.collectAsState()
            val isDirectActive = shizukuSessionState.status == com.admin.vpnshare.shizuku.UiStatus.CONNECTED

            // SHARING MODE SELECTOR (Segmented Capsule)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                shape = RoundedCornerShape(14.dp),
                color = SurfaceVariantColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isDirect = sharingMode == "DIRECT"

                    // Proxy Mode Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isDirect) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { viewModel.setSharingMode("PROXY") }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Router,
                                contentDescription = null,
                                tint = if (!isDirect) NeonCyan else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = LanguageManager.get("method_proxy"),
                                fontSize = 12.sp,
                                fontWeight = if (!isDirect) FontWeight.Bold else FontWeight.Normal,
                                color = if (!isDirect) NeonCyan else TextSecondary
                            )
                        }
                    }

                    // Shizuku Direct Mode Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDirect) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { viewModel.setSharingMode("DIRECT") }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.WifiTethering,
                                contentDescription = null,
                                tint = if (isDirect) NeonCyan else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = LanguageManager.get("method_direct"),
                                fontSize = 12.sp,
                                fontWeight = if (isDirect) FontWeight.Bold else FontWeight.Normal,
                                color = if (isDirect) NeonCyan else TextSecondary
                            )
                        }
                    }
                }
            }

            // Direct Mode Card (Shown when DIRECT selected or when tunnel is actively running)
            if (sharingMode == "DIRECT" || isDirectActive) {
                ShizukuDirectTetherCard(viewModel = viewModel)
            }

            // In PROXY Mode (or as secondary when direct is idle): Show Hero Power Button
            if (sharingMode == "PROXY" && !isDirectActive) {
                HeroPowerButton(
                    isRunning = isHttpRunning,
                    isStarting = isStarting,
                    onToggle = {
                        val hPort = httpPortText.toIntOrNull() ?: 8080
                        viewModel.toggleHttpProxy(context, hPort)
                    }
                )
            }

            // ACTIVE INTERFACE CARD WITH SIGNAL BARS & LATENCY
            ActiveInterfaceCard(
                ipAddress = activeIp,
                interfaceName = activeIfaceName,
                connectedClients = activeConnections,
                signalBars = signalBars,
                latencyMs = latencyMs,
                onLatencyClick = {
                    Toast.makeText(context, "Network Latency: ${latencyMs}ms (Gateway 8.8.8.8)", Toast.LENGTH_SHORT).show()
                }
            )

            // SPEED SHARING METRICS (SIDE-BY-SIDE)
            SpeedSharingRow(
                downloadSpeedStr = viewModel.formatSpeed(downloadSpeed),
                uploadSpeedStr = viewModel.formatSpeed(uploadSpeed)
            )

            // LIVE BANDWIDTH CANVAS CHART
            LiveBandwidthChart(
                downloadHistory = historyDown,
                uploadHistory = historyUp,
                currentDownSpeed = viewModel.formatSpeed(downloadSpeed),
                currentUpSpeed = viewModel.formatSpeed(uploadSpeed)
            )

            // SERVER CONFIGURATION CARD
            ServerConfigurationCard(
                isHttpRunning = isHttpRunning,
                isSocksRunning = isSocksRunning,
                httpPortText = httpPortText,
                socksPortText = socksPortText,
                onHttpPortChange = {
                    httpPortText = it
                    it.toIntOrNull()?.let { p -> viewModel.setHttpPort(p) }
                },
                onSocksPortChange = {
                    socksPortText = it
                    it.toIntOrNull()?.let { p -> viewModel.setSocksPort(p) }
                },
                onToggleHttp = {
                    val port = httpPortText.toIntOrNull() ?: 8080
                    viewModel.toggleHttpProxy(context, port)
                },
                onToggleSocks = {
                    val port = socksPortText.toIntOrNull() ?: 1080
                    viewModel.toggleSocksProxy(context, port)
                }
            )

            // USB TETHERING CARD
            UsbTetheringCard(
                usbStatus = usbStatus,
                expanded = usbExpanded,
                onToggleExpand = { usbExpanded = !usbExpanded }
            )

            Spacer(modifier = Modifier.height(100.dp))
        }

        // TOP FLOATING GLASS ISLANDS (Transparent background with separated capsules)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val isDark = ThemeManager.isDarkMode

            // LEFT CAPSULE: Menu Button
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = if (isDark) 14.dp else 6.dp,
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = Color.Black,
                        spotColor = if (isDark) NeonCyan.copy(alpha = 0.3f) else Color(0x18000000)
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                GlassBgDark,
                                GlassBgBase
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = if (isDark) {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0x5500E5FF),
                                    Color(0x228000FF),
                                    Color(0x5500E5FF)
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0x25000000),
                                    Color(0x15000000),
                                    Color(0x25000000)
                                )
                            )
                        },
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onOpenDrawer() }
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = StatusCyan,
                    modifier = Modifier.size(22.dp)
                )
            }

            // CENTER CAPSULE: App Brand & Name
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = if (isDark) 14.dp else 6.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = Color.Black,
                        spotColor = if (isDark) NeonCyan.copy(alpha = 0.3f) else Color(0x18000000)
                    )
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                GlassBgDark,
                                GlassBgBase
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = if (isDark) {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0x6600E5FF),
                                    Color(0x449D4EDD),
                                    Color(0x6600E5FF)
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0x330284C7),
                                    Color(0x227C3AED),
                                    Color(0x330284C7)
                                )
                            )
                        },
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Net",
                            fontSize = 19.sp,
                            fontFamily = RajdhaniFont,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Share",
                            fontSize = 19.sp,
                            fontFamily = RajdhaniFont,
                            fontWeight = FontWeight.Bold,
                            color = StatusCyan
                        )
                        Text(
                            text = " Proxy",
                            fontSize = 19.sp,
                            fontFamily = RajdhaniFont,
                            fontWeight = FontWeight.Normal,
                            color = TextPrimary.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "BETA",
                            fontSize = 8.5.sp,
                            fontFamily = SpaceMonoFont,
                            fontWeight = FontWeight.ExtraBold,
                            color = StatusAmber,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier
                                .align(Alignment.Top)
                                .offset(y = (-2).dp)
                        )
                    }
                    Text(
                        text = "PROXY SERVER",
                        fontSize = 9.sp,
                        fontFamily = SpaceMonoFont,
                        color = TextSecondary,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            // RIGHT CAPSULE: Status Indicator + Theme Mode Toggle
            val infiniteTransition = rememberInfiniteTransition(label = "appBarDot")
            val dotScale by infiniteTransition.animateFloat(
                initialValue = 0.85f,
                targetValue = 1.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dotScale"
            )

            Box(
                modifier = Modifier
                    .shadow(
                        elevation = if (isDark) 14.dp else 6.dp,
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = Color.Black,
                        spotColor = if (isDark) (if (isRunning) NeonGreen else NeonCyan).copy(alpha = 0.3f) else Color(0x18000000)
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                GlassBgDark,
                                GlassBgBase
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = if (isDark) {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    (if (isRunning) NeonGreen else NeonCyan).copy(alpha = 0.4f),
                                    Color(0x228000FF),
                                    (if (isRunning) NeonGreen else NeonCyan).copy(alpha = 0.4f)
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    (if (isRunning) Color(0x40059669) else Color(0x300284C7)),
                                    Color(0x207C3AED),
                                    (if (isRunning) Color(0x40059669) else Color(0x300284C7))
                                )
                            )
                        },
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pulsing Status Dot with Glow
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .scale(if (isRunning) dotScale else 1.0f)
                            .clip(CircleShape)
                            .background(if (isRunning) StatusGreen else StatusRed)
                    )

                    // Vertical hairline divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(14.dp)
                            .background(if (isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.15f))
                    )

                    // SLEEK CYBER DAY / NIGHT SWITCH
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isDark)
                                    Brush.horizontalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B)))
                                else
                                    Brush.horizontalGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7)))
                            )
                            .border(
                                width = 1.dp,
                                color = if (isDark) NeonCyan.copy(alpha = 0.5f) else Color(0xFFD97706).copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                ThemeManager.toggleTheme(context)
                                val msg = if (ThemeManager.isDarkMode) "🌙  Dark Theme Activated ✓" else "☀️ Light Theme Activated ✓"
                                themeChangeBanner = msg
                                themeChangeKey++
                            }
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f)
                                .align(if (isDark) Alignment.CenterStart else Alignment.CenterEnd)
                                .shadow(4.dp, CircleShape)
                                .clip(CircleShape)
                                .background(
                                    if (isDark)
                                        Brush.radialGradient(listOf(NeonCyan, Color(0xFF0091AA)))
                                    else
                                        Brush.radialGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.WbSunny,
                                contentDescription = "Toggle Theme Mode",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }

        // ANIMATED THEME ACTIVATION PILL BANNER (ELEGANT DESIGN WITH SMOOTH ANIMATION)
        AnimatedVisibility(
            visible = themeChangeBanner != null,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 115.dp)
        ) {
            themeChangeBanner?.let { bannerText ->
                val isDarkThemeNow = ThemeManager.isDarkMode
                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = Color.Black,
                            spotColor = if (isDarkThemeNow) NeonCyan.copy(alpha = 0.5f) else Color(0xFFF59E0B).copy(alpha = 0.35f)
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.horizontalGradient(
                                if (isDarkThemeNow)
                                    listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                                else
                                    listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC))
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.horizontalGradient(
                                if (isDarkThemeNow)
                                    listOf(NeonCyan, Color(0xFF8B5CF6), NeonCyan)
                                else
                                    listOf(Color(0xFFF59E0B), Color(0xFFD97706), Color(0xFFF59E0B))
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = bannerText,
                        fontSize = 14.sp,
                        fontFamily = RajdhaniFont,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = if (isDarkThemeNow) Color(0xFFE2E8F0) else Color(0xFF0F172A)
                    )
                }
            }
        }
    }
}

@Composable
fun HeroPowerButton(
    isRunning: Boolean,
    isStarting: Boolean,
    onToggle: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "powerButtonAnimation")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val isDark = ThemeManager.isDarkMode
    val amberColor = StatusAmber
    val cyanColor = StatusCyan
    val greenColor = StatusGreen
    val redColor = StatusRed

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(160.dp)
                .scale(if (isRunning) pulseScale else 1.0f)
                .clip(CircleShape)
                .clickable { onToggle() }
        ) {
            // Ambient Outer Glow Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 3.5.dp.toPx()
                when {
                    isStarting -> {
                        val dashPath = PathEffect.dashPathEffect(floatArrayOf(24f, 16f), 0f)
                        drawArc(
                            color = amberColor,
                            startAngle = rotationAngle,
                            sweepAngle = 280f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, pathEffect = dashPath)
                        )
                    }
                    isRunning -> {
                        drawCircle(
                            color = cyanColor.copy(alpha = glowAlpha),
                            style = Stroke(width = strokeWidth)
                        )
                        drawCircle(
                            color = cyanColor.copy(alpha = if (isDark) 0.15f * glowAlpha else 0.08f * glowAlpha),
                            radius = size.minDimension / 2f
                        )
                    }
                    else -> {
                        drawCircle(
                            color = if (isDark) redColor.copy(alpha = 0.35f) else redColor.copy(alpha = 0.2f),
                            style = Stroke(width = strokeWidth)
                        )
                    }
                }
            }

            // Inner Button Core
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .clip(CircleShape)
                    .background(SurfaceColor)
                    .border(
                        width = 1.5.dp,
                        color = if (isRunning) cyanColor.copy(alpha = 0.7f) else CardBorderColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Power",
                    tint = when {
                        isStarting -> amberColor
                        isRunning -> greenColor
                        else -> TextPrimary
                    },
                    modifier = Modifier.size(50.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = when {
                isStarting -> "INITIALIZING SERVERS..."
                isRunning -> LanguageManager.get("server_online")
                else -> LanguageManager.get("server_offline")
            },
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = RajdhaniFont,
            color = when {
                isStarting -> amberColor
                isRunning -> greenColor
                else -> TextSecondary
            },
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
fun ActiveInterfaceCard(
    ipAddress: String,
    interfaceName: String,
    connectedClients: Int,
    signalBars: Int,
    latencyMs: Long,
    onLatencyClick: () -> Unit
) {
    val isDark = ThemeManager.isDarkMode
    val cyanColor = StatusCyan
    val greenColor = StatusGreen
    val amberColor = StatusAmber
    val redColor = StatusRed

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SurfaceColor, SurfaceVariantColor)
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .background(cyanColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = LanguageManager.get("active_interface"),
                        fontSize = 10.sp,
                        fontFamily = SpaceMonoFont,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = cyanColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = interfaceName,
                            fontSize = 11.sp,
                            fontFamily = SpaceMonoFont,
                            color = cyanColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = ipAddress,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceMonoFont,
                    color = TextPrimary
                )

                Text(
                    text = LanguageManager.get("connect_step_3"),
                    fontSize = 12.sp,
                    fontFamily = InterFont,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LATENCY INDICATOR
                    Column {
                        Text(
                            text = LanguageManager.get("latency"),
                            fontSize = 10.sp,
                            fontFamily = SpaceMonoFont,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onLatencyClick() }
                                .padding(vertical = 2.dp)
                        ) {
                            val barColor = when (signalBars) {
                                4 -> greenColor
                                3 -> cyanColor
                                2 -> amberColor
                                else -> redColor
                            }
                            listOf(4.dp, 7.dp, 10.dp, 13.dp).forEachIndexed { index, height ->
                                val active = (index + 1) <= signalBars
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(height)
                                        .background(if (active) barColor else if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.12f))
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${latencyMs}ms",
                                fontSize = 13.sp,
                                fontFamily = SpaceMonoFont,
                                fontWeight = FontWeight.Bold,
                                color = barColor
                            )
                        }
                    }

                    // CLIENTS COUNT
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = LanguageManager.get("clients"),
                            fontSize = 10.sp,
                            fontFamily = SpaceMonoFont,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$connectedClients",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceMonoFont,
                            color = greenColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpeedSharingRow(
    downloadSpeedStr: String,
    uploadSpeedStr: String
) {
    val cyanColor = StatusCyan
    val purpleColor = StatusPurple

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Download Card (Cyan)
        Card(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceColor)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(cyanColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Download", tint = cyanColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(LanguageManager.get("download"), fontSize = 9.sp, fontFamily = SpaceMonoFont, color = TextSecondary)
                    Text(downloadSpeedStr, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = SpaceMonoFont, color = cyanColor)
                }
            }
        }

        // Upload Card (Purple)
        Card(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceColor)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(purpleColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Upload", tint = purpleColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(LanguageManager.get("upload"), fontSize = 9.sp, fontFamily = SpaceMonoFont, color = TextSecondary)
                    Text(uploadSpeedStr, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = SpaceMonoFont, color = purpleColor)
                }
            }
        }
    }
}

@Composable
fun LiveBandwidthChart(
    downloadHistory: List<Float>,
    uploadHistory: List<Float>,
    currentDownSpeed: String,
    currentUpSpeed: String
) {
    val isDark = ThemeManager.isDarkMode
    val cyanColor = StatusCyan
    val purpleColor = StatusPurple
    val greenColor = StatusGreen

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(greenColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE BANDWIDTH CHART",
                            fontSize = 11.sp,
                            fontFamily = SpaceMonoFont,
                            color = TextPrimary,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "Real-time 60s throughput graph",
                        fontSize = 11.sp,
                        fontFamily = InterFont,
                        color = TextSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Down",
                            tint = cyanColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = currentDownSpeed,
                            fontSize = 12.sp,
                            fontFamily = SpaceMonoFont,
                            fontWeight = FontWeight.Bold,
                            color = cyanColor
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Up",
                            tint = purpleColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = currentUpSpeed,
                            fontSize = 12.sp,
                            fontFamily = SpaceMonoFont,
                            fontWeight = FontWeight.Bold,
                            color = purpleColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Chart with Smooth Bezier & Gradient Fill
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                val width = size.width
                val height = size.height

                // Draw Horizontal Grid Lines
                val gridRows = 3
                for (i in 0..gridRows) {
                    val y = height * (i.toFloat() / gridRows)
                    drawLine(
                        color = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.06f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                }

                val maxVal = maxOf(
                    downloadHistory.maxOrNull() ?: 1f,
                    uploadHistory.maxOrNull() ?: 1f,
                    50f // Minimum scale 50 KB/s
                )

                fun drawSmoothCurve(points: List<Float>, strokeColor: Color, fillColor: Color) {
                    if (points.size < 2) return

                    val stepX = width / (points.size - 1)
                    val linePath = Path()
                    val fillPath = Path()

                    val firstY = (1f - (points[0] / maxVal).coerceIn(0f, 1f)) * (height - 12f) + 6f
                    linePath.moveTo(0f, firstY)
                    fillPath.moveTo(0f, height)
                    fillPath.lineTo(0f, firstY)

                    for (i in 0 until points.size - 1) {
                        val currentX = i * stepX
                        val currentY = (1f - (points[i] / maxVal).coerceIn(0f, 1f)) * (height - 12f) + 6f
                        val nextX = (i + 1) * stepX
                        val nextY = (1f - (points[i + 1] / maxVal).coerceIn(0f, 1f)) * (height - 12f) + 6f

                        val controlX1 = currentX + (nextX - currentX) / 2f
                        val controlY1 = currentY
                        val controlX2 = currentX + (nextX - currentX) / 2f
                        val controlY2 = nextY

                        linePath.cubicTo(controlX1, controlY1, controlX2, controlY2, nextX, nextY)
                        fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, nextX, nextY)
                    }

                    fillPath.lineTo(width, height)
                    fillPath.close()

                    // Draw soft translucent area fill
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(fillColor.copy(alpha = if (isDark) 0.25f else 0.15f), fillColor.copy(alpha = 0.0f)),
                            startY = 0f,
                            endY = height
                        )
                    )

                    // Draw the stroke curve
                    drawPath(
                        path = linePath,
                        color = strokeColor,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }

                // Draw Download (Cyan) and Upload (Purple)
                drawSmoothCurve(downloadHistory, cyanColor, cyanColor)
                drawSmoothCurve(uploadHistory, purpleColor, purpleColor)
            }
        }
    }
}

@Composable
fun ServerConfigurationCard(
    isHttpRunning: Boolean,
    isSocksRunning: Boolean,
    httpPortText: String,
    socksPortText: String,
    onHttpPortChange: (String) -> Unit,
    onSocksPortChange: (String) -> Unit,
    onToggleHttp: () -> Unit,
    onToggleSocks: () -> Unit
) {
    val isDark = ThemeManager.isDarkMode
    val cyanColor = StatusCyan
    val purpleColor = StatusPurple

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "SERVER CONFIGURATION",
                fontSize = 12.sp,
                fontFamily = SpaceMonoFont,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            // HTTP Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(cyanColor, Color(0xFF0088FF)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "HTTP",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "HTTP Proxy",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = InterFont,
                            color = TextPrimary
                        )
                        Text(
                            text = "Port: $httpPortText",
                            fontSize = 12.sp,
                            fontFamily = SpaceMonoFont,
                            color = TextSecondary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = httpPortText,
                        onValueChange = onHttpPortChange,
                        modifier = Modifier.width(72.dp),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = SpaceMonoFont,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = cyanColor,
                            unfocusedBorderColor = BorderCyan,
                            focusedContainerColor = SurfaceVariantColor,
                            unfocusedContainerColor = SurfaceVariantColor
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Switch(
                        checked = isHttpRunning,
                        onCheckedChange = { onToggleHttp() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BgColor,
                            checkedTrackColor = cyanColor,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = SurfaceVariantColor
                        )
                    )
                }
            }

            HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f))

            // SOCKS5 Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(purpleColor, Color(0xFF6B21A8)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "SOCKS5",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "SOCKS5 Proxy",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = InterFont,
                            color = TextPrimary
                        )
                        Text(
                            text = "Port: $socksPortText",
                            fontSize = 12.sp,
                            fontFamily = SpaceMonoFont,
                            color = TextSecondary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = socksPortText,
                        onValueChange = onSocksPortChange,
                        modifier = Modifier.width(72.dp),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = SpaceMonoFont,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = purpleColor,
                            unfocusedBorderColor = BorderCyan,
                            focusedContainerColor = SurfaceVariantColor,
                            unfocusedContainerColor = SurfaceVariantColor
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Switch(
                        checked = isSocksRunning,
                        onCheckedChange = { onToggleSocks() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BgColor,
                            checkedTrackColor = purpleColor,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = SurfaceVariantColor
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun UsbTetheringCard(
    usbStatus: UsbStatus,
    expanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val isUsbConnected = usbStatus is UsbStatus.Connected
    val isDark = ThemeManager.isDarkMode
    val greenColor = StatusGreen
    val purpleColor = StatusPurple
    val redColor = StatusRed

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isUsbConnected) greenColor.copy(alpha = 0.5f) else CardBorderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onToggleExpand() }
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isUsbConnected) greenColor.copy(alpha = 0.2f) else purpleColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Usb,
                            contentDescription = "USB",
                            tint = if (isUsbConnected) greenColor else purpleColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "USB Tethering (High-Speed Cable)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFont,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isUsbConnected) "VPN traffic routing via USB cable" else "Connect USB cable for zero latency",
                            fontSize = 11.sp,
                            fontFamily = InterFont,
                            color = if (isUsbConnected) greenColor else TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Pulsing USB Status Dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isUsbConnected) greenColor else redColor)
                    )

                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(4.dp))

                    if (isUsbConnected) {
                        val connected = usbStatus as UsbStatus.Connected
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = greenColor.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = greenColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "USB Active: ${connected.interfaceName} (${connected.ipAddress})",
                                    fontSize = 12.sp,
                                    fontFamily = SpaceMonoFont,
                                    color = greenColor
                                )
                            }
                        }
                    }

                    Text(
                        text = "Step-by-Step Guide:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusCyan,
                        fontFamily = InterFont
                    )

                    val step1Status = if (isUsbConnected) "✓ USB Connected" else "✗ Not Connected"
                    val steps = listOf(
                        "1. Connect Android to PC/Console via USB cable. ($step1Status)",
                        "2. Turn on 'USB Tethering' in Android Settings -> Network -> Hotspot.",
                        "3. Check the active IP for 'rndis0' or 'usb0' on this app's home card.",
                        "4. Configure client proxy settings with that IP and port 8080/1080."
                    )

                    steps.forEach { step ->
                        Text(
                            text = step,
                            fontSize = 12.sp,
                            fontFamily = InterFont,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
