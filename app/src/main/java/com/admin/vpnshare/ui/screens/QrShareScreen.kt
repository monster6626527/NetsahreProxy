package com.admin.vpnshare.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.admin.vpnshare.ui.theme.*
import com.admin.vpnshare.ui.viewmodel.ProxyViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun QrShareScreen(
    viewModel: ProxyViewModel
) {
    val context = LocalContext.current
    val httpPort by viewModel.httpPort.collectAsState()
    val socksPort by viewModel.socksPort.collectAsState()
    val networkInterfaces by viewModel.networkInterfaces.collectAsState()

    val bestIp = remember(networkInterfaces) { viewModel.getBestIP(networkInterfaces) }

    var selectedProtocol by remember { mutableStateOf("HTTP") } // "HTTP" or "SOCKS5"

    val currentPort = if (selectedProtocol == "HTTP") httpPort else socksPort
    val qrContent = if (selectedProtocol == "HTTP") "http://$bestIp:$currentPort" else "socks5://$bestIp:$currentPort"

    val qrBitmap = remember(qrContent) {
        generateQrCodeBitmap(qrContent, 512, 512)
    }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Proxy Config", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Proxy URL copied", Toast.LENGTH_SHORT).show()
    }

    fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "NetShare Proxy Config")
            putExtra(Intent.EXTRA_TEXT, "NetShare Proxy Config\nHTTP: $bestIp:$httpPort\nSOCKS5: $bestIp:$socksPort\nActive URL ($selectedProtocol): $text")
        }
        context.startActivity(Intent.createChooser(intent, "Share Proxy Config"))
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .verticalScroll(scrollState)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "NET",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = RajdhaniFont,
                color = TextPrimary,
                letterSpacing = 1.sp
            )
            Text(
                text = "SHARE",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = RajdhaniFont,
                color = NeonCyan,
                letterSpacing = 1.sp
            )
            Text(
                text = " PROXY CONFIG",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = RajdhaniFont,
                color = TextPrimary,
                letterSpacing = 1.sp
            )
        }

        // Protocol Selector Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("HTTP", "SOCKS5").forEach { proto ->
                val isSelected = proto == selectedProtocol
                Surface(
                    onClick = { selectedProtocol = proto },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) (if (proto == "HTTP") NeonCyan else NeonPurple) else SurfaceColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color.Transparent else BorderCyan)
                ) {
                    Text(
                        text = proto,
                        fontSize = 14.sp,
                        fontFamily = SpaceMonoFont,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) BgColor else TextSecondary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                    )
                }
            }
        }

        // QR Code Card
        Card(
            modifier = Modifier
                .size(280.dp)
                .border(1.dp, CardBorderColor, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    CircularProgressIndicator(color = NeonCyan)
                }
            }
        }

        // Subtitle text
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SurfaceVariantColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderCyan)
        ) {
            Text(
                text = "Proxy: $bestIp:$currentPort",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SpaceMonoFont,
                color = NeonCyan,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }

        // Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { copyToClipboard(qrContent) },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = NeonCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy Config", color = NeonCyan, fontFamily = RajdhaniFont, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Button(
                onClick = { shareText(qrContent) },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = BgColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share", color = BgColor, fontFamily = RajdhaniFont, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        Spacer(modifier = Modifier.height(110.dp))
    }
}

fun generateQrCodeBitmap(text: String, width: Int, height: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height)
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix[x, y]) android.graphics.Color.WHITE else android.graphics.Color.BLACK
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
