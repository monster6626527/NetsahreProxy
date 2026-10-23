package com.admin.vpnshare.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.admin.vpnshare.LanguageManager
import com.admin.vpnshare.ThemeManager
import com.admin.vpnshare.proxy.ClientSession
import com.admin.vpnshare.ui.theme.*
import com.admin.vpnshare.ui.viewmodel.ProxyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    viewModel: ProxyViewModel
) {
    val context = LocalContext.current
    val connectedClients by viewModel.connectedClients.collectAsState()
    val blockedIps by viewModel.blockedIps.collectAsState()

    var selectedClientForSheet by remember { mutableStateOf<ClientSession?>(null) }
    var showSpeedLimitDialog by remember { mutableStateOf(false) }
    var speedLimitInput by remember { mutableStateOf("1024") }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Client IP", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "IP $text copied", Toast.LENGTH_SHORT).show()
    }

    val cyanColor = StatusCyan
    val purpleColor = StatusPurple
    val amberColor = StatusAmber
    val redColor = StatusRed
    val greenColor = StatusGreen

    if (selectedClientForSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedClientForSheet = null },
            containerColor = SurfaceColor,
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "CLIENT OPTIONS: ${selectedClientForSheet?.ip}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceMonoFont,
                    color = cyanColor
                )

                HorizontalDivider(color = if (ThemeManager.isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f))

                TextButton(
                    onClick = {
                        selectedClientForSheet?.let { copyToClipboard(it.ip) }
                        selectedClientForSheet = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = cyanColor)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Copy IP Address", color = TextPrimary, fontFamily = InterFont)
                    }
                }

                TextButton(
                    onClick = {
                        showSpeedLimitDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = amberColor)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Set Speed Limit (KB/s)", color = TextPrimary, fontFamily = InterFont)
                    }
                }

                TextButton(
                    onClick = {
                        selectedClientForSheet?.let { viewModel.blockIp(it.ip) }
                        selectedClientForSheet = null
                        Toast.makeText(context, "IP Blocked", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = redColor)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Block IP Address", color = redColor, fontFamily = InterFont)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showSpeedLimitDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedLimitDialog = false },
            containerColor = SurfaceColor,
            title = {
                Text("Set Speed Limit", fontFamily = RajdhaniFont, color = cyanColor)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Limit bandwidth for ${selectedClientForSheet?.ip} in KB/s:", fontSize = 13.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = speedLimitInput,
                        onValueChange = { speedLimitInput = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = cyanColor,
                            unfocusedBorderColor = BorderCyan,
                            focusedContainerColor = SurfaceVariantColor,
                            unfocusedContainerColor = SurfaceVariantColor
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSpeedLimitDialog = false
                        selectedClientForSheet = null
                        Toast.makeText(context, "Speed limit applied", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cyanColor)
                ) {
                    Text("Apply", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSpeedLimitDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card
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
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = LanguageManager.get("connected_clients_title"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RajdhaniFont,
                        color = TextPrimary
                    )
                    Text(
                        text = "Real-time client session and throughput monitor",
                        fontSize = 12.sp,
                        fontFamily = InterFont,
                        color = TextSecondary
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = cyanColor
                ) {
                    Text(
                        text = "${connectedClients.size}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SpaceMonoFont,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (connectedClients.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 90.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyClientsState()
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(connectedClients, key = { it.ip }) { client ->
                    ClientSessionCard(
                        client = client,
                        onBlock = { viewModel.blockIp(client.ip) },
                        onLongClick = { selectedClientForSheet = client }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyClientsState() {
    val cyanColor = StatusCyan
    val purpleColor = StatusPurple
    val infiniteTransition = rememberInfiniteTransition(label = "pulseEmptyTopology")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val surfaceVariant = SurfaceVariantColor

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Network Topology Illustration
        Canvas(
            modifier = Modifier
                .size(160.dp)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = 55.dp.toPx()

            // Outer node positions
            val nodeOffsets = listOf(
                Offset(center.x, center.y - radius),
                Offset(center.x + radius * 0.866f, center.y + radius * 0.5f),
                Offset(center.x - radius * 0.866f, center.y + radius * 0.5f)
            )

            // Draw dashed link lines
            val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            nodeOffsets.forEach { nodePos ->
                drawLine(
                    color = cyanColor.copy(alpha = pulseAlpha * 0.6f),
                    start = center,
                    end = nodePos,
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = dash
                )
            }

            // Draw Central Hub Node
            drawCircle(
                color = cyanColor.copy(alpha = 0.2f),
                radius = 28.dp.toPx(),
                center = center
            )
            drawCircle(
                color = cyanColor,
                radius = 12.dp.toPx(),
                center = center
            )

            // Draw Client Nodes
            nodeOffsets.forEach { nodePos ->
                drawCircle(
                    color = surfaceVariant,
                    radius = 14.dp.toPx(),
                    center = nodePos
                )
                drawCircle(
                    color = purpleColor.copy(alpha = pulseAlpha),
                    radius = 6.dp.toPx(),
                    center = nodePos
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "No Active Client Connections",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = InterFont,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Connect PC, TV, or mobile devices to your Hotspot / USB and configure their proxy settings.",
            fontSize = 13.sp,
            fontFamily = InterFont,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClientSessionCard(
    client: ClientSession,
    onBlock: () -> Unit,
    onLongClick: () -> Unit
) {
    val cyanColor = StatusCyan
    val purpleColor = StatusPurple
    val redColor = StatusRed
    val isHttp = client.protocols.contains("HTTP")
    val glowColor = if (isHttp) cyanColor else purpleColor

    val protocolLabel = when {
        client.protocols.contains("HTTP") && client.protocols.contains("SOCKS5") -> "HTTP+SOCKS5"
        client.protocols.contains("SOCKS5") -> "SOCKS5"
        else -> "HTTP"
    }

    val (deviceIcon, deviceLabel) = remember(client.ip) {
        when {
            client.ip.endsWith(".2") || client.ip.startsWith("192.168.42.") -> Pair(Icons.Default.Laptop, "PC / Laptop")
            client.ip.endsWith(".100") || client.ip.endsWith(".101") -> Pair(Icons.Default.Tv, "Smart TV / Console")
            else -> Pair(Icons.Default.Smartphone, "Mobile Client")
        }
    }

    val formattedDuration = remember(client.firstConnectedAt) {
        val seconds = ((System.currentTimeMillis() - client.firstConnectedAt) / 1000).coerceAtLeast(0)
        val mins = seconds / 60
        val hrs = mins / 60
        String.format(java.util.Locale.US, "%02d:%02d:%02d", hrs % 24, mins % 60, seconds % 60)
    }

    val activeSockets = client.activeConnectionCount.get()
    val totalBytes = client.totalBytesUp.get() + client.totalBytesDown.get()

    val formattedData = remember(totalBytes) {
        if (totalBytes < 1024) "$totalBytes B"
        else if (totalBytes < 1024 * 1024) String.format(java.util.Locale.US, "%.1f KB", totalBytes / 1024.0)
        else String.format(java.util.Locale.US, "%.2f MB", totalBytes / (1024.0 * 1024.0))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            ),
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
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(glowColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = deviceIcon,
                        contentDescription = null,
                        tint = glowColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = client.ip,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceMonoFont,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = glowColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = protocolLabel,
                                fontSize = 10.sp,
                                fontFamily = SpaceMonoFont,
                                fontWeight = FontWeight.Bold,
                                color = glowColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (activeSockets > 1) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = cyanColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "$activeSockets active",
                                    fontSize = 10.sp,
                                    fontFamily = SpaceMonoFont,
                                    color = cyanColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "$deviceLabel  •  ↕ $formattedData  •  ⏱ $formattedDuration",
                        fontSize = 11.sp,
                        fontFamily = InterFont,
                        color = TextSecondary
                    )
                }
            }

            IconButton(onClick = onBlock) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Block IP",
                    tint = redColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
