package com.admin.vpnshare.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.admin.vpnshare.AppLanguage
import com.admin.vpnshare.LanguageManager
import com.admin.vpnshare.ThemeManager
import com.admin.vpnshare.ui.theme.*
import com.admin.vpnshare.ui.viewmodel.ProxyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    viewModel: ProxyViewModel
) {
    val context = LocalContext.current
    val isDark = ThemeManager.isDarkMode
    val cyanColor = StatusCyan
    val purpleColor = StatusPurple
    val greenColor = StatusGreen
    val amberColor = StatusAmber
    val redColor = StatusRed

    val sharedPrefs = remember { context.getSharedPreferences("vpn_share_prefs", Context.MODE_PRIVATE) }

    val httpPort by viewModel.httpPort.collectAsState()
    val socksPort by viewModel.socksPort.collectAsState()
    val autoStartOnBoot by viewModel.autoStartOnBoot.collectAsState()
    val networkInterfaces by viewModel.networkInterfaces.collectAsState()
    val blockedIps by viewModel.blockedIps.collectAsState()

    val bestIp = remember(networkInterfaces) { viewModel.getBestIP(networkInterfaces) }

    var howToExpanded by remember { mutableStateOf(true) }

    var showLanguageBottomSheet by remember { mutableStateOf(false) }
    var showResetStatsDialog by remember { mutableStateOf(false) }
    var showClearAllBlockedDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    if (showLanguageBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLanguageBottomSheet = false },
            containerColor = SurfaceColor,
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = LanguageManager.get("select_language"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = RajdhaniFont,
                    color = cyanColor
                )

                HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f))

                AppLanguage.values().forEach { lang ->
                    val isSelected = LanguageManager.currentLanguage == lang
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) SurfaceVariantColor else Color.Transparent)
                            .clickable {
                                LanguageManager.setLanguage(lang, sharedPrefs)
                                showLanguageBottomSheet = false
                                Toast.makeText(context, "Language set to ${lang.displayName}", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = if (isSelected) cyanColor else TextSecondary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = lang.displayName,
                                fontSize = 15.sp,
                                fontFamily = InterFont,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = TextPrimary
                            )
                        }

                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = cyanColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showResetStatsDialog) {
        AlertDialog(
            onDismissRequest = { showResetStatsDialog = false },
            containerColor = SurfaceColor,
            title = {
                Text("Reset Statistics?", fontFamily = RajdhaniFont, color = redColor)
            },
            text = {
                Text("This will permanently reset all session data counters, total bandwidth, peak speed, and log history.", fontSize = 13.sp, color = TextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllStats()
                        showResetStatsDialog = false
                        Toast.makeText(context, "Statistics reset successfully", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = redColor)
                ) {
                    Text("Reset All", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetStatsDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showClearAllBlockedDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllBlockedDialog = false },
            containerColor = SurfaceColor,
            title = {
                Text("Remove All IP Blocks?", fontFamily = RajdhaniFont, color = redColor)
            },
            text = {
                Text(
                    "Remove all IP blocks?\n${blockedIps.size} device(s) will be able to connect again.",
                    fontSize = 13.sp,
                    fontFamily = InterFont,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllBlocked()
                        showClearAllBlockedDialog = false
                        Toast.makeText(context, "All IP blocks cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = redColor)
                ) {
                    Text("Clear All", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllBlockedDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = LanguageManager.get("server_configuration"),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = RajdhaniFont,
            color = cyanColor,
            letterSpacing = 1.sp
        )

        // HOW TO CONNECT Card
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { howToExpanded = !howToExpanded }
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(cyanColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = cyanColor, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = LanguageManager.get("how_to_connect"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFont,
                            color = TextPrimary
                        )
                    }

                    Icon(
                        imageVector = if (howToExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }

                AnimatedVisibility(visible = howToExpanded) {
                    Column(
                        modifier = Modifier.padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f))

                        val steps = listOf(
                            LanguageManager.get("connect_step_1"),
                            LanguageManager.get("connect_step_2"),
                            "${LanguageManager.get("connect_step_3")} (IP: $bestIp, HTTP: $httpPort, SOCKS5: $socksPort)"
                        )

                        steps.forEachIndexed { index, step ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(cyanColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${index + 1}", fontSize = 11.sp, color = cyanColor, fontWeight = FontWeight.Bold)
                                }

                                Text(
                                    text = step,
                                    fontSize = 13.sp,
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

        // PORT PRESETS Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = LanguageManager.get("http_presets"),
                    fontSize = 10.sp,
                    fontFamily = SpaceMonoFont,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(8080, 8888, 1081).forEach { presetPort ->
                        OutlinedButton(
                            onClick = { viewModel.setHttpPort(presetPort) },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (httpPort == presetPort) cyanColor else CardBorderColor),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (httpPort == presetPort) cyanColor.copy(alpha = 0.15f) else Color.Transparent
                            )
                        ) {
                            Text("$presetPort", color = if (httpPort == presetPort) cyanColor else TextPrimary, fontFamily = SpaceMonoFont)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = LanguageManager.get("socks5_presets"),
                    fontSize = 10.sp,
                    fontFamily = SpaceMonoFont,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(1080, 1085, 9050).forEach { presetPort ->
                        OutlinedButton(
                            onClick = { viewModel.setSocksPort(presetPort) },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (socksPort == presetPort) purpleColor else CardBorderColor),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (socksPort == presetPort) purpleColor.copy(alpha = 0.15f) else Color.Transparent
                            )
                        ) {
                            Text("$presetPort", color = if (socksPort == presetPort) purpleColor else TextPrimary, fontFamily = SpaceMonoFont)
                        }
                    }
                }
            }
        }

        // LANGUAGE Card
        Card(
            onClick = { showLanguageBottomSheet = true },
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(cyanColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = cyanColor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(LanguageManager.get("language"), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = InterFont, color = TextPrimary)
                        Text(LanguageManager.currentLanguage.displayName, fontSize = 12.sp, fontFamily = InterFont, color = TextSecondary)
                    }
                }

                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
            }
        }

        // AUTO-START Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(greenColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Autorenew, contentDescription = null, tint = greenColor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(LanguageManager.get("auto_start_boot"), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = InterFont, color = TextPrimary)
                        Text(LanguageManager.get("auto_start_desc"), fontSize = 12.sp, fontFamily = InterFont, color = TextSecondary)
                    }
                }

                Switch(
                    checked = autoStartOnBoot,
                    onCheckedChange = { viewModel.toggleAutoStartOnBoot(sharedPrefs) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = cyanColor
                    )
                )
            }
        }

        // BLOCKED IPS Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (blockedIps.isNotEmpty()) redColor.copy(alpha = 0.5f) else CardBorderColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(redColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = redColor, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = LanguageManager.get("blocked_ips"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RajdhaniFont,
                            color = redColor
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (blockedIps.isNotEmpty()) redColor.copy(alpha = 0.15f) else SurfaceVariantColor
                    ) {
                        Text(
                            text = "${blockedIps.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceMonoFont,
                            color = if (blockedIps.isNotEmpty()) redColor else TextSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f))

                if (blockedIps.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = greenColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = LanguageManager.get("no_blocked_ips"),
                            fontSize = 13.sp,
                            fontFamily = InterFont,
                            color = TextSecondary
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        blockedIps.forEach { ip ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceVariantColor)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(redColor)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = ip,
                                        fontSize = 13.sp,
                                        fontFamily = SpaceMonoFont,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = redColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "BLOCKED",
                                            fontSize = 9.sp,
                                            fontFamily = SpaceMonoFont,
                                            fontWeight = FontWeight.Bold,
                                            color = redColor,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.unblockIp(ip)
                                        Toast.makeText(context, "IP unblocked: $ip", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LockOpen,
                                        contentDescription = "Unblock IP",
                                        tint = cyanColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { showClearAllBlockedDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, redColor)
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, tint = redColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = LanguageManager.get("clear_all_blocked"),
                            color = redColor,
                            fontFamily = RajdhaniFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // DANGER ZONE Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, redColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = LanguageManager.get("danger_zone"),
                    fontSize = 11.sp,
                    fontFamily = SpaceMonoFont,
                    color = redColor,
                    letterSpacing = 1.sp
                )

                OutlinedButton(
                    onClick = { showResetStatsDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, redColor)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = redColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(LanguageManager.get("reset_stats"), color = redColor, fontFamily = RajdhaniFont, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(140.dp))
    }
}
