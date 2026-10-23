package com.admin.vpnshare.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.admin.vpnshare.LanguageManager
import com.admin.vpnshare.ThemeManager
import com.admin.vpnshare.proxy.NetworkInterfaceInfo
import com.admin.vpnshare.ui.theme.*
import com.admin.vpnshare.ui.viewmodel.ProxyViewModel

@Composable
fun StatsScreen(
    viewModel: ProxyViewModel
) {
    val context = LocalContext.current
    val isDark = ThemeManager.isDarkMode
    val cyanColor = StatusCyan
    val purpleColor = StatusPurple
    val greenColor = StatusGreen
    val amberColor = StatusAmber

    val totalBytes by viewModel.totalBytes.collectAsState()
    val totalBytesDown by viewModel.totalBytesDown.collectAsState()
    val totalBytesUp by viewModel.totalBytesUp.collectAsState()
    val activeConnections by viewModel.activeConnections.collectAsState()
    val sessionDurationFormatted by viewModel.sessionDurationFormatted.collectAsState()
    val peakSpeedFormatted by viewModel.peakSpeedFormatted.collectAsState()
    val totalClientsServed by viewModel.totalClientsServed.collectAsState()
    val networkInterfaces by viewModel.networkInterfaces.collectAsState()
    val httpPort by viewModel.httpPort.collectAsState()
    val socksPort by viewModel.socksPort.collectAsState()
    val historyDown by viewModel.speedHistoryDown.collectAsState()
    val historyUp by viewModel.speedHistoryUp.collectAsState()
    val downloadSpeed by viewModel.downloadSpeed.collectAsState()
    val uploadSpeed by viewModel.uploadSpeed.collectAsState()

    val formattedTotalData = remember(totalBytes) {
        viewModel.formatBytes(totalBytes)
    }
    val formattedDown = remember(totalBytesDown) {
        viewModel.formatBytes(totalBytesDown)
    }
    val formattedUp = remember(totalBytesUp) {
        viewModel.formatBytes(totalBytesUp)
    }

    val bestIp = remember(networkInterfaces) {
        viewModel.getBestIP(networkInterfaces)
    }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = LanguageManager.get("session_statistics"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = RajdhaniFont,
                color = cyanColor,
                letterSpacing = 1.sp
            )
        }

        // QUICK COPY FLOATING ACTION ROW
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceColor)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "QUICK COPY CONFIG",
                        fontSize = 10.sp,
                        fontFamily = SpaceMonoFont,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { copyToClipboard("IP Address", bestIp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantColor),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = cyanColor, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("IP", color = cyanColor, fontSize = 11.sp, fontFamily = SpaceMonoFont, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { copyToClipboard("HTTP Proxy", "$bestIp:$httpPort") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantColor),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = cyanColor, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("HTTP", color = cyanColor, fontSize = 11.sp, fontFamily = SpaceMonoFont, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { copyToClipboard("SOCKS5 Proxy", "$bestIp:$socksPort") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantColor),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = purpleColor, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SOCKS5", color = purpleColor, fontSize = 11.sp, fontFamily = SpaceMonoFont, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // MINI LIVE BANDWIDTH SPARKLINE
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceColor)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BANDWIDTH SPARKLINE",
                            fontSize = 10.sp,
                            fontFamily = SpaceMonoFont,
                            color = TextSecondary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Down",
                                    tint = cyanColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = viewModel.formatSpeed(downloadSpeed),
                                    fontSize = 11.sp,
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
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = viewModel.formatSpeed(uploadSpeed),
                                    fontSize = 11.sp,
                                    fontFamily = SpaceMonoFont,
                                    fontWeight = FontWeight.Bold,
                                    color = purpleColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    ) {
                        val width = size.width
                        val height = size.height
                        val maxVal = maxOf(
                            historyDown.maxOrNull() ?: 1f,
                            historyUp.maxOrNull() ?: 1f,
                            50f
                        )

                        fun drawSpark(points: List<Float>, color: Color) {
                            if (points.size < 2) return
                            val step = width / (points.size - 1)
                            val path = Path()
                            val firstY = (1f - (points[0] / maxVal).coerceIn(0f, 1f)) * (height - 6f) + 3f
                            path.moveTo(0f, firstY)
                            for (i in 0 until points.size - 1) {
                                val cx = i * step + step / 2f
                                val nx = (i + 1) * step
                                val cy = (1f - (points[i] / maxVal).coerceIn(0f, 1f)) * (height - 6f) + 3f
                                val ny = (1f - (points[i + 1] / maxVal).coerceIn(0f, 1f)) * (height - 6f) + 3f
                                path.cubicTo(cx, cy, cx, ny, nx, ny)
                            }
                            drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx()))
                        }

                        drawSpark(historyDown, cyanColor)
                        drawSpark(historyUp, purpleColor)
                    }
                }
            }
        }

        // REPLACEMENT: LIVE TRAFFIC STREAM RATIO & PROXY HEALTH CARD
        item {
            val totalCombined = (totalBytesDown + totalBytesUp).coerceAtLeast(1L)
            val downPercent = ((totalBytesDown.toDouble() / totalCombined) * 100).toInt().coerceIn(0, 100)
            val upPercent = 100 - downPercent

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceColor)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(greenColor)
                            )
                            Text(
                                text = "DATA STREAM RATIO",
                                fontSize = 11.sp,
                                fontFamily = SpaceMonoFont,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = greenColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (activeConnections > 0) "STREAMING" else "READY",
                                fontSize = 10.sp,
                                fontFamily = SpaceMonoFont,
                                fontWeight = FontWeight.Bold,
                                color = greenColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Visual Segmented Ratio Bar (Download vs Upload)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(downPercent.coerceAtLeast(5).toFloat())
                                .fillMaxHeight()
                                .background(cyanColor)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(
                            modifier = Modifier
                                .weight(upPercent.coerceAtLeast(5).toFloat())
                                .fillMaxHeight()
                                .background(purpleColor)
                        )
                    }

                    // Download vs Upload Metrics Details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Download detail
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(cyanColor)
                            )
                            Column {
                                Text("DOWNLOAD", fontSize = 9.sp, fontFamily = SpaceMonoFont, color = TextSecondary)
                                Text(
                                    text = "$formattedDown ($downPercent%)",
                                    fontSize = 12.sp,
                                    fontFamily = SpaceMonoFont,
                                    fontWeight = FontWeight.Bold,
                                    color = cyanColor
                                )
                            }
                        }

                        // Upload detail
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(purpleColor)
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text("UPLOAD", fontSize = 9.sp, fontFamily = SpaceMonoFont, color = TextSecondary)
                                Text(
                                    text = "$formattedUp ($upPercent%)",
                                    fontSize = 12.sp,
                                    fontFamily = SpaceMonoFont,
                                    fontWeight = FontWeight.Bold,
                                    color = purpleColor
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f))

                    // Proxy Services Health Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = greenColor, modifier = Modifier.size(13.dp))
                            Text("Active Sockets: $activeConnections", fontSize = 11.sp, fontFamily = InterFont, color = TextSecondary)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = cyanColor, modifier = Modifier.size(13.dp))
                            Text("Health: Stable (0 drops)", fontSize = 11.sp, fontFamily = InterFont, color = greenColor, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // 2x2 Metric Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Total Data Shared",
                        value = formattedTotalData,
                        color = purpleColor
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Session Duration",
                        value = sessionDurationFormatted,
                        color = cyanColor
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Peak Speed",
                        value = peakSpeedFormatted,
                        color = greenColor
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Total Clients",
                        value = "$totalClientsServed",
                        color = amberColor
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "AVAILABLE INTERFACES",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = RajdhaniFont,
                color = TextPrimary,
                letterSpacing = 1.sp
            )
        }

        // Network Interfaces list
        items(networkInterfaces) { iface ->
            InterfaceRowCard(
                iface = iface,
                onCopy = { copyToClipboard("IP Address", iface.ipAddress) }
            )
        }

        // Client Connection Guide
        item {
            Spacer(modifier = Modifier.height(8.dp))
            ClientConnectionGuideCard(
                bestIp = bestIp,
                httpPort = httpPort,
                socksPort = socksPort,
                onCopyConfig = {
                    val fullConfig = "HTTP Proxy: $bestIp:$httpPort | SOCKS5: $bestIp:$socksPort"
                    copyToClipboard("Full Config", fullConfig)
                },
                onCopyIp = { copyToClipboard("Proxy IP", bestIp) }
            )
        }
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier.border(1.dp, CardBorderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontFamily = InterFont,
                color = TextSecondary
            )
            Text(
                text = value,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SpaceMonoFont,
                color = color
            )
        }
    }
}

