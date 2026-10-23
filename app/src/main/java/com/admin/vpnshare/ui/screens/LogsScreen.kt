package com.admin.vpnshare.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.admin.vpnshare.proxy.LogEntry
import com.admin.vpnshare.proxy.LogType
import com.admin.vpnshare.ui.theme.*
import com.admin.vpnshare.ui.viewmodel.ProxyViewModel
import kotlinx.coroutines.launch

@Composable
fun LogsScreen(
    viewModel: ProxyViewModel
) {
    val context = LocalContext.current
    val logs by viewModel.logs.collectAsState()
    val blockedIps by viewModel.blockedIps.collectAsState()
    val isHttpRunning by viewModel.isHttpRunning.collectAsState()
    val isSocksRunning by viewModel.isSocksRunning.collectAsState()

    val isRunning = isHttpRunning || isSocksRunning

    fun extractIpFromMessage(message: String): String? {
        val regex = Regex("""\b(?:[0-9]{1,3}\.){3}[0-9]{1,3}\b""")
        return regex.find(message)?.value
    }

    var selectedFilter by remember { mutableStateOf("ALL") }
    val filters = remember { listOf("ALL", "HTTP", "SOCKS5", "ERRORS", "BLOCKED") }

    val filteredLogs = remember(logs, selectedFilter) {
        when (selectedFilter) {
            "HTTP" -> logs.filter { it.message.contains("HTTP", ignoreCase = true) }
            "SOCKS5" -> logs.filter { it.message.contains("SOCKS", ignoreCase = true) }
            "ERRORS" -> logs.filter { it.type == LogType.ERROR || it.message.contains("Error", ignoreCase = true) }
            "BLOCKED" -> logs.filter { it.message.contains("Block", ignoreCase = true) }
            else -> logs
        }
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom on new log entry
    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    fun copyText(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Console Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isRunning) NeonGreen else TextSecondary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LIVE CONSOLE",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = RajdhaniFont,
                    color = TextPrimary
                )
            }

            Text(
                text = "${filteredLogs.size} lines",
                fontSize = 12.sp,
                fontFamily = SpaceMonoFont,
                color = TextSecondary
            )
        }

        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { filter ->
                val isSelected = filter == selectedFilter
                Surface(
                    onClick = { selectedFilter = filter },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) NeonCyan else SurfaceColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NeonCyan else BorderCyan)
                ) {
                    Text(
                        text = filter,
                        fontSize = 12.sp,
                        fontFamily = SpaceMonoFont,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) BgColor else TextSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Console Log Output
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceColor)
        ) {
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Console clean. Waiting for activity...",
                        fontSize = 13.sp,
                        fontFamily = SpaceMonoFont,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        val logColor = when {
                            log.type == LogType.ERROR || log.message.contains("Error", ignoreCase = true) -> NeonRed
                            log.type == LogType.SUCCESS || log.message.contains("Connected", ignoreCase = true) -> NeonGreen
                            log.type == LogType.DATA -> NeonCyan
                            log.message.contains("Warning", ignoreCase = true) -> NeonAmber
                            else -> TextSecondary
                        }

                        val isAccessBlocked = log.message.contains("Access Control Blocked", ignoreCase = true) || 
                            (log.message.contains("Block", ignoreCase = true) && log.message.contains("IP Address", ignoreCase = true))
                        val isBlocked = log.message.contains("Block", ignoreCase = true)
                        val extractedIp = remember(log.message) { extractIpFromMessage(log.message) }
                        val isCurrentlyBlocked = extractedIp != null && blockedIps.contains(extractedIp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    copyText("Log Line", "[${log.timeFormatted}] ${log.message}")
                                }
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "[${log.timeFormatted}] ",
                                    fontSize = 11.sp,
                                    fontFamily = SpaceMonoFont,
                                    color = TextSecondary
                                )
                                Text(
                                    text = log.message,
                                    fontSize = 12.sp,
                                    fontFamily = SpaceMonoFont,
                                    color = logColor,
                                    textDecoration = if (isBlocked) TextDecoration.LineThrough else TextDecoration.None
                                )
                            }

                            if (isAccessBlocked && extractedIp != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                if (isCurrentlyBlocked) {
                                    IconButton(
                                        onClick = {
                                            viewModel.unblockIp(extractedIp)
                                            Toast.makeText(context, "IP unblocked: $extractedIp", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LockOpen,
                                            contentDescription = "Unblock $extractedIp",
                                            tint = NeonCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = NeonGreen.copy(alpha = 0.15f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Unblocked",
                                                tint = NeonGreen,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Unblocked",
                                                fontSize = 10.sp,
                                                fontFamily = SpaceMonoFont,
                                                fontWeight = FontWeight.Bold,
                                                color = NeonGreen
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val fullLogs = logs.joinToString("\n") { "[${it.timeFormatted}] ${it.message}" }
                    copyText("Console Logs", fullLogs)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy", color = NeonCyan, fontFamily = RajdhaniFont, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            OutlinedButton(
                onClick = {
                    val fullLogs = logs.joinToString("\n") { "[${it.timeFormatted}] ${it.message}" }
                    copyText("Exported Log File", fullLogs)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export", color = NeonPurple, fontFamily = RajdhaniFont, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            OutlinedButton(
                onClick = {
                    viewModel.clearLogs()
                    Toast.makeText(context, "Console cleared", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = NeonRed, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear", color = NeonRed, fontFamily = RajdhaniFont, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(130.dp).navigationBarsPadding())
    }
}
