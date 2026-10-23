package com.admin.vpnshare.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.admin.vpnshare.LanguageManager
import com.admin.vpnshare.R
import com.admin.vpnshare.ThemeManager
import com.admin.vpnshare.ui.theme.*

@Composable
fun DrawerMenu(
    selectedRoute: String,
    onNavigate: (String) -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenFaqs: () -> Unit,
    onCloseDrawer: () -> Unit,
    onExitApp: () -> Unit
) {
    val context = LocalContext.current
    val isDark = ThemeManager.isDarkMode
    val cyanColor = StatusCyan
    val purpleColor = StatusPurple
    val greenColor = StatusGreen
    val amberColor = StatusAmber
    val redColor = StatusRed

    val infiniteTransition = rememberInfiniteTransition(label = "drawerRing")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scrollState = rememberScrollState()

    ModalDrawerSheet(
        drawerContainerColor = SurfaceColor,
        drawerContentColor = TextPrimary,
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceColor)
                .verticalScroll(scrollState)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // HEADER CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariantColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo with rotating ring
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(76.dp)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .rotate(rotationAngle)
                        ) {
                            val strokeWidth = 2.dp.toPx()
                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                            drawCircle(
                                color = cyanColor,
                                style = Stroke(width = strokeWidth, pathEffect = dashEffect)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(SurfaceColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = cyanColor,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Net",
                            fontSize = 22.sp,
                            fontFamily = RajdhaniFont,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Share",
                            fontSize = 22.sp,
                            fontFamily = RajdhaniFont,
                            fontWeight = FontWeight.Bold,
                            color = cyanColor
                        )
                        Text(
                            text = " Proxy",
                            fontSize = 22.sp,
                            fontFamily = RajdhaniFont,
                            fontWeight = FontWeight.Normal,
                            color = TextPrimary.copy(alpha = 0.85f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = purpleColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "v1.0.0",
                            fontSize = 11.sp,
                            fontFamily = SpaceMonoFont,
                            fontWeight = FontWeight.Bold,
                            color = purpleColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // 1. OFFICIAL TELEGRAM CHANNEL & UPDATES CARD
            Card(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/NetShareProxy"))
                        context.startActivity(intent)
                    } catch (e: Exception) { e.printStackTrace() }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF0088CC).copy(alpha = if (isDark) 0.4f else 0.25f), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0088CC).copy(alpha = if (isDark) 0.12f else 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0088CC)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Telegram", tint = Color.White, modifier = Modifier.size(16.dp))
                            }

                            Column {
                                Text(
                                    text = "NetShare Proxy™",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RajdhaniFont,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "t.me/NetShareProxy",
                                    fontSize = 11.sp,
                                    fontFamily = SpaceMonoFont,
                                    color = Color(0xFF0088CC),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF0088CC).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "UPDATES",
                                fontSize = 9.sp,
                                fontFamily = SpaceMonoFont,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0088CC),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = LanguageManager.get("telegram_channel_desc"),
                        fontSize = 11.sp,
                        fontFamily = InterFont,
                        color = TextSecondary,
                        lineHeight = 14.sp
                    )
                }
            }

            // 2. DEVELOPER CONTACT CARD WITH CUSTOM AVATAR
            Card(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/A_DM_IN123"))
                        context.startActivity(intent)
                    } catch (e: Exception) { e.printStackTrace() }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cyanColor.copy(alpha = if (isDark) 0.35f else 0.25f), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariantColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Developer Avatar Profile Image
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, cyanColor.copy(alpha = 0.8f), CircleShape)
                            .background(SurfaceColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_developer_avatar),
                            contentDescription = "App Developer Avatar",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Developer Name & Handle
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = LanguageManager.get("app_developer"),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RajdhaniFont,
                                color = TextPrimary
                            )
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = cyanColor,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = "@A_DM_IN123",
                            fontSize = 11.sp,
                            fontFamily = SpaceMonoFont,
                            color = cyanColor,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Compact Clean Support Pill Badge (Never wraps)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = cyanColor.copy(alpha = if (isDark) 0.18f else 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cyanColor.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = null,
                                tint = cyanColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = LanguageManager.get("dev_contact"),
                                fontSize = 10.sp,
                                fontFamily = SpaceMonoFont,
                                fontWeight = FontWeight.Bold,
                                color = cyanColor
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f))

            // NAVIGATION SECTION
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = LanguageManager.get("navigation"),
                    fontSize = 10.sp,
                    fontFamily = SpaceMonoFont,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                val navItems = listOf(
                    Triple("home", LanguageManager.get("home"), Icons.Default.Home),
                    Triple("stats", LanguageManager.get("stats"), Icons.Default.BarChart),
                    Triple("clients", LanguageManager.get("clients"), Icons.Default.Devices),
                    Triple("logs", LanguageManager.get("logs"), Icons.Default.Terminal),
                    Triple("config", LanguageManager.get("config"), Icons.Default.Settings)
                )

                navItems.forEach { (route, label, icon) ->
                    val isSelected = selectedRoute == route
                    NavigationDrawerItem(
                        icon = { Icon(icon, contentDescription = null, tint = if (isSelected) cyanColor else TextSecondary) },
                        label = {
                            Text(
                                text = label,
                                fontFamily = InterFont,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) cyanColor else TextPrimary
                            )
                        },
                        selected = isSelected,
                        onClick = {
                            onNavigate(route)
                            onCloseDrawer()
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = cyanColor.copy(alpha = 0.12f),
                            unselectedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // SHARE APP ITEM (Replaces Share QR)
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Share, contentDescription = null, tint = purpleColor) },
                    label = {
                        Text(
                            text = LanguageManager.get("share_app"),
                            fontFamily = InterFont,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    },
                    selected = false,
                    onClick = {
                        try {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "⚡ NetShare Proxy™ - Share your Android VPN/Internet connection over Wi-Fi, Hotspot, and USB with PC, TV, & consoles!\n\n📥 Updates & Official Releases: https://t.me/NetShareProxy\n👨‍💻 Developer Support: https://t.me/A_DM_IN123"
                                )
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share NetShare Proxy")
                            context.startActivity(shareIntent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        onCloseDrawer()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color.Transparent,
                        unselectedContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f))

            // QUICK TOOLS SECTION
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = LanguageManager.get("quick_tools"),
                    fontSize = 10.sp,
                    fontFamily = SpaceMonoFont,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onOpenDiagnostics()
                            onCloseDrawer()
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.WifiTethering, contentDescription = null, tint = greenColor)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(LanguageManager.get("test_proxy"), fontFamily = InterFont, color = TextPrimary)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onNavigate("home")
                            onCloseDrawer()
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.WifiTethering, contentDescription = null, tint = cyanColor)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(LanguageManager.get("shizuku_mode"), fontFamily = InterFont, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Rootless Direct Tether (No Proxy)", fontSize = 10.sp, color = cyanColor)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onOpenFaqs()
                            onCloseDrawer()
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Help, contentDescription = null, tint = amberColor)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(LanguageManager.get("faqs"), fontFamily = InterFont, color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // EXIT BUTTON
            OutlinedButton(
                onClick = onExitApp,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, redColor)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = redColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = LanguageManager.get("exit_app"),
                    color = redColor,
                    fontFamily = RajdhaniFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

