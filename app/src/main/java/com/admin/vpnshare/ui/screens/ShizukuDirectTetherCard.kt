package com.admin.vpnshare.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.admin.vpnshare.LanguageManager
import com.admin.vpnshare.ThemeManager
import com.admin.vpnshare.shizuku.*
import com.admin.vpnshare.ui.theme.*
import com.admin.vpnshare.ui.viewmodel.ProxyViewModel

/**
 * High-end Direct VPN Tethering Interface (Rootless Shizuku Engine).
 * Inspired by official Shizzi UX with ambient glowing tether glyph,
 * live telemetry dashboard, and multi-method selection (Hotspot, USB, Bluetooth).
 */
@Composable
fun ShizukuDirectTetherCard(
    viewModel: ProxyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shizukuState by viewModel.shizukuState.collectAsState()
    val sessionState by viewModel.shizukuSessionState.collectAsState()

    val isConnected = sessionState.status == UiStatus.CONNECTED
    val isLoading = sessionState.status == UiStatus.LOADING || sessionState.isBusy
    val isReady = shizukuState is ShizukuState.Ready

    val isDark = ThemeManager.isDarkMode
    val cyanColor = StatusCyan

    // Check Shizuku status when entering
    LaunchedEffect(Unit) {
        viewModel.refreshShizukuState()
    }

    // Breathing pulse animations
    val infiniteTransition = rememberInfiniteTransition(label = "directTetherHalo")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    val rotateAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotateAngle"
    )

    // Method selection state (Hotspot, USB, Bluetooth)
    var selectedMethod by remember { mutableStateOf("HOTSPOT") } // "HOTSPOT", "USB", "BT"

    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // MAIN HERO CARD (Shizzi Majestic Presentation)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isConnected) 18.dp else 6.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = Color.Black,
                    spotColor = if (isConnected) NeonCyan.copy(alpha = 0.45f) else Color(0x18000000)
                )
                .border(
                    width = if (isConnected) 1.5.dp else 1.dp,
                    brush = if (isConnected) {
                        Brush.horizontalGradient(listOf(NeonCyan, NeonGreen, NeonCyan))
                    } else {
                        Brush.horizontalGradient(
                            listOf(
                                if (isDark) Color(0x4400E5FF) else Color(0x300284C7),
                                if (isDark) Color(0x228000FF) else Color(0x207C3AED),
                                if (isDark) Color(0x4400E5FF) else Color(0x300284C7)
                            )
                        )
                    },
                    shape = RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) SurfaceColor else Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // TOP HEADER: Mode Title & Shizuku Status Chip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) NeonGreen else if (isReady) StatusAmber else NeonRed)
                        )
                        Text(
                            text = "ROOTLESS VPN ROUTING",
                            fontSize = 12.sp,
                            fontFamily = SpaceMonoFont,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 1.sp
                        )
                    }

                    // Shizuku Privilege Tier Chip
                    Surface(
                        color = when (shizukuState) {
                            is ShizukuState.Ready -> NeonGreen.copy(alpha = 0.15f)
                            is ShizukuState.PermissionRequired -> StatusAmber.copy(alpha = 0.15f)
                            else -> NeonRed.copy(alpha = 0.15f)
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            when (shizukuState) {
                                is ShizukuState.Ready -> NeonGreen.copy(alpha = 0.6f)
                                is ShizukuState.PermissionRequired -> StatusAmber.copy(alpha = 0.6f)
                                else -> NeonRed.copy(alpha = 0.6f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = when (shizukuState) {
                                    is ShizukuState.Ready -> Icons.Default.Verified
                                    is ShizukuState.PermissionRequired -> Icons.Default.Lock
                                    else -> Icons.Default.Cancel
                                },
                                contentDescription = null,
                                tint = when (shizukuState) {
                                    is ShizukuState.Ready -> NeonGreen
                                    is ShizukuState.PermissionRequired -> StatusAmber
                                    else -> NeonRed
                                },
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = when (shizukuState) {
                                    is ShizukuState.Ready -> (shizukuState as ShizukuState.Ready).let {
                                        if (it.isRoot) "ROOT" else "SHIZUKU"
                                    }
                                    is ShizukuState.PermissionRequired -> "NEEDS AUTH"
                                    ShizukuState.NotRunning -> "SERVICE OFF"
                                    ShizukuState.NotInstalled -> "NOT INSTALLED"
                                },
                                fontSize = 10.sp,
                                fontFamily = SpaceMonoFont,
                                fontWeight = FontWeight.Bold,
                                color = when (shizukuState) {
                                    is ShizukuState.Ready -> NeonGreen
                                    is ShizukuState.PermissionRequired -> StatusAmber
                                    else -> NeonRed
                                }
                            )
                        }
                    }
                }

                // REMEDY BANNER (If Shizuku needs permission or is not installed)
                AnimatedVisibility(
                    visible = !isReady,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (shizukuState) {
                                is ShizukuState.PermissionRequired -> StatusAmber.copy(alpha = 0.12f)
                                else -> NeonRed.copy(alpha = 0.10f)
                            }
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when (shizukuState) {
                                is ShizukuState.PermissionRequired -> StatusAmber.copy(alpha = 0.5f)
                                else -> NeonRed.copy(alpha = 0.4f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = when (shizukuState) {
                                        is ShizukuState.PermissionRequired -> LanguageManager.get("shizuku_permission_required")
                                        ShizukuState.NotRunning -> LanguageManager.get("shizuku_not_running")
                                        ShizukuState.NotInstalled -> LanguageManager.get("shizuku_not_installed")
                                        else -> ""
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = when (shizukuState) {
                                        is ShizukuState.PermissionRequired -> "Grant permission to share VPN with connected devices"
                                        ShizukuState.NotRunning -> "Start Shizuku service via Wireless Debugging"
                                        ShizukuState.NotInstalled -> "Install Shizuku to route VPN without manual proxy"
                                        else -> ""
                                    },
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    lineHeight = 14.sp
                                )
                            }

                            Button(
                                onClick = {
                                    if (shizukuState is ShizukuState.PermissionRequired) {
                                        viewModel.requestShizukuPermission()
                                    } else {
                                        viewModel.remedyShizuku(context)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when (shizukuState) {
                                        is ShizukuState.PermissionRequired -> StatusAmber
                                        else -> NeonCyan
                                    }
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = when (shizukuState) {
                                        is ShizukuState.PermissionRequired -> LanguageManager.get("shizuku_grant_btn")
                                        else -> LanguageManager.get("shizuku_open_btn")
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }

                // CENTER HERO: TETHERING GLYPH (Shizzi Masterpiece Icon with animated glowing halo)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(170.dp)
                        .padding(vertical = 4.dp)
                ) {
                    // Ambient Rotating Outer Ring
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(rotateAngle)
                    ) {
                        val strokeWidth = 2.dp.toPx()
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 14f), 0f)
                        drawCircle(
                            color = if (isConnected) NeonCyan.copy(alpha = glowAlpha) else cyanColor.copy(alpha = 0.25f),
                            style = Stroke(width = strokeWidth, pathEffect = dashEffect)
                        )
                    }

                    // Soft Radial Glow
                    Box(
                        modifier = Modifier
                            .size(135.dp)
                            .scale(if (isConnected) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        if (isConnected) NeonCyan.copy(alpha = 0.35f * glowAlpha) else NeonCyan.copy(alpha = 0.08f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Core Icon Surface with Shizzi-style Brush Mask
                    Box(
                        modifier = Modifier
                            .size(105.dp)
                            .clip(CircleShape)
                            .background(SurfaceVariantColor)
                            .border(
                                width = 1.5.dp,
                                color = if (isConnected) NeonCyan else CardBorderColor,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Icon with Gradient Brush
                        Icon(
                            imageVector = Icons.Default.WifiTethering,
                            contentDescription = "Direct Tethering Icon",
                            tint = if (isConnected) NeonCyan else if (isReady) NeonCyan.copy(alpha = 0.7f) else TextSecondary,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                // Status Text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = when {
                            isLoading -> "INITIALIZING TEST NETWORK..."
                            isConnected -> "DIRECT ROUTING ACTIVE"
                            isReady -> "SHIZUKU READY TO TETHER"
                            else -> "SETUP REQUIRED"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RajdhaniFont,
                        letterSpacing = 1.5.sp,
                        color = when {
                            isLoading -> StatusAmber
                            isConnected -> NeonGreen
                            isReady -> StatusCyan
                            else -> TextSecondary
                        }
                    )
                    Text(
                        text = if (isConnected) "VPN traffic routing through TUN directly to devices" else "Zero client setup: devices connect without proxy settings",
                        fontSize = 11.5.sp,
                        fontFamily = InterFont,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                // TETHER METHOD CHIPS (Wi-Fi Hotspot vs USB Cable vs Bluetooth)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("HOTSPOT", "Wi-Fi Hotspot", Icons.Default.Wifi),
                        Triple("USB", "USB Cable", Icons.Default.Usb),
                        Triple("BT", "Bluetooth", Icons.Default.Bluetooth)
                    ).forEach { (key, label, icon) ->
                        val isSelected = selectedMethod == key
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedMethod = key },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) NeonCyan.copy(alpha = if (isDark) 0.18f else 0.12f) else SurfaceVariantColor,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) NeonCyan.copy(alpha = 0.6f) else CardBorderColor
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) NeonCyan else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontFamily = InterFont,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) NeonCyan else TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // HERO ACTION BUTTON (The Power Switch)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .scale(if (isConnected) pulseScale else 1f)
                            .shadow(
                                elevation = if (isConnected) 14.dp else 6.dp,
                                shape = RoundedCornerShape(16.dp),
                                spotColor = if (isConnected) NeonRed.copy(alpha = 0.5f) else NeonCyan.copy(alpha = 0.35f)
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                when {
                                    !isReady -> Brush.horizontalGradient(listOf(SurfaceVariantColor, SurfaceVariantColor))
                                    isConnected -> Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFFFF1744),
                                            Color(0xFFD50000),
                                            Color(0xFFFF1744)
                                        )
                                    )
                                    else -> Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF00E5FF),
                                            Color(0xFF00B0FF),
                                            Color(0xFF00E676)
                                        )
                                    )
                                }
                            )
                            .clickable(enabled = !isLoading) {
                                if (!isReady) {
                                    viewModel.remedyShizuku(context)
                                } else if (isConnected) {
                                    viewModel.stopShizukuTether(context)
                                } else {
                                    viewModel.startShizukuTether(context)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color.Black,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.5.dp
                                )
                                Text(
                                    text = "Routing packets via TUN...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    fontFamily = RajdhaniFont,
                                    color = Color.Black
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = when {
                                        !isReady -> Icons.Default.Lock
                                        isConnected -> Icons.Default.StopCircle
                                        else -> Icons.Default.PowerSettingsNew
                                    },
                                    contentDescription = null,
                                    tint = if (isConnected) Color.White else Color.Black,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = when {
                                        !isReady -> LanguageManager.get("shizuku_grant_btn")
                                        isConnected -> LanguageManager.get("stop_direct_tether")
                                        else -> LanguageManager.get("start_direct_tether")
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RajdhaniFont,
                                    fontSize = 16.sp,
                                    letterSpacing = 0.5.sp,
                                    color = if (isConnected) Color.White else Color.Black
                                )
                            }
                        }
                    }
                }

                // ACTIVE TELEMETRY DASHBOARD (When Connected)
                AnimatedVisibility(
                    visible = isConnected,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 3-Column Metric Cards (Guaranteed No Text Wrapping)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1. Interface Card
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceVariantColor,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Dns, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(13.dp))
                                        Text("INTERFACE", fontSize = 9.sp, fontFamily = SpaceMonoFont, color = TextSecondary)
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = sessionState.interfaceName.ifEmpty { "tun0" },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SpaceMonoFont,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // 2. VPN Route Card
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = if (sessionState.isVpnBound) NeonGreen.copy(alpha = 0.12f) else SurfaceVariantColor,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (sessionState.isVpnBound) NeonGreen.copy(alpha = 0.4f) else CardBorderColor
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(
                                            Icons.Default.VpnKey,
                                            contentDescription = null,
                                            tint = if (sessionState.isVpnBound) NeonGreen else NeonCyan,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text("ROUTE", fontSize = 9.sp, fontFamily = SpaceMonoFont, color = TextSecondary)
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = if (sessionState.isVpnBound) "VPN Bound" else "Default",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SpaceMonoFont,
                                        color = if (sessionState.isVpnBound) NeonGreen else NeonCyan,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // 3. Client Counter Card
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceVariantColor,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Devices, contentDescription = null, tint = StatusAmber, modifier = Modifier.size(13.dp))
                                        Text("CLIENTS", fontSize = 9.sp, fontFamily = SpaceMonoFont, color = TextSecondary)
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "${sessionState.clientCount} Active",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SpaceMonoFont,
                                        color = if (sessionState.clientCount > 0) NeonGreen else TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Live Data Throughput Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceVariantColor)
                                .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp))
                                .padding(vertical = 10.dp, horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Text("UP:", fontSize = 11.sp, fontFamily = SpaceMonoFont, color = TextSecondary)
                                Text(
                                    text = Traffic.format(sessionState.traffic.up),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SpaceMonoFont,
                                    color = NeonCyan
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(18.dp)
                                    .background(if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.12f))
                            )

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                                Text("DOWN:", fontSize = 11.sp, fontFamily = SpaceMonoFont, color = TextSecondary)
                                Text(
                                    text = Traffic.format(sessionState.traffic.down),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SpaceMonoFont,
                                    color = NeonGreen
                                )
                            }
                        }
                    }
                }

                // Error alert (if any)
                if (sessionState.lastError.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NeonRed.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = NeonRed, modifier = Modifier.size(18.dp))
                            Text(
                                text = sessionState.lastError,
                                fontSize = 11.5.sp,
                                color = NeonRed,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // FEATURE ADVANTAGES MICRO-PILLS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "⚡ Zero Client Setup",
                "🎮 UDP & Games",
                "🛡️ Rootless VPN"
            ).forEach { pillText ->
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceVariantColor,
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        CardBorderColor
                    )
                ) {
                    Text(
                        text = pillText,
                        fontSize = 10.sp,
                        fontFamily = RajdhaniFont,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                        maxLines = 1
                    )
                }
            }
        }

        // STEP-BY-STEP HOW IT WORKS CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorderColor, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = StatusCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "HOW DIRECT ROUTING WORKS",
                        fontSize = 12.sp,
                        fontFamily = SpaceMonoFont,
                        fontWeight = FontWeight.Bold,
                        color = StatusCyan
                    )
                }

                val steps = listOf(
                    "1. Turn on Android Mobile Hotspot or connect USB Cable.",
                    "2. Start Shizuku service and grant permission to NetShare.",
                    "3. Tap 'Start Direct Sharing'. Devices connecting to your hotspot will instantly browse through your VPN with zero proxy settings!"
                )

                steps.forEach { step ->
                    Text(
                        text = step,
                        fontSize = 12.sp,
                        fontFamily = InterFont,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}
