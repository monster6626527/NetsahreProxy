package com.admin.vpnshare.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.admin.vpnshare.ui.viewmodel.ProxyViewModel

private val BgColor = Color(0xFF080810)
private val CardBg = Color(0xFF0F0F1A)
private val NeonCyan = Color(0xFF00E5FF)
private val NeonPurple = Color(0xFFAA00FF)
private val NeonGreen = Color(0xFF00E676)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ProxyViewModel) {
    val currentSettings by viewModel.settings.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()

    var httpPortText by remember(currentSettings.httpPort) { mutableStateOf(currentSettings.httpPort.toString()) }
    var socksPortText by remember(currentSettings.socksPort) { mutableStateOf(currentSettings.socksPort.toString()) }
    var selectedProtocol by remember(currentSettings.protocolMode) { mutableStateOf(currentSettings.protocolMode) }
    var selectedBind by remember(currentSettings.bindInterface) { mutableStateOf(currentSettings.bindInterface) }
    var authEnabled by remember(currentSettings.authEnabled) { mutableStateOf(currentSettings.authEnabled) }
    var username by remember(currentSettings.username) { mutableStateOf(currentSettings.username) }
    var password by remember(currentSettings.password) { mutableStateOf(currentSettings.password) }
    var passwordVisible by remember { mutableStateOf(false) }
    var startOnBoot by remember(currentSettings.startOnBoot) { mutableStateOf(currentSettings.startOnBoot) }
    var maxConns by remember(currentSettings.maxConnections) { mutableFloatStateOf(currentSettings.maxConnections.toFloat()) }
    var pacEnabled by remember(currentSettings.pacServerEnabled) { mutableStateOf(currentSettings.pacServerEnabled) }

    fun saveAll() {
        val hPort = httpPortText.toIntOrNull() ?: 8080
        val sPort = socksPortText.toIntOrNull() ?: 1080
        val updated = currentSettings.copy(
            httpPort = hPort.coerceIn(1024, 65535),
            socksPort = sPort.coerceIn(1024, 65535),
            protocolMode = selectedProtocol,
            bindInterface = selectedBind,
            authEnabled = authEnabled,
            username = username,
            password = password,
            startOnBoot = startOnBoot,
            maxConnections = maxConns.toInt().coerceIn(1, 100),
            pacServerEnabled = pacEnabled
        )
        viewModel.updateSettings(updated)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = "SERVER CONFIGURATION",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NeonCyan,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)
            )

            if (isRunning) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NeonCyan.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, NeonCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Note: Restart proxy server after modifying ports or protocol to apply changes.",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Protocol Selection Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, Color(0xFF222233))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ACTIVE PROTOCOLS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("HTTP", "SOCKS5", "BOTH").forEach { proto ->
                            val isSelected = selectedProtocol == proto
                            Surface(
                                onClick = {
                                    selectedProtocol = proto
                                    saveAll()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color(0xFF151522),
                                border = BorderStroke(1.dp, if (isSelected) NeonCyan else Color.Transparent)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                ) {
                                    Text(
                                        text = proto,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) NeonCyan else Color.Gray,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Ports Configuration Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, Color(0xFF222233))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "NETWORK PORTS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = httpPortText,
                            onValueChange = {
                                httpPortText = it
                                saveAll()
                            },
                            label = { Text("HTTP Port", color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color(0xFF333344),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = socksPortText,
                            onValueChange = {
                                socksPortText = it
                                saveAll()
                            },
                            label = { Text("SOCKS5 Port", color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPurple,
                                unfocusedBorderColor = Color(0xFF333344),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Authentication Settings Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, Color(0xFF222233))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "PROXY AUTHENTICATION",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Require username & password",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = authEnabled,
                            onCheckedChange = {
                                authEnabled = it
                                saveAll()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = NeonCyan.copy(alpha = 0.3f)
                            )
                        )
                    }

                    if (authEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = username,
                            onValueChange = {
                                username = it
                                saveAll()
                            },
                            label = { Text("Username", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color(0xFF333344),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                saveAll()
                            },
                            label = { Text("Password", color = Color.Gray) },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = Color.Gray
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color(0xFF333344),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Connection Limits Slider Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, Color(0xFF222233))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MAX CLIENT CONNECTIONS: ${maxConns.toInt()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = maxConns,
                        onValueChange = {
                            maxConns = it
                            saveAll()
                        },
                        valueRange = 5f..100f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = Color(0xFF222233)
                        )
                    )
                }
            }

            // Boot Auto-Start Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, Color(0xFF222233))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "START ON BOOT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Automatically start proxy server when device reboots",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = startOnBoot,
                        onCheckedChange = {
                            startOnBoot = it
                            saveAll()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = NeonCyan.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
