package com.admin.vpnshare.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.admin.vpnshare.ui.theme.*

data class FaqItemData(
    val question: String,
    val answer: String
)

@Composable
fun FaqDialog(
    onDismiss: () -> Unit
) {
    val faqs = remember {
        listOf(
            FaqItemData(
                "How do I connect my other devices?",
                "Ensure both devices are on the same Wi-Fi, Hotspot, or connected via USB cable. Open Wi-Fi/Network settings on your client device, select Manual Proxy, and enter this device's IP and configured HTTP or SOCKS5 port."
            ),
            FaqItemData(
                "How does Shizuku Direct Sharing (No-Proxy / Like Root) work?",
                "With Shizuku or Root permission, NetShare creates a privileged test-network routing tunnel (the shizzi method) that forwards your Hotspot client traffic directly through your phone's active VPN! Connected devices (phones, laptops, Smart TVs, consoles) do NOT need any manual proxy settings — they get full VPN internet access directly like on a rooted phone!"
            ),
            FaqItemData(
                "Why is my client device not connecting?",
                "Verify that the proxy server is running (green indicator). Double check that the IP address matches your active network interface (e.g. wlan0 or rndis0) and that port 8080/1080 is correctly typed without trailing spaces."
            ),
            FaqItemData(
                "What is the difference between HTTP and SOCKS5?",
                "HTTP Proxy is universally supported across web browsers. SOCKS5 Proxy works at a lower socket level, supporting any TCP/UDP traffic including games, VPN clients, and apps."
            ),
            FaqItemData(
                "How do I keep the proxy server running stably?",
                "Disable Battery Optimization for NetShare Proxy in Android Settings to prevent the Android OS from stopping background services."
            ),
            FaqItemData(
                "Can I share VPN over USB cable without Root?",
                "Yes! Connect your phone to PC via USB, enable 'USB Tethering' in Android Settings, and use the USB interface IP (rndis0) in your PC proxy settings. No root needed."
            ),
            FaqItemData(
                "Why does my carrier block connections?",
                "Some mobile carriers block hotspot tethering ports. Changing the HTTP port to 8888 or SOCKS5 port to 9050 in the Config tab often bypasses carrier restrictions."
            ),
            FaqItemData(
                "How do I share my VPN tunnel with a PC?",
                "Turn on your VPN app on Android, start NetShare Proxy server, and connect your PC to this phone's proxy IP. All PC traffic will route through your Android's active VPN tunnel."
            )
        )
    }

    var expandedIndices by remember { mutableStateOf(setOf<Int>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceColor,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HelpCenter, contentDescription = null, tint = NeonCyan)
                Spacer(modifier = Modifier.width(10.dp))
                Text("FAQs & Troubleshooting", fontFamily = RajdhaniFont, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                faqs.forEachIndexed { index, faq ->
                    val isExpanded = expandedIndices.contains(index)
                    Card(
                        onClick = {
                            expandedIndices = if (isExpanded) {
                                expandedIndices - index
                            } else {
                                expandedIndices + index
                            }
                        },
                        colors = CardDefaults.cardColors(containerColor = SurfaceVariantColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = faq.question,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = InterFont,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = TextSecondary
                                )
                            }

                            AnimatedVisibility(visible = isExpanded) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = faq.answer,
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
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Got it", color = BgColor, fontWeight = FontWeight.Bold, fontFamily = RajdhaniFont)
            }
        }
    )
}
