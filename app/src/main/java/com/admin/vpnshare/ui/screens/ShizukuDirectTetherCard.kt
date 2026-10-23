package com.admin.vpnshare.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.admin.vpnshare.LanguageManager
import com.admin.vpnshare.shizuku.*
import com.admin.vpnshare.ui.theme.*
import com.admin.vpnshare.ui.viewmodel.ProxyViewModel

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

    // Periodic check for Shizuku status
    LaunchedEffect(Unit) {
        viewModel.refreshShizukuState()
    }

    // Pulse animation when connected
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = when {
                    isConnected -> NeonCyan
                    shizukuState is ShizukuState.Ready -> NeonCyan.copy(alpha = 0.5f)
                    else -> CardBorderColor
                },
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Badge & Mode Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) NeonCyan.copy(alpha = 0.2f) else CardBorderColor.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiTethering,
                            contentDescription = null,
                            tint = if (isConnected) NeonCyan else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = LanguageManager.get("shizuku_mode"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isConnected) "Active (No Proxy Required)" else "Rootless Direct Routing (shizzi method)",
                            fontSize = 11.sp,
                            color = if (isConnected) NeonCyan else TextSecondary
                        )
                    }
                }

                // Shizuku Status Badge
                Surface(
                    color = when (shizukuState) {
                        is ShizukuState.Ready -> NeonGreen.copy(alpha = 0.15f)
                        is ShizukuState.PermissionRequired -> StatusAmber.copy(alpha = 0.15f)
                        else -> NeonRed.copy(alpha = 0.15f)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    when (shizukuState) {
                                        is ShizukuState.Ready -> NeonGreen
                                        is ShizukuState.PermissionRequired -> StatusAmber
                                        else -> NeonRed
                                    }
                                )
                        )
                        Text(
                            text = when (shizukuState) {
                                is ShizukuState.Ready -> (shizukuState as ShizukuState.Ready).let {
                                    if (it.isRoot) "Root (0)" else "Shizuku (2000)"
                                }
                                is ShizukuState.PermissionRequired -> "Permission Req."
                                ShizukuState.NotRunning -> "Service Off"
                                ShizukuState.NotInstalled -> "Not Installed"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when (shizukuState) {
                                is ShizukuState.Ready -> NeonGreen
                                is ShizukuState.PermissionRequired -> StatusAmber
                                else -> NeonRed
                            }
                        )
                    }
                }
            }

            // Shizuku Remedy Action (If not ready)
            if (shizukuState !is ShizukuState.Ready) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (shizukuState) {
                            is ShizukuState.PermissionRequired -> StatusAmber.copy(alpha = 0.12f)
                            else -> NeonRed.copy(alpha = 0.12f)
                        }
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when (shizukuState) {
                            is ShizukuState.PermissionRequired -> StatusAmber.copy(alpha = 0.5f)
                            else -> NeonRed.copy(alpha = 0.5f)
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
                        Column(modifier = Modifier.weight(1f)) {
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
                                    is ShizukuState.PermissionRequired -> "Grant permission to start direct tethering"
                                    ShizukuState.NotRunning -> "Launch Shizuku and start service via wireless debugging"
                                    ShizukuState.NotInstalled -> "Install Shizuku to share without proxy setup"
                                    else -> ""
                                },
                                fontSize = 10.sp,
                                color = TextSecondary
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

            // Big Action Button: START / STOP Direct Tether
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                val isReady = shizukuState is ShizukuState.Ready

                Button(
                    onClick = {
                        if (!isReady) {
                            viewModel.remedyShizuku(context)
                        } else if (isConnected) {
                            viewModel.stopShizukuTether(context)
                        } else {
                            viewModel.startShizukuTether(context)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .scale(if (isConnected) pulseScale else 1f)
                        .shadow(
                            elevation = if (isConnected) 12.dp else 4.dp,
                            shape = RoundedCornerShape(14.dp),
                            spotColor = if (isConnected) NeonCyan else Color.Transparent
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            isConnected -> NeonRed
                            isReady -> NeonCyan
                            else -> CardBorderColor
                        }
                    ),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Starting TUN & routing...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    } else {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isConnected -> LanguageManager.get("stop_direct_tether")
                                isReady -> LanguageManager.get("start_direct_tether")
                                else -> LanguageManager.get("shizuku_grant_btn")
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    }
                }
            }

            // Connected Stats Row (If Active)
            AnimatedVisibility(
                visible = isConnected,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Interface & VPN Bound Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // TUN Iface
                        Surface(
                            color = SurfaceVariantColor,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Dns,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "TUN: ${sessionState.interfaceName.ifEmpty { "testtun0" }}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            }
                        }

                        // VPN Bound Indicator
                        Surface(
                            color = if (sessionState.isVpnBound) NeonGreen.copy(alpha = 0.15f) else NeonCyan.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.VpnKey,
                                    contentDescription = null,
                                    tint = if (sessionState.isVpnBound) NeonGreen else NeonCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (sessionState.isVpnBound) "VPN Bound ●" else "Default Route",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sessionState.isVpnBound) NeonGreen else NeonCyan
                                )
                            }
                        }

                        // Client Count
                        Surface(
                            color = SurfaceVariantColor,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Devices,
                                    contentDescription = null,
                                    tint = StatusAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${sessionState.clientCount} clients",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    // Live Traffic Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceVariantColor)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Up: ${Traffic.format(sessionState.traffic.up)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Down: ${Traffic.format(sessionState.traffic.down)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Error display (if any)
            if (sessionState.lastError.isNotEmpty()) {
                Text(
                    text = sessionState.lastError,
                    fontSize = 11.sp,
                    color = NeonRed,
                    fontWeight = FontWeight.Medium
                )
            }

            // Helpful Guide Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = LanguageManager.get("shizuku_guide_desc"),
                    fontSize = 10.5.sp,
                    color = TextSecondary,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
