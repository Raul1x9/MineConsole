package com.raul1x9.mineconsole.views

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raul1x9.mineconsole.viewmodels.MainViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var selectedRole by remember { mutableStateOf("Viewer") }
    var generatedToken by remember { mutableStateOf("") }
    var isCopied by remember { mutableStateOf(false) }

    val biometricsEnabled = viewModel.biometricsEnabled.value
    val tailscaleConnected = viewModel.tailscaleConnected.value

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SETTINGS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color(0xFF00FF66),
                            letterSpacing = 2.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF00FF66)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0C0C0C)
                )
            )
        },
        containerColor = Color(0xFF0C0C0C)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // General Settings Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SYSTEM CONFIGURATION",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF00FF66))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Secure Biometrics", color = Color.White)
                            Text(
                                "Require Face/Fingerprint unlock",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = biometricsEnabled,
                            onCheckedChange = { viewModel.setBiometricsEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color(0xFF00FF66)
                            )
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.05f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Tailscale Link", color = Color.White)
                            Text(
                                "Simulate active WireGuard tunneling",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = tailscaleConnected,
                            onCheckedChange = { viewModel.setTailscaleConnected(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color(0xFF00FF66)
                            )
                        )
                    }
                }
            }

            // Share Settings Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ROLE-BASED SHARING LAB",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF00FF66))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Generate a restricted access token link for guest moderators.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Role Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val roles = listOf("Viewer", "Moderator", "Admin")
                        roles.forEach { role ->
                            val active = selectedRole == role
                            Button(
                                onClick = {
                                    selectedRole = role
                                    generatedToken = ""
                                    isCopied = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (active) Color(0xFF00FF66).copy(alpha = 0.15f) else Color.Transparent,
                                    contentColor = if (active) Color(0xFF00FF66) else Color.White.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        width = 1.dp,
                                        color = if (active) Color(0xFF00FF66) else Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                            ) {
                                Text(role, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            generatedToken = "mineconsole://guest?role=${selectedRole.lowercase()}&token=${UUID.randomUUID().toString().take(8)}"
                            isCopied = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00FF66),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("GENERATE GUEST ACCESS LINK", style = MaterialTheme.typography.labelMedium)
                    }

                    if (generatedToken.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = generatedToken,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )

                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("MineConsole Token", generatedToken)
                                    clipboard.setPrimaryClip(clip)
                                    isCopied = true
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = if (isCopied) Color(0xFF00FF66) else Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
