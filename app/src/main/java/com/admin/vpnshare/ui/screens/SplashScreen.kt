package com.admin.vpnshare.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.admin.vpnshare.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinish: () -> Unit
) {
    // Animation progress triggers
    var logoVisible by remember { mutableStateOf(false) }
    var ringVisible by remember { mutableStateOf(false) }
    var netVisible by remember { mutableStateOf(false) }
    var shareVisible by remember { mutableStateOf(false) }
    var proxyVisible by remember { mutableStateOf(false) }
    var subtitleVisible by remember { mutableStateOf(false) }
    var creditVisible by remember { mutableStateOf(false) }
    var isExiting by remember { mutableStateOf(false) }

    val logoAlpha by animateFloatAsState(
        targetValue = if (logoVisible && !isExiting) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "logoAlpha"
    )

    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible && !isExiting) 1.0f else 0.6f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "logoScale"
    )

    val ringSweepProgress by animateFloatAsState(
        targetValue = if (ringVisible && !isExiting) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing),
        label = "ringSweepProgress"
    )

    val netAlpha by animateFloatAsState(
        targetValue = if (netVisible && !isExiting) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "netAlpha"
    )

    val shareAlpha by animateFloatAsState(
        targetValue = if (shareVisible && !isExiting) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "shareAlpha"
    )

    val proxyAlpha by animateFloatAsState(
        targetValue = if (proxyVisible && !isExiting) 0.85f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "proxyAlpha"
    )

    val titleOffsetY by animateDpAsState(
        targetValue = if (netVisible && !isExiting) 0.dp else 24.dp,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "titleOffsetY"
    )

    val subtitleAlpha by animateFloatAsState(
        targetValue = if (subtitleVisible && !isExiting) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "subtitleAlpha"
    )

    val creditAlpha by animateFloatAsState(
        targetValue = if (creditVisible && !isExiting) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "creditAlpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAngle"
    )

    LaunchedEffect(Unit) {
        // Timeline sequence
        logoVisible = true // 0.0s
        delay(600)
        ringVisible = true // 0.6s
        delay(800)
        netVisible = true // 1.4s
        delay(150)
        shareVisible = true // 1.55s
        delay(150)
        proxyVisible = true // 1.7s
        delay(300)
        subtitleVisible = true // 2.0s
        delay(350)
        creditVisible = true // 2.35s
        delay(500)
        isExiting = true // 2.85s
        delay(400)
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Centered glowing orb / globe icon block
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(160.dp)
                    .scale(logoScale)
                    .alpha(logoAlpha)
            ) {
                // Animated rotating dashed cyan ring
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(rotationAngle)
                ) {
                    val strokeWidth = 3.dp.toPx()
                    val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    drawArc(
                        color = NeonCyan,
                        startAngle = 0f,
                        sweepAngle = 360f * ringSweepProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, pathEffect = dashPathEffect)
                    )
                }

                // Inner dark circle with shield + signal icon
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(SurfaceColor)
                        .border(1.dp, CardBorderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield Logo",
                        tint = NeonCyan,
                        modifier = Modifier.size(56.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.WifiTethering,
                        contentDescription = "Signal Logo",
                        tint = NeonGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Title
            Row(
                modifier = Modifier.offset(y = titleOffsetY),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Net",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = RajdhaniFont,
                    color = TextPrimary,
                    letterSpacing = 2.sp,
                    modifier = Modifier.alpha(netAlpha)
                )
                Text(
                    text = "Share",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = RajdhaniFont,
                    color = NeonCyan,
                    letterSpacing = 2.sp,
                    modifier = Modifier.alpha(shareAlpha)
                )
                Text(
                    text = " Proxy",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = RajdhaniFont,
                    color = TextPrimary,
                    letterSpacing = 2.sp,
                    modifier = Modifier.alpha(proxyAlpha)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle
            Text(
                text = "PROXY SERVER",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = SpaceMonoFont,
                color = NeonCyan,
                letterSpacing = 3.sp,
                modifier = Modifier.alpha(subtitleAlpha)
            )
        }

        // Elevated & Beautiful Bottom Creator Badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 68.dp)
                .alpha(creditAlpha)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(
                            SurfaceColor.copy(alpha = 0.85f),
                            SurfaceVariantColor.copy(alpha = 0.85f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(
                            NeonCyan.copy(alpha = 0.4f),
                            StatusPurple.copy(alpha = 0.3f),
                            NeonCyan.copy(alpha = 0.4f)
                        )
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 18.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "DEV",
                    fontSize = 9.sp,
                    fontFamily = SpaceMonoFont,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "•",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Text(
                    text = "@A_DM_IN123",
                    fontSize = 12.sp,
                    fontFamily = SpaceMonoFont,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