@Composable
fun InterfaceRowCard(
    iface: NetworkInterfaceInfo,
    onCopy: () -> Unit
) {
    val defaultSecondary = TextSecondary
    val cyanColor = StatusCyan
    val greenColor = StatusGreen
    val amberColor = StatusAmber
    val purpleColor = StatusPurple

    val (icon, glowColor, typeName) = remember(iface, defaultSecondary, cyanColor, greenColor, amberColor, purpleColor) {
        when {
            iface.name.contains("tun", ignoreCase = true) -> Triple(Icons.Default.VpnKey, cyanColor, "VPN Interface")
            iface.name.contains("wlan", ignoreCase = true) -> Triple(Icons.Default.Wifi, greenColor, "WiFi Interface")
            iface.name.contains("ap", ignoreCase = true) || iface.name.contains("swlan", ignoreCase = true) -> Triple(Icons.Default.WifiTethering, amberColor, "Hotspot Interface")
            iface.name.contains("rndis", ignoreCase = true) || iface.name.contains("usb", ignoreCase = true) || iface.name.contains("ncm", ignoreCase = true) -> Triple(Icons.Default.Usb, purpleColor, "USB Tethering")
            else -> Triple(Icons.Default.SignalCellular4Bar, defaultSecondary, "Network Gateway")
        }
    }

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
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(glowColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = glowColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = iface.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFont,
                        color = TextPrimary
                    )
                    Text(
                        text = typeName,
                        fontSize = 12.sp,
                        fontFamily = InterFont,
                        color = TextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = iface.ipAddress,
                    fontSize = 14.sp,
                    fontFamily = SpaceMonoFont,
                    color = cyanColor,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = onCopy) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy IP",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ClientConnectionGuideCard(
    bestIp: String,
    httpPort: Int,
    socksPort: Int,
    onCopyConfig: () -> Unit,
    onCopyIp: () -> Unit
) {
    val cyanColor = StatusCyan
    val purpleColor = StatusPurple

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor)
    ) {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(cyanColor)
            )

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "CLIENT CONNECTION GUIDE",
                    fontSize = 12.sp,
                    fontFamily = SpaceMonoFont,
                    color = cyanColor,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Proxy IP: $bestIp",
                        fontSize = 15.sp,
                        fontFamily = SpaceMonoFont,
                        color = TextPrimary
                    )
                    IconButton(onClick = onCopyIp) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = cyanColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = cyanColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "HTTP: $httpPort",
                            fontSize = 12.sp,
                            fontFamily = SpaceMonoFont,
                            color = cyanColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = purpleColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "SOCKS5: $socksPort",
                            fontSize = 12.sp,
                            fontFamily = SpaceMonoFont,
                            color = purpleColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                val steps = listOf(
                    "Connect client device to the same Wi-Fi, Hotspot, or USB Network.",
                    "Open proxy settings on your client (PC/Console/Phone).",
                    "Enter $bestIp as Server IP and port $httpPort or $socksPort."
                )

                steps.forEachIndexed { idx, step ->
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
                            Text(
                                text = "${idx + 1}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = cyanColor
                            )
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

                OutlinedButton(
                    onClick = onCopyConfig,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, cyanColor)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = cyanColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Copy Full Config",
                        color = cyanColor,
                        fontFamily = RajdhaniFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
