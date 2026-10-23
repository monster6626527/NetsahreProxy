package com.admin.vpnshare.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.admin.vpnshare.R
import com.admin.vpnshare.LanguageManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.admin.vpnshare.ThemeManager
import com.admin.vpnshare.ui.theme.*

data class OnboardingPageData(
    val title: String,
    val body: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val isDark = ThemeManager.isDarkMode

    val pages = remember {
        listOf(
            OnboardingPageData(
                title = "Welcome to NetShare Proxy",
                body = "Transform your Android device into a powerful proxy server. Share your internet with any device seamlessly.",
                icon = Icons.Default.Language,
                color = Color(0xFF00E5FF)
            ),
            OnboardingPageData(
                title = "Configure Ports",
                body = "Set custom HTTP (8080) and SOCKS5 (1080) ports, or use convenient one-tap presets.",
                icon = Icons.Default.Tune,
                color = Color(0xFF9D4EDD)
            ),
            OnboardingPageData(
                title = "Start Sharing",
                body = "Toggle the proxy on. It runs smoothly in the background with a live notification service.",
                icon = Icons.Default.PowerSettingsNew,
                color = Color(0xFF00E676)
            ),
            OnboardingPageData(
                title = "Connect Your Devices",
                body = "Point any PC, laptop, smart TV, or phone to this device's IP and port. Instant access.",
                icon = Icons.Default.Devices,
                color = Color(0xFFFFB703)
            ),
            OnboardingPageData(
                title = "Official Channel & Updates",
                body = "Join our official Telegram channel for new releases, announcements, and direct developer contact.",
                icon = Icons.Default.Send,
                color = Color(0xFF0088CC)
            )
        )
    }

    var currentPage by remember { mutableIntStateOf(0) }
    val currentData = pages[currentPage]

    val infiniteTransition = rememberInfiniteTransition(label = "ringRotate")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        // Skip Button Top Right
        if (currentPage < pages.size - 1) {
            Text(
                text = "Skip",
                color = TextSecondary,
                fontSize = 14.sp,
                fontFamily = InterFont,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onComplete() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Neon Icon Graphic
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (currentPage == 4) 0.8f else 1f),
                contentAlignment = Alignment.Center
            ) {
                // Radial Gradient glow background
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    currentData.color.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Rotating dashed outer ring
                Canvas(
                    modifier = Modifier
                        .size(160.dp)
                        .rotate(rotationAngle)
                ) {
                    val strokeWidth = 2.dp.toPx()
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f)
                    drawCircle(
                        color = currentData.color.copy(alpha = 0.6f),
                        style = Stroke(width = strokeWidth, pathEffect = dashEffect)
                    )
                }

                // Inner circle surface + icon
                Surface(
                    shape = CircleShape,
                    color = SurfaceColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, currentData.color.copy(alpha = 0.5f)),
                    modifier = Modifier.size(120.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = currentData.icon,
                            contentDescription = null,
                            tint = currentData.color,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }

            // Bottom Section: Title, Body, Interactive Links (for 5th slide), Navigation
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (currentPage == 4) 1.2f else 1f)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (currentPage == 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Welcome to Net",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RajdhaniFont,
                                color = TextPrimary
                            )
                            Text(
                                text = "Share",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RajdhaniFont,
                                color = StatusCyan
                            )
                            Text(
                                text = " Proxy",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RajdhaniFont,
                                color = TextPrimary
                            )
                        }
                    } else {
                        Text(
                            text = currentData.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RajdhaniFont,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                    }

                    Text(
                        text = currentData.body,
                        fontSize = 13.sp,
                        fontFamily = InterFont,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    // 🌟 SPECIAL 5TH SLIDE: Interactive Telegram Channel & Developer Contact Cards
                    if (currentPage == 4) {
                        Spacer(modifier = Modifier.height(4.dp))

                        // Channel Card
                        Card(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/NetShareProxy"))
                                    context.startActivity(intent)
                                } catch (e: Exception) { e.printStackTrace() }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF0088CC).copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceVariantColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF0088CC)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }

                                    Column {
                                        Text("NetShare Proxy™", fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = RajdhaniFont, color = TextPrimary)
                                        Text("t.me/NetShareProxy", fontSize = 11.sp, fontFamily = SpaceMonoFont, color = Color(0xFF0088CC))
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF0088CC).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Join Channel",
                                        fontSize = 10.sp,
                                        fontFamily = SpaceMonoFont,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0088CC),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Developer Support Card
                        Card(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/A_DM_IN123"))
                                    context.startActivity(intent)
                                } catch (e: Exception) { e.printStackTrace() }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceVariantColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(SurfaceColor)
                                            .border(1.dp, StatusCyan.copy(alpha = 0.6f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.img_developer_avatar),
                                            contentDescription = "Developer Avatar",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    Column {
                                        Text(LanguageManager.get("app_developer"), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = InterFont, color = TextPrimary)
                                        Text("@A_DM_IN123", fontSize = 11.sp, fontFamily = SpaceMonoFont, color = StatusCyan)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = StatusCyan.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = LanguageManager.get("dev_contact"),
                                        fontSize = 10.sp,
                                        fontFamily = SpaceMonoFont,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusCyan,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 28.dp)
                ) {
                    // Progress dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        pages.indices.forEach { index ->
                            if (index == currentPage) {
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(if (currentPage == 4) Color(0xFF0088CC) else StatusCyan)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(TextSecondary.copy(alpha = 0.3f))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Next / Get Started Cyan Gradient Button
                    Button(
                        onClick = {
                            if (currentPage < pages.size - 1) {
                                currentPage++
                            } else {
                                onComplete()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        if (currentPage == 4)
                                            listOf(Color(0xFF0088CC), Color(0xFF00C2FF))
                                        else
                                            listOf(StatusCyan, Color(0xFF0099FF))
                                    ),
                                    shape = RoundedCornerShape(26.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (currentPage == pages.size - 1) "GET STARTED" else "NEXT",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RajdhaniFont,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

