package com.admin.vpnshare.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.admin.vpnshare.ui.theme.*

@Composable
fun FloatingBottomNavigationBar(
    selectedRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        ScreenRoute.HOME,
        ScreenRoute.STATS,
        ScreenRoute.CLIENTS,
        ScreenRoute.LOGS
    )

    val isDark = com.admin.vpnshare.ThemeManager.isDarkMode
    val activeColor = StatusCyan
    val inactiveColor = if (isDark) Color(0xFF687490) else Color(0xFF64748B)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isDark) 20.dp else 8.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = Color.Black,
                    spotColor = if (isDark) NeonCyan.copy(alpha = 0.35f) else Color(0x18000000)
                )
                .clip(RoundedCornerShape(32.dp))
                // Frosted glass acrylic background (adaptive translucent glass with neon reflection)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            GlassBgDark,
                            GlassBgBase
                        )
                    )
                )
                // Luminous glass border
                .border(
                    width = 1.dp,
                    brush = if (isDark) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0x6600E5FF), // neon cyan glow
                                Color(0x338000FF), // neon purple
                                Color(0x6600E5FF)  // neon cyan glow
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0x300284C7),
                                Color(0x207C3AED),
                                Color(0x300284C7)
                            )
                        )
                    },
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = selectedRoute == item.route
                
                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                    label = "navScale"
                )
                
                val tintColor by animateColorAsState(
                    targetValue = if (isSelected) activeColor else inactiveColor,
                    label = "navColor"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onNavigate(item.route) }
                        .padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .scale(iconScale)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) {
                                    Brush.radialGradient(
                                        colors = listOf(
                                            activeColor.copy(alpha = if (isDark) 0.25f else 0.15f),
                                            activeColor.copy(alpha = 0.05f),
                                            Color.Transparent
                                        )
                                    )
                                } else {
                                    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                }
                            )
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        0.5.dp,
                                        activeColor.copy(alpha = 0.4f),
                                        RoundedCornerShape(16.dp)
                                    )
                                } else Modifier
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = tintColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = item.title,
                        color = tintColor,
                        fontSize = 10.sp,
                        fontFamily = RajdhaniFont,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
